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
package sorcer.configbuilder
import net.jini.config.Configuration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.GradleScriptException
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.rioproject.config.GroovyConfig
import org.rioproject.resolver.Artifact
/**
 * Generates a configuration for a provider/requestor that can be used as input to the
 * @link mil.afrl.mstc.open.tools.startme.StartMe task
 *
 * @author Dennis Reedy
 */
class ConfigBuilder extends DefaultTask {
    @Input
    String codebase
    @Input
    String providerClass
    @Input
    File serviceConfig
    @OutputFile
    File configFile
    @Input @Optional
    String provider
    @Input @Optional
    String ips
    @Input @Optional
    Boolean fork = true
    @Input @Optional
    String opSys
    @Input @Optional
    String excludeIps
    @Input @Optional
    String arch
    @Input @Optional
    String perNode = "1"
    @Input @Optional
    String jvmArgs
    @Input @Optional
    boolean providerAsArtifact = true
    @Input @Optional
    String webster
    String rootProjectPath
    String repoDir

    @Override
    String getDescription() {
        return "Generates a configuration for a provider/requestor that can be used as input to the StartMe task"
    }

    @TaskAction
    def buildConfig() {
        Project providerProject
        if (provider == null || provider.length() == 0) {
            providerProject = project
        } else {
            providerProject = getChildProject(project.rootProject, provider)
        }
        rootProjectPath = providerProject.rootProject.projectDir.path
        repoDir = new File(project.rootProject.repositories.mavenLocal().getUrl()).path.replace('\\', '/') + "/"

        /* Use the classPathList as the classPath to load the provider's configuration. We use
         * the providerClassPathList as the classPath to load the provider if we do not start
         * using an artifact
         */
        def classPathList = []
        classPathList << getProjectJar(providerProject)
        def providerClassPathList = []
        providerClassPathList << getDependencyPath(checkIfProjectDepAndConvert(getProjectJar(providerProject)))
        String sorcerHome = null
        if(project.hasProperty("sorcerHome"))
            sorcerHome = project.sorcerHome.replace('\\', '/')
        providerProject.configurations.runtime.collect { dep ->
            classPathList << dep.path
            String depPath = dep.path.replace('\\', '/')
            if(sorcerHome && !depPath.startsWith(sorcerHome)) {
                File f = checkIfProjectDepAndConvert(dep)
                providerClassPathList << getDependencyPath(f)
                if (!f.exists())
                    println "WARNING: ${f.path} is not found in the local repository"
            }
        }

        Class[] publishedInterfaces = getInterfaceClasses(classPathList)
        if(publishedInterfaces==null || publishedInterfaces.length==0)
            throw new GradleException("The [sorcer.core.provider.ServiceProvider.publishedInterfaces] configuration " +
                                      "property must be declared in ${serviceConfig}. Verify that this property is properly " +
                                      "declared and retry.")
        def interfaces = []
        for(Class c : publishedInterfaces) {
            interfaces << c.name
        }

        StringBuilder codebaseBuilder = new StringBuilder()
        String codebaseName
        if(!Artifact.isArtifact(codebase)) {
            Project codebaseProject = getChildProject(providerProject.rootProject, codebase)
            codebaseName = codebaseProject.name

            def codebaseDeps = []
            codebaseDeps << checkIfProjectDepAndConvert(getProjectJar(codebaseProject))
            codebaseProject.configurations.runtime.collect { dep ->
                String depPath = dep.path.replace('\\', '/')
                if (!depPath.startsWith(sorcerHome)) {
                    File f = checkIfProjectDepAndConvert(dep)
                    codebaseDeps << f
                    if (!f.exists())
                        println "WARNING: ${f.path} is not found in the local repository"
                }
            }
            project.libs.platform_dls.each { j ->
                codebaseDeps << new File(j)
            }
            codebaseDeps.each { dep ->
                String depPath = dep.path.replace('\\', '/')
                StringBuilder path = new StringBuilder()
                if (depPath.startsWith(repoDir)) {
                    String remainder = getDependencyPath(dep)
                    path.append(remainder)
                } else {
                    path.append(dep.path)
                }
                if (codebaseBuilder.length() > 0)
                    codebaseBuilder.append(" ")
                codebaseBuilder.append(path.toString())
            }
        } else {
            codebaseName = codebase
            codebaseBuilder.append(codebase)
        }

        project.logger.info "\ncodebase\n=================\n${codebaseBuilder.toString()}"

        project.logger.info "Provider                 : ${providerProject.name}"
        project.logger.info "Codebase                 : ${codebaseName}"
        project.logger.info "Config                   : ${serviceConfig}"
        project.logger.info "providerClass            : ${providerClass}"
        project.logger.info "providerInterfaceClasses : ${interfaces}"

        def properties = [:]
        properties["provider.class"] = providerClass
        properties["sorcer.provider.codebase"] = codebaseBuilder.toString().replace('\\','/')
        properties["sorcer.provider.classpath"] = providerAsArtifact?
                                                  ["${providerProject.group}:${providerProject.name}:${providerProject.version}"] :
                                                  trim(providerClassPathList, "jsk-platform")
        if (fork)
            properties["fork"] = "yes"

        if (jvmArgs != null)
            properties["jvmArgs"] = jvmArgs

        properties["perNode"] = perNode

        if (opSys) {
            properties["opSys"] = opSys
        }
        if (arch) {
            properties["arch"] = arch
        }
        if (ips) {
            properties["ips"] = ips
        }
        if (excludeIps) {
            properties["ips_exclude"] = excludeIps
        }
        if (webster)
            properties["webster"] = webster

        if(project.hasProperty("projectHome")) {
            String projectHome = project["projectHome"]
            String serviceConfigPath
            if (projectHome != null) {
                serviceConfigPath = serviceConfig.path.substring(projectHome.length())
            } else {
                serviceConfigPath = serviceConfig.path
            }
            properties["service.config"] = serviceConfigPath
            properties["project.home.ref"] = "project.home"
        }
        ConfigWriter writer = new ConfigWriter()
        writer.project = project
        configFile.createNewFile()
        configFile.text = serviceConfig.text

        println "Writing ${configFile.path}"
        if(project.logger.isInfoEnabled()) {
            project.logger.info "Derived properties:"
            properties.each { key, value ->
                project.logger.info "$key: $value"
            }
        }

        writer.write(configFile, properties, interfaces, null)
    }

