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
package sorcer.configbuilder
import net.jini.config.ConfigurationException
/**
 * Writes configuration entries into an existing configuration file.
 *
 * @author Dennis Reedy
 */
class ConfigWriter {
    def project
    private final marker = "//GEN-BEGIN"

    def write(configFile, properties, interfaces, currentConfig) {
        StringBuilder builder = new StringBuilder()
        for(String line : configFile.readLines()) {
            if(line.contains(marker))
                break
            builder << line
            builder << "\n"
        }

        String codebase = formatCodebase(properties["sorcer.provider.codebase"] as String)
        String classPath = listToString(properties["sorcer.provider.classpath"]).replaceAll("\\\\", "/")

        /* Test for an update to the configuration. If values have not changed do not update the file */
        if(currentConfig!=null) {
            try {
                String[] currentInterfaces = currentConfig.getEntry("sorcer.core.exertion.deployment",
                                                                    "interfaces",
                                                                    String[].class)
                String[] currentCodebaseJars = currentConfig.getEntry("sorcer.core.exertion.deployment",
                                                                      "codebaseJars",
                                                                      String[].class)
                String[] currentImplJars = currentConfig.getEntry("sorcer.core.exertion.deployment",
                                                                  "implJars",
                                                                  String[].class)
                String currentProviderClass = currentConfig.getEntry("sorcer.core.exertion.deployment",
                                                                     "providerClass",
                                                                     String.class)

                boolean interfacesEqual = Arrays.equals(interfaces as String[], currentInterfaces)
                boolean codebaseJarsEqual = Arrays.equals(currentCodebaseJars, toStringArray(codebase))
                boolean implJarsEqual = Arrays.equals(currentImplJars, toStringArray(classPath))
                boolean providerClassEqual = properties["provider.class"] == currentProviderClass
                boolean jvmArgsEqual = true
                boolean maxPerCybernodeEqual = true
                boolean archEquals = true
                boolean opSysEqual = true
                boolean ipsEqual = true
                boolean excludeIpsEqual = true
                boolean websterUrlsEqual = true
                if (properties["fork"] != null) {
                    currentConfig.getEntry("sorcer.core.exertion.deployment",
                                           "fork",
                                           Boolean.class)
                }
                if (properties["jvmArgs"] != null) {
                    String currentJvmArgs = currentConfig.getEntry("sorcer.core.exertion.deployment",
                                                                   "jvmArgs",
                                                                   String.class)
                    doLog("jvmArgs: ${properties["jvmArgs"]}")
                    doLog("currentJvmArgs: ${currentJvmArgs}")
                    String newJvmArgs = properties["jvmArgs"]
                    jvmArgsEqual = currentJvmArgs.equals(newJvmArgs)
                }
                if (properties["perNode"] != null) {
                    int currentMaxPerCybernode = currentConfig.getEntry("sorcer.core.exertion.deployment",
                                                                        "perNode",
                                                                        int.class)
                    doLog("perNode: ${properties["perNode"]}")
                    doLog("currentMaxPerCybernode: ${currentMaxPerCybernode}")
                    int maxPerCybernode = Integer.parseInt(properties["perNode"] as String)
                    maxPerCybernodeEqual = currentMaxPerCybernode == maxPerCybernode
                }

                if (properties["arch"] != null) {
                    String currentArch = currentConfig.getEntry("sorcer.core.exertion.deployment",
                                                                "arch",
                                                                String.class)
                    doLog("arch: ${properties["arch"]}")
                    doLog("currentArch: ${currentArch}")
                    String newArch = properties["arch"]
                    archEquals = newArch.equals(currentArch)
                }
                if (properties["opSys"] != null) {
                    String[] currentOpSys = currentConfig.getEntry("sorcer.core.exertion.deployment",
                                                                   "opSys",
                                                                   String[].class)
                    doLog("opSys: ${properties["opSys"]}")
                    doLog("currentOpSys: ${currentOpSys}")
                    String[] newOpSys = properties["opSys"]
                    opSysEqual = Arrays.equals(currentOpSys, newOpSys)
                }
                if (properties["ips"] != null) {
                    String[] currentIps = currentConfig.getEntry("sorcer.core.exertion.deployment",
                                                                 "ips",
                                                                 String[].class)
                    doLog("ips: ${properties["ips"]}")
                    doLog("currentIps: ${currentIps}")
                    ipsEqual = matchArrayContents(properties["ips"], currentIps)
                }
                if (properties["ips_exclude"] != null) {
                    String[] currentExcludeIps = currentConfig.getEntry("sorcer.core.exertion.deployment",
                                                                        "ips_exclude",
                                                                        String[].class)
                    doLog("ips_exclude: ${properties["ips_exclude"]}")
                    doLog("currentExcludeIps: ${currentExcludeIps}")
                    excludeIpsEqual = matchArrayContents(properties["ips_exclude"] as String,
                                                         currentExcludeIps)
                }
                if (properties["webster"] != null) {
                    String currentWebster = currentConfig.getEntry("sorcer.core.exertion.deployment",
                                                                   "webster",
                                                                   String.class)
                    doLog("webster: ${properties["webster"]}")
                    doLog("currentWebster: ${currentWebster}")
                    String newWebster = properties["webster"]
                    websterUrlsEqual = newWebster.equals(currentWebster)
                }
                doLog("interfacesEqual:      ${interfacesEqual}")
                doLog("codebaseJarsEqual:    ${codebaseJarsEqual}")
                doLog("implJarsEqual:        ${implJarsEqual}")
                doLog("providerClassEqual:   ${providerClassEqual}")
                doLog("jvmArgsEqual:         ${jvmArgsEqual}")
                doLog("maxPerCybernodeEqual: ${maxPerCybernodeEqual}")
                doLog("archEquals:           ${archEquals}")
                doLog("opSysEqual:           ${opSysEqual}")
                doLog("ipsEqual:             ${ipsEqual}")
                doLog("excludeIpsEqual:      ${excludeIpsEqual}")
                doLog("websterUrlsEqual:     ${websterUrlsEqual}")
                if (!(interfacesEqual && codebaseJarsEqual && implJarsEqual &&
                      providerClassEqual && jvmArgsEqual && maxPerCybernodeEqual &&
                      archEquals && opSysEqual && ipsEqual && excludeIpsEqual && websterUrlsEqual)) {
                    doWrite(configFile, builder, codebase, classPath, interfaces, properties)
                } else {
                    doLog("No changes detected, config file generation skipped")
                }
            } catch (ConfigurationException e) {
                doWrite(configFile, builder, codebase, classPath, interfaces, properties)
            }
        } else {
            doWrite(configFile, builder, codebase, classPath, interfaces, properties)
        }
    }

