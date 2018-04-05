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

package sorcer.core.signature;

import net.jini.core.lookup.ServiceID;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.slf4j.Logger;
import sorcer.core.SorcerConstants;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.provider.Modeler;
import sorcer.core.provider.ProviderName;
import sorcer.core.provider.ServiceName;
import sorcer.service.*;
import sorcer.service.Strategy.Provision;
import sorcer.service.modeling.Variability;
import sorcer.util.Log;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.*;

import static sorcer.eo.operator.*;

public class ServiceSignature implements Signature, SorcerConstants {

	static final long serialVersionUID = -8527094638557595398L;

	/** our logger. */
	protected final static Logger logger = Log.getSorcerLog();

	protected String name;

	protected String prefix;

	protected String ownerID;

	protected ReturnPath returnPath;

	// the indicated usage of this signature
	protected Set<Kind> rank = new HashSet<Kind>();

	// dependency management for this Signature
	protected List<Evaluation> dependers = new ArrayList<Evaluation>();

	// Must initialize to ANY to have correct JavaSpace workers behavior
	// to have mograms with providerName/serviceInfo specified going to
	// providers
	// named "providerName". Otherwise, mograms with providerName/serviceInfo
	// can
	// be picked up by workers with template null/serviceInfo that are not named
	// "providerName"
	protected ProviderName providerName = new ProviderName(ANY);

	protected Uuid signatureId;

	// service ID associated wuth this signature
	private ServiceID serviceID;

	/** signature operation */
	protected Operation operation = new Operation();

	protected ServiceMultitype serviceType = new ServiceMultitype();

	// associated exertion only if needed
	protected Exertion exertion;

	/** preprocess, process, postprocess, append context */
	protected Type execType = Type.PROC;

	// true if is sharesd by multiple evalators associated with this signature
	protected boolean isReused = false;

	/** indicates whether this method is being processed by the exert method */
	protected boolean isActive = true;

	// shell can be used to exert mograms locally or remotely (as ServiceProvider)
	protected boolean isShellRemote = false;

	// context input connector
	protected Context inConnector;

	// context output connector
	protected Context outConnector;

	/**
	 * a context template to define the context appended from a provider
	 * identified by this method
	 */
	private String[] contextTemplateIDs;

	/**
	 * URL for a mobile agent: an inserted custom method executed by service
	 * providers
	 */
	private String agentCodebase;

	/** codebase for downloaded classes */
	private String codebase = System.getProperty("java.rmi.server.codebase");

	private String agentClass;

	private String portalURL;

	protected ServiceDeployment deployment;

	public ServiceSignature() {
		signatureId = UuidFactory.generate();
		providerName = new ServiceName(ANY);
	}

	public ServiceSignature(String selector) {
		signatureId = UuidFactory.generate();
		this.operation.selector = selector;
		name = selector;
	}

	public ServiceSignature(String name, String selector) {
		signatureId = UuidFactory.generate();
		this.name = name;
		this.operation.selector = selector;
	}

    public ServiceSignature(String selector, ServiceMultitype serviceType, ProviderName providerName) {
		signatureId = UuidFactory.generate();
		this.name = selector;
        this.operation.selector = selector;
        this.serviceType = serviceType;
        this.providerName =  providerName;
        execType = Type.PROC;
    }

	public void setExertion(Exertion exertion) throws ExertionException {
		this.exertion = exertion;
	}

	public Exertion getExertion() {
		return exertion;
	}

	public Class getServiceType() throws SignatureException {
		return serviceType.getProviderType();
	}

	public Class[] getMatchTypes() {
		return serviceType.matchTypes;
	}

	public void setMatchTypes(Class[] matchTypes) {
		this.serviceType.matchTypes = matchTypes;
	}

	/**
	 * Returns a provider of <code>Variability</code> fiType.
	 *
	 * @return Variability of this service provider
	 */
	public Variability<?> getVariability() {
		return null;
	}

