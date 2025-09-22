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
    val methodName: String
        get() = name

    /**
     * Get the tag
     */
    val methodTag: String  
        get() = tag

    /**
     * Get the parameters array
     */
    val methodParameters: Array<Parameter?>?
        get() = parameters

    /**
     * Get parameter types
     */
    val methodParameterTypes: Array<Class<*>?>?
        get() = parameterTypes

    class Parameter(var type: Class<*>, var value: Any?) {
        /**
         * Get the parameter type
         */
        val parameterType: Class<*>
            get() = type

        /**
         * Get the parameter value
         */
        val parameterValue: Any?
            get() = value
    }
    companion object {
        fun valueOf(method: Method): MethodInfo {
            val annotation = method.getAnnotation(Tag::class.java)
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
