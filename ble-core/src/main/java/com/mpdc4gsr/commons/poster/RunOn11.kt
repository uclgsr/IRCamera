package com.mpdc4gsr.commons.poster

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class RunOn(val value: ThreadMode = ThreadMode.UNSPECIFIED)
