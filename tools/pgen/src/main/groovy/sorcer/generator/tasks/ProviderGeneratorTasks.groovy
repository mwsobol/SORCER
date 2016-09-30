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
import groovy.text.SimpleTemplateEngine
import static sorcer.generator.tasks.GeneratorUtil.*

/**
 * Tasks for generating provider projects.
 *
 * Created by dreedy on 1/15/15.
 */
class ProviderGeneratorTasks {

    static class CreateApiDirectory implements GeneratorTask {
        Options options
        File apiRoot
        String result

        CreateApiDirectory(Options options) {
            this.options = options
            this.apiRoot = new File(options.location, options.name+"/api");
        }

        String describe() {
            return result
        }
        void exec() {
            File apiDirs = getApiDir(options)
            if(apiDirs.mkdirs())
                result = String.format("Created provider api directory %s", apiDirs.getPath())
            else
                result = String.format("Failed to create provider api directory %s", apiDirs.getPath())
        }
    }

    static class CreateProviderDataDirectory implements GeneratorTask {
        Options options
        String result

        CreateProviderDataDirectory(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }
        void exec() {
            File dataDir = new File(getProviderRoot(options), "data")
            if(dataDir.mkdirs())
                result = String.format("Created provider data directory %s", dataDir.getPath())
            else
                result = String.format("Failed to create provider data directory %s", dataDir.getPath())
        }
    }

    static class CreateProviderDataInputDirectoryAndContent implements GeneratorTask {
        Options options
        String result

        CreateProviderDataInputDirectoryAndContent(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }
        void exec() {
            File dataDir = new File(getProviderRoot(options), "data/input")
            if(dataDir.mkdirs()) {
                result = String.format("Created provider data directory %s", dataDir.getPath())
                File inputData = new File(dataDir, "input.txt")
                inputData.text = "Greetings from ${options.name.capitalize()}"
            } else {
                result = String.format("Failed to create provider data directory %s", dataDir.getPath())
            }
        }
    }

    static class CreateApiGradleBuild implements GeneratorTask {
        Options options
        String result

        CreateApiGradleBuild(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }

        void exec() {
            String engLibDep
            if(options.group.endsWith("open")) {
                engLibDep = "project(':eng-lib')"
            } else {
                engLibDep = "\"mil.afrl.mstc.open:eng-test:\${engOpenVersion}\""
            }
            def binding = ["version" : options.version,
                           engLibDep : engLibDep]
            def url = this.getClass().getResource("/generator/api/build.gradle")
            String text = url.text
            def engine = new SimpleTemplateEngine()
            String transform = engine.createTemplate(text).make(binding)
            File apiRoot = getApiRoot(options)
            File buildGradle = new File(apiRoot, "build.gradle")
            buildGradle.text = transform
            result = "Generated ${buildGradle.path}"
        }
    }

    static class CreateApiInterface implements GeneratorTask {
        Options options
        String result

        CreateApiInterface(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }

        void exec() {
            String packageName = getApiPackageName(options)
            def binding = ["packageName": packageName,
                           interfaceName: options.className.capitalize(),
                           name         : options.className.capitalize()]
            def url = this.getClass().getResource("/generator/api/api.java")
            String text = url.text
            def engine = new SimpleTemplateEngine()
            String transform = engine.createTemplate(text).make(binding)
            File interfaceDir = getApiDir(options)
            File apiInterface = new File(interfaceDir, "${options.className.capitalize()}.java")
            apiInterface.text = transform
            result = "Generated ${apiInterface.path}"
        }
    }

    static class CreateApiContext implements GeneratorTask {
        Options options
        String result

        CreateApiContext(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }

