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
package sorcer.generator.tasks

import sorcer.generator.Options

/**
 * Utilities for use when generating project content
 *
 * @author Dennis Reedy
 */
class GeneratorUtil {

    static File getApiRoot(Options options) {
        return new File(options.location, options.name+"/api")
    }

    static File getApiDir(Options options) {
        File apiRoot = getApiRoot(options)
        String packageRoot = options.packageName.replaceAll("\\.", "/")
        return new File(apiRoot, "src/main/java/${packageRoot}")
    }

    static String getPackageRoot(Options options) {
        return options.packageName.replaceAll("\\.", "/")
    }

    static File getProviderRoot(Options options) {
        return new File(options.location, options.name + "/provider")
    }

    static File getProviderDir(Options options) {
        String packageRoot = getPackageRoot(options)
        File providerRoot = getProviderRoot(options)
        return new File(providerRoot, "src/main/java/${packageRoot}/provider")
    }

    static String getApiPackageName(Options options) {
        return "${options.packageName}"
    }

    static String getProviderPackageName(Options options) {
        return "${getApiPackageName(options)}.provider"
    }

    static File getRequestorRoot(Options options) {
        return new File(options.location, options.name + "-req")
    }

}
