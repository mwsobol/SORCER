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
import java.util.logging.Logger;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.event.EventMailbox;
import net.jini.lookup.entry.Name;
import net.jini.space.JavaSpace;
import net.jini.space.JavaSpace05;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.Caller;
import sorcer.core.provider.Cataloger;
import sorcer.core.provider.Exerter;
import sorcer.core.provider.FileStorer;
import sorcer.core.provider.Jobber;
import sorcer.core.provider.Provider;
import sorcer.core.provider.Spacer;
import sorcer.service.Accessor;
import sorcer.service.DynamicAccessor;
import sorcer.service.Service;
import sorcer.service.Signature;
import sorcer.service.SignatureException;

/**
 * A utility class that provides access to SORCER services and some
 * infrastructure services. It extends the <code>ServiceAccessor</code>
 * functionality.
 * 
 * The <code>getService</code> methods directly use ServiceAccessor calls while
 * the <code>getProvider</code> methods use a SORCER Cataloger's cached services
 * with round robin load balancing.
 * 
 * The individual SORCER services should be accessed using this utility since it
 * uses local cached proxies for frequently used SORCER infrastructure services,
 * for example: Cataloger, JavaSpace, Jobber. ProviderAccessor normally uses
 * Cataloger if available, otherwise it uses Jini lookup services as implemented
 * by <code>ServiceAccessor</code>.
 * 
 * @see ServiceAccessor
 */
