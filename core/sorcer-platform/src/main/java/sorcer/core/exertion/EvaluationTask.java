/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.signature.EvaluationSignature;
import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.Map;

/**
 * The SORCER evaluation task extending the basic task implementation
 * {@link Task}.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class EvaluationTask extends Task {

	static final long serialVersionUID = -3710507950682812041L;

	public EvaluationTask(String name) {
		super(name);
	}

	public EvaluationTask(Evaluation evaluator) throws ContextException, RemoteException {
		super(((Identifiable) evaluator).getName());
		EvaluationSignature es = new EvaluationSignature(evaluator);
		addSignature(es);
		es.setEvaluator(evaluator);
		dataContext.setExertion(this);
		if (es.getEvaluator() instanceof Par) {
			if (dataContext.getScope() == null)
				dataContext.setScope(new ParModel(name));
//			((Par) es.getEvaluator()).setScope(dataContext.getScope());
//			((Par) es.getEvaluator()).getScope().remove(((Par)es.getEvaluator()).key());
		}
	}

	public EvaluationTask(EvaluationSignature signature) {
		this(null, signature, null);
	}

	public EvaluationTask(String name, EvaluationSignature signature) {
		super(name);
		addSignature(signature);
	}

	public EvaluationTask(EvaluationSignature signature, Context context) {
		this(null, signature, context);
	}

	public EvaluationTask(String name, EvaluationSignature signature,
			Context context) {
		super(name);
		addSignature(signature);
		if (context != null) {
			if (signature.getEvaluator() instanceof Par) {
				((Par) signature.getEvaluator()).setScope(context);
			}
			setContext(context);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Task#doTask(net.jini.core.transaction.Transaction)
	 */
	@Override
	public Task doTask(Transaction txn, Arg... entries) throws ExertionException,
			SignatureException {
		dataContext.getModelStrategy().setCurrentSelector(getProcessSignature().getSelector());
		dataContext.setCurrentPrefix(getProcessSignature().getPrefix());

		if (serviceFidelity.getSelects().size() > 1) {
			try {
				return super.doBatchTask(txn);
			} catch (Exception e) {
				e.printStackTrace();
				throw new ExertionException(e);
			}
		}
		try {
			Evaluation evaluator = ((EvaluationSignature) getProcessSignature())
					.getEvaluator();
			if (evaluator instanceof Entry)
				((Entry)evaluator).isValid(false);

			if (evaluator instanceof Evaluator) {
				ArgSet vs = ((Evaluator) evaluator).getArgs();
				Object args = dataContext.getArgs();
				Object val;
				if (args != null && args instanceof Map) {
					for (Arg v : vs) {
						val = ((Map<String, Object>) args).get(v.getName());
						if (val != null && (val instanceof Setter)) {
							((Setter) v).setValue(val);
						}
					}
				} else {
					if (vs != null)
						for (Arg v : vs) {
							val = dataContext.getValueEndsWith(v.getName());
							if (val != null && v instanceof Setter) {
								((Setter) v).setValue(val);
							}
						}
				}
			} else {
				if (evaluator instanceof Par && dataContext.getScope() != null)
					((Par) evaluator).getScope().append(dataContext.getScope());
			}
			Object result = evaluator.getValue(entries);
			if (getProcessSignature().getReturnPath() != null)
				dataContext.setReturnPath(getProcessSignature().getReturnPath());
			dataContext.setReturnValue(result);
			if (evaluator instanceof Scopable) {
				(((Scopable)evaluator).getScope()).putValue(dataContext.getReturnPath().path, result);
			}
			if (evaluator instanceof Par && dataContext.getScope() != null)
				dataContext.getScope().putValue(((Par) evaluator).getName(), result);
		} catch (Exception e) {
			e.printStackTrace();
			dataContext.reportException(e);
		}
		dataContext.appendTrace("task" + getName() + " by: " + getEvaluation());
		return this;
	}

	public Evaluation getEvaluation() {
		EvaluationSignature es = (EvaluationSignature) getProcessSignature();
		return es.getEvaluator();
	}

}
