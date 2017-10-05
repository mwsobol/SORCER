/*
s * Copyright 2009 the original author or authors.
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
import com.sun.jini.start.LifeCycle;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.core.constraint.MethodConstraints;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.export.ProxyAccessor;
import net.jini.id.ReferentUuid;
import net.jini.id.Uuid;
import net.jini.lookup.JoinManager;
import net.jini.lookup.ServiceIDListener;
import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.MainUI;
import net.jini.lookup.ui.factory.JFrameFactory;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import net.jini.security.proxytrust.TrustEquivalence;
import org.rioproject.admin.ServiceActivityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import sorcer.core.SorcerConstants;
import sorcer.core.analytics.MethodAnalytics;
import sorcer.core.analytics.SystemAnalytics;
import sorcer.core.context.ControlContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetTask;
import sorcer.core.proxy.Outer;
import sorcer.core.proxy.Partner;
import sorcer.core.proxy.Partnership;
import sorcer.core.signature.ServiceSignature;
import sorcer.scratch.ScratchManager;
import sorcer.scratch.ScratchManagerSupport;
import sorcer.service.*;
import sorcer.service.Signature;
import sorcer.service.SignatureException;
import sorcer.serviceui.UIComponentFactory;
import sorcer.serviceui.UIDescriptorFactory;
import sorcer.serviceui.UIFrameFactory;
import sorcer.util.*;
import sorcer.util.url.sos.SdbURLStreamHandlerFactory;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static sorcer.util.StringUtils.tName;

/**
 * The ServiceProvider class is a type of {@link Provider} with dependency
 * injection defined by a Jini 2 configuration, proxy management, and own
 * service discovery management for registering its proxies. This class can
 * be inherited by custom service providers or used as a container for service
 * beans - objects that implement Java interfaces to be exposed as service types.
 * Service beans are declared in a provider configuration file as an array of
 * <i>dataBeans</i>. A bean can be declared with a static method, e.g.,
 * <code>MyServiceBean.buildMyBean($data)</code>, where <code>$data</code>
 * refers to provider properties passed as argument of the static method
 *  <code>buildMyBean</code> of the class <code>MyServiceBean</code>.
 * <p>
 * In the simplest case, the provider exports and registers its own (outer) proxy with
 * the primary methods {@code Service.exec(Arg[])} and service federation request
 * {@code Exerter.exert(Mogram, Transaction, Arg[])}. The functionality of an
 * outer proxy can be extended by its inner server functionality with its Remote
 * inner proxy. In this case, the outer proxies have to implement {@link sorcer.core.proxy.Outer}
 * interface and each outer proxy is registered with the inner proxy allocated
 * with {@link Outer#getInner} invoked on the outer proxy. Obviously an outer
 * proxy can implement own interfaces with the help of its embedded inner proxy
 * that in turn can consist of multiple own inner proxies as needed. This class
 * implements the {@link sorcer.core.proxy.Outer} interface, so can extend its functionality by
 * inner proxying.
 * <p>
 * A smart proxy can be defined in the provider's Jini configuration by the
 * <code>smartProxy</code> entry. This proxy does not represent directly any
 * exported server, it is registered with lookup services as is. However, the
 * smart proxy (implementing {@link sorcer.core.proxy.Outer} interface) can extend its
 * functionality by setting the providers outer proxy as its inner proxy. Thus,
 * smart proxies that implement{@link sorcer.core.proxy.Outer}, called <i>semismart</i> contain
 * this provider's proxy as its inner proxy. Smart proxies that do not extend
 * its functionality via its inner proxy are called fat proxies. Fat proxies do
 * not make remote calls back to their providers and the providers just maintain
 * lookup service registrations.
 * <p>
 * An inner or outer proxy can be a surrogate of a service partner defined by
 * this provider using <code>server</code> configuration entry or two realted
 * args: <code>serverType</code> and <code>serverName</code>. If the entry
 * <code>server</code> is defined and its exporter is defined by the entry
 * <code>serverExporter</code> then this provider will use the server's proxy as
 * the outer (primary proxy) and itself as the inner proxy. However if the
 * exporter is not defined then the provider's proxy is primary (outer) and the
 * server's proxy is the inner one.
 * <p>
 * On the other hand, if the <code>server</code> entry is not defined but at
 * least <code>serverType</code> is defined then the instance of server is
 * created and exported if the entry <code>serverExporter</code> is given. The
 * exported server proxy becomes the primary provider's proxy. However, if no
 * exporter is defined then server proxy becomes the inner proxy of this
 * provider's proxy (outer proxy). Thus, exported servers use outer proxies
 * while not exported user inner proxies of this provider. In this context, a
 * smart proxy implementing {@link sorcer.core.proxy.Outer} interface can get the outer proxy and
 * its inner proxy composed in either direction provider/server proxy
 * relationship.
 * <p>
 * A service method is a method returning {@link sorcer.core.context.ServiceContext} and having a
 * single parameter as {@link sorcer.core.context.ServiceContext}. Service beans are components that
 * implement interfaces in terms of service methods only. A list of service
 * beans can be specified in a provider's Jini configuration as the
 * <code>beans</code> entry. In this case a proxy implementing all interfaces
 * implemented by service beans are dynamically created and registered with
 * lookup services. Multiple SORCER servers can be deployed within a single
 * {@link sorcer.core.provider.ServiceProvider} as its own service beans.
 *
 * @see sorcer.core.provider.Provider
 * @see net.jini.lookup.ServiceIDListener
 * @see ReferentUuid
 * @see sorcer.core.provider.AdministratableProvider
 * @see net.jini.export.ProxyAccessor
 * @see net.jini.security.proxytrust.ServerProxyTrust
 * @see net.jini.core.constraint.RemoteMethodControl
 * @see com.sun.jini.start.LifeCycle
 * @see Partner
 * @see sorcer.core.proxy.Partnership
 * @see sorcer.core.SorcerConstants
 *
 * @author Mike Sobolewski
 */
