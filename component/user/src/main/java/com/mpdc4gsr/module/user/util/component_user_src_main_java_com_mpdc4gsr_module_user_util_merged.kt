// Merged ALL .kt and .java files from the 'component\user\src\main\java\com\mpdc4gsr\module\user\util' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:36


// ===== FROM: component\user\src\main\java\com\mpdc4gsr\module\user\util\ActivityUtils.java =====

package com.mpdc4gsr.module.user.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.mpdc4gsr.libunified.app.lms.utils.NetworkUtils;
import com.mpdc4gsr.libunified.app.lms.weiget.TToast;
import com.mpdc4gsr.module.user.R;

public class ActivityUtils {

    public static void goSystemCustomer(Context mContext)
    {
        Log.w("bcf", "[TEXT]Event");
        String url = "https://www.topdon.cc/tc-chat";
        goSystemBrowser(mContext, url);
    }

    public static void goSystemBrowser(Context mContext, String url)
    {
        Log.w("bcf", "goSystemBrowser");
        if (!NetworkUtils.isConnected(mContext)) {
            TToast.shortToast(mContext, R.string.lms_setting_http_error);
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            Uri uri = Uri . parse (url);
            intent.setData(uri);
            mContext.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}