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
import java.util.NoSuchElementException;

import sorcer.service.Arg;
import sorcer.service.EvaluationException;
import sorcer.service.Incrementor;
import sorcer.service.Invocation;

/**
 * The incremental invoker with a double increment.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class InvokeDoubleIncrementor extends ServiceInvoker<Double> implements Incrementor<Double> {

	private static final long serialVersionUID = 892510700129101886L;
	
	protected double increment;

	protected Invocation<Double> target;
	
	public InvokeDoubleIncrementor(String name, Invocation invoker) {
		super(name);
		this.target = invoker;
	}

	public InvokeDoubleIncrementor(String name, Invocation invoker, double increment) {
		super(name);
		this.target = invoker;
		this.increment = increment;
	}

	@Override
	public Double getValue(Arg... entries) throws EvaluationException {
		try {
			if (value == null)
				value = target.invoke(null, entries);
			value = value + increment;

		} catch (RemoteException e) {
			throw new EvaluationException(e);
		}
		return value;
	}

	public double getIncrement() {
		return increment;
	}

	public Invocation getTarget() {
		return target;
	}
	
	public Incrementor reset() {
		value = null;
		return this;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Incrementor#next()
	 */
	@Override
	public Double next()  {
		try {
			return getValue();
		} catch (EvaluationException e) {
			throw new NoSuchElementException(e.getMessage());
		}
	}
}
