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
package sorcer.core.provider;

import com.sun.jini.config.Config;
import groovy.lang.GroovyShell;
import net.jini.admin.Administrable;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.entry.Entry;
import net.jini.core.event.RemoteEvent;
import net.jini.core.lease.Lease;
import net.jini.core.lookup.ServiceID;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.export.Exporter;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.entry.Location;
import net.jini.lookup.entry.Name;
import net.jini.security.AccessPermission;
import net.jini.security.TrustVerifier;
import net.jini.space.JavaSpace05;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.container.jeri.AbstractExporterFactory;
import sorcer.container.jeri.ExporterFactories;
import sorcer.core.SorcerConstants;
import sorcer.core.SorcerNotifierProtocol;
import sorcer.core.context.Contexts;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.exertion.ExertionEnvelop;
import sorcer.core.exertion.NetTask;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.core.misc.MsgRef;
import sorcer.core.monitor.MonitoringSession;
import sorcer.core.provider.ServiceProvider.ProxyVerifier;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.core.proxy.Partnership;
import sorcer.core.proxy.ProviderProxy;
import sorcer.core.service.Configurer;
import sorcer.core.signature.NetSignature;
import sorcer.jini.jeri.SorcerILFactory;
import sorcer.jini.lookup.entry.SorcerServiceInfo;
import sorcer.jini.lookup.entry.VersionInfo;
import sorcer.platform.logger.RemoteLoggerInstaller;
import sorcer.security.sign.SignedServiceTask;
import sorcer.security.sign.SignedTaskInterface;
import sorcer.security.sign.TaskAuditor;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.*;
import sorcer.service.jobber.JobberAccessor;
import sorcer.service.space.SpaceAccessor;
import sorcer.service.txmgr.TransactionManagerAccessor;
import sorcer.util.*;

import javax.security.auth.Subject;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static sorcer.core.SorcerConstants.*;
import static sorcer.util.StringUtils.tName;

