package com.mpdc4gsr.ble;

/**
 * date: 2019/8/5 16:10
 * author: bichuanfeng
 */
final class Inspector {

    static <T> T requireNonNull(T obj, String message) {
        if (obj == null)
            throw new EasyBLEException(message);
        return obj;
    }
}
