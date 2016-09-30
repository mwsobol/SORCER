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
 * Append to settings.gradle the newly generated project(s)
 *
 * @author Dennis Reedy
 */
class GitSettingsUpdater implements GeneratorTask {
    final Options options
    String result

    GitSettingsUpdater(Options options) {
        this.options = options
    }

    @Override
    void exec() {
        String relativePath = ""
        if(options.location.path!=options.projectRootDir.path)
            relativePath = "${options.location.path.substring(options.projectRootDir.path.length()+1)}/"
        StringBuilder appender = new StringBuilder()
        appender.append("\n")
        String name = options.name
        if(options.getProvider()) {
            ["api", "provider"].each { module ->
                appender.append("include \"${name.toLowerCase()}-${module}\"\n")
                appender.append("project(\":${name.toLowerCase()}-${module}\").projectDir = file(\"${relativePath}${name}/${module}\")\n")
            }
        } else {
            appender.append("include \"${name.toLowerCase()}-req\"\n")
            appender.append("project(\":${name.toLowerCase()}-req\").projectDir = file(\"${relativePath}${name}-req\")\n")
        }
        File settings = new File(options.projectRootDir, "settings.gradle")
        if(settings.exists()) {
            settings.append(appender.toString())
            result = "Updated ${settings.path}"
        } else {
            result = "Skipped updating settings.gradle, file does not exist."
        }
    }

    @Override
    String describe() {
        return result
    }
}
