package com.mpdc4gsr.component.shared.app.tools;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

/**
 * Language utility for English-only application.
 * All methods enforce Locale.ENGLISH as the app only supports English.
 */
public class AppLanguageUtils {

    private static final Locale ENGLISH_LOCALE = Locale.ENGLISH;

    public static String getSystemLanguage() {
        return ConstantLanguages.ENGLISH;
    }

    public static Locale getLocaleByLanguage(String language) {
        return ENGLISH_LOCALE;
    }

    /**
     * Wraps context with English locale during Activity/Application initialization.
     * This is the correct approach for setting locale on API 24+.
     *
     * @param context  Base context to wrap
     * @param language Language parameter (ignored - always uses English)
     * @return Context wrapped with English locale on API 24+, unchanged context on older APIs
     */
    public static Context attachBaseContext(Context context, String language) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return createEnglishContext(context);
        }
        return context;
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context createEnglishContext(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(ENGLISH_LOCALE);
        configuration.setLocales(new LocaleList(ENGLISH_LOCALE));
        return context.createConfigurationContext(configuration);
    }
}


