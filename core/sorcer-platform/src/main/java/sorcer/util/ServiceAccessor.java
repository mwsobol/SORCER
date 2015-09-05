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

import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryManagement;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryManager;
import net.jini.lookup.ServiceItemFilter;
import net.jini.lookup.entry.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.service.DynamicAccessor;
import sorcer.service.Signature;
import sorcer.service.SignatureException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

/**
 * A service discovery and management utility allowing to access services by
 * matching service templates and passing filters provided by clients. The
 * optional usage of a service lookup cache is provided for clients that require
 * lookup for multiple services frequently.
 * <p>
 * A similar service discovery and management functionality is provided by a
 * SORCER Cataloger service provider with round robin load balancing.
 * <p>
 * The continuous discovery of SORCER services is usually delegated to
 * Catalogers while other SORCER service providers can query effectively
 * Cataloger's cached services with round robin load balancing.
 * <p>
 * The individual SORCER services should be accessed using the
 * {@link sorcer.util.ProviderAccessor} subclass, that uses local cached proxies
 * for frequently used SORCER infrastructure services. ProviderAccessor normally
 * uses Cataloger if available, otherwise uses Jini lookup services as
 * implemented by the ServiceAccessor.
 *
 * @see sorcer.util.ProviderAccessor
 *
 * @author Mike Sobolewski
 */
public class ServiceAccessor implements DynamicAccessor {

	static Logger logger = LoggerFactory.getLogger(ServiceAccessor.class.getName());

	static private boolean cacheEnabled = Sorcer.isLookupCacheEnabled();

	static long WAIT_FOR = Sorcer.getLookupWaitTime();
	
	// wait for cataloger 2 sec  = LUS_REPEAT x 200
	// since then falls back on LUSs managed by ServiceAccessor
	// wait for service accessor 5 sec  = LUS_REPEAT x 500
	final static int LUS_REPEAT = 10;

	private static DiscoveryManagement ldManager = null;

	private static ServiceDiscoveryManager sdManager = null;

	private static LookupCache lookupCache = null;

	protected static String[] lookupGroups = Sorcer.getLookupGroups();
	
	private static int MIN_MATCHES = Sorcer.getLookupMinMatches();

	private static int MAX_MATCHES = Sorcer.getLookupMaxMatches();

    protected Map<String, Object> cache = new HashMap<String, Object>();

    protected ProviderNameUtil providerNameUtil = new SorcerProviderNameUtil();

    public ServiceAccessor() {
        openDiscoveryManagement(SorcerEnv.getLookupGroups());
    }

	public ServiceDiscoveryManager getServiceDiscoveryManager() {
		return sdManager;
	}

	/**
	 * Returns a service item containing a service matching providerName and
	 * serviceInfo using Jini lookup service.
	 * 
	 * @param providerName name
	 * @param serviceType type
	 * @return A ServiceItem
	 */
	public static ServiceItem getServiceItem(String providerName, Class serviceType) {
		ServiceItem si = null;
		try {
			Class[] serviceTypes = new Class[] { serviceType };
			Entry[] attrSets = new Entry[] { new Name(providerName) };
			ServiceTemplate st = new ServiceTemplate(null, serviceTypes, attrSets);
			si = sdManager.lookup(st, null, WAIT_FOR);
		} catch (IOException | InterruptedException e) {
			logger.error("getServiceItem", e);
		}
		return si;
	}
	
	/**
	 * Returns a service item containing a service matching only the
	 * serviceInfo. It uses Jini lookup service.
	 * 
	 * @param serviceType type
	 * @return a ServiceItem
	 */
	protected ServiceItem getServiceItem(Class serviceType) throws SignatureException {
		ServiceItem si = null;
		try {
			Class[] serviceTypes = new Class[] { serviceType };
			ServiceTemplate st = new ServiceTemplate(null, serviceTypes, null);
			si = sdManager.lookup(st, null, WAIT_FOR);
		} catch (IOException | InterruptedException e) {
			logger.error("getServiceItem", e);
		}
		return si;
	}



