/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
 * Copyright 2013 Sorcersoft.com S.A.
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

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.ServiceItemFilter;
import net.jini.lookup.entry.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.Provider;
import sorcer.jini.lookup.entry.SorcerServiceInfo;
import sorcer.river.Filters;
import sorcer.util.*;

import java.util.ArrayList;
import java.util.List;

import static sorcer.core.SorcerConstants.ANY;

/**
 * A service accessing facility that allows to find dynamically a network
 * service provider matching its {@link Signature}. This class uses the Factory
 * Method pattern with the {@link DynamicAccessor} interface.
 *
 * @author Mike Sobolewski
 */
public class Accessor {

    protected final static Logger logger = LoggerFactory.getLogger(Accessor.class.getName());

    /**
     * A factory returning instances of {@link Service}s.
     */
    private static DynamicAccessor accessor;
    private static int minMatches = Sorcer.getLookupMinMatches();
    private static int maxMatches = Sorcer.getLookupMaxMatches();
    private static ProviderNameUtil providerNameUtil = new SorcerProviderNameUtil();
    final public static DynamicAccessor nonCachingAccessor;

    static {
        initialize(Sorcer.getProperties().getProperty(SorcerConstants.S_SERVICE_ACCESSOR_PROVIDER_NAME));
        if ("sorcer.util.ServiceAccessor".equals(getAccessorType()))
            nonCachingAccessor = accessor;
        else
            nonCachingAccessor = new ServiceAccessor();
    }

