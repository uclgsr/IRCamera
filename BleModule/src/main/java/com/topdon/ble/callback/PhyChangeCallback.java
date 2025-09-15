package com.topdon.ble.callback;

import com.topdon.ble.Request;

public interface PhyChangeCallback extends RequestFailedCallback {

    void onPhyChange(Request request, int txPhy, int rxPhy);
}