	/**
	 * Returns a service item matching a service template and passing a filter
	 * with Jini registration groups. Note that template matching is a remote
	 * operation - matching is done by lookup services while passing a filter is
	 * done on the client side. Clients should provide a service filter, usually
	 * as an object of an inner class. A filter narrows the template matching by
	 * applying more precise, for example boolean selection as required by the
	 * client. No lookup cache is used by this <code>getServiceItem</code>
	 * method.
	 *
	 * @param template
	 *            template to match remotely.
	 * @param filter
	 *            filer to use or null.
	 * @param groups
	 *            lookup groups to search in.
	 * @return a ServiceItem that matches the template and filter or null if
	 *         none is found.
	 */
    public ServiceItem getServiceItem(ServiceTemplate template, ServiceItemFilter filter, String[] groups) {
		ServiceItem si = null;
		try {
		    openDiscoveryManagement(groups);
			si = sdManager.lookup(template, filter, WAIT_FOR);
		} catch (IOException | InterruptedException ie) {
			logger.error("getServiceItem", ie);
		}
		return si;
	}

	/**
	 * Returns a service item using a lookup cache. The service item matches a
	 * service template and passes a filter associated with the lookup cache at
	 * the cache creation time. Also a returned service item passes the filter
	 * used as the parameter in this method.
	 * <p>
	 * Note that template matching is a remote operation - matching is done by
	 * lookup services while passing a filter is done on the client side.
	 * Clients should provide a service filter, usually as an object of an inner
	 * class. A filter narrows the template matching by applying more precise,
	 * for example boolean selection as required by the client.
	 *
	 * @param filter
	 *            the filter to apply.
	 * @return a ServiceItem matching the filter.
	 */
	/*public ServiceItem getServiceItem(ServiceItemFilter filter) {
		ServiceItem si = null;
		try {
            openDiscoveryManagement(SorcerEnv.getLookupGroups());
			if (lookupCache != null)
				si = lookupCache.lookup(filter);
			// The item may not be in the cache if the lookup cache was just
			// started
			if (si == null) {
				try {
					si = sdManager.lookup(
							new ServiceTemplate(null, null, null), filter,
							WAIT_FOR);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		} catch (IOException ioe) {
			logger.error(ServiceAccessor.class.getName(), "getServiceItem",
                    ioe);
			closeLookupCache();
		}
		closeDiscoveryManagement();
		return si;
	}*/

	/**
	 * Returns all service items that match a service template and pass a filter
	 * with Jini registration groups. Note that template matching is a remote
	 * operation - matching is done by lookup services while passing a filter is
	 * done on the client side. Clients should provide a service filter, usually
	 * as an object of an inner class. A filter narrows the template matching by
	 * applying more precise, for example boolean selection as required by the
	 * client.
	 *
	 *
	 * @param template
	 *            template to match remotely.
	 * @param filter
	 *            filer to use or null.
	 * @param groups
	 *            lookup groups to search in.
	 * @return a ServiceItem[] that matches the template and filter.
	 */
	/*public ServiceItem[] getServiceItems(ServiceTemplate template, ServiceItemFilter filter, String[] groups) {
		ServiceItem sis[] = null;
		try {
			openDiscoveryManagement(groups);
			sis = sdManager.lookup(template, MIN_MATCHES, MAX_MATCHES, filter, WAIT_FOR);
		} catch (IOException  | InterruptedException e) {
			logger.error("getServiceItems", e);
		}
		closeDiscoveryManagement();
		return sis;
	}*/

	/**
	 * Returns a collection service items using a lookup cache. The service
	 * items match a service template and pass a filter associated with the
	 * lookup cache at the cache creation time. Also returned service items pass
	 * the filter used as the parameter in this method.
	 * <p>
	 * Note that template matching is a remote operation - matching is done by
	 * lookup services while passing a filter is done on the client side.
	 * Clients should provide a service filter, usually as an object of an inner
	 * class. A filter narrows the template matching by applying more precise,
	 * for example boolean selection as required by the client.
	 *
	 *
	 * @param filter
	 *            the filter to apply.
	 * @return a ServiceItem[] matching the filter.
	 */
	public ServiceItem[] getServiceItems(ServiceItemFilter filter) {
		ServiceItem sis[] = null;
		if (lookupCache != null)
			sis = lookupCache.lookup(filter, MAX_MATCHES);
		return sis;
	}

	/**
	 * Creates a service lookup and discovery manager with a provided service
	 * template, lookup cache filter, and list of jini groups.
	 *
	 * @param groups River group names
	 */
	protected void openDiscoveryManagement(String[] groups) {
		if (sdManager == null) {
			LookupLocator[] locators = getLookupLocators();
			try {
				logger.debug("[openDiscoveryManagement] SORCER Group(s): {}, Locators: {}",
                             SorcerUtil.arrayToString(groups), SorcerUtil.arrayToString(locators));

				ldManager = new LookupDiscoveryManager(groups, locators, null);
				sdManager = new ServiceDiscoveryManager(ldManager, new LeaseRenewalManager());
			} catch (Throwable t) {
				logger.error(ServiceAccessor.class.getName(),
						"openDiscoveryManagement", t);
			}
		}
		// Opening a lookup cache
		openCache();
	}

