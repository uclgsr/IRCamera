package com.mpdc4gsr.commons.observer

import com.mpdc4gsr.commons.poster.MethodInfo
import com.mpdc4gsr.commons.poster.Tag
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

internal class ObserverMethodHelper(private val isObserveAnnotationRequired: Boolean) {
    fun clearCache() {
        METHOD_CACHE.clear()
    }

    fun generateRunnable(observer: Observer?, method: Method, info: MethodInfo): Runnable {
        val parameters = info.parameters
        if (parameters == null || parameters.isEmpty()) {
            return Runnable {
                try {
                    method.invoke(observer)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
            }
        } else {
            val params = arrayOfNulls<Any>(parameters.size)
            for (i in parameters.indices) {
                val parameter = parameters[i]
                params[i] = parameter?.value
            }
            return Runnable {
                try {
                    method.invoke(observer, *params)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun generateKey(tag: String, name: String?, paramTypes: Array<Class<*>?>?): String {
        val sb = StringBuilder()
        if (tag.isEmpty()) {
            sb.append(name)
        } else {
            sb.append(tag)
        }
        if (paramTypes != null) {
            for (type in paramTypes) {
                sb.append(",").append(type)
            }
        }
        return sb.toString()
    }

    fun findObserverMethod(observer: Observer): MutableMap<String?, Method?> {
        var map: MutableMap<String?, Method?>? = METHOD_CACHE.get(observer.javaClass)
        if (map != null) {
            return map
        }
        map = HashMap<String?, Method?>()
        val methods: MutableList<Method> = ArrayList<Method>()
        var cls: Class<*>? = observer.javaClass
        while (cls != null && !cls.isInterface() && Observer::class.java.isAssignableFrom(cls)) {
            var ms: Array<Method>? = null
            try {
                ms = cls.declaredMethods
            } catch (ignore: Throwable) {
            }
            if (ms != null) {
                for (m in ms) {
                    val ignore = Modifier.ABSTRACT or Modifier.STATIC or 0x40 or 0x1000
                    if ((m.modifiers and Modifier.PUBLIC) != 0 && (m.modifiers and ignore) == 0 && !contains(
                            methods,
                            m
                        )
                    ) {
                        methods.add(m)
                    }
                }
            }
            cls = cls.superclass
        }
        for (method in methods) {
            val anno = method.getAnnotation<Observe?>(Observe::class.java)
            if (anno != null || !isObserveAnnotationRequired) {
                val tagAnno = method.getAnnotation<Tag?>(Tag::class.java)
                val tag = if (tagAnno == null) "" else tagAnno.value
                val key = generateKey(tag, method.name, method.parameterTypes)
                map.put(key, method)
            }
        }
        if (!map.isEmpty()) {
            METHOD_CACHE.put(observer.javaClass, map)
        }
        return map
    }

    companion object {
        private val METHOD_CACHE: MutableMap<Class<*>?, MutableMap<String?, Method?>?> =
            ConcurrentHashMap<Class<*>?, MutableMap<String?, Method?>?>()

        private fun contains(methods: MutableList<Method>, method: Method): Boolean {
            for (m in methods) {
                if (m.name == method.name && m.returnType == method.returnType &&
                    equalParamTypes(m.parameterTypes, method.parameterTypes)
                ) {
                    return true
                }
            }
            return false
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
    }
}
