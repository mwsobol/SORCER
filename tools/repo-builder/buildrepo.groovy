#!/usr/bin/env groovy
/*
 * This file generates repository-builder.xml, an Ant file, that is then run to populate an Ivy repository.
 * Along the way if any non-Maven (defined as not available on central or not assumed to be already installed in
 * your local Maven repository) will be installed.
 */

class BuildRepo {
    String repoBuilderDir
    //static String repoAddress = "10.131.7.138:7001"
	String repoAddress = "rio-project.org/maven2"
	
    void build(artifacts, runDeploy) {
        if (repoBuilderDir == null)
            repoBuilderDir = new File(".").path
        String rootDir = repoBuilderDir
        def cacheDir = new File(".ivy2/cache")
        def localRepository = getLocalRepository()
        if (cacheDir.exists()) {
            cacheDir.deleteDir()
            println "Deleted Ivy cache directory"
        }
        def File pomDir = new File("${repoBuilderDir}/poms")

        artifacts.each { artifact ->
			String[] parts = artifact.split(":")
            String organization = parts[0]
            String module = parts[1]
            String version = parts[2]

            File pom = new File(pomDir, "${module}.pom")
            if (pom.exists()) {
                String dir
                if (organization.contains("sorcer")) {
                    if (module.endsWith("-dl") || module.endsWith("ui")) {
                        dir = rootDir + "/lib/sorcer/lib-dl"
                    } else if (module.equals("webster") || module.indexOf("shell") > 0) {
                        dir = rootDir + "/lib/sorcer/lib-ext"
                    } else if (module.indexOf("boot") > 0) {
                        dir = rootDir + "/lib"
                    } else {
                        dir = rootDir + "/lib/sorcer/lib"
                    }
                } else if (module.equals("ext") || module.equals("jep") || module.equals("jffp") 
					|| module.equalsIgnoreCase("Jama") || module.equals("je")
					|| module.equals("jakarta-regexp") || module.equals("jsc-admin")) {
						dir = rootDir + "/lib/common/"
                } else if (module.equals("tools")) {
						dir = rootDir + "/lib/common/java6/"
                } else if (organization.contains("river")) {
                    if (module.endsWith("-dl"))
                        dir = rootDir + "/lib/river/lib-dl"
                    else
                        dir = rootDir + "/lib/river/lib"
                } else if (organization.contains("dancres")) {
                    dir = rootDir + "/lib/common/blitz"
                } else {
                    dir = rootDir + "/lib/common/buildsupport"
                }
                mvnInstall("${organization}:${module}:${version}", dir, localRepository, runDeploy)
            }
        }
    }

    def mvnInstall(String artifact, String dir, File localRepository, boolean runDeploy) {
        println "Processing ${artifact}, dir: ${dir} ..."
        String[] parts = artifact.split(":")
        String gId = parts[0]
        String aId = parts[1]
        String version = parts[2]
        File jar = getJar(new File(dir), aId)
		
        if (jar != null) {
            if (jar.name.contains(version)) {
                File tmpDir = getTempDir()
                File unversioned = new File(tmpDir, "${aId}.jar")
                if (!unversioned.exists()) {
                    copy(jar, unversioned)
                }
                dir = tmpDir.path
            }
        } else {
            println "########################################################\n" +
                    "ERROR: The ${artifact} does not exist, unable to publish\n" +
                    "########################################################"
            return
        }
        File sourceJar = new File("${dir}/${aId}.jar")
        File targetJar = new File(localRepository,
                                  String.format("%s/%s/%s/%s-%s.jar",
                                                gId.replace(".", File.separator), aId, version, aId, version))
        boolean install = true/*getAndMaybeFixPomVersion("./poms/${aId}.pom", version)*/
        if (!install) {
            if (targetJar.exists())
                install = !(targetJar.length() == sourceJar.length()) && (targetJar.lastModified() == sourceJar.lastModified())
            else
                install = true
        }
        if (install) {
            getAndMaybeFixPomVersion("${repoBuilderDir}/poms/${aId}.pom", version)
            File toPublish = new File("toPublish.txt")
            if (toPublish.exists())
                toPublish.delete()
            String installCommand = "mvn install:install-file " +
                    "-Dfile=${dir}/${aId}.jar " +
                    "-Dversion=${version} " +
                    "-Dpackaging=jar " +
                    "-DgroupId=${gId} " +
                    "-DartifactId=${aId} " +
                    "-DpomFile=${repoBuilderDir}/poms/${aId}.pom"
            exec(installCommand)
            String findCommand = "find ${localRepository.path} -type f -cmin 1"
            StringBuffer out = new StringBuffer()
            Process process = findCommand.execute()
            process.consumeProcessOutputStream(out)
            process.consumeProcessErrorStream(System.err)
            process.waitFor()
            if (out.length() > 0) {
                toPublish << out.toString()
            }
            if (runDeploy) {
                String deployCommand = "mvn deploy:deploy-file " +
                        "-Dversion=${version} " +
                        "-DgeneratePom=false -Dpackaging=jar " +
                        "-DgroupId=${gId} " +
                        "-DartifactId=${aId} " +
                        "-Dfile=${dir}/${aId}.jar " +
                        "-DpomFile=${repoBuilderDir}/poms/${aId}.pom " +
                        "-Durl=http://${repoAddress}"
                exec(deployCommand)
            }

        } else {
            println "$artifact is up to date"
        }
    }

