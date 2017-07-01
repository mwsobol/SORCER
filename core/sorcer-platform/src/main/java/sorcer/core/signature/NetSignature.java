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

import net.jini.core.entry.Entry;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.lookup.entry.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.exertion.NetTask;
import sorcer.core.provider.*;
import sorcer.eo.operator;
import sorcer.service.*;
import sorcer.util.MavenUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import static sorcer.eo.operator.*;

/**
 * Represents a handle to network service provider.
 *
 * Created by Mike Sobolewski
 */
public class NetSignature extends ObjectSignature {

	private static final long serialVersionUID = 1L;

	/**
	 * URL for a mobile agent: an inserted custom method executed by service
	 * providers
	 */
	private String agentCodebase;

	/** codebase for downloaded classes */
	private String codebase = System.getProperty("java.rmi.server.codebase");

	private String agentClass;

	private String portalURL;

	private boolean isUnicast = false;

	// provider bound to this signature
	transient private Provider provider;

	protected List<Entry> attributes;

    protected String version;
    private static Logger logger = LoggerFactory.getLogger(ObjectSignature.class);

	public NetSignature() {
		providerName = new ProviderName();
	}

	public NetSignature(ServiceSignature signature) throws SignatureException {
		this.name = signature.name;
		this.operation = signature.operation;
		this.providerName =  signature.providerName;
		this.serviceType = signature.serviceType;
		this.deployment = signature.deployment;
		this.returnPath = signature.returnPath;
	}

	public NetSignature(Class<?> serviceType) {
		this("none", serviceType, ANY);
	}
	
	public NetSignature(String selector, Class<?> serviceType) {
		this(selector, serviceType, ANY);
	}


	public NetSignature(String selector, Class<?> serviceType, ProviderName providerName) {
		this(selector, serviceType);
		this.providerName =  providerName;
		execType = Type.PROC;
	}


	public NetSignature(String selector, Class<?> serviceType,
			String providerName) {
		this(selector, serviceType, providerName, (Type) null);
	}

	public NetSignature(String selector, Class<?> serviceType,
			Type methodType) throws SignatureException {
		this(selector, serviceType);
		this.execType = methodType;
	}

	public NetSignature(String selector, Class<?> serviceType,
			List<Entry> attributes, Type methodType)
			throws SignatureException {
		this(selector, serviceType);
		this.execType = methodType;
		this.attributes = attributes;
	}

	public NetSignature(String selector, Class<?> serviceType,
						String providerName, Type methodType) {

		this(selector, serviceType, providerName, methodType, null);
	}

	public NetSignature(String selector, Class<?> serviceType,
						String providerName, Type methodType, Version version) {
		this.version = version!=null ? version.getName() : null;
		this.serviceType.providerType = serviceType;
        if (serviceType != null && version == null)
            this.version = MavenUtil.findVersion(serviceType);
		if (providerName == null || providerName.length() == 0)
			this.providerName = new ProviderName(ANY);
		else
			this.providerName = new ProviderName(providerName);
		if (methodType == null) 
			execType = Type.PROC;
		else
			execType = methodType;
		
		setSelector(selector);
	}

    /**
    String version of constructor - required i.e. when running from Scilab
    */
    public NetSignature(String selector, String strServiceType) {
        try {
            Class serviceType = Class.forName(strServiceType);
            this.serviceType.providerType = serviceType;
            if (serviceType!=null) this.version = MavenUtil.findVersion(serviceType);
            setSelector(selector);
        } catch (ClassNotFoundException e) {
            logger.error("Problem creating NetSignature: " + e.getMessage());
        }
    }

    public NetSignature(String selector, Class<?> serviceType, String version,
                        String providerName, Type methodType) {
        this(selector, serviceType, providerName, methodType);
        if (version!=null) this.version = version;
    }

    public NetSignature(String selector, Class<?> serviceType, String version,
                        String providerName) {
        this(selector, serviceType, version, providerName, null);
    }


    public void setExertion(Exertion exertion) throws ExertionException {
        this.exertion = exertion;
	}

	public Exertion getExertion() {
		return exertion;
	}

	public void setAttributes(List<net.jini.core.entry.Entry> attributes) {
		this.attributes = attributes;
	}

	public List<Entry> getAttributes() {
		if (attributes == null || attributes.size() == 0) {
			Entry[] atts = new Entry[] { new Name(providerName.getName()) };
			return Arrays.asList(atts);
		}
		return attributes;
	}

	public void addAttribute(Entry attribute) {
		attributes.add(attribute);
	}

	public void addAllAttributes(List<Entry> attributes) {
		attributes.addAll(attributes);
	}

    public Provider getService() throws SignatureException {
        if (provider == null) return provider;
        try {
            // ping provider to see if alive
            provider.getProviderName();
        } catch (RemoteException e) {
            // provider is dead; get new one
            //e.printStackTrace();
            provider = null;
            provider = (Provider)Accessor.get().getService(this);
        }
        return provider;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Service provider) {
        this.provider = (Provider)provider;
    }

	public String action() {
		String pn = (providerName == null) ? ANY : providerName.getName();
		return serviceType + ", " + operation.selector + ", " + pn;
	}

