// Merged ALL .kt and .java files from the 'BleModule\src\main\java\com\topdon\commons\poster' directory and its subdirectories.
// Total files: 9 | Generated on: 2025-10-08 01:42:33


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\AsyncPoster.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

final class AsyncPoster implements Runnable, Poster {
    private final ExecutorService executorService;
    private final Queue<Runnable> queue;

    AsyncPoster(@NonNull ExecutorService executorService) {
        this.executorService = executorService;
        queue = new ConcurrentLinkedQueue < > ();
    }

    @Override
    public void enqueue(@NonNull Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null, cannot be enqueued");
        queue.add(runnable);
        executorService.execute(this);
    }

    @Override
    public void clear() {
        synchronized(this) {
            queue.clear();
        }
    }

    @Override
    public void run() {
        Runnable runnable = queue . poll ();
        if (runnable != null) {
            runnable.run();
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\BackgroundPoster.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

final class BackgroundPoster implements Runnable, Poster {
    private final ExecutorService executorService;
    private final Queue<Runnable> queue;
    private volatile boolean executorRunning;

    BackgroundPoster(@NonNull ExecutorService executorService) {
        this.executorService = executorService;
        queue = new ConcurrentLinkedQueue < > ();
    }

    @Override
    public void enqueue(@NonNull Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null, cannot be enqueued");
        synchronized(this) {
            queue.add(runnable);
            if (!executorRunning) {
                executorRunning = true;
                executorService.execute(this);
            }
        }
    }

    @Override
    public void clear() {
        synchronized(this) {
            queue.clear();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Runnable runnable = queue . poll ();
                if (runnable == null) {
                    synchronized(this) {
                        runnable = queue.poll();
                        if (runnable == null) {
                            executorRunning = false;
                            return;
                        }
                    }
                }
                runnable.run();
            }
        } finally {
            executorRunning = false;
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\MainThreadPoster.java =====

package com.topdon.commons.poster;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

final class MainThreadPoster extends Handler implements Poster {
    private final Queue<Runnable> queue;
    private boolean handlerActive;

    MainThreadPoster() {
        super(Looper.getMainLooper());
        queue = new ConcurrentLinkedQueue < > ();
    }

    @Override
    public void enqueue(@NonNull Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null, cannot be enqueued");
        synchronized(this) {
            queue.add(runnable);
            if (!handlerActive) {
                handlerActive = true;
                if (!sendMessage(obtainMessage())) {
                    throw new RuntimeException ("Could not send handler message");
                }
            }
        }
    }

    @Override
    public void clear() {
        synchronized(this) {
            queue.clear();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            while (true) {
                Runnable runnable = queue . poll ();
                if (runnable == null) {
                    synchronized(this) {
                        runnable = queue.poll();
                        if (runnable == null) {
                            handlerActive = false;
                            return;
                        }
                    }
                }
                runnable.run();
            }
        } finally {
            handlerActive = false;
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\MethodInfo.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;

public class MethodInfo {
    @NonNull
    private String name;
    @Nullable
    private Parameter[] parameters;
    @NonNull
    private String tag;

    public MethodInfo(@NonNull String name, @Nullable Parameter... parameters)
    {
        this(name, name, parameters);
    }

    public MethodInfo(@NonNull String name, @NonNull String tag, @Nullable Parameter... parameters)
    {
        this.name = name;
        this.tag = tag;
        this.parameters = parameters;
    }

    public MethodInfo(@NonNull String name, @Nullable Class<?>[] parameterTypes)
    {
        this(name, name, parameterTypes);
    }

    public MethodInfo(@NonNull String name, @NonNull String tag, @Nullable Class<?>[] parameterTypes)
    {
        this(name, tag, toParameters(parameterTypes));
    }

    public static MethodInfo valueOf(@NonNull Method method)
    {
        Tag annotation = method . getAnnotation (Tag.class);
        return new MethodInfo (method.getName(), annotation == null ? method.getName() : annotation.value(),
        method.getParameterTypes());
    }

    private static Parameter[] toParameters(Class<?>[] parameterTypes)
    {
        Parameter[] parameters = null;
        if (parameterTypes != null) {
            parameters = new Parameter [parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameters[i] = new Parameter (parameterTypes[i], null);
            }
        }
        return parameters;
    }

    @NonNull
    public String getName()
    {
        return name;
    }

    public void setName(@NonNull String name)
    {
        this.name = name;
    }

    @NonNull
    public String getTag()
    {
        return tag;
    }

    public void setTag(@NonNull String tag)
    {
        this.tag = tag;
    }

    @Nullable
    public Parameter[] getParameters()
    {
        return parameters;
    }

    public void setParameters(@Nullable Parameter[] parameters)
    {
        this.parameters = parameters;
    }

    @Nullable
    public Class<?>[] getParameterTypes()
    {
        if (parameters == null) {
            return null;
        } else {
            Class<?>[] types = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                types[i] = parameters[i].type;
            }
            return types;
        }
    }

    @Nullable
    public Object[] getParameterValues()
    {
        if (parameters == null) {
            return null;
        } else {
            Object[] values = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                values[i] = parameters[i].value;
            }
            return values;
        }
    }

    public static
    class Parameter {
        @Nullable
        private Object value ;
        @NonNull
        private Class<?> type;

        public Parameter(@NonNull Class<?> type, @Nullable Object value )
        {
            this.type = type;
            this.value = value;
        }

        @Nullable
        public Object getValue()
        {
            return value;
        }

        public void setValue(@Nullable Object value )
        {
            this.value = value;
        }

        @NonNull
        public Class<?> getType()
        {
            return type;
        }

        public void setType(@NonNull Class<?> type)
        {
            this.type = type;
        }
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\Poster.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;

interface Poster {

    void enqueue(@NonNull Runnable runnable);

    void clear();
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\PosterDispatcher.java =====

package com.topdon.commons.poster;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

public class PosterDispatcher {
    private final ThreadMode defaultMode;
    private final Poster backgroundPoster;
    private final Poster mainThreadPoster;
    private final ExecutorService executorService;
    private final Poster asyncPoster;

    public PosterDispatcher(@NonNull ExecutorService executorService, @NonNull ThreadMode defaultMode)
    {
        this.defaultMode = defaultMode;
        this.executorService = executorService;
        backgroundPoster = new BackgroundPoster (executorService);
        mainThreadPoster = new MainThreadPoster ();
        asyncPoster = new AsyncPoster (executorService);
    }

    public ThreadMode getDefaultMode()
    {
        return defaultMode;
    }

    public ExecutorService getExecutorService()
    {
        return executorService;
    }

    public void clearTasks()
    {
        backgroundPoster.clear();
        mainThreadPoster.clear();
        asyncPoster.clear();
    }

    public void shutdown()
    {
        clearTasks();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void post(@Nullable Method method, @NonNull Runnable runnable)
    {
        if (method != null) {
            RunOn annotation = method . getAnnotation (RunOn.class);
            ThreadMode mode = defaultMode;
            if (annotation != null) {
                mode = annotation.value();
            }
            post(mode, runnable);
        }
    }

    public void post(@NonNull ThreadMode mode, @NonNull Runnable runnable)
    {
        if (mode == ThreadMode.UNSPECIFIED) {
            mode = defaultMode;
        }
        switch(mode) {
            case MAIN :
            mainThreadPoster.enqueue(runnable);
            break;
            case POSTING :
            runnable.run();
            break;
            case BACKGROUND :
            backgroundPoster.enqueue(runnable);
            break;
            case ASYNC :
            asyncPoster.enqueue(runnable);
            break;
        }
    }

    public void post(@NonNull Object owner, @NonNull String methodName, @NonNull String tag,
    @Nullable MethodInfo.Parameter... parameters)
    {
        Class<?>[] classes = new Class[0];
        Object[] params = new Object[0];
        if (parameters != null) {
            params = new Object [parameters.length];
            classes = new Class [parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                MethodInfo.Parameter parameter = parameters [i];
                classes[i] = parameter.getType();
                params[i] = parameter.getValue();
            }
        }
        Method[] methods = owner . getClass ().getDeclaredMethods();
        Method tm = null;
        Method mm = null;
        for (Method method : methods) {
        Tag annotation = method . getAnnotation (Tag.class);
        if (annotation != null && !annotation.value().isEmpty() && annotation.value().equals(tag) &&
            equalParamTypes(method.getParameterTypes(), classes)
        ) {
            tm = method;
        }
        if (tm == null) {
            if (method.getName().equals(methodName) && equalParamTypes(method.getParameterTypes(), classes)) {
                mm = method;
            }
        } else {
            break;
        }
    }
        Method method = tm == null ? mm : tm;
        if (method == null) {
            return;
        }
        try {
            Object[] finalParams = params;
            post(method, () -> {
                try {
                    method.invoke(owner, finalParams);
                } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            });
        } catch (Exception ignore) {
        }
    }

    private boolean equalParamTypes(Class<?>[] params1, Class<?>[] params2)
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

    public void post(@NonNull final Object owner, @NonNull String methodName, @Nullable MethodInfo.Parameter... parameters)
    {
        post(owner, methodName, "", parameters);
    }

    public void post(@NonNull Object owner, @NonNull MethodInfo methodInfo)
    {
        post(owner, methodInfo.getName(), methodInfo.getTag(), methodInfo.getParameters());
    }
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\RunOn.java =====

package com.topdon.commons.poster;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @ interface RunOn {

    ThreadMode value () default ThreadMode.UNSPECIFIED;
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\Tag.java =====

package com.topdon.commons.poster;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @ interface Tag {
    String value () default "";
}


// ===== FROM: BleModule\src\main\java\com\topdon\commons\poster\ThreadMode.java =====

package com.topdon.commons.poster;

public enum ThreadMode {

    POSTING,

    MAIN,

    BACKGROUND,

    ASYNC,

    UNSPECIFIED
}