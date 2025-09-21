package com.mpdc4gsr.commons.poster;

import androidx.annotation.NonNull;

/**
 * date: 2019/8/7 09:44
 * author: chuanfeng.bi
 */
interface Poster {

    void enqueue(@NonNull Runnable runnable);


    void clear();
}
