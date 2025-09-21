package com.mpdc4gsr.commons.observer;


public interface Observer {
    
    @Observe
    default void onChanged(Object o) {}
}
