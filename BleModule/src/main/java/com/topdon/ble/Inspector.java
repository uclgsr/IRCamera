package com.topdon.ble;

/**
 * date: 2019/8/5 16:10
 * author: bichuanfeng
 */
final class Inspector {
    /**
     * EasyBLEException
     *
     * @param obj     
     * @param message 
     */
    static <T> T requireNonNull(T obj, String message) {
        if (obj == null)
            throw new EasyBLEException(message);
        return obj;
    }
}
