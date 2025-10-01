package com.topdon.ble;

/**
 * 
 * <p>
 * date: 2019/8/9 22:10
 * author: bichuanfeng
 */
public enum RequestType {
    /**
     * 
     */
    SET_NOTIFICATION,
    /**
     * Indication
     */
    SET_INDICATION,
    /**
     * 
     */
    READ_CHARACTERISTIC,
    /**
     * 
     */
    READ_DESCRIPTOR,
    /**
     * 
     */
    READ_RSSI,
    /**
     * 
     */
    WRITE_CHARACTERISTIC,
    /**
     * 
     */
    CHANGE_MTU,
    /**
     * 
     */
    READ_PHY,
    /**
     * 
     */
    SET_PREFERRED_PHY
}