    private void doLog(String message) {
        if(project!=null)
            project.logger.info(message)
    }

    private boolean matchArrayContents(String s, String[] array) {
        boolean arraysMatched = true
        StringTokenizer st = new StringTokenizer(s, " ,");
        def parts = st.toList()
        for(String part : parts) {
            boolean matched = false;
            for(String s1 : array) {
                if(part.equals(s1)) {
                    matched = true
                    break
                }
            }
            if(!matched) {
                arraysMatched = false;
                break;
            }
        }
        return arraysMatched
    }

    private void doWrite(configFile, builder, codebase, classPath, interfaces, properties) {
        doLog "Writing ${configFile.path}"
        /* These tables are used to get the lengths of the declarations so indentation can occur, allowing a more
         * readable configuration */
        def configContentTable = [
                "codebase" :   "codebaseJars = new String[]{",
                "interfaces" : "interfaces = new String[]{",
                "classpath" :  "implJars = new String[]{"]

        def groovyContentTable = [
                "codebase" :   "def codebaseJars = [",
                "interfaces" : "def interfaces = [",
                "classpath" :  "def implJars = ["]


        String content
        if(configFile.getName().endsWith("config"))  {
            content = asConfig(properties,
                               indent(codebase, configContentTable["codebase"].length()+4),
                               indent(classPath, configContentTable["classpath"].length()+4),
                               indent(listToString(interfaces), configContentTable["interfaces"].length()+4))
        } else {
            content = asGroovy(properties,
                               indent(codebase, groovyContentTable["codebase"].length()+4),
                               indent(classPath, groovyContentTable["classpath"].length()+4),
                               indent(listToString(interfaces), groovyContentTable["interfaces"].length()+4))
        }
        builder << banner()
        builder << content
        configFile.text = builder.toString()
    }

