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
import java.util.Arrays;
import java.util.List;

/**
 * The alternative Exertion that executes sequentially a collection of optional
 * exertions. It executes the first optExertion in the collection such that its
 * condition is true.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
public class AltMogram extends ConditionalMogram {

	private static final long serialVersionUID = 4012356285896459828L;
	
	protected List<OptMogram> optExertions;

	public AltMogram(OptMogram... optExertions) {
		super();
		this.optExertions = Arrays.asList(optExertions);
	}
	
	public AltMogram(String name, OptMogram... optExertions) {
		super(name);
		this.optExertions = Arrays.asList(optExertions);
	}

	public AltMogram(String name, List<OptMogram> optExertions) {
		super(name);
		this.optExertions = optExertions;
	}

	@Override
	public Task doTask(Transaction txn, Arg... args) throws ExertionException,
			SignatureException, RemoteException {
		OptMogram opt = null;
		try {
			for (int i = 0; i < optExertions.size(); i++) {
				opt = optExertions.get(i);
				if (opt.getCondition().isTrue()) {
					opt.isActive = true;
					Context cxt = opt.getCondition().getConditionalContext();
					if (cxt != null) {
						Condition.clenupContextScripts(cxt);
							opt.getTarget().getDataContext().updateEntries(cxt);
					}
					Exertion out = opt.getTarget().exert(txn, args);
					opt.setTarget(out);
					dataContext = (ServiceContext) out.getContext();
					controlContext.append(out.getControlContext());
					dataContext.putValue(Condition.CONDITION_VALUE, true);
					dataContext.putValue(Condition.CONDITION_TARGET, opt.getName());
					return this;
				}
			}
			dataContext.putValue(Condition.CONDITION_VALUE, false);
			dataContext.putValue(Condition.CONDITION_TARGET, opt.getName());
			dataContext.setExertion(null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ExertionException(e);
		}
		return this;
	}
	
	public OptMogram getActiveOptExertion() {
		OptMogram active = null;
		for (OptMogram oe : optExertions) {
			if (oe.isActive())
				return oe;
		}
		return active;
	}
		
	public List<OptMogram> getOptExertions() {
		return optExertions;
	}

	public void setOptExertions(List<OptMogram> optExertions) {
		this.optExertions = optExertions;
	}

	public OptMogram getOptExertion(int index) {
		return optExertions.get(index);
	}
	
	public boolean isConditional() {
		return true;
	}
	
	public void reset(int state) {
			for (ServiceExertion e : optExertions)
				e.reset(state);
		
		this.setStatus(state);
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Conditional#getConditions()
	 */
	@Override
	public List<Conditional> getConditions() {
		List<Conditional> cs = new ArrayList<Conditional>(optExertions.size());
		for (OptMogram oe : optExertions)
			cs.add(oe.getCondition());
		return cs;
	}

	@Override
	public List<ThrowableTrace> getExceptions(List<ThrowableTrace> exceptions) {
		for (Exertion ext : optExertions) {
			exceptions.addAll(((ServiceExertion)ext).getExceptions(exceptions));
		}
		exceptions.addAll(this.getExceptions());
		return exceptions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Exertion#getMograms()
	 */
	@Override
	public List<Mogram> getMograms() {
		ArrayList<Mogram> list = new ArrayList<Mogram>(1);
		list.addAll(optExertions);
		return list;
	}
	
	public List<Mogram> getMograms(List<Mogram> exs) {
		for (Exertion e : optExertions)
			((ServiceExertion) e).getMograms(exs);
		exs.add(this);
		return exs;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.ConditionalExertion#getTargets()
	 */
	@Override
	public List<Mogram> getTargets() {
		List<Mogram> tl = new ArrayList<Mogram>(optExertions.size());
		for (OptMogram oe : optExertions)
			tl.add(oe.getTarget());
		return tl;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Exertion#isCompound()
	 */
	@Override
	public boolean isCompound() {
		return false;
	}

}