    public static void initialize(String providerType) {
        try {
            logger.debug("SORCER DynamicAccessor provider: " + providerType);
            Class type = Class.forName(providerType, true, Thread.currentThread().getContextClassLoader());
            if(!DynamicAccessor.class.isAssignableFrom(type)){
                throw new IllegalArgumentException("Configured class must implement DynamicAccessor: "+providerType);
            }
            accessor = (DynamicAccessor) type.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No service accessor available for: " + providerType,e);
        } catch (InstantiationException e) {
            throw new RuntimeException("No service accessor available for: " + providerType,e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("No service accessor available for: " + providerType,e);
        }
    }

    public static String getAccessorType(){
        return accessor.getClass().getName();
    }

    public static <T> T getService(Class<T> type){
        return getService(null, type);
    }

    public static ServiceTemplate getServiceTemplate(ServiceID serviceID,
            String providerName, Class[] serviceTypes,
            String[] publishedServiceTypes) {
        Class[] types;
        List<Entry> attributes = new ArrayList<Entry>(2);

        if (providerName != null && !providerName.isEmpty() && !ANY.equals(providerName))
            attributes.add(new Name(providerName));

        if (publishedServiceTypes != null) {
            SorcerServiceInfo st = new SorcerServiceInfo();
            st.publishedServices = publishedServiceTypes;
            attributes.add(st);
        }

        if (serviceTypes == null) {
            types = new Class[] { Provider.class };
        } else {
            types = serviceTypes;
        }

        logger.debug("getServiceTemplate >> \n serviceID: {}\nproviderName: {}\nserviceTypes: {}\npublishedServiceTypes: {}",
                     serviceID, providerName, StringUtils.arrayToString(serviceTypes), StringUtils.arrayToString(publishedServiceTypes));

        return new ServiceTemplate(serviceID, types, attributes.toArray(new Entry[attributes.size()]));
    }

    private static String overrideName(String providerName, Class serviceType) {
        if (providerName == null || "*".equals(providerName)) return providerName;
        if (SorcerConstants.NAME_DEFAULT.equals(providerName))
            providerName = providerNameUtil.getName(serviceType);
        return Sorcer.getActualName(providerName);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getService(String serviceName, Class<T> serviceType){
        serviceName = overrideName(serviceName, serviceType);
        ServiceTemplate serviceTemplate = getServiceTemplate(null, serviceName, new Class[]{serviceType}, null);
        return (T) getService(serviceTemplate, Filters.any());
    }

    public static Object getService(ServiceTemplate template, ServiceItemFilter filter, String[] groups) {
        ServiceItem serviceItem = getServiceItem(template, filter, groups);
        return serviceItem == null ? null : serviceItem.service;
    }

    public static Object getService(ServiceTemplate template, ServiceItemFilter filter) {
        return getService(template, filter, Sorcer.getLookupGroups());
    }

    public static Object getService(ServiceID serviceID){
        return getService(serviceID, null, null);
    }

    public static Object getService(ServiceID serviceID, Class[] serviceTypes, Entry[] attrSets){
        return getService(serviceID, serviceTypes, attrSets, Sorcer.getLookupGroups());
    }

    public static Object getService(ServiceID serviceID, Class[] serviceTypes, Entry[] attrSets, String[] groups){
        ServiceTemplate serviceTemplate = new ServiceTemplate(serviceID, serviceTypes, attrSets);
        return getService(serviceTemplate, Filters.any(), groups);
    }

    /**
     * Returns a service item containing the servicer matching its {@link Signature}
     * using the particular factory <code>accessor</code> of this service accessor facility.
     *
     * @param signature
     *            the signature of requested servicer
     * @return the requested {@link ServiceItem}
     */
    public static ServiceItem getServiceItem(Signature signature){
        return getServiceItem(signature.getProviderName(), signature.getServiceType());
    }

    /**
     * Returns a servicer matching its {@link Signature} using the particular
     * factory <code>accessor</code> of this service accessor facility.
     *
     * @param signature
     *            the signature of requested servicer
     * @return the requested {@link Service}
     */
    public static Object getService(Signature signature) {
        ServiceItem serviceItem = getServiceItem(signature);
        return serviceItem == null ? null : serviceItem.service;
    }

    public static ServiceItem getServiceItem(ServiceTemplate template, ServiceItemFilter filter){
        return getServiceItem(template, filter, Sorcer.getLookupGroups());
    }

    public static ServiceItem[] getServiceItems(Class type, ServiceItemFilter filter){
        ServiceTemplate serviceTemplate = getServiceTemplate(null, null, new Class[]{type}, null);
        return getServiceItems(serviceTemplate, filter);
    }

    public static ServiceItem getServiceItem(ServiceTemplate template, ServiceItemFilter filter, String[] groups){
        ServiceItem[] serviceItems = getServiceItems(template, 1, 1, filter, groups);
        return serviceItems.length > 0 ? serviceItems[0] : null;
    }

    public static ServiceItem getServiceItem(String providerName, Class serviceType){
        providerName = overrideName(providerName, serviceType);
        ServiceTemplate serviceTemplate = getServiceTemplate(null, providerName, new Class[]{serviceType}, null);
        return getServiceItem(serviceTemplate, Filters.any());
    }

    public static ServiceItem[] getServiceItems(ServiceTemplate template, ServiceItemFilter filter){
        return getServiceItems(template, filter, Sorcer.getLookupGroups());
    }

    public static ServiceItem[] getServiceItems(ServiceTemplate template, ServiceItemFilter filter, String[] groups){
        return getServiceItems(template, minMatches, maxMatches, filter, groups);
    }

    public static ServiceItem[] getServiceItems(ServiceTemplate template, int minMatches, int maxMatches, ServiceItemFilter filter, String[] groups){
        checkNullName(template);
        if (filter == null) filter = Filters.any();
        return accessor.getServiceItems(template, minMatches, maxMatches, filter, groups);
    }

    private static void checkNullName(ServiceTemplate template) {
        if (template.attributeSetTemplates == null)
            return;
        for (Entry attr : template.attributeSetTemplates) {
            if (attr instanceof Name) {
                Name name = (Name) attr;
                if (ANY.equals(name.name)) {
                    name.name = null;
                    logger.warn("Requested service with name '*'", new IllegalArgumentException());
                }
            } else if (attr instanceof SorcerServiceInfo) {
                SorcerServiceInfo info = (SorcerServiceInfo) attr;
                if (ANY.equals(info.providerName)) {
                    info.providerName = null;
                    logger.warn("Requested service with name '*'", new IllegalArgumentException());
                }
            }
        }
    }

    /**
     * Test if provider is still replying.
     *
     * @param provider the provider to check
     * @return true if a provider is alive, otherwise false
     */
    public static boolean isAlive(Provider provider) {
        if (provider == null)
            return false;
        try {
            provider.getProviderName();
            return true;
        } catch (Exception e) {
            logger.debug("Provider is dead " + e.getMessage());
            return false;
        }
    }

}
