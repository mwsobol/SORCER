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
import sorcer.core.context.ThrowableTrace;
import sorcer.core.context.model.srv.SrvModel;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.Signature.ReturnPath;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
//import sorcer.service.Condition;


/**
 * The loop Exertion executes its target exertion while its condition is true.
 * Other types of looping types depend on parameters provided as described for
 * each LoopExertion constructor.
 * 
 * @author Mike Sobolewski
 * 
 */
public class LoopMogram extends ConditionalMogram {

	private static final long serialVersionUID = 8538804142085766935L;
	
	private int min = 0;

	private int max = 0;

	/**
	 * Loop: while(true) { operand }
	 * 
	 * @param name
	 * @param exertion
	 */
	public LoopMogram(String name, Exertion exertion) {
		super(name);
		condition = new Condition(true);
		target = exertion;
	}

	/**
	 * Iteration: for i = n to m { operand }
	 * 
	 * @param name
	 * @param min
	 * @param max
	 * @param mogram
	 */
	public LoopMogram(String name, int min, int max, Mogram mogram) {
		super(name);
		this.min = min;
		this.max = max;
		target = mogram;
	}

	/**
	 * Loop: while (condition) { operand }
	 * 
	 * @param name
	 * @param condition
	 * @param mogram
	 */
	public LoopMogram(String name, Condition condition, Mogram mogram) {
		super(name);
		this.condition = condition;
		target = mogram;
	}

	/**
	 * The var loop operation is as follows: loop min times, then while
	 * condition is true, loop (max - min) times. (UML semantics of the loop operator)
	 * 
	 * @param name
	 * @param min
	 * @param max
	 * @param condition
	 * @param invoker
	 */
	public LoopMogram(String name, int min, int max, Condition condition,
					  Mogram invoker) {
		super(name);
		this.min = min;
		this.max = max;
		this.condition = condition;
		target = invoker;
	}

	@Override
	public Task doTask(Transaction txn, Arg... args) throws ExertionException,
			SignatureException, RemoteException {
		try {
			// update the scope of target
			if (target.getScope() == null) {
				target.setScope(scope);
			} else {
				target.getScope().append(scope);
			}

			ReturnPath rp = (ReturnPath)target.getContext().getReturnPath();

			if (condition == null) {
				for (int i = 0; i < max - min; i++) {
					target = target.exert(txn);
					if (rp != null && rp.path != null) {
						scope.putValue(target.getName(), target.getContext().getReturnValue());
					}
				}
				return this;
			} else if (condition != null && max - min == 0) {
				if (target instanceof Model) {
					Context cxt = condition.getConditionalContext();
					condition.setConditionalContext((Context) target);
					if (cxt != null && cxt.size() > 0) {
						((Context) target).append(cxt);
					}
				}
				while (condition.isTrue()) {
					if (target instanceof Exertion) {
						Signature sig = target.getProcessSignature();
						if (sig != null && sig.getVariability() != null) {
							target.getContext().append(condition.getConditionalContext());
						}
						target = target.exert(txn, args);
						if (sig != null && sig.getVariability() != null) {
							((Task) target).updateConditionalContext(condition);
						}
					} else {
						if (target instanceof SrvModel)
							((SrvModel)target).clearOutputs();
						target = target.exert(txn, args);
					}
				}
			} else if (condition != null && max - min > 0) {
				// exert min times
				for (int i = 0; i < min; i++) {
					target = target.exert(txn, args);
				}
				for (int i = 0; i < max - min; i++) {
					target = target.exert(txn);
					if (condition.isTrue())
						target = target.exert(txn, args);
					else
						return this;
				}
			}
		} catch (Exception e) {
			throw new ExertionException(e);
		}
		return this;
	}
	
	public boolean isConditional() {
		return true;
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
	
	public List<Mogram> getMograms(List<Mogram> exs) {
		exs.add(target);
		exs.add(this);
		return exs;
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
