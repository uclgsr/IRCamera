try:
    from ..core.timesync import TimeSyncService, TimeSyncStats, TimeSyncProtocol
except ImportError:

    TimeSyncService = None
    TimeSyncStats = None
    TimeSyncProtocol = None

from .timesync_service import AdvancedTimeSyncService


class AdvancedTimeSyncServer:

    def __init__(self, port: int = 1234):

        self.port = port
        self.sync_service = TimeSyncService()
        self._running = False

    async def start(self) -> bool:

        try:
            success = await self.sync_service.start_server(self.port)
            if success:
                self._running = True
                return True
            return False
        except Exception as e:
                                    return False

    async def stop(self):

        if self._running:
            await self.sync_service.stop()
            self._running = False

    def get_stats(self) -> dict:

        return self.sync_service.get_stats()

    def is_running(self) -> bool:

        return self._running


__all__ = ['AdvancedTimeSyncServer', 'AdvancedTimeSyncService', 'TimeSyncService', 'TimeSyncStats', 'TimeSyncProtocol']
