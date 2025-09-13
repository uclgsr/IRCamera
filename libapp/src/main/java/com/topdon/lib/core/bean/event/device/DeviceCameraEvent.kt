package com.topdon.lib.core.bean.event.device

/**
 * @param action
 * 100:initialize
 * 101:有image
 */
@Deprecated("只有Listener没有Send，一个没有用处的 Event")
data class DeviceCameraEvent(val action: Int)
