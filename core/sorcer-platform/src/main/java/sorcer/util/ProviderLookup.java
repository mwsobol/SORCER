/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.util;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscovery;
import net.jini.lookup.entry.Name;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.Provider;
import sorcer.service.Accessor;
import sorcer.service.DynamicAccessor;
import sorcer.service.Service;
import sorcer.service.Signature;

//import sorcer.service.*;

/**
 * A class which supports a simple Jini multicast lookup. It doesn't register
 * with any ServiceRegistrars it simply interrogates each one that's discovered
 * for a ServiceItem associated with the passed type/signature of a provider.
 */
public class ProviderLookup implements DynamicAccessor, SorcerConstants {
    private ServiceTemplate template;
    private Listener listener;
	private LookupDiscovery discoverer;
    //static final long WAIT_FOR = Sorcer.getLookupWaitTime();
    /* Since we wait for 100 milliseconds, we want the amount of seconds x 10 */
	final static int MAX_TRIES = (int)TimeUnit.MINUTES.toSeconds(3)*10; 
    static final private Logger logger = /*Log.getTestLog();*/ Logger.getLogger(ProviderLookup.class.getName());
    private int tries = 0;

    public static void init() {
        Accessor.setAccessor(new ProviderLookup());
    }

	public ProviderLookup() {
        // do noting
    }

    /*
     * Returns a @link{Service} with the given signatures.
     *
     * @see sorcer.service.DynamicAccessor#getService(sorcer.service.Signature)
     */
    public Service getService(Signature signature) {
        return getProvider(signature);
    }

    /**
     * Returns a SORCER service provider with the specified signature, using
     * a Cataloger if available, otherwise using Jini lookup services.
     *
     * @param signature a provider signature
     * @return a SORCER service provider
     */
	public static Provider getProvider(Signature signature) {
		ProviderLookup lookup = new ProviderLookup(signature.getServiceType(),
				signature.getProviderName());
		return (Provider) lookup.getService();
	}

	 /**
     * Returns a SORCER service provider with the specified service type and name, using
     * a Cataloger if available, otherwise using Jini lookup services.
     *
     * @param serviceType a provider service type (interface)
     * @param providerName a provider name
     * @return a SORCER service provider
     */
	public static Provider getProvider(Class serviceType, String providerName) {
		ProviderLookup lookup = new ProviderLookup(serviceType, providerName);
		return (Provider) lookup.getService();
	}
    
    /**
     * Returns a SORCER service provider with the specified service type, using
     * a Cataloger if available, otherwise using Jini lookup services.
     *
     * @param serviceType a provider service type (interface)
     * @return a SORCER service provider
     */
	public static Provider getProvider(Class serviceType) {
		return getProvider(serviceType, null); 
	}
	
    /**
     * Returns a service provider with the specified service type.
     *
     * @param serviceType a provider service type (interface)
     * @return a service provider
     */
    public static Object getService(Class<?> serviceType) {
        return getService(null, serviceType);
    }


    /**
     * Returns a service provider with the specified name and service type.
     *
     * @param providerName The name of the provider to search for
     * @param serviceType  The interface to look for
     * @return a service provider
     */
    public static Object getService(String providerName, Class<?> serviceType) {
        ProviderLookup lookup = new ProviderLookup(serviceType, providerName);
        return lookup.getService();
    }

    /**
     * Returns a service provider with the specified name and service type.
     *
     * @param providerName The name of the provider to search for
     * @param serviceType  The interface to look for
     * @return a service provider
     */
    public static Object getService(String providerName, String serviceType) {
        Class type;
        try {
            type = Class.forName(serviceType);
        } catch (ClassNotFoundException cnfe) {
            //logger.throwing("ProviderLookup", "getService", cnfe);
            return null;
        }
        ProviderLookup lookup = new ProviderLookup(type, providerName);
        return lookup.getService();
    }

    private ProviderLookup(Class<?> serviceInterface, String providerName) {
        Class[] serviceTypes = new Class[]{serviceInterface};
        Entry[] attrs = null;
        String pn = providerName;
        if (pn != null && pn.equals(ANY))
            pn = null;
        if (providerName != null) {
            attrs = new Entry[]{new Name(pn)};
        }
        template = new ServiceTemplate(null, serviceTypes, attrs);
        listener = new Listener(template);
    }

    /**
     * Having created a Lookup (which means it now knows what type of service
     * you require), invoke this method to attempt to locate a service of that
     * type. The result should be cast to the interface of the service you
     * originally specified to the constructor.
     *
     * @return proxy for the service type you requested - could be an rmi stub
     *         or a smart proxy.
     */
    private Object getService() {
        Object proxy = lookupProxy();
        terminate();
        return proxy;
    }

    private Object lookupProxy() {
    	// com.sun.jini.start.ClassLoaderUtil.displayContextClassLoaderTree();
        if (discoverer == null) {
            try {
                discoverer = new LookupDiscovery(getGroups());
                // discoverer = new
                // LookupDiscovery(LookupDiscovery.ALL_GROUPS);
                //logger.finer("service lookup for groups: " + Arrays.toString(getGroups()));
                //logger.finer("WAIT_FOR: " + WAIT_FOR);
                discoverer.addDiscoveryListener(listener);
            } catch (IOException ioe) {
                logger.finer("Failed to lookup proxy: " + template);
                logger.throwing(getClass().getName(), "getService", ioe);
            }
        }
        return waitForProxy();
    }

    /**
     * Location of a service causes the creation of some threads. Call this
     * method to shut those threads down either before exiting or after a proxy
     * has been returned from getService().
     */
    void terminate() {
        if (discoverer != null) {
            discoverer.terminate();
        }
    }

    /**
     * Caller of getService ends up here, blocked until we find a proxy.
     *
     * @return the newly downloaded proxy
     */
    private Object waitForProxy() {
        while (listener.serviceRef.get() == null && tries < (MAX_TRIES)) {
            try {
                listener.lookup();
                //wait(100);
                Thread.sleep(100);
                tries++;
//                logger.fine("has tried times: " + tries + " for "+ template);
            } catch (InterruptedException ie) {
                logger.throwing(getClass().getName(), "waitForProxy", ie);
                return null;
            }

        }
        return listener.serviceRef.get();
    }


    class Listener implements DiscoveryListener {
        final AtomicReference<Object> serviceRef = new AtomicReference<Object>();
        private final ServiceTemplate template;
        private final List<ServiceRegistrar> lookups = new ArrayList<ServiceRegistrar>();

        public Listener(ServiceTemplate template) {
            this.template = template;
        }

        void lookup() {
            for(ServiceRegistrar registrar : lookups) {
                try {
                    Object service = registrar.lookup(template);
                    if(service!=null) {
                        serviceRef.set(service);
                        break;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        public void discovered(DiscoveryEvent discoveryEvent) {
            Collections.addAll(lookups, discoveryEvent.getRegistrars());
        }

        public void discarded(DiscoveryEvent discoveryEvent) {
        }
    }

    /**
     * Returns a list of groups as defined in the SORCER environment
     * configuration, the sorcer.env file.
     *
     * @return a list of group names
     * @see Sorcer
     */
    protected static String[] getGroups() {
        return Sorcer.getLookupGroups();
    }

    /**
     * Added for compatibility with DynamicAccessor. This method is implemented
     * in { @link sorcer.util.ProviderAccessor } and { @link
     * sorcer.servme.QosProviderAccessor }
     */
    public ServiceItem getServiceItem(Signature signature) {
    	return null;
        //throw new SignatureException("Not implemented by this service accessor");
    }

}
