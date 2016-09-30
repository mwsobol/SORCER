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

import groovy.text.SimpleTemplateEngine
import sorcer.generator.Options
import static sorcer.generator.tasks.GeneratorUtil.*

/**
 * Tasks for generating requestor projects.
 *
 * @author Dennis Reedy
 */
class RequestorGeneratorTasks {

    static class CreateRequestorGradleBuild implements GeneratorTask {
        Options options
        String result = "CreateRequestorGradleBuild pending"

        CreateRequestorGradleBuild(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }

        void exec() {
            String indent = "    "
            String versionIndent = "            "
            StringBuilder projectDeps = new StringBuilder()
            StringBuilder deployVersions = new StringBuilder()
            StringBuilder startme = new StringBuilder()
            startme.append("\"install\"")
            StringBuilder stopme = new StringBuilder()
            Set deployedProviders = new HashSet()

            options.providers.each { method, metaData ->
                String provider = metaData[Options.ProviderData.NAME]

                String depToAdd = "compile project(\':${provider}-api\')"
                if(!projectDeps.contains(depToAdd)) {
                    if(projectDeps.length()>0)
                        projectDeps.append("\n")
                    projectDeps.append(indent).append(depToAdd)
                }

                String deployVersionToAdd ="it.replace(\'${provider}.version\', project(\':${provider}-api\').version)"
                if(!deployVersions.contains(deployVersionToAdd)) {
                    if(deployVersions.length()>0)
                        deployVersions.append("\n")
                    deployVersions.append(versionIndent).append(deployVersionToAdd)
                }

                boolean toBeDeployed = Boolean.parseBoolean(metaData[Options.ProviderData.DEPLOY]) ||
                                       deployedProviders.contains(provider)
                if(toBeDeployed)
                    deployedProviders << provider
                String startMeToAdd = "\":${provider}-provider:startme\""
                if(!startme.contains(startMeToAdd) && !toBeDeployed) {
                    startme.append(", ").append(startMeToAdd)
                }

                String stopMeToAdd = "\":${provider}-provider:stopme\""
                if(!stopme.contains(stopMeToAdd) && !toBeDeployed) {
                    if(stopme.length()>0)
                        stopme.append(", ")
                    stopme.append(stopMeToAdd)
                }
            }

            StringBuilder finalizedBy = new StringBuilder()
            if(stopme.length()>0)
                finalizedBy.append("\n${indent}finalizedBy (${stopme.toString()})\n")

            String engTestDep
            String engLibDep
            if(options.group.endsWith("open")) {
                engTestDep = "project(':eng-test')"
                engLibDep = "project(':eng-lib')"
            } else {
                engTestDep = "\"mil.afrl.mstc.open:eng-test:\${engOpenVersion}\""
                engLibDep = "\"mil.afrl.mstc.open:eng-lib:\${engOpenVersion}\""
            }

            def binding = [myVersion        : options.version,
                           projectDeps      : projectDeps.toString(),
                           testDep          : engTestDep,
                           filteredVersions : deployVersions.toString(),
                           engLibDep        : engLibDep,
                           projectDataPath  : '${project.projectDir.path}/data',
                           startme          : startme.toString(),
                           finalizedBy      : finalizedBy.toString(),
                           name             : options.name.toLowerCase(),
                           codebase         : 'artifact:${project.group}/'+options.name.toLowerCase()+'-req/$version',
                           requestorClass   : "${getApiPackageName(options)}.requestor.${options.className.capitalize()}Requestor"
            ]
            def url = this.getClass().getResource("/generator/requestor/build.gradle")
            String text = url.text
            def engine = new SimpleTemplateEngine()
            String transform = engine.createTemplate(text).make(binding)
            File requestorRoot = getRequestorRoot(options)
            requestorRoot.mkdirs()
            File buildGradle = new File(requestorRoot, "build.gradle")
            buildGradle.text = transform
            result = "Generated ${buildGradle.path}"
        }
    }

    static class CreateRequestorProperties implements GeneratorTask {
        Options options
        String result

        CreateRequestorProperties(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }

        void exec() {
            StringBuilder deployConfigs = new StringBuilder()
            def providers = []
            options.providers.each { method, metaData ->
                String provider = metaData[Options.ProviderData.NAME]
                if(!providers.contains(provider)) {
                    String deployConfig = "${options.group}:${provider}-provider:config:${provider}.version"
                    if (deployConfigs.length() > 0)
                        deployConfigs.append("\n")
                    deployConfigs.append("${provider}-deploy-config=${deployConfig}")
                    providers << provider
                }
            }
            def binding = [deployConfigs : deployConfigs.toString(),
                           capName       : options.name.capitalize()]
            def url = this.getClass().getResource("/generator/requestor/requestor.properties")
            String text = url.text
            def engine = new SimpleTemplateEngine()
            String transform = engine.createTemplate(text).make(binding)
            File requestorConfig = new File(getRequestorRoot(options), "src/main/resources/")
            requestorConfig.mkdirs()
            File requestorProps = new File(requestorConfig, "${options.name.toLowerCase()}-requestor.properties")
            requestorProps.text = transform
            result = "Generated ${requestorProps.path}"
        }
    }

