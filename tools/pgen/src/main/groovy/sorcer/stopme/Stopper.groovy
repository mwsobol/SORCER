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
package sorcer.stopme
import com.sun.tools.attach.VirtualMachine
import sun.management.ConnectorAddressLink

import javax.management.MBeanServerConnection
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
/**
 *
 * @author Dennis Reedy
 */
class Stopper {

    static void stop(String projectId) {
        stop(projectId, false)

    }
    static void stop(String projectId, boolean force) {
        def pids = getPids(projectId)
        destroy(projectId, pids)
        kill(pids, force)
    }

    static void destroy(projectId, pids)  {
        pids.each { pid ->
            try {
                String address = ConnectorAddressLink.importFrom(Integer.valueOf(pid))
                if (address == null)
                    startManagementAgent(pid)
                address = ConnectorAddressLink.importFrom(Integer.valueOf(pid))
                if (address != null) {
                    JMXServiceURL jmxUrl = new JMXServiceURL(address)
                    JMXConnector jmxc = JMXConnectorFactory.connect(jmxUrl, null);
                    MBeanServerConnection serverConnection = jmxc.getMBeanServerConnection()
                    serverConnection.queryNames(null, null).each { mBean ->
                        if (mBean.domain == "sorcer.core.provider") {
                            String id = mBean.getKeyProperty("projectId")
                            if (id != null && id == projectId) {
                                serverConnection.invoke(mBean, "destroy", null, null)
                            }
                        }
                    }
                    jmxc.close()
                } else {
                    println "Unable to obtain JMX URL for $pid"
                }
            } catch(Exception e) {
                println "${e.getClass().getName()}: ${e.getMessage()}, recieved processing pid [$pid], continue..."
            }

        }
    }

    static def getPids(projectId) {
        def pids = []
        "jps -v".execute().text.eachLine { line ->
            if(line.contains("project.id=${projectId}")) {
                pids << line.split()[0]
            }
        }
        String p = pids.size()==1? "JVM" : "JVMs"
        println ":${projectId}:stopme: Found ${pids.size()} $p for ${projectId} to destroy"
        pids
    }

    static void kill(pids, force) {
        String command
        if(System.getProperty("os.name").startsWith("Windows")) {
            command = "taskkill /f /pid "
        } else {
            String option = force?"-9":""
            command = "kill ${option} "
        }
        pids.each { pid ->
            "$command $pid".execute().waitFor()
        }
    }

    private static void startManagementAgent(String pid) throws IOException {
        /*
         * JAR file normally in ${java.home}/jre/lib but may be in ${java.home}/lib
         * with development/non-images builds
         */
        String home = System.getProperty("java.home");
        String agent = "$home/jre/lib/management-agent.jar"
        File f = new File(agent)
        if (!f.exists()) {
            agent = "$home/lib/management-agent.jar"
            f = new File(agent)
            if (!f.exists()) {
                throw new RuntimeException("management-agent.jar missing")
            }
        }
        agent = f.getCanonicalPath()
        //println("Loading $agent into target JVM ...")
        try {
            VirtualMachine.attach(pid).loadAgent(agent);
        } catch (Exception x) {
            throw new IOException(x.getMessage());
        }
    }
}
