// Merged ALL .kt and .java files from the 'BleModule\src\main\java\com\topdon\commons\base\entity' directory and its subdirectories.
// Total files: 5 | Generated on: 2025-10-08 01:42:33


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