package com.mpdc4gsr.commons.base.interfaces;

public interface Checkable<T> {
    boolean isChecked();

    T setChecked(boolean isChecked);
}