        void exec() {
            String packageName = getApiPackageName(options)
            def binding = ["packageName": packageName,
                           contextName  : "${options.className.capitalize()}Context"]
            def url = this.getClass().getResource("/generator/api/context.java")
            String text = url.text
            def engine = new SimpleTemplateEngine()
            String transform = engine.createTemplate(text).make(binding)
            File interfaceDir = getApiDir(options)
            File context = new File(interfaceDir, "${options.className.capitalize()}Context.java")
            context.text = transform
            result = "Generated ${context.path}"
        }
    }

    static class CreateProviderDirectory implements GeneratorTask {
        Options options
        String result

        CreateProviderDirectory(Options options) {
            this.options = options
        }

        String describe() {
            result
        }

        void exec() {
            File providerDirs = getProviderDir(options)
            if(providerDirs.mkdirs())
                result = String.format("Created provider directory %s", providerDirs.getPath())
            else
                result = String.format("Failed to create provider directory %s", providerDirs.getPath())
        }
    }

    static class CreateProviderGradleBuild implements GeneratorTask {
        Options options
        String result = "CreateProviderGradleBuild pending"

        CreateProviderGradleBuild(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }

        void exec() {
            String engTestDep
            String engLibDep
            if(options.group.endsWith("open")) {
                engTestDep = "project(':eng-test')"
                engLibDep = "project(':eng-lib')"
            } else {
                engTestDep = "\"mil.afrl.mstc.open:eng-test:\${engOpenVersion}\""
                engLibDep = "\"mil.afrl.mstc.open:eng-lib:\${engOpenVersion}\""
            }
            def binding = [apiName      : "${options.name.toLowerCase()}-api",
                           capName      : options.name.capitalize(),
                           name         : options.name.toLowerCase(),
                           myVersion    : options.version,
                           engTestDep   : engTestDep,
                           engLibDep    : engLibDep,
                           dataDir      : '${project.projectDir.path}/data',
                           deploy       : 'build/config/${project.name}-deploy.config',
                           codebase     : '${project.group}:'+options.name.toLowerCase()+'-api:$version',
                           testCodebase : 'artifact:${project.group}/'+options.name.toLowerCase()+'-api/$version',
                           deployConfig : '${project.group}:'+options.name.toLowerCase()+'-provider:config:$version',
                           projectPath  : '${project.projectDir.path}/configs/startup.config',
                           providerClass  : "${getApiPackageName(options)}.provider.${options.className.capitalize()}ProviderImpl"
                           ]
            def url = this.getClass().getResource("/generator/provider/build.gradle")
            String text = url.text
            def engine = new SimpleTemplateEngine()
            String transform = engine.createTemplate(text).make(binding)
            File providerRoot = getProviderRoot(options)
            File buildGradle = new File(providerRoot, "build.gradle")
            buildGradle.text = transform
            result = "Generated ${buildGradle.path}"
        }
    }

    static class CreateProviderStarterConfig implements GeneratorTask {
        Options options
        String result

        CreateProviderStarterConfig(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }

        void exec() {
            def binding = ["name"     : options.name.toLowerCase(),
                           "capName"  : options.name.capitalize(),
                           "apiClass" : "${getApiPackageName(options)}.${options.className.capitalize()}.class"]
            def url = this.getClass().getResource("/generator/provider/startup.config")
            String text = url.text
            def engine = new SimpleTemplateEngine()
            String transform = engine.createTemplate(text).make(binding)
            File providerConfigDir = new File(getProviderRoot(options), "configs")
            providerConfigDir.mkdirs()
            File startup = new File(providerConfigDir, "startup.config")
            startup.text = transform
            result = "Generated ${startup.path}"
        }
    }

    static class CreateProviderImpl implements GeneratorTask {
        Options options
        String result

        CreateProviderImpl(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }

