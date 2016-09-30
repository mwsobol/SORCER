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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction


/**
 * 
 * @author Dennis Reedy
 */
class StopMe extends DefaultTask {
    @Input @Optional
    projectId
    @Input @Optional
    boolean force

    @Override
    String getDescription() {
        return "Terminates processes started by the StartMe task"
    }

    @TaskAction
    def stop() {
        projectId = projectId==null?project.name:projectId
        project.logger.info "projectId: ${projectId}"
        Stopper.stop(projectId, force)
    }
}
