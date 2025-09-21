package com.mpdc4gsr.ble.core

import java.util.UUID

interface Request {
    val device: Device

    val type: RequestType

    val tag: String?

    val service: UUID?

    val characteristic: UUID?

    val descriptor: UUID?

    fun execute(connection: Connection?)
}
