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
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;

/**
 * @author Rafał Krupiński
 */
public class Fields {
    public static <T extends Annotation> Collection<Field> findAll(Class<?> type, Class<T> anno) {
        LinkedList<Field> result = new LinkedList<Field>();
        findAll(type, anno, result);
        return result;
    }

    private static <T extends Annotation> void findAll(Class<?> type, Class<T> anno, Collection<Field> result) {
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(anno))
                result.add(field);
        }
        Class<?> s = type.getSuperclass();
        if (s != null)
            findAll(s, anno, result);
    }
}
