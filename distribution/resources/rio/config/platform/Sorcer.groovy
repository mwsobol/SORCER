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

import org.rioproject.config.PlatformCapabilityConfig

import groovy.util.logging.Slf4j

/**
 * Declares Sorcer capability in the platform
 */
@Slf4j
class SorcerPlatformConfig {

    def getPlatformCapabilityConfigs() {
        def configs = []
        String sorcerHome = System.getProperty("sorcer.home", System.getenv("SORCER_HOME"))
        if(sorcerHome==null) {
            String scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
            sorcerHome =  new File(scriptDir, "../../..").absolutePath
            System.setProperty("sorcer.home", sorcerHome)
        }
        File sorcerHomeDir = new File(sorcerHome)
        if(sorcerHomeDir.exists()) {
            def jars = ["JE-"             : "lib/common | Sleepy Cat",
                        "javax.inject-"   : "lib/common | Javax Inject",
                        "guava-"          : "lib/common | Guava",
                        "plexus-utils-"   : "lib/common | Plexus Utils",
                        "commons-exec-"   : "lib/common | Apache Commons Exec",
                        "Sorcer-Platform" : "lib/sorcer/lib | Sorcer Platform"]
            jars.each { jar, data ->
                String[] parts = data.split("\\|")
                String dir = parts[0].trim()
                String name = parts[1].trim()
                File jarFile = getJar(new File(sorcerHomeDir, dir), jar.toLowerCase())
                if(jarFile.exists()) {
                    configs << new PlatformCapabilityConfig(jar,
                                                            getVersion(jar, jarFile.name),
                                                            name,
                                                            "",
                                                            jarFile.path)
                } else {
                    logger.error("The ${dir}/${jar} does not exist, cannot add to platform")
                }
            }

        } else {
            logger.error("The ${sorcerHomeDir.path} does not exist, cannot add Sorcer jars to platform")
        }
        return configs
    }

    File getJar(File dir, String prefix) {
        File jar
        for(File f : dir.listFiles()) {
            if(f.name.startsWith(prefix)) {
                jar = f
                break
            }
        }
        jar
    }

    String getVersion(String prefix, String name) {
        String base = name.substring(0, name.length()-".jar".length())
        return base.substring(prefix.length()+1)
    }
}
