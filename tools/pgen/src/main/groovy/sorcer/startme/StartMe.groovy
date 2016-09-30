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

package sorcer.startme

import net.jini.config.Configuration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.Project
import org.gradle.api.tasks.*
import org.rioproject.config.GroovyConfig
import org.rioproject.resolver.Artifact
/**
 * Starts a provider/requestor using the output of the
 * @link mil.afrl.mstc.open.tools.configbuilder.ConfigBuilder task
 *
 * @author Dennis Reedy
 */
class StartMe extends DefaultTask {
    @InputFile
    File starterConfig
    @Input @Optional
    def systemProperties = [:]
    @Input @Optional
    def environment = [:]
    @Input @Optional
    String securityPolicy
    @Input @Optional
    boolean debug
    @Input @Optional
    boolean spawn = true
    @Input
    String distribution
    final String osType
    final String nativeLib
    final String nativeLibPrefix
    final String nativeLibExt
    private Configuration config
    private String serviceConfig
    private String serviceName

    StartMe() {
        String os = System.getProperty("os.name").toLowerCase()
        String projectHomeProperty = "projectHome"
        if (os.indexOf("mac") > -1) {
            osType = "Mac"
            if(project.hasProperty('nativeLibOpenHome')) {
                nativeLib = "${project['nativeLibOpenHome']}/mac"
            } else if(project.hasProperty(projectHomeProperty)) {
                println "WARNING: '${project.name}' does not have property 'nativeLibOpenHome' declared, " +
                        "unable to locate ${osType} native library open distribution"
            }
            nativeLibPrefix = "lib"
            nativeLibExt = "dylib"
        } else if (os.indexOf("win") > -1) {
            osType = "Windows"
            if(project.hasProperty('nativeLibOpenHome')) {
                nativeLib = "${project['nativeLibOpenHome']}/win"
            } else if(project.hasProperty(projectHomeProperty)) {
                println "WARNING: '${project.name}' does not have property 'nativeLibOpenHome' declared, " +
                        "unable to locate ${osType} native library open distribution"
            }
            nativeLibPrefix = "lib"
            nativeLibExt = "dll"
        } else if ((os.indexOf("nix") > -1) || (os.indexOf("nux") > -1)) {
            osType = "Linux"
            if(project.hasProperty('nativeLibOpenHome')) {
                nativeLib = "${project['nativeLibOpenHome']}/linux"
            } else if(project.hasProperty(projectHomeProperty)) {
                println "WARNING: '${project.name}' does not have property 'nativeLibOpenHome' declared, " +
                        "unable to locate ${osType} native library open distribution"
            }
            nativeLibPrefix = "lib"
            nativeLibExt = "so"
        } else {
            throw new TaskInstantiationException("Unknown operating system ${os}")
        }
    }

    void setSystemProperties(systemProperties) {
        this.systemProperties.putAll(systemProperties)
    }

    @Override
    String getDescription() {
        return "Starts a provider/requestor using the output of the ConfigBuilder task"
    }

