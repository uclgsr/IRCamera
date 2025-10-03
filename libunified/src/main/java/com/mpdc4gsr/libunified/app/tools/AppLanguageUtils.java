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

    public static void changeAppLanguage(Context context, String newLanguage) {
        Locale locale = Locale.ENGLISH;
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration);
        } else {
            updateConfigurationLegacy(context.getResources(), configuration);
        }
    }
    
    @SuppressWarnings("deprecation")
    private static void updateConfigurationLegacy(Resources resources, Configuration configuration) {
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
