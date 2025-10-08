// Merged ALL .kt and .java files from the 'BleModule\src\main\java\com\topdon\ble\callback' directory and its subdirectories.
// Total files: 10 | Generated on: 2025-10-08 01:42:33


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\MtuChangeCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface MtuChangeCallback extends RequestFailedCallback {

    void onMtuChanged (Request request, int mtu);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\NotificationChangeCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface NotificationChangeCallback extends RequestFailedCallback {

    void onNotificationChanged (Request request, boolean isEnabled);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\PhyChangeCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface PhyChangeCallback extends RequestFailedCallback {

    void onPhyChange (Request request, int txPhy, int rxPhy);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\ReadCharacteristicCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface ReadCharacteristicCallback extends RequestFailedCallback {

    void onCharacteristicRead (Request request, byte[] value);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\ReadDescriptorCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface ReadDescriptorCallback extends RequestFailedCallback {

    void onDescriptorRead (Request request, byte[] value);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\ReadRssiCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface ReadRssiCallback extends RequestFailedCallback {

    void onRssiRead (Request request, int rssi);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\RequestCallback.java =====

package com.topdon.ble.callback;

public interface RequestCallback {
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\RequestFailedCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface RequestFailedCallback extends RequestCallback {

    void onRequestFailed (Request request, int failType, Object value);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\ScanListener.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Device;

public interface ScanListener {

    int ERROR_LACK_LOCATION_PERMISSION = 0;

    int ERROR_LOCATION_SERVICE_CLOSED = 1;

    int ERROR_SCAN_FAILED = 2;

    void onScanStart();

    void onScanStop();

    @Deprecated
    default void onScanResult(Device device)
    {
    }

    void onScanResult(Device device, boolean isConnectedBySys);

    void onScanError(int errorCode, String errorMsg);
}


// ===== FROM: BleModule\src\main\java\com\topdon\ble\callback\WriteCharacteristicCallback.java =====

package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface WriteCharacteristicCallback extends RequestFailedCallback {

    void onCharacteristicWrite (Request request, byte[] value);
}