public class ServiceProvider implements Identifiable, Provider, ServiceIDListener,
		ReferentUuid, ProxyAccessor, ServerProxyTrust, RemoteMethodControl, ServiceActivityProvider,
		LifeCycle, Partner, Partnership, SorcerConstants, AdministratableProvider, ScratchManager {
	// RemoteMethodControl is needed to enable Proxy Constraints

	/** Logger and configuration component name for service provider. */
	public static final String COMPONENT = ServiceProvider.class.getName();

	/** Logger for logging information about this instance */
	protected static final Logger logger = LoggerFactory.getLogger(COMPONENT);

	static {
		try {
			URL.setURLStreamHandlerFactory(new SdbURLStreamHandlerFactory());
		} catch (Error e) {
			logger.info(e.getMessage());
		}
	}

	private ScratchManager scratchManager = new ScratchManagerSupport();
	protected ProviderDelegate delegate;

	static final String DEFAULT_PROVIDER_PROPERTY = "provider.properties";

	int loopCount = 0;

	/** The login context, for logging out */
	private LoginContext loginContext;

	/** The provider's JoinManager. */
	protected JoinManager joinManager;

	private LookupDiscoveryManager ldmgr;

	// the current number of shared providers
	protected static int tally = 0;
	// the size of this service node
	protected static int size = 0;

	/** Object to notify when this service is destroyed, or null. */
	private LifeCycle lifeCycle;

	// all providers in the same shared JVM
	private static Collection<ServiceProvider> providers = new CopyOnWriteArraySet<>();

	private ClassLoader serviceClassLoader;

	private String[] accessorGroups = DiscoveryGroupManagement.ALL_GROUPS;

	private final AtomicBoolean running = new AtomicBoolean(true);

	protected Map<Uuid, ServiceSession> sessions;

	protected ScheduledExecutorService scheduler;

	// a service bean used for local execution in this container
	protected Object bean;

	/** MBean for JMX access*/
	private ProviderAdmin providerAdmin;

	public ServiceProvider() {
		providers.add(this);
		delegate = new ProviderDelegate();
		delegate.provider = this;
		sessions = new ConcurrentHashMap<Uuid, ServiceSession>();
		logger.info("\n\t<init> providers.size() = " + providers.size()
				+ "\n\t<init> providers = " + providers
				+ "\n\t<init> this.getName = " + this.getName());
		ShutdownHook shutdownHook =  new ShutdownHook(this);
		shutdownHook.setDaemon(true);
		Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Required constructor for Jini 2 NonActivatableServiceDescriptors
     *
     * @param args config args
     * @param lifeCycle lifecycle management
     * @throws Exception
     */
    public ServiceProvider(String[] args, LifeCycle lifeCycle) throws Exception {
        this();
        // count initialized shared providers
        tally = tally + 1;
        size = tally;
        // load Sorcer environment properties via static initializer
        Sorcer.getProperties();
        serviceClassLoader = Thread.currentThread().getContextClassLoader();
        final Configuration config = ConfigurationProvider.getInstance(args, serviceClassLoader);
        Accessor.create(config);
        delegate.setJiniConfig(config);
        // inspect class loader tree
        if(logger.isTraceEnabled())
            com.sun.jini.start.ClassLoaderUtil.displayContextClassLoaderTree();
        // System.out.println("service provider class loader: " +
        // serviceClassLoader);
		String providerProperties =
				(String) config.getEntry(COMPONENT, "propertiesFile", String.class, "");
	    // setup injections by subclasses of this class
		providerSetup();
		// configure the provider's delegate
        delegate.getProviderConfig().init(true, providerProperties);
        ((ScratchManagerSupport)scratchManager).setProperties(getProviderProperties());
        delegate.configure(config);
        providerAdmin = new ProviderAdmin(this);
        providerAdmin.register();
        // decide if thread management is needed for ExertionDispatcher
        setupThreadManager();
        init(args, lifeCycle);
        logger.info("<init> (String[], LifeCycle); name = {}", this.getName());
    }

    // this is only used to instantiate provider impl objects and use their
    // methods
    public ServiceProvider(String providerPropertiesFile) {
        this();
        delegate.getProviderConfig().loadConfiguration(providerPropertiesFile);
        ((ScratchManagerSupport)scratchManager).setProperties(getProviderProperties());
	}

	/**
	 * Subclasses inject problematically configurable entities,
	 * for example proxies for this provider.
	 */
	protected void providerSetup() {
		// optional programmatic setup by subclassing provider
	}

	protected void setScratchManager(final ScratchManager scratchManager) {
        if(scratchManager!=null) {
            this.scratchManager = scratchManager;
            logger.debug("Set ScratchManager with {}", this.scratchManager.getClass().getName());
        } else {
            logger.warn("Attempt to set null ScratchManager avoided");
        }
    }

    public ScratchManager getScratchManager() {
        return scratchManager;
    }

	// Implement ServerProxyTrust
	/**
	 * @throws UnsupportedOperationException
	 *             if the server proxy does not implement both
	 *             {@link net.jini.core.constraint.RemoteMethodControl}and {@linkTrustEquivalence}
	 */

	public Permission[] getGrants(Class<?> cl, Principal[] principals) {
		return null;
	}

	public void grant(Class<?> cl, Principal[] principals, Permission[] permissions) {
	}

	public boolean grantSupported() {
		return false;
	}

	public TrustVerifier getProxyVerifier() {
		return delegate.getProxyVerifier();
	}

	@Override
	public MethodConstraints getConstraints() {
		//return ((RemoteMethodControl) delegate.getProxy()).getConstraints();
		return null;
	}


	@Override
	public RemoteMethodControl setConstraints(MethodConstraints constraints) {
		//return (RemoteMethodControl)delegate.getProxy();
		return (RemoteMethodControl) delegate.getProxy();
	}

	/**
	 * Returns an object that implements whatever administration interfaces are
	 * appropriate for the particular service.
	 *
	 * @return an object that implements whatever administration interfaces are
	 *         appropriate for the particular service.
	 */
	@Override
	public Object getAdmin() {
		return delegate.getAdmin();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see sorcer.core.provider.OuterProxy#setAdmin(java.lang.Object)
	 */
	public void setAdmin(Object proxy) {
		delegate.setAdmin(proxy);
	}

	/**
	 * Get the current attribute sets for the service.
	 *
	 * @return the current attribute sets for the service
	 */
	public Entry[] getLookupAttributes() {
		return joinManager.getAttributes();
	}

	/**
	 * Add attribute sets for the service. The resulting setValue will be used for
	 * all future joins. The attribute sets are also added to all
	 * currently-joined lookup services.
	 *
	 * @param: attrSets the attribute sets to add
	 * @see net.jini.admin.JoinAdmin#addLookupAttributes(net.jini.core.entry.Entry[])
	 */
	public void addLookupAttributes(Entry[] attrSets) {
		joinManager.addAttributes(attrSets, true);
		logger.debug( "Added attributes");
	}

	/**
	 * Modify the current attribute sets, using the same semantics as
	 * ServiceRegistration.modifyAttributes. The resulting setValue will be used for
	 * all future joins. The same modifications are also made to all
	 * currently-joined lookup services.
	 *
	 * @param attrSetTemplates
	 *            - the templates for matching attribute sets
	 * @param attrSets
	 *            the modifications to make to matching sets
	 * @exception java.rmi.RemoteException
	 * @see net.jini.admin.JoinAdmin#modifyLookupAttributes(net.jini.core.entry.Entry[],
	 *      net.jini.core.entry.Entry[])
	 */
	public void modifyLookupAttributes(Entry[] attrSetTemplates,
									   Entry[] attrSets) {
		joinManager.modifyAttributes(attrSetTemplates, attrSets, true);
		logger.debug("Modified attributes");
	}

	/**
	 * Get the list of groups to join. An empty array means the service joins no
	 * groups (as opposed to "all" groups).
	 *
	 * @return an array of groups to join. An empty array means the service
	 *         joins no groups (as opposed to "all" groups).
	 *
	 * @exception java.rmi.RemoteException
	 * @see net.jini.admin.JoinAdmin#getLookupGroups()
	 */
	public String[] getLookupGroups() {
		return ldmgr.getGroups();
	}

	/**
	 * Add new groups to the setValue to join. Lookup services in the new groups will
	 * be discovered and joined.
	 *
	 * @param groups
	 *            groups to join
	 * @exception java.rmi.RemoteException
	 * @see net.jini.admin.JoinAdmin#addLookupGroups(String[])
	 */
	public void addLookupGroups(String[] groups) {
		try {
			ldmgr.addGroups(groups);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("Error while adding groups : {0}", e);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Added lookup groups: {0}",
					SorcerUtil.arrayToString(groups));
		}
	}

	protected static void checkFileExists(File file) throws IOException {
		if (!file.exists()) {
			throw new IOException("***error: the file does not exist: "
					+ file.getAbsolutePath());
		}
		if (!file.canRead()) {
			throw new IOException("***error: the file is not readable: "
					+ file.getAbsolutePath());
		}
	}

	/**
	 * Remove groups from the setValue to join. Leases are cancelled at lookup
	 * services that are not members of any of the remaining groups.
	 *
	 * @param groups
	 *            groups to leave
	 * @exception java.rmi.RemoteException
	 * @see net.jini.admin.JoinAdmin#removeLookupGroups(String[])
	 */
	public void removeLookupGroups(String[] groups) {
		try {
			ldmgr.removeGroups(groups);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("Error while removing groups : {0}", e);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Removed lookup groups: {0}",
					SorcerUtil.arrayToString(groups));
		}
	}

	/**
	 * Replace the list of groups to join with a new list. Leases are cancelled
	 * at lookup services that are not members of any of the new groups. Lookup
	 * services in the new groups will be discovered and joined.
	 *
	 * @param groups
	 *            groups to join
	 * @exception java.rmi.RemoteException
	 * @see net.jini.admin.JoinAdmin#setLookupGroups(String[])
	 */
	public void setLookupGroups(String[] groups) {
		try {
			ldmgr.setGroups(groups);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("Error while setting groups : {0}", e);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Set lookup groups: {0}",
					SorcerUtil.arrayToString(groups));
		}
	}

	/**
	 * Get the list of locators of specific lookup services to join.
	 *
	 * @return the list of locators of specific lookup services to join
	 * @exception java.rmi.RemoteException
	 * @see net.jini.admin.JoinAdmin#getLookupLocators()
	 */
	public LookupLocator[] getLookupLocators() {
		return ldmgr.getLocators();
	}

	/**
	 * Add locators for specific new lookup services to join. The new lookup
	 * services will be discovered and joined.
	 *
	 * @param locators locators of specific lookup services to join

	 * @see net.jini.admin.JoinAdmin#addLookupLocators(net.jini.core.discovery.LookupLocator[])
	 */
	public void addLookupLocators(LookupLocator[] locators) {
		// for (int i = locators.length; --i >= 0; ) {
		// locators[i] = (LookupLocator)
		// locatorPreparer.prepareProxy(locators[i]);
		// }
		ldmgr.addLocators(locators);
		if (logger.isDebugEnabled()) {
			logger.debug("Added lookup locators: {0}",
					SorcerUtil.arrayToString(locators));
		}
	}

	/**
	 * Remove locators for specific lookup services from the setValue to join. Any
	 * leases held at the lookup services are cancelled.
	 *
	 * @param locators
	 *            locators of specific lookup services to leave
	 * @exception java.rmi.RemoteException
	 * @see net.jini.admin.JoinAdmin#removeLookupLocators(net.jini.core.discovery.LookupLocator[])
	 */
	public void removeLookupLocators(LookupLocator[] locators) {
		// for (int i = locators.length; --i >= 0; ) {
		// locators[i] = (LookupLocator) locatorPreparer.prepareProxy(
		// locators[i]);
		// }
		ldmgr.removeLocators(locators);
		if (logger.isDebugEnabled()) {
			logger.debug("Removed lookup locators: {0}",
					SorcerUtil.arrayToString(locators));
		}
	}

	// Inherit java doc from super type
	public void setLookupLocators(LookupLocator[] locators) {
		// for (int i = locators.length; --i >= 0; ) {
		// locators[i] = (LookupLocator)
		// locatorPreparer.prepareProxy(locators[i]);
		// }
		ldmgr.setLocators(locators);
		if (logger.isDebugEnabled()) {
			logger.debug("Set lookup locators: {}", SorcerUtil.arrayToString(locators));
		}
	}

	/**
	 * Method invoked by a server to inform the LifeCycle object that it can
	 * release any resources associated with the server.
	 *
	 * @param impl
	 *            Object reference to the implementation object created by the
	 *            NonActivatableServiceDescriptor. This reference must be equal,
	 *            in the "==" sense, to the object created by the
	 *            NonActivatableServiceDescriptor.
	 * @return true if the invocation was successfully processed and false
	 *         otherwise.
	 */
	public boolean unregister(Object impl) {
		logger.info("Unregistering service");
		if (this == impl)
			this.destroy();
		return true;
	}

