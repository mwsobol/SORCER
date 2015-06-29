package sorcer.util;
/**
 * Copyright 2013, 2014 Sorcersoft.com S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Rafał Krupiński
 */
public class ConfigurableThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final static String defaultNameFormat = "pool-%d-thread-%d";
    private final static Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = new LoggingExceptionHandler();

    private final int thisPoolNumber;
    private ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private boolean daemon = false;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = defaultUncaughtExceptionHandler;
    private String nameFormat = defaultNameFormat;
    private ClassLoader classLoader;
    private int priority = Thread.currentThread().getPriority();

    public ConfigurableThreadFactory() {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        thisPoolNumber = poolNumber.getAndIncrement();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = String.format(nameFormat, thisPoolNumber, threadNumber.getAndIncrement());
        Thread thread = new Thread(group, runnable, name);
        thread.setDaemon(daemon);

        if (classLoader != null)
            thread.setContextClassLoader(classLoader);
        thread.setPriority(priority);

        thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);

        return thread;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    /**
     * @param nameFormat    format taking 2 parameters - global pool number and thread number per thread factory instance
     */
    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
    }

    public void setThreadGroup(ThreadGroup group) {
        this.group = group;
    }

    public void setContextClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