	/**
	 * Terminates lookup discovery and service discovery mangers.
	 */
	private void closeDiscoveryManagement() {
		if (cacheEnabled) {
			return;
		}
		if (ldManager != null) {
			ldManager.terminate();
		}
		if (sdManager != null) {
			sdManager.terminate();
		}
		closeLookupCache();
		ldManager = null;
		sdManager = null;
	}

	/**
	 * Creates a lookup cache for the existing service discovery manager
	 */
	private void openCache() {
		if (cacheEnabled && lookupCache == null) {
			try {
				lookupCache = sdManager.createLookupCache(null, null, null);
			} catch (RemoteException e) {
				closeLookupCache();
			}
		}
	}

	/**
	 * Terminates a lookup cache used by this ServiceAccessor.
	 */
	private void closeLookupCache() {
		if (lookupCache != null) {
			lookupCache.terminate();
			lookupCache = null;
		}
	}

	/**
	 * Returns a service matching serviceType, service attributes (entries), and
	 * passes a provided filter.
	 *
	 * @param attributes   attributes of the requested provider
	 * @param serviceType type of the requested provider
	 * @return a SORCER provider
	 */
    @SuppressWarnings("unchecked")
	public <T>T getService(Class<T> serviceType, Entry[] attributes, ServiceItemFilter filter) {
		if (serviceType == null) {
			throw new IllegalArgumentException("Missing service type for a ServiceTemplate");
		}
        ServiceTemplate tmpl = new ServiceTemplate(null, new Class[] { serviceType }, attributes);
		ServiceItem si = getServiceItem(tmpl, filter);
		if (si != null)
			return (T)si.service;
		else
			return null;
	}

	/**
	 * Returns a service matching serviceName and serviceInfo using Jini lookup
	 * service.
	 * 
	 * @param serviceName name
	 * @param serviceType type
	 * @return a service provider
	 */
	public <T> T getService(String serviceName, Class<T> serviceType) {
		T proxy = null;
		if (serviceName != null && serviceName.equals(SorcerConstants.ANY))
			serviceName = null;
		int tryNo = 0;
		while (tryNo < LUS_REPEAT) {
			logger.info("trying to get service: {}: {}; attempt: {}...",serviceType, serviceName, tryNo);
			try {
				tryNo++;
				proxy = getService(serviceType, new Entry[] { new Name(serviceName) }, null);
				if (proxy != null)
					break;

				Thread.sleep(WAIT_FOR);
			} catch (Exception e) {
				logger.error("" + ServiceAccessor.class, "getService", e);
			}
		}
		logger.info("got LUS service [type={} name={}]: {}", serviceType.getName(), serviceName, proxy);
		return proxy;
	}


    /**
     * Implements DynamicAccessor interface - provides compatibility with ProviderAccessor
     */
    public <T> T getProvider(String serviceName, Class<T> serviceType) {
        return getService(serviceName, serviceType);
    }

    /**
	 * Returns a service matching a given template filter, and Jini lookup
	 * service.
	 *
	 * @param template service template
	 * @param filter   service filter
	 * @param groups   River groups list
	 * @return a service provider
	 */
	public Object getService(ServiceTemplate template, ServiceItemFilter filter, String[] groups) {
        ServiceItem si;
        if (groups == null) {
            return getServiceItem(template, filter);
        } else {
            si = getServiceItem(template, filter, SorcerEnv.getLookupGroups());
        }
        return si == null ? null : si.service;
    }

	/**
	 * Returns a list of lookup locators with the URLs defined in the SORCER
	 * environment
	 *
	 * @see sorcer.util.Sorcer
	 *
	 * @return a list of locators for unicast lookup discovery
	 */
	private LookupLocator[] getLookupLocators() {
		String[] locURLs = SorcerEnv.getLookupLocators();
        if (locURLs == null || locURLs.length == 0) {
            return null;
        }
        List<LookupLocator> locators = new ArrayList<LookupLocator>(locURLs.length);
        logger.debug("ProviderAccessor Locators: " + Arrays.toString(locURLs));

		for (String locURL : locURLs)
			try {
				locators.add(new LookupLocator(locURL));
			} catch (Throwable t) {
				logger.warn(
                        "Invalid Lookup URL: " + locURL);
			}

		if (locators.isEmpty())
			return null;
        return locators.toArray(new LookupLocator[locators.size()]);
	}

