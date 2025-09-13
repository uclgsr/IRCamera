package com.infisense.usbir;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author: CaiSongL
 * @date: 2023/5/24 9:47
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private static CrashHandler crashHandler = new CrashHandler();

    private Context mContext;
    /** errorLogfile */
    private File logFile ;

    private CrashHandler() {

    }

    public static CrashHandler getInstance() {
        if (crashHandler == null) {
            synchronized (CrashHandler.class) {
                if (crashHandler == null) {
                    crashHandler = new CrashHandler();
                }
            }
        }
        return crashHandler;
    }

    public void init(Context context) {
        mContext = context;
        logFile = new File(mContext.getCacheDir(),"crashLog.trace");
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //settings为line程默认的exceptionprocessing器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // 打印exceptioninfo
        ex.printStackTrace();
        // 我们没有processingexception 并且默认exceptionprocessing不为空 则交给系统processing
        if (!handlelException(ex) && mDefaultHandler != null) {
            // 系统processing
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                // UploaderrorLog到service器
                upLoadErrorFileToServer(logFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
//            Intent intent = new Intent(mContext, SplashActivity.class);
//            // 新开task栈
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            mContext.startActivity(intent);
//            // 杀死我们的process
//            Timer timer = new Timer();
//            timer.schedule(new TimerTask() {
//
//                @Override
//                public void run() {
//                    Process.killProcess(Process.myPid());
//                }
//            }, 2 * 1000);

        }
    }

    private boolean handlelException(Throwable ex) {
        if (ex == null) {
            return false;
        }

        // 使用Toast来Show/Displayexceptioninfo
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
//                Log.e("发成exception","data采集success"+ex.getMessage());
                Toast.makeText(mContext, "程序发生exception，即将重启", Toast.LENGTH_LONG)
                        .show();
                Looper.loop();
            }
        }.start();

        PrintWriter pw = null;
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            pw = new PrintWriter(logFile);
            // 收集手机及errorinfo
            logFile = collectInfoToSDCard(pw, ex);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * UploaderrorLog到service器
     */
    private void upLoadErrorFileToServer(File errorFile) {

    }

    /**
     * 收集手机info
     *
     */
    private File collectInfoToSDCard(PrintWriter pw, Throwable ex)
            throws PackageManager.NameNotFoundException {

        PackageManager pm = mContext.getPackageManager();
        PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(),PackageManager.GET_ACTIVITIES);
        // error发生时间
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        pw.print("time : ");
        pw.println(time);
        // versioninfo
        pw.print("versionCode : ");
        pw.println(pi.versionCode);
        // 应用version号
        pw.print("versionName : ");
        pw.println(pi.versionName);
        try {
            /** 暴力反射Get/Retrievedata */
            Field[] Fields = Build.class.getDeclaredFields();
            for (Field field : Fields) {
                field.setAccessible(true);
                pw.print(field.getName() + " : ");
                pw.println(field.get(null).toString());
            }
        } catch (Exception e) {
            Log.i(TAG, "an error occured when collect crash info" + e);
        }

        // 打印stackinfo
        ex.printStackTrace(pw);
        return logFile;
    }
}