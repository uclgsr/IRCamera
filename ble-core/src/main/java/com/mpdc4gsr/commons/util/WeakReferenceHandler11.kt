package com.mpdc4gsr.commons.util

import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference

class WeakReferenceHandler<T> : Handler {
    private val mReference: WeakReference<T?>

    constructor(referencedObject: T?) {
        mReference = WeakReference<T?>(referencedObject)
    }

    constructor(looper: Looper?, referencedObject: T?) : super(looper!!) {
        mReference = WeakReference<T?>(referencedObject)
    }

    protected val referencedObject: T?
        get() = mReference.get()
}
