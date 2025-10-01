package com.topdon.ble;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

/**
 * date: 2019/8/11 15:34
 * author: bichuanfeng
 */
public interface Request {
    /**
     *
     */
    @NonNull
    Device getDevice();

    /**
     *
     */
    @NonNull
    RequestType getType();

    /**
     *
     */
    @Nullable
    String getTag();

    /**
     * UUID
     */
    @Nullable
    UUID getService();

    /**
     * UUID
     */
    @Nullable
    UUID getCharacteristic();

    /**
     * UUID
     */
    @Nullable
    UUID getDescriptor();

    /**
     *
     *
     * @param connection
     */
    void execute(Connection connection);
}
