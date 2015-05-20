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

/*
 * Defines the roots for Webster as well as whether to spawn webster into it's own JVM.
 *
 * Note: The values ${sorcerHome}, ${rioVersion} and ${m2Repo} are set as bindings by
 * the webster.groovy script
 */

/*@Grab(group="org.rioproject", module="rio-platform", version="5.1.5")
import org.rioproject.net.HostUtil;*/

webster {
//    address = org.rioproject.net.HostUtil.getInetAddress().toString()
//    address = HostUtil.getInetAddress().getHostAddress()

    roots = ["${sorcerHome}/lib/sorcer/lib-dl",
             "${sorcerHome}/lib/sorcer/lib",
             "${sorcerHome}/lib/river",
             "${sorcerHome}/rio-${rioVersion}/lib",
             "${sorcerHome}/rio-${rioVersion}/lib-dl",
             "${sorcerHome}/lib/common",
             "${sorcerHome}/lib/blitz",
             "${sorcerHome}/lib",
             "${m2Repo}"/*,
             "${sorcerHome}/data"*/]

    spawn = true
}


