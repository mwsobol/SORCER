/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.proxy;

import com.sun.jini.admin.DestroyAdmin;
import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.id.ReferentUuid;
import net.jini.id.Uuid;
import net.jini.security.proxytrust.SingletonProxyTrustIterator;
import org.rioproject.admin.ServiceActivityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import sorcer.core.provider.AdministratableProvider;
import sorcer.core.provider.Provider;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Service provider should wrap up the smart proxy/stub. The
 * java.lang.reflect.Proxy returned from this object will have the following
 * features. If the server implements RemoteMethodControl, the proxy returned
 * would implement the functionality of Jini2.0(TM) Security and semantics to
 * ReferentUuid. If not it implements the semantics of ReferentUuid.
 * 
 * Functionality of Jini2.0 Security implemented by this ProxyWrapper Ability to
 * set and get client constraints Ability to getProxyTrustIterator
 * 
 * Functionality of ReferentUuid The unique identifier assigned to the current
 * instance of this proxy class by the lookup discovery service. This ID is used
 * to determine equality between proxies.
 *  * 
 * @author Mike Sobolewski
 */

@SuppressWarnings("rawtypes")
public class ProviderProxy implements Serializable {
	private static final long serialVersionUID = -242006752320266252L;
	protected final static Logger logger = LoggerFactory.getLogger(ProviderProxy.class);

	/**
	 * Public static factory method that creates and returns an instance of
	 * <code>java.lang.reflect.Proxy</code>. This proxy will implement the
	 * semantics of ReferentUuid and jini2.0 security semantics if the server
	 * passed onto the createServiceProxy method will implement
	 * RemoteMethodControl.
	 * 
	 * @param proxy
	 *            reference to the server object through which communication
	 *            occurs between the client-side and server-side of the
	 *            associated service.
	 * @param proxyID
	 *            the unique identifier assigned by the service to each instance
	 *            of this proxy
	 * 
	 * @return an instance of <code>java.lang.reflect.Proxy</code> that
	 *         implements <code>RemoteMethodControl</code> if the given
	 *         <code>server</code> does.
	 */
	public static Object wrapServiceProxy(Object proxy,
                                          Uuid proxyID,
                                          Object adminProxy,
                                          Class... additionalInterfaces) {

		if (proxy == null)
			throw new NullPointerException("Cannot have a server which is null");

		ReferentUuidInvocationHandler handler =
                (proxy instanceof RemoteMethodControl) ?
                        new ConstrainableInvocationHandler(proxy, proxyID, adminProxy) :
                        new ReferentUuidInvocationHandler(proxy, proxyID, adminProxy);

        return Proxy.newProxyInstance(proxy.getClass().getClassLoader(),
                                      handler.getInterfaces(proxy, additionalInterfaces),
                                      handler);
    }
	
	public static Object wrapAdminProxy(Object adminProxy, Uuid adminProxyID, Class... additionalInterfaces) {

		if (adminProxy == null)
			throw new NullPointerException("Cannot have an admin server which is null");

		ReferentUuidInvocationHandler handler =
                (adminProxy instanceof RemoteMethodControl) ?
                        new ConstrainableInvocationHandler(adminProxy, adminProxyID, adminProxy) :
                        new ReferentUuidInvocationHandler(adminProxy, adminProxyID, adminProxy);

        return Proxy.newProxyInstance(adminProxy.getClass().getClassLoader(),
                                      handler.getInterfaces(adminProxy, additionalInterfaces),
                                      handler);
    }
	
	private static class ReferentUuidInvocationHandler implements InvocationHandler, Serializable {
		private static final long serialVersionUID = 242006752320266247L;
		protected final Object proxy;
		protected final Uuid proxyID;
		protected final Object adminProxy;

		public ReferentUuidInvocationHandler(Object proxy,
                                             Uuid proxyID,
                                             Object adminProxy) {
			this.proxy = proxy;
			this.proxyID = proxyID;
			this.adminProxy = adminProxy;
		}

		public Class[] getInterfaces(Object proxy, Class... additionalInterfaces) {
			List<Class> list = new ArrayList<>();
			if (proxy == adminProxy) {
				// admin interfaces
				list.add(ReferentUuid.class);
				list.add(DestroyAdmin.class);
				list.add(JoinAdmin.class);
//				list.add(ServiceActivityProvider.class);
				list.add(RemoteMethodControl.class);
			} else {
				// provider interfaces
				Class[] interfaces = proxy.getClass().getInterfaces();
				for (Class c : interfaces) {
					if (!list.contains(c))
						list.add(c);
				}
                if (!list.contains(ReferentUuid.class))
					list.add(ReferentUuid.class);

				// for smart proxies add Administrable
				if (!proxy.getClass().isInterface())
					list.add(Administrable.class);
				// used by Rio provisioning
				if (!list.contains(ServiceActivityProvider.class))
					list.add(ServiceActivityProvider.class);
				if (list.contains(Provider.class)) {
					list.remove(AdministratableProvider.class);
				}
			}
			for (Class c : additionalInterfaces) {
				if (!list.contains(c))
					list.add(c);
			}
			return list.toArray(new Class[list.size()]);
		}

