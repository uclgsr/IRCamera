package com.topdon.ble;

/**
 *
 * <p>
 * date: 2019/8/12 14:26
 * author: bichuanfeng
 */
public enum ConnectionState {
    /**
     *
     */
    DISCONNECTED,
    /**
     *
     */
    CONNECTING,
    /**
     *
     */
    SCANNING_FOR_RECONNECTION,
    /**
     * ，
     */
    CONNECTED,
    /**
     * ，
     */
    SERVICE_DISCOVERING,
    /**
     * ，
     */
    SERVICE_DISCOVERED,
    /**
     *
     */
    RELEASED,
    /**
     *
     */
    TIMEOUT,
    /**
     * MTUs
     */
    MTU_SUCCESS
}
