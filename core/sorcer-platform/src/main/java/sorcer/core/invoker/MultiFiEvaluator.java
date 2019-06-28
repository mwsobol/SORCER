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

import sorcer.service.*;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Represents a multifidelity signature.
 *
 * Created by Mike Sobolewski
 */
public class MultiFiEvaluator<T> extends MultiFiSlot<String, Evaluation<T> > implements Evaluation<T> {

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
				}
				impl = multiFi.getSelect();
				key = ((Identifiable)impl).getName();
				if (((Evaluation)impl).getContextReturn() == null && contextReturn != null) {
					((Signature)impl).setContextReturn(contextReturn);
				}
				return (T) ((Evaluation)impl).evaluate(args);
			} catch (ServiceException | ConfigurationException | RemoteException e) {
				throw new EvaluationException(e);
			}
		} else {
			throw new EvaluationException("misconfigured MultiEvaluation with multiFi: " + multiFi);
		}
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
}
