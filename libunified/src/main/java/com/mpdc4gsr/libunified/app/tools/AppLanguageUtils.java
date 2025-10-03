package com.mpdc4gsr.libunified.app.tools;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.DisplayMetrics;

import java.util.Locale;

public class AppLanguageUtils {

    public static String getSystemLanguage() {
        return ConstantLanguages.ENGLISH;
    }

    @SuppressWarnings("deprecation")
    public static void changeAppLanguage(Context context, String newLanguage) {
        // Note: For runtime configuration changes, updateConfiguration() must be used
        // even on API 24+. The createConfigurationContext() approach only works when
        // wrapping contexts during Activity/Application initialization (see attachBaseContext).
        // While updateConfiguration() is deprecated, it remains the only way to change
        // app-wide configuration at runtime. Activities should be recreated after calling
        // this method to fully apply the configuration change.
        Locale locale = Locale.ENGLISH;
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(locale));
        }
        
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    public static String getSupportLanguage(String language) {
        return ConstantLanguages.ENGLISH;
    }

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
        Locale locale = Locale.ENGLISH;

        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        configuration.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(configuration);
    }
}
