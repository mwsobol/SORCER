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
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.par.Par;
import sorcer.service.*;
import sorcer.service.Signature.ReturnPath;
import java.rmi.RemoteException;

/**
 * @author Mike Sobolewski
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ExertInvoker extends ServiceInvoker implements ExertionInvoking {
	private static final long serialVersionUID = -8257643691945276788L;
	private Exertion exertion;
	private String path;
	private Exertion evaluatedExertion;
	private Transaction txn;
	private Object updatedValue;

	{
		defaultName = "xrtInvoker-";
	}
	
	public ExertInvoker(String name, Exertion exertion, String path, Par... parEntries) {
		super(name);
		this.path = path;
		this.exertion = exertion;
		this.pars = new ArgSet(parEntries);
	}

	public ExertInvoker(Exertion exertion, String path, Par... parEntries) {
		this(exertion.getName(), exertion, path, parEntries);
	}
	
	public ExertInvoker(Exertion exertion, Par... parEntries) {
		this(null, exertion, null, parEntries);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getValue(sorcer.service.Args[])
	 */
	@Override
	public Object getValue(Arg... entries) throws InvocationException,
			RemoteException {
		Context cxt = null;
		try {
			evaluatedExertion = exertion.exert(txn);
			ReturnPath returnPath = ((ServiceContext)evaluatedExertion.getDataContext())
					.getReturnPath();
			if (evaluatedExertion instanceof Job) {
				cxt = ((Job) evaluatedExertion).getJobContext();
			} else {
				cxt = evaluatedExertion.getContext();
			}

			if (returnPath != null) {
				if (returnPath.path == null)
					return cxt;
				else if (returnPath.path.equals(Signature.SELF))
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
	
	public Exertion getExertion() {
		return exertion;
	}

	public Exertion getEvaluatedExertion() {
		return evaluatedExertion;
	}

	public void substitute(Entry... entries) throws SetterException,
			RemoteException {
		((ServiceExertion)exertion).substitute(entries);
	}

	public Object getUpdatedValue() {
		return updatedValue;
	}

	public void setUpdatedValue(Object updatedValue) {
		this.updatedValue = updatedValue;
	}
}
