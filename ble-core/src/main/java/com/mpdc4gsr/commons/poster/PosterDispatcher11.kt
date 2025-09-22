package com.mpdc4gsr.commons.poster

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ExecutorService

class PosterDispatcher(val executorService: ExecutorService, val defaultMode: ThreadMode) {
    private val backgroundPoster: Poster
    private val mainThreadPoster: Poster
    private val asyncPoster: Poster

    init {
        backgroundPoster = BackgroundPoster(executorService)
        mainThreadPoster = MainThreadPoster()
        asyncPoster = AsyncPoster(executorService)
    }

    fun clearTasks() {
        backgroundPoster.clear()
        mainThreadPoster.clear()
        asyncPoster.clear()
    }

    fun post(method: Method?, runnable: Runnable) {
        if (method != null) {
            val annotation = method.getAnnotation<RunOn?>(RunOn::class.java)
            var mode = defaultMode
            if (annotation != null) {
                mode = annotation.value
            }
            post(mode, runnable)
        }
    }

    fun post(mode: ThreadMode, runnable: Runnable) {
        var mode = mode
        if (mode == ThreadMode.UNSPECIFIED) {
            mode = defaultMode
        }
        when (mode) {
            ThreadMode.MAIN -> mainThreadPoster.enqueue(runnable)
            ThreadMode.POSTING -> runnable.run()
            ThreadMode.BACKGROUND -> backgroundPoster.enqueue(runnable)
            ThreadMode.ASYNC -> asyncPoster.enqueue(runnable)
            ThreadMode.UNSPECIFIED -> mainThreadPoster.enqueue(runnable) // Default to main thread
        }
    }

    fun post(
        owner: Any, methodName: String, tag: String,
        vararg parameters: MethodInfo.Parameter?
    ) {
        var classes = arrayOfNulls<Class<*>>(0)
        var params = arrayOfNulls<Any>(0)
        if (parameters != null) {
            params = arrayOfNulls<Any>(parameters.size)
            classes = arrayOfNulls<Class<*>>(parameters.size)
            for (i in parameters.indices) {
                val parameter: MethodInfo.Parameter = parameters[i]!!
                classes[i] = parameter.getType()
                params[i] = parameter.getValue()
            }
        }
        val methods = owner.javaClass.getDeclaredMethods()
        var tm: Method? = null
        var mm: Method? = null
        for (method in methods) {
            val annotation = method.getAnnotation<Tag?>(Tag::class.java)
            if (annotation != null && !annotation.value.isEmpty() && annotation.value == tag &&
                equalParamTypes(method.getParameterTypes(), classes)
            ) {
                tm = method
            }
            if (tm == null) {
                if (method.getName() == methodName && equalParamTypes(method.getParameterTypes(), classes)) {
                    mm = method
                }
            } else {
                break
            }
        }
        val method = if (tm == null) mm else tm
        if (method == null) {
            return
        }
        try {
            val finalParams = params
            post(method, Runnable {
                try {
                    method.invoke(owner, *finalParams)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
            })
        } catch (ignore: Exception) {
        }
    }

    private fun equalParamTypes(params1: Array<Class<*>?>, params2: Array<Class<*>?>): Boolean {
        if (params1.size == params2.size) {
            for (i in params1.indices) {
                if (params1[i] != params2[i]) return false
            }
            return true
        }
        return false
    }

    fun post(owner: Any, methodName: String, vararg parameters: MethodInfo.Parameter?) {
        post(owner, methodName, "", *parameters)
    }

    fun post(owner: Any, methodInfo: MethodInfo) {
        post(owner, methodInfo.getName(), methodInfo.getTag(), *methodInfo.getParameters()!!)
    }
}
