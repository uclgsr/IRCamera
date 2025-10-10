package com.mpdc4gsr.libunified.app.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;

import androidx.core.content.ContextCompat;

import com.mpdc4gsr.libunified.app.BaseApplication;

public class EasyWifi {
    private static volatile EasyWifi mInstance;
    private final WifiManager wifiManager = (WifiManager) BaseApplication.instance.getSystemService(Context.WIFI_SERVICE);
    private final ConnectivityManager connectivityManager = (ConnectivityManager) BaseApplication.instance.getSystemService(Context.CONNECTIVITY_SERVICE);
    String TAG = "EasyWifi";
    private WifiConnectCallback wifiConnectCallback;

    public static EasyWifi getInstance() {
        if (null == EasyWifi.mInstance) {
            synchronized (EasyWifi.class) {
                if (null == EasyWifi.mInstance) {
                    mInstance = new EasyWifi();
                }
            }
        }
        return mInstance;
    }

    public static boolean isNetConnected(ConnectivityManager connectivityManager) {
        return null != connectivityManager.getActiveNetwork();
    }

    public static boolean isWifi(ConnectivityManager connectivityManager) {
        NetworkCapabilities networkCapabilities;
        if (null != connectivityManager.getActiveNetwork() && null != (networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork()))) {
            return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        return false;
    }

    public void useWifiFirst() {
        this.connectivityManager.setNetworkPreference(1);
    }

    public void setWifiConnectCallback(WifiConnectCallback wifiConnectCallback) {
        this.wifiConnectCallback = wifiConnectCallback;
    }

    public boolean isWifiEnabled() {
        return this.wifiManager.isWifiEnabled();
    }

    public WifiManager getWifiManager() {
        return this.wifiManager;
    }

    public ConnectivityManager getConnectivityManager() {
        return this.connectivityManager;
    }

    public void connectByNew(String str, String str2) {
        connectByNew(str, str2, WiFiEncryptionStandard.WPA2);
    }

    public void connectByNew(String str, String str2, WiFiEncryptionStandard wiFiEncryptionStandard) {
        WifiNetworkSpecifier build = new WifiNetworkSpecifier.Builder().setSsid(str).setWpa2Passphrase(str2).build();
        if (WiFiEncryptionStandard.WPA3 == wiFiEncryptionStandard) {
            build = new WifiNetworkSpecifier.Builder().setSsid(str).setWpa3Passphrase(str2).build();
        }
        this.connectivityManager.requestNetwork(new NetworkRequest.Builder().addTransportType(1).addCapability(13).addCapability(14).setNetworkSpecifier(build).build(), new ConnectivityManager.NetworkCallback() { // from class: com.ir.networklib.EasyWifi.1
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onAvailable(Network network) {
                super.onAvailable(network);
                if (null != wifiConnectCallback) {
                    EasyWifi.this.wifiConnectCallback.onSuccess(network);
                }
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onUnavailable() {
                super.onUnavailable();
                if (null != wifiConnectCallback) {
                    EasyWifi.this.wifiConnectCallback.onFailure();
                }
            }
        });
    }

    public boolean connectByOld(String str, String str2, WifiCapability wifiCapability) {
        int addNetwork = this.wifiManager.addNetwork(createWifiConfig(str, str2, wifiCapability));
        if (-1 == addNetwork) {
        }
        boolean enableNetwork = this.wifiManager.enableNetwork(addNetwork, true);
        return enableNetwork;
    }

    private WifiConfiguration isExist(String str) {
        if (ContextCompat.checkSelfPermission(BaseApplication.instance, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        for (WifiConfiguration wifiConfiguration : this.wifiManager.getConfiguredNetworks()) {
            if (wifiConfiguration.SSID.equals("\"" + str + "\"")) {
                return wifiConfiguration;
            }
        }
        return null;
    }

    private WifiConfiguration createWifiConfig(String str, String str2, WifiCapability wifiCapability) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.allowedAuthAlgorithms.clear();
        wifiConfiguration.allowedGroupCiphers.clear();
        wifiConfiguration.allowedKeyManagement.clear();
        wifiConfiguration.allowedPairwiseCiphers.clear();
        wifiConfiguration.allowedProtocols.clear();
        wifiConfiguration.SSID = "\"" + str + "\"";
        WifiConfiguration isExist = isExist(str);
        if (null != isExist) {
        }
        if (WifiCapability.WIFI_CIPHER_NO_PASS == wifiCapability) {
            wifiConfiguration.allowedKeyManagement.set(0);
        } else if (WifiCapability.WIFI_CIPHER_WEP == wifiCapability) {
            wifiConfiguration.hiddenSSID = true;
            wifiConfiguration.wepKeys[0] = "\"" + str2 + "\"";
            wifiConfiguration.allowedAuthAlgorithms.set(0);
            wifiConfiguration.allowedAuthAlgorithms.set(1);
            wifiConfiguration.allowedKeyManagement.set(0);
            wifiConfiguration.wepTxKeyIndex = 0;
        } else if (WifiCapability.WIFI_CIPHER_WPA == wifiCapability) {
            wifiConfiguration.preSharedKey = "\"" + str2 + "\"";
            wifiConfiguration.hiddenSSID = true;
            wifiConfiguration.allowedAuthAlgorithms.set(0);
            wifiConfiguration.allowedGroupCiphers.set(2);
            wifiConfiguration.allowedKeyManagement.set(1);
            wifiConfiguration.allowedPairwiseCiphers.set(1);
            wifiConfiguration.allowedGroupCiphers.set(3);
            wifiConfiguration.allowedPairwiseCiphers.set(2);
            wifiConfiguration.status = 2;
            wifiConfiguration.priority = 100000;
        }
        return wifiConfiguration;
    }

    public void setNetworkType(NetType netType) {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        if (NetType.WIFI == netType) {
            builder.addTransportType(1);
        } else {
            builder.addTransportType(0);
        }
        connectivityManager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() { // from class: com.ir.networklib.EasyWifi.2
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onAvailable(Network network) {
                try {
                    EasyWifi.this.getConnectivityManager().bindProcessToNetwork(network);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public String getConnectSSID() {
        return this.wifiManager.getConnectionInfo().getSSID();
    }

    /* loaded from: classes2.dex */
    public enum WiFiEncryptionStandard {
        WEP,
        WPA_EAP,
        WPA_PSK,
        WPA2,
        WPA3
    }

    /* loaded from: classes2.dex */
    public enum WifiCapability {
        WIFI_CIPHER_WEP,
        WIFI_CIPHER_WPA,
        WIFI_CIPHER_NO_PASS
    }

    /* loaded from: classes2.dex */
    public interface WifiConnectCallback {
        void onFailure();

        void onSuccess(Network network);
    }
}
