package com.mpdc4gsr.commons.poster

/**
 * Tag annotation for method tagging
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Tag(val value: String = "")