        void exec() {
            String packageName = getApiPackageName(options)
            def binding = ["providerPackage"  : getProviderPackageName(options),
                           "apiClass"         : "${packageName}.${options.className.capitalize()}",
                           "apiContext"       : "${packageName}.${options.className.capitalize()}Context",
                           "name"             : options.name.toLowerCase(),
                           "apiClassName"     : options.className.capitalize(),
                           "scratchDir"       : "${options.name.toLowerCase()}_execute_",
                           "contextClassName" : "${options.className.capitalize()}Context",
                           "contextName"      : "${options.className.toLowerCase()}Context",
                           "setOutput"        : "${options.className.toLowerCase()}Context.setOutput",
                           "getInput"         : "${options.className.toLowerCase()}Context.getInput",
                           "providerImpl"     : "${options.className.capitalize()}ProviderImpl",
                           "logger"           : 'LoggerFactory.getLogger('+options.className.capitalize()+'ProviderImpl.class)']
            def url = this.getClass().getResource("/generator/provider/provider.java")
            String text = url.text
            def engine = new SimpleTemplateEngine()
            String transform = engine.createTemplate(text).make(binding)
            File providerDir = getProviderDir(options)
            File providerImpl = new File(providerDir, "${options.className.capitalize()}ProviderImpl.java")
            providerImpl.text = transform
            result = "Generated ${providerImpl.path}"
        }
    }

    static class CreateProviderProperties implements GeneratorTask {
        Options options
        String result

        CreateProviderProperties(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }

        void exec() {
            String homeDir
            if(options.group.contains("open"))
                homeDir = '${eng.home}'
            else if(options.group.contains("gov"))
                homeDir = '${eng.gov.home}'
            else
                homeDir = '${eng.itar.home}'
            def binding = ["name"     : options.name.toLowerCase(),
                           "home"     : homeDir]
            def url = this.getClass().getResource("/generator/provider/provider.properties")
            String text = url.text
            def engine = new SimpleTemplateEngine()
            String transform = engine.createTemplate(text).make(binding)
            File providerResources = new File(getProviderRoot(options), "src/main/resources/configs/${options.name.toLowerCase()}")
            providerResources.mkdirs()
            File providerProps = new File(providerResources, "provider.properties")
            providerProps.text = transform
            result = "Generated ${providerProps.path}"
        }
    }

    static class CreateProviderTest implements GeneratorTask {
        Options options
        String result

        CreateProviderTest(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }

        void exec() {
            String packageName = getApiPackageName(options)
            String relativePath = ""
            if(options.location.path!=options.projectRootDir.path)
                relativePath = "${options.location.path.substring(options.projectRootDir.path.length()+1)}/"
            def binding = ["providerPackage"    : "${packageName}.provider",
                           "apiClass"           : "${options.className.capitalize()}.class",
                           "apiPackageAndClass" : "${packageName}.${options.className.capitalize()}",
                           "engProviderName"    : "Engineering-${options.name.capitalize()}",
                           "name"               : options.name.toLowerCase(),
                           "capName"            : options.name.capitalize(),
                           "contextClass"       : "${packageName}.${options.className.capitalize()}Context",
                           "contextName"        : "${options.className.capitalize()}Context",
                           "relativePath"       : "${relativePath}${options.name.toLowerCase()}",
                           "className"          : "${options.className.capitalize()}Tester",
                           "setContext"         : "${options.className.toLowerCase()}Task.setContext",
                           "exert"              : "${options.className.toLowerCase()}Task.exert",
                           "taskName"           : "${options.className.toLowerCase()}Task",
                           "reqName"            : "${options.className.capitalize()}Tester",
                           "logger"             : 'LoggerFactory.getLogger('+options.className.capitalize()+'Tester.class)']
            def url = this.getClass().getResource("/generator/provider/test.java")
            String text = url.text
            def engine = new SimpleTemplateEngine()
            String transform = engine.createTemplate(text).make(binding)
            File providerDir = new File(getProviderRoot(options),
                                         "src/test/java/${getPackageRoot(options)}/provider")
            providerDir.mkdirs()
            File requestor = new File(providerDir, "${options.className.capitalize()}Tester.java")
            requestor.text = transform
            result = "Generated ${requestor.path}"
        }
    }
}