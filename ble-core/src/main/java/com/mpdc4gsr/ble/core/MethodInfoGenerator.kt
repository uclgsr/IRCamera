package com.mpdc4gsr.ble.core

import java.lang.reflect.Method

/**
 * Utility class for generating method information for BLE operations
 */
object MethodInfoGenerator {
    
    fun generateInfo(method: Method, vararg params: Any?): String {
        val paramTypes = method.parameterTypes.joinToString(", ") { it.simpleName }
        val paramValues = params.joinToString(", ") { it?.toString() ?: "null" }
        return "${method.declaringClass.simpleName}.${method.name}($paramTypes) with values: [$paramValues]"
    }
    
    fun generateInfo(className: String, methodName: String, vararg params: Any?): String {
        val paramValues = params.joinToString(", ") { it?.toString() ?: "null" }
        return "$className.$methodName() with values: [$paramValues]"
    }
    
    fun generateRequestInfo(requestType: String, params: Map<String, Any?>): String {
        val paramStr = params.entries.joinToString(", ") { "${it.key}=${it.value}" }
        return "BLE Request: $requestType [$paramStr]"
    }
}