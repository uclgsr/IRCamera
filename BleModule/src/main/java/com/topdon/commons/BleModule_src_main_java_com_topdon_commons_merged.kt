// Merged ALL .kt and .java files from the 'BleModule\src\main\java\com\topdon\commons' directory and its subdirectories.
// Total files: 35 | Generated on: 2025-10-08 01:42:33


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


// ===== FROM: BleModule\src\main\java\com\topdon\commons\BleObserver.java =====

package com.topdon.commons;

import com.topdon.ble.EventObserver;

public interface BleObserver extends EventObserver {
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\helper\PermissionsRequester.java =====

package com.topdon.commons.helper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class PermissionsRequester {
    private static final int PERMISSION_REQUEST_CODE = 10;
    private static final int REQUEST_CODE_WRITE_SETTINGS = 11;
    private static final int REQUEST_CODE_UNKNOWN_APP_SOURCES = 12;

    private final List<String> allPermissions = new ArrayList<>();
    private final List<String> refusedPermissions = new ArrayList<>();
    private Callback callback;
    private Activity activity;
    private Fragment fragment;
    private boolean checking;

    public PermissionsRequester(@NonNull Activity activity)
    {
        this.activity = activity;
    }

    public PermissionsRequester(@NonNull Fragment fragment)
    {
        this.fragment = fragment;
    }

    public void setCallback(Callback callback)
    {
        this.callback = callback;
    }

    public void checkAndRequest(@NonNull List<String> permissions)
    {
        if (checking) {
            return;
        }
        refusedPermissions.clear();
        allPermissions.clear();
        allPermissions.addAll(permissions);
        checkPermissions(allPermissions, false);
    }

    public boolean hasPermissions(@NonNull List<String> permissions)
    {
        return checkPermissions(permissions, true);
    }

    @SuppressWarnings("all")
    private boolean checkPermissions(List<String> permissions, boolean onlyCheck)
    {
        Context context = activity != null ? activity : fragment.getContext();
        if (context == null) return false;
        if (permissions.remove(Manifest.permission.WRITE_SETTINGS) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                if (!onlyCheck) {
                    Intent intent = new Intent(
                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + context.getPackageName())
                    );
                    if (activity != null) {
                        activity.startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS);
                    } else {
                        fragment.startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS);
                    }
                    checking = true;
                }
                return false;
            }
        }
        if (permissions.remove(Manifest.permission.REQUEST_INSTALL_PACKAGES) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                if (!onlyCheck) {
                    Intent intent = new Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + context.getPackageName())
                    );
                    if (activity != null) {
                        activity.startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP_SOURCES);
                    } else {
                        fragment.startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP_SOURCES);
                    }
                    checking = true;
                }
                return false;
            }
        }
        List<String> needRequestPermissonList = findDeniedPermissions (permissions);
        if (onlyCheck) {
            return needRequestPermissonList.isEmpty();
        } else if (!needRequestPermissonList.isEmpty()) {
            if (activity != null) {
                ActivityCompat.requestPermissions(
                    activity,
                    needRequestPermissonList.toArray(new String [0]),
                    PERMISSION_REQUEST_CODE
                );
            } else {
                fragment.requestPermissions(needRequestPermissonList.toArray(new String [0]), PERMISSION_REQUEST_CODE);
            }
            checking = true;
            return false;
        } else {
            if (callback != null) {
                callback.onRequestResult(refusedPermissions);
            }
            checking = false;
            return true;
        }
    }

    //
    private List<String> findDeniedPermissions(List<String> permissions)
    {
        List<String> needRequestPermissionList = new ArrayList<>();
        Activity activity = this.activity != null ? this.activity : fragment.getActivity();
        if (activity != null) {
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
                ) {
                    needRequestPermissionList.add(perm);
                }
            }
        }
        return needRequestPermissionList;
    }

    public void onActivityResult(int requestCode)
    {
        Context context = activity != null ? activity : fragment.getContext();
        if (context == null) return;
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                refusedPermissions.add(Manifest.permission.WRITE_SETTINGS);
            }
            checkPermissions(allPermissions, false);
        }
        if (requestCode == REQUEST_CODE_UNKNOWN_APP_SOURCES && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                refusedPermissions.add(Manifest.permission.REQUEST_INSTALL_PACKAGES);
            }
            checkPermissions(allPermissions, false);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions [i];
                if (allPermissions.remove(permission) && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    refusedPermissions.add(permission);
                }
            }
            if (callback != null) {
                callback.onRequestResult(refusedPermissions);
            }
            checking = false;
        }
    }

    public interface Callback {

        void onRequestResult(List<String> refusedPermissions);
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\MyEvent.java =====

package com.topdon.commons;

public class MyEvent {
    public String msg;

    public MyEvent(String msg)
    {
        this.msg = msg;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\Observable.java =====

package com.topdon.commons.observer;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topdon.commons.poster.MethodInfo;
import com.topdon.commons.poster.PosterDispatcher;

import java.lang.reflect.Method;
import java.util.*;

public final class Observable {
    private final List<ObserverInfo> observerInfos = new ArrayList<>();
    private final PosterDispatcher posterDispatcher;
    private final ObserverMethodHelper helper;

    public Observable(@NonNull PosterDispatcher posterDispatcher, boolean isObserveAnnotationRequired)
    {
        this.posterDispatcher = posterDispatcher;
        helper = new ObserverMethodHelper (isObserveAnnotationRequired);
    }

    public PosterDispatcher getPosterDispatcher()
    {
        return posterDispatcher;
    }

    public void registerObserver(@NonNull Observer observer)
    {
        Objects.requireNonNull(observer, "observer can't be null");
        synchronized(observerInfos) {
            boolean registered = false;
            for (Iterator< ObserverInfo > it = observerInfos.iterator(); it.hasNext(); ) {
            ObserverInfo info = it . next ();
            Observer o = info . weakObserver . get ();
            if (o == null) {
                it.remove();
            } else if (o == observer) {
                registered = true;
            }
        }
            if (registered) {
                Log.e("Observable", "", new Error ("Observer " + observer + " is already registered."));
                return;
            }
            Map<String, Method> methodMap = helper . findObserverMethod (observer);
            observerInfos.add(new ObserverInfo (observer, methodMap));
        }
    }

    public boolean isRegistered(@NonNull Observer observer)
    {
        synchronized(observerInfos) {
            for (ObserverInfo info : observerInfos) {
            if (info.weakObserver.get() == observer) {
                return true;
            }
        }
            return false;
        }
    }

    public void unregisterObserver(@NonNull Observer observer)
    {
        synchronized(observerInfos) {
            for (Iterator< ObserverInfo > it = observerInfos.iterator(); it.hasNext(); ) {
            ObserverInfo info = it . next ();
            Observer o = info . weakObserver . get ();
            if (o == null || observer == o) {
                it.remove();
            }
        }
        }
    }

    public void unregisterAll()
    {
        synchronized(observerInfos) {
            observerInfos.clear();
        }
        helper.clearCache();
    }

    private List<ObserverInfo> getObserverInfos()
    {
        synchronized(observerInfos) {
            ArrayList<ObserverInfo> infos = new ArrayList<>();
            for (ObserverInfo info : observerInfos) {
            Observer observer = info . weakObserver . get ();
            if (observer != null) {
                infos.add(info);
            }
        }
            return infos;
        }
    }

    public void notifyObservers(@NonNull String methodName, @Nullable MethodInfo.Parameter... parameters)
    {
        notifyObservers(new MethodInfo (methodName, parameters));
    }

    public void notifyObservers(@NonNull MethodInfo info)
    {
        List<ObserverInfo> infos = getObserverInfos ();
        for (ObserverInfo oi : infos) {
        Observer observer = oi . weakObserver . get ();
        if (observer != null) {
            String key = helper . generateKey (info.getTag(), info.getName(), info.getParameterTypes());
            Method method = oi . methodMap . get (key);
            if (method != null) {
                Runnable runnable = helper . generateRunnable (observer, method, info);
                posterDispatcher.post(method, runnable);
            }
        }
    }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\Observe.java =====

package com.topdon.commons.observer;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @ interface Observe {
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\Observer.java =====

package com.topdon.commons.observer;

public interface Observer {

    @Observe
    default void onChanged(Object o)
    {
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\ObserverInfo.java =====

package com.topdon.commons.observer;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;

class ObserverInfo {
    final WeakReference<Observer> weakObserver;
    final Map<String, Method> methodMap;

    ObserverInfo(Observer observer, Map<String, Method> methodMap)
    {
        weakObserver = new WeakReference < > (observer);
        this.methodMap = methodMap;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\ObserverMethodHelper.java =====

package com.topdon.commons.observer;

import com.topdon.commons.poster.MethodInfo;
import com.topdon.commons.poster.Tag;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ObserverMethodHelper {
    private static final Map<Class<?>, Map<String, Method>> METHOD_CACHE = new ConcurrentHashMap<>();
    private boolean isObserveAnnotationRequired;

    ObserverMethodHelper(boolean isObserveAnnotationRequired)
    {
        this.isObserveAnnotationRequired = isObserveAnnotationRequired;
    }

    private static boolean contains(List<Method> methods, Method method)
    {
        for (Method m : methods) {
        if (m.getName().equals(method.getName()) && m.getReturnType().equals(method.getReturnType()) &&
            equalParamTypes(m.getParameterTypes(), method.getParameterTypes())
        ) {
            return true;
        }
    }
        return false;
    }

    private static boolean equalParamTypes(Class<?>[] params1, Class<?>[] params2)
    {
        if (params1.length == params2.length) {
            for (int i = 0; i < params1.length; i++) {
                if (params1[i] != params2[i])
                    return false;
            }
            return true;
        }
        return false;
    }

    void clearCache()
    {
        METHOD_CACHE.clear();
    }

    Runnable generateRunnable(Observer observer, Method method, MethodInfo info)
    {
        MethodInfo.Parameter[] parameters = info . getParameters ();
        if (parameters == null || parameters.length == 0) {
            return () -> {
                try {
                    method.invoke(observer);
                } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            };
        } else {
            final Object [] params = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                MethodInfo.Parameter parameter = parameters [i];
                params[i] = parameter.getValue();
            }
            return () -> {
                try {
                    method.invoke(observer, params);
                } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            };
        }
    }

    String generateKey(String tag, String name, Class<?>[] paramTypes)
    {
        StringBuilder sb = new StringBuilder();
        if (tag.isEmpty()) {
            sb.append(name);
        } else {
            sb.append(tag);
        }
        for (Class<?> type : paramTypes) {
        sb.append(",").append(type);
    }
        return sb.toString();
    }

    Map<String, Method> findObserverMethod(Observer observer)
    {
        Map<String, Method> map = METHOD_CACHE . get (observer.getClass());
        if (map != null) {
            return map;
        }
        map = new HashMap < > ();
        List<Method> methods = new ArrayList<>();
        Class<?> cls = observer . getClass ();
        while (cls != null && !cls.isInterface() && Observer.class. isAssignableFrom (cls)) {
            Method[] ms = null;
            try {
                ms = cls.getDeclaredMethods();
            } catch (Throwable ignore) {
            }
            if (ms != null) {
                for (Method m : ms) {
                    int ignore = Modifier . ABSTRACT | Modifier . STATIC | 0x40 | 0x1000;
                    if ((m.getModifiers() & Modifier.PUBLIC) != 0 && (m.getModifiers() & ignore) == 0 && !contains(methods, m)) {
                    methods.add(m);
                }
                }
            }
            cls = cls.getSuperclass();
        }
        for (Method method : methods) {
        Observe anno = method . getAnnotation (Observe.class);
        if (anno != null || !isObserveAnnotationRequired) {
            Tag tagAnno = method . getAnnotation (Tag.class);
            String tag = tagAnno == null ? "" : tagAnno.value();
            String key = generateKey (tag, method.getName(), method.getParameterTypes());
            map.put(key, method);
        }
    }
        if (!map.isEmpty()) {
            METHOD_CACHE.put(observer.getClass(), map);
        }
        return map;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\AsyncPoster.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

final class AsyncPoster implements Runnable, Poster {
    private final ExecutorService executorService;
    private final Queue<Runnable> queue;

    AsyncPoster(@NonNull ExecutorService executorService) {
        this.executorService = executorService;
        queue = new ConcurrentLinkedQueue < > ();
    }

    @Override
    public void enqueue(@NonNull Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null, cannot be enqueued");
        queue.add(runnable);
        executorService.execute(this);
    }

    @Override
    public void clear() {
        synchronized(this) {
            queue.clear();
        }
    }

    @Override
    public void run() {
        Runnable runnable = queue . poll ();
        if (runnable != null) {
            runnable.run();
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\BackgroundPoster.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

final class BackgroundPoster implements Runnable, Poster {
    private final ExecutorService executorService;
    private final Queue<Runnable> queue;
    private volatile boolean executorRunning;

    BackgroundPoster(@NonNull ExecutorService executorService) {
        this.executorService = executorService;
        queue = new ConcurrentLinkedQueue < > ();
    }

    @Override
    public void enqueue(@NonNull Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null, cannot be enqueued");
        synchronized(this) {
            queue.add(runnable);
            if (!executorRunning) {
                executorRunning = true;
                executorService.execute(this);
            }
        }
    }

    @Override
    public void clear() {
        synchronized(this) {
            queue.clear();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Runnable runnable = queue . poll ();
                if (runnable == null) {
                    synchronized(this) {
                        runnable = queue.poll();
                        if (runnable == null) {
                            executorRunning = false;
                            return;
                        }
                    }
                }
                runnable.run();
            }
        } finally {
            executorRunning = false;
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\MainThreadPoster.java =====

package com.topdon.commons.poster;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

final class MainThreadPoster extends Handler implements Poster {
    private final Queue<Runnable> queue;
    private boolean handlerActive;

    MainThreadPoster() {
        super(Looper.getMainLooper());
        queue = new ConcurrentLinkedQueue < > ();
    }

    @Override
    public void enqueue(@NonNull Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null, cannot be enqueued");
        synchronized(this) {
            queue.add(runnable);
            if (!handlerActive) {
                handlerActive = true;
                if (!sendMessage(obtainMessage())) {
                    throw new RuntimeException ("Could not send handler message");
                }
            }
        }
    }

    @Override
    public void clear() {
        synchronized(this) {
            queue.clear();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            while (true) {
                Runnable runnable = queue . poll ();
                if (runnable == null) {
                    synchronized(this) {
                        runnable = queue.poll();
                        if (runnable == null) {
                            handlerActive = false;
                            return;
                        }
                    }
                }
                runnable.run();
            }
        } finally {
            handlerActive = false;
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\MethodInfo.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;

public class MethodInfo {
    @NonNull
    private String name;
    @Nullable
    private Parameter[] parameters;
    @NonNull
    private String tag;

    public MethodInfo(@NonNull String name, @Nullable Parameter... parameters)
    {
        this(name, name, parameters);
    }

    public MethodInfo(@NonNull String name, @NonNull String tag, @Nullable Parameter... parameters)
    {
        this.name = name;
        this.tag = tag;
        this.parameters = parameters;
    }

    public MethodInfo(@NonNull String name, @Nullable Class<?>[] parameterTypes)
    {
        this(name, name, parameterTypes);
    }

    public MethodInfo(@NonNull String name, @NonNull String tag, @Nullable Class<?>[] parameterTypes)
    {
        this(name, tag, toParameters(parameterTypes));
    }

    public static MethodInfo valueOf(@NonNull Method method)
    {
        Tag annotation = method . getAnnotation (Tag.class);
        return new MethodInfo (method.getName(), annotation == null ? method.getName() : annotation.value(),
        method.getParameterTypes());
    }

    private static Parameter[] toParameters(Class<?>[] parameterTypes)
    {
        Parameter[] parameters = null;
        if (parameterTypes != null) {
            parameters = new Parameter [parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameters[i] = new Parameter (parameterTypes[i], null);
            }
        }
        return parameters;
    }

    @NonNull
    public String getName()
    {
        return name;
    }

    public void setName(@NonNull String name)
    {
        this.name = name;
    }

    @NonNull
    public String getTag()
    {
        return tag;
    }

    public void setTag(@NonNull String tag)
    {
        this.tag = tag;
    }

    @Nullable
    public Parameter[] getParameters()
    {
        return parameters;
    }

    public void setParameters(@Nullable Parameter[] parameters)
    {
        this.parameters = parameters;
    }

    @Nullable
    public Class<?>[] getParameterTypes()
    {
        if (parameters == null) {
            return null;
        } else {
            Class<?>[] types = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                types[i] = parameters[i].type;
            }
            return types;
        }
    }

    @Nullable
    public Object[] getParameterValues()
    {
        if (parameters == null) {
            return null;
        } else {
            Object[] values = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                values[i] = parameters[i].value;
            }
            return values;
        }
    }

    public static
    class Parameter {
        @Nullable
        private Object value ;
        @NonNull
        private Class<?> type;

        public Parameter(@NonNull Class<?> type, @Nullable Object value )
        {
            this.type = type;
            this.value = value;
        }

        @Nullable
        public Object getValue()
        {
            return value;
        }

        public void setValue(@Nullable Object value )
        {
            this.value = value;
        }

        @NonNull
        public Class<?> getType()
        {
            return type;
        }

        public void setType(@NonNull Class<?> type)
        {
            this.type = type;
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\Poster.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;

interface Poster {

    void enqueue(@NonNull Runnable runnable);

    void clear();
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\PosterDispatcher.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

public class PosterDispatcher {
    private final ThreadMode defaultMode;
    private final Poster backgroundPoster;
    private final Poster mainThreadPoster;
    private final ExecutorService executorService;
    private final Poster asyncPoster;

    public PosterDispatcher(@NonNull ExecutorService executorService, @NonNull ThreadMode defaultMode)
    {
        this.defaultMode = defaultMode;
        this.executorService = executorService;
        backgroundPoster = new BackgroundPoster (executorService);
        mainThreadPoster = new MainThreadPoster ();
        asyncPoster = new AsyncPoster (executorService);
    }

    public ThreadMode getDefaultMode()
    {
        return defaultMode;
    }

    public ExecutorService getExecutorService()
    {
        return executorService;
    }

    public void clearTasks()
    {
        backgroundPoster.clear();
        mainThreadPoster.clear();
        asyncPoster.clear();
    }

    public void shutdown()
    {
        clearTasks();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void post(@Nullable Method method, @NonNull Runnable runnable)
    {
        if (method != null) {
            RunOn annotation = method . getAnnotation (RunOn.class);
            ThreadMode mode = defaultMode;
            if (annotation != null) {
                mode = annotation.value();
            }
            post(mode, runnable);
        }
    }

    public void post(@NonNull ThreadMode mode, @NonNull Runnable runnable)
    {
        if (mode == ThreadMode.UNSPECIFIED) {
            mode = defaultMode;
        }
        switch(mode) {
            case MAIN :
            mainThreadPoster.enqueue(runnable);
            break;
            case POSTING :
            runnable.run();
            break;
            case BACKGROUND :
            backgroundPoster.enqueue(runnable);
            break;
            case ASYNC :
            asyncPoster.enqueue(runnable);
            break;
        }
    }

    public void post(@NonNull Object owner, @NonNull String methodName, @NonNull String tag,
    @Nullable MethodInfo.Parameter... parameters)
    {
        Class<?>[] classes = new Class[0];
        Object[] params = new Object[0];
        if (parameters != null) {
            params = new Object [parameters.length];
            classes = new Class [parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                MethodInfo.Parameter parameter = parameters [i];
                classes[i] = parameter.getType();
                params[i] = parameter.getValue();
            }
        }
        Method[] methods = owner . getClass ().getDeclaredMethods();
        Method tm = null;
        Method mm = null;
        for (Method method : methods) {
        Tag annotation = method . getAnnotation (Tag.class);
        if (annotation != null && !annotation.value().isEmpty() && annotation.value().equals(tag) &&
            equalParamTypes(method.getParameterTypes(), classes)
        ) {
            tm = method;
        }
        if (tm == null) {
            if (method.getName().equals(methodName) && equalParamTypes(method.getParameterTypes(), classes)) {
                mm = method;
            }
        } else {
            break;
        }
    }
        Method method = tm == null ? mm : tm;
        if (method == null) {
            return;
        }
        try {
            Object[] finalParams = params;
            post(method, () -> {
                try {
                    method.invoke(owner, finalParams);
                } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            });
        } catch (Exception ignore) {
        }
    }

    private boolean equalParamTypes(Class<?>[] params1, Class<?>[] params2)
    {
        if (params1.length == params2.length) {
            for (int i = 0; i < params1.length; i++) {
                if (params1[i] != params2[i])
                    return false;
            }
            return true;
        }
        return false;
    }

    public void post(@NonNull final Object owner, @NonNull String methodName, @Nullable MethodInfo.Parameter... parameters)
    {
        post(owner, methodName, "", parameters);
    }

    public void post(@NonNull Object owner, @NonNull MethodInfo methodInfo)
    {
        post(owner, methodInfo.getName(), methodInfo.getTag(), methodInfo.getParameters());
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\RunOn.java =====

package com.topdon.commons.poster;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @ interface RunOn {

    ThreadMode value () default ThreadMode.UNSPECIFIED;
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\Tag.java =====

package com.topdon.commons.poster;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @ interface Tag {
    String value () default "";
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\ThreadMode.java =====

package com.topdon.commons.poster;

public enum ThreadMode {

    POSTING,

    MAIN,

    BACKGROUND,

    ASYNC,

    UNSPECIFIED
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\util\DiagnoseEventBusBean.java =====

package com.topdon.commons.util;

public class DiagnoseEventBusBean {
    private int what;//1   2 sn  3 4   5 Folder sn   6 diagMenuMask
    private String language;
    private boolean snConnection;// true sn  false
    private boolean isDiagnose;// true   false
    private long mDiagEntryType;//
    private long mDiagMenuMask;//
    private String snPath;//sn

    public String getSnPath()
    {
        return snPath;
    }

    public void setSnPath(String snPath)
    {
        this.snPath = snPath;
    }

    public int getWhat()
    {
        return what;
    }

    public void setWhat(int what)
    {
        this.what = what;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public boolean isSnConnection()
    {
        return snConnection;
    }

    public void setSnConnection(boolean snConnection)
    {
        this.snConnection = snConnection;
    }

    public boolean isDiagnose()
    {
        return isDiagnose;
    }

    public void setDiagnose(boolean diagnose)
    {
        isDiagnose = diagnose;
    }

    public long getmDiagEntryType()
    {
        return mDiagEntryType;
    }

    public void setmDiagEntryType(long mDiagEntryType)
    {
        this.mDiagEntryType = mDiagEntryType;
    }

    public long getDiagMenuMask()
    {
        return mDiagMenuMask;
    }

    public void setDiagMenuMask(long diagMenuMask)
    {
        mDiagMenuMask = diagMenuMask;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\util\LLog.java =====

package com.topdon.commons.util;

import android.util.Log;

import com.elvishew.xlog.XLog;

public class LLog {

    public final static int MAX_LENGTH = 2000;
    private static boolean isDebug = true; // Simplified for now

    public static void d(String tag, String value )
    {
        XLog.tag(tag).d(value);
//        if (isDebug) {
//            Log.d(tag, value);
//        }
    }

    public static void i(String tag, String value )
    {
        XLog.tag(tag).i(value);
//        if (isDebug) {
//            Log.i(tag, value);
//        }
    }

    public static void w(String tag, String value )
    {
        XLog.tag(tag).w(value);
//        if (isDebug) {
//            Log.w(tag, value);
//        }
    }

    public static void e(String tag, String value )
    {
        XLog.tag(tag).e(value);
//        if (isDebug) {
//            Log.e(tag, value);
//        }
    }

    public static void LogMaxPrint(String tag, String msg)
    {
        if (msg.length() > MAX_LENGTH) {
            int length = MAX_LENGTH +1;
            String remain = msg;
            int index = 0;
            while (length > MAX_LENGTH) {
                index++;
                Log.v(tag + "[" + index + "]", " \n" + remain.substring(0, MAX_LENGTH));
                remain = remain.substring(MAX_LENGTH);
                length = remain.length();
            }
            if (length <= MAX_LENGTH) {
                index++;
                Log.v(tag + "[" + index + "]", " \n" + remain);
            }
        } else {
            Log.v(tag, msg);
        }
    }

}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\util\Topdon.java =====

package com.topdon.commons.util;

import android.content.Context;

public class Topdon {
    private static Context app;

    public static void init(Context context)
    {
        app = context;
    }

    public static Context getApp()
    {
        return app;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\util\UnicodeReader.java =====

package com.topdon.commons.util;

import java.io.*;

public class UnicodeReader extends Reader {
    private static final int BOM_SIZE = 4;
    PushbackInputStream internalIn;
    InputStreamReader internalIn2 = null;
    String defaultEnc;

    UnicodeReader(InputStream in, String defaultEnc) {
        internalIn = new PushbackInputStream ( in, BOM_SIZE);
        this.defaultEnc = defaultEnc;
    }

    public String getDefaultEncoding() {
        return defaultEnc;
    }

    public String getEncoding() {
        if (internalIn2 == null)
            return null;
        return internalIn2.getEncoding();
    }

    protected void init() throws IOException {
        if (internalIn2 != null)
            return;

        String encoding;
        byte bom [] = new byte [BOM_SIZE];
        int n, unread;
        n = internalIn.read(bom, 0, bom.length);

        if ((bom[0] == (byte) 0x00) && (bom[1] == (byte) 0x00)
        && (bom[2] == (byte) 0xFE) && (bom[3] == (byte) 0xFF)) {
        encoding = "UTF-32BE";
        unread = n - 4;
    } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)
        && (bom[2] == (byte) 0x00) && (bom[3] == (byte) 0x00)) {
        encoding = "UTF-32LE";
        unread = n - 4;
    } else if ((bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB)
        && (bom[2] == (byte) 0xBF)) {
        encoding = "UTF-8";
        unread = n - 3;
    } else if ((bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF)) {
        encoding = "UTF-16BE";
        unread = n - 2;
    } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)) {
        encoding = "UTF-16LE";
        unread = n - 2;
    } else {
        // Unicode BOM mark not found, unread all bytes
        encoding = defaultEnc;
        unread = n;
    }
        // System.out.println("read=" + n + ", unread=" + unread);

        if (unread > 0)
            internalIn.unread(bom, (n - unread), unread);

        // Use given encoding
        if (encoding == null) {
            internalIn2 = new InputStreamReader (internalIn);
        } else {
            internalIn2 = new InputStreamReader (internalIn, encoding);
        }
    }

    public void close() throws IOException {
        init();
        internalIn2.close();
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        init();
        return internalIn2.read(cbuf, off, len);
    }

}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\util\WeakReferenceHandler.java =====

package com.topdon.commons.util;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

public class WeakReferenceHandler<T> extends Handler {

    private final WeakReference<T> mReference;

    public WeakReferenceHandler (T referencedObject) {
        mReference = new WeakReference < T >(referencedObject);
    }

    public WeakReferenceHandler (Looper looper, T referencedObject) {
        super(looper);
        mReference = new WeakReference < T >(referencedObject);
    }

    protected T getReferencedObject() {
        return mReference.get();
    }

}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\UUIDManager.java =====

package com.topdon.commons;

public class UUIDManager {

    public static final String SERVICE_UUID = "00010203-0405-0607-0809-0a0b0c0d1910";//"00010203-0405-0607-0809-0A0B0C0D1910";//

    public static final String NOTIFY_UUID = "00010203-0405-0607-0809-0a0b0c0d2b10";

    public static final String WRITE_UUID = "00010203-0405-0607-0809-0a0b0c0d2b11";//"00010203-0405-0607-0809-0A0B0C0D2B11";//

    public static final String READ_UUID = "00010203-0405-0607-0809-0a0b0c0d2b10";//"00010203-0405-0607-0809-0A0B0C0D2B10";//

    public static final String NOTIFY_DESCRIPTOR = "00010203-0405-0607-0809-0a0b0c0d2b10";
}