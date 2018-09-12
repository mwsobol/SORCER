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

import sorcer.service.Arg;
import sorcer.service.EvaluationException;
import sorcer.service.Incrementor;
import sorcer.service.Invocation;

import java.util.NoSuchElementException;

/**
 * The incremental invoke with an int increment.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class IncrementInvoker<T> extends ServiceInvoker<T> implements Incrementor<T> {

    private static final long serialVersionUID = 6556962121786495504L;

	protected T increment;

	protected String path;

	protected Invocation<T> target;

	public IncrementInvoker(String path) {
		this.path = path;
	}

	public IncrementInvoker(String path, T increment) {
		this.path = path;
		this.increment = increment;
	}

	public IncrementInvoker(String path, T increment, T value) {
		this.path = path;
		this.increment = increment;
		this.value = value;
	}

	public IncrementInvoker(Invocation invoker, T increment) {
		this.target = invoker;
		this.increment = increment;
	}
	public IncrementInvoker(String name, Invocation invoker) {
		super(name);
		this.target = invoker;
	}

	public IncrementInvoker(String name, Invocation invoker, T increment) {
		super(name);
		this.target = invoker;
		this.increment = increment;
	}

	@Override
	public T evaluate(Arg... args) throws EvaluationException {
		try {
			if (value == null && target != null)
				value = target.invoke(null, args);
			else if (path != null && target == null && invokeContext != null) {
					T val = (T) invokeContext.getValue(path);
					if (val != null) value = val;
			}
			value = getIncrement(value, increment);
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
		return value;
	}

	protected abstract T getIncrement(T value, T increment) throws EvaluationException;

	public Invocation getTarget() {
		return target;
	}

	public void setTarget(Invocation<T> target) {
		this.target = target;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Incrementor reset() {
		value = null;
		return this;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Incrementor#next()
	 */
	@Override
	public T next()  {
		try {
			return evaluate();
		} catch (Exception e) {
			throw new NoSuchElementException(e.getMessage());
		}
	}
}
