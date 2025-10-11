package com.mpdc4gsr.module.user.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.mpdc4gsr.component.shared.app.lms.utils.NetworkUtils;
import com.mpdc4gsr.component.shared.app.lms.weiget.TToast;
import com.mpdc4gsr.component.thermal.R;

public class ActivityUtils {

    public static void goSystemCustomer(Context mContext) {
        String url = "https://www.topdon.cc/tc-chat";
        goSystemBrowser(mContext, url);
    }

    public static void goSystemBrowser(Context mContext, String url) {
        if (!NetworkUtils.isConnected(mContext)) {
            TToast.shortToast(mContext, R.string.lms_setting_http_error);
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(url);
            intent.setData(uri);
            mContext.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



