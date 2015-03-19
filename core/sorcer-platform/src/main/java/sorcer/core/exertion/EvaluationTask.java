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

import java.util.Map;

import net.jini.core.transaction.Transaction;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.par.Par;
import sorcer.core.signature.EvaluationSignature;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.Arg;
import sorcer.service.ArgSet;
import sorcer.service.Context;
import sorcer.service.Evaluation;
import sorcer.service.Evaluator;
import sorcer.service.ExertionException;
import sorcer.service.Identifiable;
import sorcer.service.Scopable;
import sorcer.service.Setter;
import sorcer.service.SignatureException;
import sorcer.service.Task;

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

	public EvaluationTask(Evaluation evaluator) {
		super(((Identifiable) evaluator).getName());
		EvaluationSignature es = new EvaluationSignature(evaluator);
		addSignature(es);
		es.setEvaluator(evaluator);
		dataContext.setExertion(this);
		if (es.getEvaluator() instanceof Par) {
			((Par) es.getEvaluator()).setScope(dataContext);
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
	public Task doTask(Transaction txn) throws ExertionException,
			SignatureException {
		((ServiceContext)dataContext).setCurrentSelector(getProcessSignature().getSelector());
		((ServiceContext)dataContext).setCurrentPrefix(((ServiceSignature)getProcessSignature()).getPrefix());

		if (fidelity.size() > 1) {
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
			if (evaluator instanceof Evaluator) {
				ArgSet vs = ((Evaluator) evaluator).getArgs();
				Object args = dataContext.getArgs();
				Object val;
				if (args != null && args instanceof Map) {
					for (Arg v : vs) {
						val = ((Map<String, Object>) args).get(v.getName());
						if (val != null && (val instanceof Setter)) {
							((Setter)v).setValue(val);
						}
					}
				} else {
					if (vs != null)
						for (Arg v : vs) {
							val = dataContext.getValueEndsWith(v.getName());
							if (val != null && v instanceof Setter) {
								((Setter)v).setValue(val);
							}
						}
				}
			} else {
            if (evaluator instanceof Par && ((Par)evaluator).getScope()==null)
                ((Par)evaluator).setScope(dataContext);
            }
			Object result = evaluator.getValue();
			if (getProcessSignature().getReturnPath() != null)
				dataContext.setReturnPath(getProcessSignature().getReturnPath());
			dataContext.setReturnValue(result);
			if (evaluator instanceof Scopable) {
				((Context)((Scopable)evaluator).getScope()).putValue(dataContext.getReturnPath().path, result);
			}
		} catch (Exception e) {
			e.printStackTrace();
			dataContext.reportException(e);
		}
		dataContext.appendTrace("" + getEvaluation());
		return this;
	}

	public Evaluation getEvaluation() {
		EvaluationSignature es = (EvaluationSignature) getProcessSignature();
		return es.getEvaluator();
	}

}
