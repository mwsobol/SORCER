/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.util;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import net.jini.discovery.LookupDiscovery;
import net.jini.lookup.ServiceItemFilter;
import net.jini.lookup.entry.Name;
import sorcer.service.DynamicAccessor;
import sorcer.service.Service;
import sorcer.service.Signature;
import sorcer.service.SignatureException;

import static sorcer.core.SorcerConstants.*;

/**
 * A class which supports a simple Jini multicast lookup. It doesn't register
 * with any ServiceRegistrars it simply interrogates each one that's discovered
 * for a ServiceItem associated with the passed type/signature of a provider.
 */
public class ProviderLookup implements DiscoveryListener, DynamicAccessor {
	private ServiceTemplate template;

	private LookupDiscovery discoverer;

	private Object proxy;

	static final long WAIT_FOR = Sorcer.getLookupWaitTime();

	static final int MAX_TRIES = 7;

	static final private Logger logger = LoggerFactory.getLogger(ProviderLookup.class.getName());

	private int tries = 0;

    //instantiated by reflection
    @SuppressWarnings("unused")
	public ProviderLookup() {
		// do noting
	}

	/*
	 * Returns a @link{Service} with the given signtures.
	 * 
	 * @see sorcer.service.DynamicAccessor#getService(sorcer.service.Signature)
	 */
	public Service getServicer(Signature signature) {
		return getService(signature);
	}

	public static Service getService(Signature signature) {
		ProviderLookup lookup = new ProviderLookup(signature.getProviderName(),
				signature.getServiceType());
		return (Service) lookup.getService();
	}

	/**
	 * Returns a service provider with the specified service type.
	 * 
	 * @param serviceType
	 *            a provider service type (interface)
	 * @return a service provider
	 */
	public static Object getService(Class serviceType) {
		return getService(null, serviceType);
	}

	
	/**
	 * Returns a service provider with the specified name and service type.
	 * 
	 * @param providerName
	 *            The name of the provider to search for
	 * @param serviceType
	 *            The interface to look for
	 * 
	 * @return a service provider
	 */
	public static Object getService(String providerName,
			Class serviceType) {
		ProviderLookup lookup = new ProviderLookup(providerName, serviceType);
		return lookup.getService();
	}
	
	/**
	 * Returns a SORCER service provider with the specified name and service
	 * type.
	 * 
	 * @param providerName
	 *            the name of service provider
	 * @param serviceType
	 *            a provider service type (interface)
	 * @return a SORCER service provider
	 */
	public static Service getProvider(String providerName,
			String serviceType) {
		return (Service) getService(providerName, serviceType);
	}

	/**
	 * Returns a service provider with the specified name and service type.
	 * 
	 * @param providerName
	 *            The name of the provider to search for
	 * @param serviceType
	 *            The interface to look for
	 * 
	 * @return a service provider
	 */
	public static Object getService(String providerName,
			String serviceType) {
		Class type;
		try {
			type = Class.forName(serviceType);
		} catch (ClassNotFoundException cnfe) {
			//logger.error("ProviderLookup", "getService", cnfe);
			return null;
		}
		ProviderLookup lookup = new ProviderLookup(providerName, type);
		return lookup.getService();
	}

	/**
	 * Returns a SORCER service provider with the specified service type, using
	 * a Cataloger if availabe, otherwise using Jini lookup services.
	 * 
	 * @param serviceType
	 *            a provider service type (interface)
	 * @return a SORCER service provider
	 */
	public static Service getProvider(String serviceType) {
		return getProvider(null, serviceType);
	}

	ProviderLookup(String providerName, Class serviceInterface) {
		Class[] serviceTypes = new Class[] { serviceInterface };
		Entry[] attrs = null;
		String pn = providerName;
		if (pn != null && pn.equals(ANY))
			pn = null;
		if (providerName != null) {
			attrs = new Entry[] { new Name(pn) };
		}
		template = new ServiceTemplate(null, serviceTypes, attrs);
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
		proxy = lookupProxy();
		if (proxy != null) {
			terminate();
			return proxy;
		}
		terminate();
		return null;
	}