    @TaskAction
    def startMe() {
        StringBuilder startClassPath = new StringBuilder()
        StringBuilder classPath = new StringBuilder()
        StringBuilder codebase = new StringBuilder()

        project.libs.starter.each { j ->
            if (startClassPath.length() > 0)
                startClassPath.append(File.pathSeparator)
            startClassPath.append(j)
        }

        /*
         * Set system properties (if not set) in order to load the provider's configuration
         */
        if (System.getProperty("project.home") == null)
            System.setProperty("project.home", project.rootProject.projectDir.path)

        ClassLoader ccl = Thread.currentThread().getContextClassLoader()
        try {
            config = new GroovyConfig([starterConfig.path] as String[], org.rioproject.config.Component.class.classLoader)
        } catch (Exception e) {
            throw new GradleScriptException("Could not create configuration using ${starterConfig}", e)
        } finally {
            Thread.currentThread().setContextClassLoader(ccl)
        }
        def codebaseJars = getEntry("codebaseJars", String[].class)
        String websterPort = project.websterPort
        String websterAddress = project.websterAddress
        if (codebaseJars.length == 1 && Artifact.isArtifact(codebaseJars[0])) {
            Artifact a = new Artifact(codebaseJars[0])
            codebase.append("artifact:${a.groupId}/${a.artifactId}/${a.version}")
            codebase.append(";http://${websterAddress}:${websterPort}")
            project.publishing.repositories.toSet().each { r ->
                codebase.append(";").append(r.url.toString())
            }
        } else {
            String codeServer = String.format("http://%s:%s/", websterAddress, websterPort)
            for (String codebaseJar : codebaseJars) {
                if (codebase.length() > 0)
                    codebase.append(" ")
                codebase.append(codeServer).append(codebaseJar)
            }
        }

        def implJars = getEntry("implJars", String[].class)
        /*if (implJars.length == 1 && Artifact.isArtifact(implJars[0])) {
            Artifact a = new Artifact(implJars[0])
            Project providerProject = project.rootProject.childProjects["${a.artifactId}"]
            def classPathList = [implJars[0]]
            classPathList << ConfigBuilder.getProjectJar(providerProject)
            providerProject.configurations.runtime.collect { dep ->
                if(!classPathList.contains(dep.path)) {
                    classPathList << dep.path
                }
            }
            implJars = classPathList as String[]
        }*/
        for (String implJar : implJars) {
            if (classPath.length() > 0)
                classPath.append(File.pathSeparator)
            classPath.append(implJar)
        }

        String providerClass = getEntry("providerClass", String.class)
        serviceConfig = starterConfig.path
        String args = getEntry("jvmArgs", String.class)
        String jvmArgs = args == null ? "" : args

        try {
            Configuration sConfig = new GroovyConfig([serviceConfig] as String[], null)
            serviceName = sConfig.getEntry("sorcer.core.provider.ServiceProvider", "name", String.class, null)
        } catch (Exception e) {
            e.printStackTrace()
            serviceName = project.name
        }
        exec(project, providerClass, startClassPath.toString(), classPath.toString(), codebase.toString(), jvmArgs)
    }

    def getEntry(String entry, Class<?> type) {
        return config.getEntry("sorcer.core.exertion.deployment", entry, type, null)
    }

