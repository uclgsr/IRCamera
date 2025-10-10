package com.mpdc4gsr.libunified.app.repository

// ProductBean data class to replace removed TC007 functionality
data class ProductBean(
    val ProductName: String = "",
    val ProductPN: String = "",
    val ProductSN: String = "",
    val Code: String = "",
    val SoftwareVersion: Version07Bean? = null,
) {
    fun getVersionStr(): String =
        "${SoftwareVersion?.Major ?: "-"}.${SoftwareVersion?.Minor ?: "-"}${SoftwareVersion?.Build ?: "-"}"
}

data class Version07Bean(
    val Major: String? = "",
    val Minor: String? = "",
    val Build: String? = "",
)
