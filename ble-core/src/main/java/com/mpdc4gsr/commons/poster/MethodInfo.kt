package com.mpdc4gsr.commons.poster

import java.lang.reflect.Method

class MethodInfo(var name: String, var tag: String, vararg parameters: Parameter?) {
    var parameters: Array<Parameter?>?

    constructor(name: String, vararg parameters: Parameter?) : this(name, name, *parameters)

    init {
        this.parameters = arrayOf(*parameters)
    }

    constructor(name: String, parameterTypes: Array<Class<*>?>?) : this(name, name, parameterTypes)

    constructor(name: String, tag: String, parameterTypes: Array<Class<*>?>?) : this(
        name,
        tag,
        *toParameters(parameterTypes)!!
    )

    val parameterTypes: Array<Class<*>?>?
        get() {
            val params = parameters
            if (params == null) {
                return null
            } else {
                val types = arrayOfNulls<Class<*>>(params.size)
                for (i in params.indices) {
                    types[i] = params[i]!!.type
                }
                return types
            }
        }

    val parameterValues: Array<Any?>?
        get() {
            val params = parameters
            if (params == null) {
                return null
            } else {
                val values: Array<Any?> = arrayOfNulls<Any>(params.size)
                for (i in params.indices) {
                    values[i] = params[i]!!.value
                }
                return values
            }
        }

    /**
     * Get the method name
     */
    fun getName(): String {
        return name
    }

    /**
     * Get the tag
     */
    fun getTag(): String {
        return tag
    }

    /**
     * Get the parameters array
     */
    fun getParameters(): Array<Parameter?>? {
        return parameters
    }

    /**
     * Get parameter types
     */
    fun getParameterTypes(): Array<Class<*>?>? {
        return parameterTypes
    }

    class Parameter(var type: Class<*>, var value: Any?) {
        /**
         * Get the parameter type
         */
        fun getType(): Class<*> {
            return type
        }

        /**
         * Get the parameter value
         */
        fun getValue(): Any? {
            return value
        }
    }
    companion object {
        fun valueOf(method: Method): MethodInfo {
            val annotation = method.getAnnotation<Tag?>(Tag::class.java)
            return MethodInfo(
                method.name, if (annotation == null) method.name else annotation.value,
                method.parameterTypes
            )
        }

        private fun toParameters(parameterTypes: Array<Class<*>?>?): Array<Parameter?>? {
            var parameters: Array<Parameter?>? = null
            if (parameterTypes != null) {
                parameters = arrayOfNulls<Parameter>(parameterTypes.size)
                for (i in parameterTypes.indices) {
                    parameters[i] = MethodInfo.Parameter(parameterTypes[i]!!, null)
                }
            }
            return parameters
        }
    }
}
