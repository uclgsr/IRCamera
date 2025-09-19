package com.mpdc4gsr.commons.poster;

import androidx.annotation.NonNull;

interface Poster {

    void enqueue(@NonNull Runnable runnable);

    void clear();
}
