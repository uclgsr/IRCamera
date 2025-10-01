package com.topdon.commons.observer;

/**
 *
 * <p>
 * date: 2019/8/3 13:15
 * author: chuanfeng.bi
 */
public interface Observer {
    /**
     *
     */
    @Observe
    default void onChanged(Object o) {
    }
}
