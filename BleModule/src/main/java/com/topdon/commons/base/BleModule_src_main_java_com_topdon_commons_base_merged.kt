// Merged ALL .kt and .java files from the 'BleModule\src\main\java\com\topdon\commons\base' directory and its subdirectories.
// Total files: 12 | Generated on: 2025-10-08 01:42:33


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\AppHolder.java =====

package com.topdon.commons.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class AppHolder implements Application.ActivityLifecycleCallbacks {
    //Activity
    private final List<RunningActivity> runningActivities = new CopyOnWriteArrayList<>();
    //
    private boolean isCompleteExit = false;
    private Application application;
    private Looper mainLooper;
    private RunningActivity topActivity;

    private AppHolder () {
        mainLooper = Looper.getMainLooper();
        //application
        application = tryGetApplication();
        if (application != null) {
            application.registerActivityLifecycleCallbacks(this);
        }
    }

    @NonNull
    public static AppHolder getInstance () {
        return Holder.INSTANCE;
    }

    public static void initialize (@NonNull Application application) {
        Objects.requireNonNull(application, "application is null");
        //Applicationï¼Œ
        if (Holder.INSTANCE.application != null && Holder.INSTANCE.application != application) {
            Holder.INSTANCE.application.unregisterActivityLifecycleCallbacks(Holder.INSTANCE);
            application.registerActivityLifecycleCallbacks(Holder.INSTANCE);
        }
        Holder.INSTANCE.application = application;
    }

    @SuppressLint("PrivateApi")
    @Nullable
    private Application tryGetApplication() {
        try {
            Class<?> cls = Class . forName ("android.app.ActivityThread");
            Method catMethod = cls . getMethod ("currentActivityThread");
            catMethod.setAccessible(true);
            Object aThread = catMethod . invoke (null);
            Method method = aThread . getClass ().getMethod("getApplication");
            return (Application) method . invoke (aThread);
        } catch (Exception e) {
            return null;
        }
    }

    @CallSuper
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        RunningActivity a = new RunningActivity(activity.getClass().getName(), new WeakReference < > (activity));
        if (!runningActivities.contains(a)) {
            runningActivities.add(a);
        }
        topActivity = a;
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @CallSuper
    @Override
    public void onActivityDestroyed(Activity activity) {
        if (runningActivities.isEmpty()) {
            topActivity = null;
        }
        RunningActivity a = new RunningActivity(activity.getClass().getName(), new WeakReference < > (activity));
        runningActivities.remove(a);
        if (isCompleteExit && runningActivities.isEmpty()) {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }

    public boolean isMainThread() {
        return Looper.myLooper() == mainLooper;
    }

    @NonNull
    public Looper getMainLooper() {
        if (mainLooper == null) {
            mainLooper = Looper.getMainLooper();
        }
        return mainLooper;
    }

    @NonNull
    public Context getContext() {
        Objects.requireNonNull(
            application,
            "The AppHolder has not been initialized, make sure to call AppHolder.initialize(app) first."
        );
        return application;
    }

    @Nullable
    public PackageInfo getPackageInfo() {
        try {
            PackageManager pm = application . getPackageManager ();
            return pm.getPackageInfo(application.getPackageName(), 0);
        } catch (Exception ignore) {
        }
        return null;
    }

    public boolean isAppOnForeground() {
        ActivityManager am =(ActivityManager) application . getSystemService (Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> processes = am . getRunningAppProcesses ();
            if (processes != null) {
                for (ActivityManager. RunningAppProcessInfo process : processes) {
                    if (application.getPackageName().equals(process.processName) &&
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND == process.importance
                    ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //
    private boolean contains(Object[] array, Object obj) {
        if (array != null && array.length > 0) {
            for (Object o : array) {
                if (o.equals(obj)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void finish(String className, String... classNames) {
        List<RunningActivity> list = new ArrayList<>(runningActivities);
        Collections.reverse(list);//ï¼Œfinish
        for (RunningActivity runningActivity : list) {
        Activity activity = runningActivity . weakActivity . get ();
        if (activity != null) {
            String name = activity . getClass ().getName();
            if (name.equals(className) || contains(classNames, name)) {
                activity.finish();
            }
        }
    }
    }

    public void finishAllWithout(@Nullable String className, String... classNames) {
        List<RunningActivity> list = new ArrayList<>(runningActivities);
        Collections.reverse(list);//ï¼Œfinish
        for (RunningActivity runningActivity : list) {
        Activity activity = runningActivity . weakActivity . get ();
        if (activity != null) {
            String name = activity . getClass ().getName();
            if (!name.equals(className) && !contains(classNames, name)) {
                activity.finish();
            }
        }
    }
    }

    public void finishAll() {
        finishAllWithout(null);
    }

    public void backTo(String className) {
        List<RunningActivity> list = new ArrayList<>(runningActivities);
        Collections.reverse(list);//ï¼Œfinish
        for (RunningActivity runningActivity : list) {
        Activity activity = runningActivity . weakActivity . get ();
        if (activity != null) {
            String name = activity . getClass ().getName();
            if (name.equals(className)) {
                activity.finish();
                return;
            }
        }
    }
    }

    @Nullable
    public Activity getActivity(String className) {
        for (RunningActivity runningActivity : runningActivities) {
        if (runningActivity.name.equals(className)) {
            return runningActivity.weakActivity.get();
        }
    }
        return null;
    }

    public boolean isAllFinished() {
        return runningActivities.isEmpty();
    }

    public List < Activity > getAllActivities () {
        List<Activity> activities = new ArrayList<>();
        for (RunningActivity runningActivity : runningActivities) {
        Activity activity = runningActivity . weakActivity . get ();
        if (activity != null) {
            activities.add(activity);
        }
    }
        return activities;
    }

    public void completeExit() {
        isCompleteExit = true;
        List<RunningActivity> list = new ArrayList<>(runningActivities);
        Collections.reverse(list);//ï¼Œfinish
        for (RunningActivity runningActivity : list) {
        Activity activity = runningActivity . weakActivity . get ();
        if (activity != null) {
            activity.finish();
        }
    }
    }

    public Activity getTopActivity() {
        return topActivity == null ? null : topActivity.weakActivity.get();
    }

    private static final class Holder {
        private static final AppHolder INSTANCE = new AppHolder();
    }

    private static class RunningActivity {
        String name;
        WeakReference<Activity> weakActivity;

        RunningActivity(String name, WeakReference<Activity> weakActivity)
        {
            this.name = name;
            this.weakActivity = weakActivity;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof RunningActivity)) return false;
            RunningActivity runningActivity =(RunningActivity) o;
            return name.equals(runningActivity.name);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(name);
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\entity\AbstractTimer.java =====

package com.topdon.commons.base.entity;

import android.os.Handler;
import android.os.Looper;

import java.util.Timer;
import java.util.TimerTask;

public abstract class AbstractTimer {
    private final Handler handler;
    private final boolean callbackOnMainThread;
    private Timer timer;

    public AbstractTimer(boolean callbackOnMainThread)
    {
        handler = new Handler (Looper.getMainLooper());
        this.callbackOnMainThread = callbackOnMainThread;
    }

    public abstract void onTick();

    public synchronized final void start(long delay, long period)
    {
        if (timer == null) {
            timer = new Timer ();
            timer.schedule(new TimerTask () {
                @Override
                public void run() {
                    if (callbackOnMainThread) {
                        handler.post(new Runnable () {
                            @Override
                            public void run() {
                                onTick();
                            }
                        });
                    } else {
                        onTick();
                    }
                }
            }, delay, period);
        }
    }

    public synchronized final void stop()
    {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public boolean isRunning()
    {
        return timer != null;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\entity\CheckableItem.java =====

package com.topdon.commons.base.entity;

import com.topdon.commons.base.interfaces.Checkable;

public class CheckableItem<T> implements Checkable<CheckableItem<T>> {
    private T data;
    private boolean isChecked;

    public CheckableItem () {
    }

    public CheckableItem (T data) {
        this.data = data;
    }

    public CheckableItem (T data, boolean isChecked) {
        this.data = data;
        this.isChecked = isChecked;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public CheckableItem < T > setChecked (boolean isChecked) {
        this.isChecked = isChecked;
        return this;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\entity\CheckableParcelable.java =====

package com.topdon.commons.base.entity;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class CheckableParcelable<T extends Parcelable> extends CheckableItem<T> implements Parcelable {
    public static final Creator < CheckableParcelable > CREATOR = new Creator<CheckableParcelable>() {
        @Override
        public CheckableParcelable createFromParcel(Parcel source) {
            return new CheckableParcelable (source);
        }

        @Override
        public CheckableParcelable [] newArray (int size) {
            return new CheckableParcelable [size];
        }
    };

    public CheckableParcelable () {
    }

    public CheckableParcelable (T data) {
        super(data);
    }

    public CheckableParcelable (T data, boolean isChecked) {
        super(data, isChecked);
    }

    @SuppressWarnings("unchecked")
    protected CheckableParcelable (Parcel in) {
        Bundle bundle = in . readBundle (getClass().getClassLoader());
        if (bundle != null) {
            setData((T) bundle . getParcelable ("items"));
        }
        setChecked(in.readByte() != 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("items", getData());
        dest.writeBundle(bundle);
        dest.writeByte(isChecked() ?(byte) 1 : (byte) 0);
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\entity\DatabaseContext.java =====

package com.topdon.commons.base.entity;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Objects;

public class DatabaseContext extends ContextWrapper {
    private File dbDir;

    public DatabaseContext (Context base, @NonNull File dbDir) {
        super(base);
        Objects.requireNonNull(dbDir, "dbDir is null");
        this.dbDir = dbDir;
    }

    @Override
    public File getDatabasePath(String name) {
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        return new File (dbDir, name);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(
        String name,
        int mode,
        SQLiteDatabase.CursorFactory factory,
        DatabaseErrorHandler errorHandler
    ) {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        return super.openOrCreateDatabase(getDatabasePath(name).getName(), mode, factory);
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\entity\UnitDBBean.java =====

package com.topdon.commons.base.entity;

import java.io.Serializable;

public class UnitDBBean implements Serializable {
//    {
//        "": "",
//            "": "m",
//            "": "",
//            "": "yd.",
//            "": "",
//            "": "1  = 1.094",
//            "": "1.094"
//    },

    private static final long serialVersionUID = -1L;
    public Long dbid;
    String LoginName;//
    int unitType;//0   1 
    String conversionRelation;//
    String preUnit;//
    String preName;//
    String afterUnit;//
    String afterName;//
    String conversionFormula;//
    String calcFactor;//

    public Long getDbid() {
        return dbid;
    }

    public void setDbid(Long dbid) {
        this.dbid = dbid;
    }

    public String getLoginName() {
        return LoginName;
    }

    public void setLoginName(String loginName) {
        LoginName = loginName;
    }

    public int getUnitType() {
        return unitType;
    }

    public void setUnitType(int unitType) {
        this.unitType = unitType;
    }

    public String getConversionRelation() {
        return conversionRelation;
    }

    public void setConversionRelation(String conversionRelation) {
        this.conversionRelation = conversionRelation;
    }

    public String getPreUnit() {
        return preUnit;
    }

    public void setPreUnit(String preUnit) {
        this.preUnit = preUnit;
    }

    public String getPreName() {
        return preName;
    }

    public void setPreName(String preName) {
        this.preName = preName;
    }

    public String getAfterUnit() {
        return afterUnit;
    }

    public void setAfterUnit(String afterUnit) {
        this.afterUnit = afterUnit;
    }

    public String getAfterName() {
        return afterName;
    }

    public void setAfterName(String afterName) {
        this.afterName = afterName;
    }

    public String getConversionFormula() {
        return conversionFormula;
    }

    public void setConversionFormula(String conversionFormula) {
        this.conversionFormula = conversionFormula;
    }

    public String getCalcFactor() {
        return calcFactor;
    }

    public void setCalcFactor(String calcFactor) {
        this.calcFactor = calcFactor;
    }

}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\interfaces\Callback.java =====

package com.topdon.commons.base.interfaces;

public interface Callback<T> {
    void onCallback(T obj);
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\interfaces\Checkable.java =====

package com.topdon.commons.base.interfaces;

public interface Checkable<T> {
    boolean isChecked();

    T setChecked(boolean isChecked);
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\interfaces\DrawableBuilder.java =====

package com.topdon.commons.base.interfaces;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public interface DrawableBuilder {
    @NonNull
    Drawable build();
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\interfaces\IText.java =====

package com.topdon.commons.base.interfaces;

import androidx.annotation.NonNull;

public interface IText {
    @NonNull
    String getText();
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\interfaces\IWeight.java =====

package com.topdon.commons.base.interfaces;

public interface IWeight {

    Integer getWeight();
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\base\SaveBean.java =====

package com.topdon.commons.base;

public class SaveBean {

    public String type;
    public String mac;
    public String name;

    public SaveBean(String type, String mac, String name)
    {
        this.type = type;
        this.mac = mac;
        this.name = name;
    }

    public SaveBean()
    {
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getMac()
    {
        return mac;
    }

    public void setMac(String mac)
    {
        this.mac = mac;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}