	public ProviderName getProviderName() {
		return providerName;
	}

	public void setProviderName(String name) {
		providerName.setName(name);
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

	public boolean equals(ServiceSignature method) {
		return (method != null) ? toString().equals(method.toString()) : false;
	}

	public int hashCode() {
		return toString().hashCode();
	}

	public String getCodebase() {
		return codebase;
	}

	public void setCodebase(String codebase) {
		this.codebase = codebase;
	}

	@Override
	public void close() throws RemoteException, IOException {
		if (provider instanceof Closing)
			((Closing)provider).close();
	}

	public Exertion invokeMethod(Exertion ex) throws RemoteException,
			ExertionException {
		// If customized method provided by Mobile Agent
		Method m = getSubstituteMethod(new Class[] { Mogram.class });
		try {
			if (m != null)
				return (Exertion) m.invoke(this, new Object[] { ex });

			if (((ServiceProvider) provider).isValidMethod(operation.selector)) {
				return ((ServiceProvider) provider).getDelegate()
						.invokeMethod(operation.selector, ex);
			} else {
				ExertionException eme = new ExertionException(
						"Not supported method: " + serviceType + "#" + operation.selector
								+ " by: "
								+  provider.getProviderName());
				((ServiceProvider) provider).notifyException(ex, "unsupported method",
						eme);
				throw eme;
			}
		} catch (Exception e) {
			throw new ExertionException(e);
		}
	}

	public Context invokeMethod(Context context) throws RemoteException,
			ExertionException {
		// If customized method provided by Mobile Agent
		Method m = getSubstituteMethod(new Class[] { Context.class });
		try {
			if (m != null)
				return ((Context) m.invoke(this, new Object[] { context }));

			if (((ServiceProvider) provider).isValidMethod(operation.selector)) {
				Context resultContext = ((ServiceProvider) provider)
						.getDelegate().invokeMethod(operation.selector, context);
				return resultContext;
			} else {
				ExertionException eme = new ExertionException(
						"Not supported method: " + serviceType + "#" + operation.selector
								+ " by: "
								+ ((Provider) provider).getProviderName());
				((ServiceProvider) provider).notifyException(context.getMogram(),
						"unsupported method", eme);
				throw eme;
			}
		} catch (Exception e) {
			throw new ExertionException(e);
		}
	}

	@Override
	public Context exert(Mogram mogram) throws TransactionException,
			MogramException, RemoteException {
		return exert(mogram, null);
	}

	@Override
	public Context exert(Mogram mogram, Transaction txn, Arg... args) throws TransactionException,
			MogramException, RemoteException {
		try {
			if (this.isShellRemote()) {
				Provider prv= (Provider) Accessor.get().getService(sig(RemoteServiceShell.class));
				return prv.exert(mogram, txn).getContext();
			}
			Provider prv = (Provider) operator.provider(this);
			Context cxt = null;
			NetTask task = null;
			if (mogram instanceof Context)
				cxt = (Context) mogram;
			else
				cxt = mogram.exert(txn);

			task = new NetTask(this, cxt);
			return task.exert(txn).getContext();
		} catch (Exception e) {
			throw new MogramException(e);
		}
	}

	public boolean isUnicast() {
		return isUnicast;
	}

	public void setUnicast(boolean isUnicast) {
		this.isUnicast = isUnicast;
	}

    public String getVersion() {
        return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String toString() {
		return this.getClass() + ":" + providerName + ";" + execType + ";"
				+ serviceType + ";" + operation.selector
					+ (prefix !=null ? "#" + prefix : "") 
					+ (returnPath != null ? ";"  + "result " + returnPath : "");
	}

	@Override
	public Object exec(Arg... args) throws MogramException, RemoteException {
		Exertion mog = Arg.getExertion(args);
		Context cxt = (Context) Arg.getServiceModel(args);
		if (cxt == null && returnPath != null) {
			cxt = returnPath.getDataContext();
		}
		Mogram result = null;
		try {
			if (mog != null && cxt == null) {
				if (serviceType.providerType == RemoteServiceShell.class) {
					Exerter prv = (Exerter) Accessor.get().getService(sig(RemoteServiceShell.class));
					result = prv.exert(mog, null, new Arg[] {});
				} else {
					if (mog.getProcessSignature() != null
							&& ((ServiceSignature) mog.getProcessSignature()).isShellRemote()) {
						Exerter prv = null;
						prv = (Exerter) Accessor.get().getService(sig(RemoteServiceShell.class));
						result = prv.exert(mog, null);
					} else {
						result = (exert(mog));
					}
				}
			} else if (cxt != null) {
				Context out = null;
				ReturnPath rp = returnPath;
				if (rp == null) {
					rp = (ReturnPath)cxt.getReturnPath();;
				}
				if (rp != null && rp.path != null) {
					cxt.setReturnPath(rp);
					out = exert(task(this, cxt));
					return out.getValue(rp.path);
				}
				out = exert(task(this, cxt));
				return out;
			}
		} catch (Exception ex) {
			throw new MogramException(ex);
		}
		return context(result);
	}
}
