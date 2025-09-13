package com.topdon.lib.core.utils;

import android.content.Context;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

import com.topdon.lib.core.BaseApplication;

/**
 * des:
 * author: CaiSongL
 * date: 2024/5/23 17:39
 **/
public class EasyWifi {
    private static volatile EasyWifi mInstance;
    private WifiConnectCallback wifiConnectCallback;
    String TAG = "EasyWifi";
    private final WifiManager wifiManager = (WifiManager) BaseApplication.instance.getSystemService(Context.WIFI_SERVICE);
    private final ConnectivityManager connectivityManager = (ConnectivityManager) BaseApplication.instance.getSystemService(Context.CONNECTIVITY_SERVICE);

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

    public static EasyWifi getInstance() {
        if (mInstance == null) {
            synchronized (EasyWifi.class) {
                if (mInstance == null) {
                    mInstance = new EasyWifi();
                }
            }
        }
        return mInstance;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectByNew(str, str2, WiFiEncryptionStandard.WPA2);
        } else {
            // Fallback to old method for API < 29 - assume WPA encryption
            connectByOld(str, str2, WifiCapability.WIFI_CIPHER_WPA);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void connectByNew(String str, String str2, WiFiEncryptionStandard wiFiEncryptionStandard) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Fallback to old method for API < 29 - assume WPA encryption
            connectByOld(str, str2, WifiCapability.WIFI_CIPHER_WPA);
            return;
        }
        
        WifiNetworkSpecifier build = new WifiNetworkSpecifier.Builder().setSsid(str).setWpa2Passphrase(str2).build();
        if (wiFiEncryptionStandard == WiFiEncryptionStandard.WPA3) {
            build = new WifiNetworkSpecifier.Builder().setSsid(str).setWpa3Passphrase(str2).build();
        }
        this.connectivityManager.requestNetwork(new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(build).build(), new ConnectivityManager.NetworkCallback() { // from class: com.ir.networklib.EasyWifi.1
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onAvailable(Network network) {
                super.onAvailable(network);
                if (EasyWifi.this.wifiConnectCallback != null) {
                    EasyWifi.this.wifiConnectCallback.onSuccess(network);
                }
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onUnavailable() {
                super.onUnavailable();
                if (EasyWifi.this.wifiConnectCallback != null) {
                    EasyWifi.this.wifiConnectCallback.onFailure();
                }
            }
        });
    }

    public boolean connectByOld(String str, String str2, WifiCapability wifiCapability) {
        int addNetwork = this.wifiManager.addNetwork(createWifiConfig(str, str2, wifiCapability));
        if (addNetwork == -1) {
            Log.e(this.TAG, "操作failed,需要您到手机wifilist中Cancel对deviceconnection的saved");
        }
        boolean enableNetwork = this.wifiManager.enableNetwork(addNetwork, true);
        Log.d(this.TAG, "connectByOld: " + (enableNetwork ? "success" : "failed"));
        return enableNetwork;
    }

    private WifiConfiguration isExist(String str) {
        // Check for required permissions
        if (ActivityCompat.checkSelfPermission(BaseApplication.instance, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(BaseApplication.instance, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing WiFi permissions");
            return null;
        }
        
        try {
            for (WifiConfiguration wifiConfiguration : this.wifiManager.getConfiguredNetworks()) {
                if (wifiConfiguration.SSID.equals("\"" + str + "\"")) {
                    return wifiConfiguration;
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException accessing configured networks: " + e.getMessage());
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
        if (isExist != null) {
            Log.d(this.TAG, "createWifiConfig: 移除网路（true:success，false:failed），结果=" + this.wifiManager.removeNetwork(isExist.networkId) + "移除后saved" + this.wifiManager.saveConfiguration());
        }
        Log.d(this.TAG, "createWifiConfig: currentssid=" + str);
        if (wifiCapability == WifiCapability.WIFI_CIPHER_NO_PASS) {
            wifiConfiguration.allowedKeyManagement.set(0);
        } else if (wifiCapability == WifiCapability.WIFI_CIPHER_WEP) {
            wifiConfiguration.hiddenSSID = true;
            wifiConfiguration.wepKeys[0] = "\"" + str2 + "\"";
            wifiConfiguration.allowedAuthAlgorithms.set(0);
            wifiConfiguration.allowedAuthAlgorithms.set(1);
            wifiConfiguration.allowedKeyManagement.set(0);
            wifiConfiguration.wepTxKeyIndex = 0;
        } else if (wifiCapability == WifiCapability.WIFI_CIPHER_WPA) {
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

    public static boolean isNetConnected(ConnectivityManager connectivityManager) {
        return connectivityManager.getActiveNetwork() != null;
    }

    public static boolean isWifi(ConnectivityManager connectivityManager) {
        NetworkCapabilities networkCapabilities;
        if (connectivityManager.getActiveNetwork() != null && (networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork())) != null) {
            return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        return false;
    }

    public void setNetworkType(NetType netType) {
        Log.d(this.TAG, "selectNetworkType: 强制使用wifinetwork或者移动datanetwork");
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        if (netType == NetType.WIFI) {
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        }
        getConnectivityManager().requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() { // from class: com.ir.networklib.EasyWifi.2
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onAvailable(Network network) {
                try {
                    Log.d(EasyWifi.this.TAG, "settingsnetworktype时onAvailable: ");
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
}
