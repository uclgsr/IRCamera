"""
Tests for IRCamera PC Controller

Basic test suite for core functionality validation.
"""

import shutil
import tempfile
from datetime import datetime
from unittest.mock import Mock, patch

import pytest

from ..core.config import ConfigManager
from ..core.session import SessionManager, SessionState
from ..core.timesync import TimeSyncService
from ..network.server import DeviceInfo, NetworkServer


class TestConfigManager:
    """Tests for ConfigManager."""

    def test_config_loading_with_defaults(self):
        """Test config loading with default values."""
        # Create config manager with non-existent file
        config_manager = ConfigManager("/nonexistent/config.yaml")

        # Should fall back to defaults
        assert config_manager.get("network.server_port") == 8080
        assert config_manager.get("gsr.default_mode") == "local"

    def test_config_get_set(self):
        """Test config get/set operations."""
        config_manager = ConfigManager("/nonexistent/config.yaml")

        # Test dot notation access
        assert config_manager.get("network.server_port") == 8080

        # Test setting values
        config_manager.set("test.value", "test")
        assert config_manager.get("test.value") == "test"

        # Test default values
        assert config_manager.get("nonexistent.key", "default") == "default"


class TestSessionManager:
    """Tests for SessionManager."""

    def setup_method(self):
        """Set up test environment."""
        self.temp_dir = tempfile.mkdtemp()

        # Mock config to use temp directory
        with patch("ircamera_pc.core.session.config") as mock_config:
            mock_config.get.return_value = self.temp_dir
            self.session_manager = SessionManager()

    def teardown_method(self):
        """Clean up test environment."""
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_create_session(self):
        """Test session creation."""
        session = self.session_manager.create_session("test_session")

        assert session.name == "test_session"
        assert session.state == SessionState.IDLE.value
        assert session.session_id is not None
        assert session.created_at is not None

    def test_session_lifecycle(self):
        """Test complete session lifecycle."""
        # Create session
        session = self.session_manager.create_session("test_session")
        assert session.state == SessionState.IDLE.value

        # Start session
        self.session_manager.start_session()
        current = self.session_manager.get_current_session()
        assert current.state == SessionState.ACTIVE.value
        assert current.started_at is not None

        # Begin recording
        self.session_manager.begin_recording()
        current = self.session_manager.get_current_session()
        assert current.state == SessionState.RECORDING.value

        # End session
        ended_session = self.session_manager.end_session()
        assert ended_session.state == SessionState.COMPLETED.value
        assert ended_session.ended_at is not None
        assert ended_session.duration_seconds is not None

    def test_session_metadata_persistence(self):
        """Test session metadata persistence."""
        # Create session
        session = self.session_manager.create_session("test_session")
        session_id = session.session_id

        # Add some data
        self.session_manager.add_device(
            {"device_id": "test_device", "device_type": "android"}
        )

        self.session_manager.add_sync_event("test_event", {"data": "test"})

        # Load session from another manager instance
        with patch("ircamera_pc.core.session.config") as mock_config:
            mock_config.get.return_value = self.temp_dir
            new_manager = SessionManager()
            loaded_session = new_manager.load_session(session_id)

        assert loaded_session is not None
        assert loaded_session.name == "test_session"
        assert len(loaded_session.devices) == 1
        assert len(loaded_session.sync_events) == 1

    def test_single_session_constraint(self):
        """Test that only one session can be active at a time."""
        # Create first session
        self.session_manager.create_session("session1")
        self.session_manager.start_session()

        # Try to create second session while first is active
        with pytest.raises(ValueError, match="another session is active"):
            self.session_manager.create_session("session2")


class TestTimeSyncService:
    """Tests for TimeSyncService."""

    def setup_method(self):
        """Set up test environment."""
        self.time_sync_service = TimeSyncService()

    @pytest.mark.asyncio
    async def test_service_lifecycle(self):
        """Test time sync service start/stop."""
        assert not self.time_sync_service.is_running

        # Start service
        await self.time_sync_service.start(
            host="localhost", port=0
        )  # Use any available port
        assert self.time_sync_service.is_running

        # Stop service
        await self.time_sync_service.stop()
        assert not self.time_sync_service.is_running

    def test_sync_stats_tracking(self):
        """Test synchronization statistics tracking."""
        service = self.time_sync_service

        # Initially no stats
        assert len(service.get_all_stats()) == 0

        # Simulate sync request
        request_data = (
            b"\x00\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f"
        )
        service.handle_sync_request("test_device", request_data, ("127.0.0.1", 12345))

        # Should have stats now
        stats = service.get_device_stats("test_device")
        assert stats is not None
        assert stats.device_id == "test_device"
        assert stats.sync_count == 1

    def test_synchronization_quality(self):
        """Test synchronization quality metrics."""
        service = self.time_sync_service

        # Initially no devices
        quality = service.get_synchronization_quality()
        assert quality["total_devices"] == 0
        assert quality["synchronized_devices"] == 0
        assert quality["synchronization_rate"] == 0.0

        # Add some mock stats
        from datetime import timezone

        from ..core.timesync import TimeSyncStats

        stats = TimeSyncStats("test_device")
        stats.recent_offsets = [1.0, 2.0, 3.0, 4.0, 5.0]
        stats.last_sync = datetime.now(timezone.utc)
        service._device_stats["test_device"] = stats

        quality = service.get_synchronization_quality()
        assert quality["total_devices"] == 1
        assert quality["overall_median_offset_ms"] == 3.0


class TestNetworkServer:
    """Tests for NetworkServer."""

    def setup_method(self):
        """Set up test environment."""
        self.network_server = NetworkServer()

    @pytest.mark.asyncio
    async def test_server_lifecycle(self):
        """Test network server start/stop."""
        assert not self.network_server.is_running

        # Start server on available port
        await self.network_server.start()
        assert self.network_server.is_running

        # Stop server
        await self.network_server.stop()
        assert not self.network_server.is_running

    def test_device_info_creation(self):
        """Test DeviceInfo creation and serialization."""
        device_info = DeviceInfo(
            device_id="test_device",
            device_type="android",
            capabilities=["camera", "gsr"],
            ip_address="192.168.1.100",
            port=8080,
            is_gsr_leader=True,
        )

        assert device_info.device_id == "test_device"
        assert device_info.is_gsr_leader is True

        # Test serialization
        device_dict = device_info.to_dict()
        assert isinstance(device_dict, dict)
        assert device_dict["device_id"] == "test_device"

    def test_message_handling(self):
        """Test message handling logic."""
        server = self.network_server

        # Mock writer
        mock_writer = Mock()
        mock_writer.get_extra_info.return_value = ("127.0.0.1", 12345)

        # This would be tested with async context in full integration test
        assert "device_register" in server._message_handlers


if __name__ == "__main__":
    pytest.main([__file__])