    def exec(String cmd) {
        Process process = cmd.execute()
        process.consumeProcessOutputStream(System.out)
        process.consumeProcessErrorStream(System.err)
        process.waitFor()
    }

    def copy(File source, File target) {
        def input = source.newDataInputStream()
        def output = target.newDataOutputStream()
        output << input
        input.close()
        output.close()
    }

    def getTempDir() {
        File tmpDir = new File(System.getProperty("user.dir") + "/tmp")
        if (!tmpDir.exists())
            tmpDir.mkdirs()
        tmpDir
    }

    def getJar(File dir, String name) {
        File jar = null		
        for (File file : dir.listFiles()) {
            if (file.name.startsWith(name)) {
                jar = file
                break
            }
        }
        return jar
    }

    def getAndMaybeFixPomVersion(String pom, String version) {
        boolean changed = false
        File pomFile = new File(pom)
        def parsedPom = new XmlSlurper().parse(pomFile)
        String pomVersion = parsedPom.version
        if (version != pomVersion) {
            changed = true
            def pomText = pomFile.text
            pomText = pomText.replace("<version>${pomVersion}", "<version>${version}")
            pomFile.write(pomText)
            println "Changed version number in ${pom} from ${pomVersion} to ${version}"
        }
        changed
    }

    /**
     * Get the local repository
     *
     * @return The File for the local maven repository, taking into account settings.xml
     */
    def File getLocalRepository() {
        String localRepository = null
        File defaultM2Home =
            new File(System.getProperty("user.home") + File.separator + ".m2")
        if (System.getProperty("M2_HOME") != null) {
            File settingsFile = new File(System.getProperty("M2_HOME"), "conf/settings.xml")
            if (settingsFile.exists()) {
                def settings = new XmlSlurper().parse(settingsFile)
                localRepository = settings.localRepository
            }
        }
        File settingsFile = new File(defaultM2Home, "settings.xml")
        if (settingsFile.exists()) {
            def settings = new XmlSlurper().parse(settingsFile)
            localRepository = settings.localRepository
        }
        File repoDir
        if (localRepository == null) {
            repoDir = new File(defaultM2Home, "repository")
        } else if (localRepository != null && localRepository.length() == 0) {
            repoDir = new File(defaultM2Home, "repository")
        } else {
            repoDir = new File(localRepository)
        }
        return repoDir
    }

    def void publish() {
        File localRepository = getLocalRepository()
        File toPublish = new File("toPublish.txt")
        if (!toPublish.exists()) {
            println "The toPublish.txt file does not exist"
            System.exit(-1)
        }
        def failures = []
        String[] lines = toPublish.text.split("\\n");
        for (String s : lines) {
            if (s.length() == 0)
                continue
            println "Sending $s ..."
            StringBuffer out = new StringBuffer()
            String target
            if (s.startsWith("../distribution/")) {
                target = "distributions/iGrid/" + s.substring("../distribution".length() + 1)
            } else {
                target = s.substring(localRepository.path.length() + 1)
            }
            String curlExec = "curl --upload  $s http://${repoAddress}/$target"
            Process curlP = curlExec.execute()
            curlP.consumeProcessOutputStream(out)
            curlP.consumeProcessErrorStream(System.err)
            curlP.waitFor()
            if (out.contains("201") || out.contains("200")) {
                println "Sent    $s"
            } else {
                println out.toString()
                failures << s
            }
        }
        File lastPublished = new File("lastPublished.txt")
        if (lastPublished.exists())
            lastPublished.delete()
        lastPublished << toPublish.text
        toPublish.delete()
        if (!failures.isEmpty()) {
            File publishFailures = new File("publishFailed.txt")
            if (publishFailures.exists())
                publishFailures.delete()
            for (String failed : failures) {
                publishFailures << failed
                publishFailures << "\n"
            }
        }
    }
}

