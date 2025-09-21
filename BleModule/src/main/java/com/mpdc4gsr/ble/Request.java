package com.mpdc4gsr.ble;




import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

/**
 * date: 2019/8/11 15:34
 * author: bichuanfeng
 */
public interface Request {

    @NonNull
    Device getDevice();


    @NonNull
    RequestType getType();


    @Nullable
    String getTag();


    @Nullable
    UUID getService();


    @Nullable
    UUID getCharacteristic();


    @Nullable
    UUID getDescriptor();


    void execute(Connection connection);
}