    def exec(Project project,
             String providerClass,
             String starterClassPath,
             String providerClassPath,
             String providerCodebase,
             String jvmArgs) {

        project.logger.info "\nStarter Classpath\n=================\n${starterClassPath}"
        project.logger.info "\nService Codebase\n================\n${providerCodebase}"
        project.logger.info "\nService Classpath\n=================\n${providerClassPath}\n"
        project.logger.info "\nProvider Class\n=================\n${providerClass}\n"

        String policy = securityPolicy == null ?
                        "${project.rootProject.projectDir.path}/configs/policy.all" :
                        securityPolicy

        StringBuilder java = new StringBuilder()
        java.append(System.getProperty('java.home')).append("/bin/java")

        def args = []
        if (System.getProperty("os.name").startsWith("Windows")) {
            args << "cmd.exe"
            args << "/C"
        }
        args << java.toString()
        if (debug) {
            args << "-Xdebug"
            args << "-Xrunjdwp:transport=dt_socket,server=y,address=8765"
        }
        String logConfig = systemProperties['java.util.logging.config.file']
        if(logConfig==null)
            logConfig = "${distribution}/configs/sorcer.logging"
        args << "-Djava.util.logging.config.file=${logConfig}"
        if (System.getProperty("logback.configurationFile") != null)
            args << "-Dlogback.configurationFile=${System.getProperty("logback.configurationFile")}"
        else
            args << "-Dlogback.configurationFile=${distribution}/configs/sorcer-logging.groovy"

        String logDir = systemProperties['rio.log.dir']
        if(logDir==null)
            logDir = "${project.rootProject.projectDir.path}/logs"
        File logDirFile = new File(logDir)
        if (!logDirFile.exists())
            logDirFile.mkdirs()

        args << "-cp"
        args << starterClassPath
        //args << "-Djava.rmi.server.RMIClassLoaderSpi=sorcer.rio.rmi.SorcerResolvingLoader"
        //args << "-Djava.rmi.server.RMIClassLoaderSpi=org.rioproject.rmi.ResolvingLoader"
        args << "-Dproject.id=${project.name}"
        if(osType == "Windows") {
            args << "-Djava.protocol.handler.pkgs=\"net.jini.url|sorcer.util.url|org.rioproject.url\""
        } else {
            args << "-Djava.protocol.handler.pkgs=net.jini.url|sorcer.util.url|org.rioproject.url"
        }
        if(!spawn)
            args << "-DforceConsoleLogging=true"
        args << "-Djava.rmi.server.useCodebaseOnly=false"
        args << "-Djava.security.policy=${policy}"
        args << "-Dorg.rioproject.service=${serviceName}"
        args << "-Drio.log.dir=${logDir}"
        args << "-Dsorcer.provider.codebase=${providerCodebase}"
        args << "-Dsorcer.provider.classpath=${providerClassPath}"
        args << "-Dsorcer.provider.impl=${providerClass}"
        args << "-Dsorcer.provider.config=${serviceConfig}"
        args << "-Dsorcer.home=${distribution}"
        args << "-Drio.home=${distribution}/rio-${project.rioVersion}"

        systemProperties.each { k, v ->
            args << "-D$k=$v"
        }
        if (jvmArgs.indexOf("-Xms") == -1)
            args << "-Xms128m"
        if (jvmArgs.indexOf("-Xmx") == -1)
            args << "-Xmx512m"
        if (jvmArgs.indexOf("-Xss") == -1)
            args << "-Xss512m"
        String[] jvmArgParts = jvmArgs.split(" ")
        for (String jvmArg : jvmArgParts) {
            if (jvmArg.length() > 0)
                args << jvmArg
        }

        args << "org.rioproject.start.ServiceStarter"
        String projectHomeProperty = "projectHome"
        if(project.hasProperty(projectHomeProperty)) {
            args << "${project[projectHomeProperty]}/configs/startup-prv.config"
        } else
            args << "${project["sorcerHome"]}/configs/startup-prv.config"

        genScriptCommandLine(project, args)
        StringBuilder sb = new StringBuilder()
        for(String arg : args) {
            if(arg.startsWith("-Dsorcer.provider.codebase") || arg.startsWith("-Djava.protocol.handler.pkgs")) {
                String[] parts = arg.split("=")
                if(!parts[1].startsWith("\""))
                    arg = String.format("%s=\"%s\"", parts[0], parts[1])
            }
            if(sb.length()>0)
                sb.append(" ")
            sb.append(arg)
        }
        //generateStarter(project, sb.toString())
        if(project.logger.isInfoEnabled()) {
            project.logger.info "\nCommand line\n=================\n${sb.toString()}\n"
        }
        ProcessBuilder pb = new ProcessBuilder(args as String[])
        Map<String, String> env = pb.environment()
        env.put("SORCER_HOME", "${distribution}")
        env.put("RIO_HOME", "${distribution}/rio-${project.rioVersion}")
        environment.each {key, value ->
            env.put(key, value)
        }
        /*if(project.hasProperty(engHomeProperty)) {
            env.put("ENG_HOME", project[getProjectHomeProperty(project)])
        }*/
        Process process = pb.start()
        if (!spawn) {
            process.consumeProcessOutput(System.out, System.err)
            process.waitFor()
        } else {
            process.in.close()
            process.out.close()
            process.err.close()
        }
    }

