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

package sorcer.core.exertion;

import net.jini.core.transaction.Transaction;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.ThrowableTrace;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * The option Exertion. There is a single target exertion that executes if the
 * condition is true (like if... then).
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class OptMogram extends ConditionalMogram {

	private static final long serialVersionUID = 172930501527871L;
	
	protected boolean isActive = false;
	
	public OptMogram(String name) {
		super(name);
	}
		
	public OptMogram(String name, Exertion exertion) {
		super(name);
		this.condition = new Condition(true);
		this.target = exertion;
	}
	public OptMogram(Condition condition, Exertion exertion) {
		super();
		this.condition = condition;
		this.target = exertion;
	}
	
	public OptMogram(String name, Condition condition, Exertion exertion) {
		super(name);
		this.condition = condition;
		this.target = exertion;
	}

	public Task doTask(Transaction txn, Arg... args) throws ExertionException,
			SignatureException, RemoteException {
		try {

			if (condition.isTrue()) {
				isActive = true;
				target = target.exert(txn, args);
//				if (target.getScope() != null) {
//					((Context) target.getScope()).append(dataContext);
//				} else {
//					target.setScope(dataContext);
//				}
				dataContext = (ServiceContext) target.getDataContext();
				if (target instanceof Exertion) {
					target.getContext().setExertion(null);
					controlContext.append(((Exertion)target).getControlContext());
				}
				dataContext.putValue(Condition.CONDITION_VALUE, true);
				dataContext.putValue(Condition.CONDITION_TARGET, target.getName());

				dataContext.setExertion(null);
				return this;
			} else {
				dataContext.putValue(Condition.CONDITION_VALUE, false);
				dataContext.putValue(Condition.CONDITION_TARGET, target.getName());
				return this;
			}
		} catch (Exception e) {
			throw new ExertionException(e);
		}
	}
		
	public boolean isActive() {
		return isActive;
	}

	public boolean isConditional() {
		return true;
	}

	public void reset(int state) {
		((ServiceExertion)target).reset(state);
		this.setStatus(state);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Conditional#getConditions()
	 */
	@Override
	public List<Conditional> getConditions() {
		List<Conditional> cs = new ArrayList<Conditional>();
		if (condition != null)
			cs.add(condition);
		return cs;
	}
	
	public List<Mogram> getMograms(List<Mogram> exs) {
		exs.add(target);
		exs.add(this);
		return exs;
	}
	
	@Override
	public List<ThrowableTrace> getExceptions(List<ThrowableTrace> exceptions) {
		try {
			exceptions.addAll(target.getExceptions());
		} catch (RemoteException e) {
			exceptions.add(new ThrowableTrace("Problem while collecting exceptions", e));
		}
		exceptions.addAll(this.getExceptions());
		return exceptions;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.ConditionalExertion#getTargets()
	 */
	@Override
	public List<Mogram> getTargets() {
		List<Mogram> tl = new ArrayList<Mogram>();
		tl.add(target);
		return tl;
	}
}
