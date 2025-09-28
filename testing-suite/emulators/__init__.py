"""
Software Sensor Emulators for IRCamera Testing Suite

This package provides realistic software emulation of hardware sensors
for comprehensive testing without requiring physical devices.

Emulators generate behaviorally-accurate data patterns that test actual
system capabilities rather than using simple statistical sampling.
"""

from .tc001_emulator import TC001ThermalEmulator
from .shimmer3_emulator import Shimmer3GSREmulator
from .network_emulator import NetworkEmulator
from .android_emulator import AndroidSystemEmulator

__all__ = [
    'TC001ThermalEmulator',
    'Shimmer3GSREmulator', 
    'NetworkEmulator',
    'AndroidSystemEmulator'
]