def getArtifacts(String[] options, String scriptDir) {
    def props = new Properties()
    new File("${scriptDir}/versions.properties").withInputStream {
        stream -> props.load(stream)
    }
    for (prop in props) {
        if (System.getProperty(prop.key) != null) {
            props.setProperty(prop.key, System.getProperty(prop.key))
        }
    }
    def artifacts = []
    for (String option : options) {
        switch (option) {
            case "sorcer":
                artifacts << "org.sorcer:sorcer-lib:${props['sorcer.version']}"
                artifacts << "org.sorcer:sorcer-platform:${props['sorcer.version']}"
                artifacts << "org.sorcer:sorcer-dl:${props['sorcer.version']}"
                artifacts << "org.sorcer:sorcer-boot:${props['sorcer.version']}"
                artifacts << "org.sorcer:exertlet-ui:${props['sorcer.version']}"
                artifacts << "org.sorcer:provider-ui:${props['sorcer.version']}"
                artifacts << "org.sorcer:webster:${props['sorcer.version']}"
                artifacts << "org.sorcer:sos-cataloger-ui:${props['sorcer.version']}"
                artifacts << "org.sorcer:sos-cataloger:${props['sorcer.version']}"
                artifacts << "org.sorcer:sos-rendezvous:${props['sorcer.version']}"
                artifacts << "org.sorcer:sos-db-prv:${props['sorcer.version']}"
                artifacts << "org.sorcer:sos-exertmonitor:${props['sorcer.version']}"
                artifacts << "org.sorcer:sos-logger:${props['sorcer.version']}"
                artifacts << "org.sorcer:sos-shell:${props['sorcer.version']}"
                artifacts << "org.sorcer:ju-arithmetic-beans:${props['sorcer.version']}"
                artifacts << "org.sorcer:ju-arithmetic-dl:${props['sorcer.version']}"
                break
            case "buildtools":
                artifacts << "mil.afrl.mstc.eng.tools:configbuilder:${props['configbuilder.version']}"
                break
            case "common":
                artifacts << "gov.nist.math:Jama:${props['jama.version']}"
                artifacts << "org.nfunk.jep:jep:${props['jep.version']}"
				artifacts << "org.cheiron:jsc-admin:1.0"
                artifacts << "org.lsmp.djep:ext:${props['ext.version']}"
                artifacts << "org.sadun:jffp:${props['jffp.version']}"
				artifacts << "com.sleepycat:je:${props['je.version']}"
				artifacts << "jakarta-regexp:jakarta-regexp:${props['jakarta-regexp.version']}"
                break
            case "javatools":
                artifacts << "java-tools:tools:6"
                break
            case "blitz":
                artifacts << "org.dancres:blitz-dl:${props['blitz.version']}"
                artifacts << "org.dancres:blitz:${props['blitz.version']}"
                artifacts << "org.dancres:blitzui:${props['blitz.version']}"
                artifacts << "org.dancres:dashboard:${props['blitz.version']}"
                artifacts << "org.dancres:stats:${props['blitz.version']}"
                artifacts << "org.dancres:lockmgr:${props['blitz.version']}"
                break
            default:
                println "Unknown option $option"
                return null
        }
    }
    artifacts
}
def options = []
if (args[0].equals("all")) {

    options << "sorcer"
    options << "buildtools"
    options << "common"
    options << "river"
    options << "blitz"
} else {
    if (args[0].equals("push")) {
        new BuildRepo().push()
        System.exit(0)
    } else {
        options << args[0]
    }
}

String scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
def artifacts = getArtifacts(options as String[], scriptDir)
if (artifacts) {
    boolean doDeploy = Boolean.valueOf(args[1])
    new BuildRepo().build(artifacts, doDeploy)
}
