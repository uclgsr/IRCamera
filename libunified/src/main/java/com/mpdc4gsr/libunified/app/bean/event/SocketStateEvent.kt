package com.mpdc4gsr.libunified.app.bean.event

data class SocketStateEvent(val isConnect: Boolean, val isTS004: Boolean = false) // TS004 functionality removed
