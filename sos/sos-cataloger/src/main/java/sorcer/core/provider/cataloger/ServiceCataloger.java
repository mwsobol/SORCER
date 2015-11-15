/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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
package sorcer.core.provider.cataloger;

import com.sun.jini.start.LifeCycle;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.id.ReferentUuid;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryListener;
import net.jini.lookup.ServiceDiscoveryManager;
import net.jini.lookup.entry.Name;
import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.MainUI;
import org.rioproject.admin.ServiceActivityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import sorcer.core.exertion.NetTask;
import sorcer.core.provider.Cataloger;
import sorcer.core.provider.Provider;
import sorcer.core.provider.ServiceProvider;
import sorcer.core.provider.cataloger.ServiceCataloger.CatalogerInfo.InterfaceList;
import sorcer.core.provider.cataloger.ui.CatalogerUI;
import sorcer.core.signature.NetSignature;
import sorcer.jini.lookup.entry.SorcerServiceInfo;
import sorcer.service.Context;
import sorcer.service.Servicer;
import sorcer.service.Task;
import sorcer.serviceui.UIDescriptorFactory;
import sorcer.serviceui.UIFrameFactory;
import sorcer.util.GenericUtil;
import sorcer.util.SOS;
import sorcer.util.Sorcer;
import sorcer.util.SorcerUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The facility for maintaining a cache of all SORCER providers {@link Servicer}s
 * as specified by <code>provider.template.match=sorcer.service.Service</code>
 * in the <code>sorcer.env</code> configuration file.
 * <p>
 * <ul>
 * <li>It uses <code>ServiceDiscoveryManager</code> with lookup cache.<br>
 * <li>It uses an internal map for storing services called {@link CatalogerInfo}
 * <li>The key of the map is an {@link InterfaceList}, the value is the list of
 * service proxies (<code>ServiceItem<code>s)
 * <li>{@link InterfaceList} is a list of interfaces with <code>equals</code>
 * overridden such that for <code>(interfaceList1.equals(interfaceList2)</code>
 * returns <code>true</code> if all elements contained in
 * <code>interfaceList2</code> are contained in <code>interfaceList1</code>.
 * <li><code>get</code> and <code>put</code> method of {@link CatalogerInfo} are
 * overridden to do nothing.
 * </ul>
 * <p>
 * Only access to {@link CatalogerInfo} is via a set of "service-aware" methods.
 * They include
 * <ol>
 * <li><code>addServiceItem(SeviceItem)</code>: adds an entry to this hash map
 * such that the key is the <code>InterfaceList</code> describing the service.
 * value is the serviceItem itself. Value is added always to the first to
 * improve load balancing heuristics (assuming that the latest served service is
 * always removed and added to the end
 * 
 * <li> <code>getServiceItem(String[] interfaces), String providerName))</code>:
 * not only returns the serviceItem with the following specs, but also removes
 * and adds the sericeItem to the end of the list to provide load-balancing
 * 
 * <li>synchronized <code>getServiceItem(ServiceID serviceID)</code> returns a
 * service with a serviceID, with the same load-balancing feature mentioned
 * above
 * 
 * <li> <code>getServiceMethods())</code> returns a hash map with the key as a
 * service interface (those interfaces package name starting with
 * <code>sorcer.</code>) and its value is a list of interface's method names.
 * 
 * <li> <code>getMethodContext(providerName, methodName))</code> returns the
 * template context with which the provider is registered. This template context
 * is pulled out of the service attribute (Entry): {@link SorcerServiceInfo}.
 * </ol>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ServiceCataloger extends ServiceProvider implements Cataloger {

	/** Logger for logging information about this instance */
	private static Logger logger = LoggerFactory.getLogger(ServiceCataloger.class);

	public boolean debug = false;
	
	public ServiceDiscoveryManager lookupMgr;

	public LookupCache cache;

	protected final CatalogerInfo cinfo = new CatalogerInfo();

	private String[] locators = null;

	public LookupLocator[] getLL() throws RemoteException {
		LookupLocator[] specificLocators = null;
		String sls = getProperty(P_LOCATORS);

		if (sls != null)
			locators = SorcerUtil.tokenize(sls, ",");
		try {
			if (locators != null && locators.length > 0) {
				specificLocators = new LookupLocator[locators.length];
				for (int i = 0; i < specificLocators.length; i++) {
					specificLocators[i] = new LookupLocator(locators[i]);
				}
			}
			return specificLocators;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;

	}

    public UIDescriptor getMainUIDescriptor() {
        UIDescriptor uiDesc = null;
        try {
            URL uiUrl = new URL(Sorcer.getWebsterUrl() + "/sos-cataloger-"+ SOS.getSorcerVersion()+"-ui.jar");
            uiDesc = UIDescriptorFactory.getUIDescriptor(MainUI.ROLE,
                                                         new UIFrameFactory(new URL[]{uiUrl},
                                                                            CatalogerUI.class.getName(),
                                                                            "Cataloger UI",
                                                                            null));
        } catch (Exception ex) {
			logger.warn("Could not create UI descriptor", ex);
		}
        return uiDesc;
    }

	public String[] getGroups() {
		String gs = getProperty(P_GROUPS);
		String[] groups = (gs == null) ? Sorcer.getLookupGroups() : SorcerUtil
				.tokenize(gs, ",");
		return groups;
	}

	public ServiceTemplate getTemplate() throws RemoteException {
		String templateMatch = Sorcer.getProperty(P_TEMPLATE_MATCH, ""
				+ Provider.class);
		logger.info(P_TEMPLATE_MATCH + ": " + templateMatch);
		ServiceTemplate template;
		try {
			template = new ServiceTemplate(null,
					new Class[] { Class.forName(templateMatch) }, null);
			return template;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ServiceCataloger() throws RemoteException {
		// do nothing
	}

	public ServiceCataloger(String[] args, LifeCycle lifeCycle)
			throws Exception {
		super(args, lifeCycle);
		init();
	}

	public void init() {
		try {
			LookupLocator[] specificLocators = null;
			String sls = getProperty(P_LOCATORS);

			if (sls != null)
				locators = SorcerUtil.tokenize(sls, " ,");
			if (locators != null && locators.length > 0) {
				specificLocators = new LookupLocator[locators.length];
				for (int i = 0; i < specificLocators.length; i++) {
					specificLocators[i] = new LookupLocator(locators[i]);
				}
			}

			String gs = getProperty(P_GROUPS);
			String[] groups = (gs == null) ? Sorcer.getLookupGroups()
					: SorcerUtil.tokenize(gs, ",");
			lookupMgr = new ServiceDiscoveryManager(new LookupDiscoveryManager(
					groups, specificLocators, null), null);

			String templateMatch = Sorcer.getProperty(P_TEMPLATE_MATCH,
					Provider.class.getName());
			ServiceTemplate template = new ServiceTemplate(null,
					new Class[] { Class.forName(templateMatch) }, null);

			cache = lookupMgr.createLookupCache(template, null,
					new CatalogerEventListener(cinfo));

			logger.info("-----------------------------");
			logger.info("Matching services that are: " + templateMatch);
			logger.info(P_GROUPS + ": " + Arrays.toString(groups));
			logger.info(P_LOCATORS + ": " + Arrays.toString(specificLocators));
			logger.info("------------------------------");
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
	}

//	public void setLogger(Logger logger) {
//		ServiceCataloger.logger = logger;
//	}

	/**
	 * Returns a Jini ServiceItem containing SORCER service provider based on
	 * two entries provided. The first entry is a provider's service type, the
	 * second provider's name. Expected that more entries will be needed to
	 * identify a provider in the future. See also lookup for a given ServiceID.
	 * 
	 * @see sorcer.core.provider.Cataloger#lookup(Class[])
	 */
	@Override
	public ServiceItem lookupItem(String providerName, Class... serviceTypes)
			throws RemoteException {
		return cinfo.getServiceItem(serviceTypes, providerName);
	}

	/**
	 * Returns a SORCER service provider identified by its primary service type.
	 * 
	 * @param serviceTypes interface of a SORCER provider
	 * @return a SORCER service provider
	 * @throws RemoteException
	 */
	@Override
	public Provider lookup(Class... serviceTypes) throws RemoteException {
		return lookup(null, serviceTypes);

	}

	/**
	 * * Returns a SORCER service provider identified by its primary service
	 * type and the provider's name/
	 * 
	 * @param providerName
	 *            - a provider name, a friendly provider's ID.
	 * @param serviceTypes
	 *            - interface of a SORCER provider
	 * @return a SORCER service provider
	 * @throws RemoteException
	 */
	@Override
	public Provider lookup(String providerName, Class... serviceTypes)
			throws RemoteException {
        // TODO RemoteLoggerAppender may call Cataloger to look for RemoteLogger which introduces recursion; make RLA not use Cataloger
        String mdcRemoteCall = MDC.get(MDC_SORCER_REMOTE_CALL);
        MDC.remove(MDC_SORCER_REMOTE_CALL);
        // another workaround: disable warning on ConnectionException in ProviderProxy
        //MDC.put("java.net.ConnectException.ignore", "TRUE");
        String pn = providerName;
		if (ANY.equals(providerName))
			pn = null;
		try {
			ServiceItem sItem = cinfo.getServiceItem(serviceTypes, pn);
			logger.info("lookup: sItem = " + sItem);

			if (sItem != null && (sItem.service instanceof Provider))
				return (Provider) sItem.service;
		} catch (Exception t) {
            logger.warn("Error while getting service item: " + t.getMessage());
        } finally {
            if (mdcRemoteCall != null)
                MDC.put(MDC_SORCER_REMOTE_CALL, mdcRemoteCall);
            //MDC.remove("java.net.ConnectException.ignore");
        }

		return null;
	}

	/**
	 * Returns a SORCER service provider identified by its service ID.
	 * 
	 * @param sid
	 *            - provider's ID
	 * @return a SORCER service provider
	 * @throws RemoteException
	 */
	@Override
	public Provider lookup(ServiceID sid) throws RemoteException {
		if (sid == null)
			return null;
		ServiceItem sItem = cinfo.getServiceItem(sid);
		return (sItem != null && sItem.service instanceof Provider) ? (Provider) sItem.service
				: null;
	}

	@Override
	public Map<String, String> getProviderMethods() throws RemoteException {
		if (cinfo == null)
			return new HashMap();
		else
			return cinfo.getProviderMethods();
	}

	@Override
	public Map<String, URL[]>  getProviderCodebaseURLs() throws RemoteException {
		if (cinfo == null)
			return new HashMap();
		else
			return cinfo.getProviderCodebaseURLs();
	}


	/**
	 * Returns the list of available providers in this catalog.
	 * 
	 * @return
	 * @throws RemoteException
	 */
	@Override
	public String[] getProviderList() throws RemoteException {
		List<ServiceItem> items = cinfo.getAllServiceItems();

		List<String> names = new ArrayList<String>();
		for (ServiceItem si : items) {
				Entry[] attributes = si.attributeSets;
				for (Entry a : attributes) {
					if (a instanceof Name) {
						names.add(((Name) a).name);
						break;
					}
				}
			}
		return names.toArray(new String[names.size()]);
	}

	@Override
	public String[] getInterfaceList(String providerName)
			throws RemoteException {
		if (cinfo == null) {
			return new String[0];
		}
		return cinfo.getInterfaceList(providerName);
	}

	@Override
	public String[] getMethodsList(String providerName, String interfaceName)
			throws RemoteException {
		if (cinfo == null) {
			return new String[0];
		}
		return cinfo.getMethodsList(providerName, interfaceName);
	}

	@Override
	public Context exertService(String providerName, Class serviceType,
			String methodName, Context theContext) throws RemoteException {
		return cinfo.exertService(providerName, serviceType, methodName,
				theContext);
	}

	@Override
	public String getServiceInfo() throws RemoteException {
		return cinfo.toString();
	}

	/**
	 * A customized &quot;sorcer provider&quot; aware a map of the cataloger
	 * info.
	 * <p>
	 * The key of the cataloger info is the <code>InterfaceList</code>. This
	 * inner <code>InterfaceList</code> is an array list of interface names with
	 * overridden <code>equals</code> such that it follows the following
	 * semantics: <code>arrayList1.equals(arrayList2</code> iff all interfaces
	 * of arrayList2 are contained in arrayList1.<br>
	 * The key value in <code>InterfaceList</code> is the list of service items
	 * implementing its key interfaces. Methods <code>get</code> and
	 * <code>put</code> are overridden to do nothing.
	 * <p>
	 * The method <code>addServiceItem(SeviceItem)</code> adds an entry to this
	 * map such that the key is the <code>InterfaceList</code> describing this
	 * service, the value is the serviceItem itself.
	 * <p>
	 * The method
	 * <code>getServiceItem(String[] interfaces, String providerName)</code> not
	 * only returns the serviceItem with the following specs, but also removes
	 * and adds the sericeItem to the end of the list to provide load-balancing
	 * <p>
	 * The method <code>getServiceItem(ServiceID serviceID)</code> returns a
	 * service with a given serviceID
	 * <p>
	 * The method <code>getProviderMethods</code> return a map of provider's
	 * <code>service methods</code>
	 */
	protected static class CatalogerInfo {
		Cataloger cataloger = null;
		final ConcurrentMap<InterfaceList, List<ServiceItem>> interfaceListMap = new ConcurrentHashMap<CatalogerInfo.InterfaceList, List<ServiceItem>>();

        public ConcurrentMap<InterfaceList, List<ServiceItem>> getInterfaceListMap() {
            return interfaceListMap;
        }

		private class CatalogObservable extends Observable {
			public void tellOfAction(String action) {
				logger.info("notifiying observers!");
				logger.info("num observers" + this.countObservers());
				setChanged();
				notifyObservers(action);
			}
		}

		private String[] interfaceIgnoreList;
		private CatalogObservable observable;

		public CatalogerInfo() {
			super();
			interfaceIgnoreList = new String[8];
			interfaceIgnoreList[0] = "sorcer.core.provider.Provider";
			interfaceIgnoreList[1] = "sorcer.core.provider.AdministratableProvider";
			interfaceIgnoreList[2] = "java.rmi.Remote";
			interfaceIgnoreList[3] = "net.jini.core.constraint.RemoteMethodControl";
			interfaceIgnoreList[4] = "net.jini.security.proxytrust.TrustEquivalence";
			interfaceIgnoreList[5] = "net.jini.id.ReferentUuid";
			interfaceIgnoreList[6] = "sorcer.service.RemoteTasker";
			interfaceIgnoreList[7] = "org.rioproject.admin.ServiceActivityProvider";
			observable = new CatalogObservable();
		}

		public void setCataloger(Cataloger cataloger) {
			this.cataloger = cataloger;
		}

		public void remove(CatalogerInfo.InterfaceList key) {
			interfaceListMap.remove(key);
		}

		public void remove(ServiceItem value) {
            InterfaceList found = null;
            ServiceItem itemFound = null;
            for (Map.Entry<InterfaceList, List<ServiceItem>> entry : interfaceListMap
                    .entrySet()) {
                for (ServiceItem item : entry.getValue()) {
                    if (item.serviceID.equals(value.serviceID)) {
                        found = entry.getKey();
                        itemFound = item;
                        break;
                    }
                }
            }
        }


		public void removeServiceItem(ServiceItem sItem) {
			InterfaceList searchInterfaceList = new InterfaceList(sItem.service
					.getClass().getInterfaces());
			List<ServiceItem> value;
            logger.info("Removing ServiceItem from Cataloger: " + sItem.toString());

			for (InterfaceList key : interfaceListMap.keySet()) {
                if (key.containsAllInterfaces(searchInterfaceList)) {
					value = interfaceListMap.get(key);
                    if (value != null) {
						if (value.size() == 1)
							remove(key);
						else
							removeFrom(value, sItem);
					}
				}
			}
			observable.tellOfAction("UPDATEDPLEASE");
		}

		public void addObserver(Observer observer) {
			// lookupMgr.setGUIBrowser(model);
			observable.addObserver(observer);
		}

		public List<ServiceItem> getAll(InterfaceList interfaceList) {
			List<ServiceItem> sItems = new ArrayList<ServiceItem>();
			for (Map.Entry<InterfaceList, List<ServiceItem>> entry : interfaceListMap.entrySet()) {
                if(logger.isDebugEnabled())
				logger.debug("list = " + entry.getValue());
				if (entry.getKey().containsAllInterfaces(interfaceList)) {
					logger.info("Cataloger found matching interface list: "+ entry.getValue());
					sItems.addAll(interfaceListMap.get(entry.getKey()));
				}
			}
			return sItems;
		}

		private boolean alreadyHas(ServiceItem item, List<ServiceItem> list) {
			boolean has = false;
			for(ServiceItem si : list) {
				if(si.serviceID.equals(item.serviceID)) {
					has = true;
					break;
				}
			}
			return has;
		}

		public void addServiceItem(ServiceItem sItem) {
			InterfaceList keyList = new InterfaceList(sItem.service.getClass().getInterfaces());
			List<ServiceItem> sItems = interfaceListMap.get(keyList);
			if(sItems!=null) {
				List<ServiceItem> workingList = new ArrayList<>();
				workingList.addAll(sItems);
				boolean added = false;
				for (ServiceItem si : sItems) {
					if(!alreadyHas(si, workingList)) {
						workingList.add(0, sItem);
						added = true;
					}
				}
				interfaceListMap.put(keyList, workingList);
				if(added) {
					StringBuilder sb = new StringBuilder();
					for (ServiceItem i : workingList) {
						if (sb.length() > 0)
							sb.append("\n");
						sb.append("\t")
						  .append(getName(i.attributeSets))
						  .append(" sid: ").append(i.serviceID)
						  .append(" rid: ").append(((ReferentUuid) i.service).getReferentUuid());
					}
					logger.info("ServiceItem list for {} was {} now {}\n{}",
								getName(sItem.attributeSets),
								sItems.size(),
								workingList.size(),
								sb.toString());
				} else {
					logger.info("ServiceItem list unchanged for {}",
								getName(sItem.attributeSets));
				}
			} else {
				sItems = new ArrayList<>();
				sItems.add(0, sItem);
				interfaceListMap.put(keyList, sItems);
				logger.info("ServiceItem list created for {}",
							getName(sItem.attributeSets));
			}
			logger.debug("Added new service, calling notify");
			observable.tellOfAction("UPDATEDPLEASE");
		}

		private String getName(Entry[] entries) {
			String name = null;
			for(Entry e : entries) {
				if(e instanceof Name) {
					name = ((Name)e).name;
					break;
				}
			}
			return name;
		}

        private void removeFrom(List<ServiceItem> sis, ServiceItem si) {
			if (si.service == null)
				return;
			for (int i = 0; i < sis.size(); i++)
				if (si.service.equals(sis.get(i).service)) {
					sis.remove(i);
					return;
				}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			List<ServiceItem> sItems;
			for(Map.Entry<InterfaceList, List<ServiceItem>> entry : interfaceListMap.entrySet()){
				sItems = entry.getValue();
				sb.append("\n");
				if (sItems != null && sItems.size() > 0
						&& sItems.get(0) != null) {
					if (sItems.get(0).attributeSets[0] instanceof Name)
						sb.append(((Name) (sItems.get(0).attributeSets[0])).name);
					else
						sb.append(sItems.get(0).attributeSets[0]);

					for (int i = 1; i < sItems.size(); i++) {
						if (sItems.get(i).attributeSets[0] instanceof Name)
							sb.append(",").append(((Name) (sItems.get(i).attributeSets[0])).name);
						else
							sb.append(",").append(sItems.get(i).attributeSets[0]);
					}
					sb.append("==>\n");
					sb.append(entry.getKey());
				}
			}
			return sb.toString();
		}

		/**
		 * The caller of this method must follow this generic protocol
		 * <p>
		 * first parameter = String[] of interfaces<br>
		 * second parameter = providerName if any
		 * <p>
		 * This method provides automatic load balancing by providing the
		 * serviceItem from the beginning and by removing it and adding to the
		 * end upon each request.
		 */
		public ServiceItem getServiceItem(Class[] interfaces,
				String providerName) {

			logger.info("providerName = " + providerName + "\ninterfaces: "
					+ GenericUtil.arrayToString(interfaces));

			List<ServiceItem> list = getAll(new InterfaceList(interfaces));
			logger.info("Cinfo getServiceItem, got: " + list);
			if (providerName != null && providerName.equals(ANY))
				providerName = null;
			if (list == null)
				return null;

			ServiceItem sItem;
			// provide load balancing and check if still alive
			if (providerName == null || providerName.length() == 0) {
				do {
					if (list.size() == 0)
						return null;
					sItem = list.remove(0);
					if (sItem != null) {
						if (isAlive(sItem)) {
							list.add(sItem);
							return sItem;
						} else {
							// not Alive anymore removing from cataloger
							removeServiceItem(sItem);
						}
					}
				} while (sItem != null);
			} else {
				net.jini.core.entry.Entry[] attrs;
				for (int i = 0; i < list.size(); i++) {
					attrs = list.get(i).attributeSets;
					for (net.jini.core.entry.Entry et : attrs) {
						if (et instanceof Name
								&& providerName.equals(((Name) et).name)) {
							sItem = list.remove(i);
                            if (sItem != null) {
                                if (isAlive(sItem)) {
                                    list.add(sItem);
                                    return sItem;
                                } else {
                                    // not Alive anymore removing from cataloger
                                    removeServiceItem(sItem);
                                }
                            }
						}
					}
				}
			}
			return null;
		}

		// there's no other better way of doing this because of the structure we
		// maintain.
		// we need to iterate through each and every one of the list and get the
		// service
		public ServiceItem getServiceItem(ServiceID serviceID) {
			Collection<List<ServiceItem>> c = interfaceListMap.values();
			if (c == null)
				return null;
			List<ServiceItem> sItems;
			ServiceItem sItem;
			for (Iterator<List<ServiceItem>> it = c.iterator(); it.hasNext();) {
				sItems = it.next();
				for (int i = 0; i < sItems.size(); i++) {
					if (serviceID.equals((sItems.get(i)).serviceID)) {
						sItem = sItems.remove(i);
						sItems.add(sItem);
						return sItem;
					}
				}
			}
			return null;
		}

		public List<ServiceItem> getAllServiceItems() {
			List<ServiceItem> items = new ArrayList<ServiceItem>();
			for (Map.Entry<InterfaceList, List<ServiceItem>> entry : interfaceListMap
					.entrySet()) {
				items.addAll(entry.getValue());
			}
			return items;
		}

		public ServiceItem[] getServiceItems(Class[] interfaces,
				String providerName, int maxItems) {
			// if maxItems is less or 0 then get all possible ServiceItems
			if (maxItems <= 0)
				maxItems = Integer.MAX_VALUE;
			if (providerName != null && providerName.equals(ANY))
				providerName = null;
			// logger.debug("Looking for interfaces: " + interfaces);
			List<ServiceItem> list = interfaceListMap.get(new InterfaceList(
					interfaces));
			// logger.debug("Got list: " + list.toString());
			if (list == null)
				return null;

			ServiceItem sItem;
			// provide load balancing
			if (providerName == null || "".equals(providerName)) {
				sItem = list.remove(0);
				list.add(sItem);
				ArrayList<ServiceItem> arItems = new ArrayList<ServiceItem>();
				Iterator<ServiceItem> it = list.iterator();
				while (it.hasNext() && arItems.size() < maxItems) {
					// Check if provider is still alive
					ServiceItem si = it.next();
					if (isAlive(si) && (!arItems.contains(si)))
						arItems.add(si);
				}
				ServiceItem[] sitems = new ServiceItem[arItems.size()];
				for (int i = 0; i < arItems.size(); i++) {
					sitems[i] = arItems.get(i);
				}
				return sitems;
			} else {
				net.jini.core.entry.Entry[] attrs;
				List<ServiceItem> slist = new ArrayList<ServiceItem>();
				Iterator<ServiceItem> it = list.iterator();
				ServiceItem si = null;
				while (it.hasNext() && slist.size() < maxItems) {
					si = it.next();
					attrs = si.attributeSets;
					if (attrs != null && attrs.length > 0
							&& (attrs[0] instanceof Name)
							&& providerName.equals(((Name) attrs[0]).name)) {
						if (isAlive(si) && (!slist.contains(si))) {
							slist.add(si);
						}
					}
				}
				ServiceItem[] sitems = new ServiceItem[slist.size()];
				for (int i = 0; i < slist.size(); i++) {
					sitems[i] = slist.get(i);
				}
				return sitems;
			}
		}


		public Map<String, String> getProviderMethods() throws RemoteException {
			logger.info("Inside GetProviderMethods");
			observable.tellOfAction("UPDATEDPLEASEPM");
			Map<String, String> map = new HashMap<String, String>();
			Collection<List<ServiceItem>> c = interfaceListMap.values();
			if (c == null) {
				return map;
			}
			Type[] clazz;
			Object service;
			String serviceName = null;
			net.jini.core.entry.Entry[] attributes;
			for (List<ServiceItem> sItems : c) {
				// get the first service proxy
				service = sItems.get(0).service;
				// get proxy interfaces
				clazz = service.getClass().getInterfaces();
				attributes = sItems.get(0).attributeSets;
				for (int i = 0; i < attributes.length; i++) {
					if (service instanceof Proxy) {
						if (attributes[i] instanceof Name) {
							serviceName = ((Name) attributes[i]).name;
							break;
						}
					} else
						serviceName = service.getClass().getName();
				}
				// list only interfaces of the Service type in package name
				if (service instanceof Servicer) {
					if (map.get(serviceName) == null) {
						map.put(serviceName, SorcerUtil.arrayToString(clazz)
								+ ";;" + SorcerUtil.arrayToString(attributes));
					}
				}
			}
			logger.info("getProviderMethods>>map:\n" + map);
			return map;
		}

		public Map<String, URL[]> getProviderCodebaseURLs() throws RemoteException {
			logger.info("Inside GetProviderMethods");
			observable.tellOfAction("UPDATEDPLEASEPM");
			Map<String, URL[]> map = new HashMap<String, URL[]>();
			Collection<List<ServiceItem>> c = interfaceListMap.values();
			if (c == null) {
				return map;
			}
			Type[] clazz;
			Object service;
			String serviceName = null;
			net.jini.core.entry.Entry[] attributes;
			for (List<ServiceItem> sItems : c) {
				// get the first service proxy
				service = sItems.get(0).service;
				// get proxy interfaces

				attributes = sItems.get(0).attributeSets;
				for (int i = 0; i < attributes.length; i++) {
					if (service instanceof Proxy) {
						if (attributes[i] instanceof Name) {
							serviceName = ((Name) attributes[i]).name;
							break;
						}
					} else
						serviceName = service.getClass().getName();
				}
				// list only interfaces of the Service type in package name
				if (service instanceof Servicer) {
					String annotation = RMIClassLoader.getClassAnnotation(service.getClass());
					if(annotation!=null && annotation.length()>0) {
						StringTokenizer tok = new StringTokenizer(annotation, " ");
						URL[] urls = new URL[tok.countTokens()];
						int i=0;
						while(tok.hasMoreTokens()) {
							String url = tok.nextToken();
							try {
								urls[i] = new URL(url);
							} catch (MalformedURLException e) {
								logger.warn(" Unable to create URL for ["+url+"]", e);
							}
							i++;
						}
						if (map.get(serviceName) == null) {
							map.put(serviceName, urls);
						}
					}
				}
			}
			logger.info("getProviderCodebaseURLs>>map:\n" + map);
			return map;
		}

		/**
		 * Returns the list of providers for a given provider name.
		 * 
		 * @return
		 * @throws RemoteException
		 */
		public String[] getInterfaceList(String providerName) {
			Collection<List<ServiceItem>> c = interfaceListMap.values();
			if (c == null) {
				return new String[0];
			}
			Type[] interfaceList;
			Object service;
			String serviceName = null;
			net.jini.core.entry.Entry[] attributes;
			Set set = interfaceListMap.keySet();
			InterfaceList key;
			for (Iterator<InterfaceList> it = set.iterator(); it.hasNext();) {
				key = it.next();
				List<ServiceItem> sItems = interfaceListMap.get(key);
				for (int k = 0; k < sItems.size(); k++) {
					service = sItems.get(k).service;
					// logger.info(".........  attributes: "+ service);
					attributes = sItems.get(k).attributeSets;
					for (int i = 0; i < attributes.length; i++) {
						if (service instanceof Proxy) {
							if (attributes[i] instanceof Name) {
								serviceName = ((Name) attributes[i]).name;
								break;
							}
						} else
							serviceName = service.getClass().getName();
					}
					if (serviceName.equals(providerName)) {

						interfaceList = service.getClass().getInterfaces();

						int count = 0;
						for (int i = 0; i < interfaceList.length; i++) {
							// logger.info("interface "+interfaceList[i].toString());
							String currentInterface = interfaceList[i]
									.toString().substring(10); // remove the
							// interface
							// part!
							boolean onList = false;
							for (int j = 0; j < interfaceIgnoreList.length; j++) {
								if (currentInterface
										.equals(interfaceIgnoreList[j])) {
									onList = true;
									break;
								}
							}
							if (!onList)
								count++;

						}
						String[] toReturn = new String[count];
						count = 0;
						for (int i = 0; i < interfaceList.length; i++) {
							// logger.info("interface "+interfaceList[i].toString());
							String currentInterface = interfaceList[i]
									.toString().substring(10); // remove the
							// interface part!
							boolean onList = false;
							for (int j = 0; j < interfaceIgnoreList.length; j++) {
								if (currentInterface
										.equals(interfaceIgnoreList[j])) {
									onList = true;
									break;
								}
							}
							if (!onList) {
								toReturn[count] = currentInterface;
								count++;
							}

						}
						return toReturn;
					}
				}
			}
			return new String[0];
		}

		/**
		 * Get provider list is a method to get a hashmap with the list of
		 * providers and their service id.
		 * 
		 * @return
		 * @throws RemoteException
		 */
		public String[] getMethodsList(String providerName, String interfaceName)
				throws RemoteException {
			logger.info("Inside Get Methods List");
			Collection<List<ServiceItem>> c = interfaceListMap.values();
			if (c == null) {
				return new String[0];
			}

			Class[] interfaceList;
			Object service;
			String serviceName = null;
			net.jini.core.entry.Entry[] attributes;
			logger.info("Provider Name " + providerName + " interface name "
					+ interfaceName);
			for (Map.Entry<InterfaceList, List<ServiceItem>> entry : interfaceListMap
					.entrySet()) {
				for (ServiceItem item : entry.getValue()) {
					service = item.service;
					attributes = item.attributeSets;
					for (int i = 0; i < attributes.length; i++) {
						if (service instanceof Proxy) {
							if (attributes[i] instanceof Name) {
								serviceName = ((Name) attributes[i]).name;
								break;
							}
						} else
							serviceName = service.getClass().getName();
					}
					if (serviceName.equals(providerName)) {
						interfaceList = service.getClass().getInterfaces();
						for (int i = 0; i < interfaceList.length; i++) {
							if (interfaceList[i].toString().equals(
									interfaceName)) {
								logger.info("Found interface" + interfaceName);
								Method methods[] = interfaceList[i]
										.getMethods();
								logger.info("Methods Found" + methods.length);
								String meths[] = new String[methods.length];
								for (int j = 0; j < methods.length; j++) {
									meths[j] = methods[j].getName();
								}
                                Set<String> setTemp = new HashSet(Arrays.asList(meths));
                                return setTemp.toArray(new String[setTemp.size()]);
							}

						}

					}
				}
			}
			return new String[0];
		}

		public Context exertService(String providerName, Class serviceType,
				String methodName, Context theContext)  {
			Collection<List<ServiceItem>> c = interfaceListMap.values();
			if (c == null) {
				return null;
			}
			List<ServiceItem> sItems;
			Object service;
			String serviceName = null;
			net.jini.core.entry.Entry[] attributes;
			InterfaceList key;
			for(Map.Entry<InterfaceList, List<ServiceItem>> entry : interfaceListMap.entrySet()) {
				for(ServiceItem item : entry.getValue()) {
					service = item.service;
					attributes = item.attributeSets;
					for (int i = 0; i < attributes.length; i++) {
						if (service instanceof Proxy) {
							if (attributes[i] instanceof Name) {
								serviceName = ((Name) attributes[i]).name;
								break;
							}
						} else
							serviceName = service.getClass().getName();
					}
					if (serviceName.equals(providerName)) {
						if (service instanceof sorcer.core.provider.Provider) {
							logger.info("service is a provider!");
							try {
								sorcer.core.provider.Provider temp = (sorcer.core.provider.Provider) service;
								NetSignature method = new NetSignature(
										methodName, serviceType);
								Task task = new NetTask(serviceType
										+ methodName, method);
								task.setContext(theContext);
								NetTask task2 = (NetTask) temp.service(task,
										null);
								return task2.getContext();
							} catch (Exception e) {
								logger.info("error converting to provider"
										+ e.getMessage());
							}
						}
					}
				}
			}
			return null;
		}

		private List<String> getSelectorList(Class serviceType) {
			Method[] methods = serviceType.getMethods();
			List<String> selectors = new ArrayList<String>(methods.length);
			for (int k = 0; k < methods.length; k++)
				selectors.add(methods[k].getName());
			return selectors;
		}

		/**
		 * Returns a list of selectors of <code>serviceType</code> along with
		 * hash tables per each interface. The hash table key is an interface
		 * name and the value is a list of all interface selectors.
		 * 
		 * @param serviceType
		 * @return list of selectors of serviceType and its superinterface hash
		 *         tables
		 */
		private List<String> getInterfaceList(Class serviceType) {
			Type type;
			Type[] clazz = serviceType.getGenericInterfaces();
			if (clazz.length == 0) {
				return getSelectorList(serviceType);
			}
			ArrayList ilist = new ArrayList();
			ilist.addAll(getSelectorList(serviceType));
			Map<String, List<String>> imap;
			for (int j = 0; j < clazz.length; j++) {
				type = clazz[j];
				imap = new HashMap();
				imap.put(((Class) type).getName(),
						getInterfaceList((Class) type));
				ilist.add(imap);
			}
			return ilist;
		}

		/**
		 * Returns a list of maps per each interface in the array clazz
		 * such that they are registered by the service provider as sorcerTypes.
		 * The hash table key is an interface name and the value is a list of
		 * all interface selectors.
		 * 
		 * @param clazz
		 *            the list of all types
		 * @param sorcerTypes
		 *            the registered types by a service provider
		 * @return the list of superinterface hash tables
		 */
		private List<Map<String, List<String>>> getInterfaceList(Type[] clazz, List<String> sorcerTypes) {
			Type serviceType;
			ArrayList ilist = new ArrayList();
			Map<String, List<String>> imap;
			for (int j = 0; j < clazz.length; j++) {
				serviceType = clazz[j];
				if (sorcerTypes != null
						&& sorcerTypes
								.contains(((Class)serviceType).getName())) {
					imap = new HashMap<String, List<String>>();
					imap.put(((Class)serviceType).getName(),
							getSelectorList((Class)serviceType));
					ilist.add(imap);
				}
			}
			return ilist;
		}

		/**
		 * See above CatalogerInfo for comments.
		 */
		public static class InterfaceList extends ArrayList<Class> {
			private static final long serialVersionUID = 1L;

			public InterfaceList(Class[] clazz) {
				if (clazz != null && clazz.length > 0)
					for (int i = 0; i < clazz.length; i++)
						add(clazz[i]);
			}

			public boolean containsAllInterfaces(InterfaceList interfaceList) {
				Set<Class> all = new HashSet<Class>(this);
				for (int i = 0; i < size(); i++) {
					all.addAll(Arrays.asList(get(i).getInterfaces()));
				}
				return getServiceList(all).containsAll(
						getServiceList(interfaceList));
			}

			private List<String> getServiceList(
					Collection<Class> interfaceCollection) {
				List<String> serviceList = new ArrayList<String>();
				for (Class service : interfaceCollection) {
					serviceList.add("" + service);
				}
				return serviceList;
			}
			
			public boolean equals(InterfaceList otherList) {
				return this.containsAllInterfaces(otherList)
						&& otherList.containsAllInterfaces(this);
			}
		}// end of InterfaceList
	}// end of CatalogerInfo

	// As these are not remote listeners, and the CatalogerInfo is thread safe,
	// it's not important to spawn a new thread for each change in the service.
	protected static class CatalogerEventListener implements
			ServiceDiscoveryListener, Runnable {
		String msg;
		final CatalogerInfo cinfo;

		public CatalogerEventListener(CatalogerInfo cinfo) {
			this.cinfo = cinfo;
		}

		public CatalogerEventListener(CatalogerInfo cinfo, String msg) {
			this(cinfo);
			this.msg = msg;
		}

		public void serviceAdded(ServiceDiscoveryEvent ev) {
			cinfo.addServiceItem(ev.getPostEventServiceItem());
			refreshScreen("++++ SERVICE ADDED ++++");
		}

		public void serviceRemoved(ServiceDiscoveryEvent ev) {
			refreshScreen("++++ SERVICE REMOVING ++++");
			cinfo.removeServiceItem(ev.getPreEventServiceItem());
			refreshScreen("++++ SERVICE REMOVED ++++");
		}

		public void serviceChanged(ServiceDiscoveryEvent ev) {
			ServiceItem pre = ev.getPreEventServiceItem();
			ServiceItem post = ev.getPostEventServiceItem();

			// This should not happen
			if (pre == null && post == null) {
				logger.info(">>serviceChanged::Null serviceItem! ? ");
				return;
			} else if (pre.service == null && post.service != null) {
				logger.info(">>serviceChanged::The proxy's service is now not null \n");
				logger.info(">>serviceChanged::Proxy later: post.service ("
						+ post.service.getClass().getName() + ")\n");
			} else if (pre.service != null && post.service == null) {
				logger.info(">>serviceChanged::The service's proxy has become null::check codebase problem");
				logger.info(">>serviceChanged::Proxy later: pre.service ("
						+ pre.service.getClass().getName() + ")\n");
				cinfo.remove(post);
			} else {
				logger.debug("Service attribute has changed pre=" + pre
						+ " post=" + post);
				cinfo.remove(pre);
				cinfo.addServiceItem(post);
			}
			refreshScreen("++++ SERVICE CHANGED ++++");
		}

		private void refreshScreen(String msg) {
			new Thread(new CatalogerEventListener(cinfo, msg)).start();
		}

		public void run() {
            if (cinfo != null && logger.isDebugEnabled()) {
				StringBuilder buffer = new StringBuilder(msg).append("\n");
				buffer.append(cinfo).append("\n");
				logger.debug(buffer.toString());
			}
		}
	}


    /**
     * Tests if provider is still alive.
     *
     * @param si service to check
     * @return true if a provider is alive, otherwise false
     */
    private static boolean isAlive(ServiceItem si) {
        if (si == null)
            return false;
        try {
            if (si.service instanceof ServiceActivityProvider) {
                boolean result = ((ServiceActivityProvider)si.service).isActive();
            } else if (si.service instanceof Provider) {
                String name = ((Provider) si.service).getProviderName();
            }
            return true;
        } catch (Exception e) {
            logger.warn("Service ID: " + si.serviceID
					+ " is not Alive anymore");
            // throw e;
            return false;
        }
	}

	/**
	 * Returns at most maxMatches items matching the template, plus the total
	 * number of items that match the template. The return value is never null,
	 * and the returned items array is only null if maxMatches is zero. For each
	 * returned item, if the service object cannot be deserialized, the service
	 * field of the item is set to null and no exception is thrown. Similarly,
	 * if an attribute set cannot be deserialized, that element of the
	 * attributeSets array is set to null and no exception is thrown.
	 * 
	 * @param tmpl
	 *            - the template to match
	 * @param maxMatches
	 * @return a ServiceMatches instance that contains at most maxMatches items
	 *         matching the template, plus the total number of items that match
	 *         the template. The return value is never null, and the returned
	 *         items array is only null if maxMatches is zero.
	 * @throws RemoteException
	 */
	public ServiceMatches lookup(ServiceTemplate tmpl, int maxMatches)
			throws RemoteException {
        List<ServiceItem> result = new LinkedList<ServiceItem>();
        if(cinfo==null){
            logger.warn("Cataloger not initialized");
        } else
        for (Map.Entry<InterfaceList, List<ServiceItem>> entry : cinfo.getInterfaceListMap().entrySet()) {
            List<ServiceItem> serviceItems;
            List<ServiceItem> down = new LinkedList<ServiceItem>();
            synchronized (entry) {
                serviceItems = new LinkedList<ServiceItem>(entry.getValue());
            }
            SRVITEM:

            for (ServiceItem serviceItem : serviceItems) {
                if (tmpl.serviceID != null && tmpl.serviceID.equals(serviceItem.serviceID)) {
                    result.add(serviceItem);
                    //serviceID is unique, stop on first matching service
                    break;
                }

                CatalogerInfo.InterfaceList interfaceList = entry.getKey();
                if (interfaceList.containsAll(Arrays.asList(tmpl.serviceTypes))) {
                    List<Entry> sItemEntryList = Arrays.asList(serviceItem.attributeSets);
                    for (Entry attr : tmpl.attributeSetTemplates) {
                        if (!sItemEntryList.contains(attr)) {
                            continue SRVITEM;
                        }
                    }
                    if (isAlive(serviceItem)) {
                        logger.info("Service " + serviceItem.serviceID + " is adding to results for: " + tmpl.toString());
                        result.add(serviceItem);
                    } else {
                        // not Alive anymore removing from cataloger
                        down.add(serviceItem);
                    }
                    if (result.size() >= maxMatches) break;
                }
            }
            synchronized (cinfo){
                for(ServiceItem serviceItem: down)
                        cinfo.removeServiceItem(serviceItem);
            }

        }
        return new ServiceMatches(result.toArray(new ServiceItem[result.size()]), result.size());
    }
	public String returnString() throws RemoteException {
		return getClass().getName() + ":" + getProviderName();
	}

}
