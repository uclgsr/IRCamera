package com.topdon.commons.util;



public class DiagnoseEventBusBean {
    private int what;//1   2 sn  3 4   5 Folder sn   6 diagMenuMask
    private String language;
    private boolean snConnection;// true sn  false
    private boolean isDiagnose;// true   false
    private long mDiagEntryType;//
    private long mDiagMenuMask;//
    private String snPath;//sn

    public String getSnPath() {
        return snPath;
    }

    public void setSnPath(String snPath) {
        this.snPath = snPath;
    }

    public int getWhat() {
        return what;
    }

    public void setWhat(int what) {
        this.what = what;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isSnConnection() {
        return snConnection;
    }

    public void setSnConnection(boolean snConnection) {
        this.snConnection = snConnection;
    }

    public boolean isDiagnose() {
        return isDiagnose;
    }

    public void setDiagnose(boolean diagnose) {
        isDiagnose = diagnose;
    }

    public long getmDiagEntryType() {
        return mDiagEntryType;
    }

    public void setmDiagEntryType(long mDiagEntryType) {
        this.mDiagEntryType = mDiagEntryType;
    }

    public long getDiagMenuMask() {
        return mDiagMenuMask;
    }

    public void setDiagMenuMask(long diagMenuMask) {
        mDiagMenuMask = diagMenuMask;
    }
}
