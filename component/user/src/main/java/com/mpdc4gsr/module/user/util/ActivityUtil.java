package com.mpdc4gsr.module.user.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.mpdc4gsr.libunified.app.lms.utils.NetworkUtil;
import com.mpdc4gsr.libunified.app.lms.weiget.TToast;
import com.mpdc4gsr.module.user.R;

public class ActivityUtil {

    public static void goSystemCustomer(Context mContext) {
        Log.w("bcf", "[TEXT]Event");
        String url = "https://www.topdon.cc/tc-chat";
        goSystemBrowser(mContext, url);
    }


    public static void goSystemBrowser(Context mContext, String url) {
        Log.w("bcf", "goSystemBrowser");
        if (!NetworkUtil.isConnected(mContext)) {
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
