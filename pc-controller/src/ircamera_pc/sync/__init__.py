"""Time Synchronization & Session Management - PC Controller Side"""
try:
    from ..core.timesync import TimeSyncService, TimeSyncStats, TimeSyncProtocol
except ImportError:
    # Fallback for missing core components
    TimeSyncService = None
    TimeSyncStats = None
    TimeSyncProtocol = None

from .timesync_service import AdvancedTimeSyncService


class AdvancedTimeSyncServer:
    """
    Advanced Time Synchronization Server for the PC Controller Hub.
    
    This class provides advanced time synchronization capabilities built on top of
    the base TimeSyncService, with additional features for the Hub-and-Spoke architecture.
    """

    def __init__(self, port: int = 1234):
        """
        Initialize the Advanced Time Sync Server.
        
        Args:
            port: UDP port for time sync service
        """
        self.port = port
        self.sync_service = TimeSyncService()
        self._running = False

    async def start(self) -> bool:
        """
        Start the advanced time synchronization server.
        
        Returns:
            True if started successfully
        """
        try:
            success = await self.sync_service.start_server(self.port)
            if success:
                self._running = True
                return True
            return False
        except Exception as e:
            from loguru import logger
            logger.error(f"Failed to start advanced time sync server: {e}")
            return False

    async def stop(self):
        """Stop the advanced time synchronization server."""
        if self._running:
            await self.sync_service.stop()
            self._running = False

    def get_stats(self) -> dict:
        """
        Get time synchronization statistics.
        
        Returns:
            Dictionary of sync stats by device
        """
        return self.sync_service.get_stats()

    def is_running(self) -> bool:
        """Check if the server is running."""
        return self._running


__all__ = ['AdvancedTimeSyncServer', 'AdvancedTimeSyncService', 'TimeSyncService', 'TimeSyncStats', 'TimeSyncProtocol']