	private Object lookupProxy() {
		synchronized (this) {
			if (discoverer == null) {
				try {
					discoverer = new LookupDiscovery(SorcerEnv.getLookupGroups());
					// discoverer = new
					// LookupDiscovery(LookupDiscovery.ALL_GROUPS);
					//logger.debug("service lookup for groups: " + Arrays.toString(getGroups()));
					//.logger.debug("WAIT_FOR: " + WAIT_FOR);
					discoverer.addDiscoveryListener(this);
				} catch (IOException ioe) {
					logger.debug("Failed to lookup proxy: " + template);
					logger.error(getClass().getName(), "getService", ioe);
				}
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
		synchronized (this) {
			if (discoverer != null)
				discoverer.terminate();
		}
	}

	/**
	 * Caller of getService ends up here, blocked until we find a proxy.
	 * 
	 * @return the newly downloaded proxy
	 */
	private Object waitForProxy() {
		synchronized (this) {
			while (proxy == null && tries < MAX_TRIES) {
				try {
					wait(WAIT_FOR);
					tries++;
					logger.debug("has tried times: " + tries + " for "
							+ template);
				} catch (InterruptedException ie) {
					logger.error(getClass().getName(), "waitForProxy", ie);
					proxy = null;
					return proxy;
				}

			}
			return proxy;
		}
	}

	/**
	 * Invoked to inform a blocked client waiting in waitForProxy that one is
	 * now available.
	 * 
	 * @param proxy
	 *            the newly downloaded proxy
	 */
	private void signalGotProxy(Object proxy) {
		synchronized (this) {
			if (this.proxy == null) {
				this.proxy = proxy;
				notify();
			}
		}
	}

	/**
	 * Everytime a new ServiceRegistrar is found, we will be called back on this
	 * interface with a reference to it. We then ask it for a service instance
	 * of the type specified in our constructor.
	 */
	public void discovered(DiscoveryEvent event) {
		synchronized (this) {
			if (proxy != null)
				return;
		}
		ServiceRegistrar[] regs = event.getRegistrars();
		for (int i = 0; i < regs.length; i++) {
			ServiceRegistrar reg = regs[i];
			Object foundProxy;
			try {
				foundProxy = reg.lookup(template);
				if (foundProxy != null) {
					signalGotProxy(foundProxy);
					break;
				}
			} catch (RemoteException re) {
				logger.debug("ServiceRegistrar barfed");
				logger.error(getClass().getName(), "discovered", re);
			}
		}
	}

	/**
	 * When a ServiceRegistrar "disappears" due to network partition etc. we
	 * will be advised via a call to this method - as we only care about new
	 * ServiceRegistrars, we do nothing here.
	 */
	public void discarded(DiscoveryEvent anEvent) {
		// do nothing for now
	}

	/**
	 * Added for compatibility with DynamicAccessor. This method is implemented
	 * in { @link sorcer.util.ProviderAccessor } and { @link
	 * sorcer.servme.QosProviderAccessor }
	 */
	public ServiceItem getServiceItem(Signature signature)
			throws SignatureException {
		throw new SignatureException("Not implemented by this service accessor");
	}

    public <T> T getProvider (String name, Class<T> type) {
        ProviderLookup lookup = new ProviderLookup(name, type);
        return (T) lookup.getService();
	}

    public Object getService(ServiceTemplate template, ServiceItemFilter filter) {
        LookupDiscovery lookupDiscovery = null;
        try {
            lookupDiscovery = new LookupDiscovery(SorcerEnv.getLookupGroups());
            SorcerDiscoveryListener resultListener = new SorcerDiscoveryListener(template, 1, SorcerEnv.getLookupMaxMatches(), filter);
            lookupDiscovery.addDiscoveryListener(resultListener);
            return resultListener.get(WAIT_FOR*MAX_TRIES, TimeUnit.MILLISECONDS);
        } catch (IOException ignored) {
            return null;
        } catch (InterruptedException ignored) {
            return null;
        } finally {
            if (lookupDiscovery != null) {
                lookupDiscovery.terminate();
            }
        }
    }

    @Override
    public ServiceItem[] getServiceItems(ServiceTemplate template, int minMatches, int maxMatches, ServiceItemFilter filter, String[] groups) {
        LookupDiscovery lookupDiscovery = null;
        try {
            lookupDiscovery = new LookupDiscovery(groups);
            //SorcerDiscoveryListener resultListener = new SorcerDiscoveryListener(template, minMatches, maxMatches, filter);
            //lookupDiscovery.addDiscoveryListener(resultListener);
            //List<ServiceItem> serviceItems = resultListener.get(WAIT_FOR, TimeUnit.MILLISECONDS);
            Thread.sleep(WAIT_FOR*MAX_TRIES);
            List<ServiceItem>serviceItems=new LinkedList<ServiceItem>();
            for (ServiceRegistrar registrar : lookupDiscovery.getRegistrars()) {
                serviceItems.addAll(Arrays.asList(registrar.lookup(template, maxMatches).items));
            }
            return serviceItems.toArray(new ServiceItem[serviceItems.size()]);
        } catch (IOException ignored) {
            return new ServiceItem[0];
        } catch (InterruptedException ignored) {
            return new ServiceItem[0];
        } finally {
            if (lookupDiscovery != null) {
                lookupDiscovery.terminate();
            }
        }
    }
}

class SorcerDiscoveryListener implements DiscoveryListener, Future<List<ServiceItem>> {
    private ServiceTemplate template;
    private ServiceItemFilter filter;
    private int minResults;
    private int maxResults;
    final private List<ServiceItem> result = new LinkedList<ServiceItem>();
    private boolean canceled;
    private boolean done;

    SorcerDiscoveryListener(ServiceTemplate template, int minResults, int maxResults, ServiceItemFilter filter) {
        this.template = template;
        this.minResults = minResults;
        this.maxResults = maxResults;
        this.filter = filter;
    }

    @Override
    public void discovered(DiscoveryEvent e) {
        ServiceRegistrar[] registrars = e.getRegistrars();
        for (ServiceRegistrar registrar : registrars) {
            if (canceled || done) return;
            try {
                ServiceMatches serviceMatches = registrar.lookup(template, maxResults);
                for (ServiceItem item : serviceMatches.items) {
                    if (filter != null && filter.check(item)) {
                        synchronized (result) {
                            result.add(item);
                            done = result.size() >= minResults;

                        }
                    }
                }
            } catch (RemoteException ignored) {

            }
        }
    }

    @Override
    public synchronized void discarded(DiscoveryEvent e) {
        //ignored
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        //cancel if not done
        return !done || (canceled = true);
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public synchronized List<ServiceItem> get() throws InterruptedException {
        synchronized (result){
            if (!result.isEmpty()) return result;
        }
        wait();
        return result;
    }

    @Override
    public synchronized List<ServiceItem> get(long timeout, TimeUnit unit) throws InterruptedException {
        synchronized (result){
            if (!result.isEmpty()) return result;
        }
        wait(unit.toMillis(timeout));
        return result;
    }
}
