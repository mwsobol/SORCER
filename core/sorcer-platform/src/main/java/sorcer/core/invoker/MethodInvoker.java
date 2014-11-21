/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

package sorcer.core.invoker;

import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.par.Par;
import sorcer.service.*;
import sorcer.util.SorcerUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Logger;

/**
 * @author Mike Sobolewski
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MethodInvoker<T> extends ServiceInvoker<T> implements MethodInvoking<T> {

	private static final long serialVersionUID = -1158778636907725414L;

	final protected static Logger logger = Logger.getLogger(MethodInvoker.class
			.getName());

	protected String className;

	private String selector;

	private Class<?>[] paramTypes;

	private Object[] params;

	private Context context;

	transient private Method m;

	transient private URLClassLoader miLoader;

	private URL[] exportURL;

	// the object used in the constructor of the target object
	private Object initObject;

	protected Object target;

	protected static int count;

	{
		defaultName = "methodInvoker-";
	}

	public MethodInvoker() {
		name = "oi-" + count++;
	}

	public MethodInvoker(String name) {
		if (name != null)
			this.name = name;
		else
			this.name = "oe-" + count++;
	}

	public MethodInvoker(String name, String methodName) {
		this(name);
		selector = methodName;
	}

	public MethodInvoker(Object target, Par... pars) {
		this.target = target;
		this.pars = new ArgSet(pars);
	}

	public MethodInvoker(Object target, String methodName, Par... pars) {
		this(methodName, target, methodName);
		this.pars = new ArgSet(pars);
	}

	public MethodInvoker(String name, Object target, String methodName,
			Par... pars) {
		this(name);
		this.target = target;
		selector = methodName;
		this.pars = new ArgSet(pars);
	}

	public MethodInvoker(String name, String className, String methodName,
			Par... pars) {
		this(name, className, methodName, null, null, pars);
	}

	public MethodInvoker(String name, String className, String methodName,
			Class<?>[] signature, Par... pars) {
		this(name, className, methodName, signature, null, pars);
	}

	public MethodInvoker(String name, String className, String methodName,
			Class<?>[] paramTypes, String distributionParameter, Par... pars) {
		this(name);
		this.className = className;
		selector = methodName;
		this.paramTypes = paramTypes;
		this.pars = new ArgSet(pars);
		if (distributionParameter != null)
			params = new Object[] { distributionParameter };
	}

	public MethodInvoker(String name, URL exportUrl, String className,
			String methodName) {
		this(name);
		this.className = className;
		this.exportURL = new URL[] { exportUrl };
		selector = methodName;
	}

	public MethodInvoker(String name, URL exportUrl, String className,
			String methodName, Object initObject) {
		this(name, exportUrl, className, methodName);
		this.initObject = initObject;
	}

	public MethodInvoker(URL[] exportURL, String className, String methodName) {
		this.className = className;
		this.exportURL = exportURL;
		selector = methodName;
	}

	public MethodInvoker(URL[] exportURL, String className, String methodName,
			Object initObject) {
		this(exportURL, className, methodName);
		this.initObject = initObject;
	}

	public void setArgs(String methodName, Class<?>[] paramTypes,
			Object[] parameters) {
		selector = methodName;
		this.paramTypes = paramTypes;
		params = parameters;
	}

	public void setArgs(Class<?>[] paramTypes, Object[] parameters) {
		this.paramTypes = paramTypes;
		this.params = parameters;
	}

	public void setArgs(Object[] parameters) {
		params = parameters;
	}

	public void setSignatureTypes(Class<?>[] paramTypes) {
		this.paramTypes = paramTypes;
	}

	public void setSelector(String methodName) {
		selector = methodName;
	}

	@Override
	public T getValue(Arg... entries) throws EvaluationException,
			RemoteException {
		Object[] parameters = getParameters();
		Object val = null;
		Class<?> evalClass = null;

		try {
			if (target == null) {
				if (exportURL != null) {
					target = getInstance();
				} else if (className != null) {
					evalClass = Class.forName(className);

					Constructor<?> constructor;
					if (initObject != null) {
						constructor = evalClass
								.getConstructor(new Class[] { Object.class });
						target = constructor
								.newInstance(new Object[] { initObject });
					} else
						target = evalClass.newInstance();
				}
			} else {
				evalClass = target.getClass();
			}
			// if no paramTypes defined assume that the method name 'selector'
			// is unique
			if (paramTypes == null) {
				Method[] mts = evalClass.getDeclaredMethods();
				for (Method mt : mts) {
					if (mt.getName().equals(selector)) {
						m = mt;
						break;
					}
				}
			} else {
				if (selector == null) {
					Method[] mts = evalClass.getDeclaredMethods();
					if (mts.length == 1)
						m = mts[0];
				} else {
//					// exception when Arg... is not specified for the invoke
					if (target instanceof Invocation && paramTypes.length == 1 
								&&  paramTypes[0] == Context.class
									&& selector.equals("invoke"))	{
						paramTypes = new Class[2];
						paramTypes[0] = Context.class;
						paramTypes[1] = Arg[].class;		
						Object[] parameters2 = new Object[2];
						parameters2[0] = parameters[0];
						parameters2[1] = new Arg[0];
						parameters = parameters2;
					}
					m = evalClass.getMethod(selector, paramTypes);
					if (m == null) {
						Method[] mts = evalClass.getMethods();
						if (Context.class.isAssignableFrom(paramTypes[0])) {
							for (Method mt : mts) {
								if (mt.getName().equals(selector)) {
									m = mt;
									break;
								}
							}
						}
					}
				}
			}		
			if (context != null)
				((ServiceContext)context).setCurrentSelector(selector);
			val = m.invoke(target, parameters);
		} catch (Exception e) {
			logger.severe("**error in object invoker; target = " + target);
			System.out.println("class: " + evalClass);
			System.out.println("method: " + m);
			System.out.println("selector: " + selector);
			System.out.println("paramTypes: "
					+ (paramTypes == null ? "null" : SorcerUtil
							.arrayToString(paramTypes)));
			System.out.println("parameters: "
					+ (parameters == null ? "null" : SorcerUtil
							.arrayToString(parameters)));
			e.printStackTrace();
			throw new EvaluationException(e);
		}
		return (T) val;
	}

	Class<?>[] getParameterTypes() {
		return paramTypes;
	}

	private Object getInstance() {
		Object instanceObj = null;
		ClassLoader cl = this.getClass().getClassLoader();
		try {
			miLoader = URLClassLoader.newInstance(exportURL, cl);
			final Thread currentThread = Thread.currentThread();
			final ClassLoader parentLoader = (ClassLoader) AccessController
					.doPrivileged(new PrivilegedAction() {
						public Object run() {
							return (currentThread.getContextClassLoader());
						}
					});

			try {
				AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						currentThread.setContextClassLoader(miLoader);
						return (null);
					}
				});
				Class<?> clazz = null;
				try {
					clazz = miLoader.loadClass(className);
				} catch (ClassNotFoundException ex) {
					ex.printStackTrace();
					throw ex;
				}

				Constructor<?> constructor = clazz
						.getConstructor(new Class[] { Object.class });
				if (initObject != null) {
					instanceObj = constructor
							.newInstance(new Object[] { initObject });
				} else {
					instanceObj = clazz.newInstance();
				}
			} finally {
				AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						currentThread.setContextClassLoader(parentLoader);
						return (null);
					}
				});
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw new IllegalArgumentException(
					"Unable to instantiate method of this oject invoker: "
							+ e.getClass().getName() + ": "
							+ e.getLocalizedMessage());
		}
		return instanceObj;
	}

	public Object getInitObject() {
		return initObject;
	}

	public void setInitObject(Object initObject) {
		this.initObject = initObject;
	}

	public void setParameterTypes(Class<?>[] types) {
		paramTypes = types;
	}

	public void setParameters(Object... args) {
		params = args;
	}

	Object[] getParameters() throws EvaluationException {
//		 logger.info("params: " + SorcerUtil.arrayToString(params));
//		 logger.info("paramTypes: " + SorcerUtil.arrayToString(paramTypes));
//		 logger.info("context: " + context);
		if (context != null) {
			paramTypes = new Class[] { Context.class };
			params = new Object[] { context };
			return params;
		} else if (params != null) {
			try {
				if (params.length == 1 && params[0] instanceof Context) {
					params = (Object[]) ((ServiceContext) params[0]).getArgs();
				}
			} catch (ContextException e) {
				e.printStackTrace();
				throw new EvaluationException(e);
			}
			return params;
		}
		return params;
	}

	public String getClassName() {
		return className;
	}

	public Object getTarget() {
		return target;
	}

	public String getSelector() {
		return selector;
	}

	public String describe() {
		StringBuilder sb = new StringBuilder("\nObjectIvoker");
		sb.append(", class name: " + className);
		sb.append(", selector: " + selector);
		sb.append(", target: " + target);
		sb.append("\nargs: " + params);

		return sb.toString();
	}

	public void setContext(Context context) {
		this.context = context;
	}

}
