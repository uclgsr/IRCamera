// Merged ALL .kt and .java files from the 'BleModule\src\main\java\com\topdon\commons\base\interfaces' directory and its subdirectories.
// Total files: 5 | Generated on: 2025-10-08 01:42:33


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