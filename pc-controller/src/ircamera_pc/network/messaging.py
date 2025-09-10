"""
Reliable Messaging Service for IRCamera PC Controller

Provides reliable message delivery with acknowledgments, retry logic, and
priority queuing to match the Android implementation.
"""

import asyncio
import json
import time
import uuid
from collections import defaultdict, deque
from dataclasses import asdict, dataclass
from datetime import datetime
from enum import Enum
from typing import Any, Callable, Dict, List, Optional

try:
    from loguru import logger
except ImportError:
    try:
        from ..utils.simple_logger import logger
    except ImportError:
        # Fallback logger for testing
        class FallbackLogger:
            def info(self, msg):
                print(f"INFO: {msg}")

            def debug(self, msg):
                print(f"DEBUG: {msg}")

            def warning(self, msg):
                print(f"WARNING: {msg}")

            def error(self, msg):
                print(f"ERROR: {msg}")

        logger = FallbackLogger()

try:
    from ..core.config import config
except ImportError:
    # Fallback config for testing
    class FallbackConfig:
        def get(self, key, default=None):
            config_map = {
                "messaging.base_retry_delay": 1.0,
                "messaging.max_retry_delay": 30.0,
                "messaging.default_timeout": 30.0,
                "messaging.cleanup_interval": 60.0,
            }
            return config_map.get(key, default)

    config = FallbackConfig()


class MessagePriority(Enum):
    """Message priority levels."""

    LOW = 1
    NORMAL = 2
    HIGH = 3
    CRITICAL = 4


class MessageStatus(Enum):
    """Message delivery status."""

    PENDING = "pending"
    SENT = "sent"
    ACKNOWLEDGED = "acknowledged"
    FAILED = "failed"
    EXPIRED = "expired"


@dataclass
class ReliableMessage:
    """Represents a reliable message."""

    message_id: str
    target_host: str
    target_port: int
    message_type: str
    content: Dict[str, Any]
    priority: MessagePriority
    created_at: float
    expires_at: float
    max_retries: int
    retry_count: int = 0
    status: MessageStatus = MessageStatus.PENDING
    last_attempt: Optional[float] = None
    error_message: Optional[str] = None


@dataclass
class MessageCallback:
    """Callback configuration for message delivery events."""

    on_acknowledged: Optional[Callable[[str], None]] = None
    on_failed: Optional[Callable[[str, str], None]] = None
    on_retrying: Optional[Callable[[str, int], None]] = None