	public Signature addRank(Kind... kinds) {
		rank.addAll(Arrays.asList(kinds));
		return this;
	}

	public void addRank(List<Kind> kinds) {
		for (Kind k : kinds)
			rank.add(k);
	}

	public boolean isKindOf(Kind kind) {
		return rank.contains(kind);
	}

	public Set<Kind> getRank() {
		return rank;
	}

	public void removeKind(Kind kind) {
		rank.remove(kind);
	}

	public void setServiceType(Class<?> serviceType) {
		this.serviceType.providerType = serviceType;
	}

	public void setServiceType(ServiceMultitype serviceType) {
		this.serviceType = serviceType;
	}

	public String getSelector() {
		return operation.selector;
	}

	public ProviderName getProviderName() {
		return providerName;
	}

	@Override
	public Object getProvider() throws SignatureException {
		return provider(this);
	}

	public void setProviderName(ProviderName name) {
		providerName = name;
	}

	public void setProviderType(Class<?> providerType) {
		this.serviceType.providerType = providerType;
	}

	public void setOwnerId(String oid) {
		ownerID = oid;
	}

	public String getOwnerID() {
		return ownerID;
	}

	public String getAgentCodebase() {
		return agentCodebase;
	}

	public void setAgentCodebase(String codebase) {
		agentCodebase = codebase;
	}

	public String getAgentClass() {
		return agentClass;
	}

	public void setAgentClass(String className) {
		agentClass = className;
	}

	public String getPortalURL() {
		return portalURL;
	}

	public void setPortalURL(String url) {
		portalURL = url;
	}

	public static <T> T newSignature(T signature) throws SecurityException,
			NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {
		Constructor<? extends Object> constructor;
		Class<? extends Object> sigClass = signature.getClass();
		constructor = sigClass.getConstructor(signature.getClass());
		Object sig = constructor.newInstance(signature);

		return (T) sig;
	}

	public Signature copySignature(Signature m) {
		ServiceSignature method = (ServiceSignature) m;
		providerName = method.providerName;
		serviceType = method.serviceType;
		portalURL = method.portalURL;
		codebase = method.codebase;
		agentCodebase = method.agentCodebase;
		agentClass = method.agentClass;
		execType = method.execType;
		isActive = method.isActive;
		contextTemplateIDs = method.contextTemplateIDs;
		exertion = method.exertion;
		setSelector(method.operation.selector);

		return this;
	}

	public Type getType() {
		return execType;
	}

	public Signature setType(Type type) {
		execType = type;
		return this;
	}

	public boolean isActive() {
		logger.info("Returning "+name+".isActive()="+isActive);
		return isActive;
	}

	public void setActive(boolean state) {
		isActive = state;
		logger.info("Setting "+name+" Active to: "+isActive);
	}

	public void setActive(Operating state) {
		if (state == Operating.YES || state == Operating.TRUE) {
			isActive = true;
		} else {
			isActive = false;
		}
		logger.info("Setting "+name+" Active to: "+isActive);
	}

	public String[] getContextTemplateIDs() {
		return contextTemplateIDs;
	}

	/** Temporary adjustment for backward compatibility */
	public String getContextTemplateID() {
		return (contextTemplateIDs == null) ? null : contextTemplateIDs[0];
	}

	public void addContextTemplateID(String id) {
		String[] newContextTemplateIDs;
		if (contextTemplateIDs == null) {
			newContextTemplateIDs = new String[1];
			newContextTemplateIDs[0] = id;
		} else {
			newContextTemplateIDs = new String[contextTemplateIDs.length + 1];
			int i;
			for (i = 0; i < contextTemplateIDs.length; i++)
				newContextTemplateIDs[i] = contextTemplateIDs[i];
			newContextTemplateIDs[i] = id;
		}
		contextTemplateIDs = newContextTemplateIDs;
		// selfModified();
	}

