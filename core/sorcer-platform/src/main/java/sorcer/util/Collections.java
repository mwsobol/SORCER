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

import java.util.*;

/**
 * @author Rafał Krupiński
 */
public class Collections {
    /**
     * Makes an arry from the parameter enumeration <code>e</code>.
     *
     * @param e an enumeration
     * @return an arry of objects in the underlying enumeration <code>e</code>
     */
    static public Object[] makeArray(final Enumeration e) {
        List<Object> objs = new LinkedList<Object>();
        while (e.hasMoreElements()) {
            objs.add(e.nextElement());
        }
        return objs.toArray();
    }

    public static <T> Iterable<T> i(final Enumeration<T> e) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    @Override
                    public boolean hasNext() {
                        return e.hasMoreElements();
                    }

                    @Override
                    public T next() {
                        return e.nextElement();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public static <T> void copy(Iterator<T> src, T[] target, int beginIndex, int endIndex) {
        if (endIndex > target.length) throw new ArrayIndexOutOfBoundsException(endIndex);
        for (int i = beginIndex; i < endIndex; i++) {
            if (!src.hasNext()) throw new IndexOutOfBoundsException();
            target[i] = src.next();
        }
    }

    public static Map<String, String> toMap(Properties properties) {
        Map<String, String> result = new HashMap<String, String>();
        toMap(properties, result);
        return result;
    }

    public static void toMap(Properties properties, Map<String, String> result) {
        for (String key : properties.stringPropertyNames())
            result.put(key, properties.getProperty(key));
    }

    public static Properties toProperties(Map<String, String> source) {
        Properties p;
        if ((Map) source instanceof Properties)
            p = (Properties) (Map) source;
        else {
            p = new Properties();
            p.putAll(source);
        }
        return p;
    }
}
