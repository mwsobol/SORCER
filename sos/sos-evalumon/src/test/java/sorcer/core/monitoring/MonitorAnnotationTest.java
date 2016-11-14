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

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;

/**
 * @author Dennis Reedy
 */
public class MonitorAnnotationTest {
    @Test
    public void verifyPresent() {
        for(Method m : Foo.class.getMethods()) {
            if(m.getName().equals("hasAnnotation")) {
                assertTrue(m.getAnnotationsByType(Monitored.class)!=null);
            }
            if(m.getName().equals("noAnnotation")) {
                assertTrue(m.getAnnotationsByType(Monitored.class)==null);
            }
        }
    }

    private class Foo {
        @Monitored
        void hasAnnotation() {

        }

        void noAnnotation() {

        }
    }
}