	public void removeContextTemplateID(String id) {
		if (contextTemplateIDs == null)
			return;
		List<String> v = new ArrayList<String>();
		for (int i = 0; i < contextTemplateIDs.length; i++)
			v.add(contextTemplateIDs[i]);
		v.remove(id);
		if (contextTemplateIDs.length != v.size()) {
			contextTemplateIDs = new String[v.size()];
			v.toArray(contextTemplateIDs);
			// selfModified();
		}
	}

	public void setSelector(String selector) {
		if (selector != null) {
			if (selector.indexOf("#") > 0) {
				StringTokenizer token = new StringTokenizer(selector, "#");
				this.operation.selector = token.nextToken();
				prefix = token.nextToken();
			} else if (selector.equals("new")) {
				this.operation.selector = null;
			} else {
				this.operation.selector = selector;
			}
		} else {
			this.operation.selector = null;
		}
	}

	public boolean isSelectable() {
		if (operation.selector == null && serviceType == null) {
			return false;
		}
		Method[] methods = null;
		if (serviceType.providerType.isInterface())
			methods = serviceType.providerType.getMethods();
		else
			methods = serviceType.providerType.getMethods();

		for (Method m : methods) {
			if (m.getName().equals(operation.selector)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasContextTemplate() {
		if (contextTemplateIDs != null)
			return true;
		else
			return false;
	}

	public boolean isProcessType() {
		return execType == Type.PROC;
	}

	public boolean isPreprocessType() {
		return execType == Type.PRE;
	}

	public boolean isPostprocessType() {
		return execType == Type.POST;
	}

	public boolean isAppendType() {
		return execType == Type.APD_DATA;
	}

	public String toString() {
		return this.getClass() + ":" + providerName + ";" + execType + ";"
				+ ";" + serviceType + ";"
				+ (prefix != null ? "#" + operation.selector : "")
				+ ";" + returnPath;
	}

	public ServiceID getServiceID() {
		return serviceID;
	}

	public void setServiceID(ServiceID id) {
		serviceID = id;
	}

	@Override
	public boolean equals(Object signature) {
		if (!(signature instanceof ServiceSignature))
			return false;
		else if (!(signature.getClass() == this.getClass()))
			return false;
		ServiceSignature sig = (ServiceSignature) signature;
		return ("" + sig.serviceType).equals("" + serviceType)
				&& ("" + sig.operation.selector).equals("" + operation.selector)
				&& ("" + sig.providerName).equals("" + providerName);
	}

	@Override
	public int hashCode() {
		return 31 * ("" + serviceType).hashCode() + ("" + operation.selector).hashCode()
				+ ("" + providerName).hashCode();
	}

	public String getCodebase() {
		return codebase;
	}

	public void setCodebase(String codebase) {
		this.codebase = codebase;
	}

	@Override
	public void close() throws RemoteException, IOException {
		// implemented in subclasses
	}

	/**
	 * Returns a method provided by the requestor itself to substitute the
	 * existing provider'smethod. The implementation of this requestor's method
	 * overrides the existing implementation of the provider if one exists.Thus
	 * a new functionality is inserted into the executing provider if the
	 * alternative inserted method is acceptable (valid).
	 */
	public Method getSubstituteMethod(Class<?>[] argTypes) {
		Method m = null;
		try {
			Class<?> clazz = getSubstituteClass();
			if (clazz == null)
				return null;
			m = clazz.getMethod(operation.selector, argTypes);
		} catch (NoSuchMethodException nsme) {
			nsme.printStackTrace();
			// logger.info("The method: \"" + selector
			// + "\" doesn't exist in requestor's signature code");
		}
		return m;
	}

	private Class<?> getSubstituteClass() {
		// should be implemented by subclasses
		return null;
	}

	@Override
	public Object getId() {
		return signatureId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isService() {
		return serviceType.providerType.isInterface();
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String[] getGroups() {
		if (providerName instanceof ServiceName)
			return ((ServiceName)providerName).getGroups();
		else
			return null;
	}

	public void setGroups(String[] groups) {
		if (providerName instanceof ServiceName)
			((ServiceName)providerName).setGroups(groups);
	}

	@Override
	public void setReturnPath(SignatureReturnPath returnPath) {
		this.returnPath = (ReturnPath)returnPath;
	}

	public boolean isProvisionable() {
		return operation.isProvisionable;
	}

	public void setProvisionable(boolean isProvisionable) {
		this.operation.isProvisionable = isProvisionable;
	}

	public void setProvisionable(Provision isProvisionable) {
		if (isProvisionable == Provision.YES
				|| isProvisionable == Provision.TRUE) {
			this.operation.isProvisionable = true;
		} else {
			this.operation.isProvisionable = false;
		}
	}

	public boolean isShellRemote() {
		return isShellRemote;
	}

	public void setShellRemote(boolean isShellRemote) {
		this.isShellRemote = isShellRemote;
	}


	public void setShellRemote(Strategy.Shell shellExec) {
		if (shellExec == Strategy.Shell.REMOTE) {
			this.isShellRemote = true;
		} else {
			this.isShellRemote = false;
		}
	}

	@Override
	public void setReturnPath(String path) {
		returnPath = new ReturnPath<Object>(path);
	}

	@Override
	public void setReturnPath(String path, Direction direction) {
		returnPath = new ReturnPath<Object>(path, direction);
	}

	public ReturnPath getReturnPath() {
		return returnPath;
	}

	public ServiceDeployment getDeployment() {
		return deployment;
	}

	public void setDeployment(ServiceDeployment deployment) {
		this.deployment = deployment;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Object signature) {
		if (!(signature instanceof ServiceSignature))
			return -1;
		int typeComp = (""+serviceType)
				.compareTo(""+((ServiceSignature) signature).serviceType);
		if (typeComp == 0) {
			typeComp = (operation.selector)
					.compareTo(""+((ServiceSignature) signature).operation.selector);
		}
		return (typeComp != 0 ? typeComp : (""+providerName)
				.compareTo(""+((ServiceSignature) signature).providerName));
	}

	public Context exert(Mogram mogram, Transaction txn, Arg... args)
			throws TransactionException, MogramException, RemoteException {
		Context cxt = null;
		if (mogram instanceof Context) {
			cxt = (Context)mogram;
		} else {
			 cxt = context(exert(mogram, txn, args));
		}
		Task out = null;
        out = task(this, cxt);
        Object result = exert(out);
		if (result instanceof Context)
			return (Context)result;
		else
			return exert(out).getContext();
	}

	public Context exert(Mogram mogram) throws TransactionException,
			MogramException, RemoteException {
		return exert(mogram, null);
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	public Context getInConnector() {
		return inConnector;
	}

	public void setInConnector(Context inConnector) {
		this.inConnector = inConnector;
	}

	public Context getOutConnector() {
		return outConnector;
	}

	public void setOutConnector(Context outConnector) {
		this.outConnector = outConnector;
	}

	public boolean isReused() {
		return isReused;
	}

	public void setReused(boolean reused) {
		isReused = reused;
	}

	public boolean isModelerSignature() {
		if(serviceType.providerType != null)
			return (Modeler.class.isAssignableFrom(serviceType.providerType));
		else
			return false;
	}

	public Strategy.Access getAccessType() {
		return operation.accessType;
	}

	public void setAccessType(Strategy.Access accessType) {
		this.operation.accessType = accessType;
	}

	@Override
	public void addDependers(Evaluation... dependers) {
		if (this.dependers == null)
			this.dependers = new ArrayList<Evaluation>();
		for (Evaluation depender : dependers)
			this.dependers.add(depender);
	}

	@Override
	public List<Evaluation> getDependers() {
		return dependers;
	}

	@Override
	public Object exec(Arg... args) throws MogramException, RemoteException {
	    throw new MogramException("Signature service exec should be implementd in subclasses");
	}

}
