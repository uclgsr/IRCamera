package com.topdon.lib.core.tools;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.DisplayMetrics;

import java.util.Locale;

/**
 * @author YanLu
 * @since 17/5/12
 * 
 * English-only language utilities
 */

public class AppLanguageUtils {

    /**
     * Get system default language - always return English
     */
    public static String getSystemLanguage() {
        return ConstantLanguages.ENGLISH;
    }

    @SuppressWarnings("deprecation")
    public static void changeAppLanguage(Context context, String newLanguage) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        // app locale - always English
        Locale locale = Locale.ENGLISH;
        configuration.setLocale(locale);
        // updateConfiguration
        DisplayMetrics dm = resources.getDisplayMetrics();
        resources.updateConfiguration(configuration, dm);
    }

    public static String getSupportLanguage(String language) {
        return ConstantLanguages.ENGLISH;
    }

    /**
     * Get locale for specified language - always return English
     *
     * @param language language
     * @return English locale
     */
    public static Locale getLocaleByLanguage(String language) {
        return Locale.ENGLISH;
    }

    public static Context attachBaseContext(Context context, String language) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, language);
        } else {
            return context;
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context updateResources(Context context, String language) {
        Resources resources = context.getResources();
        Locale locale = Locale.ENGLISH; // Always use English

        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        configuration.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(configuration);
    }
}
