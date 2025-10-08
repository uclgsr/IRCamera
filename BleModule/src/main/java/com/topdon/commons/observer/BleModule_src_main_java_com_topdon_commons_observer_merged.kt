// Merged ALL .kt and .java files from the 'BleModule\src\main\java\com\topdon\commons\observer' directory and its subdirectories.
// Total files: 5 | Generated on: 2025-10-08 01:42:33


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\Observable.java =====

package com.topdon.commons.observer;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topdon.commons.poster.MethodInfo;
import com.topdon.commons.poster.PosterDispatcher;

import java.lang.reflect.Method;
import java.util.*;

public final class Observable {
    private final List<ObserverInfo> observerInfos = new ArrayList<>();
    private final PosterDispatcher posterDispatcher;
    private final ObserverMethodHelper helper;

    public Observable(@NonNull PosterDispatcher posterDispatcher, boolean isObserveAnnotationRequired)
    {
        this.posterDispatcher = posterDispatcher;
        helper = new ObserverMethodHelper (isObserveAnnotationRequired);
    }

    public PosterDispatcher getPosterDispatcher()
    {
        return posterDispatcher;
    }

    public void registerObserver(@NonNull Observer observer)
    {
        Objects.requireNonNull(observer, "observer can't be null");
        synchronized(observerInfos) {
            boolean registered = false;
            for (Iterator< ObserverInfo > it = observerInfos.iterator(); it.hasNext(); ) {
            ObserverInfo info = it . next ();
            Observer o = info . weakObserver . get ();
            if (o == null) {
                it.remove();
            } else if (o == observer) {
                registered = true;
            }
        }
            if (registered) {
                Log.e("Observable", "", new Error ("Observer " + observer + " is already registered."));
                return;
            }
            Map<String, Method> methodMap = helper . findObserverMethod (observer);
            observerInfos.add(new ObserverInfo (observer, methodMap));
        }
    }

    public boolean isRegistered(@NonNull Observer observer)
    {
        synchronized(observerInfos) {
            for (ObserverInfo info : observerInfos) {
            if (info.weakObserver.get() == observer) {
                return true;
            }
        }
            return false;
        }
    }

    public void unregisterObserver(@NonNull Observer observer)
    {
        synchronized(observerInfos) {
            for (Iterator< ObserverInfo > it = observerInfos.iterator(); it.hasNext(); ) {
            ObserverInfo info = it . next ();
            Observer o = info . weakObserver . get ();
            if (o == null || observer == o) {
                it.remove();
            }
        }
        }
    }

    public void unregisterAll()
    {
        synchronized(observerInfos) {
            observerInfos.clear();
        }
        helper.clearCache();
    }

    private List<ObserverInfo> getObserverInfos()
    {
        synchronized(observerInfos) {
            ArrayList<ObserverInfo> infos = new ArrayList<>();
            for (ObserverInfo info : observerInfos) {
            Observer observer = info . weakObserver . get ();
            if (observer != null) {
                infos.add(info);
            }
        }
            return infos;
        }
    }

    public void notifyObservers(@NonNull String methodName, @Nullable MethodInfo.Parameter... parameters)
    {
        notifyObservers(new MethodInfo (methodName, parameters));
    }

