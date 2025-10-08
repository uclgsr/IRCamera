// Merged ALL .kt and .java files from the 'BleModule\src\main\java\com\topdon\commons\util' directory and its subdirectories.
// Total files: 5 | Generated on: 2025-10-08 01:42:33


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