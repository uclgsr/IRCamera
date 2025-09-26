import asyncio
import time
import uuid
from collections import deque
from dataclasses import dataclass
from enum import Enum
from typing import Any, Callable, Dict, Optional

try:
    except ImportError:
    try:
            except ImportError:

        class FallbackLogger:
            def info(self, msg) -> Any:
                print(f"INFO: {msg}")

            def debug(self, msg) -> Any:
                print(f"DEBUG: {msg}")

            def warning(self, msg) -> Any:
                print(f"WARNING: {msg}")

            def error(self, msg) -> Any:
                print(f"ERROR: {msg}")


        logger = FallbackLogger()

try:
    from ..core.config import config
except ImportError:

    class FallbackConfig:
        def get(self, key, default=None) -> Any:
            config_map = {
                "messaging.base_retry_delay": 1.0,
                "messaging.max_retry_delay": 30.0,
                "messaging.default_timeout": 30.0,
                "messaging.cleanup_interval": 60.0,
            }
            return config_map.get(key, default)


    config = FallbackConfig()


class MessagePriority(Enum):
    LOW = 1
    NORMAL = 2
    HIGH = 3
    CRITICAL = 4


class MessageStatus(Enum):
    PENDING = "pending"
    SENT = "sent"
    ACKNOWLEDGED = "acknowledged"
    FAILED = "failed"
    EXPIRED = "expired"


@dataclass
class ReliableMessage:
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
    on_acknowledged: Optional[Callable[[str], None]] = None
    on_failed: Optional[Callable[[str, str], None]] = None
    on_retrying: Optional[Callable[[str, int], None]] = None


