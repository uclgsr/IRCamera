package com.mpdc4gsr.lib.core.comm.util

import android.app.Activity
import com.topdon.libcom.navigation.NavigationManager


object ARouterUtil {

    fun jumpImagePick(
        activity: Activity,
        isTC007: Boolean,
        imgPath: String,
    ) {
        NavigationManager.jumpImagePick(activity, isTC007, imgPath)
    }
}
