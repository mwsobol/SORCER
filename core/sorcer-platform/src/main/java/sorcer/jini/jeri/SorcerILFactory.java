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

package sorcer.jini.jeri;

import net.jini.core.constraint.MethodConstraints;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.core.transaction.Transaction;
import net.jini.jeri.*;
import net.jini.security.proxytrust.ProxyTrust;
import net.jini.security.proxytrust.ServerProxyTrust;
import net.jini.security.proxytrust.TrustEquivalence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import sorcer.core.provider.Modeler;
import sorcer.core.provider.RemoteServiceShell;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.service.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.Remote;
import java.rmi.server.ExportException;
import java.security.Permission;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static sorcer.core.SorcerConstants.*;

/**
 * A SorcerILFactory can be used with object interfaces as its services. Those
 * services exposed as interfaces do need implement Remote. A
 * {@link sorcer.core.provider.ServiceProvider} or
 * service beans using this factory should tell
 * the factory what objects should be exposed as services and the invocation
 * dispatcher created by this factory will manage the delegation of the method
 * calls to the right exposed object. SORCER service beans (objects with methods
 * taking a parameter {@link sorcer.service.Context} and returning
 * {@link sorcer.service.Context} can be used transparently as
 * {@link Service}s with either
 * {@link sorcer.core.provider.ServiceProvider}
 */

/**
 * @author Mike Sobolewski
 */
public class SorcerILFactory extends BasicILFactory {
    protected final Logger logger = LoggerFactory.getLogger(SorcerILFactory.class);
    /**
     * Exposed service type map. A key is an interface and a value its
     * implementing object.
     */
    protected final Map<Class<?>, Object> serviceBeanMap = new ConcurrentHashMap<>();

    /**
     * Creates a <code>SorcerILFactory</code> instance with no server
     * constraints, no permission class, a <code>null</code> class loader and
     * service beans.
     */
    public SorcerILFactory() {
        super();
    }

    /**
     * Creates a <code>SorcerILFactory</code> instance with no server
     * constraints, no permission class, and a <code>null</code> class loader.
     *
     * @param serviceBeans the objects to be exposed as services by the dispatcher of
     *                     this ILFactory
     */
    public SorcerILFactory(Map<Class<?>, Object> serviceBeans, ClassLoader loader) {
        super(null, null, loader);
        setServiceBeans(serviceBeans);
    }

    /**
     * Creates a <code>SorcerILFactory</code> with the specified server
     * constraints, permission class, and a <code>null</code> class loader.
     *
     * @param serverConstraints the server constraints, or <code>null</code>
     * @param permissionClass   the permission class, or <code>null</code>
     * @param serviceBeans      the objects to be exposed as services by the dispatcher of
     *                          this ILFactory
     * @throws IllegalArgumentException if the permission class is abstract, is not a subclass of
     *                                  {@link Permission}, or does not have a public constructor
     *                                  that has either one <code>String</code> parameter or one
     *                                  {@link Method}parameter and has no declared exceptions
     */
    public SorcerILFactory(MethodConstraints serverConstraints,
                           Class permissionClass,
                           Map<Class<?>, Object> serviceBeans) throws IllegalArgumentException {
        super(serverConstraints, permissionClass, null);
        setServiceBeans(serviceBeans);
    }

    /**
     * Creates a <code>SorcerILFactory</code> with the specified server
     * constraints, permission class, and class loader. The server constraints,
     * if not <code>null</code>, are used to enforce minimum constraints for
     * remote calls. The permission class, if not <code>null</code>, is used to
     * perform server-side access control on incoming remote calls. The class
     * loader, which may be <code>null</code>, is passed to the superclass
     * constructor and is used by the {@link #createInstances createInstances}
     * method.
     *
     * @param serverConstraints the server constraints, or <code>null</code>
     * @param permissionClass   the permission class, or <code>null</code>
     * @param loader            the class loader, or <code>null</code>
     * @param serviceBeans      the objects to be exposed as services by the dispatcher of
     *                          this ILFactory
     * @throws IllegalArgumentException if the permission class is abstract, is not a subclass of
     *                                  {@link Permission}, or does not have a public constructor
     *                                  that has either one <code>String</code> parameter or one
     *                                  {@link Method}parameter and has no declared exceptions
     */
    public SorcerILFactory(MethodConstraints serverConstraints,
                           Class permissionClass,
                           ClassLoader loader,
                           Map<Class<?>, Object> serviceBeans) throws IllegalArgumentException {
        super(serverConstraints, permissionClass, loader);
        setServiceBeans(serviceBeans);
    }

