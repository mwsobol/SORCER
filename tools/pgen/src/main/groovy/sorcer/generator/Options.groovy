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
package sorcer.generator

import java.lang.reflect.Method

/**
 * Project Generator options
 *
 * Created by dreedy on 1/14/15.
 */
class Options {
    enum ProviderData {NAME, INTERFACE, SIMPLE_NAME, USE_SPACE, DEPLOY}

    File projectRootDir
    File location
    String name
    String version
    String group
    String packageName
    String className
    boolean provider
    boolean requestor
    Map<Method, Map<ProviderData, String>> providers = [:]

    String getCapName() {
        return className.capitalize()
    }
}
