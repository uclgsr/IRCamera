package com.topdon.module.thermal.ir.repository

import com.google.gson.Gson
import com.topdon.lib.core.common.SharedManager
import com.topdon.module.thermal.ir.bean.DataBean
import com.topdon.module.thermal.ir.bean.ModelBean
import java.lang.Exception

/**
 * Config repository utility class for thermal imaging operations.
 * Provides helper functions and common functionality.
 */
object ConfigRepository {
    fun read(isTC007: Boolean): ModelBean =
        try {
            Gson().fromJson(if (isTC007) SharedManager.irConfigJsonTC007 else SharedManager.getIRConfig(), ModelBean::class.java)
        } catch (_: Exception) {
当SP里没data必定抛exception，所以这里Return一个默认的
            ModelBean(DataBean(id = 0, use = true))
        }

    fun update(
        isTC007: Boolean,
        bean: ModelBean,
    ) {
        if (isTC007) {
            SharedManager.irConfigJsonTC007 = Gson().toJson(bean)
        } else {
            SharedManager.setIRConfig(Gson().toJson(bean))
        }
    }

    /**
读取selected的configurationinfo
     */
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
