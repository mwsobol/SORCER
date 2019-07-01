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

import sorcer.service.*;

import java.rmi.RemoteException;

/**
 * The loop Invoker invokes its target while its condition is true. Other types
 * of looping types depend on parameters provided as described for each
 * LoopInvoker constructor.
 * 
 * @author Mike Sobolewski
 * 
 * @param <V>
 */
public class LoopInvoker<V> extends ServiceInvoker<V> implements ConditionalInvocation {

	private int min = 0;

	private int max = 0;

	protected Condition condition;

	protected ServiceInvoker<V> target;

	/**
	 * Loop: while(true) { operand }
	 * 
	 * @param name
	 */
	public LoopInvoker(String name, ServiceInvoker<V> invoker) {
		super(name);
		condition = new Condition(true);
		target = invoker;
	}

	/**
	 * Iteration: for i = n to m { operand }
	 * 
	 * @param name
	 * @param min
	 * @param max
	 */
	public LoopInvoker(String name, int min, int max, ServiceInvoker<V> invoker) {
		super(name);
		this.min = min;
		this.max = max;
		target = invoker;
	}

	/**
	 * Loop: while (condition) { operand }
	 * 
	 * @param name
	 * @param condition
	 */
	public LoopInvoker(String name, Condition condition, Invocation<V> invoker) {
		super(name);
		this.condition = condition;
		target = (ServiceInvoker)invoker;
		invokeContext = ((ServiceInvoker) invoker).getInvokeContext();
	}

	/**
	 * The var loop operation is as follows: loop min times, then while
	 * condition is true, loop (max - min) times.
	 * 
	 * @param name
	 * @param min
	 * @param max
	 * @param condition
	 */
	public LoopInvoker(String name, int min, int max, Condition condition,
			ServiceInvoker<V> invoker) {
		super(name);
		this.min = min;
		this.max = max;
		this.condition = condition;
		target = invoker;
	}

	@Override
	public V evaluate(Arg... args) throws EvaluationException, RemoteException {
		V obj = null;
		try {
			if (condition == null) {
				for (int i = 0; i < max - min; i++) {
					obj = target.evaluate(args);
				}
				return obj;
			} else if (condition != null && max - min == 0) {
				target.setInvokeContext(invokeContext);
				if (condition.getConditionalContext() == null
						|| condition.getConditionalContext().size()==0) {
					condition.setConditionalContext(invokeContext);
				}
				while (condition.isTrue()) {
					obj = target.evaluate(args);
				}
			} else if (condition != null && max - min > 0) {
				// exert min times
				for (int i = 0; i < min; i++) {
					obj = target.evaluate(args);
				}
				for (int i = 0; i < max - min; i++) {
					obj = target.evaluate(args);
					if (condition.isTrue())
						obj = target.evaluate(args);
					else
						return obj;
				}
			}
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
		return obj;
	}

	@Override
	public Condition getCondition() {
		return condition;
	}
}
