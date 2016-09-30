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
package sorcer

import java.lang.reflect.Method
import java.lang.reflect.Parameter

/**
 * Some helpful utilities
 *
 * @author Dennis Reedy
 */
class Util {

    static String getCompoundName(Method m, String className) {
        StringBuilder parameters = new StringBuilder();
        for(Parameter parameter : m.getParameters()) {
            if(parameters.length()>0)
                parameters.append(", ");
            parameters.append(parameter.getType().getSimpleName());
        }
        int ndx = className.lastIndexOf('.');
        String simpleName = className.substring(ndx+1);
        return String.format("%s.%s(%s)", simpleName, m.getName(), parameters.toString());
    }
}