        public Object invoke(Object server, Method m, Object[] args) throws Throwable {
			String selector = m.getName();
            if ("getReferentUuid".equals(selector))
                return proxyID;
            if ("hashCode".equals(selector)) {
                return proxyID.hashCode();
            } else if ("equals".equals(selector)) {
                return !(args.length != 1 || !(args[0] instanceof ReferentUuid)) && proxyID.equals(((ReferentUuid) args[0]).getReferentUuid());
            } else if ("toString".equals(selector)) {
				return "refID=" + proxyID + " : proxy=" + proxy;
			}

            try {
            	return doInvoke(server, selector, m, args);
            } catch (InvocationTargetException ie) {
                Throwable cause = ie.getCause();
                // disable logging for calls coming from ServiceCataloger
                if (cause instanceof ConnectException && MDC.get("java.net.ConnectException.ignore") == null) {
                    logger.warn("Proxy Connection problem to : {} to perform: {} for args: {}", proxyID, m, Arrays.toString(args), ie);
                }
                for (Class<?> throwableType : m.getExceptionTypes()) {
                    if(throwableType.isInstance(cause))
                        throw cause;
                }
                throw ie;
            } catch (Throwable e) {
                // this block is for debugging, can be deleted
                // do not report broken network connection on destruction or getAdmin after the service is
				// undeployed by Rio
				logger.warn("Caught while invoking {}", selector, e);
                if (!selector.equals("getAdmin"))
					logger.warn("proxy method: {} for args: {}", m, Arrays.toString(args), e);
                if (!(selector.equals("destroyNode") || selector.equals("destroy"))) {
                    throw e;
                } else
                    return null;
            }
        }

        protected Object doInvoke(Object server, String selector, Method m, Object[] args) throws IllegalAccessException, InvocationTargetException, RemoteException {
            try {
                if(logger.isTraceEnabled()) {
					String indent = "    ";
					logger.trace("Invoking\n{}{}", indent, m);
				}
                return m.invoke(proxy, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error("Failed invoking {}", m, e);
                throw e;
            }
        }

        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            doReadObject(s);
		}

        protected void doReadObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
			s.defaultReadObject();
			/* Verify server */
			if (proxy == null) {
				throw new InvalidObjectException("ProviderProxy.readObject "
						+ "failure - server " + "field is null");
			}// endif
			/* Verify proxyID */
			if (proxyID == null) {
				throw new InvalidObjectException("ProviderProxy.readObject "
						+ "failure - proxyID " + "field is null");
			}
			// endif
		}// end readObject

		private void readObjectNoData() throws InvalidObjectException {
			throw new InvalidObjectException(
					"no data found when attempting to "
							+ "deserialize ProviderProxy instance");
		}// end readObjectNoData
	}

	private static class ConstrainableInvocationHandler extends ReferentUuidInvocationHandler {
		private static final long serialVersionUID = -1L;

		public ConstrainableInvocationHandler(Object server,
                                              Uuid proxyID,
                                              Object adminProxy) {
			super(server, proxyID, adminProxy);
		}

		@Override
		public Class[] getInterfaces(Object proxy, Class... additionalInterfaces) {
			Class[] interfaces = Arrays.copyOf(additionalInterfaces, additionalInterfaces.length + 1);
			interfaces[additionalInterfaces.length] = RemoteMethodControl.class;
			return super.getInterfaces(proxy, interfaces);
		}

		public Object doInvoke(Object server, String selector, Method m, Object[] args) throws RemoteException, InvocationTargetException, IllegalAccessException {
			if ("getConstraints".equals(selector)) {
				return ((RemoteMethodControl) proxy).getConstraints();
			} else if ("setConstraints".equals(selector)) {
				return server;
			} else if ("getProxyTrustIterator".equals(selector)) {
				return new SingletonProxyTrustIterator(server);
			} else if ("isActive".equals(selector)) {
				return ((Provider) proxy).isBusy();
			} else {
				return super.doInvoke(server, selector, m, args);
			}
		}

		private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
			doReadObject(s);
		}// end readObject

		private void readObjectNoData() throws InvalidObjectException {
			throw new InvalidObjectException("no data found when attempting to deserialize ProviderProxy instance");
		}// end readObjectNoData
	}

}