    static class CreateRequestorData implements GeneratorTask {
        Options options
        String result

        CreateRequestorData(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }

        void exec() {
            File requestorDataDir = new File(getRequestorRoot(options), "data")
            requestorDataDir.mkdirs()
            result = "Created ${requestorDataDir.path}"
        }
    }

    static class CreateRequestor implements GeneratorTask {
        Options options
        String result

        CreateRequestor(Options options) {
            this.options = options
        }

        String describe() {
            return result
        }

        void exec() {
            String indent = "        "
            String packageName = getApiPackageName(options)
            StringBuilder imports = new StringBuilder()
            StringBuilder providerList = new StringBuilder()
            StringBuilder netTaskGeneration = new StringBuilder()
            def taskFactory = []

            options.providers.each { method, metaData ->
                String provider = metaData[Options.ProviderData.NAME]
                String toImport = "import ${metaData[Options.ProviderData.INTERFACE]};"
                if (!imports.contains(toImport)) {
                    if (imports.length() > 0)
                        imports.append("\n")
                    imports.append(toImport)
                }
                String providerListToAdd = '<li>@{link ' + metaData[Options.ProviderData.INTERFACE] + '}</li>'
                if (!providerList.contains(providerListToAdd)) {
                    if (providerList.length() > 0)
                        providerList.append("\n")
                    providerList.append(" * ${providerListToAdd}")
                }
                if (netTaskGeneration.length() > 0)
                    netTaskGeneration.append("\n");

                if (!taskFactory.contains(provider)) {
                    String methodFirstLine = "${indent}TaskFactory ${provider}TaskFactory = new TaskFactory("
                    String methodIndent = createIndent(methodFirstLine.length());
                    netTaskGeneration.append("${methodFirstLine}Sorcer.getActualName(\"Engineering-${provider.capitalize()}\"),\n")
                    netTaskGeneration.append("${methodIndent}${metaData[Options.ProviderData.SIMPLE_NAME]}.class,\n")
                    netTaskGeneration.append("${methodIndent}properties.getProperty(\"${provider}-deploy-config\"));\n")
                    netTaskGeneration.append("\n")
                    taskFactory << provider
                }
                netTaskGeneration.append("${indent}Context ${method.getName()}Context = new ServiceContext();\n")
                String taskLine = "${indent}NetTask ${method.getName()} = ${provider}TaskFactory.createNetTask("
                String methodIndent2 = createIndent(taskLine.length());
                netTaskGeneration.append("${taskLine}\"${method.getName()}\",")
                boolean useSpace = Boolean.parseBoolean(metaData[Options.ProviderData.USE_SPACE])
                boolean deploy = Boolean.parseBoolean(metaData[Options.ProviderData.DEPLOY])
                if(useSpace || deploy) {
                    netTaskGeneration.append("\n${methodIndent2}${method.getName()}Context,\n")
                    if(deploy) {
                        netTaskGeneration.append("${methodIndent2}ProviderStrategy.DEPLOY")
                        if(useSpace)
                            netTaskGeneration.append("\n${methodIndent2}ProviderStrategy.PULL);\n")
                        else
                            netTaskGeneration.append(");\n")
                    } else {
                        netTaskGeneration.append("${methodIndent2}ProviderStrategy.PULL);\n")
                    }
                } else {
                    netTaskGeneration.append(" ${method.getName()}Context);\n")
                }
            }

            def binding = [logger              : 'LoggerFactory.getLogger('+options.className.capitalize()+'Requestor.class.getName())',
                           requestorPackage    : "${packageName}.requestor",
                           imports             : imports.toString(),
                           props               : "${options.name.toLowerCase()}-requestor.properties",
                           providerList        : providerList.toString(),
                           netTaskGeneration   : netTaskGeneration.toString(),
                           reqName             : "${options.className.capitalize()}Requestor",
                           logger              : 'LoggerFactory.getLogger('+options.className.capitalize()+'Requestor.class)'
                          ]
            def url = this.getClass().getResource("/generator/requestor/requestor.java")
            String text = url.text
            def engine = new SimpleTemplateEngine()
            String transform = engine.createTemplate(text).make(binding)
            File requestorDir = new File(getRequestorRoot(options),
                                         "src/main/java/${getPackageRoot(options)}/requestor")
            requestorDir.mkdirs()
            File requestor = new File(requestorDir, "${options.className.capitalize()}Requestor.java")
            requestor.text = transform
            result = "Generated ${requestor.path}"
        }

        static String createIndent(int size) {
            StringBuilder b = new StringBuilder()
            for(int i=0; i<size; i++)
                b.append(" ")
            b.toString()
        }
    }
}