    String getDependencyPath(File dep) {
        String path
        String depPath = dep.path.replace('\\', '/')
        if (depPath.startsWith(repoDir)) {
            path = depPath.substring(repoDir.length())
        } else {
            path = depPath
        }
        path
    }

    File checkIfProjectDepAndConvert(File dep) {
        File file
        String depPath = dep.path.replace('\\', '/')
        String projectHome = project.rootProject.projectDir.path.replace('\\', '/')
        //String projectHome = project[getProjectHomeProperty(project)].replace('\\', '/')
        if(depPath.startsWith(projectHome)) {
            String jarName = dep.name
            int ndx = depPath.lastIndexOf("/")
            String s = depPath.substring(ndx+1, depPath.length())
            String nameAndVer = s.substring(0, s.length()-".jar".length())
            String version = parseVersion(nameAndVer)
            String name = nameAndVer.substring(0, (nameAndVer.length()-version.length())-1)
            project.logger.info "dependency name: $name, version: $version"
            file = new File(String.format("%s%s/%s/%s/%s",
                    repoDir, project.group.replaceAll("\\.", "/"), name, version, jarName))
        } else {
            file = dep
        }
        file
    }

    String parseVersion(String s) {
        String version
        String[] parts = s.split("-")
        for(String part : parts) {
            if(version!=null) {
                version = String.format("%s-%s", version, part)
            } else {
                if (Character.isDigit(part.charAt(0))) {
                    version = part
                }
            }
        }
        if(version==null)
            version = parts[parts.length-1]
        version
    }

    static Project getChildProject(Project root, String name) {
        if(name.startsWith(":"))
            name = name.substring(1)
        Project child
        for(Map.Entry<String, Project> entry : root.childProjects) {
            if(entry.value.name.equals(name)) {
                child = entry.value
                break
            }
        }
        return child
    }

    static def getProjectJar(Project p) {
        return p.tasks.getByName("jar").archivePath
    }

    def getInterfaceClasses(def classPath) {
        Class[] publishedInterfaces
        Configuration currentConfig
        try {
            currentConfig = new GroovyConfig([serviceConfig] as String[], getProjectClassLoader(classPath))
            publishedInterfaces = (Class[])currentConfig.getEntry("sorcer.core.provider.ServiceProvider",
                                                                  "publishedInterfaces",
                                                                  Class[].class,
                                                                  null)
        } catch(Exception e) {
            throw new GradleScriptException("The provider seems to be missing required dependencies, "+
                                             "because the ${serviceConfig} could not be loaded using " +
                                             "the provided classPath: ${classPath}", e)
        }
        publishedInterfaces
    }

    def getProjectClassLoader(def classPath) {
        def urls = []
        classPath.each { element ->
            File file = new File(element as String)
            urls << file.toURI().toURL()
        }
        return new URLClassLoader(urls as URL[])
    }

    static boolean filter(String path, boolean filterPlatform) {
        def excludes = ["jsk-resources", "start", "groovy-all", "rio-", "resolver-api", "slf4j", "sleepycat"]
        if(filterPlatform)
            excludes << "jsk-platform"
        for (String s : excludes) {
            if (path.contains(s)) {
                return true
            }
        }
        return false
    }

    def trim(List<String> list, String fragment) {
        def trimmed = []
        for(String element : list) {
            if(!element.contains(fragment))
                trimmed << element
        }
        trimmed
    }
}