    public void notifyObservers(@NonNull MethodInfo info)
    {
        List<ObserverInfo> infos = getObserverInfos ();
        for (ObserverInfo oi : infos) {
        Observer observer = oi . weakObserver . get ();
        if (observer != null) {
            String key = helper . generateKey (info.getTag(), info.getName(), info.getParameterTypes());
            Method method = oi . methodMap . get (key);
            if (method != null) {
                Runnable runnable = helper . generateRunnable (observer, method, info);
                posterDispatcher.post(method, runnable);
            }
        }
    }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\Observe.java =====

package com.topdon.commons.observer;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @ interface Observe {
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\Observer.java =====

package com.topdon.commons.observer;

public interface Observer {

    @Observe
    default void onChanged(Object o)
    {
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\ObserverInfo.java =====

package com.topdon.commons.observer;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;

class ObserverInfo {
    final WeakReference<Observer> weakObserver;
    final Map<String, Method> methodMap;

    ObserverInfo(Observer observer, Map<String, Method> methodMap)
    {
        weakObserver = new WeakReference < > (observer);
        this.methodMap = methodMap;
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\observer\ObserverMethodHelper.java =====

package com.topdon.commons.observer;

import com.topdon.commons.poster.MethodInfo;
import com.topdon.commons.poster.Tag;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ObserverMethodHelper {
    private static final Map<Class<?>, Map<String, Method>> METHOD_CACHE = new ConcurrentHashMap<>();
    private boolean isObserveAnnotationRequired;

    ObserverMethodHelper(boolean isObserveAnnotationRequired)
    {
        this.isObserveAnnotationRequired = isObserveAnnotationRequired;
    }

    private static boolean contains(List<Method> methods, Method method)
    {
        for (Method m : methods) {
        if (m.getName().equals(method.getName()) && m.getReturnType().equals(method.getReturnType()) &&
            equalParamTypes(m.getParameterTypes(), method.getParameterTypes())
        ) {
            return true;
        }
    }
        return false;
    }

    private static boolean equalParamTypes(Class<?>[] params1, Class<?>[] params2)
    {
        if (params1.length == params2.length) {
            for (int i = 0; i < params1.length; i++) {
                if (params1[i] != params2[i])
                    return false;
            }
            return true;
        }
        return false;
    }

    void clearCache()
    {
        METHOD_CACHE.clear();
    }

    Runnable generateRunnable(Observer observer, Method method, MethodInfo info)
    {
        MethodInfo.Parameter[] parameters = info . getParameters ();
        if (parameters == null || parameters.length == 0) {
            return () -> {
                try {
                    method.invoke(observer);
                } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            };
        } else {
            final Object [] params = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                MethodInfo.Parameter parameter = parameters [i];
                params[i] = parameter.getValue();
            }
            return () -> {
                try {
                    method.invoke(observer, params);
                } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            };
        }
    }

    String generateKey(String tag, String name, Class<?>[] paramTypes)
    {
        StringBuilder sb = new StringBuilder();
        if (tag.isEmpty()) {
            sb.append(name);
        } else {
            sb.append(tag);
        }
        for (Class<?> type : paramTypes) {
        sb.append(",").append(type);
    }
        return sb.toString();
    }

    Map<String, Method> findObserverMethod(Observer observer)
    {
        Map<String, Method> map = METHOD_CACHE . get (observer.getClass());
        if (map != null) {
            return map;
        }
        map = new HashMap < > ();
        List<Method> methods = new ArrayList<>();
        Class<?> cls = observer . getClass ();
        while (cls != null && !cls.isInterface() && Observer.class. isAssignableFrom (cls)) {
            Method[] ms = null;
            try {
                ms = cls.getDeclaredMethods();
            } catch (Throwable ignore) {
            }
            if (ms != null) {
                for (Method m : ms) {
                    int ignore = Modifier . ABSTRACT | Modifier . STATIC | 0x40 | 0x1000;
                    if ((m.getModifiers() & Modifier.PUBLIC) != 0 && (m.getModifiers() & ignore) == 0 && !contains(methods, m)) {
                    methods.add(m);
                }
                }
            }
            cls = cls.getSuperclass();
        }
        for (Method method : methods) {
        Observe anno = method . getAnnotation (Observe.class);
        if (anno != null || !isObserveAnnotationRequired) {
            Tag tagAnno = method . getAnnotation (Tag.class);
            String tag = tagAnno == null ? "" : tagAnno.value();
            String key = generateKey (tag, method.getName(), method.getParameterTypes());
            map.put(key, method);
        }
    }
        if (!map.isEmpty()) {
            METHOD_CACHE.put(observer.getClass(), map);
        }
        return map;
    }
}