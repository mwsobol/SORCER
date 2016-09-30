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
package sorcer.generator

import groovy.io.FileType

/**
 *
 * @author Dennis Reedy
 */
class ProviderGatherer {
    static  List<ProviderModule> gatherFromLocalRepo(String group, File repoDir, Map<String, String> versions, File... distDirs) {
        print "Gathering provider information from ${repoDir.path} ... "
        long t0 = System.currentTimeMillis()
        def providerModules = []
        repoDir.eachFileRecurse FileType.DIRECTORIES, {
            String path = it.path.replace('\\', '/')
            if(it.name.endsWith("-api") && path.contains("mil/afrl/mstc") && include(group, it.getParent())) {
                String name = it.name.substring(0, it.name.length()-4)
                //println name
                ProviderModule providerModule = new ProviderModule(name)
                it.listFiles().each { v ->
                    if(v.isDirectory() && versionSupported(versions, v)) {
                        File jar = null
                        File pom = null
                        for(File f : v.listFiles()) {
                            if(f.name.endsWith("jar") && jar==null) {
                                jar = f
                            }
                            if(f.name.endsWith("pom")) {
                                pom = f
                            }
                        }
                        if(jar!=null) {
                            //println "\t${v.path}"
                            providerModule.providerInfos << new ProviderInfo()
                                    .setBaseName(name)
                                    .setVersion(v.name)
                                    .setApiJar(jar)
                                    .setClassPath(getClassPathFor(jar, pom, repoDir, distDirs))
                        } else {
                            println "No jar found in ${v.path}"
                        }
                    }
                }
                providerModules << providerModule
            }
        }
        println "complete (${System.currentTimeMillis()-t0} ms)"
        providerModules
    }

    static boolean versionSupported(versions, path) {
        String v = null;
        if(path.path.contains("open")) {
            v = versions.get("engOpenVersion")
        }
        if(path.path.contains("gov")) {
            v = versions.get("engGovVersion")
        }
        if(path.path.contains("itar")) {
            v = versions.get("engITarAndOrPropVersion")
        }
        return v!=null && path.name==v
    }

    static boolean include(group, path) {
        if(group.contains("open") && path.contains("open")) {
            return true
        }
        if(group.contains("gov") && (path.contains("open") || path.contains("gov"))) {
            return true
        }
        if(group.contains("itar")) {
            return true
        }
        return false
    }

    static def getClassPathFor(apiJar, pomFile, repoDir, distDirs) {
        def urls = []
        urls.addAll(getDefaultJars(distDirs))
        urls << apiJar.toURI().toURL()
        def pom = new XmlSlurper().parse(pomFile)


        pom.dependencies.dependency.each { dep ->
            StringBuilder sb = new StringBuilder()
            String fileName = "${dep.artifactId}-${dep.version}.jar"
            if(dep.groupId!="org.sorcer" && !dep.groupId.text().startsWith("org.rioproject")) {
                sb.append(dep.groupId.text().replace(".", File.separator))
                        .append(File.separator)
                        .append(dep.artifactId)
                        .append(File.separator)
                        .append(dep.version)
                        .append(File.separator)
                File file = new File(repoDir, "${sb.toString()}${File.separator}$fileName")
                if(file.exists()) {
                    //println "\t\t${file.path}"
                    urls << file.toURI().toURL()
                }
            }
        }
        urls
    }

    static def getDefaultJars(distDirs) {
        def urls = []
        ["jsk-platform"]
        for(File dir : distDirs) {
            for(File f : dir.listFiles()) {
                if(f.name.startsWith("jsk-platform") ||
                   f.name.startsWith("jsk-lib") ||
                   f.name.startsWith("serviceui") ||
                   f.name.startsWith("rio-platform") ||
                   f.name.startsWith("rio-api") ||
                   f.name.startsWith("rio-lib") ||
                   f.name.startsWith("sorcer-platform")) {
                    urls << f.toURI().toURL()                }
            }
        }
        urls
    }
}
