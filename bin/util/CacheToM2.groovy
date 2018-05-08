#!/usr/bin/env groovy
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
 * This script will copy artifacts from the Gradle cache to the local Maven repository
 */
boolean overWrite = false
boolean quiet = false
if(args.length>0) {
    switch(args[0]) {
        case "-o":
            overWrite = true
            break
        case "-q":
            quiet = true
            break
        case "-h":
            println "\nCacheToM2.groovy options:"
            println "\t-h help"
            println "\t-o Will overwrite artifacts in the local"
            println "\t   Maven repository with the downloaded Gradle artifact"
            println "\t-q Quiet mode, will not echo status"
            println ""
            System.exit 0
    }
}

/**
 * Get the local repository
 *
 * @return The File for the local maven repository, taking into account
 * settings.xml
 */
 getLocalRepository = {
    File repoDir
    String localRepository = null
    File defaultM2Home =
            new File(System.getProperty("user.home")+File.separator+".m2")
    if(System.getProperty("M2_HOME")!=null) {
        File settingsFile = new File(System.getProperty("M2_HOME"), "conf/settings.xml")
        if(settingsFile.exists()) {
            def settings = new XmlSlurper().parse(settingsFile)
            localRepository = settings.localRepository
        }
    }
    File settingsFile = new File(defaultM2Home, "settings.xml")
    if(settingsFile.exists()) {
        def settings = new XmlSlurper().parse(settingsFile)
        localRepository = settings.localRepository
    }
    if(localRepository==null) {
        repoDir = new File(defaultM2Home, "repository")
    } else if(localRepository!=null && localRepository.length()==0) {
        repoDir = new File(defaultM2Home, "repository")
    } else {
        repoDir = new File(localRepository)
    }
    return repoDir
}

getVersion = { String name ->
    int lastIndex = name.lastIndexOf(".")
    if(lastIndex==-1)
        return null;
    return name.substring(0, lastIndex);
}

copy = { File src, File dest ->
    def input = src.newDataInputStream()
    def output = dest.newDataOutputStream()
    output << input
    input.close()
    output.close()
}

int created = 0
int skipped = 0
int overwrote = 0
File m2Repo = getLocalRepository()
File gradleCacheDir = new File(System.getProperty("user.home"), ".gradle/caches/modules-2/files-2.1")
for(File first : gradleCacheDir.listFiles()) {
    if(!first.isDirectory())
        continue
    for(File second : first.listFiles()) {
        if(!second.isDirectory())
            continue
        second.traverse(type: groovy.io.FileType.FILES) { file ->
            String groupId = first.name.replaceAll("\\.", "/")
            String version = getVersion(file.name.substring(second.name.length()+1))
            File target = new File(m2Repo, "${groupId}/${second.name}/$version/${file.name}")
            if(!target.exists()) {
                File targetDir = new File(m2Repo, "${groupId}/${second.name}/$version")
                if(!targetDir.exists())
                    targetDir.mkdirs()
                copy(file, target)
                if(!quiet)
                    println "Created ${target.path}"
                created++
            } else {
                if(overWrite) {
                    copy(file, target)
                    if(!quiet)
                        println "Overwrote ${target.path}"
                    overwrote++
                } else {
                    if(!quiet)
                        println "Skipped ${target.path}, already exists"
                    skipped++
                }
            }
        }
    }
}
println "\nLocal Repository ${m2Repo.path}\n"

println "Created:   ${created}"
println "Overwrote: ${overwrote}"
println "Skipped:   ${skipped}"