    private String asConfig(properties, codebase, classPath, interfaces) {
        StringBuilder configBuilder = new StringBuilder()
        configBuilder << "sorcer.core.exertion.deployment {\n"
        configBuilder << "    interfaces = new String[]{"+interfaces+"};\n\n"
        configBuilder << "    codebaseJars = new String[]{"+codebase+"};\n\n"
        configBuilder << "    implJars = new String[]{"+classPath+"};\n"
        if(properties["provider.class"]!=null) {
            configBuilder << "\n"
            configBuilder << "    providerClass = \""+properties["provider.class"]+"\";\n"
        }
        if(properties["service.config"]!=null) {
            configBuilder << "\n"
            configBuilder << "    /* Used by startme */\n"
            configBuilder << "    serviceConfig = \"\${"+properties["project.home.ref"]+"}"+properties["service.config"]+"\";\n"
        }
        if(properties["fork"]!=null) {
            configBuilder << "\n"
            configBuilder << "    fork = Boolean.valueOf(true);\n"
        }
        if(properties["jvmArgs"]!=null) {
            configBuilder << "\n"
            configBuilder << "    jvmArgs = \"${properties["jvmArgs"]}\";\n"
        }
        if(properties["perNode"]!=null) {
            int perNode = Integer.parseInt(properties["perNode"] as String)
            if(perNode>0) {
                configBuilder << "\n"
                configBuilder << "    perNode = ${perNode};\n"
            }
        }
        if(properties["opSys"]!= null) {
            configBuilder << "\n"
            configBuilder << "    opSys = new String[]{"+formatCommaSeparatedString(properties["opSys"] as String)+"};\n"
        }
        if(properties["arch"]!=null) {
            configBuilder << "\n"
            configBuilder << "    arch = \""+properties["arch"]+"\";\n"
        }
        if(properties["ips"]!=null) {
            configBuilder << "\n"
            configBuilder << "    ips = new String[]{"+formatCommaSeparatedString(properties["ips"] as String)+"};\n"
        }
        if(properties["ips_exclude"]!=null) {
            configBuilder << "\n"
            configBuilder << "    ips_exclude = new String[]{"+formatCommaSeparatedString(properties["ips_exclude"] as String)+"};\n"
        }
        if(properties["webster"]!=null) {
            configBuilder << "\n"
            configBuilder << "    webster = \""+properties["webster"]+"\";\n"
        }
        configBuilder << "}\n"
        return configBuilder.toString()
    }

