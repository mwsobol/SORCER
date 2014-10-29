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

import sorcer.service.Arg;
import sorcer.service.Condition;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;
import sorcer.service.InvocationException;

/**
 * The option Var. There is a single target invoker that executes if the condition is true (like if... then).
 * 
 * @author Mike Sobolewski
 */
public class OptInvoker<T> extends ServiceInvoker<T> {
	
	protected Condition condition;
	
	protected ServiceInvoker<T> target;
	
	public OptInvoker(String name) {
		super(name);
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
	public T getValue(Arg... entries) throws EvaluationException, RemoteException {
		try {
			checkInvokeContext();
			if (condition.isTrue())
				return target.getValue(entries);
			else 
				return null;
		} catch (ContextException e) {
			throw new InvocationException(e);
		}
	}
	
	public T invoke(Arg... entries) throws RemoteException,
	InvocationException {
		try {
			checkInvokeContext();
			if (condition.isTrue())
				return target.invoke(entries);
			else 
				return null;
		} catch (ContextException e) {
			throw new InvocationException(e);
		}
	}

	public T invoke(Context context, Arg... entries)
			throws RemoteException, InvocationException {
		try {
			checkInvokeContext();
			if (condition.isTrue())
				return target.invoke(context, entries);
			else 
				return null;
		} catch (ContextException e) {
			throw new InvocationException(e);
		}
	}
	
	private void checkInvokeContext() throws RemoteException, ContextException {
		if (target.getScope().size() == 0 && invokeContext.size() > 0) {
			target.setScope(invokeContext);
		}
	}
}
