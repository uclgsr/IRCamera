package com.mpdc4gsr.module.thermalunified.repository

import com.google.gson.Gson
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.module.thermalunified.bean.DataBean
import com.mpdc4gsr.module.thermalunified.bean.ModelBean

object ConfigRepository {
    fun read(isTC007: Boolean): ModelBean =
        try {
            // TC007 functionality removed
            Gson().fromJson(
                if (isTC007) "{}" else SharedManager.getIRConfig(), // Use empty JSON for TC007
                ModelBean::class.java
            )
        } catch (_: Exception) {
            ModelBean(DataBean(id = 0, use = true))
        }

    fun update(
        isTC007: Boolean,
        bean: ModelBean,
    ) {
        if (isTC007) {
            // TC007 functionality removed - do nothing
        } else {
            SharedManager.setIRConfig(Gson().toJson(bean))
        }
    }

    fun readConfig(isTC007: Boolean): DataBean {
        val config = read(isTC007)
        if (config.defaultModel.use) {
            return config.defaultModel
        }
        config.myselfModel.forEach {
            if (it.use) {
                return it
            }
        }
        return config.defaultModel
    }
}