@SuppressWarnings("rawtypes")
public class ProviderAccessor extends ServiceAccessor implements
		DynamicAccessor, SorcerConstants {

	static Logger logger = Logger.getLogger(ProviderAccessor.class.getName());

	private static ProviderAccessor accessor;

	/**
	 * Used for local caching to speed up getting frequently needed service
	 * providers. Calls to discover JavaSpace takes a lot of time.
	 */
	protected static Cataloger cataloger;

	// to verify by name without call in the provider
	protected static String catalogerName;

	protected static Exerter exerter;
	
	protected static Jobber jobber;

	protected static Spacer spacer;

	protected static FileStorer fileStorer;

	protected static Caller caller;

	protected static TransactionManager transactionMgr;

	protected static JavaSpace05 javaSpace;

	protected ProviderAccessor() {
		// Nothing to do, uses the singleton design pattern
	}

	protected ProviderAccessor(String[] lookupGroups) {
		ProviderAccessor.lookupGroups = lookupGroups;
	}

	/**
	 * Initializes the cache of frequently used SORCER services. That includes a
	 * Cataloger, Jobber, and Spacer used by this ProviderAccessor.
	 * 
	 * @throws AccessorException
	 */
	public static void init() throws AccessorException {
		// initialize a generic servicer accessing facility
		if (accessor == null) accessor = new ProviderAccessor();
		lookupGroups = Sorcer.getLookupGroups();		
		Accessor.setAccessor(accessor);
		try {
			openDiscoveryManagement(lookupGroups);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static ProviderAccessor getAccessor(String[] lookupGroups) {
		if (accessor != null) {
			return accessor;
		} else {
			return getAccessor(lookupGroups);
		}
					
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
	public static Object getService(ServiceID serviceID, Class[] serviceTypes,
			Entry[] attrSets, String[] groups) {
		ServiceTemplate st = new ServiceTemplate(serviceID, serviceTypes,
				attrSets);
		return getService(st, null, groups);
	}

	/**
	 * Returns a SORCER service provider registered with serviceID.
	 * 
	 * @param serviceID
	 *            a service provider ID
	 * @return a SORCER provider service
	 */
	public static Object getService(ServiceID serviceID) {
		return getService(serviceID, null, null, getLookupGroups());
	}

	/**
	 * Returns a SORCER service provider registered with the most significant
	 * and the least significant bits.
	 * 
	 * @param mostSig
	 *            most significant bits
	 * @param leastSig
	 *            least significant bits
	 * @return a SORCER provider service
	 */
	public static Object getService(long mostSig, long leastSig) {
		ServiceID serviceID = new ServiceID(mostSig, leastSig);
		return getService(serviceID, null, null, getLookupGroups());
	}

	/**
	 * Returns a SORCER service provider with the specified name and service
	 * type, using a Cataloger if available, otherwise using Jini lookup
	 * services.
	 * 
	 * @param providerName
	 *            the name of service provider
	 * @param serviceType
	 *            a provider service type (interface)
	 * @return a SORCER provider service
	 */
	public final static Provider getProvider(String providerName, Class serviceType) {
//		logger.info("providerName = " + providerName
//				+ "\nserviceInfo = " + serviceInfo);
		Provider servicer = null;
		if (providerName != null && providerName.equals(ANY))
			providerName = null;
		try {
			//servicer = (Service)ProviderLookup.getService(providerName, serviceInfo);
			cataloger = getCataloger();
			if (cataloger != null) {
				int tryNo = 0;
//				logger.info("Total timeout for service discovery is: " + LUS_REAPEAT);
				while (tryNo < LUS_REAPEAT) {
					servicer = cataloger.lookup(providerName, serviceType);
					//servicer = (Service)cataloger.lookupItem(providerName, serviceInfo).service;
					if (servicer != null)
						break;
						
					Thread.sleep(200);
					tryNo++;
				}
			}
			// fall back on Jini LUS
			if (servicer == null) {
				servicer = (Provider) getService(providerName, serviceType);
			}
		} catch (Throwable ex) {
			logger.throwing(ProviderAccessor.class.getName(), "getProvider", ex);
			ex.printStackTrace();
		}
		logger.info("servicer = " + servicer);
		return servicer;
	}


	/**
	 * Returns a SORCER service provider with the specified signature, using a
	 * Cataloger if available, otherwise using Jini lookup services.
	 * 
	 * @param signature
	 *            the signature of service provider
	 * @return a SORCER provider service
	 */
	public static Provider getProvider(Signature signature) {
		return ProviderAccessor.getProvider(signature.getProviderName(),
				signature.getServiceType());
	}

	/**
	 * Returns a SORCER Provider with the specified name, service type, and a
	 * codebase where the interface class can be downloaded from.
	 * 
	 * @param providerName
	 *            The name of the provider to search for
	 * @param serviceType
	 *            The interface to look for
	 * @param codebase
	 *            The location where to download the class from
	 * @return a SORCER Provider
	 */
	public final static Provider getProvider(String providerName,
			Class serviceType, String codebase) {
		return (Provider) getService(providerName, serviceType, codebase);
	}

	/**
	 * Returns a SORCER service provider with the specified service ID using a
	 * Cataloger if available, otherwise using Jini lookup services.
	 * 
	 * @param serviceID
	 *            serviceID of the desired service
	 * @return a SORCER provider service
	 */
	public final static Provider getProvider(ServiceID serviceID) {
		try {
			cataloger = getCataloger();
			if (cataloger != null)
				return cataloger.lookup(serviceID);
			else
				return (Provider) getService(serviceID);
		} catch (Exception ex) {
			logger.throwing(ProviderAccessor.class.getName(), "getProvider", ex);
			return null;
		}
	}

	/**
	 * Returns a SORCER service provider with the specified service type, using
	 * a Cataloger if available, otherwise using Jini lookup services.
	 * 
	 * @param serviceType
	 *            a provider service type (interface)
	 * @return a SORCER provider service
	 */
	public final static Provider getProvider(Class serviceType) {
		return getProvider(null, serviceType);
	}

	/**
	 * Returns a SORCER service provider matching a given attributes.
	 * 
	 * @param attributes
	 *            attribute set to match
	 * @return a SORCER provider
	 */
	public static Provider getProvider(Entry[] attributes) {
		return (Provider) getService(null, null, attributes, getLookupGroups());
	}

	/**
	 * Returns a SORCER service provider matching a given list of implemented
	 * service types (interfaces).
	 * 
	 * @param serviceTypes
	 *            a set of service types to match
	 * @return a SORCER provider
	 */
	public static Provider getProvider(Class[] serviceTypes) {
		return (Provider) getService(null, serviceTypes, null, getLookupGroups());
	}

	/**
	 * Returns a JINI ServiceItem containing the SORCER service provider with
	 * the specified providerName and serviceInfo using Cataloger if available,
	 * otherwise using Jini lookup services.
	 * 
	 * @param providerName
	 *            , serviceInfo serviceID of the desired service
	 * @return JINI ServiceItem
	 */

	public final static ServiceItem getCatalogServiceItem(String providerName, Class serviceType) {
		try {
			cataloger = getCataloger();
			if (cataloger != null)
				return cataloger.lookupItem(providerName, serviceType);
			else
				return getServiceItem(providerName, serviceType);
		} catch (Exception ex) {
			logger.throwing(ProviderAccessor.class.getName(), "getProvider", ex);
			return null;
		}
	}

	/**
	 * Returns any SORCER Jobber provider.
	 * 
	 * @return a SORCER Jobber provider
	 * @throws AccessorException
	 */
	public static Jobber getJobber() throws AccessorException {
		return getJobber(null);
	}

	/**
	 * Returns any SORCER Exerter provider.
	 * 
	 * @return a SORCER Exerter provider
	 * @throws AccessorException
	 */
	public static Exerter getExerter() throws AccessorException {
		return getExerter(null);
	}

	
	/**
	 * Returns any SORCER Spacer provider.
	 * 
	 * @return a SORCER Spacer provider
	 * @throws AccessorException
	 */
	public static Spacer getSpacer() throws AccessorException {
		return getSpacer(null);
	}

	/**
	 * Returns a SORCER Jobber provider using Jini lookup and discovery.
	 * 
	 * @param name
	 *            the name of a Jobber provider
	 * @return a Jobber proxy
	 */
	public final static Jobber getJobber(String name) {
		String jobberName = (name == null) ? Sorcer.getProperty(S_JOBBER_NAME)
				: name;
		try {
			if (isAlive((Provider) jobber)) {
				logger.info(">>>returned cached Jobber ("
						+ ((Provider) jobber).getProviderID() + ") by "
						+ ProviderAccessor.class.getName());
			} else {
				jobber = (Jobber) getProvider(jobberName, Jobber.class);
			}
			return jobber;
		} catch (Exception e) {
			logger.throwing(ProviderAccessor.class.getName(), "getJobber", e);
			return null;
		}
	}

	/**
	 * Returns a SORCER Exerter  provider using Jini lookup and discovery.
	 * 
	 * @param name
	 *            the name of a Exerter service provider
	 * @return a Exerter proxy
	 */
	public final static Exerter getExerter(String name) {
		String exerterName = (name == null) ? Sorcer.getProperty(S_EXERTER_NAME)
				: name;
		try {
			if (isAlive((Provider) exerter)) {
				logger.info(">>>returned cached Neter ("
						+ ((Provider) exerter).getProviderID() + ") by "
						+ ProviderAccessor.class.getName());
			} else {
				exerter = (Exerter) getProvider(exerterName, Exerter.class);
			}
			return exerter;
		} catch (Exception e) {
			logger.throwing(ProviderAccessor.class.getName(), "getExerter", e);
			return null;
		}
	}
	
	/**
	 * Returns a SORCER Spacer service provider using Jini lookup and discovery.
	 * 
	 * @param name
	 *            the name of a spacer service provider
	 * @return a Spacer proxy
	 */
	public final static Spacer getSpacer(String name) {
//		String spacerName = (name == null) ? Sorcer.getProperty(S_SPACER_NAME)
//				: name;
		try {
			if (isAlive((Provider) spacer)) {
				logger.info(">>>returned cached Spacer ("
						+ ((Provider) spacer).getProviderID() + ") by "
						+ ProviderAccessor.class.getName());
			} else {
				spacer = (Spacer) getProvider(name, Spacer.class);
			}
			return spacer;
		} catch (Exception e) {
			logger.throwing(ProviderAccessor.class.getName(), "getSpacer", e);
			return null;
		}
	}

	public static Provider getNotifierProvider() throws ClassNotFoundException {
		Class[] svcTypes = new Class[] { Class
				.forName("sorcer.service.SorcerNotifierProtocol") };
		return (Provider) getService(null, svcTypes, null, getLookupGroups());
	}

	public static EventMailbox getEventMailbox() throws ClassNotFoundException {
		Class[] svcTypes = new Class[] { Class
				.forName("net.jini.event.EventMailbox") };
		return (EventMailbox) getService(null, svcTypes, null, getLookupGroups());
	}

	/**
	 * Returns a database file store service provider.
	 * 
	 * @return - a SORCER FileStorer service provider
	 */
	public final static FileStorer getFileStorer() {
		try {
			if (!ProviderAccessor.isAlive((Provider) fileStorer))
				fileStorer = (FileStorer) getService(null,
						new Class[] { Class
								.forName("sorcer.service.FileStorer") }, null,
						getLookupGroups());
			return (FileStorer) fileStorer;
		} catch (Exception e) {
			// Just Report and exit
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns a Jobber server using Jini discovery.
	 * 
	 * @return a SORCER Caller service provider
	 */
	public final static Caller getCaller() {
		try {
			if (!ProviderAccessor.isAlive((Provider) caller))
				caller = (Caller) getService(null,
						new Class[] { Class.forName("sorcer.service.Caller") },
						null, getLookupGroups());

			return (Caller) caller;
		} catch (Exception e) {
			// Just Report and exit
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns a Jini transaction manager service.
	 * 
	 * @return Jini transaction manager
	 */
	public static TransactionManager getTransactionManager() {
		try {
			if (transactionMgr == null)
				return getNewTransactionManger();
			transactionMgr.getState(-1);
			return (TransactionManager) transactionMgr;
		} catch (net.jini.core.transaction.UnknownTransactionException ute) {
			return (TransactionManager) transactionMgr;
		} catch (Exception e) {
			try {
				transactionMgr = getNewTransactionManger();
				return transactionMgr;
			} catch (Exception ex) {
				logger.throwing(ProviderAccessor.class.getName(),
						"getTransactionManager", ex);
				return null;
			}
		}
	}

	private static TransactionManager getNewTransactionManger() {
		transactionMgr = (TransactionManager) getService(null,
				new Class[] { TransactionManager.class }, null, getLookupGroups());

		return (TransactionManager) transactionMgr;
	}

	/**
	 * Returns a JavaSpace service with a given name.
	 * 
	 * @return JavaSpace proxy
	 */
	public static JavaSpace05 getSpace(String spaceName) {
		return getSpace(spaceName, Sorcer.getSpaceGroup());
	}

	/**
	 * Returns a JavaSpace service with a given name and group.
	 * 
	 * @return JavaSpace proxy
	 */
	public static JavaSpace05 getSpace(String spaceName, String spaceGroup) {
		// first test if our cached JavaSpace is alive
		// and if it's the case then return it,
		// otherwise get a new JavSpace proxy
		Entry[] attrs = null;
		if (spaceName != null) {
			attrs = new Entry[] { new Name(spaceName) };
		}
		String sg = spaceGroup;
		if (spaceGroup == null) {
			sg = getSpaceGroup();
		}
		try {
			if (javaSpace == null) {
//				logger.info("getting Exertion Space name: " 
//						+ (attrs == null ? null : attrs[0]) + " group: " + sg);
				javaSpace = (JavaSpace05) getService(null,
						new Class[] { JavaSpace05.class }, attrs,
						new String[] { sg });
			} else {
				javaSpace.readIfExists(new Name("_SORCER_"), null,
						JavaSpace.NO_WAIT);
			}
		} catch (Exception e) {
			//e.printStackTrace();
			javaSpace = (JavaSpace05) getService(null,
					new Class[] { JavaSpace05.class }, attrs,
					new String[] { sg });
		}
		return javaSpace;
	}

	/**
	 * Returns a Jini JavaSpace service.
	 * 
	 * @return Jini JavaSpace
	 */
	public static JavaSpace05 getSpace() {
//		return getSpace(Sorcer.getActualSpaceName());
		return getSpace(null, Sorcer.getSpaceGroup());
	}

	/**
	 * Returns a SORCER Cataloger Service.
	 * 
	 * This method searches for either a JINI or a RMI Cataloger service.
	 * 
	 * @return a Cataloger service proxy
	 * @throws AccessorException
	 * @see Cataloger
	 */
	public static Cataloger getCataloger() throws AccessorException {
		return getCataloger(Sorcer.getActualCatalogerName()) ;
	}

	/**
	 * Returns a SORCER Cataloger service provider using JINI discovery.
	 * 
	 * @return a SORCER Cataloger
	 */
	public final static Cataloger getCataloger(String serviceName) {
		boolean catIsOk = false;
		try {
			catIsOk = isAlive((Provider) cataloger);
		} catch (Exception re) {
			return cataloger = (Cataloger) getService(serviceName,
					Cataloger.class);
		}
		try {
			if (catIsOk) {
				return cataloger;
			} else
				return cataloger = (Cataloger) getService(serviceName,
						Cataloger.class);
		} catch (Exception e) {
			logger.throwing(ProviderAccessor.class.getName(), "getProvider", e);
			return null;
		}
	}


	/**
	 * Returns a SORCER service using a cached Cataloger instance by this
	 * ProviderAccessor.
	 * 
	 * @param primaryInterface
	 *            - service type of requested provider
	 * @return a requested service or null if a Catloger is not available
	 * @throws RemoteException
	 * @throws AccessorException
	 */
	public static Provider lookup(Class primaryInterface)
			throws RemoteException, AccessorException {
		return lookup(null, primaryInterface);
	}

	/**
	 * Returns a SORCER service using a cached Cataloger instance by this
	 * ProviderAccessor. However if it not possible uses a ServiceAccessor to
	 * get a requested service form Jini lookup services directly. This approach
	 * allows for SORCER requestors and providers to avoid continuous usage of
	 * lookup discovery for each needed service that is delegated to a SORCER
	 * Cataloger service.
	 * 
	 * @param providerName
	 *            - a name of requested service
	 * @param primaryInterface
	 *            - service type of requested provider
	 * @return a requested service or null if a Cataloger is not available
	 * @throws AccessorException
	 */
	public static Provider lookup(String providerName, Class primaryInterface)
			throws AccessorException {
		try {
			// check if the cataloger is alive then return a reqested service
			// provider
			if (ProviderAccessor.isAlive((Provider) cataloger))
				return cataloger.lookup(providerName, primaryInterface);
			else {
				// try to get a new cataloger and lookup again
				cataloger = (Cataloger) getService(
						Sorcer.getProperty(S_CATALOGER_NAME), Cataloger.class);
				if (cataloger != null) {
					logger.info("Got service provider from Cataloger");
					return cataloger.lookup(providerName, primaryInterface);
				} else {
					// just get a provider without a Cataloger, use directly
					// LUSs
					logger.severe("No SORCER cataloger available");
					return (Provider) getService(providerName, primaryInterface);
				}
			}
		} catch (RemoteException ex) {
			logger.throwing(ProviderAccessor.class.getName(), "lookup", ex);
			return null;
		}
	}

	/**
	 * Test if provider is still replying.
	 * 
	 * @param provider
	 * @return true if a provider is alive, otherwise false
	 * @throws RemoteException
	 */
	protected final static boolean isAlive(Provider provider)
			throws RemoteException {
		if (provider == null)
			return false;
		try {
			provider.getProviderName();
			return true;
		} catch (RemoteException e) {
			logger.throwing(ProviderAccessor.class.getName(), "isAlive", e);
			throw e;
		}
	}

	/**
	 * Used by the {@link Accessor} facility.
	 * 
	 * @throws SignatureException
	 * 
	 * @see sorcer.service.DynamicAccessor#getService(sorcer.service.Signature)
	 */
	public Service getService(Signature signature) {
		return getProvider(signature.getProviderName(),
				signature.getServiceType());
	}

	/**
	 * Used by the {@link Accessor} facility.
	 * 
	 * @throws QosResourceException
	 * 
	 * @see sorcer.service.DynamicAccessor#getServiceItem(sorcer.service.Signature)
	 */
	public ServiceItem getServiceItem(Signature signature) {
		logger.info("Using ProviderAccessor.getService");
		return getServiceItem(signature.getProviderName(),
				signature.getServiceType());
	}

}
