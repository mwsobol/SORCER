/*
 * Copyright 2014 Sorcersoft.com S.A.
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

package sorcer.util;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * @author Rafał Krupiński
 */
public class ClassLoaders {
    public static void doWith(ClassLoader cl, final Runnable runnable) {
        try {
            doWith(cl, new Callable<Void, Exception>() {
                @Override
                public Void call() throws Exception {
                    runnable.run();
                    return null;
                }
            });
        } catch (RuntimeException e) {
            throw e;

        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    public static <T> T doWith(ClassLoader cl, final java.util.concurrent.Callable<T> callable) throws Exception {
        return doWith(cl, new Callable<T, Exception>() {
            @Override
            public T call() throws Exception {
                return callable.call();
            }
        });
    }

    public static <T, E extends Throwable> T doWith(ClassLoader cl, Callable<T, E> callable) throws E {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(cl);
        try {
            return callable.call();
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    public static interface Callable<V, E extends Throwable> {
        V call() throws E;
    }

    public static ClassLoader current() {
        return Thread.currentThread().getContextClassLoader();
    }
}

