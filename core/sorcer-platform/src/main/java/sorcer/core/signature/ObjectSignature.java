/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.exertion.ObjectTask;
import sorcer.core.invoker.MethodInvoker;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.service.*;
import sorcer.service.modeling.Modeling;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;

import static sorcer.eo.operator.context;
import static sorcer.eo.operator.task;

public class ObjectSignature extends ServiceSignature {

	static final long serialVersionUID = 8042346568722803852L;

	private MethodInvoker evaluator;

	private Object target;

	private Signature targetSignature;

	// list of initialization arguments for the constructor
	private Object[] args;

	private String initSelector;

	private Class<?>[] argTypes;

	private static Logger logger = LoggerFactory.getLogger(ObjectSignature.class);

	public ObjectSignature() {
		this.providerType = Object.class;
	}

	public ObjectSignature(ServiceSignature signature) throws SignatureException {
        this.name = signature.name;
        this.operation = signature.operation;
        this.providerName =  signature.providerName;
        this.serviceType = signature.serviceType;
        this.providerType = signature.providerType;
        this.returnPath = signature.returnPath;
	}

	public ObjectSignature(String selector, Object object, Class<?>[] argTypes,
						   Object... args) throws InstantiationException,
			IllegalAccessException {
		this(selector, object, null, argTypes, args);
	}

	public ObjectSignature(Object object, String initSelector, Class<?>[] argTypes,
						   Object... args) throws InstantiationException,
			IllegalAccessException {
		this(null, object, initSelector, argTypes, args);
	}

	public ObjectSignature(String selector, Object object, String initSelector, Class<?>[] argTypes,
						   Object... args) throws InstantiationException,
			IllegalAccessException {
		this();
		if (object instanceof Class) {
			this.serviceType.providerType = (Class<?>) object;
			this.providerType = (Class<?>) object;
		} else if (object instanceof Signature) {
			targetSignature = (Signature)object;
		} else {
			target = object;
		}

		setSelector(selector);
		setInitSelector(initSelector);

		this.argTypes = argTypes;
		if (args != null && args.length > 0)
			this.args = args;
	}

	public ObjectSignature(String selector, Class<?> providerClass) {
		this.serviceType.providerType = providerClass;
		this.providerType = providerClass;
		setSelector(selector);
	}

	public ObjectSignature(Class<?> providerClass) {
		this(null, providerClass);
	}

	/**
	 * <p>
	 * Returns the object being a provider of this signature.
	 * </p>
	 *
	 * @return the object provider
	 */
	public Object getTarget() {
		return target;
	}

	/**
	 * <p>
	 * Assigns the object being a provider of this signature.
	 * </p>
	 *
	 * @param target
	 *            the  object provider to setValue
	 */
	public void setTarget(Object target) {
		this.target = target;
	}

	/**
	 * <p>
	 * Returns a provider class for this signature.
	 * </p>
	 *
	 * @return the providerClass
	 */
	public Class<?> getProviderType() {
		return providerType;
	}

	/**
	 * <p>
	 * Assigns a provider class for this signature.
	 * </p>
	 */
	public void setProviderType(Class<?> providerType) {
		this.providerType = providerType;
	}

	/**
	 <p> Returns the evaluator for this signature. </p>

	 @return the evaluation
	 */
	public MethodInvoker getEvaluator() {
		return evaluator;
	}

	/**
	 <p> Sets the evaluator for this signature. </p>

	 @param evaluator the evaluation to setValue
	 */
	public void setEvaluator(MethodInvoker evaluator) {
		this.evaluator = evaluator;
	}