/**
 * There are two types of SORCER service servers: generic service servers -
 * subclasses of {@link ServiceProvider} - and service beans - classes of
 * implementing one or more SORCER service types. This class does the
 * actual work for both generic SORCER servers and SORCER service beans. Also it
 * provides the basic functionality for {@link Provider}s. Multiple SORCER
 * beans can be deployed within a single (@link ServiceProvider}.
 * 
 * @see sorcer.core.provider.ServiceProvider
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ProviderDelegate {

	static ThreadGroup threadGroup = new ThreadGroup(PROVIDER_THREAD_GROUP);

	static final int TRY_NUMBER = 5;

	// service class loader
	private ClassLoader implClassLoader;

	private ArrayList<SpaceTaker> spaceTakerThreads = new ArrayList<SpaceTaker>();

	// visited exertion for forwardExertion to check for potential looping
	private static Set visited;

	private static final Logger logger = LoggerFactory.getLogger(ProviderDelegate.class);

	private Logger remoteLogger;

	/** Provider logger used in custom provider methods */
	private Logger providerLogger;

	/** Context logger used in custom provider methods */
	private Logger contextLogger;

    private boolean remoteLogging = false;

	/** Provider deployment configuration. */
	protected DeploymentConfiguration config = new DeploymentConfiguration();

	/** The unique ID for this provider proxy verification. */
	private Uuid providerUuid;

	private Uuid adminProviderUuid;

	protected String[] groupsToDiscover;

	protected JavaSpace05 space;

	protected TransactionManager tManager;

	protected boolean workerTransactional = false;

	protected String spaceGroup;

	protected String spaceName;

    private List<SpaceTaker> spaceTakers = new ArrayList<SpaceTaker>();

	protected Class[] publishedServiceTypes;

	/** provider service type entry used to be included in the provider's proxy. */
	protected SorcerServiceInfo serviceInfo;

	protected boolean idPersistent = false;

	/** if true then we match all entries with interface type only. */
	protected boolean matchInterfaceOnly = true;

	/** if true then its provider can be monitored for its exerting behavior. */
	protected boolean monitorable = false;

	/** if true then its provider can produce notification. */
	protected boolean notifying = false;

	/* use Spacer workers, when false no space computing support. */
	protected boolean spaceEnabled = false;

	/* delay space takers startup in miliseconds */
	protected int spaceTakerDelay = 1000;

	protected boolean spaceReadiness = false;

	protected boolean spaceSecurityEnabled = false;

	private ThreadGroup namedGroup, interfaceGroup;

	private int workerCount = 10;

	private int[] workerPerInterfaceCount = new int[0];

	private int queueSize = 0;

	private int maximumPoolSize = 20;

	private List<ExecutorService> spaceHandlingPools;

	/** lease manager also used by provider workers. */
	protected static LeaseRenewalManager leaseManager = new LeaseRenewalManager();

	protected sorcer.core.provider.Provider provider;

	protected boolean mutualExclusion = true;

	// all exported services with corresponding exporter
	// <Remote, Exporter> or <service bean, service provider>
	private static Map exports = new HashMap();

	protected Object providerProxy;

	private long eventID = 0, seqNum = 0;

	private List<Entry> extraLookupAttributes = new Vector<Entry>();

    public int processedExertionsCount=0;

	/** Map of exertion ID's and state of execution */
	static final Map exertionStateTable = Collections.synchronizedMap(new HashMap(11));

	/**
	 * A smart proxy instance
	 */
	private Object smartProxy = null;

	/**
	 * A {@link Remote} partner object expending functionality of this provider.
	 * The provider's inner proxy can be used by the outer proxy of this
	 * provider to make remote redirectional calls on this partner.
	 */
	private Remote partner = null;

	/**
	 * A remote inner proxy implements Remote interface. Usually outer proxy
	 * complements its functionality by invoking remote calls on the inner proxy
	 * server. Thus, inner proxy can make remote calls on another service
	 * provider, for example {@code Provider.service(Exertion)), while the
	 * outer proxy still can call directly on the originating service provider.
	 */
	private Remote innerProxy = null;

	/**
	 * An outer service proxy, by default the proxy of this provider, is used
	 * by service requestors if provider's smart proxy is absent. At least
	 * two generic Remote interface: {@link Service} and {@link Provider} are
	 * implemented by outer proxies of all SORCER service providers. Each SORCER
	 * provider uses outer proxy to actually call directly its provider and make
	 * redirected calls using its inner proxy (redirected remote invocations).
	 * Any method of not Remote interface implemented by a SORCER service
	 * provider can be invoked via the Service remote interface,
	 * {@code Service.service(Mogram)} - the recommended access proxy approach.
	 * The provider's direct invocation methods are embedded into service signatures
	 * of serviced mograms.
	 */
	private Remote outerProxy = null;

	/** The exporter for exporting and unexporting outer proxy */
	private Exporter outerExporter;

	private SorcerILFactory ilFactory;

	/** The exporter for exporting and unexporting inner proxy */
	private Exporter partnerExporter;

	/**
	 * The admin proxy handles the standard Jini Admin interface.
	 */
	protected Object adminProxy;

	private Exporter adminExporter;

	/**
	 * SORCER service beans instantiated by this delegate
	 */
	private Object[] serviceBeans;

	/**
	 * Exposed service type components. A key is an interface and a value its
	 * implementing service-object.
	 */
	private Map<Class<?>, Object> serviceComponents;

	/**
	 * Indicates a single threaded execution for service beans or providers
	 * implementing the SingleThreadModel interface.
	 */
	private boolean singleThreadModel = false;

	private String hostName, hostAddress;

	private ContextManagement contextManager;

    protected AbstractExporterFactory exporterFactory;
    private boolean shuttingDown = false;

	private RemoteLoggerInstaller remoteLoggerInstaller;

	/*
	 * A nested class to hold the state information of the executing thread for
	 * a served exertion.
	 */
	public static class ExertionSessionInfo {

		static LeaseRenewalManager lrm = new LeaseRenewalManager();

		private static class ExertionSessionBundle {
			public Uuid exertionID;
			public MonitoringSession session;
		}

		private static final ThreadLocal<ExertionSessionBundle> tl = new ThreadLocal<ExertionSessionBundle>() {
			@Override
			protected ExertionSessionBundle initialValue() {
				return new ExertionSessionBundle();
			}
		};

		public static void add(ServiceExertion ex) {
			ExertionSessionBundle esb = tl.get();
			esb.exertionID = ex.getId();
			esb.session = ex.getMonitorSession();
			if (ex.getMonitorSession() != null)
				lrm.renewUntil(
						ex.getMonitorSession().getLease(),
						Lease.ANY, null);
		}

		public static MonitoringSession getSession() {
			ExertionSessionBundle esb = tl.get();
			return (esb != null) ? esb.session : null;
		}

		public static Uuid getID() {
			ExertionSessionBundle esb = tl.get();
			return (esb != null) ? esb.exertionID : null;
		}

		public static void removeLease() {
			ExertionSessionBundle esb = tl.get();
			try {
				lrm.remove(esb.session.getLease());
			} catch (Exception e) {
			}
		}
	}

	public ProviderDelegate() {
	}

	public void init(sorcer.core.provider.Provider provider) throws ConfigurationException {
		init(provider, null);
	}

	public void init(sorcer.core.provider.Provider provider, String configFilename) throws ConfigurationException {
		this.provider = provider;
		String providerProperties = configFilename;
		// Initialize remote logging
		remoteLoggerInstaller = RemoteLoggerInstaller.getInstance();
		// This allows us to specify different properties for different hosts
		// using a shared mounted file system
		if (providerProperties != null && providerProperties.contains("HOSTNAME")) {
			try {
				providerProperties = providerProperties.replace("HOSTNAME", Sorcer.getHostName());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		restore();
        // Initialize hostName and hostAddress
        getHostName();
        getHostAddress();
        // set provider's ID persistance flag if defined in provider's
		// properties
		idPersistent = Sorcer.getProperty(P_SERVICE_ID_PERSISTENT, "false").equals("true");
		// set provider join groups if defined in provider's properties
		groupsToDiscover = Sorcer.getLookupGroups();
		logger.info("ServiceProvider:groups to discover={}"+ SorcerUtil.arrayToString(groupsToDiscover));
		// set provider space group if defined in provider's properties
		spaceGroup = config.getProperty(J_SPACE_GROUP, Sorcer.getSpaceGroup());
		// set provider space name if defined in provider's properties
		spaceName = config.getProperty(J_SPACE_NAME, Sorcer.getActualSpaceName());

		try {
			singleThreadModel = (Boolean) config.jiniConfig.getEntry(ServiceProvider.COMPONENT,
                                                                     J_SINGLE_TRHREADED_MODEL,
                                                                     boolean.class,
                                                                     false);
		} catch (ConfigurationException e) {
			// do nothing, used the default value
		}
	//	initDynamicServiceAccessor();
	}

	/*private void initDynamicServiceAccessor() {
		try {
			String val = Sorcer.getProperty(S_SERVICE_ACCESSOR_PROVIDER_NAME);
			if (val != null && val.equals(ProviderLookup.class.getName())) {
				ProviderLookup.init();
			} else if (val != null
					&& val.equals(ProviderLocator.class.getName())) {
				ProviderLocator.init();
			} else if (val != null
					&& val.equals(ProviderAccessor.class.getName())) {
				ProviderAccessor.init();
			} else if (val != null
					&& val.equals(ProviderAccessor.class.getName())) {
				ProviderAccessor.init();
			}
		} catch (AccessorException e) {
			e.printStackTrace();
		}
	}*/

	void initSpaceSupport() throws ConfigurationException {
		if (!spaceEnabled)
			return;

		space = SpaceAccessor.getSpace(spaceName);
		if (space == null) {
			int ctr = 0;
			while (space == null && ctr++ < TRY_NUMBER) {
                if(shuttingDown) {
                    logger.warn("Shutting down, abort space discovery");
                    break;
                }
				logger.warn("could not get space, trying again... try number = "+ ctr);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				space = SpaceAccessor.getSpace(spaceName);

			}
			if (space != null) {
				logger.info("got space = " + space);
			} else {
				logger.warn("*** Warn: could not get space...moving on.");
			}
		}
        if(!shuttingDown) {
            if (workerTransactional)
                tManager = TransactionManagerAccessor.getTransactionManager();

            try {
                startSpaceTakers();
            } catch (Exception e) {
                logger.error("Provider HALTED: Couldn't start Workers", e);
                try {
                    provider.destroy();
                } catch (RemoteException e1) {
                    logger.error("Could not destroy provider", e1);
                }
            }
        }
	}

    protected void configure(Configuration jconfig) throws ExportException {
        final Thread currentThread = Thread.currentThread();
        implClassLoader = currentThread.getContextClassLoader();
        Class partnerType;
        String partnerName;

        try {
            monitorable = (Boolean) jconfig.getEntry(ServiceProvider.COMPONENT,
                                                     PROVIDER_MONITORING, boolean.class, false);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, PROVIDER_MONITORING, e);
        }

        try {
            notifying = (Boolean) jconfig.getEntry(ServiceProvider.COMPONENT,
                                                   PROVIDER_NOTIFYING, boolean.class, false);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, PROVIDER_NOTIFYING, e);
        }

        try {
            mutualExclusion = (Boolean) jconfig.getEntry(ServiceProvider.COMPONENT, MUTUAL_EXCLUSION, boolean.class,
                                                         false);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, MUTUAL_EXCLUSION, e);
        }

        try {
            matchInterfaceOnly = (Boolean) jconfig.getEntry(ServiceProvider.COMPONENT, INTERFACE_ONLY, boolean.class,
                                                            true);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, INTERFACE_ONLY, e);
        }

        try {
			spaceEnabled = (Boolean) jconfig.getEntry(ServiceProvider.COMPONENT, SPACE_ENABLED, boolean.class,
					false);
		} catch (Exception e) {
			logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, SPACE_ENABLED, e);
		}

		try {
			spaceTakerDelay = (Integer) jconfig.getEntry(ServiceProvider.COMPONENT, SPACE_TAKER_DELAY, int.class,
					1000); // defult 1000 milisecnds
		} catch (Exception e) {
			logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, SPACE_TAKER_DELAY, e);
		}

        try {
            workerCount = (Integer) jconfig.getEntry(ServiceProvider.COMPONENT,
                                                     WORKER_COUNT, int.class, 10);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, WORKER_COUNT, e);
        }

        try {
            workerPerInterfaceCount = (int[]) jconfig.getEntry(ServiceProvider.COMPONENT, WORKER_PER_INTERFACE_COUNT,
                                                               int[].class, new int[0]);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, WORKER_PER_INTERFACE_COUNT, e);
        }

        try {
            queueSize = (Integer) jconfig.getEntry(ServiceProvider.COMPONENT,
                                                   SPACE_WORKER_QUEUE_SIZE, int.class, 0);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, SPACE_WORKER_QUEUE_SIZE, e);
        }

        try {
            maximumPoolSize = (Integer) jconfig.getEntry(ServiceProvider.COMPONENT, MAX_WORKER_POOL_SIZE, int.class,
                                                         20);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, MAX_WORKER_POOL_SIZE, e);
        }

        try {
            spaceReadiness = (Boolean) jconfig.getEntry(ServiceProvider.COMPONENT, SPACE_READINESS, boolean.class,
                                                        false);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, SPACE_READINESS, e);
        }

        try {
            workerTransactional = (Boolean) jconfig.getEntry(ServiceProvider.COMPONENT, WORKER_TRANSACTIONAL,
                                                             boolean.class, false);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, WORKER_TRANSACTIONAL, e);
        }

        try {
            spaceSecurityEnabled = (Boolean) jconfig.getEntry(ServiceProvider.COMPONENT, SPACE_SECURITY_ENABLED,
                                                              boolean.class, false);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, SPACE_SECURITY_ENABLED, e);
        }

        try {
            contextManager = (ContextManagement) jconfig.getEntry(ServiceProvider.COMPONENT, CONTEXT_MANAGER,
                                                                  ContextManagement.class, null);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, CONTEXT_MANAGER, e);
        }
        logger.info("*** assigned context manager: " + contextManager);

        try {
            partnerType = (Class) jconfig.getEntry(ServiceProvider.COMPONENT,
                                                   SERVER_TYPE, Class.class, null);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, SERVER_TYPE, e);
            partnerType = null;
        }
        try {
            partnerName = (String) jconfig.getEntry(ServiceProvider.COMPONENT,
                                                    SERVER_NAME, String.class, null);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, SERVER_NAME, e);
            partnerName = null;
        }
        try {
            partner = (Remote) jconfig.getEntry(ServiceProvider.COMPONENT,
                                                SERVER, Remote.class, null);
            logger.info("partner=" + partner);
        } catch (Exception e) {
            logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, SERVER, e);
            partnerName = null;
        }
		try {
			remoteLogging = (Boolean) jconfig.getEntry(
					ServiceProvider.COMPONENT, REMOTE_LOGGING, boolean.class,
					false);
			logger.info("remoteLogging=" + remoteLogging);
		} catch (Exception e) {
			logger.warn("Problem getting {}.{}", ServiceProvider.COMPONENT, SERVER, e);
			remoteLogging = false;
		}
        if (partner != null) {
            getPartner(partnerName, partnerType);
            exports.put(partner, partnerExporter);
        }
        Class[] serviceTypes = new Class[0];
        try {
            serviceTypes = (Class[]) config.jiniConfig.getEntry(ServiceProvider.COMPONENT, J_INTERFACES, Class[].class);
        } catch (ConfigurationException e) {
            // do nothing, used the default value
            // e.printStackTrace();
        }
        if ((serviceTypes != null) && (serviceTypes.length > 0)) {
            Set<Class<?>> toPublish = new HashSet<>();
            for(Class<?> c : serviceTypes) {
                toPublish.addAll(getAllInterfaces(c));
            }
            publishedServiceTypes = toPublish.toArray(new Class<?>[toPublish.size()]);
            logger.info("*** published services: {}", Arrays.toString(publishedServiceTypes));
        }
        // get exporters for outer and inner proxy
        getExporters(jconfig);
        logger.debug("exporting provider: {}", provider);
        logger.info("outerExporter = {}", outerExporter);
        try {
            if (outerExporter == null) {
                logger.error("No exporter for provider: {}", getProviderName());
                return;
            }
			outerProxy = outerExporter.export(provider);
            logger.debug("outerProxy: {}", outerProxy);
        } catch (Exception ee) {
            logger.warn("{} deployment failed", ProviderDelegate.class.getName(), ee);
        }
        adminProxy = createAdmin();
        providerProxy = getProxy();
        exports.put(outerProxy, outerExporter);
        logger.debug(">>>>>>>>>>> exported outerProxy: \n{}, outerExporter: \n{}", outerProxy, outerExporter);

        logger.info("PROXIES >>>>>\nprovider: {}\nsmart: {}\nouter: {}\ninner: {}\nadmin: {}",
                    providerProxy, smartProxy, outerProxy, innerProxy, adminProxy);
    }

    private Collection<Class<?>> getAllInterfaces(Class<?> c) {
        Set<Class<?>> set = new HashSet<>();
        if(!c.getPackage().getName().startsWith("java")) {
            set.add(c);
            for (Class<?> i : c.getInterfaces()) {
                set.add(i);
                set.addAll(getAllInterfaces(i));
            }
        }
        return set;
    }

    private void initThreadGroups() {
        namedGroup = new ThreadGroup("Provider Group: " + getProviderName());
        namedGroup.setDaemon(true);
        namedGroup.setMaxPriority(Thread.NORM_PRIORITY - 1);
        interfaceGroup = new ThreadGroup("Interface Group: " + getProviderName());
		interfaceGroup.setDaemon(true);
		interfaceGroup.setMaxPriority(Thread.NORM_PRIORITY - 1);
	}

	public void setSmartProxy(Object smartProxy) {
		this.smartProxy = smartProxy;
	}

	public void startSpaceTakers() throws ConfigurationException, RemoteException {
		ExecutorService spaceWorkerPool;
		spaceHandlingPools = new ArrayList<ExecutorService>();
		String msg;
		if (space == null) {
			msg = "ERROR: No space found, spaceName = " + spaceName
					+ ", spaceGroup = " + spaceGroup;
			logger.error(msg);
		}
		if (workerTransactional && tManager == null) {
			msg = "ERROR: no transactional manager found....";
			logger.error(msg);
		}
		if (publishedServiceTypes == null || publishedServiceTypes.length == 0) {
			msg = "ERROR: no published interfaces found....";
			logger.error(msg);
		}

		initThreadGroups();
		ExertionEnvelop envelop;
		LokiMemberUtil memberInfo = null;
		if (spaceSecurityEnabled) {
			memberInfo = new LokiMemberUtil(ProviderDelegate.class.getName());
		}

		logger.debug("*** provider worker count: {}, spaceTransactional: {}", workerCount, workerTransactional);
		logger.info("publishedServiceTypes.length = {}", publishedServiceTypes.length);
		logger.info(Arrays.toString(publishedServiceTypes));

		// create a pair of taker threads for each published interface
		SpaceTaker worker = null;

		// make sure that the number of core threads equals the maximum number
		// of threads
		if (queueSize == 0) {
			if (maximumPoolSize > workerCount)
				workerCount = maximumPoolSize;
		}

        ConfigurableThreadFactory factory = new ConfigurableThreadFactory();
        factory.setNameFormat(tName("SpcTkr-" + getProviderName()+ "-%2$d"));

        ConfigurableThreadFactory namedWorkerFactory = new ConfigurableThreadFactory();
        namedWorkerFactory.setThreadGroup(namedGroup);
        namedWorkerFactory.setNameFormat(tName("SpcTkr-" + getProviderName() + "-%2$d"));
        namedWorkerFactory.setDaemon(true);

        for (int i = 0; i < publishedServiceTypes.length; i++) {
            // spaceWorkerPool = Executors.newFixedThreadPool(workerCount);
            spaceWorkerPool = new ThreadPoolExecutor(workerCount,
                                                     maximumPoolSize > workerCount ? maximumPoolSize
                                                                                   : workerCount, 0L, TimeUnit.MILLISECONDS,
                                                     new LinkedBlockingQueue<Runnable>((queueSize == 0 ? workerCount : queueSize)),
                                                     factory);
            spaceHandlingPools.add(spaceWorkerPool);
            // SORCER.ANY is required for a ProviderWorker
            // to avoid matching to any provider name
            // that is Java null matching everything
            envelop = ExertionEnvelop.getTemplate(publishedServiceTypes[i], getProviderName());
            if (spaceReadiness) {
                worker = new SpaceIsReadyTaker(new SpaceTaker.SpaceTakerData(envelop,
                                                                             memberInfo,
                                                                             provider,
                                                                             spaceName,
                                                                             spaceGroup,
                                                                             workerTransactional,
                                                                             queueSize == 0),
										spaceWorkerPool);
                spaceTakers.add(worker);
            } else {
                worker = new SpaceTaker(new SpaceTaker.SpaceTakerData(envelop,
                                                                      memberInfo,
                                                                      provider,
                                                                      spaceName,
                                                                      spaceGroup,
                                                                      workerTransactional,
                                                                      queueSize == 0),
                                        spaceWorkerPool,
                                        remoteLogging);
                spaceTakers.add(worker);
            }
            ConfigurableThreadFactory ifaceWorkerFactory = new ConfigurableThreadFactory();
            ifaceWorkerFactory.setThreadGroup(interfaceGroup);
            ifaceWorkerFactory.setDaemon(true);
            ifaceWorkerFactory.setNameFormat(tName("SpcTkr-" + publishedServiceTypes[i].getSimpleName()));

            Thread sith = ifaceWorkerFactory.newThread(worker);
			sith.start();			
			logger.info("*** {} named space worker {} started for: {}",
					getProviderName(), i, publishedServiceTypes[i]);
			// System.out.println("space template: " +
			// envelop.describe());

			if (matchInterfaceOnly) {
				// spaceWorkerPool = Executors.newFixedThreadPool(workerCount);
				spaceWorkerPool = new ThreadPoolExecutor(workerCount,
						maximumPoolSize > workerCount ? maximumPoolSize
								: workerCount, 0L, TimeUnit.MILLISECONDS,
						new LinkedBlockingQueue<Runnable>(
								(queueSize == 0 ? workerCount : queueSize)), factory);
				spaceHandlingPools.add(spaceWorkerPool);
				envelop = ExertionEnvelop.getTemplate(publishedServiceTypes[i],
						SorcerConstants.ANY);
				if (spaceReadiness) {
					worker = new SpaceIsReadyTaker(
							new SpaceTaker.SpaceTakerData(envelop, memberInfo,
									provider, spaceName, spaceGroup,
									workerTransactional, queueSize == 0),
							spaceWorkerPool);
                    spaceTakers.add(worker);
				} else {
					worker = new SpaceTaker(new SpaceTaker.SpaceTakerData(
							envelop, memberInfo, provider, spaceName,
							spaceGroup, workerTransactional, queueSize == 0),
							spaceWorkerPool, remoteLogging);
                    spaceTakers.add(worker);
				}
				Thread snth = namedWorkerFactory.newThread(worker);
				snth.start();
				logger.info("*** {} unnamed space worker {} started for: ",
						getProviderName(), i, publishedServiceTypes[i]);
				// System.out.println("space template: " +
				// envelop.describe());
			}
		}
//		 interfaceGroup.list();
//		 namedGroup.list();
	}

	public Task doTask(Task task, Transaction transaction, Arg... args)
			throws MogramException, SignatureException, RemoteException {
		// prepare a default net batch task (has all sigs of PROC type)
		// and make the last signature as master PROC type only.
		task.correctBatchSignatures();
		task.getControlContext().appendTrace(
				provider.getProviderName() + " to exert: "
						+ (task.getProcessSignature()!=null ? task.getProcessSignature().getSelector() : "null") + ":"
						+ (task.getProcessSignature()!=null ? task.getProcessSignature().getServiceType() : "null") + ":"
						+ getHostName());

		if (task instanceof SignedTaskInterface) {
			try {
				new TaskAuditor().audit((SignedServiceTask) task);
				task = (Task) ((SignedTaskInterface) task).getObject();
			} catch (Exception e) {
				logger.error("Exception while retrieving SIGNED TASK", e);
			}
		}
		/*
		 * String actions = task.method.action(); GuardedObject go = new
		 * GuardedObject(task.method, new ServiceMethodPermission(task.userID,
		 * actions)); try { Object o = go.getObject(); Util.debug(this, "Got
		 * access to method: " + actions); } catch (AccessControlException ace)
		 * { throw new ExertionMethodException ("Can't access method: " +
		 * actions); }
		 */
		if (isValidTask(task)) {
            logger.info("task " + task.getName() + " is valid");
			try {
				task.updateContext();
				task.startExecTime();
				exertionStateTable.put(task.getId(), Exec.RUNNING);
				if (((ServiceProvider) provider).isValidTask(task)) {
					// append context from Contexters
					if (task.getApdProcessSignatures().size() > 0) {
						Context cxt = apdProcess(task);
						cxt.setExertion(task);
						task.setContext(cxt);
						task.setService(provider);
					}
					// preprocessing
					if (task.getPreprocessSignatures().size() > 0) {
						Context cxt = preprocess(task);
						cxt.setExertion(task);
						task.setContext(cxt);
						task.setService(provider);
					}
					// service processing
					NetSignature tsig = (NetSignature) task
							.getProcessSignature();

					tsig.setProvider(provider);
					// reset path prefix and return path
                    if (tsig.getPrefix() != null)
                        ((ServiceContext)task.getContext()).setPrefix(tsig.getPrefix());
					if (tsig.getReturnPath() != null)
							((ServiceContext) task.getContext()).setReturnPath(tsig.getReturnPath());

					if (isBeanable(task)) {
						task = useServiceComponents(task, transaction, args);
					} else {
						logger.info("going to execTask(); transaction = {}", transaction);
						task = execTask(task);
						logger.info("DONE going to execTask(); transaction = {}", transaction);
					}
					// postprocessing
					logger.info("postprocessing task...transaction = {}", transaction);
					if (task.getPostprocessSignatures().size() > 0) {
						Context cxt = postprocess(task);
						cxt.setExertion(task);
						task.setContext(cxt);
						task.setService(provider);
					}
					confirmExec(task);
					task.stopExecTime();
					logger.info("provider name = {}\nreturning task; transaction = {}", provider.getDescription(), transaction);
					return task;
				} else {
					provider.fireEvent();
					task.stopExecTime();
                    ExertionException ex = new ExertionException("Unacceptable task received, requested provider: "
                                                                 + getProviderName() + " task: " + task.getName());
					task.reportException(ex);
					task.setStatus(Exec.FAILED);
					return (Task) forwardTask(task, provider);
				}
			} finally {
                processedExertionsCount++;
                logger.warn("EXERTIONS PROCESSED: {}", processedExertionsCount);
                exertionStateTable.remove(exertionStateTable.remove(task.getId()));
			}
		}
		return (Task) forwardTask(task, provider);
	}

	private Context apdProcess(Task task) throws ExertionException, SignatureException, ContextException {
		return processContinousely(task, task.getApdProcessSignatures());
	}

	private Context preprocess(Task task) throws ExertionException,
	SignatureException, ContextException {
		return processContinousely(task, task.getPreprocessSignatures());
	}

	private Context postprocess(Task task) throws ExertionException,
	SignatureException, ContextException {
		return processContinousely(task, task.getPostprocessSignatures());
	}

	private Context processContinousely(Task task, List<Signature> signatures)
			throws ExertionException, ContextException {
		try {
            ControlFlowManager cfm = new ControlFlowManager(task, this);
            return cfm.processContinousely(task, signatures);
        }   catch (Exception e) {
            ((Task) task).reportException(e);
            throw new ExertionException(e);
        }
	}

	private void resetSigantures(List<Signature> signatures, Signature.Type type) {
		for (int i = 0; i < signatures.size(); i++) {
			signatures.get(i).setType(type);
		}
	}

	private void confirmExec(Task task)  {
		String pn;
		try {
			pn = getProviderName();
			if (pn == null || pn.length() == 0)
				pn = getDescription();
			Contexts.putOutValue(task.getContext(), TASK_PROVIDER, pn + "@"
					+ hostName + ":" + hostAddress);
		} catch (ContextException ex) {
			// ignore ocall call
		}
	}

	private boolean isBeanable(Task task) {
		if (serviceComponents == null || serviceComponents.size() == 0)
			return false;
		Class serviceType = task.getProcessSignature().getServiceType();
		logger.debug("match serviceType: {}", serviceType);
        // check declared interfaces
        if(serviceComponents.containsKey(serviceType))
            return true;

        for (Class next: serviceComponents.keySet()) {
			// check implemented interfaces
            if(next.isAssignableFrom(serviceType))
                return true;
		}
		return false;
	}

	private Task useServiceComponents(Task task, Transaction transaction, Arg... args)
			throws ContextException {
		String selector = task.getProcessSignature().getSelector();
		Class serviceType = task.getProcessSignature().getServiceType();
		Iterator i = serviceComponents.entrySet().iterator();
		Map.Entry next;
		Object impl = null;
		while (i.hasNext()) {
			next = (Map.Entry) i.next();
			if (next.getKey() == serviceType) {
				impl = next.getValue();
				break;
			}
			Class[] supertypes = ((Class)next.getKey()).getInterfaces();			
			for (Class st : supertypes) {
				if (st == serviceType) {
					impl = next.getValue();
					break;
				}
			}
		}
		if (impl != null) {
			if (task.getProcessSignature().getReturnPath() != null) {
				((ServiceContext) task.getContext()).setReturnPath(task
						.getProcessSignature().getReturnPath());
			}
			// determine args and parameterTpes from the context
			Class[] argTypes = new Class[] { Context.class };
			ServiceContext cxt = (ServiceContext) task.getContext();
			boolean isContextual = true;
			if (cxt.getParameterTypes() != null & cxt.getArgs() != null) {
				argTypes = cxt.getParameterTypes();
				isContextual = false;
			} 
			Method m = null;
			try {
				// select the proper method for the bean type
				if (selector.equals("invoke") && (impl instanceof Exertion  
						|| impl instanceof ParModel)) {
					m = impl.getClass().getMethod(selector,
							new Class[] { Context.class, Arg[].class });
					isContextual = true;
				} else if (selector.equals("exert") && impl instanceof ServiceShell) {
					m = impl.getClass().getMethod(selector,
							new Class[] { Mogram.class, Arg[].class });
					isContextual = false;
				} else if (selector.equals("getValue") && impl instanceof Evaluation) {
					m = impl.getClass().getMethod(selector,
							new Class[] { Arg[].class });
					isContextual = false;
				} else
					m = impl.getClass().getMethod(selector, argTypes);
				logger.info("Executing service bean method: " + m + " by: "
						+ config.getProviderName() + " isContextual: " + isContextual);
				task.getContext().setExertion(task);
				((ServiceContext) task.getContext()).getMogramStrategy().setCurrentSelector(selector);
				String pf = task.getProcessSignature().getPrefix();
				if (pf != null)
					((ServiceContext) task.getContext()).setCurrentPrefix(pf);

				Context result = task.getContext();
				if (isContextual) 
					result = execContextualBean(m, task, impl, args);
				else
					result = execParametricBean(m, task, impl, args);

				// clear task in the context
				result.setExertion(null);
				task.setContext(result);
				task.setStatus(Exec.DONE);
				return task;
            } catch (InvocationTargetException e){
                Throwable t = e.getCause();
                task.reportException(t);
                logger.warn("Error while executing: " + m + " " + t.getMessage());
			} catch (Exception e) {
				task.reportException(e);
                logger.warn("Error while executing: " + m + " " + e.getMessage());
			}
		}
		task.setStatus(Exec.FAILED);
		return task;
	}

	private Context execContextualBean(Method m, Task task, Object impl, Arg... args)
			throws ContextException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException, RemoteException {
		Context result;
		result = task.getContext();
		String selector = task.getProcessSignature().getSelector();
		Object[] pars = new Object[] { task.getContext() };
		if (selector.equals("invoke")
				&& (impl instanceof Exertion || impl instanceof ParModel)) {
			Object obj = m.invoke(impl, new Object[] { pars[0], args });

			if (obj instanceof Job)
				result = ((Job) obj).getJobContext();
			else if (obj instanceof Task)
				result = ((Task) obj).getContext();
			else if (obj instanceof Context)
				result = result.append((Context) obj);
			else
				result.setReturnValue(obj);

			if (obj instanceof Exertion) {
				task.getControlContext().getExceptions().addAll(((Exertion) obj).getExceptions());
				task.getTrace().addAll(((Exertion) obj).getTrace());
			}
		} else {
			logger.debug("getProviderName: {} invoking: {}" + getProviderName(), m);
			logger.debug("imp: {} args: {}" + impl, Arrays.toString(pars));
			result = (Context) m.invoke(impl, pars);
			logger.debug("result: {}", result);
		}
		return result;
	}

	private Context execParametricBean(Method m, Task task,
			Object impl, Arg... args) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException, ContextException, RemoteException {
		Context result = task.getContext();
		String selector = task.getProcessSignature().getSelector();
		Class[] argTypes = ((ServiceContext)result).getParameterTypes();
		Object[] pars = ((ServiceContext)result).getArgs();
		if (selector.equals("exert") && impl instanceof ServiceShell) {
			Exertion xrt = null;
			if (pars.length == 1) {
				xrt = (Exertion) m.invoke(impl, new Object[] { pars[0], args });
			} else {
				xrt = (Exertion) m.invoke(impl, pars);
			}
			if (xrt.isJob())
				result = ((Job) xrt).getJobContext();
			else
				result = xrt.getContext();
			task.getControlContext().getExceptions()
			.addAll(xrt.getExceptions());
			task.getTrace().addAll(xrt.getTrace());
			//((ServiceContext) result).setReturnValue(result);

		} else if (selector.equals("getValue") && impl instanceof Evaluation) {
			Object obj;
			if (argTypes == null) {
				obj = m.invoke(impl, new Object[] { args });
			} else {
				obj = m.invoke(impl, pars);
			}
			result.setReturnValue(obj);
		} else {
			result.setReturnValue(m.invoke(impl, pars));
		}
		return result;
	}

	protected ServiceExertion forwardTask(ServiceExertion task,
			Provider requestor) throws MogramException,
			RemoteException, SignatureException, ContextException {
		// check if we do not look with the same exertion
		Service recipient = null;
		String prvName = task.getProcessSignature().getProviderName();
		NetSignature fm = (NetSignature) task.getProcessSignature();
		ServiceID serviceID = fm.getServiceID();
		Class prvType = fm.getServiceType();
		logger.info("ProviderDelegate#forwardTask \nprvType: {}\nprvName = {}", prvType, prvName);

		if (visited == null)
			visited = new HashSet();

		if (visited.contains(serviceID)) {
			visited.remove(serviceID);
			throw new ExertionException("Not able to get relevant type: "+ prvType + ", name: " + prvName);
		}
		visited.add(serviceID);
		if (serviceComponents != null) {
			Task result = useServiceComponents((Task) task, null);
			logger.info("forwardTask executed by a service bean: {}", result);
			if (result != null) {
				visited.remove(serviceID);
				return result;
			} else {
				task.setStatus(Exec.ERROR);
				return task;
			}
		}
		if (serviceID != null)
			recipient = (Service) Accessor.get().getService(serviceID);
		else if (prvType != null && prvName != null) {
			recipient = (Service) Accessor.get().getService(prvName, prvType);
		} else if (prvType != null) {
			recipient = (Service) Accessor.get().getService(null, prvType);
		}
		if (recipient == null) {
			visited.remove(serviceID);
			ExertionException re = new ExertionException(
					"Not able to get provider type: " + prvType + ", name: "
							+ prvName);
			notifyException(task, "", re);
			throw re;
		} else if (recipient.getClass().getName()
				.startsWith(requestor.getClass().getName())) {
			visited.remove(serviceID);
			ExertionException re = new ExertionException(
					"Invalid task for provider type: " + prvType + ", name: "
							+ prvName + " " + task.toString());
			notifyException(task, "", re);
			throw re;
		} else
			try {
				Task result = (Task) ((Exerter)recipient).exert(task, null);
				if (result != null) {
					visited.remove(serviceID);
					return result;
				} else {
					visited.remove(serviceID);
					throw new ExertionException(
							"Not able to get relevant type: " + prvType
							+ ", name: " + prvName);
				}
			} catch (TransactionException te) {
				visited.remove(serviceID);
				throw new ExertionException("transaction failure", te);
			}
	}

	public ServiceExertion dropTask(Exertion entryTask)
			throws ExertionException, SignatureException, RemoteException {
		return null;
	}

	public static Job doJob(Job job) throws MogramException, RemoteException {
		String jobberName = job.getRendezvousName();
		Jobber jobber;
		try {
			if (jobberName != null)
				jobber = JobberAccessor.getJobber(jobberName);
			else
				jobber = JobberAccessor.getJobber();
		} catch (AccessorException ae) {
			logger.warn("Failed", ae);
			throw new ExertionException(
					"Provider Delegate Could not find the Jobber");
		}

		Job outJob;
		try {
			outJob = (Job) ((Exerter)jobber).exert(job, null);
		} catch (TransactionException te) {
			throw new ExertionException("transaction failure", te);
		}
		return outJob;
	}

	public Job dropJob(Job job) throws RemoteException, ExertionException {
		return null;
	}

	public void hangup() throws RemoteException {
		String str = config.getProperty(P_DELAY_TIME);
		if (str != null) {
			try {
				// delay is in seconds
				int delay = Integer.parseInt(str);
				Thread.sleep(delay * 1000);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}

	public boolean isValidMethod(String name) throws RemoteException {
		// modify name for SORCER providers
		Method[] methods = provider.getClass().getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(name))
				return true;
		}
		return false;
	}

	public Task execTask(Task task) throws ContextException, ExertionException,
			SignatureException, RemoteException {
		ServiceContext cxt = (ServiceContext) task.getContext();
		try {
			if (cxt.isValid(task.getProcessSignature())) {
				Signature sig = task.getProcessSignature();
				if (sig.getReturnPath() != null)
					cxt.setReturnPath(sig.getReturnPath());

				cxt.getMogramStrategy().setCurrentSelector(sig.getSelector());
				cxt.setCurrentPrefix(sig.getPrefix());

				cxt.setExertion(task);
				task.setService(provider);

				if (sig instanceof NetSignature)
					((NetSignature) sig).setProvider(provider);
				task.setStatus(Exec.FAILED);
				logger.info("DELEGATE EXECUTING TASK: " + task + " by sig: "
						+ task.getProcessSignature() + " for context: " + cxt);
				cxt = (ServiceContext) invokeMethod(sig.getSelector(), cxt);
				logger.info("doTask: TASK DONE BY DELEGATE OF ="
						+ provider.getProviderName());
				task.setContext(cxt);
				task.setStatus(Exec.DONE);
				if (cxt.getReturnPath() != null) {
					cxt.setReturnValue(cxt.getValue(cxt.getReturnPath().path));
				} else if (task.getDataContext().getScope() != null) {
					task.getDataContext().getScope().append(cxt);
				}
				// clear the exertion and the context
				cxt.setExertion(null);
				task.setService(null);
				logger.info("CONTEXT GOING OUT: " + cxt);
			}
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
		return task;
	}

	public Exertion invokeMethod(String selector, Exertion ex)
			throws ExertionException {
		Class[] argTypes = new Class[] { Mogram.class };
		try {
			Method m = provider.getClass().getMethod(selector, argTypes);
			logger.info("Executing method: " + m + " by: "
					+ config.getProviderName());

			Exertion result = (Exertion) m
					.invoke(provider, new Object[] { ex });
			return result;
		} catch (Exception e) {
			ex.getControlContext().addException(e);
			throw new ExertionException(e);
		}
	}

	public Context invokeMethod(String selector, Context sc)
			throws ExertionException {
		try {
			Class[] argTypes = new Class[] { Context.class };
			Object[] args = new Object[] { sc };
			ServiceContext cxt = (ServiceContext) sc;
			boolean isContextual = true;
			if (cxt.getParameterTypes() != null & cxt.getArgs() != null) {
				argTypes = cxt.getParameterTypes();
				args = cxt.getArgs();
				isContextual = false;
			}
			Method execMethod = provider.getClass().getMethod(selector,
					argTypes);
			Context result = null;
			if (isContextual) {
				result = (ServiceContext) execMethod.invoke(provider, args);
				// Setting Return Values
				if (result.getReturnPath() != null) {
					Object resultValue = result.getValue(((ServiceContext)result).getReturnPath().path);
					result.setReturnValue(resultValue);
				} 
			} else {
				sc.setReturnValue(execMethod.invoke(provider, args));
				result = sc;
			}
			return result;

		} catch (Exception e) {
			throw new ExertionException(e);
		}
	}

	private void doMethodAs(Subject subject, final String methodName)
			throws java.security.PrivilegedActionException,
			AccessControlException {

		Subject.doAs(subject, new PrivilegedExceptionAction() {
			public Object run() throws Exception {
				AccessController.checkPermission(new AccessPermission(
						methodName));
				return null;
			}
		});
	}

	/**
	 * Returns a service type of the provider served by this delegate as
	 * registered with lookup services.
	 * 
	 * @return a SorcerServiceType
	 */
	public SorcerServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	public Properties getProviderProperties() {
		return config.getProviderProperties();
	}

	public Configuration getProviderConfiguration() {
		return config.getProviderConfiguration();
	}

	public String getDescription() {
		return config.getProperty(P_DESCRIPTION);
	}

	/**
	 * Set a name of the provider. The name may be defined in this provider's
	 * properties file.
	 */
	public void setProviderName(String name) {
		config.setProviderName(name);
	}

	public String[] getGroups() {
		return groupsToDiscover;
	}

	/**
	 * Adds an additional entry to add to the lookup attributes.
	 * 
	 * @param extra
	 *            the extra Lookup entry.
	 */
	public void addExtraLookupAttribute(Entry extra) {
		extraLookupAttributes.add(extra);
	}

	public List<Object> getProperties() {
		List<Object> allAttributes = new ArrayList<Object>();
		Entry[] attributes = getAttributes();
		for (Entry entry : attributes)
			allAttributes.add(entry);
		allAttributes.add(config.getProviderProperties());
		allAttributes.add(Sorcer.getProperties());
		return allAttributes;
	}

	/**
	 * Creates the service attributes to be used with Jini lookup services.
	 * <p>
	 * This function will create the following entries:
	 * <ul>
	 * <li>A {@link Name}.
	 * <li>A {@link SorcerServiceInfo}entry with all the information about this
	 * provider.
	 * <li>A main UIDescriptor if the provider overrides
	 * <li>Extra lookup attributes set via #addExtraLookupAttribute(Entry)
	 * </ul>
	 * 
	 * @return an array of Jini Service Entries.
	 * @throws ConfigurationException
	 */
	public Entry[] getAttributes() {
		final List<Entry> attrVec = new ArrayList<Entry>();

		try {
			// name of the provider suffixed in loadJiniConfiguration
			boolean discoveryEnabled = (Boolean) Config.getNonNullEntry(
					getDeploymentConfig(), ServiceProvider.COMPONENT,
					ProviderDelegate.DISCOVERY_ENABLED, boolean.class, true);
			if (discoveryEnabled)
				attrVec.add(new Name(getProviderName()));
			else
				attrVec.add(new Name("Admin-" + getProviderName()));

			attrVec.addAll(VersionInfo.productAttributesFor(getProviderName()));
			Entry sst = getSorcerServiceTypeEntry();
			attrVec.add(sst);
			// add additional entries declared in the Jini provider's
			// configuration
			Entry[] miscEntries = (Entry[]) config.jiniConfig.getEntry(
					ServiceProvider.COMPONENT, "entries", Entry[].class,
					new Entry[] {});
			for (int i = 0; i < miscEntries.length; i++) {
				attrVec.add(miscEntries[i]);
				// transfer location from entries if not defined in
				// SorcerServiceInfo
				if (miscEntries[i] instanceof Location
						&& ((SorcerServiceInfo) sst).location == null) {
					((SorcerServiceInfo) sst).location = "" + miscEntries[i];
				}
			}

			// add the service context of this provider to provider attributes
			// AccessControlContext context = AccessController.getContext();
			// Subject subject = Subject.getSubject(context);
			// logger.debug("The subject in Provider Delegate is: " + subject);
		} catch (Exception ex) {
			logger.warn(SorcerUtil.stackTraceToString(ex));
		}

		// This construct may look strange. But it ensures that this class loads
		// if rio is not in the classpath
		//
		// The code is equivalent to
		// ComputeResourceInfo a = new ComputeResourceInfo();
		// a.initialize();
		// attrVec.add(a);
		try {
			Class c = this.getClass().getClassLoader().loadClass("org.rioproject.entry.ComputeResourceInfo");
			Object computeResourceInfo = c.newInstance();
			Method m = c.getMethod("initialize", new Class[] {});
			m.invoke(computeResourceInfo, (Object[]) null);
			attrVec.add((Entry) computeResourceInfo);
		} catch (Exception e) {
			// This happens if RIO classes are not in classpath. Ignore
		}

		attrVec.addAll(extraLookupAttributes);

		return attrVec.toArray(new Entry[] {});
	}

	/**
	 * Creates an entry that is a {@link SorcerServiceInfo}.
	 * 
	 * @return an entry for the provider.
	 * @throws UnknownHostException 
	 */
	private Entry getSorcerServiceTypeEntry() throws UnknownHostException {
		SorcerServiceInfo serviceType = new SorcerServiceInfo();
		try {
			serviceType.providerName = config.getProviderName();
			serviceType.shortDescription = config.getProperty(P_DESCRIPTION);
			serviceType.location = config.getProperty(P_LOCATION);
			serviceType.groups = SorcerUtil.arrayToCSV(groupsToDiscover);
			serviceType.spaceGroup = spaceGroup;
			serviceType.spaceName = spaceName;
			serviceType.puller = spaceEnabled;
			serviceType.monitorable = monitorable;
			serviceType.matchInterfaceOnly = matchInterfaceOnly;
			serviceType.startDate = new Date().toString();
			serviceType.serviceHome = Sorcer.getHome();
			serviceType.userName = System.getProperty("user.name");
			serviceType.repository = config.getDataDir();
			serviceType.iconName = config.getIconName();

			if (publishedServiceTypes == null && spaceEnabled) {
				logger.error("{} does NOT declare its space interfaces", getProviderName());
				provider.destroy();
			}
			if (publishedServiceTypes != null) {
				String[] typeNames = new String[publishedServiceTypes.length];
				for (int i = 0; i < publishedServiceTypes.length; i++) {
					typeNames[i] = publishedServiceTypes[i].getName();
				}
				serviceType.publishedServices = typeNames;
			}
			serviceType.serviceID = provider.getProviderID();
		} catch (Exception ex) {
			logger.warn("Some problem in accessing attributes");
			logger.warn(SorcerUtil.stackTraceToString(ex));
		}
		String hostName = null, hostAddress = null;
		hostName = Sorcer.getHostName();
		hostAddress = Sorcer.getHostAddress();

		if (hostName != null) {
			serviceType.hostName = hostName;
		} else {
			logger.warn("Host is null!");
		}

		if (hostAddress != null) {
			serviceType.hostAddress = hostAddress;
			;
		} else {
			logger.warn("Host address is null!!");
		}
		return serviceType;
	}

	/**
	 * Restores the ServiceID from {@link SorcerConstants#S_SERVICE_ID_FILENAME}
	 */
	public void restore() {
		if (idPersistent) {
			try {
				this.setProviderUuid((ServiceID) ObjectLogger.restore(Sorcer
						.getProperty(S_SERVICE_ID_FILENAME,
								Sorcer.getServiceIdFilename())));
			} catch (Exception e) { // first time if exception caught
				e.printStackTrace();
			}
		}
	}

	private void ensurePrviderUuidIsSet() {
		if (providerUuid == null) {
			providerUuid = UuidFactory.generate();
		}
	}

	/**
	 * Returns a ServiceID for a given Uuid.
	 * 
	 * @return a ServiceID representation of a Uuid.
	 */
	public ServiceID getServiceID(Uuid uuid) {
		return new ServiceID(uuid.getMostSignificantBits(),
				uuid.getLeastSignificantBits());
	}

	/**
	 * Retrieves the ServerUUID as an ServiceID.
	 * 
	 * @return a ServiceID representation of the ServerUUID.
	 */
	public ServiceID getServiceID() {
		ensurePrviderUuidIsSet();
		return getServiceID(providerUuid);
	}

	protected Uuid getProviderUuid() {
		if (providerUuid == null) {
			providerUuid = UuidFactory.generate();
		}
		return providerUuid;
	}

	protected Uuid getAdminProviderUuid() {
		if (adminProviderUuid == null) {
			adminProviderUuid = UuidFactory.generate();
		}
		return adminProviderUuid;
	}

	/**
	 * Sets the Uuid of this server from a given ServiceID.
	 * 
	 * @param serviceID
	 *            the ServiceID to use.
	 */
	public void setProviderUuid(ServiceID serviceID) {
		logger.info("Setting service ID:" + serviceID);
		providerUuid = UuidFactory.create(serviceID.getMostSignificantBits(),
				serviceID.getLeastSignificantBits());
	}

	/**
	 * Sets the Uuid of this provider from a given {@link Uuid}.
	 * 
	 * @param providerUuid
	 *            the Uuid to use.
	 */
	public void setServerUuid(Uuid providerUuid) {
		logger.info("Setting provider Uuid:" + providerUuid);
		this.providerUuid = providerUuid;
	}

	public String getInfo() throws RemoteException {
		return provider.getInfo();
	}

	private void addType(Class type, Set typeSet, boolean withSupertypes) {
		if (type == null)
			return;
		String typeName = type.getName();
		if (typeSet.contains(typeName))
			return;

		typeSet.add(typeName);
		if (!withSupertypes)
			return;

		if (withSupertypes)
			addType(type.getSuperclass(), typeSet, withSupertypes);

		Class[] stypes = type.getInterfaces();
		for (int i = 0; i < stypes.length; i++) {
			addType(stypes[i], typeSet, withSupertypes);
		}
	}

	private Set getTypes() {
		SortedSet sortSet = new TreeSet();
		addType(provider.getClass(), sortSet, true);
		String proxyName = config.getProperty(P_PROXY_CLASS);
		// Util.debug(this, "getTypes:proxyName=" + proxyName);
		if (proxyName != null) {
			try {
				Class proxyClass = Class.forName(proxyName);
				addType(proxyClass, sortSet, true);
			} catch (ClassNotFoundException cnfe) {
				cnfe.printStackTrace();
			}
		}
		return sortSet;
	}

	static LeaseRenewalManager getLeaseManager() {
		return leaseManager;
	}

	public void destroy() {
        shuttingDown = true;
        if (remoteLoggerInstaller!=null) {
            remoteLoggerInstaller.destroy();
        }
		if (spaceEnabled && spaceHandlingPools != null) {
            for (SpaceTaker st : spaceTakers) {
                if(st!=null)
                    st.destroy();
            }
			for (ExecutorService es : spaceHandlingPools)
				shutdownAndAwaitTermination(es);
			if (interfaceGroup != null) {
				Thread[] ifgThreads = new Thread[interfaceGroup.activeCount()];
				Thread[] ngThreads = new Thread[namedGroup.activeCount()];
				interfaceGroup.enumerate(ifgThreads);
				namedGroup.enumerate(ngThreads);
                // Wait until spaceTakers shutdown
                int attempts = 0;
                Set<Thread> spaceTakerThreads = new HashSet<Thread>();
                while (attempts < 11 && !spaceTakerThreads.isEmpty()) {
                    try {
                        Thread.sleep(SpaceTaker.SPACE_TIMEOUT/10);
                    } catch (InterruptedException ie) {
                    }
                    attempts++;
                    for (Thread thread : ifgThreads) {
                        if (thread.isAlive())
                            spaceTakerThreads.add(thread);
                        else
                            spaceTakerThreads.remove(thread);
                    }
                    for (Thread thread : ngThreads) {
                        if (thread.isAlive())
                            spaceTakerThreads.add(thread);
                        else
                            spaceTakerThreads.remove(thread);
                    }
                    if (spaceTakerThreads.isEmpty())
                        break;
                }
                for (Thread thread : spaceTakerThreads) {
                    if (thread.isAlive()) {
						thread.interrupt();
						logger.warn("Thread alive " + thread.getName());
					}
                }
            }
		}
/*        if (beanListener != null && serviceBeans != null)
            for (Object serviceBean : serviceBeans)
                beanListener.destroy(serviceBuilder, serviceBean);*/
	}

	public void fireEvent() throws RemoteException {
		provider.fireEvent();
	}

	public boolean isValidTask(Exertion servicetask) throws ExertionException, ContextException {

		if (servicetask.getContext() == null) {
			servicetask.getContext().reportException(
					new ExertionException(getProviderName()
							+ " no service context in task: "
							+ servicetask.getClass().getName()));
			return false;
		}
		Task task = (Task)servicetask;

		// if (task.subject == null)
		// throw new ExertionException("No subject provided with the task '" +
		// task.getName() + "'");
		// else if (!isAuthorized(task))
		// throw new ExertionException("The subject provided with the task '" +
		// task.getName() + "' not authorized to use the service '" +
		// providerName + "'");

		String pn = task.getProcessSignature().getProviderName();
		if (pn != null && !matchInterfaceOnly) {
			if (!pn.equals(getProviderName())) {
				servicetask.getContext().reportException(
						new ExertionException(
								"Not valid task for service provider: "
										+ config.getProviderName() + " for:" + pn));
				return false;
			}
		}
		Class st = ((NetSignature) task.getProcessSignature()).getServiceType();

		if (publishedServiceTypes == null) {
			servicetask.getContext().reportException(
					new ExertionException("No published interfaces defined by: "+ getProviderName()));
			return false;
		} else {
			for (int i = 0; i < publishedServiceTypes.length; i++) {
				if (publishedServiceTypes[i] == st) {
					return true;
				}
			}
		}
		servicetask.getContext().reportException(
			new ExertionException("Not valid task for published service types: \n"
								  + Arrays.toString(publishedServiceTypes)
								  + "\nwith Signature: \n"
								  + servicetask.getProcessSignature()));
		return false;
	}

	private boolean isAuthorized(NetTask task) {
		Set principals = task.getSubject().getPrincipals();
		Iterator iterator = principals.iterator();
		Principal principal;
		while (iterator.hasNext()) {
			principal = (Principal) iterator.next();
			if (principal instanceof SorcerPrincipal) 
				return true;
		}
		return false;
	}

	protected void notify(Exertion task, int notificationType, String message) {
		if (!notifying)
			return;
		logger.info(getClass().getName() + "::notify() START message:"
				+ message);

		try {
			MsgRef mr;
			SorcerNotifierProtocol notifier = Accessor.get().getService(null, SorcerNotifierProtocol.class);

			mr = new MsgRef(task.getId(), notificationType,
					config.getProviderName(), message,
					((ServiceExertion) task).getSessionId());
			// Util.debug(this, "::notify() RUNTIME SESSION ID:" +
			// task.getRuntimeSessionID());
			RemoteEvent re = new RemoteEvent(mr, eventID++, seqNum++, null);
			logger.info(getClass().getName() + "::notify() END.");
			notifier.notify(re);
		} catch (RemoteException e) {
            logger.warn("Problem notifying", e);
        }
    }

	public void notifyException(Exertion task, String message, Exception e,
			boolean fullStackTrace) {

		if (message == null && e == null)
			message = "NO MESSAGE OR EXCEPTION PASSED";
		else if (message == null) {
			if (fullStackTrace)
				message = SorcerUtil.stackTraceToString(e);
			else
				message = e.getMessage();
		} else {
			if (fullStackTrace)
				message = message + " " + SorcerUtil.stackTraceToString(e);
			else
				message = message + " " + e.getMessage();
		}

		notify(task, NOTIFY_EXCEPTION, message);
	}

	public void notifyException(Exertion task, String message, Exception e) {
		notifyException(task, message, e, false);
	}

	public void notifyExceptionWithStackTrace(Exertion task, Exception e) {
		notifyException(task, null, e, true);
	}

	public void notifyException(Exertion task, Exception e) {
		notifyException(task, null, e, false);
	}

	public void notifyInformation(Exertion task, String message) {
		notify(task, NOTIFY_INFORMATION, message);
	}

	/*
	 * public void notifyFailure(ServiceTask task, Exception e, boolean
	 * fullStackTrace)throws RemoteException{ String message = null;
	 * if(fullStackTrace) message = Debug.stackTraceToString(e); else message =
	 * Debug.stackTraceToArray(e)[0];
	 * 
	 * notify(task, NOTIFY_WARNING, message); }
	 */

	public void notifyFailure(Exertion task, Exception e) {
		notifyFailure(task, e.getMessage());
	}

	public void notifyFailure(Exertion task, String message) {
		notify(task, NOTIFY_FAILURE, message);
	}

	public void notifyWarning(Exertion task, String message) {
		notify(task, NOTIFY_WARNING, message);
	}

	// task/job monitoring API
	public void stop(Uuid uuid, Subject subject)
			throws UnknownExertionException, AccessDeniedException {
		synchronized (exertionStateTable) {
			if (exertionStateTable.get(uuid) == null)
				throw new UnknownExertionException(
						" No exertion exists corresponding to "
								+ uuid);

			exertionStateTable.put(uuid, Exec.STOPPED);
		}
	}

	public boolean suspend(Uuid uuid, Subject subject) throws UnknownExertionException {
		synchronized (exertionStateTable) {
			if (exertionStateTable.get(uuid) == null)
				throw new UnknownExertionException(
						" No exertion exists corresponding to "
								+ uuid);

			exertionStateTable.put(getServiceID(uuid), Exec.SUSPENDED);
		}

		return true;
	}

	/**
	 * @return Returns the provider config.
	 */
	public DeploymentConfiguration getProviderConfig() {
		return config;
	}

	/**
	 * @return Returns the provider Jini configuration instance.
	 */
	public Configuration getDeploymentConfig() {
		return config.jiniConfig;
	}

	/**
	 * Set the Jini Configuration for this provider delegate.
	 */
	public void setJiniConfig(Configuration config) {
		getProviderConfig().jiniConfig = config;
	}

	/**
	 * The configuration class for SORCER providers. This configuration collects
	 * the configuration settings for all SORCER service providers. It uses the
	 * provider properties file and/or Jini configuration file. The global
	 * environment properties are copied from this configuration to the
	 * {@link Sorcer} properties.
	 */
	public class DeploymentConfiguration {

		/** Properties found in provider.properties file */
		protected Properties props = new Properties();

		/** Jini Configuration */
		protected Configuration jiniConfig;

		/** Our data directory */
		protected String dataDir = null;

		/** Our data directory size in bytes */
		protected long dataLimit = 0;

		/**
		 * initializes this config object (loads all information).
		 */
		public void init(boolean exitOnEmptyName, String propsFilename) {
			// load configuration from a provider properties file
			logger.info("propsFilename = " + propsFilename);
			if (propsFilename != null && propsFilename.length() > 0)
				loadConfiguration(propsFilename);
			// load configuration as defined in provider Jini configuration file
			// or as defined in SBP in relevant opstrings
			loadJiniConfiguration(jiniConfig);
			checkProviderName(exitOnEmptyName);
			fillInProviderHost();
			logger.info("*** provider deployment configuration: \n"
					+ GenericUtil.getPropertiesString(props));
		}

		public Configuration getProviderConfiguration() {
			return jiniConfig;
		}

		private void checkProviderName(boolean exitOnEmptyName) {
			String str;
			String providerName;

			// set provider name if defined in provider's properties
			str = getProperty(P_PROVIDER_NAME);
			if (str != null) {
				providerName = str.trim();
				if (!str.equals(providerName))
					props.setProperty(P_PROVIDER_NAME, providerName);
			} else {
				if (exitOnEmptyName) {
					logger.error("Provider HALTED: its name not defined in the provider config file");
					try {
						provider.destroy();
					} catch (Exception e) {
						logger.warn("Problem trying to destroy the provider due to empty name in the provider config file");
					}
                }
			}
		}

		private void fillInProviderHost() {
			String hostname = null, hostaddress = null;
			try {
				hostname = SorcerEnv.getLocalHost().getHostName();
				if (hostname == null) {
					logger.warn("Could not aquire hostname");
					hostname = "[unknown]";
				} else {
					hostaddress = SorcerEnv.getLocalHost().getHostAddress();
				}
			} catch (Throwable t) {
				// Can be ignored.
			}

			props.put(P_PROVIDR_HOST, hostname);
			props.put(P_PROVIDR_ADDRESS, hostaddress);
		}

		private void extractDataDir() {
			try {
				dataDir = new File(".").getCanonicalPath() + File.separatorChar;
			} catch (IOException e) {
				e.printStackTrace();
			}
			String rootDir = Sorcer.getProperty(DOC_ROOT_DIR);
			String appDir = Sorcer.getProperty(P_DATA_DIR);

			if (rootDir == null || appDir == null)
				return;

			rootDir = rootDir.replace('/', File.separatorChar);
			appDir = appDir.replace('/', File.separatorChar);

			if (!rootDir.endsWith(File.separator)) {
				rootDir += File.separator;
			}

			if (!appDir.endsWith(File.separator)) {
				appDir += File.separator;
			}

			dataDir = rootDir + appDir;
		}

		/**
		 * @return the directory where this provider should store its local
		 *         data.
		 */
		public String getDataDir() {
			if (dataDir == null)
				extractDataDir();

			return dataDir;
		}

		/**
		 * @return the directory where this provider should store its local
		 *         data.
		 */
		public long getDataLimit() {
			if (dataLimit == 0) {
				long limit = Long.parseLong(Sorcer.getProperty(P_DATA_LIMIT));
				dataLimit = limit;
			}
			return dataLimit;
		}

		/**
		 * Sets the provider name. Can be called manually if needed.
		 * 
		 * @param name
		 */
		public void setProviderName(String name) {
			props.setProperty(P_PROVIDER_NAME, name);
		}

		/**
		 * Sets a configuration property.
		 * 
		 * @param key
		 *            they key to set (usualy starts with provider.)
		 * @param value
		 *            the value to set to.
		 */
		public void setProperty(String key, String value) {
			props.setProperty(key, value);
		}

		/**
		 * Return a name of the provider. The name may be specified in this
		 * provider's properties file.
		 * 
		 * 
		 * @return the name of the provider
		 */
		public String getProviderName() {
			return getProperty(P_PROVIDER_NAME);
		}

		/**
		 * Return a file name of the provider's icon. The name may be specified
		 * in this provider's properties file.
		 * 
		 * @return the name of the provider
		 */
		public String getIconName() {
			return getProperty(P_ICON_NAME);
		}

		/**
		 * @return the host name for this provider
		 */
		public String getProviderHostName() {
			return getProperty(P_PROVIDR_HOST);
		}

		/**
		 * @return the host address for this provider
		 */
		public String getProviderHostAddress() {
			return getProperty(P_PROVIDR_ADDRESS);
		}

		/**
		 * Loads provider properties from a <code>filename</code> file. By
		 * default a provider loads its properties from
		 * <code>provider.properties</code> file located in the provider's
		 * package. Also, a provider properties file name can be specified as a
		 * variable <code>providerProperties</code> in a Jini configuration file
		 * for a SORCER provider. In this case the provider loads properties
		 * from the specified <code>providerProperties</code> file. Properties
		 * are available from the instance field <code>props</code> field and
		 * accessible calling the <code> getProperty(String)</code> method.
		 * 
		 * @param filename
		 *            the properties file name
		 * @see #getProperty
		 */
		public void loadConfiguration(String filename) {
			filename = filename.replace("\\", "/");
			
			try {
				// check the class resource
				InputStream is = null;
				Path filePath = Paths.get("configs").resolve(filename);
				if (!filePath.isAbsolute()) {
					String name = filePath.toString().replace("\\", "/");
					logger.info("Try to load configuration: [{}] {}", System.getProperty(JavaSystemProperties.USER_DIR), name);
					ClassLoader resourceLoader = Thread.currentThread().getContextClassLoader();
					URL resourceURL = resourceLoader.getResource(name);
					if (resourceURL != null) {
						logger.info("Loaded from " + resourceURL.toExternalForm());
						is = resourceURL.openStream();
						logger.info("* Loading properties using: " + is);
					} else {
						logger.info("Try to load configuration: [{}] {}", System.getProperty(JavaSystemProperties.USER_DIR), filename);
						resourceURL = resourceLoader.getResource(filename);
						if (resourceURL != null) {
							logger.info("Loaded from " + resourceURL.toExternalForm());
							is = resourceURL.openStream();
							logger.info("* Loading properties using: " + is);
						}
					}
				}
				// next check local resource
				if (is == null) {
					if (filePath.toFile().exists())
						is = new FileInputStream(filePath.toFile());
					else if (Paths.get(filename).toFile().exists())
						is = new FileInputStream(filePath.toFile());
					else {
						logger.warn("Could not load configuration from: " + filename +
								"\nchecked in resources: " + filePath.toString() + ", and files: " + filePath.toFile().getAbsolutePath()
								+ ", and: " + Paths.get(filename).toFile().getAbsolutePath());
						return;
					}
				}

                String expandingEnv = null;
                try {
                    if (jiniConfig != null)
                        expandingEnv = (String) jiniConfig.getEntry(ServiceProvider.COMPONENT,
                                                                    "expandingEnv",
                                                                    String.class);
                } catch (ConfigurationException e) {
                    expandingEnv = null;
                }
                if (expandingEnv != null) {
                    props = Sorcer.loadProperties(expandingEnv, filename);
                } else {
                    props = Sorcer.loadProperties(is);
                    is.close();
                    // copy loaded provider's properties to global Env
                    // properties
                    Sorcer.updateFromProperties(props);
                }
//					logger.debug("*** loaded provider properties: /configs/" + filename + ":\n"
//							+ GenericUtil.getPropertiesString(props));
            } catch (Exception ex) {
                logger.warn("Not able to load provider's properties: " + filename, ex);
            }
        }

        public Properties getProviderProperties() {
			return props;
		}

		/**
		 * Returns a value of a comma separated property as defined in. If the
		 * property value is not defined for the delegate's provider then the
		 * equivalent SORCR environment value value is returned.
		 * {@link SorcerConstants}.
		 * 
		 * @param key
		 *            a property (attribute)
		 * @return a property value
		 */
		public String getProperty(String key) {
			return props.getProperty(key);
		}

		public String getProperty(String property, String defaultValue) {
			String prop = getProperty(property);
			if (prop == null)
				return defaultValue;
			return prop;
		}

		public void updateFromProperties() {
			Sorcer.updateFromProperties(props);
		}

		/**
		 * Loads the provider deployment configuration. The properties can be
		 * accessed calling getProperty() methods to obtain properties of this
		 * service provider. Also, the SORCER environment properties are updated
		 * by corresponding properties found in the provider's configuration and
		 * in the JVM system properties.
		 */
		private void loadJiniConfiguration(Configuration config) {
			String val = null;

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.COMPONENT,
						J_PROVIDER_NAME, String.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				setProviderName(val);

			String nameSuffixed = "";
			boolean globalNameSuffixed = Sorcer.nameSuffixed();
			try {
				nameSuffixed = (String) config.getEntry(
						ServiceProvider.COMPONENT, "nameSuffixed", String.class,
						"");
			} catch (ConfigurationException e1) {
				nameSuffixed = "";
			}
			// check for the specified suffix by the user
			String suffix = Sorcer.getNameSuffix();

			String suffixedName = null;
			if (nameSuffixed.length() == 0) {
				if (suffix == null)
					suffixedName = Sorcer.getSuffixedName(val);
				else
					suffixedName = val + "-" + suffix;
			} else if (!nameSuffixed.equals("true")
					&& !nameSuffixed.equals("false")) {
				suffixedName = val + "-" + nameSuffixed;
				nameSuffixed = "true";
			}
			// add provider name and SorcerServiceType entries
			// nameSuffixed not defined by this provider but in sorcer.env
			if (nameSuffixed.length() == 0 && globalNameSuffixed) {
				setProviderName(suffixedName);
			} else if (nameSuffixed.equals("true")) {
				setProviderName(suffixedName);
			}

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.COMPONENT,
						J_DESCRIPTION, String.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_DESCRIPTION, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.COMPONENT,
						J_LOCATION, String.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_LOCATION, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.COMPONENT,
						J_TEMPLATE_MATCH, String.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_TEMPLATE_MATCH, val);

			try {
				val = ""
						+ (Boolean) jiniConfig.getEntry(
								ServiceProvider.COMPONENT,
								J_SERVICE_ID_PERSISTENT, boolean.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_SERVICE_ID_PERSISTENT, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.COMPONENT,
						J_DATA_LIMIT, String.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_DATA_LIMIT, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.COMPONENT,
						J_PORTAL_HOST, String.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_PORTAL_HOST, val);

			try {
				val = ""
						+ jiniConfig.getEntry(
								ServiceProvider.COMPONENT, J_PORTAL_PORT,
								int.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_PORTAL_PORT, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.COMPONENT,
						J_WEBSTER_INTERFACE, String.class);
			} catch (ConfigurationException e5) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_WEBSTER_INTERFACE, val);

			try {
				val = ""
						+ jiniConfig.getEntry(
								ServiceProvider.COMPONENT, J_WEBSTER_PORT,
								int.class);
			} catch (ConfigurationException e4) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_WEBSTER_PORT, val);

			try {
				val = SorcerUtil.arrayToCSV((String[]) jiniConfig.getEntry(
						ServiceProvider.COMPONENT, J_GROUPS, String[].class));
			} catch (ConfigurationException e3) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_GROUPS, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.COMPONENT,
						J_SPACE_GROUP, String.class);
			} catch (ConfigurationException e2) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_SPACE_GROUP, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.COMPONENT,
						J_SPACE_NAME, String.class);
			} catch (ConfigurationException e2) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_SPACE_NAME, val);

			try {
				val = SorcerUtil.arrayToCSV((String[]) jiniConfig.getEntry(
						ServiceProvider.COMPONENT, J_LOCATORS, String[].class));
			} catch (ConfigurationException e) {
				val = null;
			}

			// if not defined in provider deployment file use from sorcer.env
			if ((val == null) || (val.length() == 0))
				val = Sorcer.getProperty(P_LOCATORS);

			if ((val != null) && (val.length() > 0))
				props.put(P_LOCATORS, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.COMPONENT,
						J_ICON_NAME, String.class);
			} catch (ConfigurationException e5) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_ICON_NAME, val);

			// update and log Sorcer properties
			Sorcer.updateFromProperties(props);
			Sorcer.updateFromProperties(System.getProperties());
			Properties envProps = Sorcer.getEnvProperties();
			logger.debug("All SORCER updated properties: " + envProps);
		}
	}

	public String getProviderName() {
		return config.getProviderName();
	}

	public Provider getProvider() {
		return provider;
	}

	public boolean mutualExlusion() {
		return mutualExclusion;
	}

	public void mutualExlusion(boolean mutualExlusion) {
		this.mutualExclusion = mutualExlusion;
	}

	public boolean isSpaceTransactional() {
		return workerTransactional;
	}

	public TrustVerifier getProxyVerifier() {
		return new ProxyVerifier(getProxy(), this.getProviderUuid());
	}

	/**
	 * Returns an object that implements whatever administration interfaces are
	 * appropriate for the particular service.
	 * 
	 * @return an object that implements whatever administration interfaces are
	 *         appropriate for the particular service.
	 */
	public Object getAdmin() {
		return adminProxy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.core.provider.OuterProxy#setAdmin(java.lang.Object)
	 */
	public void setAdmin(Object proxy) {
		//		adminProxy = proxy;
	}

	/**
	 * Unexports the services of this provider appropriately.
	 * 
	 * @param force
	 *            terminate in progress calls if necessary
	 * @return true if unexport succeeds
	 * @throws NoSuchObjectException 
	 */
	public boolean unexport(boolean force) throws NoSuchObjectException {
		boolean success = true;
		if (outerExporter != null) {
			exports.remove(outerProxy);
			success &= outerExporter.unexport(force);
			outerExporter = null;
			outerProxy = null;
		}

		if (partnerExporter != null && outerProxy !=null) {
			exports.remove(innerProxy);
			success &= partnerExporter.unexport(force);
			outerProxy = null;
			partnerExporter = null;
		}

		if (serviceBeans != null && serviceBeans.length > 0) {
			for (int i = 0; i < serviceBeans.length; i++) {
				exports.remove(serviceBeans[i]);
			}
		}
		return success;
	}

	public Object createAdmin() {
		if (adminProxy != null)
			return adminProxy;
		try {
            adminProxy = ProviderProxy.wrapAdminProxy(outerProxy, getAdminProviderUuid());
        } catch (Exception e) {
            logger.warn("No admin proxy created by: {}", provider, e);
        }
        return adminProxy;
    }

    public Object getAdminProxy() {
        try {
            providerProxy = ProviderProxy.wrapServiceProxy(adminProxy,
                                                           getProviderUuid(),
                                                           adminProxy,
                                                           Administrable.class);
        } catch (Exception e) {
			logger.warn("No admin proxy created by: {}", provider, e);
		}
		return providerProxy;
	}

	/**
	 * Returns a proxy object for this provider. If the smart proxy is alocated
	 * then returns a non exported object to be registerd with loookup services.
	 * However, if a smart proxy is defined then the provider's proxy is set as
	 * its inner proxy. Otherwise the {@link Remote} outer proxy of this provider
	 * is returned.
	 * 
	 * @return a proxy, or null
	 * @see sorcer.core.provider.ServiceProvider#getProxy()
	 */
	public Object getProxy() {
		if (providerProxy != null)
			return providerProxy;
		try {
			if (smartProxy == null) {
				if (innerProxy != null && partner == null
						&& outerProxy instanceof Partnership) {
					((Partnership) outerProxy).setInner(innerProxy);
					((Partnership) outerProxy).setAdmin(adminProxy);
				} else if (partner != null && partner instanceof Partnership) {
					((Partnership) partner).setInner(innerProxy);
					((Partnership) partner).setAdmin(adminProxy);
				}
                providerProxy = ProviderProxy.wrapServiceProxy(outerProxy,
                                                               getProviderUuid(),
                                                               adminProxy,
                                                               publishedServiceTypes);
                return providerProxy;
            } else if (smartProxy instanceof Partnership) {
                ((Partnership) smartProxy).setInner(outerProxy);
                ((Partnership) smartProxy).setAdmin(adminProxy);
            }
            providerProxy = ProviderProxy.wrapServiceProxy(smartProxy,
                                                           getProviderUuid(),
                                                           adminProxy);
        } catch (ProviderException e) {
            logger.warn("No proxy created by: {}", provider, e);
		}
		return providerProxy;
	}

	/** {@inheritDoc} */
	public Remote getInner() {
		return innerProxy;
	}

	/** {@inheritDoc} */
	public void setInner(Object innerProxy) throws ProviderException {
		if (outerProxy instanceof Partnership)
			((Partnership) outerProxy).setInner(innerProxy);
		else
			throw new ProviderException("wrong inner proxy for this provider");
	}

	/**
	 * Returns the exporter to use to export this server.
	 * <p>
	 * Two ways for a client to expose his service:
	 * <ol>
	 * <li>Directly subclass ServiceProvider in which case, configuration should
	 * provide the following: <br>
	 * <code>exporter = xxx //Object exported will be this object</code><br>
	 * By default BasicJeriExporter is used
	 * 
	 * <li>Expose objects as services <br>
	 * <code>beans = new String[] { ..... }<br>
	 *    proxyName = "xxx.xxx"</code><br>
	 * Provide the proxy name and have a constructor with one argument, which
	 * accepts the exported inner proxy.
	 * </ol>
	 * 
	 * @param config the configuration to use for supplying the exporter
	 */
	private void getExporters(Configuration config) {
		try {
            String exporterInterface = Sorcer.getProperty(P_EXPORTER_INTERFACE);
            try {
                exporterInterface = (String) config.getEntry(ServiceProvider.COMPONENT,
                                                             EXPORTER_INTERFACE,
                                                             String.class,
                                                             SorcerEnv.getLocalHost().getHostAddress());
			} catch (Exception e) {
				// do nothng
			}
			logger.info(">>>>> exporterInterface: {}", exporterInterface);

			int exporterPort;
			String port = Sorcer.getProperty(P_EXPORTER_PORT);
			if (port != null)
				exporterPort = Integer.parseInt(port);
            else
                exporterPort = (Integer) config.getEntry(ServiceProvider.COMPONENT,
                                                         EXPORTER_PORT,
                                                         Integer.class,
                                                         0);
			logger.info(">>>>> exporterPort: {}", exporterPort);

			try {
				// check if not set by the provider
				if (smartProxy == null) {
					// initialize smart proxy
					smartProxy = config.getEntry(ServiceProvider.COMPONENT,
												 SMART_PROXY,
												 Object.class,
												 null);
				}
			} catch (Exception e) {
				logger.warn(">>>>> NO SMART PROXY specified", e);
				smartProxy = null;
			}

			List<Object> allBeans = new ArrayList<>();
            // find it out if service bean instances are available
            Object[] beans = (Object[]) Config.getNonNullEntry(config,
                                                               ServiceProvider.COMPONENT,
                                                               BEANS,
                                                               Object[].class,
                                                               new Object[] {});
			if (beans.length > 0) {
				logger.debug("*** service beans by {}\nfor: {}", getProviderName(), Arrays.toString(beans));
                for (Object bean : beans) {
                    allBeans.add(bean);
                    exports.put(bean, this);
                }
			}

            // find it out if data service bean instances are available
            Object[] dataBeans = (Object[]) Config.getNonNullEntry(config,
                                                                   ServiceProvider.COMPONENT,
                                                                   DATA_BEANS,
                                                                   Object[].class,
                                                                   new Object[] {},
                                                                   getProviderProperties());
            if (dataBeans.length > 0) {
				logger.debug("*** data service beans by {}\nfor: {}", getProviderName(), Arrays.toString(dataBeans));
                for (Object dataBean : dataBeans) {
                    allBeans.add(dataBean);
                    exports.put(dataBean, this);
                }
			}

            // find it out if service classes are available
            Class[] beanClasses = (Class[]) Config.getNonNullEntry(config,
                                                                   ServiceProvider.COMPONENT,
                                                                   BEAN_CLASSES,
                                                                   Class[].class,
                                                                   new Class[] {});
			if (beanClasses.length > 0) {
				logger.debug("*** service bean classes by {} for: \n{}", getProviderName(), Arrays.toString(beanClasses));
                for (Class beanClass : beanClasses)
                    allBeans.add(instantiate(beanClass));
			}

			// find it out if Groovy scripts are available
            String[] scriptlets = (String[]) Config.getNonNullEntry(config,
                                                                    ServiceProvider.COMPONENT,
                                                                    SCRIPTLETS,
                                                                    String[].class,
                                                                    new String[] {});
            if (scriptlets.length > 0) {
				logger.debug("*** service scriptlets by {} for: \n{}", getProviderName(), Arrays.toString(scriptlets));
                for (String scriptlet : scriptlets)
                    allBeans.add(instantiateScriplet(scriptlet));
			}

            exporterFactory = (AbstractExporterFactory) config.getEntry(ServiceProvider.COMPONENT,
                                                                        "exporterFactory",
                                                                        AbstractExporterFactory.class,
                                                                        null);
            if (exporterFactory == null)
                exporterFactory = ExporterFactories.EXPORTER;

			if (allBeans.size() > 0) {
				logger.debug("*** all beans by: {} for: \n{}", getProviderName(), allBeans);
				serviceBeans = allBeans.toArray();
				initServiceBeans(serviceBeans);
                SorcerILFactory ilFactory = new SorcerILFactory(serviceComponents, implClassLoader);
                ilFactory.setRemoteLogging(remoteLogging);
                outerExporter = exporterFactory.get(ilFactory);
                logger.info("{}, {}", outerExporter, ((BasicJeriExporter)outerExporter).getInvocationLayerFactory().getClass().getName());
			} else {
				logger.info("*** NO beans used by {}", getProviderName());
                outerExporter = (Exporter) config.getEntry(ServiceProvider.COMPONENT,
                                                           EXPORTER,
                                                           Exporter.class,
                                                           null);
				if (outerExporter == null) {
                    outerExporter = exporterFactory.get();
				}
                logger.info("current exporter: {}", outerExporter.toString());
                try {
                    partnerExporter = (Exporter) Config.getNonNullEntry(config,
                                                                        ServiceProvider.COMPONENT, SERVER_EXPORTER,
                                                                        Exporter.class);
					if (partnerExporter == null) {
						logger.warn("NO provider inner exporter defined!!!");
					} else {
						logger.debug("your partner exporter: {}", partnerExporter);
					}
				} catch (ConfigurationException e) {
					// do nothing
				}
			}
		} catch (Exception ex) {
			logger.warn("Error getting exporters", ex);
			// ignore missing exporters and use default configurations for exporters
		}
	}

	/**
	 * Use javax.inject.Provider implementations as factory objects
	 */
	private static void callProviders(Object[] serviceBeans) {
		for (int i = 0; i < serviceBeans.length; i++) {
			Object bean = serviceBeans[i];
			if(bean instanceof javax.inject.Provider)
				serviceBeans[i] = ((javax.inject.Provider) bean).get();
		}
	}

	/**
	 * Initializes the map between all the interfaces and the service object
	 * passed via the configuration file.
	 * 
	 * @param serviceBeans
	 *            service objects exposing their interface types
	 */
    private void initServiceBeans(Object[] serviceBeans) {
        if (serviceBeans == null)
            throw new NullPointerException("No service beans defined by: "+ getProviderName());

		callProviders(serviceBeans);
		serviceComponents = new HashMap<>();

		if (serviceComponents.size() == 1) {
			for (Object serviceBean : serviceBeans) {
				Class[] interfaces = serviceBean.getClass().getInterfaces();
				logger.debug("service component interfaces {}", Arrays.toString(interfaces));
				List<Class> exposedInterfaces = new LinkedList<>();
				for (Class publishedType : publishedServiceTypes) {
					if (publishedType.isInstance(serviceBean)) {
						serviceComponents.put(publishedType, serviceBean);
						exposedInterfaces.add(publishedType);
						for (Class iface : publishedType.getInterfaces()) {
							if (!iface.equals(Remote.class) && !iface.equals(Serializable.class)) {
								serviceComponents.put(iface, serviceBean);
								exposedInterfaces.add(iface);
							}
						}
					}
				}
				logger.debug("service component exposed interfaces {}", exposedInterfaces);
			}
		} else {
			for (Class publishedType : publishedServiceTypes) {
				for (Object serviceBean : serviceBeans) {
					if (publishedType.isInstance(serviceBean)) {
						serviceComponents.put(publishedType, serviceBean);
					}
				}
			}
		}
		logger.info("service components: {}", serviceComponents);
	}

	private Object instantiateScriplet(String scripletFilename)
			throws Exception {
		String[] tokens = SorcerUtil.tokenize(scripletFilename, "|");
		Object bean = null;
		Object configurator = null;
		GroovyShell shell = new GroovyShell();
		bean = shell.evaluate(new File(tokens[0]));
		for (int i = 1; i < tokens.length; i++) {
			configurator = shell.evaluate(new File(tokens[i]));
			if ((configurator instanceof Configurator)
					&& (bean instanceof Configurable)) {
				shell.setVariable("configurable", bean);
				bean = ((Configurator) configurator).configure();
			}
		}
		initBean(bean);
		return bean;
	}

	private Object instantiate(Class beanClass) throws Exception {
		return createBean(beanClass);
	}

	private Object instantiate(String serviceBean) throws Exception {
		Class clazz = Class.forName(serviceBean, false, implClassLoader);
		return createBean(clazz);
	}

	private Object createBean(Class beanClass) throws Exception {
		Object bean = beanClass.newInstance();
		initBean(bean);
		return bean;
	}

	private Object initBean(Object serviceBean) {
		try {
			// Configure the bean
			Configurer configurer = new Configurer();
			configurer.preProcess((ServiceProvider)this.getProvider(), serviceBean);
			//
			Method m = serviceBean.getClass().getMethod(
					"init", new Class[] { Provider.class });
			m.invoke(serviceBean, new Object[] { provider });
		} catch (Exception e) {
			logger.info("No 'init' method for this service bean: "
					+ serviceBean.getClass().getName());
		}
		exports.put(serviceBean, this);
		logger.debug(">>>>>>>>>>> exported service bean: \n" + serviceBean
				+ "\n by provider: " + provider);
		return serviceBean;
	}

	/**
	 * Returns a partner service specified in the provider's Jini configuration.
	 * 
	 * @param partnerName
	 *            name of the partner service
	 * @param partnerType
	 *            service type (interface) of the partner service
	 * @throws ExportException
	 */
	private Remote getPartner(String partnerName, Class partnerType)
			throws ExportException {
		// get the partner and its proxy
		// if it is exportable, export it, otherwise discover one
		Remote pp;
		if (partner == null) {
			if (partnerType != null) {
				// Class clazz = null;
				// if (partnerExporter != null) {
				// // get the partner instance
				// try {
				// clazz = Class.forName(partnerType);
				// } catch (ClassNotFoundException e) {
				// try {
				// String codebase = System
				// .getProperty("java.rmi.server.codebase");
				// logger.info(">>>> partner codebase: "
				// + codebase);
				//
				// String[] urlNames = SorcerUtil
				// .tokenize(codebase, " ");
				// URL[] urls = new URL[urlNames.length];
				// for (int i = 0; i < urlNames.length; i++)
				// urls[i] = new URL(urlNames[i]);
				//
				// ClassLoader loader = new URLClassLoader(urls);
				// clazz = Class.forName(partnerType, false,
				// loader);
				// } catch (MalformedURLException e1) {
				// logger.throwing(
				// ServiceAccessor.class.getName(),
				// "getService", e1);
				// clazz = null;
				// } catch (ClassNotFoundException e2) {
				// logger.throwing(
				// ServiceAccessor.class.getName(),
				// "getService", e2);
				// clazz = null;
				// }
				// }
				try {
					partner = (Remote) partnerType.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					logger.error("Could not create partner", e);
				}
				// if partner exported use it as the primary proxy
				if (partner != null) {
					pp = partnerExporter.export(partner);
					if (pp != null) {
						innerProxy = outerProxy;
						outerProxy = pp;
					}
				}
			} else {
				// if partner discovered use it as the inner proxy
				innerProxy = (Remote) Accessor.get().getService(partnerName, partnerType);
			}
		} else {
            // if partner exported use it as the primary proxy
            if (partner != null) {
                if (partnerExporter == null)
                    try {
                        partnerExporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(Sorcer.getLocalHost().getHostAddress(), 0),
                                                                new BasicILFactory());
                    } catch (UnknownHostException e) {
                        throw new ExportException("Could not obtain local address", e);
                    }
				pp = partnerExporter.export(partner);
				if (pp != null) {
					innerProxy = outerProxy;
					outerProxy = pp;
				} else
					// use partner as this provider's inner proxy
					innerProxy = partner;
			}
			logger.info(">>>>> got innerProxy: {}\nfor: {}\nusing exporter: {}", innerProxy, partner, partnerExporter);
		}
		return partner;
	}

	public static String[] toArray(String arg) {
		StringTokenizer token = new StringTokenizer(arg, " ,;");
		String[] array = new String[token.countTokens()];
		int i = 0;
		while (token.hasMoreTokens()) {
			array[i] = token.nextToken();
			i++;
		}
		return (array);
	}

	public Object getSmartProxy() {
		return smartProxy;
	}

	public Remote getPartner() {
		return partner;

	}

	public Object getProviderProxy() {
		return providerProxy;
	}

	public boolean isSpaceSecurityEnabled() {
		return spaceSecurityEnabled;
	}

	public Map getServiceComponents() {
		return serviceComponents;
	}

	public void setServiceComponents(Map serviceComponents) {
		this.serviceComponents = serviceComponents;
	}
	
	public void addBean(Object bean) {
		Class[] interfazes = bean.getClass().getInterfaces();
		for (int j = 0; j < interfazes.length; j++) {
			// if (interfaze[j].getDeclaredMethods().length != 0)
			// allow marker interfaces to be added
			serviceComponents.put(interfazes[j], bean);
		}
	}
	
	public Object getBean(Class<?> serviceType) {
		if (serviceComponents == null || serviceComponents.size() == 0)
			return null;
		// direct beans
		Object serviceBean = serviceComponents.get(serviceType);
        if (serviceBean != null ) 
        	return serviceBean;

        // check assignable beans
        for (Object bean : serviceComponents.values()) {
            if (bean.getClass().isAssignableFrom(serviceType)) 
            	return bean;
        }
		return null;
	}
	
	public Logger getContextLogger() {
		return contextLogger;
	}

	public Logger getProviderLogger() {
		return providerLogger;
	}

	public Logger getRemoteLogger() {
		return remoteLogger;
	}

	public String getHostAddress() {
		if (hostAddress == null)
			try {
				hostAddress = SorcerEnv.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		return hostAddress;
	}

	public String getHostName() {
		if (hostName == null) {
			try {
				hostName = SorcerEnv.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		return hostName;
	}

	public boolean isMonitorable() {
		return monitorable;
	}

	void spaceEnabled(boolean enabled) {
		spaceEnabled = enabled;
	}

	public boolean spaceEnabled() {
		return spaceEnabled;
	}

	public boolean isRemoteLogging() {
		return remoteLogging;
	}

	public List<ExecutorService> getSpaceHandlingPools() {
		return spaceHandlingPools;
	}

	void shutdownAndAwaitTermination(ExecutorService pool) {
		pool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(3, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * <p>
	 * Returns a context manager of this delegate as defined in the Jini
	 * configuration file.
	 * </p>
	 * 
	 * @return the contextManager
	 */
	public ContextManagement getContextManager() {
		return contextManager;
	}

	public Class[] getPublishedServiceTypes() {
		return publishedServiceTypes;
	}

	public final static String EXPORTER = "exporter";

	public final static String BEANS = "beans";

	public final static String DATA_BEANS = "dataBeans";

	public final static String SCRIPTLETS = "scriptlets";

	public final static String BEAN_CLASSES = "beanClasses";

	public final static String CONTEXT_MANAGER = "contextManager";

	public final static String SMART_PROXY = "smartProxy";

	public final static String SERVER = "server";

	public final static String SERVER_TYPE = "serverType";

	public final static String REMOTE_LOGGING = "remoteLogging";

	public final static String REMOTE_LOGGER_MANAGER_NAME = "remoteLoggerManagerName";

	public final static String REMOTE_LOGGER_NAME = "remoteLoggerName";

	public final static String REMOTE_LOGGER_LEVEL = "remoteLoggerLevel";

	public final static String REMOTE_CONTEXT_LOGGING = "remoteContextLogging";

	public final static String REMOTE_PROVIDER_LOGGING = "remoteProviderLogging";

	public final static String PROVIDER_MONITORING = "monitorEnabled";

	public final static String PROVIDER_NOTIFYING = "notifierEnabled";

	public final static String SERVER_NAME = "serverName";

	public final static String SERVER_EXPORTER = "serverExporter";

	public final static String EXPORTER_INTERFACE = "exporterInterface";

	public final static String EXPORTER_PORT = "exporterPort";

	public final static int KEEP_ALIVE_TIME = 1000;

	public static final String DISCOVERY_ENABLED = "discoveryEnabled";

	public static final String SPACE_ENABLED = "spaceEnabled";

	public static final String SPACE_TAKER_DELAY = "spaceTakerDelay";

	public static final String SPACE_READINESS = "spaceReadiness";

	public static final String MUTUAL_EXCLUSION = "mutualExclusion";

	public static final String SPACE_SECURITY_ENABLED = "spaceSecurityEnabled";

	public static final String WORKER_TRANSACTIONAL = "workerTransactional";

	public static final String WORKER_COUNT = "workerCount";

	public static final String WORKER_PER_INTERFACE_COUNT = "workerPerInterfaceCount";

	public static final String SPACE_WORKER_QUEUE_SIZE = "workerQueueSize";

	public static final String MAX_WORKER_POOL_SIZE = "maxWorkerPoolSize";

	public static final String WORKER_TRANSACTION_LEASE_TIME = "workerTransactionLeaseTime";

	public static final String SPACE_TIMEOUT = "workerTimeout";

	public static final String INTERFACE_ONLY = "matchInterfaceOnly";

}
