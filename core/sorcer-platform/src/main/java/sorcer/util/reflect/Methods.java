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

package sorcer.util.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Rafał Krupiński
 */
public class Methods {
    /**
     * Find and return the first method annotated with given annotation type.
     * Methods are searched first in the current class, then  in the superclasses.
     *
     * @param type Class which is searched for the annotated methods
     * @param anno annotation type
     */
    public static Method findFirst(Class<?> type, Class<? extends Annotation> anno) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(anno))
                return method;
        }
        Class<?> superclass = type.getSuperclass();

        if (superclass != Object.class)
            return findFirst(superclass, anno);
        else
            return null;
    }

    public static <T extends Annotation> Collection<Method> findAll(Class<?> type, Class<T> anno) {
        List<Method> result = new LinkedList<Method>();
        findAll(type, anno, result);
        return result;
    }

    private static <T extends Annotation> void findAll(Class<?> type, Class<T> anno, Collection<Method> result) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(anno))
                result.add(method);
        }
        Class<?> s = type.getSuperclass();
        if (s != null)
            findAll(s, anno, result);
    }
}