    public void terminateDiscovery() {
		sdManager.terminate();
		sdManager = null;
	}

    public ServiceItem getServiceItem(Signature signature) {
        return getServiceItem(signature.getProviderName(),
                signature.getServiceType());
    }

    /**
     * Returns a SORCER service provider registered with serviceID.
     *
     * @param serviceID
     *            a service provider ID
     * @return a SORCER provider service
	 */
	public Object getService(ServiceID serviceID) {
        return getService(serviceID, null, null, SorcerEnv.getLookupGroups());
    }

    /**
     * Returns a SORCER service provider matching a registered serviceID,
     * serviceTypes, attribute set, and Jini groups.
     *
     * @param serviceID
     *            a service provider ID
     * @param serviceTypes
     *            service types to match
     * @param attrSets
     *            a list of attributes describing the requested service
     * @param groups
     *            Jini lookup service groups
     * @return a SORCER provider service
     */
    public Object getService(ServiceID serviceID, Class[] serviceTypes,
                             Entry[] attrSets, String[] groups) {
        ServiceTemplate st = new ServiceTemplate(serviceID, serviceTypes, attrSets);
        return getService(st, null, groups);
    }

    public ServiceItem getServiceItem(ServiceTemplate template, ServiceItemFilter filter) {
        try {
            return sdManager.lookup(template, filter, WAIT_FOR);
        } catch (InterruptedException e) {
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public ServiceItem[] getServiceItems(ServiceTemplate template, ServiceItemFilter filter) {
        return doGetServiceItems(template, MIN_MATCHES, MAX_MATCHES, filter);
    }

    public Object getService(ServiceTemplate template, ServiceItemFilter filter) {
        ServiceItem serviceItem = getServiceItem(template, filter);
        return serviceItem == null ? null : serviceItem.service;
    }

    public Object getService(ServiceID serviceID, Class[] serviceTypes, Entry[] attrSets) {
        ServiceTemplate st = new ServiceTemplate(serviceID, serviceTypes,
                attrSets);
        return getService(st, null);
    }

	@Override
	public ServiceItem[] getServiceItems(ServiceTemplate template, int minMatches, int maxMatches, ServiceItemFilter filter, String[] groups) {
		return getServiceItems(template, minMatches, maxMatches, filter, groups, LUS_REPEAT);
	}

    public ServiceItem[] getServiceItems(ServiceTemplate template, int minMatches, int maxMatches, ServiceItemFilter filter, String[] groups, int lusRepeat) {
        if (groups != null) {
            Set<String> defaultGroups = new HashSet<String>(Arrays.asList(SorcerEnv.getLookupGroups()));
            Set<String> userGroups = new HashSet<String>(Arrays.asList(groups));
            if (!defaultGroups.equals(userGroups)) {
                throw new IllegalArgumentException("User requested River group other than default, this is currently unsupported");
            }
        }
        logger.info("Lookup {}, lusRepeat: {}, timeout: {}", formatServiceTemplate(template), lusRepeat, WAIT_FOR);
        for (int tryNo = 0; tryNo < lusRepeat; tryNo++) {
            ServiceItem[] result = doGetServiceItems(template, minMatches, maxMatches, filter);
            if (result != null && result.length > 0)
                return result;
        }
        return new ServiceItem[0];
    }


	private ServiceItem[] doGetServiceItems(ServiceTemplate template, int minMatches, int maxMatches, ServiceItemFilter filter) {
        try {
            return sdManager.lookup(template, minMatches, maxMatches, filter, WAIT_FOR);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error while getting service", e);
            return null;
        }
    }

	static String formatServiceTemplate(ServiceTemplate template) {
		return String.format("[%s] [%s]",  getNames(template.attributeSetTemplates), getTypes(template.serviceTypes));
    }

    static String getNames(Entry[] entries) {
        StringBuilder sb = new StringBuilder();
        if(entries!=null) {
            for (Entry e : entries) {
                if (e instanceof Name) {
                    if (sb.length() > 0)
                        sb.append(", ");
                    sb.append(((Name) e).name);
                }
            }
        } else {
            sb.append("<null>");
        }
        return sb.toString();
    }

    static String getTypes(Class<?>[] classes) {
        StringBuilder sb = new StringBuilder();
        if(classes!=null) {
            for (Class<?> c : classes) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(c.getName());
            }
        } else {
            sb.append("<null>");
        }
        return sb.toString();
    }

}