    private void setServiceBeans(Map<Class<?>, Object> serviceBeans) {
        serviceBeanMap.putAll(serviceBeans);
        if (logger.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Class<?>, Object> entry : serviceBeans.entrySet()) {
                if (sb.length() > 0)
                    sb.append("\n");
                sb.append("    ").append(entry.getKey().getName()).append(": ").append(entry.getValue()
                                                                                           .getClass()
                                                                                           .getName());
            }
            logger.trace("Created SorcerILFactory with\n{}", sb.toString());
        }
    }

    /**
     * Returns a new array containing any additional interfaces that the proxy
     * should implement, beyond the interfaces obtained by passing
     * <code>impl</code> to the {@link #getRemoteInterfaces getRemoteInterfaces}
     * method.
     * <p>
     * <p>
     * <code>SorcerILFactory</code> implements this method to return a new array
     * containing the interfaces of all services to be exposed and
     * {@link RemoteMethodControl}and {@link TrustEquivalence}interfaces, in
     * that order.
     *
     * @throws NullPointerException {@inheritDoc}
     */
    protected Class[] getExtraProxyInterfaces(Remote impl) throws NullPointerException {
        if (impl == null) {
            throw new NullPointerException("impl is null");
        }

        List<Class<?>> exposedInterfaces = new ArrayList<>();
        for (Class<?> curr : serviceBeanMap.keySet()) {
            if (curr != null && !exposedInterfaces.contains(curr))
                exposedInterfaces.add(curr);
        }
        exposedInterfaces.add(Service.class);
        exposedInterfaces.add(RemoteMethodControl.class);
        exposedInterfaces.add(TrustEquivalence.class);
        // exposedInterfaces.add(net.jini.admin.Administrable.class);

        Class[] clazzes = new Class[exposedInterfaces.size()];
        for (int i = 0; i < clazzes.length; i++) {
            clazzes[i] = exposedInterfaces.get(i);
        }
        return clazzes;
    }

    @Override
    protected InvocationDispatcher createInvocationDispatcher(Collection methods,
                                                              Remote impl,
                                                              ServerCapabilities caps) throws ExportException {
        return new SorcerInvocationDispatcher(methods, caps, getServerConstraints(), getPermissionClass(), getClassLoader());
    }

    @Override protected Collection getInvocationDispatcherMethods(Remote impl) throws ExportException {
        Set<Method> methods = new HashSet<>();
        methods.addAll(super.getInvocationDispatcherMethods(impl));
        for (Class<?> c : serviceBeanMap.keySet()) {
            Collections.addAll(methods, c.getDeclaredMethods());
        }
        return methods;
    }

    private String formatMethodList(Collection<Method> l) {
        StringBuilder s = new StringBuilder();
        for (Method m : l) {
            if (s.length() > 0)
                s.append("\n");
            s.append("    ").append(m.getName());
        }
        return s.toString();
    }

    /**
     * Our custom dispatcher to be used for exporting SORCER service beans.
     */
    private class SorcerInvocationDispatcher extends BasicInvocationDispatcher {
        Logger logger = LoggerFactory.getLogger(SorcerInvocationDispatcher.class);

        public SorcerInvocationDispatcher(Collection methods,
                                          ServerCapabilities serverCapabilities,
                                          MethodConstraints serverConstraints,
                                          Class<?> permissionClass,
                                          ClassLoader loader) throws ExportException {
            super(methods, serverCapabilities, serverConstraints, permissionClass, loader);
            if (logger.isTraceEnabled())
                logger.trace("Created SorcerInvocationDispatcher");
        }

        @Override
        protected Object invoke(Remote impl, Method method, Object[] args, Collection context) throws Throwable {
            try {
                setupLogging(impl, args);
                return doInvoke(impl, method, args, context);
            } catch (Throwable t) {
                logger.error("Failed", t);
                throw t;
            } finally {
                cleanLogging();
            }
        }

        private void setupLogging(Remote impl, Object[] args) {
            if (remoteLogging)
                MDC.put(MDC_SORCER_REMOTE_CALL, MDC_SORCER_REMOTE_CALL);
            if (impl instanceof Identifiable) {
                Identifiable identifiable = (Identifiable) impl;
                MDC.put(MDC_PROVIDER_ID, identifiable.getId().toString());
                MDC.put(MDC_PROVIDER_NAME, identifiable.getName());
            }
            if (args.length > 0 && args[0] instanceof Exertion) {
                Exertion xrt = ((Exertion) args[0]);
                if (xrt != null && xrt.getId() != null)
                    MDC.put(MDC_MOGRAM_ID, xrt.getId().toString());
            }
        }

        private void cleanLogging() {
            MDC.remove(MDC_PROVIDER_NAME);
            MDC.remove(MDC_SORCER_REMOTE_CALL);
            MDC.remove(MDC_MOGRAM_ID);
            MDC.remove(MDC_PROVIDER_ID);
        }


        protected Object doInvoke(Remote impl,
                                  Method method,
                                  Object[] args,
                                  Collection context) throws Throwable {
            if (logger.isTraceEnabled())
                logger.trace("Invoke {}", method);
            if (impl == null || args == null || context == null)
                throw new NullPointerException();

            if (!method.isAccessible() &&
                !(Modifier.isPublic(method.getDeclaringClass().getModifiers()) &&
                  Modifier.isPublic(method.getModifiers())))
                throw new IllegalArgumentException("method not public or set accessible");

            Class decl = method.getDeclaringClass();
            if (decl == ProxyTrust.class &&
                method.getName().equals("getProxyVerifier") &&
                impl instanceof ServerProxyTrust) {
                if (args.length != 0)
                    throw new IllegalArgumentException("incorrect arguments");

                return ((ServerProxyTrust) impl).getProxyVerifier();
            }
            Object obj;
            try {
                // handle context management by the containing provider
                if (decl == ContextManagement.class) {
                    obj = method.invoke(impl, args);
                    return obj;
                }

                Object service;
                if (args.length > 0 && isSorcerType(args[0])) {
                    if (logger.isTraceEnabled())
                        logger.trace("Process Sorcer type for {}", args[0].getClass().getName());
                    service = serviceBeanMap.get(((Exertion) args[0]).getProcessSignature().getServiceType());
                    if (service != null) {
                        if (logger.isTraceEnabled())
                            logger.trace("Service determined to be {}", service.getClass().getName());
                        obj = method.invoke(service, args);
                    } else {
                        service = serviceBeanMap.get(RemoteServiceShell.class);
                        if (logger.isTraceEnabled())
                            logger.trace("ServiceShell exertion\nargs[0]: {}\nargs[1]: {}\nargs[2]: {}",
                                         args[0], args[1], args[2]);
                        obj = ((ServiceShell) service).exert((Mogram) args[0], (Transaction) args[1], (Arg[]) args[2]);
                    }
                } else {
                    if (logger.isTraceEnabled())
                        logger.trace("{} declaring class {}", method.getName(), method.getDeclaringClass().getName());
                    service = getBean(method.getDeclaringClass());
                    if (service != null) {
                        if (logger.isTraceEnabled()) {
                            String indent = "    ";
                            logger.trace("Process bean invocation for\n{}{}\n{}{}",
                                         indent, method, indent, service.getClass().getName());
                        }
                        obj = method.invoke(service, args);
                    } else {
                        if (logger.isTraceEnabled()) {
                            String indent = "    ";
                            logger.trace("Process bean invocation for\n{}{}\n{}{}",
                                         indent, method, indent, impl.getClass().getName());
                        }
                        obj = method.invoke(impl, args);
                    }
                }
            } catch (Throwable t) {
                logger.error("SorcerInvocationDispatcher failed", t);
                throw new ExertionException(t);
            }
            return obj;
        }
    }

    Object getBean(Class<?> declaringClass) {
        Object bean = serviceBeanMap.get(declaringClass);
        if (bean == null) {
            for (Map.Entry<Class<?>, Object> entry : serviceBeanMap.entrySet()) {
                if (logger.isTraceEnabled())
                    logger.trace("Check if {} is assignable from {}", declaringClass.getName(), entry.getKey().getName());
                if (declaringClass.isAssignableFrom(entry.getKey())) {
                    bean = entry.getValue();
                    if (logger.isTraceEnabled())
                        logger.trace("{} is assignable from {}", declaringClass.getName(), entry.getKey().getName());
                    break;
                }
            }
            if (bean != null) {
                serviceBeanMap.put(declaringClass, bean);
            }
        }
        return bean;
    }

    private static boolean isSorcerType(Object target) {
        if (target instanceof Exertion) {
            Class serviceType = ((Exertion) target).getProcessSignature().getServiceType();
            if (target instanceof CompoundExertion
                || Modeler.class.isAssignableFrom(serviceType)
//					|| Modeling.class.isAssignableFrom(serviceType)
                || Evaluation.class.isAssignableFrom(serviceType)
                || Invocation.class.isAssignableFrom(serviceType))
                return true;
            else
                return false;
        } else {
            return false;
        }
    }

    private boolean remoteLogging = true;

    public void setRemoteLogging(boolean remoteLogging) {
        this.remoteLogging = remoteLogging;
    }
}