    private String asGroovy(properties, codebase, classPath, interfaces) {
        StringBuilder configBuilder = new StringBuilder()
        configBuilder << "@org.rioproject.config.Component('sorcer.core.exertion.deployment')\n"
        configBuilder << "class DeployConfig {\n"
        configBuilder << "    def interfaces = ["+interfaces+"] as String[]\n\n"
        configBuilder << "    def codebaseJars = ["+codebase+"] as String[]\n\n"
        configBuilder << "    def implJars = ["+classPath+"] as String[]\n"
        if(properties["provider.class"]!=null) {
            configBuilder << "\n"
            configBuilder << "    String providerClass = \""+properties["provider.class"]+"\"\n"
        }
        if(properties["service.config"]!=null) {
            configBuilder << "\n"
            configBuilder << "    /* Used by startme */\n"
            configBuilder << "    String serviceConfig = \"\${"+properties["project.home.ref"]+"}"+properties["service.config"]+"\"\n"
        }
        if(properties["fork"]!=null) {
            configBuilder << "\n"
            configBuilder << "    boolean fork = true\n"
        }
        if(properties["jvmArgs"]!=null) {
            configBuilder << "\n"
            configBuilder << "    String jvmArgs = \"${properties["jvmArgs"]}\"\n"
        }
        if(properties["perNode"]!=null) {
            int perNode = Integer.parseInt(properties["perNode"] as String)
            if(perNode>0) {
                configBuilder << "\n"
                configBuilder << "    int perNode = ${perNode}\n"
            }
        }
        if(properties["opSys"]!= null) {
            configBuilder << "\n"
            configBuilder << "    def opSys = ["+formatCommaSeparatedString(properties["opSys"] as String)+"] as String[]\n"
        }
        if(properties["arch"]!=null) {
            configBuilder << "\n"
            configBuilder << "    String arch = \""+properties["arch"]+"\"\n"
        }
        if(properties["ips"]!=null) {
            configBuilder << "\n"
            configBuilder << "    def ips = ["+formatCommaSeparatedString(properties["ips"] as String)+"] as String[]\n"
        }
        if(properties["ips_exclude"]!=null) {
            configBuilder << "\n"
            configBuilder << "    def ips_exclude = ["+formatCommaSeparatedString(properties["ips_exclude"] as String)+"] as String[]\n"
        }
        if(properties["webster"]!=null) {
            configBuilder << "\n"
            configBuilder << "    String webster = \""+properties["webster"]+"\"\n"
        }
        configBuilder << "}\n"
        return configBuilder.toString()
    }

    private String listToString(list) {
        if(list instanceof List) {
            StringBuilder builder = new StringBuilder()
            for(String s : list) {
                if(builder.length()>0) {
                    builder << ",\n"
                }
                builder << "\"$s\""
            }
            return builder.toString()
        }
        return list
    }

    private String formatCommaSeparatedString(String input) {
        StringBuilder b = new StringBuilder()
        for(String s : toStringArray(input)) {
            if(b.length()>0)
                b.append(", ")
            b.append("\"").append(s.trim()).append("\"")
        }
        return b.toString()
    }

    private String formatCodebase(String codebase) {
        def jars = []
        String[] parts = codebase.split(" ")
        for(String part : parts) {
            String result
            if(part.startsWith("artifact:")) {
                result = part
            } else if(part.contains("\${")) {
                int ndx = part.lastIndexOf("/")
                if (ndx != -1) {
                    result = part.substring(ndx + 1)
                } else {
                    result = part
                }
            } else {
                result = part
            }
            jars << result
        }
        return listToString(jars)
    }

    private String[] toStringArray(String s) {
        StringTokenizer tok = new StringTokenizer(s, ",\n\"")
        String[] array = new String[tok.countTokens()]
        int i=0;
        while(tok.hasMoreTokens()) {
            array[i] = tok.nextToken();
            i++;
        }
        return array;
    }

    def banner() {
        StringBuilder banner = new StringBuilder()
        banner << "$marker Do not remove\n"
        banner << "/*\n"
        banner << " * Generated by ConfigBuilder\n"
        banner << " *\n"
        banner << " * WARNING: Do NOT modify this code. The content of this configuration may\n"
        banner << " * be regenerated by the ConfigBuilder.\n"
        banner << " */\n"
    }

    def indent(String s, int indent) {
        int lineNumber = 0
        StringBuilder builder = new StringBuilder()
        List<String> lines = s.readLines()
        if(lines.size()==1)
            return s
        for(String line : lines) {
            if(lineNumber>0) {
                for(int i=0; i< indent; i++)
                    builder << " "
            }
            builder << line
            lineNumber++
            if(lineNumber<lines.size())
                builder << "\n"
        }
        return builder.toString()
    }
}
