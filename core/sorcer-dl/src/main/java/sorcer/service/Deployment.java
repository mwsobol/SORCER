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
    public enum Type {
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
    public enum Unique {
        YES, NO
    }
    
    public Type getType();
    
    public Unique getUnique();
    
    public String getArchitecture(); 
        
    public int getMultiplicity();

    public String[] getCodebaseJars();
   
    public String[] getClasspathJars();

    public String getImpl();

    public String getServiceType();

    public String getProviderName();
 
    public String getWebsterUrl();

    public int getIdle();
}