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
import sorcer.generator.ui.ProjectFrame
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
/**
 * Generates MSTC Engineering provider project
 *
 * @author Dennis Reedy
 */
class ProjectGenerator extends DefaultTask {
    @Input
    @Optional
    boolean absoluteVersion = false

    @TaskAction
    def generate() {
        String version
        String projectVersion
        if (project.rootProject.group.endsWith("open")) {
            projectVersion =  project['engOpenVersion']
            version = absoluteVersion ? projectVersion : '${engOpenVersion}'
        } else if (project.rootProject.group.endsWith("gov")) {
            projectVersion =  project['engGovVersion']
            version = absoluteVersion ? projectVersion : '${engGovVersion}'
        } else {
            projectVersion =  project['engITarAndOrPropVersion']
            version = absoluteVersion ? projectVersion : '${engITarAndOrPropVersion}'
        }
        def providers = gatherFromLocalRepo()
        /*project.rootProject.subprojects { subproject ->
            if(subproject.name.endsWith("-api") &&
               !(subproject.name.startsWith("conmin") || subproject.name.startsWith("dot"))) {
                File jar = new File(subproject.getTasks().findByName("jar").archivePath as String)
                providers <<
                new ProviderInfo()
                        .setBaseName(subproject.name)
                        .setApiJar(jar)
                        .setClassPath(getClassPathFor(subproject, jar))
            }
        }*/
        ProjectFrame projectFrame = new ProjectFrame((String)project.group,
                                                     version,
                                                     projectVersion,
                                                     project.rootProject.projectDir.path,
                                                     providers)
        projectFrame.setVisible(true);
    }

    def getClassPathFor(project, apiJar) {
        def urls = []
        urls << apiJar.toURI().toURL()
        StringBuilder classPath = new StringBuilder()
        project.configurations.compile.collect { dep ->
            if(classPath.length()>0)
                classPath.append(File.pathSeparator)
            urls << dep.toURI().toURL()
            classPath.append(dep)
        }
        urls
    }

    def gatherFromLocalRepo() {
        File repoDir = new File(project.rootProject.repositories.mavenLocal().getUrl())
        def flatDirs = []
        project.rootProject.repositories.each { repo ->
            if(repo instanceof FlatDirectoryArtifactRepository) {
                flatDirs.addAll(repo.getDirs())
            }
        }
        def versions = [:]
        if(project.hasProperty("engOpenVersion")) {
            versions.put("engOpenVersion", project['engOpenVersion'])
        }
        if(project.hasProperty("engGovVersion")) {
            versions.put("engGovVersion", project['engGovVersion'])
        }
        if(project.hasProperty("engITarAndOrPropVersion")) {
            versions.put("engITarAndOrPropVersion", project['engITarAndOrPropVersion'])
        }
        return ProviderGatherer.gatherFromLocalRepo((String)project.group, repoDir, versions, flatDirs as File[])
    }
}
