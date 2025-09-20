package com.mpdc4gsr.lib.core.comm.util

import android.app.Activity
import com.mpdc4gsr.lib.core.comm.navigation.NavigationManager


object ARouterUtil {

    fun jumpImagePick(
        activity: Activity,
        // isTC007 parameter removed - TC007 functionality disabled
        imgPath: String,
    ) {
        NavigationManager.jumpImagePick(activity, imgPath)
    }
}
