package com.mpdc4gsr.commons.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Bundle
import android.os.Looper
import android.os.Process
import androidx.annotation.CallSuper
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.Objects
import java.util.concurrent.CopyOnWriteArrayList

class AppHolder private constructor() : Application.ActivityLifecycleCallbacks {
    private val runningActivities: MutableList<RunningActivity> = CopyOnWriteArrayList<RunningActivity>()

    private var isCompleteExit = false
    private var application: Application?
    private var mainLooper: Looper?
    private var topActivity: RunningActivity? = null

    init {
        mainLooper = Looper.getMainLooper()

        application = tryGetApplication()
        if (application != null) {
            application!!.registerActivityLifecycleCallbacks(this)
        }
    }

    @SuppressLint("PrivateApi")
    private fun tryGetApplication(): Application? {
        try {
            val cls = Class.forName("android.app.ActivityThread")
            val catMethod = cls.getMethod("currentActivityThread")
            catMethod.setAccessible(true)
            val aThread = catMethod.invoke(null)
            val method = aThread!!.javaClass.getMethod("getApplication")
            return method.invoke(aThread) as Application?
        } catch (e: Exception) {
            return null
        }
    }

    @CallSuper
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val a = RunningActivity(activity.javaClass.getName(), WeakReference<Activity?>(activity))
        if (!runningActivities.contains(a)) {
            runningActivities.add(a)
        }
        topActivity = a
    }

    @CallSuper
    override fun onActivityStarted(activity: Activity) {
    }

    @CallSuper
    override fun onActivityResumed(activity: Activity) {
    }

    @CallSuper
    override fun onActivityPaused(activity: Activity) {
    }

    @CallSuper
    override fun onActivityStopped(activity: Activity) {
    }

    @CallSuper
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    @CallSuper
    override fun onActivityDestroyed(activity: Activity) {
        if (runningActivities.isEmpty()) {
            topActivity = null
        }
        val a = RunningActivity(activity.javaClass.getName(), WeakReference<Activity?>(activity))
        runningActivities.remove(a)
        if (isCompleteExit && runningActivities.isEmpty()) {
            Process.killProcess(Process.myPid())
            System.exit(0)
        }
    }

    val isMainThread: Boolean
        get() = Looper.myLooper() == mainLooper

    fun getMainLooper(): Looper {
        if (mainLooper == null) {
            mainLooper = Looper.getMainLooper()
        }
        return mainLooper!!
    }

    val context: Context
        get() {
            Objects.requireNonNull<Application?>(
                application,
                "The AppHolder has not been initialized, make sure to call AppHolder.initialize(app) first."
            )
            return application!!
        }

    val packageInfo: PackageInfo?
        get() {
            try {
                val pm = application!!.getPackageManager()
                return pm.getPackageInfo(application!!.getPackageName(), 0)
            } catch (ignore: Exception) {
            }
            return null
        }

    val isAppOnForeground: Boolean
        get() {
            val am =
                application!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            if (am != null) {
                val processes = am.getRunningAppProcesses()
                if (processes != null) {
                    for (process in processes) {
                        if (application!!.getPackageName() == process.processName &&
                            RunningAppProcessInfo.IMPORTANCE_FOREGROUND == process.importance
                        ) {
                            return true
                        }
                    }
                }
            }
            return false
        }

    private fun contains(array: Array<Any>?, obj: Any?): Boolean {
        if (array != null && array.size > 0) {
            for (o in array) {
                if (o == obj) {
                    return true
                }
            }
        }
        return false
    }

    fun finish(className: String?, vararg classNames: String) {
        val list: MutableList<RunningActivity> = ArrayList<RunningActivity>(runningActivities)
        Collections.reverse(list)
        for (runningActivity in list) {
            val activity = runningActivity.weakActivity.get()
            if (activity != null) {
                val name = activity.javaClass.getName()
                if (name == className || contains(classNames.toTypedArray<Any>(), name)) {
                    activity.finish()
                }
            }
        }
    }

    fun finishAllWithout(className: String?, vararg classNames: String) {
        val list: MutableList<RunningActivity> = ArrayList<RunningActivity>(runningActivities)
        Collections.reverse(list)
        for (runningActivity in list) {
            val activity = runningActivity.weakActivity.get()
            if (activity != null) {
                val name = activity.javaClass.getName()
                if (name != className && !contains(classNames.toTypedArray<Any>(), name)) {
                    activity.finish()
                }
            }
        }
    }

    fun finishAll() {
        finishAllWithout(null)
    }

    fun backTo(className: String?) {
        val list: MutableList<RunningActivity> = ArrayList<RunningActivity>(runningActivities)
        Collections.reverse(list)
        for (runningActivity in list) {
            val activity = runningActivity.weakActivity.get()
            if (activity != null) {
                val name = activity.javaClass.getName()
                if (name == className) {
                    activity.finish()
                    return
                }
            }
        }
    }

    fun getActivity(className: String?): Activity? {
        for (runningActivity in runningActivities) {
            if (runningActivity.name == className) {
                return runningActivity.weakActivity.get()
            }
        }
        return null
    }

    val isAllFinished: Boolean
        get() = runningActivities.isEmpty()

    val allActivities: MutableList<Activity?>
        get() {
            val activities: MutableList<Activity?> =
                ArrayList<Activity?>()
            for (runningActivity in runningActivities) {
                val activity = runningActivity.weakActivity.get()
                if (activity != null) {
                    activities.add(activity)
                }
            }
            return activities
        }

    fun completeExit() {
        isCompleteExit = true
        val list: MutableList<RunningActivity> = ArrayList<RunningActivity>(runningActivities)
        Collections.reverse(list)
        for (runningActivity in list) {
            val activity = runningActivity.weakActivity.get()
            if (activity != null) {
                activity.finish()
            }
        }
    }

    fun getTopActivity(): Activity? {
        return if (topActivity == null) null else topActivity!!.weakActivity.get()
    }

    private object Holder {
        val instance: AppHolder = AppHolder()
    }

    private class RunningActivity(var name: String, var weakActivity: WeakReference<Activity?>) {
        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o !is RunningActivity) return false
            val runningActivity: RunningActivity = o
            return name == runningActivity.name
        }

        override fun hashCode(): Int {
            return Objects.hash(name)
        }
    }

    companion object {
        fun initialize(application: Application) {
            Objects.requireNonNull<Application?>(application, "application is null")

            if (Holder.instance.application != null && Holder.instance.application !== application) {
                Holder.instance.application!!.unregisterActivityLifecycleCallbacks(Holder.instance)
                application.registerActivityLifecycleCallbacks(Holder.instance)
            }
            Holder.instance.application = application
        }
    }
}
