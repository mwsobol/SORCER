/*
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

import net.jini.core.transaction.Transaction;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.Prc;
import sorcer.core.context.model.ent.Subroutine;
import sorcer.service.*;

import java.rmi.RemoteException;

/**
 * @author Mike Sobolewski
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ExertInvoker extends ServiceInvoker implements Invocation {
	private static final long serialVersionUID = -8257643691945276788L;
	private Routine exertion;
	private String path;
	private Routine evaluatedExertion;
	private Transaction txn;
	private Object updatedValue;

	{
		defaultName = "xrtInvoker-";
	}
	
	public ExertInvoker(String name, Routine exertion, String path, Prc... callEntries) {
		super(name);
		this.path = path;
		this.exertion = exertion;
		this.args = new ArgSet(callEntries);
	}

	public ExertInvoker(Routine exertion, String path, Prc... callEntries) {
		this(exertion.getName(), exertion, path, callEntries);
	}
	
	public ExertInvoker(Routine exertion, Prc... callEntries) {
		this(null, exertion, null, callEntries);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#execute(sorcer.service.Args[])
	 */
	@Override
	public Object evaluate(Arg... args) throws InvocationException,
			RemoteException {
		Context cxt = null;
		try {
			evaluatedExertion = exertion.exert(txn);
			Context.RequestReturn returnPath = ((ServiceContext)evaluatedExertion.getDataContext())
					.getRequestReturn();
			if (evaluatedExertion instanceof Job) {
				cxt = ((Job) evaluatedExertion).getJobContext();
			} else {
				cxt = evaluatedExertion.getContext();
			}

			if (returnPath != null) {
				if (returnPath.returnPath == null)
					return cxt;
				else if (returnPath.returnPath.equals(Signature.SELF))
					return this;
				else
					return cxt.getReturnValue();
			} else {
				if (path != null)
					return cxt.getValue(path);
			}
		} catch (Exception e) {
			throw new InvocationException(e);
		}
		return cxt;
	}
	
	public Routine getExertion() {
		return exertion;
	}

	public Routine getEvaluatedExertion() {
		return evaluatedExertion;
	}

	public void substitute(Subroutine... entries) throws SetterException,
			RemoteException {
		((ServiceRoutine)exertion).substitute(entries);
	}

	public Object getUpdatedValue() {
		return updatedValue;
	}

	public void setUpdatedValue(Object updatedValue) {
		this.updatedValue = updatedValue;
	}

}