	public MethodInvoker<?> createEvaluator() throws InstantiationException,
			IllegalAccessException {
		if (target == null && serviceType != null) {
			evaluator = new MethodInvoker(serviceType.providerType.newInstance(), operation.selector);
		} else
			evaluator = new MethodInvoker(target, operation.selector);
		this.evaluator.setParameters(args);
		return evaluator;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public Class<?>[] getParameterTypes() {
		return argTypes;
	}

	public void setParameterTypes(Class<?>... types) {
		argTypes = types;
	}

	/**
	 * Returns a new instance using a constructor as specified by this
	 * signature.
	 *
	 * @return a new instance
	 * @throws SignatureException
	 */
	public Object newInstance() throws SignatureException {
		Constructor<?> constructor = null;
		Object obj = null;
		try {
			if (args == null) {
				if (Modifier.isAbstract(serviceType.providerType.getModifiers()) ||
						serviceType.providerType.getConstructors().length == 0) {
					Method sm = serviceType.providerType.getMethod(initSelector, (Class[])null);
					obj = sm.invoke(serviceType, (Object[])null);
				} else {
					constructor = serviceType.providerType.getConstructor();
					obj = constructor.newInstance();
				}
			} else {
				constructor = serviceType.providerType.getConstructor(argTypes);
				obj = constructor.newInstance(args);
			}
		} catch (Exception e) {
			logger.error("newInstance failed", e);
			throw new SignatureException(e);
		}
		logger.debug(">>>>>>>>>>> instantiated: \n" + obj + "\n by signature: "
				+ this);
		return obj;
	}

	public Signature getTargetSignature() {
		return targetSignature;
	}

	public void setTargetSignature(Signature targetSignature) {
		this.targetSignature = targetSignature;
	}

	public Object newInstance(Object[] args) throws SignatureException {
		this.args = args;
		return newInstance();
	}

	public Object newInstance(Object[] args, Class<?>[] argClasses)
			throws SignatureException {
		this.args = args;
		this.argTypes = argClasses;
		return newInstance();
	}

	/**
	 * Returns a new instance using initialization by the instance or class method as
	 * specified by this signature.
	 *
	 * @return a new instance
	 * @throws SignatureException
	 */
	public Object initInstance() throws SignatureException {
		Object obj = null;
		Method m = null;

		try {
			if(operation.selector!=null) {
				try {
					Method selectorMethod = providerType.getDeclaredMethod(operation.selector, argTypes);
					if(Modifier.isStatic(selectorMethod.getModifiers())) {
						return  selectorMethod.invoke(null, args);
					}
				} catch (NoSuchMethodException e) {
					//skip;
				}
			}
			if ((initSelector == null || initSelector.equals("new")) && args == null) {
				obj = providerType.newInstance();
				return obj;
			}

			if (argTypes != null) {
				if (initSelector != null)
					m = providerType.getMethod(initSelector, argTypes);
				else if (operation.selector != null)
					m = providerType.getMethod(operation.selector, argTypes);
			} else  {
				if (initSelector != null)
					m = providerType.getMethod(initSelector);
				else
					m = providerType.getMethod(operation.selector);
			}
			if (args != null) {
				obj = m.invoke(obj, args);
			} else if (argTypes != null && argTypes.length == 1) {
				obj = m.invoke(obj, new Object[] { null });
			} else {
				obj = m.invoke(obj);
			}
		} catch (Exception e) {
			logger.error("initInstance failed", e);
			try {
				// check if that is SORCER service bean signature
				m = providerType.getMethod(operation.selector, Context.class);
				if (m.getReturnType() == Context.class)
					return obj;
				else
					throw new SignatureException(e);
			} catch (Exception e1) {
				logger.error("initInstance failed #2", e);
				throw new SignatureException(e);
			}
		}
		// logger.debug(">>>>>>>>>>> instantiated: \n" + obj +
		// "\n by signature: " + this);
		target = obj;
		return obj;
	}

	public String getInitSelector() {
		return initSelector;
	}

	public void setInitSelector(String initSelector) {
		this.initSelector = initSelector;
	}

	public Object initInstance(Object[] args) throws SignatureException {
		this.args = args;
		return initInstance();
	}

	public Object initInstance(Object[] args, Class<?>[] argClasses)
			throws SignatureException {
		this.args = args;
		this.argTypes = argClasses;
		return initInstance();
	}

	public Object build() throws SignatureException {
		return build(null);
	}

	public Object build(Context<?> inContext) throws SignatureException {
		Object obj;
		if ((operation.selector == null && initSelector == null)
				|| (operation.selector != null && operation.selector.equals("new"))
				|| (initSelector != null && initSelector.equals("new"))) {
			obj = newInstance();
		} else {
			obj = initInstance();
		}

		if (obj instanceof Modeling) {
			try {
				((Modeling)obj).isolateModel(inContext);
				((Modeling)obj).setContext(inContext);
				((Modeling)obj).initializeBuilder();
			} catch (ContextException e) {
				logger.error("Isolation failed", e);
				throw new SignatureException(e);
			}
		}
		return obj;
	}

	@Override
	public Context exert(Mogram mogram) throws TransactionException,
			MogramException, RemoteException {
		return exert(mogram, null);
	}

	public Context exert(Mogram mogram, Transaction txn) throws TransactionException,
			MogramException, RemoteException {
		Context cxt = null;
		ObjectTask task = null;
		if (mogram instanceof Context)
			cxt = (Context)mogram;
		else
			cxt = context(mogram.exert());

		try {
			task = new ObjectTask(this, cxt);
		} catch (SignatureException e) {
			throw new MogramException(e);
		}
		return task.exert(txn).getContext();
	}

	@Override
	public Object exec(Arg... args) throws MogramException, RemoteException, TransactionException {
		Mogram mog = Arg.getMogram(args);
		if (mog == null && returnPath != null) {
			mog = returnPath.getDataContext();
		}
		Context out = null;
		if (mog != null) {
			if (serviceType.providerType == ServiceShell.class) {
				ServiceShell shell = new ServiceShell(mog);
				out = context(shell.exert(args));
			} else if (mog instanceof Context) {
				argTypes = new Class[]{Context.class};
				ReturnPath rp = returnPath;
				if (rp == null) {
					rp = (ReturnPath) ((Context) mog).getReturnPath();
				}
				if (rp != null && rp.path != null) {
					((Context) mog).setReturnPath(rp);
					out = exert(task(this, mog));
					return out.getValue(rp.path);
				}
				out = exert(task(this, mog));
			}
		} else {
				out = exert(task(this));
		}
		return out;
	}

	public String toString() {
		return this.getClass() + ";" + execType + ";"
				+ (providerType == null ? "" : providerType + ";") + operation.selector
				+ (prefix !=null ? "#" + prefix : "")
				+ (returnPath != null ? ";"  + "result " + returnPath : "");
	}
}
