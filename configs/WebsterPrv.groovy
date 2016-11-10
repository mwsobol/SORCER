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
import org.rioproject.config.Component
import org.rioproject.resolver.maven2.Repository
import sorcer.provider.boot.Booter

@Component("sorcer.tools.codeserver")
class WebsterProv {

    String[] getRoots() {
        def roots = []
        String sorcerHome = System.getProperty("sorcer.home")
        String libPath = "${sorcerHome}/lib"

        // webster root directories
        roots << "${libPath}/sorcer/lib"
        roots <<  "${libPath}/sorcer/lib-dl"
        roots <<  "${libPath}/common"
        roots <<  "${libPath}/river"
        roots <<  "${sorcerHome}/rio-${System.properties['rio.version']}/lib-dl"
        roots <<  "${libPath}/blitz"
        roots <<  Repository.getLocalRepository().absolutePath
        roots <<  "${sorcerHome}/data"

        return roots as String[]
    }

    String[] getOptions() {
        def options = []
        options << "-port"
        options << Booter.getWebsterPort()
        options << "-bindAddress"
        options << Booter.getWebsterInterface()
        options << "-startPort"
        options << Booter.getWebsterStartPort()
        options << "-endPort"
        options << Booter.getWebsterEndPort()
        options << "-isDaemon"
        options << Boolean.toString(false)
        options << "-debug"
        options << Boolean.toString(false)
        return options as String[]
    }
}