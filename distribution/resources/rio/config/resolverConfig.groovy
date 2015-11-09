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
import org.rioproject.RioVersion

/*
 * This file is used to configure the org.rioproject.resolver.Resolver, defining
 * the resolver jar file name as well as the remote Maven repositories that the
 * resolver will use. The order in which these repositories are listed is the
 * order in which they will be searched.
 *
 * If you have configured repositories into your ~/.m2/settings.xml, those repositories
 * will be appended to this repositories listed below.
 *
 * Add or remove entries as needed.
 */
	
String hostName = InetAddress.getLocalHost().getHostName()
String address = InetAddress.getLocalHost().getHostAddress()
boolean onEnclave = (hostName.endsWith("wpafb.af.mil") || address.startsWith("10.131"))
	
resolver {
    jar = "${rioHome()}/lib/resolver/resolver-aether-${RioVersion.VERSION}.jar"

    repositories {
        if (onEnclave) {
            remote = ["repo": "http://10.131.7.138:7001"]
        } else {
            remote = ["rio"    : "http://www.rio-project.org/maven2",
                      "central": "http://repo1.maven.org/maven2"]
        }
        flatDirs = [new File(sorcerHome() as String, "lib/sorcer/lib"),
                    new File(sorcerHome() as String, "lib/sorcer/lib-dl"),
                    new File(sorcerHome() as String, "lib/common"),
                    new File(sorcerHome() as String, "lib/blitz"),
                    new File(rioHome() as String, "lib-dl"),
                    new File(rioHome() as String, "config/poms"),
                    new File(rioHome() as String, "lib")]
    }   
}

def sorcerHome() {
    return System.getProperty("sorcer.home")
}

def rioHome() {
    return System.getProperty("rio.home")
}
