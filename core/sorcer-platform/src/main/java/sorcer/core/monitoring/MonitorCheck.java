/*
 * Copyright to the original author or authors.
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
package sorcer.core.monitoring;

import sorcer.service.Exerter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Check for the {@link Monitored} annotation.
 *
 * @author Dennis Reedy
 */
public class MonitorCheck {

    public static boolean monitor(Method m) {
        boolean monitor = false;
        for(Annotation a : m.getAnnotations()) {
            if(a instanceof Monitored) {
                monitor = true;
                break;
            }
        }
        return monitor;
    }

    public static boolean check(Method m) {
        return !(m.getName().equals("destroy") && m.getDeclaringClass().getName().equals(Exerter.class.getName()));
    }
}
