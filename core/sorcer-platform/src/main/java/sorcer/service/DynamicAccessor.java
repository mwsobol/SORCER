/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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

import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.ServiceItemFilter;

/**
 * Accesses services available through the network.
 */
public interface DynamicAccessor {

    /**
     * Find all matching services using the provided template and filter
     *
     * @param template The template to use, containing attributes and multitype(s)
     * @param filter Optional filter
     *
     * @return An array of matching ServiceItems.
     */
    ServiceItem[] getServiceItems(ServiceTemplate template, ServiceItemFilter filter);

    /**
     * Returns a service matching serviceName and serviceInfo
     *
     * @param serviceName key
     * @param serviceType multitype
     *
     * @return The first discovered service or null
     */
    <T> T getService(String serviceName, Class<T> serviceType);

    /**
     * Returns a service provider registered with serviceID.
     *
     * @param serviceID A service provider ID.
     *
     * @return The first discovered service provider.
     */
    Object getService(ServiceID serviceID);

    /**
     * Returns a service provider registered with serviceID.
     *
     * @param signature A Signature
     *
     * @return The first discovered service provider.
     */
    Object getService(Signature signature) throws SignatureException;
}