    private void genScriptCommandLine(Project project, def args) {
        String repoDir = new File(project.rootProject.repositories.mavenLocal().getUrl()).path.replace('\\', '/')
        StringBuilder sb = new StringBuilder()
        String dist
        String proj
        String repo
        String distRoot
        String dataDir
        String dataDirectory = null
        if(osType=="Windows") {
            dist = "%DIST%"
            proj = "%PROJECT%"
            repo = "%REPO%"
            distRoot = "%DIST_ROOT%"
            dataDir = "%DATA_DIR%"
        } else {
            dist = '\\$DIST'
            proj = '\\$PROJECT'
            repo = '\\$REPO'
            distRoot = '\\$DIST_ROOT'
            dataDir = '\\$DATA_DIR'
        }
        for(String arg : args) {
            if(arg.startsWith("-Dsorcer.provider.codebase") ||
               arg.startsWith("-Djava.protocol.handler.pkgs") ||
               arg.startsWith("-Dsorcer.provider.classpath")) {
                String[] parts = arg.split("=")
                if(!parts[1].startsWith("\""))
                    arg = String.format("%s=\"%s\"", parts[0], parts[1])
            }
            else if(arg.startsWith("-Dnative")) {
                String[] parts = arg.split("=")
                if(osType=="Windows")
                    arg = String.format("%s=%s", parts[0], parts[1].replace(new File(distribution).parent, distRoot))
                else
                    arg = String.format("%s=%s", parts[0], parts[1].replaceAll(new File(distribution).parent, distRoot))
            }
            else if(arg.startsWith("-Dsorcer.data.dir")) {
                String[] parts = arg.split("=")
                dataDirectory = parts[1]
                if(osType=="Windows")
                    arg = String.format('%s=%s', parts[0], parts[1].replace(parts[1], dataDir))
                else
                    arg = String.format('%s=%s', parts[0], parts[1].replaceAll(parts[1], dataDir))
            } else if(arg.indexOf("=")!=-1) {
                String[] parts = arg.split("=")
                if(parts[1].contains(";") && !parts[1].startsWith("\""))
                    arg = String.format("%s=\"%s\"", parts[0], parts[1])
            }
            if(sb.length()>0)
                sb.append(" ")

            String replaced
            if(osType=="Windows") {
                String d = distribution.replace('\\', '/')
                replaced = arg.replace(distribution, dist)
                        .replace(project.rootProject.projectDir.path, proj)
                        .replace(repoDir, repo)
            } else {
                replaced = arg.replaceAll(distribution, dist)
                        .replaceAll(project.rootProject.projectDir.path, proj)
                        .replaceAll(repoDir, repo)
            }
            sb.append(replaced)
        }
        generateStarter(project, sb.toString(), repoDir, dataDirectory)
    }

    private void generateStarter(Project project,
                                 String cmdLine,
                                 String repoDir,
                                 String dataDir) {
        File bin = new File(project.buildDir, "bin")
        if(!bin.exists())
            bin.mkdirs()
        String ext
        StringBuilder content = new StringBuilder()
        String comment
        String export
        if(osType=="Windows") {
            ext = "cmd"
            content.append("@echo off\n")
            comment = "::"
            export = "set "
        } else {
            ext = "sh"
            content.append("#!/bin/sh\n")
            comment = "#"
            export = ""
        }
        content.append("$comment --------------------\n")
        content.append("$comment Script for ${project.name}, version ${project.version} generated by startme\n")
        content.append("$comment\n")
        def now = new Date()
        def dateString = now.format("yyyy-MMM-dd HH:mm:ss a")
        content.append("$comment When: ${dateString}\n")
        content.append("$comment OS  : ${System.getProperty("os.name")} ${System.getProperty("os.version")} \n")
        content.append("$comment --------------------\n")
        content.append("${export}DIST_ROOT=${new File(distribution).parent}\n")
        content.append("${export}DIST=${distribution}\n")
        content.append("${export}PROJECT=${project.rootProject.projectDir.path}\n")
        content.append("${export}REPO=${repoDir}\n")
        if(dataDir!=null)
            content.append("${export}DATA_DIR=${dataDir}\n\n")
        content.append(cmdLine)
        File starter = new File(bin, "${project.name}-${project.version}-startme.$ext")
        if(starter.exists()) {
            if(project.logger.isInfoEnabled())
                project.logger.info("Removing ${starter.path}")
            starter.delete()
        }
        starter.text = content.toString()
        if(ext=="sh") {
            Process chmod = "chmod +x ${starter.path}".execute()
            chmod.waitFor()
        }
        if(project.logger.isInfoEnabled())
            project.logger.info("Generated ${starter.path}")
    }
}

