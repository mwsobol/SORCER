/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package examples.sml.configs

import com.sun.jini.start.ServiceDescriptor
import org.rioproject.config.Component
import sorcer.provider.boot.SorcerServiceDescriptor

/**
 * Configuration to start all EOL example providers
 *
 * @author Dennis Reedy
 */
@Component("com.sun.jini.start")
class StartAll {

    ServiceDescriptor[] getServiceDescriptors() {
        String riverVersion = System.getProperty("river.version")
        String sorcerVersion = System.getProperty("sorcer.version")
        String policy = System.getProperty("java.security.policy")

        String relativeRepoPath = System.getProperty("relative.repo.path")
        String projectBuildDir = System.getProperty("project.build.dir")
        String buildLibPath = "${projectBuildDir}/libs";
        String configPath = "${projectBuildDir}/../configs"

        def descriptors = []
//        ["arithmetic", "adder", "multiplier", "divider", "subtractor", "averager", "exerter", "contexter"].each { provider ->
        ["adder", "adder-srvname", "multiplier", "divider", "subtractor", "averager", "exerter", "contexter"].each { provider ->
            def configArg = ["${configPath}/${provider}-prv.config"]
            def codebase = "${relativeRepoPath}/sml-${sorcerVersion}-dl.jar sorcer-dl-${sorcerVersion}.jar sorcer-ui-${sorcerVersion}.jar jsk-dl-${riverVersion}.jar"

            descriptors << new SorcerServiceDescriptor(codebase,
                                                       policy,
                                                       "${buildLibPath}/sml-${sorcerVersion}.jar",
                                                       "sorcer.core.provider.ServiceTasker",
                                                       configArg as String[])
        }
        return descriptors as ServiceDescriptor[]
    }
}	