//	@Override
	public boolean isActive() throws IOException {
		return isBusy();
	}

	@Override
	public Object exec(Arg... args) throws MogramException, RemoteException {
		Mogram srv = Arg.getMogram(args);
		if (srv != null) {
			return service(srv);
		}
		return null;
	}

	/**
	 * This method spawns a separate thread to destroy this provider after 1
	 * sec, should make a reasonable attempt to let this remote call return
	 * successfully.
	 */
	private class Destroyer implements Runnable {
		public void run() {
			try {
				// allow for remaining cleanup
				logger.info("going to call System.exit() real soon...");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			} finally {
				logger.info("going to call System.exit() NOW...");
				System.exit(0);
			}
		}
	}

	/**
	 * Unexport the service provider appropriately.
	 *
	 * @param force
	 *            terminate in progress calls if necessary
	 * @return true if unexport succeeds
	 */
	boolean unexport(boolean force)  {
		boolean unexported;
		try {
			unexported = delegate.unexport(force);
		} catch (NoSuchObjectException | IllegalStateException e) {
			unexported= false;
			logger.warn("Could not unexport ProviderDelegate", e);
		}
		return unexported;
	}

	/**
	 * Returns a proxy object for this object. This eval should not be null.
	 * Implements the <code>ServiceProxyAccessor</code> interface.
	 *
	 * @return a proxy object reference
	 * @exception java.rmi.RemoteException
	 */
	public Object getServiceProxy() {
		return getProxy();
	}

	/**
	 * Returns a proxy object for this provider. If the smart proxy is alocated
	 * then returns a non exported object to be registerd with loookup services.
	 * However, if a smart proxy implements {@link Outer} then the
	 * provider's proxy is setValue as its inner proxy. Otherwise the {@link java.rmi.Remote}
	 * outer proxy of this provider is returned.
	 *
	 * @return a proxy, or null
	 */
	public Object getProxy() {
		return delegate.getProxy();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see sorcer.core.provider.OuterProxy#getInnerProxy()
	 */
	public Remote getInner() {
		return delegate.getInner();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see sorcer.core.provider.OuterProxy#setInnerProxy(java.rmi.Remote)
	 */
	public void setInner(Object innerProxy) throws ProviderException {
		delegate.setInner(innerProxy);
	}

	/**
	 * Returns a string representation of this service provider.
	 *
	 * @see Object#toString()
	 */
	public String toString() {
		String className = getClass().getName();
		className = className.substring(className.lastIndexOf('.') + 1);
		String msg = null;
		if (delegate == null) {
			msg = "[delegate is null; no serviceID available]";
		} else {
			msg =  "[" + delegate.getServiceID() + "]";
		}
		return className + msg;
	}

	/**
	 * Simple container for an alternative return a eval so we can provide more
	 * detailed diagnostics.
	 */
	class InitException extends Exception {
		private static final long serialVersionUID = 1;

		private InitException(String message, Throwable nested) {
			super(message, nested);
		}
	}

	/**
	 * Portion of construction that is common between the activatable and not
	 * activatable cases. This method performs the minimum number of operations
	 * before establishing the Subject, and logs errors.
	 */
	public void init(String[] configOptions, LifeCycle lifeCycle)
			throws Exception {
		logger.debug("Entering init");
		this.lifeCycle = lifeCycle;
		try {
			// Take the login context entry from the configuration file, if this
			// entry is null, server will start without a subject
			loginContext = (LoginContext) delegate.getDeploymentConfig().getEntry(
					COMPONENT, "loginContext", LoginContext.class, null);
			logger.debug("loginContext " + loginContext);
			if (loginContext == null) {
				logger.debug("Login Context was null when the service was Started");
				// Starting the Service with NO subject provided
				initAsSubject();
			} else {
				logger.debug("Login Context was not null when the service was Started");
				loginContext.login();
				logger.debug("Login Context subject= "
						+ loginContext.getSubject());

				try {
					// Starting the Service with a subject
					Subject.doAsPrivileged(loginContext.getSubject(),
							new PrivilegedExceptionAction() {
								public Object run() throws Exception {
									initAsSubject();
									return null;
								}
							}, null);
				} catch (PrivilegedActionException e) {
					logger.warn("######## Priviledged Exception Occured ########");
					throw e.getCause();
				}
			}
			logger.info("Provider service started: "
					+ getProviderName(), this);

			// allow for enough time to export the provider's proxy and stay alive
			scheduler.schedule(new Callable<Void>() {
				@Override
				public Void call() throws RemoteException, ConfigurationException {
					delegate.initSpaceSupport();
					return null;
				}
			}, delegate.spaceTakerDelay, TimeUnit.MILLISECONDS);
		} catch (Throwable e) {
			initFailed(e);
		}
	}

	/**
	 * Log information about failing to initialize the service and rethrow the
	 * appropriate exception.
	 *
	 * @param throwable
	 *            exception produced by the failure
	 */
	static void initFailed(Throwable throwable) throws Exception {
		String message = null;
		if (throwable instanceof InitException) {
			message = throwable.getMessage();
			throwable = throwable.getCause();
		}
		if (logger.isErrorEnabled()) {
			if (message != null) {
				logger.error("initFailed, Unable to start provider service: "+message, throwable);
			} else {
				logger.error("Unable to start provider service", throwable);
			}
		}
		if (throwable instanceof Exception) {
			throw (Exception) throwable;
		} else if (throwable instanceof Error) {
			throw (Error) throwable;
		} else {
			IllegalStateException ise = new IllegalStateException(
					throwable.getMessage());
			ise.initCause(throwable);
			throw ise;
		}
	}

	/**
	 * Common construction for activatable and non-activatable cases, run under
	 * the proper Subject. If used, provider properties file has to declared as
	 * "properties" in the provider's Jini configuration file. However
	 * provider's properties can be defined directly in Jini provider's
	 * configuration file - the latter recommended.
	 */
	/**
	 *
	 */
	private void initAsSubject() {
		boolean done = false;
		// Initialize all properties of a provider, from provider properties
		// file and from provider's Jini configuration file
		try {
			delegate.init(this);

			// Use locators specified in the Jini configuration file, otherwise
			// from the environment configuration
			// String[] lookupLocators = (String[]) Config.getNonNullEntry(
			// delegate.getJiniConfig(), PROVIDER, "lookupLocators",
			// String[].class, new String[] {});
			String[] lookupLocators = new String[] {};
			String locators = delegate.getProviderConfig().getProperty(P_LOCATORS);
			if (locators != null && locators.length() > 0) {
				lookupLocators = locators.split("[ ,]");
			}
			logger.debug("provider lookup locators: "
					+ (lookupLocators.length == 0 ? "no locators" : Arrays
					.toString(lookupLocators)));

			String[] lookupGroups = (String[]) Config.getNonNullEntry(
					delegate.getDeploymentConfig(), COMPONENT, "lookupGroups",
					String[].class, new String[] {});
			if (lookupGroups.length == 0)
				lookupGroups = DiscoveryGroupManagement.ALL_GROUPS;
			logger.debug("provider lookup groups: "
					+ (lookupGroups != null ? "all groups" : Arrays
					.toString(lookupGroups)));

			String[] accessorGroups = (String[]) Config.getNonNullEntry(
					delegate.getDeploymentConfig(), COMPONENT, "accessorGroups",
					String[].class, new String[] {});
			if (accessorGroups.length == 0)
				accessorGroups = lookupGroups;
			logger.debug("service accessor groups: "
					+ (accessorGroups != null ? "all groups" : Arrays
					.toString(accessorGroups)));

			Entry[] serviceAttributes = getAttributes();
			serviceAttributes = addServiceUIDesciptors(serviceAttributes);

			logger.debug("service attributes: "
					+ Arrays.toString(serviceAttributes));
			ServiceID sid = getProviderID();
			if (sid != null) {
				delegate.setProviderUuid(sid);
			} else {
				logger.debug("Provider does not provide ServiceID, using default");
			}
			logger.debug("ServiceID: " + delegate.getServiceID());
			LookupLocator[] locs = new LookupLocator[lookupLocators.length];
			for (int i = 0; i < locs.length; i++) {
				locs[i] = new LookupLocator(lookupLocators[i]);
			}
			// Make sure to turn off multicast discovery if requested
			String[] groups;
			if (Sorcer.isMulticastEnabled()) {
				if (lookupGroups != null && lookupGroups.length > 0)
					groups = lookupGroups;
				else
					groups = delegate.groupsToDiscover;
				logger.warn(">>>> USING MULTICAST");
			} else {
//				groups = LookupDiscoveryManager.NO_GROUPS;
				groups = LookupDiscoveryManager.ALL_GROUPS;
				logger.warn(">>>> USING UNICAST ONLY");
			}

			logger.info(">>>LookupDiscoveryManager with groups: "
					+ Arrays.toString(groups) + "\nlocators: "
					+ Arrays.toString(locs));
			ldmgr = new LookupDiscoveryManager(groups, locs, null);
			/* registers provider's proxy so this provider is discoverable or not*/
			boolean discoveryEnabled = (Boolean) Config.getNonNullEntry(
					delegate.getDeploymentConfig(), COMPONENT, ProviderDelegate.DISCOVERY_ENABLED,
					boolean.class, true);
			logger.info(ProviderDelegate.DISCOVERY_ENABLED + ": " + discoveryEnabled);
			Object proxy = null;
			if (discoveryEnabled) {
				proxy = delegate.getProxy();
			} else {
				proxy = delegate.getAdminProxy();
			}
			logger.info("*** PROXY>>>>>registering proxy for: "
					+ getProviderName() + ":" + proxy);

			joinManager = new JoinManager(proxy, serviceAttributes, sid,
					ldmgr, null);
			done = true;
		} catch (Throwable e) {
			logger.error("Error initializing service: ", e);
		} finally {
			if (!done) {
				try {
					unexport(true);
				} catch (Exception e) {
					logger.info("unable to unexport after failure during startup", e);
				}
			}
		}
	}

	/** A trust verifier for secure dynamic and smart proxies. */
	final static class ProxyVerifier implements TrustVerifier, Serializable {
		private final RemoteMethodControl serverProxy;

		private final Uuid serverUuid;

		/**
		 * Create the verifier, throwing UnsupportedOperationException if the
		 * server proxy does not implement both RemoteMethodControl and
		 * TrustEquivalence.
		 */
		public ProxyVerifier(Object serverProxy, Uuid serverUuid) {
			if (serverProxy instanceof RemoteMethodControl
					&& serverProxy instanceof TrustEquivalence) {
				this.serverProxy = (RemoteMethodControl) serverProxy;
			} else {
				throw new UnsupportedOperationException();
			}
			this.serverUuid = serverUuid;
		}

		/** Implement TrustVerifier */
		public boolean isTrustedObject(Object obj, Context ctx) {
			if (obj == null || ctx == null) {
				throw new NullPointerException();
			} else if (!(obj instanceof ProxyAccessor)) {
				return false;
			} else if (!(obj instanceof ReferentUuid))
				return false;

			if (!serverUuid.equals(((ReferentUuid) obj).getReferentUuid()))
				return false;

			RemoteMethodControl otherServerProxy = (RemoteMethodControl) ((ProxyAccessor) obj)
					.getProxy();
			MethodConstraints mc = otherServerProxy.getConstraints();
			TrustEquivalence trusted = (TrustEquivalence) serverProxy
					.setConstraints(mc);
			return trusted.checkTrustEquivalence(otherServerProxy);
		}
	}

	/**
	 * Returns a UI descriptor for this provider to be included in a UI
	 * descriptor for your provider. This method should be implemented in
	 * sublcasses inmplementing the Jini ServiceUI framwork.
	 *
	 * @return an UI descriptor for your provider. <code>null</code> if not
	 *         overwritten in subclasses.
	 */
	public UIDescriptor getMainUIDescriptor() {
		return null;
	}

	public static UIDescriptor getProviderUIDescriptor() {
		UIDescriptor descriptor = null;
		try {
			descriptor = UIDescriptorFactory.getUIDescriptor(
					MainUI.ROLE,
					new UIComponentFactory(new URL[] { new URL(String.format("%s/sorcer-ui-%s.jar",
							Sorcer.getWebsterUrl(),
							SOS.getSorcerVersion()))
					},
							"sorcer.ui.provider.ProviderUI"));
		} catch (Exception ex) {
			logger.debug("getServiceUI", ex);
		}
		return descriptor;
	}

	/**
	 * Returns an array of additional service UI descriptors to be included in a
	 * Jini service item that is registerd with lookup services. By default a
	 * generic ServiceProvider service UI is provided with: attribute viewer,
	 * context and task editor for this service provider.
	 *
	 * @return an array of service UI descriptors
	 */
	public UIDescriptor[] getServiceUIEntries() {
		// Service UI as a panel of the ServiceBrowser
//		UIDescriptor uiDesc1 = null;
//		try {
//			uiDesc1 = UIDescriptorFactory.getUIDescriptor(
//					MainUI.ROLE,
//					new UIComponentFactory(new URL[] {new URL(String.format("%s/sorcer-ui-%s.jar",
//							Sorcer.getWebsterUrl(),
//							SOS.getSorcerVersion()))
//					},
//							"sorcer.ui.exertlet.NetletEditor"));
//		} catch (Exception ex) {
//			logger.debug("getServiceUI", ex);
//		}

		// Service UI as a standalone frame is the ServiceBrowser
		UIDescriptor uiDesc2 = null;
		try {
			URL uiUrl = new URL(Sorcer.getWebsterUrl() + "/exertlet-ui.jar");
			URL helpUrl = new URL(Sorcer.getWebsterUrl()
					+ "/exertlet/exertlet-ui.html");

			// URL exportUrl, String className, String name, String helpFilename
			uiDesc2 = UIDescriptorFactory.getUIDescriptor(MainUI.ROLE,
					(JFrameFactory) new UIFrameFactory(new URL[] { uiUrl },
							"sorcer.ui.exertlet.NetletUI", "Netlet Editor",
							helpUrl));
		} catch (Exception ex) {
			logger.debug("getServiceUI", ex);
		}

		return new UIDescriptor[] { getProviderUIDescriptor(), uiDesc2 };
	}

	/**
	 * Returnes an appended list of enrties that includes UI descriptors of this
	 * provider.
	 *
	 * @param serviceAttributes
	 * @return an array of UI descriptors
	 */
	private Entry[] addServiceUIDesciptors(Entry[] serviceAttributes) {
		if (delegate.getSmartProxy() != null || delegate.getPartner() != null) {
			return serviceAttributes;
		}

		Entry[] attrs = serviceAttributes;
		Entry uiDescriptor = getMainUIDescriptor();
		UIDescriptor[] uiDescriptors = getServiceUIEntries();
		int tally = 0;
		if (uiDescriptor != null)
			tally++;
		if (uiDescriptors != null)
			tally = tally + uiDescriptors.length;
		if (tally == 0)
			return attrs;
		attrs = new Entry[serviceAttributes.length + tally];
		System.arraycopy(serviceAttributes, 0, attrs, 0,
				serviceAttributes.length);
		if (uiDescriptors != null)
			for (int i = 0; i < uiDescriptors.length; i++)
				attrs[serviceAttributes.length + i] = uiDescriptors[i];

		if (uiDescriptor != null)
			attrs[serviceAttributes.length + tally - 1] = uiDescriptor;
		return attrs;
	}

	public Map<?, ?> getServiceComponents() {
		return delegate.getServiceComponents();
	}

	public void setServiceComponents(Map<?, ?> serviceComponents) {
		delegate.setServiceComponents(serviceComponents);
	}

	public boolean isSpaceSecurityEnabled() {
		return delegate.isSpaceSecurityEnabled();
	}

	public boolean isMonitorable() {
		return delegate.isMonitorable();
	}

	public Logger getContextLogger() {
		return delegate.getContextLogger();
	}

	public Logger getProviderLogger() {
		return delegate.getProviderLogger();
	}

	public Logger getRemoteLogger() {
		return delegate.getRemoteLogger();
	}

	/**
	 * Defines rediness of the provider: true if this provider is ready to
	 * process the incoming exertion, otherwise false.
	 *
	 * @return true if the provider is redy to exert the exertion
	 */
	public boolean isReady(Exertion exertion) {
		return true;
	}

	private String[] providerCurrentContextList(String interfaceName) {
		boolean contextLoaded = false;
		try {
			FileInputStream fis = new FileInputStream("../configs/"
					+ ContextManagement.CONTEXT_FILENAME);
			ObjectInputStream in = new ObjectInputStream(fis);
			try {
				theContextMap = (HashMap<String, Context>) in.readObject();
			} catch (ClassNotFoundException e) {
				logger.warn("currentContextList", e);
			}
			in.close();
			fis.close();
			contextLoaded = true;
		} catch (IOException e) {
			logger.warn( "currentContextList", e);
			contextLoaded = false;
		}
		String[] toReturn = new String[0];
		if (contextLoaded) {
			Set<String> keys = theContextMap.keySet();
			String[] temp = new String[keys.size()];

			int j = 0;
			for (Iterator<String> iter = keys.iterator(); iter.hasNext();) {
				temp[j] = iter.next();
				j++;
			}

			int counter = 0;
			for (int i = 0; i < temp.length; i++)
				if (temp[i].startsWith(interfaceName))
					counter++;
			toReturn = new String[counter];
			counter = 0;
			for (int i = 0; i < temp.length; i++)
				if (temp[i].startsWith(interfaceName)) {
					toReturn[counter] = temp[i].substring(interfaceName
							.length() + 2);
					counter++;
				}
		}
		return toReturn;
	}

	private boolean providerDeleteContext(String interfaceName,
										  String methodName) {
		boolean contextLoaded = false;
		try {
			FileInputStream fis = new FileInputStream("../configs/"
					+ ContextManagement.CONTEXT_FILENAME);
			ObjectInputStream in = new ObjectInputStream(fis);
			try {
				theContextMap = (HashMap<String, Context>) in.readObject();
			} catch (ClassNotFoundException e) {
				logger.warn( "deleteContext", e);
			}
			in.close();
			contextLoaded = true;
		} catch (IOException e) {
			logger.warn( "deleteContext", e);
			contextLoaded = false;
		}

		try {
			if (theContextMap.containsKey(interfaceName + "core/sorcer-ui/src/main" + methodName))
				theContextMap.remove(interfaceName + "core/sorcer-ui/src/main" + methodName);
			// theContextMap.put(interfaceName+".."+methodName, theContext);

			FileOutputStream fos = new FileOutputStream("../configs/"
					+ ContextManagement.CONTEXT_FILENAME);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			out.writeObject(theContextMap);
			out.close();
			fos.close();
		} catch (IOException e) {
			logger.warn( "deleteContext", e);
			return false;
		}
		return true;
	}

	private Context<?> providerGetMethodContext(String interfaceName,
												String methodName) {
		logger.info("user directory is " + System.getProperty("user.dir"));
		boolean contextLoaded = false;
		try {
			FileInputStream fis = new FileInputStream("../configs/"
					+ ContextManagement.CONTEXT_FILENAME);
			ObjectInputStream in = new ObjectInputStream(fis);
			try {
				theContextMap = (HashMap<String, Context>) in.readObject();
			} catch (ClassNotFoundException e) {
				logger.warn( "getMethodContext",
						e);
			}
			in.close();
			fis.close();
			contextLoaded = true;
		} catch (IOException ioe) {
			// logger.warn( "getMethodContext",
			// ioe);
			// logger.info("no context file availabe for the provider: " +
			// getProviderName());
			contextLoaded = false;
		}
		Context context = new ServiceContext();
		if (contextLoaded
				&& theContextMap.containsKey(interfaceName + "core/sorcer-ui/src/main" + methodName)) {
			context = theContextMap.get(interfaceName + "core/sorcer-ui/src/main" + methodName);
		}
		return context;
	}

	private boolean providerSaveMethodContext(String interfaceName,
											  String methodName, Context<?> theContext) {
		boolean contextLoaded = false;
		try {
			FileInputStream fis = new FileInputStream("../configs/"
					+ ContextManagement.CONTEXT_FILENAME);
			ObjectInputStream in = new ObjectInputStream(fis);
			try {
				theContextMap = (HashMap<String, Context>) in.readObject();
			} catch (ClassNotFoundException e) {
				logger.warn( "saveMethodContext",
						e);
			}
			in.close();
			contextLoaded = true;
		} catch (IOException e) {
			logger.warn( "saveMethodContext", e);
			contextLoaded = false;
		}
		theContextMap.put(interfaceName + "core/sorcer-ui/src/main" + methodName, theContext);
		try {

			FileOutputStream fos = new FileOutputStream("../configs/"
					+ ContextManagement.CONTEXT_FILENAME);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			out.writeObject(theContextMap);
			out.close();
			fos.close();
		} catch (IOException e) {
			logger.warn( "put", e);
			return false;
		}
		return true;
	}

	public String[] getAccessorGroups() {
		return accessorGroups;
	}

	public void setAccessorGroups(String[] accessorGroups) {
		this.accessorGroups = accessorGroups;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see sorcer.core.Provider#mutualExclusion()
	 */
	@Override
	public boolean mutualExclusion() {
		if (delegate != null) {
			return delegate.mutualExclusion;
		} else {
			return false;
		}
	}

	@Override public Map<String, MethodAnalytics> getMethodAnalytics() {
		return delegate.getAnalyticsRecorder().getMethodAnalytics();
	}

	@Override public MethodAnalytics getMethodAnalytics(String name) {
		return delegate.getAnalyticsRecorder().getMethodAnalytics(name);
	}

	@Override public SystemAnalytics getSystemAnalytics() {
		return delegate.getAnalyticsRecorder().getSystemAnalytics();
	}

	protected synchronized void doTimeKeeping(double callTimeSec) {
		totalCallTime += callTimeSec;
		avgExecTime = totalCallTime / numCalls;
		logger.info("execution time = " + (callTimeSec) + " [s]");
		logger.info("average execution time = " + (avgExecTime) + " [s]");
	}

	// fields for thread metrics
	//
	private int numThreads = 0;
	private List<String> threadIds = new ArrayList<>();
	private int numCalls = 0;
	private double avgExecTime = 0;
	private double totalCallTime = 0;

	public synchronized String getThreadStatus() {
		String host = delegate.getHostAddress();
		String msg = "host = " + host + " "
				   + "\ntotal service op calls = " + numCalls + " "
				   + "\nnumber of service op calls running = "	+ numThreads + " "
				   + "\nservice op call ids running = " + threadIds + " "
		           + "\naverage exec time [s]       = " + avgExecTime;
        return msg;
	}

	public synchronized String doThreadMonitor(String serviceIdString) {
		String prefix;
		if (serviceIdString == null) {
			numCalls++;
			numThreads++;
			prefix = "adding service op call";
			serviceIdString = Integer.toString(numCalls);
			threadIds.add(serviceIdString);
		} else {
			numThreads--;
			prefix = "subtracting service op call";
			threadIds.remove(serviceIdString);
		}
		logger.info("\n\n***provider class = " + this.getClass()
				+ "\n***" + prefix + ": total service op calls = " + numCalls
				+ "\n***" + prefix + ": number of service op calls running = "
				+ numThreads + "\n***" + prefix + ": service op call ids running = "
				+ threadIds + "\n");

		return serviceIdString;
	}

	public void init() throws ConfigurationException {
		delegate.init(this);
	}

	public void init(String propFile) throws ConfigurationException {
		delegate.init(this, propFile);
	}

	public void init(ProviderDelegate delegate) {
		this.delegate = delegate;
	}

	public ServiceID getProviderID() {
		return delegate.getServiceID();
	}

	/**
	 * Implements {@link net.jini.lookup.ServiceIDListener}.
	 * <p>
	 * This function is called when a service ID is assigned. It also tries to
	 * persist the service ID.
	 * <p>
	 * TODO: This functionality is similar / identical / linked to
	 * {@link sorcer.core.provider.ProviderDelegate#restore()}. Investigate.
	 *
	 * @param sid
	 *            The assigned ServiceID
	 *
	 * @see net.jini.lookup.ServiceIDListener#serviceIDNotify(net.jini.core.lookup.ServiceID)
	 */
	public void serviceIDNotify(ServiceID sid) {
		logger.info("Service has been assigned service ID: " + sid.toString());
		delegate.setProviderUuid(sid);
		try {
			// args ->fileName, object, isAbsolutePath
			String fileName = getServiceIDFile();
			File file = new File(fileName);
			if (!file.exists())
				file.createNewFile();
			ObjectLogger.persist(fileName, sid, true);
		} catch (Exception e) {
			// e.printStackTrace();
			logger.info("Cannot write service ID to persistent storage. So writting to present directory");
			try {
				File file = new File("provider.sid");
				if (!file.exists())
					file.createNewFile();
				ObjectLogger.persist("provider.sid", sid, true);
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * Returns a server delegate for this server.
	 *
	 * @return this server delegate
	 */
	public ProviderDelegate getDelegate() {
		return delegate;
	}

	private String getServiceIDFile() {
		String packagePath = this.getClass().getName();
		packagePath = packagePath.substring(0, packagePath.lastIndexOf("."))
				.replace('.', File.separatorChar);
		String sidFile = new StringBuffer(Sorcer.getWebsterUrl())
				.append(File.separatorChar)
				.append(packagePath)
				.append(packagePath.substring(packagePath
						.lastIndexOf(File.separatorChar))).append(".sid")
				.toString();
		return sidFile;
	}

	public long getLeastSignificantBits() {
		return (delegate.getServiceID() == null) ? -1 : delegate.getServiceID()
				.getLeastSignificantBits();
	}

	public long getMostSignificantBits() {
		return (delegate.getServiceID() == null) ? -1 : delegate.getServiceID()
				.getMostSignificantBits();
	}

	/**
	 * A provider responsibility is to check a task completeness in paricular
	 * the relevance of the task's context.
	 */
	public boolean isValidTask(Exertion task) throws ExertionException {
		return true;
	}

	public String getInfo() {
		return SorcerUtil.arrayToString(getAttributes());
	}

	public boolean isValidMethod(String name) throws RemoteException, SignatureException {
		return delegate.isValidMethod(name);
	}

	public Context invokeMethod(String method, Context context)
			throws ExertionException {
		return delegate.invokeMethod(method, context);
	}

	public Exertion invokeMethod(String methodName, Exertion ex)
			throws ExertionException {
		return delegate.invokeMethod(methodName, ex);
	}

	/**
	 * This method calls on an ExertionProcessor which executes the exertion
	 * accordingly to its compositional type.
	 *
	 * @param exertion
	 *            Exertion
	 * @return Exertion
	 * @throws sorcer.service.ExertionException
	 * @see sorcer.service.Exertion
	 * @see sorcer.service.Conditional
	 * @see sorcer.core.provider.ControlFlowManager
	 * @throws java.rmi.RemoteException
	 * @throws sorcer.service.ExertionException
	 */
    public Exertion doExertion(final Exertion exertion, Transaction txn) throws ExertionException {
        logger.debug("service: {}", exertion.getName());
        // create an instance of the ControlFlowManager and call on the
        // process method, returns an Exertion
        Exertion out;
        try {
			if(delegate.isRemoteLogging()) {
				MDC.put(MDC_SORCER_REMOTE_CALL, MDC_SORCER_REMOTE_CALL);
				MDC.put(MDC_PROVIDER_ID, this.getId().toString());
				MDC.put(MDC_PROVIDER_NAME, this.getName());
			}
            if (exertion.getId() != null)
                MDC.put(MDC_MOGRAM_ID, exertion.getId().toString());

            out = (Exertion) getControlFlownManager(exertion).process();

        } finally  {
            MDC.remove(MDC_PROVIDER_NAME);
            MDC.remove(MDC_SORCER_REMOTE_CALL);
            MDC.remove(MDC_MOGRAM_ID);
            MDC.remove(MDC_PROVIDER_ID);
        }
        return out;
    }

    protected ControlFlowManager getControlFlownManager(Exertion exertion) throws ExertionException {
        List<Class> publishedIfaces = Arrays.asList(this.delegate.getPublishedServiceTypes());
        if (!(exertion instanceof Task) && (!publishedIfaces.contains(Spacer.class))
            && (!publishedIfaces.contains(Jobber.class)) && (!publishedIfaces.contains(Concatenator.class)))
            throw new ExertionException(new IllegalArgumentException("Unknown exertion type " + exertion));
        try {
            if (exertion.isMonitorable())
                return new MonitoringControlFlowManager(exertion, delegate);
            else
                return new ControlFlowManager(exertion, delegate);
        } catch (Exception e) {
			((Task) exertion).reportException(e);
			throw new ExertionException(e);
		}
	}

	public Exertion serviceContextOnly(Context mogram) throws ExertionException, RemoteException {
		Task task = null;
		try {
			Object subject = mogram.getSubjectValue();
			if (subject instanceof Signature) {
				task = new NetTask((Signature)mogram.getSubjectValue(), mogram);
				task = delegate.doTask(task, null);
			} else {
				throw new ExertionException("no signature in the service context");
			}
		} catch (Exception e) {
			throw new ExertionException(e);
		}
		return task;
	}

	public Exertion service(Mogram exertion) throws RemoteException,
			ExertionException {
		return doExertion((Exertion)exertion, null);
	}

	@Override
	public Mogram exert(Mogram mogram, Transaction txn, Arg... args) throws TransactionException,
			ExertionException, RemoteException {
		if (mogram instanceof Task) {
			ServiceContext cxt;
			try {
				cxt = (ServiceContext) mogram.getDataContext();
				cxt.updateContextWith(mogram.getProcessSignature().getInConnector());
				Uuid id = cxt.getId();
				// a created session to be used in the implementation class of the bean itself
				ProviderSession ps = (ProviderSession) sessions.get(id);
				if (ps == null) {
					ps = new ProviderSession(id);
					sessions.put(id, ps);
				}
				if (bean != null) {
                    return delegate.exertBeanTask((Task) mogram, bean, args);
                }
			} catch (ContextException e) {
				e.printStackTrace();
			}
		} else if (mogram instanceof Context) {
			return serviceContextOnly((Context)mogram);
		}

		// TODO transaction handling to be implemented when needed
		// TO DO HANDLING SUSSPENDED mograms
		// if (((ServiceExertion) exertion).monitorSession != null) {
		// new Thread(new ServiceThread(exertion, this)).start();
		// return exertion;
		// }
		Exertion exertion = (Exertion)mogram;
		// when service Locker is used
		if (delegate.mutualExlusion()) {
			Object mutexId = ((ControlContext)exertion.getControlContext()).getMutexId();
			if (mutexId == null) {
				exertion.getControlContext().appendTrace(
						"mutex required by: " + getProviderName() + ":"
								+ getProviderID());
				return exertion;
			} else if (!(mutexId.equals(delegate.getServiceID()))) {
				exertion.getControlContext().appendTrace(
						"invalid mutex for: " + getProviderName() + ":"
								+ getProviderID());
				return exertion;
			}
		}
		// allow provider to leave a trace
		// exertion.getControlContext().appendTrace(
		// delegate.mutualExlusion() ? "mutex in: "
		// + getProviderName() + ":" + getProviderID()
		// : "in: " + getProviderName() + ":"
		// + getProviderID());
		Exertion out = exertion;
		try {
			out = doExertion(exertion, txn);
		} catch (Exception e) {
			logger.error("{} failed", getProviderName(), e);
			out.reportException(new ExertionException(getProviderName() + " failed", e));
		}
		return out;
	}

	// TODO in/out/inout marking as defined in the inConnector
	private void updateContext(Task task) throws ContextException {
		Context connector = task.getProcessSignature().getInConnector();
		if (connector != null){
			Context dataContext = task.getDataContext();
			Iterator it = ((Map) connector).entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry e = (Map.Entry) it.next();
				dataContext.putInValue((String) e.getKey(), dataContext.asis((String) e.getValue()));
				dataContext.removePath((String) e.getKey());
			}
		}
	}

	public Map<Uuid, ServiceSession> getSessions() {
		return sessions;
	}

	public ServiceSession getSession(Context context) throws ContextException {
		return sessions.get(context.getId());
	}

	public void deletedSession(Context context) {
		sessions.remove(context.getId());
	}

	public Entry[] getAttributes() {
		return delegate.getAttributes();
	}

	public List<Object> getProperties() {
		return delegate.getProperties();
	}

	public Configuration getProviderConfiguration() {
		return delegate.getProviderConfiguration();
	}

	public String getDescription() {
		return delegate.getDescription();
	}

	public String[] getGroups() {
		return delegate.getGroups();
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Identifiable#getName()
	 */
	@Override
	public String getName() {
		String name = "<no delgate; no name available>";
		if (delegate != null) name = delegate.getProviderName();
		return name;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Identifiable#getId()
	 */
	@Override
	public Object getId() {
		return delegate.getServiceID();
	}

	public String getProviderName() {
		return delegate.getProviderName();
	}

	public void restore() {
		delegate.restore();
	}

	public void fireEvent() {
		// do noting
	}

	public void loadConfiguration(String filename) {
		delegate.getProviderConfig().loadConfiguration(filename);
	}

	public File getScratchDir() {
		return scratchManager.getScratchDir();
	}

	public File getScratchDir(String suffix) {
		return scratchManager.getScratchDir(suffix);
	}

	public File getScratchDir(Context context, String suffix)  {
		return scratchManager.getScratchDir(context, suffix);
	}

	public File getScratchDir(Context context) {
		return getScratchDir(context, "");
	}

	public URL getScratchURL(File scratchFile) {
		return scratchManager.getScratchURL(scratchFile);
	}

	public String getProperty(String key) {
		return delegate.getProviderConfig().getProperty(key);
	}

	public String getProperty(String key, String defaultValue) {
		return delegate.getProviderConfig().getProperty(key, defaultValue);
	}

	/**
	 * @return Returns the provider Jini configuration instance.
	 */
	public Configuration getDeploymentConfig() {
		return delegate.getDeploymentConfig();
	}

	public Properties getProviderProperties() {
		return delegate.getProviderProperties();
	}

	public void notifyInformation(Exertion task, String message) {
		delegate.notifyInformation(task, message);
	}

	public void notifyException(Exertion task, String message, Exception e) {
		delegate.notifyException(task, message, e);
	}

	public void notifyExceptionWithStackTrace(Exertion task, Exception e){
		delegate.notifyExceptionWithStackTrace(task, e);
	}

	public void notifyException(Exertion task, Exception e) {
		delegate.notifyException(task, e);
	}

	public void notifyWarning(Exertion task, String message) {
		delegate.notifyWarning(task, message);
	}

	public void notifyFailure(Exertion task, Exception e) {
		delegate.notifyFailure(task, e);
	}

	public void notifyFailure(Exertion task, String message) {
		delegate.notifyFailure(task, message);
	}

	// task/job monitoring API
	public void stop(Uuid uuid, Subject subject) throws UnknownExertionException, AccessDeniedException {
		delegate.stop(uuid, subject);
	}

	/**
	 * MonitorManagers call suspend a MonitorableService. Once suspend is
	 * called, the monitorables must suspend immediatly and return the suspended
	 * state of the context.
	 *
	 * @throws sorcer.service.UnknownExertionException
	 *             if the exertion is not executed by this provider.
	 *
	 * @throws java.rmi.RemoteException
	 *             if there is a communication error
	 *
	 */

	public void suspend(Uuid ref, Subject subject) throws UnknownExertionException, AccessDeniedException {
		delegate.suspend(ref, subject);
	}

	/**
	 * Resume if the resume functionality is supported by the provider Else
	 * start from the begining.
	 *
	 * @throws sorcer.service.UnknownExertionException
	 *             if the exertion is not executed by this provider.
	 *
	 * @throws java.rmi.RemoteException
	 *             if there is a communication error
	 *
	 */
	public void resume(Exertion ex) throws RemoteException, ExertionException {
		service((Mogram) ex);
	}

	/**
	 * Step if the step functionality is supported by the provider Else start
	 * from the begining.
	 *
	 * @throws sorcer.service.UnknownExertionException
	 *             if the exertion is not executed by this provider.
	 *
	 * @throws java.rmi.RemoteException
	 *             if there is a communication error
	 *
	 */
	public void step(Exertion ex) throws RemoteException, ExertionException {
		service(ex);
	}
/*
	*//**
	 * Calls the delegate to update the monitor with the current context.
	 *
	 * @param context
	 * @throws sorcer.service.MonitorException
	 * @throws java.rmi.RemoteException
	 *//*
	public void changed(Context<?> context, Object aspect) throws RemoteException,
			MonitorException {
		delegate.changed(context, aspect);
	}*/

	/**
	 * Destroy the service, if possible, including its persistent storage.
	 *
	 * @see Provider#destroy()
	 */
	public void destroy() {
		// stop KeepAwake thread
		if (!running.compareAndSet(true, false)) {
			logger.debug("destroy called another time");
			return;
		}

		try {
			logger.debug("Destroying service " + getProviderName());
			// Close remote logging
			if (ldmgr != null)
				ldmgr.terminate();
			boolean destroyJVM = true;
			if (joinManager != null) {
				for(Entry e : joinManager.getAttributes()) {
					if(e.getClass().getName().equals("org.rioproject.entry.OperationalStringEntry")) {
						destroyJVM = false;
					}
				}
				joinManager.terminate();
			}
			providers.remove(this);
			tally = tally - 1;

			logger.debug("destroyed provider: {} providers left: {}" + getProviderName(), tally);
			//if (threadManager != null)
			//	threadManager.terminate();

			unexport(true);
			if(providerAdmin!=null)
				providerAdmin.unregister();

			logger.debug("calling destroy on the delegate...");
			delegate.destroy();
			logger.debug("DONE calling destroy on the delegate.");
			if (lifeCycle != null) {
				lifeCycle.unregister(this);
			}
			checkAndMaybeKillJVM(destroyJVM);
		} catch(Exception e) {
			logger.error("Problem destroying service " + getProviderName(), e);
		}
	}

	void checkAndMaybeKillJVM(boolean destroyJVM) {
		if (destroyJVM && tally == 0) {
			new Thread(new Destroyer()).start();
		}
	}

	public boolean isBusy() {
		//if (threadManager != null)
		//	isBusy = isBusy || threadManager.getPending().size() > 0;
		boolean isBusy = delegate.exertionStateTable.size() > 0;
		logger.info("{} is busy? {}", getName(), isBusy);
		return isBusy;
	}

	/**
	 * ShutdownHook for the ServiceProvider
	 */
	static class ShutdownHook extends Thread {
		final ServiceProvider provider;
		ShutdownHook(ServiceProvider provider) {
			super("ShutdownHook");
			this.provider = provider;
		}

		public void run() {
			try {
				provider.destroy();
			} catch(Throwable t) {
				logger.error("Terminating ServiceProvider", t);
			}
		}
	}

	/**
	 * Destroy all services in this node (virtual machine) by calling each
	 * destroy().
	 *
	 * @see Provider#destroy()
	 */
	public void destroyNode() throws RemoteException {
		logger.info("providers.size() = " + providers.size());
		for (ServiceProvider provider : providers) {
			logger.info("calling destroy on provider name = " + provider.getName());
			provider.destroy();
		}
		// exit JVM after destroying all providers
		logger.debug("calling destroy provider node");
		System.exit(0);
	}

	/** {@inheritDoc} */
	public Uuid getReferentUuid() {
		return delegate.getProviderUuid();
	}

	public void updatePolicy(Policy policy) {
		if (Sorcer.getProperty("sorcer.policer.mandatory").equals("true")) {
			Policy.setPolicy(policy);
		} else {
			logger.info("sorcer.policer.mandatory property in sorcer.env is false");
		}
	}

	public HashMap<String, Context> theContextMap = new HashMap<String, Context>();

	public boolean loadContextDatabase() {
		try {
			FileInputStream fis = new FileInputStream("context.cxnt");
			ObjectInputStream in = new ObjectInputStream(fis);
			try {
				theContextMap = (HashMap<String, Context>) in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			in.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected void setupThreadManager() {
		// TaskManger(int maxThreads, long timeout, float loadFactor)
		// 10, 1000 * 15, 3.0f
		Configuration config = delegate.getDeploymentConfig();
		int maxThreads = 10, waitIncrement = 50;
		long timeout = 1000 * 15;
		float loadFactor = 3.0f;
		boolean threadManagement = false;
		try {
			threadManagement = (Boolean) config.getEntry(
					ServiceProvider.COMPONENT, THREAD_MANAGEMNT, boolean.class,
					false);
		} catch (Exception e) {
			// do nothing, default eval is used
			// e.printStackTrace();
		}
		ConfigurableThreadFactory tf = new ConfigurableThreadFactory();
		tf.setDaemon(true);
		tf.setNameFormat(tName(getName()) + "-init-%2$s");
		tf.setThreadGroup(ProviderDelegate.threadGroup);
		scheduler = Executors.newScheduledThreadPool(1, tf);
		logger.info("threadManagement: " + threadManagement);
		if (!threadManagement) {
			return;
		}
		logger.debug("Initialized scheduler: " + scheduler.toString());
		try {
			maxThreads = (Integer) config.getEntry(ServiceProvider.COMPONENT,
					MAX_THREADS, int.class);
		} catch (Exception e) {
//			logger.throwing(ServiceProvider.class.getName(),
//					"setupThreadManger#maxThreads", e);
		}
		logger.info("maxThreads: " + maxThreads);
		try {
			timeout = (Long) config.getEntry(ServiceProvider.COMPONENT,
					MANAGER_TIMEOUT, long.class);
		} catch (Exception e) {
//			logger.throwing(ServiceProvider.class.getName(),
//					"setupThreadManger#timeout", e);
		}
		logger.info("timeout: " + timeout);
		try {
			loadFactor = (Float) config.getEntry(ServiceProvider.COMPONENT,
					LOAD_FACTOR, float.class);
		} catch (Exception e) {
//			logger.throwing(ServiceProvider.class.getName(),
//					"setupThreadManger#loadFactor", e);
		}
		logger.info("loadFactor: " + loadFactor);
		try {
			waitIncrement = (Integer) config.getEntry(
					ServiceProvider.COMPONENT, WAIT_INCREMENT, int.class,
					waitIncrement);
		} catch (Exception e) {
//			logger.throwing(ServiceProvider.class.getName(),
//					"setupThreadManger#waitIncrement", e);
		}
		logger.info("waitIncrement: " + waitIncrement);

		//ControlFlowManager.WAIT_INCREMENT = waitIncrement;

		/**
		 * Create a task manager.
		 *
		 * @param maxThreads maximum number of threads to use on tasks
		 * @param timeout idle time before a thread exits
		 * @param loadFactor threshold for creating new threads.  A new
		 * thread is created if the total number of runnable tasks (both active
		 * and pending) exceeds the number of threads times the loadFactor,
		 * and the maximum number of threads has not been reached.
		 */
		//threadManager = new TaskManager(maxThreads, timeout, loadFactor);
	}

	/**
	 * <p>
	 * Returns a threda manger of this provider.
	 * </p>
	 *
	 * @param
	 * @return the thread manager
	 */
	/*public TaskManager getThreadManager() {
		return threadManager;
	} */

	public final static String DB_HOME = "dbHome";
	public final static String THREAD_MANAGEMNT = "threadManagement";
	public final static String MAX_THREADS = "maxThreads";
	public final static String MANAGER_TIMEOUT = "threadTimeout";
	public final static String LOAD_FACTOR = "loadFactor";
	// wait for a TaskThread result in increments
	public final static String WAIT_INCREMENT = "waitForResultIncrement";

	/*
	 * (non-Javadoc)
	 *
	 * @see sorcer.core.Provider#getJavaSystemProperties()
	 */
	@Override
	public Properties getJavaSystemProperties() {
		return System.getProperties();
	}

	public class KeepAwake implements Runnable {

		public void run() {
			try {
				while (running.get()) {
					Thread.sleep(ProviderDelegate.KEEP_ALIVE_TIME);

					// remove inactive sessions
					Iterator<Map.Entry<Uuid, ServiceSession>> si = sessions.entrySet().iterator();
					while (si.hasNext())  {
						Map.Entry<Uuid, ServiceSession> se = si.next();
						ProviderSession ss = (ProviderSession)se.getValue();
						long now = System.currentTimeMillis();
						if (now - ss.getLastAccessedTime() > ss.getMaxInactiveInterval() * 1000) {
							si.remove();
						}
					}
				}
			} catch (Exception doNothing) {
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see sorcer.core.Provider#isContextValid(sorcer.service.Context,
	 * sorcer.service.Signature)
	 */
	public boolean isContextValid(Context<?> dataContext, Signature forSignature) {
		return true;
	}

	private final int SLEEP_TIME = 250;

	protected void copyDirectoryFromJarResource(String resourceDirRef, File destinationDir) throws IOException, URISyntaxException {

		// example
		// resourceDirRef="bin/win"
		// destinationDir=new File("C:/temp")
		//
		// result is to copy bin/win from jar resources to c:/temp
		// C:/temp/win
		//

		if (!(destinationDir.exists())) destinationDir.mkdirs();

//        doLog("0. resourceDirRef = " + resourceDirRef);
//        doLog("destinationDir = " + destinationDir);

		// strip first '/' if included
		if (resourceDirRef.substring(0,0).equals("/"))resourceDirRef = resourceDirRef.substring(1);
		// strip last '/' if included
		int l0 = resourceDirRef.length();
		if (resourceDirRef.substring(l0,l0).equals("/"))resourceDirRef = resourceDirRef.substring(0,l0 - 1);
//        doLog("1. resourceDirRef = " + resourceDirRef);

		// get resource directory name to copy
		String resourceDirectoryName = resourceDirRef;
		l0 = resourceDirectoryName.lastIndexOf("/");
		if (l0 != -1) resourceDirectoryName = resourceDirectoryName.substring(l0 + 1);
//        doLog("resourceDirectoryName = " + resourceDirectoryName);


		// destination directory on disk
		File newDestDir = new File(destinationDir, resourceDirectoryName);
//        doLog("newDestDir = " + newDestDir);


		String[] files = getResourceListing(this.getClass(), resourceDirRef);
		for (String resourceItem:files) {

//            doLog("resourceItem = " + resourceItem);

			String resourceItemMinusDirRef = resourceItem.substring(resourceDirRef.length());
//            doLog("resourceItemMinusDirRef = " + resourceItemMinusDirRef);

			File destFile = new File(newDestDir.getAbsolutePath() + "/" +  resourceItemMinusDirRef);
//            doLog("destFile = " + destFile.getAbsolutePath());

			copyFileFromJarResource(resourceItem, destFile);
			destFile.setExecutable(true);
			destFile.setReadable(true);
		}
	}

	protected void copyFileFromJarResource(String resourceFileRef, File destination) throws IOException {
		if (destination.exists()) destination.delete();
		destination.getParentFile().mkdirs();
		destination.createNewFile();

		ClassLoader ex = Thread.currentThread().getContextClassLoader();
//        doLog("trying to load file from jar resource: " + resourceFileRef);
//        doLog("classLoader ex = " + ex);
		URL resourceURL = ex.getResource(resourceFileRef);
		InputStream is = null;
		if(resourceURL != null) {
//            doLog("Loaded from " + resourceURL.toExternalForm());
			is = resourceURL.openStream();
//            doLog("* Loading properties using: " + is);
			GenericUtil.redirectInputStream2File(is, destination);
		} else {
			throw new IOException("**error: file not found in jar resource. resourceFileRef = " + resourceFileRef);
		}
	}

	protected String[] getResourceListing(Class clazz, String path) throws URISyntaxException, IOException {
		URL dirURL = clazz.getClassLoader().getResource(path);
		//doLog("$$$$$$$$$$$$$$$$$$$$$$$$$ dirURL = " + dirURL);
		if (dirURL != null && dirURL.getProtocol().equals("file")) {
                /* A file path: easy enough */
			return new File(dirURL.toURI()).list();
		}

		if (dirURL == null) {
                /*
                 * In case of a jar file, we can't actually find a directory.
                 * Have to assume the same jar as clazz.
                 */
			String me = clazz.getName().replace(".", "/") + ".class";
			dirURL = clazz.getClassLoader().getResource(me);
		}

		if (dirURL.getProtocol().equals("jar")) {
			//doLog("&&&&&&&&&&&&&&&&&&&&&&&&&&&&& here");
                /* A JAR path */
			int beginIndex = 5;
			if (GenericUtil.isWindows()) beginIndex++;

			String jarPath = dirURL.getPath().substring(beginIndex, dirURL.getPath().indexOf("!")); //strip out only the JAR file
			// doLog("jarPath=" + jarPath);
			JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
			Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
			Set<String> result = new HashSet<String>(); //avoid duplicates in case it is a subdirectory
			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();
				//doLog("name = " + name);
				if (name.startsWith(path)) { //filter according to the path
					//doLog("** matched name = " + name);
					if (name.endsWith("/")) continue;
					String entry = name;
//                    String entry = name.substring(path.length());
//                    doLog("entry = " + entry);
//                    int checkSubdir = entry.indexOf("/");
//                    if (checkSubdir >= 0) {
//                        // if it is a subdirectory, we just return the directory name
//                        entry = entry.substring(0, checkSubdir);
//                    }
					result.add(entry);
				}
			}
			return result.toArray(new String[result.size()]);
		}

		throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
	}

	public Object getBean() {
		return bean;
	}

	public void setBean(Object bean) {
		this.bean = bean;
	}
}
