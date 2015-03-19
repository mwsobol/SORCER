#!/usr/bin/env groovy
/*
 * Distribution Statement
 *
 * This computer software has been developed under sponsorship of the United States Air Force Research Lab. Any further
 * distribution or use by anyone or any data contained therein, unless otherwise specifically provided for,
 * is prohibited without the written approval of AFRL/RQVC-MSTC, 2210 8th Street Bldg 146, Room 218, WPAFB, OH  45433
 *
 * Disclaimer
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government. Neither
 * the United States Government nor the United States Air Force, nor any of their employees, makes any warranty,
 * express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness
 * of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately
 * owned rights.
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