class ReliableMessageService:
    """
    Reliable messaging service that ensures message delivery with acknowledgments,
    automatic retries, and priority-based queuing.
    """

    def __init__(self):
        """Initialize the reliable messaging service."""
        self.pending_messages: Dict[str, ReliableMessage] = {}
        self.message_callbacks: Dict[str, MessageCallback] = {}
        self.message_handlers: Dict[
            str, Callable[[Dict[str, Any]], Optional[Dict[str, Any]]]
        ] = {}
        self.priority_queues: Dict[MessagePriority, deque] = {
            priority: deque() for priority in MessagePriority
        }

        # Message transport - will be set by the server
        self.transport: Optional[Callable] = None

        # Service state
        self.is_running = False
        self.processing_task: Optional[asyncio.Task] = None
        self.cleanup_task: Optional[asyncio.Task] = None

        # Configuration
        self.base_retry_delay = config.get("messaging.base_retry_delay", 1.0)
        self.max_retry_delay = config.get("messaging.max_retry_delay", 30.0)
        self.default_timeout = config.get("messaging.default_timeout", 30.0)
        self.cleanup_interval = config.get("messaging.cleanup_interval", 60.0)

    def set_transport(self, transport: Callable):
        """
        Set the message transport function.

        Args:
            transport: Async function that takes (host, port, message_dict) and returns bool
        """
        self.transport = transport

    def register_message_handler(
        self,
        message_type: str,
        handler: Callable[[Dict[str, Any]], Optional[Dict[str, Any]]],
    ):
        """
        Register a handler for incoming messages of a specific type.

        Args:
            message_type: The message type to handle
            handler: Function that processes the message and optionally returns a response
        """
        self.message_handlers[message_type] = handler
        logger.debug(f"Registered handler for message type: {message_type}")

    def unregister_message_handler(self, message_type: str):
        """Unregister a message handler."""
        if message_type in self.message_handlers:
            del self.message_handlers[message_type]
            logger.debug(f"Unregistered handler for message type: {message_type}")

    async def initialize(self) -> bool:
        """
        Initialize the reliable messaging service.

        Returns:
            bool: True if initialization successful
        """
        try:
            if self.is_running:
                logger.warning("Messaging service already running")
                return True

            logger.info("Initializing reliable messaging service...")

            # Start background tasks
            self.processing_task = asyncio.create_task(self._message_processor())
            self.cleanup_task = asyncio.create_task(self._cleanup_processor())

            self.is_running = True
            logger.info("Reliable messaging service initialized")
            return True

        except Exception as e:
            logger.error(f"Failed to initialize messaging service: {e}")
            await self.shutdown()
            return False

    async def shutdown(self):
        """Shutdown the messaging service."""
        if not self.is_running:
            return

        logger.info("Shutting down reliable messaging service...")

        self.is_running = False

        # Cancel background tasks
        if self.processing_task:
            self.processing_task.cancel()
            try:
                await self.processing_task
            except asyncio.CancelledError:
                pass

        if self.cleanup_task:
            self.cleanup_task.cancel()
            try:
                await self.cleanup_task
            except asyncio.CancelledError:
                pass

        # Fail any pending messages
        for message_id, message in self.pending_messages.items():
            message.status = MessageStatus.FAILED
            message.error_message = "Service shutdown"
            await self._notify_message_failed(message_id, "Service shutdown")

        self.pending_messages.clear()
        self.message_callbacks.clear()

        logger.info("Reliable messaging service shutdown complete")

    async def send_message(
        self,
        target_host: str,
        target_port: int,
        message_type: str,
        content: Dict[str, Any],
        priority: MessagePriority = MessagePriority.NORMAL,
        timeout_seconds: float = None,
        max_retries: int = 3,
        callback: Optional[MessageCallback] = None,
    ) -> str:
        """
        Send a reliable message with automatic retry logic.

        Args:
            target_host: Target device IP address
            target_port: Target device port
            message_type: Type of message
            content: Message content
            priority: Message priority level
            timeout_seconds: Message timeout in seconds
            max_retries: Maximum retry attempts
            callback: Optional callback for delivery events

        Returns:
            str: Unique message ID
        """
        if not self.is_running:
            raise RuntimeError("Messaging service not running")

        if not self.transport:
            raise RuntimeError("No transport configured for messaging service")

        # Generate unique message ID
        message_id = str(uuid.uuid4())

        # Set default timeout
        if timeout_seconds is None:
            timeout_seconds = self.default_timeout

        # Create message
        current_time = time.time()
        message = ReliableMessage(
            message_id=message_id,
            target_host=target_host,
            target_port=target_port,
            message_type=message_type,
            content=content,
            priority=priority,
            created_at=current_time,
            expires_at=current_time + timeout_seconds,
            max_retries=max_retries,
        )

        # Store message and callback
        self.pending_messages[message_id] = message
        if callback:
            self.message_callbacks[message_id] = callback

        # Add to priority queue
        self.priority_queues[priority].append(message_id)

        logger.debug(
            f"Queued reliable message {message_id} to {target_host}:{target_port} (priority: {priority.name})"
        )
        return message_id

    async def handle_acknowledgment(
        self, message_id: str, success: bool, error_message: str = None
    ):
        """
        Handle an acknowledgment for a sent message.

        Args:
            message_id: The message ID being acknowledged
            success: Whether the message was successfully processed
            error_message: Error message if success is False
        """
        if message_id not in self.pending_messages:
            logger.warning(f"Received acknowledgment for unknown message: {message_id}")
            return

        message = self.pending_messages[message_id]

        if success:
            message.status = MessageStatus.ACKNOWLEDGED
            await self._notify_message_acknowledged(message_id)
            logger.debug(f"Message {message_id} acknowledged successfully")
        else:
            message.status = MessageStatus.FAILED
            message.error_message = error_message or "Remote processing failed"
            await self._notify_message_failed(message_id, message.error_message)
            logger.warning(f"Message {message_id} failed: {message.error_message}")

        # Remove from pending messages
        self._remove_pending_message(message_id)

    async def handle_incoming_message(
        self, message_data: Dict[str, Any], sender_info: Dict[str, Any] = None
    ) -> Optional[Dict[str, Any]]:
        """
        Handle an incoming message from a remote device.

        Args:
            message_data: The received message data
            sender_info: Information about the sender (host, port, etc.)

        Returns:
            Optional[Dict[str, Any]]: Response message or None
        """
        try:
            message_type = message_data.get("message_type")
            if not message_type:
                logger.warning("Received message without message_type")
                return self._create_error_response("Missing message_type")

            # Check for acknowledgment messages
            if message_type == "message_ack":
                await self._handle_ack_message(message_data)
                return None
            elif message_type == "message_nack":
                await self._handle_nack_message(message_data)
                return None

            # Handle regular messages
            if message_type in self.message_handlers:
                handler = self.message_handlers[message_type]

                try:
                    response = handler(message_data)

                    # Send positive acknowledgment
                    await self._send_acknowledgment(message_data, True, sender_info)

                    return response

                except Exception as e:
                    logger.error(f"Handler error for {message_type}: {e}")

                    # Send negative acknowledgment
                    await self._send_acknowledgment(
                        message_data, False, sender_info, str(e)
                    )

                    return self._create_error_response(f"Handler error: {e}")
            else:
                logger.warning(f"No handler for message type: {message_type}")

                # Send negative acknowledgment
                await self._send_acknowledgment(
                    message_data, False, sender_info, f"No handler for {message_type}"
                )

                return self._create_error_response(
                    f"No handler for message type: {message_type}"
                )

        except Exception as e:
            logger.error(f"Error handling incoming message: {e}")
            return self._create_error_response(f"Processing error: {e}")

    async def _message_processor(self):
        """Background task that processes the message queues."""
        logger.debug("Message processor started")

        while self.is_running:
            try:
                # Process messages by priority (highest first)
                message_processed = False

                for priority in reversed(list(MessagePriority)):
                    queue = self.priority_queues[priority]

                    if queue:
                        message_id = queue.popleft()

                        if message_id in self.pending_messages:
                            await self._process_message(message_id)
                            message_processed = True
                            break

                if not message_processed:
                    # No messages to process, sleep briefly
                    await asyncio.sleep(0.1)

            except Exception as e:
                logger.error(f"Error in message processor: {e}")
                await asyncio.sleep(1.0)

        logger.debug("Message processor stopped")

    async def _process_message(self, message_id: str):
        """Process a single message."""
        message = self.pending_messages.get(message_id)
        if not message:
            return

        current_time = time.time()

        # Check if message has expired
        if current_time >= message.expires_at:
            message.status = MessageStatus.EXPIRED
            await self._notify_message_failed(message_id, "Message expired")
            self._remove_pending_message(message_id)
            return

        # Check if we should retry yet (exponential backoff)
        if message.last_attempt:
            retry_delay = min(
                self.base_retry_delay * (2**message.retry_count), self.max_retry_delay
            )

            if current_time - message.last_attempt < retry_delay:
                # Not time to retry yet, put back in queue
                self.priority_queues[message.priority].append(message_id)
                return

        # Attempt to send message
        try:
            # Create message payload
            payload = {
                "message_id": message.message_id,
                "message_type": message.message_type,
                "timestamp": current_time,
                "content": message.content,
                "requires_ack": True,
            }

            # Attempt delivery
            success = await self.transport(
                message.target_host, message.target_port, payload
            )

            message.last_attempt = current_time

            if success:
                message.status = MessageStatus.SENT
                # Wait for acknowledgment (timeout handled by expiry)
                logger.debug(f"Message {message_id} sent, waiting for acknowledgment")
            else:
                # Send failed, check for retry
                await self._handle_send_failure(message_id, "Transport failed")

        except Exception as e:
            logger.error(f"Error sending message {message_id}: {e}")
            await self._handle_send_failure(message_id, str(e))

    async def _handle_send_failure(self, message_id: str, error: str):
        """Handle a message send failure."""
        message = self.pending_messages.get(message_id)
        if not message:
            return

        message.retry_count += 1

        if message.retry_count <= message.max_retries:
            # Schedule retry
            await self._notify_message_retrying(message_id, message.retry_count)
            self.priority_queues[message.priority].append(message_id)
            logger.debug(
                f"Retrying message {message_id} (attempt {message.retry_count}/{message.max_retries})"
            )
        else:
            # Max retries exceeded
            message.status = MessageStatus.FAILED
            message.error_message = f"Max retries exceeded: {error}"
            await self._notify_message_failed(message_id, message.error_message)
            self._remove_pending_message(message_id)
            logger.warning(
                f"Message {message_id} failed permanently: {message.error_message}"
            )

    async def _cleanup_processor(self):
        """Background task that cleans up expired messages and callbacks."""
        logger.debug("Cleanup processor started")

        while self.is_running:
            try:
                await asyncio.sleep(self.cleanup_interval)

                current_time = time.time()
                expired_messages = []

                # Find expired messages
                for message_id, message in self.pending_messages.items():
                    if current_time >= message.expires_at and message.status in [
                        MessageStatus.PENDING,
                        MessageStatus.SENT,
                    ]:
                        expired_messages.append(message_id)

                # Clean up expired messages
                for message_id in expired_messages:
                    message = self.pending_messages[message_id]
                    message.status = MessageStatus.EXPIRED
                    await self._notify_message_failed(message_id, "Message expired")
                    self._remove_pending_message(message_id)

                if expired_messages:
                    logger.debug(f"Cleaned up {len(expired_messages)} expired messages")

            except Exception as e:
                logger.error(f"Error in cleanup processor: {e}")

        logger.debug("Cleanup processor stopped")

    async def _handle_ack_message(self, message_data: Dict[str, Any]):
        """Handle positive acknowledgment message."""
        original_message_id = message_data.get("original_message_id")
        if original_message_id:
            await self.handle_acknowledgment(original_message_id, True)

    async def _handle_nack_message(self, message_data: Dict[str, Any]):
        """Handle negative acknowledgment message."""
        original_message_id = message_data.get("original_message_id")
        error_message = message_data.get("error_message", "Remote processing failed")
        if original_message_id:
            await self.handle_acknowledgment(original_message_id, False, error_message)

    async def _send_acknowledgment(
        self,
        original_message: Dict[str, Any],
        success: bool,
        sender_info: Dict[str, Any] = None,
        error_message: str = None,
    ):
        """Send acknowledgment for a received message."""
        if not self.transport or not sender_info:
            return

        original_message_id = original_message.get("message_id")
        if not original_message_id:
            return

        ack_type = "message_ack" if success else "message_nack"
        ack_payload = {
            "message_id": str(uuid.uuid4()),
            "message_type": ack_type,
            "original_message_id": original_message_id,
            "timestamp": time.time(),
        }

        if not success and error_message:
            ack_payload["error_message"] = error_message

        try:
            await self.transport(
                sender_info.get("host"), sender_info.get("port"), ack_payload
            )
        except Exception as e:
            logger.error(f"Failed to send acknowledgment: {e}")

    def _create_error_response(self, error_message: str) -> Dict[str, Any]:
        """Create a standard error response."""
        return {
            "message_type": "error",
            "message_id": str(uuid.uuid4()),
            "timestamp": time.time(),
            "error": error_message,
        }

    def _remove_pending_message(self, message_id: str):
        """Remove a message from pending messages and callbacks."""
        self.pending_messages.pop(message_id, None)
        self.message_callbacks.pop(message_id, None)

    async def _notify_message_acknowledged(self, message_id: str):
        """Notify callback that message was acknowledged."""
        callback = self.message_callbacks.get(message_id)
        if callback and callback.on_acknowledged:
            try:
                callback.on_acknowledged(message_id)
            except Exception as e:
                logger.error(f"Error in acknowledgment callback: {e}")

    async def _notify_message_failed(self, message_id: str, error_message: str):
        """Notify callback that message failed."""
        callback = self.message_callbacks.get(message_id)
        if callback and callback.on_failed:
            try:
                callback.on_failed(message_id, error_message)
            except Exception as e:
                logger.error(f"Error in failure callback: {e}")

    async def _notify_message_retrying(self, message_id: str, attempt: int):
        """Notify callback that message is being retried."""
        callback = self.message_callbacks.get(message_id)
        if callback and callback.on_retrying:
            try:
                callback.on_retrying(message_id, attempt)
            except Exception as e:
                logger.error(f"Error in retry callback: {e}")

    def get_pending_message_count(self) -> int:
        """Get the number of pending messages."""
        return len(self.pending_messages)

    def get_queue_sizes(self) -> Dict[MessagePriority, int]:
        """Get the size of each priority queue."""
        return {
            priority: len(queue) for priority, queue in self.priority_queues.items()
        }
