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

import org.rioproject.config.Component

@Component('sorcer.core.exertion.deployment')
class TestIP {
    String[] interfaces = ["some.example.interface.Test"]
    String[] codebaseJars = ["ju-arithmetic-dl.jar"]
    String[] implJars = ["ju-arithmetic-beans.jar"]
    String jvmArgs = "-Xmx4G"
    boolean fork = true

    def opSys = ["Linux", "Mac"] as String[]

    String arch = "x86_64"

    def ips = ["10.131.5.106", "10.131.4.201", "macdna.rb.rad-e.wpafb.af.mil", "10.0.1.9"] as String[]

    def ips_exclude = ["127.0.0.1"] as String[]
}