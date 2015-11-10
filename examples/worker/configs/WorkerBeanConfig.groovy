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
package sorcer.worker.provider.configs

import org.rioproject.config.Component
import sorcer.worker.provider.impl.WorkerBean

/**
 * Configuration for the Worker bean
 *
 * @author Mike Sobolewski
 */
@Component('sorcer.core.provider.ServiceProvider')
class WorkerBeanConfig {

    /* service provider config properties */
    String name = "Worker Bean"
    String description = "Worker - bean provider"
    String location = "AFRL/WPAFB"
    String iconName = "sorcer.jpg";
    // String properties="provider.properties";

    boolean spaceEnabled = true
    boolean matchInterfaceOnly = false
    // boolean workerTransactional = true;
    // int workerCount = 100;
    // boolean  monitorEnabled = true;


    /* An array of service implementation classes required to load the service */
    Class[] getBeanClasses() {
        return [WorkerBean.class]
    }

    /* An array of service published service types by this bean */
    Class[] getPublishedInterfaces() {
        return [Thread.currentThread().contextClassLoader.loadClass(serviceType)]
    }

    /* Service types are declared as a static properties so they can be referenced above */
    static String serviceType = "sorcer.worker.provider.Worker"

}
