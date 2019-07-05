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

package sorcer.service;

import sorcer.core.provider.ProviderName;
import sorcer.service.modeling.EvaluationComponent;
import sorcer.service.modeling.SupportComponent;
import sorcer.service.modeling.Functionality;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A service <code>Signature</code> is an indirect behavioral feature of
 * {@link Subroutine}s that declares a service that can be performed by instances
 * of {@link Service}s. It contains a service fiType and a selector of operation
 * of that service fiType (interface). Its implicit parameter and return execute is
 * a service {@link Context}. Thus, the explicit signature of service-oriented
 * operations is defined by the same {@link Context} fiType for any exertion
 * parameter and return execute . A signature may include a collection of optional
 * attributes describing a preferred {@link Service} with a given service fiType.
 * Also a signature can carry own implementation when its fiType is implemented
 * with the provided codebase.
 * <p>
 * In other words, a service signature is a specification of a service that can
 * be requested dynamically at the boundary of a service provider. Operations
 * include modifying a service {@link Context} or disclosing information about
 * the service context.
 *
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public interface Signature extends Opservice, Exertion, Comparable, Dependency, Identifiable,
		Arg, EvaluationComponent, SupportComponent, Serializable {

	/**
	 * Returns a name of this signature.
	 *
	 * @return name of signature
	 */
	public String getName();

	/**
	 * Returns an operation name of this signature.
	 *
	 * @return name of operation
	 */
	public String getSelector();

	/**
	 * Returns a fragment of operation of this signature. It's the part
	 * preceeding # in its selector.
	 *
	 * @return fragment of operation
	 */
	public String getPrefix();

	/**
	 * Returns a service provider name.
	 *
	 * @return name of service provider
	 */
	public ProviderName getProviderName();

	/**
	 * Returns a service provider.
	 *
	 * @return name of service provider
	 */
	public Object getProvider() throws SignatureException;

	/**
	 * Returns a provider of <code>Variability</code> fiType.
	 *
	 * @return Variability of this service provider
	 */
	public Functionality<?> getVariability();

	public void setProviderName(ProviderName providerName);

	/**
	 * Returns a service fiType name of this signature.
	 *
	 * @return name of service interface
	 */
	public Class getServiceType() throws SignatureException;

	/**
	 * Assigns a service type of this signature.
	 *
	 * @param serviceType
	 *            service serviceType
	 */
	public void setServiceType(Class serviceType);

	/**
	 * Returns a service multitype of this signature.
	 *
	 * @return service multitype
	 */
	public Multitype getMultitype() throws SignatureException;

	/**
	 * Assigns a service multi of this signature.
	 *
	 * @param multitype
	 *            service multitype
	 */
	public void setMultitype(Multitype multitype);

	/**
	 * Returns an array of service types of this signature
	 * to be matched by its service proxy.
	 *
	 * @return array of service types matched by service proxy
	 */
	public Class[] getMatchTypes();

	/**
	 * Assigns a request return of this signature with a given return path.
	 *
	 * @param contextReturn
	 * 			a context return
	 */
	public void setContextReturn(Context.Return contextReturn);

	public void setContextReturn(String path);

	public void setContextReturn();

	/**
	 * Assigns a request return to the return execute with a return path and directional attribute.
	 *
	 * @param path
	 *            the return path of the request return
	 * @param direction
	 *            the request return directional attribute
	 */
	public void setContextReturn(String path, Direction direction);

	/**
	 * Returns a Context.Return to the return execute by this signature.
	 *
	 * @return Context.Return to the return execute
	 */
	public Context.Return getContextReturn();

	/**
	 * Returns a signature Type of this signature.
	 *
	 * @return a Type of this signature
	 */
	public Type getExecType();

	/**
	 * Returns a inConnector specifying output paths for existing
	 * paths in returned context for this signature.
	 *
	 * @return a context mapping output paths to existing returnPath
	 */
	public Context getInConnector();

	/**
	 * Assigns a signature <code>fiType</code> for this service signature.
	 *
	 * @param type
	 *            a signature fiType
	 */
	public Signature setType(Signature.Type type);

	/**
	 * Returns a codebase for the code implementing this signature. The codebase
	 * is a space separated string (list) of URls.
	 *
	 * @return a codebase for the code implementing this signature
	 */
	public String getCodebase();

	/**
	 * Assigns a codbase to <code>urls</code> for the code implementing this
	 * signature. The codebase is a space separated string (list) of URls.
	 *
	 * @param urls
	 *            a list of space separated URLS
	 */
	public void setCodebase(String urls);

	/**
	 *  Close and connectivity to the bound service provider.
	 * @throws RemoteException
	 * @throws IOException
	 */
	public void close() throws RemoteException, IOException;

	/**
	 * Returns a deployment for provisioning a referenced service provider;
	 */
	public Deployment getDeployment();

	/**
	 * Returns an access types to a provider, synchronous (PUSH) or asynchronous (PULL);
	 */
	public Strategy.Access getAccessType();
	
	/**
	 * There are four types of {@link Signature} operations that can be
	 * associated with signatures: <code>PRE</code> (preprocess),
	 * <code>PROC</code> (process/service) , <code>POST</code> (postprocess), and
	 * <code>APD_DATA</code> (append data) and code>APD_CONTROL</code> (append
	 * control strategy). Only one <code>PROC</code> signature can be associated
	 * with any exertion. The <code>PROC</code> signature defines an executing
	 * provider dynamically bounded at runtime. The <code>APD_DATA</code>
	 * signatures are invoked invoked first to getValue specified contexts from
	 * {@link sorcer.service.Contexter}s that are appended to the task's current
	 * context.
	 */
	public enum Type implements Arg {
		PROC, PRE, POST, SRV, APD_DATA, APD_CONTROL, BUILDER;

		/* (non-Javadoc)
         * @see sorcer.service.Arg#getName()
         */
		@Override
		public String getName() {
			return toString();
		}

		public Object execute(Arg... args) {
			return this;
		}
	}

	/**
	 * Used to indicate if signature is active.
	 */
	public enum Operating implements Arg {
		YES, NO, TRUE, FALSE;

		/* (non-Javadoc)
		 * @see sorcer.service.Arg#getName()
		 */
		@Override
		public String getName() {
			return toString();
		}

		public Object execute(Arg... args) {
			return this;
		}
	}

	public enum Kind implements Arg {
		TASKER, JOBBER, SPACER, DISPATCHER, OPTIMIZER, EXPLORER, SOLVER, DRIVER, MODEL, MODEL_MANAGER;

		/* (non-Javadoc)
		 * @see sorcer.service.Arg#getName()
		 */
		@Override
		public String getName() {
			return toString();
		}

		public Object execute(Arg... args) {
			return this;
		}
	}

	public enum Direction implements Arg {
		IN, OUT, INOUT, FROM, TO;

		/* (non-Javadoc)
		 * @see sorcer.service.Arg#getName()
		 */
		@Override
		public String getName() {
			return toString();
		}

		static public Direction fromString(String direction) {
			if (direction == null) {
				return null;
			} else if (direction.equals(""+IN)) {
				return IN;
			} else if (direction.equals(""+OUT)) {
				return OUT;
			} else if (direction.equals(""+INOUT)) {
				return INOUT;
			} else {
				return null;
			}
		}

		public Object execute(Arg... args) {
			return this;
		}
	};

	static final String SELF = "_self_";
	static final String SELF_VALUE = "_self_value_";
	static final Type SRV = Type.PROC;
	static final Type PRE = Type.PRE;
	static final Type POST = Type.POST;
	static final Type APD = Type.APD_DATA;

	public static class Read extends Paths {
		private static final long serialVersionUID = 1L;

		public Read() {
			super();
		}

		public Read(Path[] paths) {
			for (Path path : paths) {
				add(path) ;
			}
		}
		public Read(String[] names) {
			for (String name : names) {
				add(new Path(name)) ;
			}
		}
	}

	public static class Write extends Paths {
		private static final long serialVersionUID = 1L;

		public Write() {
			super();
		}

		public Write(Path[] paths) {
			for (Path path : paths) {
				add(path) ;
			}
		}
		public Write(String[] names) {
			for (String name : names) {
				add(new Path(name)) ;
			}
		}
	}

	public static class State extends Paths {
		private static final long serialVersionUID = 1L;

		public State() {
			super();
		}

		public State(Path[] paths) {
			for (Path path : paths) {
				add(path) ;
			}
		}
		public State(String[] names) {
			for (String name : names) {
				add(new Path(name)) ;
			}
		}
	}


	public static class Append extends Paths {
		private static final long serialVersionUID = 1L;

		public Append() {
			super();
		}

		public Append(Path[] paths) {
			for (Path path : paths) {
				add(path) ;
			}
		}
		public Append(String[] names) {
			for (String name : names) {
				add(new Path(name)) ;
			}
		}
	}

	public static class SessionPaths extends ArrayList<Paths> implements Arg {
		private static final long serialVersionUID = 1L;

		public SessionPaths() {
			super();
		}

		public SessionPaths(Paths[] lists) {
			for (Paths al : lists) {
				add(al);
			}
		}

		public Paths getPaths(Class<?> clazz) {
			for(Paths al : this) {
				if (clazz.isInstance(al)) {
					return al;
				}
			}
			return null;
		}

		@Override
		public String getName() {
			return toString();
		}

		public Object execute(Arg... args) {
			return this;
		}
	}

	public static class Operation implements Serializable, Arg {
		static final long serialVersionUID = 1L;

		public String selector;

		public String path;

		public Strategy.Access accessType = Strategy.Access.PUSH;

		public Strategy.Flow flowType = Strategy.Flow.SEQ;

		public Strategy.Monitor toMonitor = Strategy.Monitor.NO;

		private List matchTokens;

		public Strategy.Wait toWait = Strategy.Wait.YES;

		public Strategy.FidelityManagement toManageFi = Strategy.FidelityManagement.NO;

		public Strategy.Shell isShellRemote = Strategy.Shell.LOCAL;

		public boolean isProvisionable = false;

		@Override
		public String getName() {
			return selector;
		}


		public List getMatchTokens() {
			return matchTokens;
		}

		public void setMatchTokens(List matchTokens) {
			this.matchTokens = matchTokens;
		}

		public Object execute(Arg... args) {
			return this;
		}
	}

	public static class Multitype implements Serializable, Arg {
		static final long serialVersionUID = 1L;

		public String typeName;

		// default prvType
		public Class providerType;

		// service types implemented by the service provider
		public Class[] matchTypes;

		public Multitype() {
			// do nothing
		}

		public Multitype(String className) {
			typeName = className;
		}

		public Multitype(Class classType) {
			providerType = classType;
		}

		@Override
		public String getName() {
			if (typeName != null) {
				return typeName;
			} else {
				return providerType.toString();
			}
		}

		public Class getProviderType() throws SignatureException {
			return getProviderType(null);
		}

		public Class getProviderType(ClassLoader loader) throws SignatureException {
			if (providerType != null) {
				return providerType;
			} else if (typeName != null) {
				try {
					if (loader == null)
						providerType = Class.forName(typeName);
					else
						providerType = Class.forName(typeName, true, loader);
				} catch (ClassNotFoundException e) {
					throw new SignatureException(e);
				}
			}
			return providerType;
		}

		@Override
		public String toString() {
			return (providerType != null ? providerType.getSimpleName() : "null")
					+ (matchTypes != null ? ":" + Arrays.toString(matchTypes) : "");
		}

		public Object execute(Arg... args) {
			return this;
		}
	}

}
