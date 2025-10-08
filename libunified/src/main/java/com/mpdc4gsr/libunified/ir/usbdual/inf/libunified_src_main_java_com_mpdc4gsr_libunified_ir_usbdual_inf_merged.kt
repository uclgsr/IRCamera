// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\inf' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ir\usbdual\inf\OnUSBConnectListener.java =====

package com.mpdc4gsr.libunified.ir.usbdual.inf;

import android.hardware.usb.UsbDevice;

import com.energy.iruvc.ircmd.IRCMD;
import com.energy.iruvc.usb.USBMonitor;

public interface OnUSBConnectListener {

    void onAttach(UsbDevice device);

    void onGranted(UsbDevice usbDevice, boolean granted);

    void onDettach(UsbDevice device);

    void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew);

    void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock);

    void onCancel(UsbDevice device);

    void onIRCMDInit(IRCMD ircmd);

    void onCompleteInit();

    void onSetPreviewSizeFail();

}