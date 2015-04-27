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
public class OptExertion extends Task implements ConditionalExertion {

	private static final long serialVersionUID = 172930501527871L;

	protected Condition condition;

	protected Exertion target;
	
	protected boolean isActive = false;
	
	public OptExertion(String name) {
		super(name);
	}
		
	public OptExertion(String name, Exertion exertion) {
		super(name);
		this.condition = new Condition(true);
		this.target = exertion;
	}
	public OptExertion(Condition condition, Exertion exertion) {
		super();
		this.condition = condition;
		this.target = exertion;
	}
	
	public OptExertion(String name, Condition condition, Exertion exertion) {
		super(name);
		this.condition = condition;
		this.target = exertion;
	}

	public Exertion getTarget() {
		return target;
	}

	public void setTarget(Exertion exertion) {
		this.target = exertion;
	}
	
	public Task doTask(Transaction txn) throws ExertionException,
			SignatureException, RemoteException {
		try {

			if (condition.isTrue()) {
				isActive = true;
				target = target.exert(txn);
//				if (target.getScope() != null) {
//					((Context) target.getScope()).append(dataContext);
//				} else {
//					target.setScope(dataContext);
//				}
				dataContext = (ServiceContext) target.getDataContext();
				controlContext.append(target.getControlContext());
				dataContext.putValue(Condition.CONDITION_VALUE, true);
				dataContext.putValue(Condition.CONDITION_TARGET, target.getName());
				target.getContext().setExertion(null);
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
	
	public Condition getCondition() {
		return condition;
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
		exceptions.addAll(target.getExceptions());
		exceptions.addAll(this.getExceptions());
		return exceptions;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.ConditionalExertion#getTargets()
	 */
	@Override
	public List<Exertion> getTargets() {
		List<Exertion> tl = new ArrayList<Exertion>();
		tl.add(target);
		return tl;
	}
}
