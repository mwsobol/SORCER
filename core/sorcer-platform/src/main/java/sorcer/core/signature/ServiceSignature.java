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
import org.slf4j.Logger;
import sorcer.core.SorcerConstants;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.provider.Modeler;
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

	/** the name of operation */
	protected String selector;

	protected String prefix;

	protected String ownerID;

	protected ReturnPath returnPath;

	// the indicated usage of this signature
	protected Set<Kind> rank = new HashSet<Kind>();

	// dependency management for this Signature
	protected List<Evaluation> dependers = new ArrayList<Evaluation>();

	// Must initialize to ANY to have correct JavaSpace workers behavior
	// to have exertions with providerName/serviceInfo specified going to
	// providers
	// named "providerName". Otherwise, exertions with providerName/serviceInfo
	// can
	// be picked up by workers with template null/serviceInfo that are not named
	// "providerName"
	protected String providerName = ANY;

	private ServiceID serviceID;

	protected Class<?> serviceType;

	// service typed to be mached by its service proxy
	protected Class[] matchTypes;

	// implementation of the serviceType
	protected Class<?> providerType;

	protected String group = "";

	protected Exertion exertion;

	/** preprocess, process, postprocess, append context */
	protected Type execType = Type.PROC;

	/** indicates whether this method is being processed by the exert method */
	protected boolean isActive = true;

	protected boolean isProvisionable = false;

	// shell can be used to exert exertions locally or remotely (as ServiceProvider)
	protected boolean isShellRemote = false;

	protected Context inConnector;

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

	private ServiceDeployment deployment;

	public ServiceSignature() {
		providerName = ANY;
	}

	public ServiceSignature(String selector) {
		this.selector = selector;
		name = selector;
	}

	public ServiceSignature(String name, String selector) {
		this.name = name;
		this.selector = selector;
	}

	public void setExertion(Exertion exertion) throws ExertionException {
		this.exertion = exertion;
	}

	public Exertion getExertion() {
		return exertion;
	}

	public Class<?> getServiceType() {
		return serviceType;
	}

	public Class[] getMatchTypes() {
		return matchTypes;
	}

	public void setMatchTypes(Class[] matchTypes) {
		this.matchTypes = matchTypes;
	}

	/**
	 * Returns a provider of <code>Variability</code> type.
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
		this.serviceType = serviceType;
	}

	public String getSelector() {
		return selector;
	}

	public String getProviderName() {
		return providerName;
	}

	@Override
	public Object getProvider() throws SignatureException {
		return provider(this);
	}

	public void setProviderName(String name) {
		providerName = name;
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
		group = method.group;
		contextTemplateIDs = method.contextTemplateIDs;
		exertion = method.exertion;
		setSelector(method.selector);

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
				this.selector = token.nextToken();
				prefix = token.nextToken();
			} else if (selector.equals("new")) {
				this.selector = null;
			} else {
				this.selector = selector;
			}
		} else {
			this.selector = null;
		}
	}

	public boolean isSelectable() {
		if (selector == null && serviceType == null) {
			return false;
		}
		Method[] methods = null;
		if (serviceType.isInterface())
			methods = serviceType.getMethods();
		else
			methods = providerType.getMethods();

		for (Method m : methods) {
			if (m.getName().equals(selector)) {
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
				+ (prefix != null ? "#" + selector : "")
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
				&& ("" + sig.selector).equals("" + selector)
				&& ("" + sig.providerName).equals("" + providerName);
	}

	@Override
	public int hashCode() {
		return 31 * ("" + serviceType).hashCode() + ("" + selector).hashCode()
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
			m = clazz.getMethod(selector, argTypes);
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
		return selector;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isService() {
		return serviceType.isInterface();
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	@Override
	public void setReturnPath(SignatureReturnPath returnPath) {
		this.returnPath = (ReturnPath)returnPath;
	}

	public boolean isProvisionable() {
		return isProvisionable;
	}

	public void setProvisionable(boolean isProvisionable) {
		this.isProvisionable = isProvisionable;
	}

	public void setProvisionable(Provision isProvisionable) {
		if (isProvisionable == Provision.YES
				|| isProvisionable == Provision.TRUE) {
			this.isProvisionable = true;
		} else {
			this.isProvisionable = true;
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
			typeComp = (""+selector)
					.compareTo(""+((ServiceSignature) signature).selector);
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
		try {
			out = task(this, cxt);
		} catch (SignatureException e) {
			throw new MogramException(e);
		}
		Object result = exert(out);
		if (result instanceof Context)
			return (Context)result;
		else
			return ((Task)exert(out)).getContext();
	}

	public Context exert(Mogram mogram) throws TransactionException,
			MogramException, RemoteException {
		return exert(mogram, null);
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

	public boolean isModelerSignature() {
		if(serviceType != null)
			return (Modeler.class.isAssignableFrom(serviceType));
		else
			return false;
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
	public Object exec(Arg... args) throws MogramException, RemoteException, TransactionException {
	    throw new MogramException("Signature service exec should be implementd in subclasses");
	}
}
