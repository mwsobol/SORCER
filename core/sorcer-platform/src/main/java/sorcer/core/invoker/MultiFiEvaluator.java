/*
 * Copyright 2019 the original author or authors.
 * Copyright 2019 SorcerSoft.org.
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
import sorcer.service.*;
import sorcer.service.modeling.Data;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Represents a multifidelity signature.
 *
 * Created by Mike Sobolewski
 */
public class MultiFiEvaluator<T> extends MultiFiSlot<String, T> implements Evaluator<T> {

	public MultiFiEvaluator(Evaluation... evaluators) {
		multiFi = new Fidelity(evaluators);
		multiFi.setSelect(evaluators[0]);
		impl = evaluators[0];
	}

	@Override
	public T evaluate(Arg... args) throws EvaluationException, RemoteException {
		if (multiFi != null) {
			try {
				List<Fidelity> fis = Arg.selectFidelities(args);
				if (fis.size() > 0) {
					multiFi.selectSelect(fis.get(0).getName());
					isValid = false;
					((Evaluator)impl).setValid(false);
				}
				impl = multiFi.getSelect();
				key = ((Identifiable)impl).getName();
				if (((Evaluation)impl).getContextReturn() == null && contextReturn != null) {
					((Evaluation)impl).setContextReturn(contextReturn);
				}
				if (impl instanceof Invocation) {
					Context cxt = Arg.selectContext(args);
					if (cxt == null && contextReturn != null && contextReturn.getDataContext() != null){
						cxt = contextReturn.getDataContext();
					}
					if (cxt == null && scope != null) {
                        ((ServiceInvoker)impl).setInvokeContext(scope);
                    }
					return (T) ((Invocation) impl).invoke(cxt, args);
				} else {
					return (T) ((Evaluation) impl).evaluate(args);
				}
			} catch (ServiceException | ConfigurationException | RemoteException e) {
				throw new EvaluationException(e);
			}
		} else {
			throw new EvaluationException("misconfigured MultiEvaluation with multiFi: " + multiFi);
		}
	}

	@Override
	public void addArgs(ArgSet set) throws EvaluationException, RemoteException {

	}

	@Override
	public ArgSet getArgs() {
		return null;
	}

	@Override
	public void setParameterTypes(Class<?>[] types) {

	}

	@Override
	public void setParameters(Object... args) {

	}

	@Override
	public void update(Setup... entries) throws ContextException {

	}

	@Override
	public void setNegative(boolean negative) {
		((Evaluation)impl).setNegative(negative);
	}

	@Override
	public void setName(String name) {
		key = name;
	}

	@Override
	public void substitute(Arg... args) throws SetterException, RemoteException {
		scope.substitute(args);
	}

	@Override
	public Data act(Arg... args) throws ServiceException, RemoteException {
		return null;
	}

	@Override
	public Data act(String entryName, Arg... args) throws ServiceException, RemoteException {
		return null;
	}

	@Override
	public <T extends Mogram> T exert(T mogram, Transaction txn, Arg... args) throws MogramException, RemoteException {
		Context cxt = Arg.selectContext(args);
		if (cxt == null) {
			cxt = new ServiceContext();
		}
		Object out = ((Evaluation)multiFi.getSelect()).evaluate(args);
		if (out instanceof Context) {
			cxt.append((Context) out);
		} else {
			cxt.putValue(getName(), out);
		}
		return (T)out;
	}
}