class ReliableMessageService:

    def __init__(self):

        self.pending_messages: Dict[str, ReliableMessage] = {}
        self.message_callbacks: Dict[str, MessageCallback] = {}
        self.message_handlers: Dict[
            str, Callable[[Dict[str, Any]], Optional[Dict[str, Any]]]
        ] = {}
        self.priority_queues: Dict[MessagePriority, deque] = {
            priority: deque() for priority in MessagePriority
        }

        self.transport: Optional[Callable] = None

        self.is_running = False
        self.processing_task: Optional[asyncio.Task] = None
        self.cleanup_task: Optional[asyncio.Task] = None

        self.base_retry_delay = config.get("messaging.base_retry_delay", 1.0)
        self.max_retry_delay = config.get("messaging.max_retry_delay", 30.0)
        self.default_timeout = config.get("messaging.default_timeout", 30.0)
        self.cleanup_interval = config.get("messaging.cleanup_interval", 60.0)

    def set_transport(self, transport: None = Callable) -> None:

        self.transport = transport

    def register_message_handler(
            self,
            message_type: Any = str,
            handler: Any = Callable[[Dict[str, Any]], Optional[Dict[str, Any]]],
    ) -> Any:

        self.message_handlers[message_type] = handler
        
    def unregister_message_handler(self, message_type: Any = str) -> Any:

        if message_type in self.message_handlers:
            del self.message_handlers[message_type]
            
    async def initialize(self) -> bool:

        try:
            if self.is_running:
                                return True

            
            self.processing_task = asyncio.create_task(self._message_processor())
            self.cleanup_task = asyncio.create_task(self._cleanup_processor())

            self.is_running = True
                        return True

        except Exception as e:
                        await self.shutdown()
            return False

    async def shutdown(self) -> Any:

        if not self.is_running:
            return

        
        self.is_running = False

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

        for message_id, message in self.pending_messages.items():
            message.status = MessageStatus.FAILED
            message.error_message = "Service shutdown"
            await self._notify_message_failed(message_id, "Service shutdown")

        self.pending_messages.clear()
        self.message_callbacks.clear()

        
    async def send_message(
            self,
            target_host: str,
            target_port: int,
            message_type: str,
            content: Dict[str, Any],
            priority: MessagePriority = MessagePriority.NORMAL,
            timeout_seconds: Optional[float] = None,
            max_retries: int = 3,
            callback: Optional[MessageCallback] = None,
    ) -> str:

        if not self.is_running:
            raise RuntimeError("Messaging service not running")

        if not self.transport:
            raise RuntimeError("No transport configured for messaging service")

        message_id = str(uuid.uuid4())

        if timeout_seconds is None:
            timeout_seconds = self.default_timeout

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

        self.pending_messages[message_id] = message
        if callback:
            self.message_callbacks[message_id] = callback

        self.priority_queues[priority].append(message_id)

        logger.debug(
            f"Queued reliable message {message_id} to {target_host}:{target_port} (priority: {priority.name})"
        )
        return message_id

    async def handle_acknowledgment(
            self, message_id: str, success: bool, error_message: Optional[str] = None
    ) -> None:

        if message_id not in self.pending_messages:
                        return

        message = self.pending_messages[message_id]

        if success:
            message.status = MessageStatus.ACKNOWLEDGED
            await self._notify_message_acknowledged(message_id)
                    else:
            message.status = MessageStatus.FAILED
            message.error_message = error_message or "Remote processing failed"
            await self._notify_message_failed(message_id, message.error_message)
            
        self._remove_pending_message(message_id)

    async def handle_incoming_message(
            self, message_data: Dict[str, Any], sender_info: Dict[str, Any] = None
    ) -> Optional[Dict[str, Any]]:

        try:
            message_type = message_data.get("message_type")
            if not message_type:
                                return self._create_error_response("Missing message_type")

            if message_type == "message_ack":
                await self._handle_ack_message(message_data)
                return None
            elif message_type == "message_nack":
                await self._handle_nack_message(message_data)
                return None

            if message_type in self.message_handlers:
                handler = self.message_handlers[message_type]

                try:
                    response = handler(message_data)

                    await self._send_acknowledgment(message_data, True, sender_info)

                    return response

                except Exception as e:
                    
                    await self._send_acknowledgment(
                        message_data, False, sender_info, str(e)
                    )

                    return self._create_error_response(f"Handler error: {e}")
            else:
                
                await self._send_acknowledgment(
                    message_data, False, sender_info, f"No handler for {message_type}"
                )

                return self._create_error_response(
                    f"No handler for message type: {message_type}"
                )

        except Exception as e:
                        return self._create_error_response(f"Processing error: {e}")

    async def _message_processor(self):

        
        while self.is_running:
            try:

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
                    await asyncio.sleep(0.1)

            except Exception as e:
                                await asyncio.sleep(1.0)

        
    async def _process_message(self, message_id: str):

        message = self.pending_messages.get(message_id)
        if not message:
            return

        current_time = time.time()

        if current_time >= message.expires_at:
            message.status = MessageStatus.EXPIRED
            await self._notify_message_failed(message_id, "Message expired")
            self._remove_pending_message(message_id)
            return

        if message.last_attempt:
            retry_delay = min(
                self.base_retry_delay * (2 ** message.retry_count), self.max_retry_delay
            )

            if current_time - message.last_attempt < retry_delay:
                self.priority_queues[message.priority].append(message_id)
                return

        try:

            payload = {
                "message_id": message.message_id,
                "message_type": message.message_type,
                "timestamp": current_time,
                "content": message.content,
                "requires_ack": True,
            }

            success = await self.transport(
                message.target_host, message.target_port, payload
            )

            message.last_attempt = current_time

            if success:
                message.status = MessageStatus.SENT

                            else:

                await self._handle_send_failure(message_id, "Transport failed")

        except Exception as e:
                        await self._handle_send_failure(message_id, str(e))

    async def _handle_send_failure(self, message_id: str, error: str):

        message = self.pending_messages.get(message_id)
        if not message:
            return

        message.retry_count += 1

        if message.retry_count <= message.max_retries:

            await self._notify_message_retrying(message_id, message.retry_count)
            self.priority_queues[message.priority].append(message_id)
            logger.debug(
                f"Retrying message {message_id} (attempt {message.retry_count}/{message.max_retries})"
            )
        else:

            message.status = MessageStatus.FAILED
            message.error_message = f"Max retries exceeded: {error}"
            await self._notify_message_failed(message_id, message.error_message)
            self._remove_pending_message(message_id)
            
    async def _cleanup_processor(self):

        
        while self.is_running:
            try:
                await asyncio.sleep(self.cleanup_interval)

                current_time = time.time()
                expired_messages = []

                for message_id, message in self.pending_messages.items():
                    if current_time >= message.expires_at and message.status in [
                        MessageStatus.PENDING,
                        MessageStatus.SENT,
                    ]:
                        expired_messages.append(message_id)

                for message_id in expired_messages:
                    message = self.pending_messages[message_id]
                    message.status = MessageStatus.EXPIRED
                    await self._notify_message_failed(message_id, "Message expired")
                    self._remove_pending_message(message_id)

                if expired_messages:
                    logger.debug(f"Cleaned up {len(expired_messages)} expired messages")

            except Exception as e:
                
        
    async def _handle_ack_message(self, message_data: Dict[str, Any]):

        original_message_id = message_data.get("original_message_id")
        if original_message_id:
            await self.handle_acknowledgment(original_message_id, True)

    async def _handle_nack_message(self, message_data: Dict[str, Any]):

        original_message_id = message_data.get("original_message_id")
        error_message = message_data.get("error_message", "Remote processing failed")
        if original_message_id:
            await self.handle_acknowledgment(original_message_id, False, error_message)

    async def _send_acknowledgment(
            self,
            original_message: Dict[str, Any],
            success: bool,
            sender_info: Dict[str, Any] = None,
            error_message: Optional[str] = None,
    ):

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
            
    def _create_error_response(self, error_message: str) -> Dict[str, Any]:

        return {
            "message_type": "error",
            "message_id": str(uuid.uuid4()),
            "timestamp": time.time(),
            "error": error_message,
        }

    def _remove_pending_message(self, message_id: str):

        self.pending_messages.pop(message_id, None)
        self.message_callbacks.pop(message_id, None)

    async def _notify_message_acknowledged(self, message_id: str):

        callback = self.message_callbacks.get(message_id)
        if callback and callback.on_acknowledged:
            try:
                callback.on_acknowledged(message_id)
            except Exception as e:
                
    async def _notify_message_failed(self, message_id: str, error_message: str):

        callback = self.message_callbacks.get(message_id)
        if callback and callback.on_failed:
            try:
                callback.on_failed(message_id, error_message)
            except Exception as e:
                
    async def _notify_message_retrying(self, message_id: str, attempt: int):

        callback = self.message_callbacks.get(message_id)
        if callback and callback.on_retrying:
            try:
                callback.on_retrying(message_id, attempt)
            except Exception as e:
                
    def get_pending_message_count(self) -> int:

        return len(self.pending_messages)

    def get_queue_sizes(self) -> Dict[MessagePriority, int]:

        return {
            priority: len(queue) for priority, queue in self.priority_queues.items()
        }
