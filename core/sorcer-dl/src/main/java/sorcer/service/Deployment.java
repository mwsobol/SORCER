/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

package sorcer.service;

import java.io.Serializable;

/**
 * Attributes related to signature based deployment.
 *
 * @author Mike Sobolewski
 */
public interface Deployment extends Arg, Serializable {

	 /**
     * There are three types of provisioning: per signature (SELF), for a
     * collection of signatures as a federation (all signatures in exertion with
     * Type.FED), and no provisioning (suspended provisioning) for a signature
     * (NONE)
     */
    enum Type {
        SELF, FED, NONE
    }
    
    /**
     * A service can be deployed with uniqueness. If a service is unique, a new instance is always
     * created. If a service is not unique, the following behavior comes into play:
     * <ul>
     *     <li>If the service is @{code Type.SELF} it will be checked to see if has already been deployed. If not
     *     deployed it will be created in it's own deployment. If found it will not be deployed.</li>
     *     <li>If the service is @{code Type.FED} it will be checked to see if has already been deployed. If not
     *     deployed it will be created as part of a deployment. If deployed, the number of instances will be
     *     incremented within it's current deployment.</li>
     * </ul>.
     */
    enum Unique {
        YES, NO
    }

    /**
     *
     * <ul>
     *     <li>DYNAMIC indicates that a service will be deployed to available machine instances that support the
     *     service's operational criteria up to the amount specified by the number of planned instances.</li>
     *     <li>FIXED fiType that the service will be deployed to every machine instance that supports the
     *     service's operational criteria, and every machine will have the number of planned instances.</li>
     * </ul>
     */
    enum Strategy {
        DYNAMIC, FIXED
    }
    
    Type getType();
    
    Unique getUnique();
    
    String getArchitecture();
        
    int getMultiplicity();

    String[] getCodebaseJars();
   
    String[] getClasspathJars();

    String getImpl();

    String getServiceType();

    String getProviderName();
 
    String getWebsterUrl();

    int getIdle();

    Strategy getStrategy();
}