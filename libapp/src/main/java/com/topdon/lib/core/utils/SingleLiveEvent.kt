package com.topdon.lib.core.utils

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 解决LiveData粘性Event
 * Created by jzh on 2020-12-28.
 */
/**
 * SingleLiveEvent manages camera operations and image capture functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {
    private val mPending: AtomicBoolean = AtomicBoolean(false)

    override fun observe(
        owner: LifecycleOwner,
        observer: Observer<in T>,
    ) {
        super.observe(owner, {
            if (mPending.compareAndSet(true, false)) {
                observer.onChanged(it)
            }
        })
    }

    @MainThread
    override fun setValue(t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    /**
     * Executes call functionality.
     */
    fun call() {
        this.setValue(null)
    }
}
