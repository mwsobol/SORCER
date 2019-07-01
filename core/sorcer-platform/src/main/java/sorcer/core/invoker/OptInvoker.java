/*
 *
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

import java.rmi.RemoteException;

import sorcer.service.*;
import sorcer.service.Condition;

/**
 * The option Var. There is a single target invoker that executes if the condition is true (like if... then).
 * 
 * @author Mike Sobolewski
 */
public class OptInvoker<T> extends ServiceInvoker<T> implements ConditionalInvocation {
	
	protected Condition condition;
	
	protected ServiceInvoker<T> target;

	protected T value;
	
	public OptInvoker(String name) {
		super(name);
	}

	public OptInvoker(T value) {
		this(null, value);
	}

	public OptInvoker(String name, T value) {
		this(name);
		this.value = value;
	}

	public OptInvoker(String name, ServiceInvoker<T> invoker) {
		super(name);
		this.condition = new Condition(true);
		this.target = invoker;
	}
	
	public OptInvoker(String name, Condition condition, ServiceInvoker<T> invoker) {
		super(name);
		this.condition = condition;
		this.target = invoker;
	}

	public ServiceInvoker<T> getTarget() {
		return target;
	}

	public void setTarget(ServiceInvoker<T> target) {
		this.target = target;
	}

	@Override
	public T evaluate(Arg... args) throws EvaluationException, RemoteException {
		try {
			if (value != null) {
				return value;
			}
			checkInvokeContext();
			if (condition == null || condition.isTrue()) {
				if (target.isFunctional) {
					return (T) target;
				} else {
					return target.evaluate(args);
				}
			} else {
				return null;
			}
		} catch (ContextException e) {
			throw new InvocationException(e);
		}
	}
	
	public T invoke(Arg... entries) throws RemoteException, InvocationException {
		if (value != null) {
			return value;
		}
		try {
			checkInvokeContext();
			if (condition.isTrue())
				return target.evaluate(entries);
			else 
				return null;
		} catch (ContextException e) {
			throw new InvocationException(e);
		}
	}

	public T invoke(Context context, Arg... args)
			throws RemoteException, InvocationException {
		try {
			checkInvokeContext();
			if (condition.isTrue())
				return target.invoke(context, args);
			else 
				return null;
		} catch (ContextException e) {
			throw new InvocationException(e);
		}
	}

	public boolean isTrue() throws ContextException  {
		if (condition == null) {
			return true;
		} else {
			return condition.isTrue();
		}
	}

	private void checkInvokeContext() throws RemoteException, ContextException {
		if (target.getInvokeContext().size() == 0 && invokeContext.size() > 0) {
			target.setInvokeContext(invokeContext);
		}
		if (scope != null && target.getScope() == null) {
			target.setScope(scope);
		}
	}

	@Override
	public Condition getCondition() {
		return condition;
	}
}
