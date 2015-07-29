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

/**
 * @author Rafał Krupiński
 */
public class Classes {
    public static boolean isInstanceOf(String typeName, Object object) {
        return isAssignableFrom(typeName, object.getClass());
    }

    public static boolean isAssignableFrom(String self, Class cls) {
        if (self.equals(cls.getName()))
            return true;
        Class s = cls.getSuperclass();
        if (s != null && isAssignableFrom(self, s))
            return true;
        for (Class i : cls.getInterfaces()) {
            if (isAssignableFrom(self, i))
                return true;
        }
        return false;
    }
}

