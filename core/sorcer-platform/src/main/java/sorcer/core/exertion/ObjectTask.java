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

package sorcer.core.exertion;


import net.jini.core.transaction.Transaction;
import sorcer.core.context.ServiceContext;
import sorcer.core.invoker.MethodInvoker;
import sorcer.core.signature.ObjectSignature;
import sorcer.service.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import static sorcer.eo.operator.provider;

/**
 * The SORCER object task extending the basic task implementation {@link Task}.
 *
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class ObjectTask extends Task {

	static final long serialVersionUID = 1793342047789581449L;

	public ObjectTask() { }

	public ObjectTask(String name) {
		super(name);
	}

	public ObjectTask(String name, Signature... signatures) {
		this(name);
		for (Signature s : signatures) {
			if (s instanceof ObjectSignature)
				addSignature(s);
		}
	}

	public ObjectTask(String name, String description, Signature signature)
			throws SignatureException {
		this(name);
		if (signature instanceof ObjectSignature)
			addSignature(signature);
		else
			throw new SignatureException("Object task requires ObjectSignature: "
					+ signature);
		if (((ObjectSignature)signature).getEvaluator() == null)
			try {
				((ObjectSignature)signature).createEvaluator();
			} catch (Exception e) {
				e.printStackTrace();
				throw new SignatureException(e);
			}
		this.description = description;
	}

	public ObjectTask(String name, Signature signature, Context context)
			throws SignatureException {
		this(name, signature);
		this.dataContext = (ServiceContext) context;
	}

	public ObjectTask(Signature signature, Context context)
			throws SignatureException {
		addSignature(signature);
		if (context != null)
			this.dataContext = (ServiceContext) context;
	}

	@SuppressWarnings("unchecked")
	public Task doTask(Transaction txn) throws ExertionException, SignatureException, RemoteException, MogramException {
		MethodInvoker evaluator = null;
		ObjectSignature os = (ObjectSignature) getProcessSignature();
		dataContext.setCurrentSelector(os.getSelector());
		dataContext.setCurrentPrefix(os.getPrefix());
		try {
			if (getProcessSignature().getReturnPath() != null && getProcessSignature().getReturnPath().inPaths != null)
				dataContext.updateContext(getProcessSignature().getReturnPath().inPaths);
			else
				dataContext.updateContext();
//			dataContext = (ServiceContext)dataContext.getCurrentContext();
			if (dataContext.getArgs() != null)
				os.setArgs(dataContext.getArgs());
			if (dataContext.getParameterTypes() != null)
				os.setParameterTypes(dataContext.getParameterTypes());
			evaluator = ((ObjectSignature) getProcessSignature())
					.getEvaluator();
			Object result = null;
			if (evaluator == null) {
				// create a provider of this object signature
				Object prv = provider(os);
				Object target = os.getTarget();
				if (target != null) {
					if (target instanceof Method) {
						result = invokeMethod((Method)target, os);
					} else {
						evaluator = new MethodInvoker(target, os.getSelector());
					}
				}
				else {
					evaluator = new MethodInvoker(prv, os.getSelector());
				}
			}
			if (os.getReturnPath() != null)
				dataContext.setReturnPath(os.getReturnPath());

//			Context currentContext = dataContext;
			if (result == null) {
				if (getArgs() == null && os.getParameterTypes() == null) {
					// assume this task context is used by the signature's
					// provider
					if (dataContext != null) {
						evaluator
								.setParameterTypes(new Class[] { Context.class });
						evaluator.setContext(dataContext);
					}
				} else if (dataContext.getArgsPath() != null) {
					evaluator
							.setArgs(getParameterTypes(), (Object[]) getArgs());
				}
				// evaluator.setParameters(context);
				result = evaluator.evaluate();
			}

			if (result instanceof Context) {
				Signature.ReturnPath rp = dataContext.getReturnPath();
				if (rp != null) {
					if (((Context) result).getValue(rp.path) != null) {
						dataContext.setReturnValue(((Context) result).getValue(rp.path));
					} else if (rp.outPaths != null && rp.outPaths.length > 0) {
						Context out = dataContext.getSubcontext(rp.outPaths);
						dataContext.setReturnValue(out);
					}
				} else if (dataContext.getScope() != null) {
					dataContext.getScope().append((Context)result);
				}
			} else {
				dataContext.setReturnValue(result);
			}
			dataContext.updateContextWith(os.getOutConnector());
		} catch (Throwable e) {
			e.printStackTrace();
			dataContext.reportException(e);
			if (e instanceof Exception)
				setStatus(FAILED);
			else
				setStatus(ERROR);
		}
		setStatus(DONE);
		if (evaluator != null)
			dataContext.appendTrace(evaluator.toString());
		else
			dataContext.appendTrace(os.toString());

		return this;
	}

	private Object invokeMethod(Method method, ObjectSignature os)
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, ContextException, SignatureException {
		Object[] args = os.getArgs();
		Class<?>[] argTypes = os.getParameterTypes();
		Object result = null;
		if (args != null) {
			result = method.invoke(null, args);
		} else if (argTypes != null && argTypes.length == 1 && args == null) {
			result = method.invoke(null, new Object[] { null });
		} else {
			result = method.invoke(null, (Object[])null);
		}
		return result;
	}

	public Object getArgs() throws ContextException {
		return dataContext.getArgs();
	}

	public Class[]  getParameterTypes() throws ContextException {
		return dataContext.getParameterTypes();
	}
}
