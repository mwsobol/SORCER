/*
 * Copyright 2014 the original author or authors.
 * Copyright 2014 SorcerSoft.org.
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

package sorcer.core.context;

import sorcer.service.*;
import sorcer.service.Domain;

import java.rmi.RemoteException;

/**
 * Copier copies values of args from one Context to another one.
 */
public class Copier implements Service, Evaluation<Context>, Identifiable {
	private String name;
	private Context fromContext;
	private Arg[] fromEntries;
	private Context toContext;
	private Arg[] toEntries;

	public Copier(Domain fromContext, Arg[] fromEntries, Domain toContext, Arg[] toEntries) throws EvaluationException {
		this.fromContext = (Context)fromContext;
		this.fromEntries = fromEntries;
		this.toContext = (Context)toContext;
		this.toEntries = toEntries;
		if (fromEntries.length != toEntries.length)
			throw new EvaluationException("Sizes of from and to arguments do not match");
	}

	@Override
	public Context asis() throws EvaluationException, RemoteException {
		return toContext;
	}

	@Override
	public Context evaluate(Arg... entries) throws EvaluationException, RemoteException {
		try {
			for (int i = 0; i < fromEntries.length; i++) {
				toContext.putValue(toEntries[i].getName(), fromContext.getValue(fromEntries[i].getName()));
			}
            ((ServiceContext)toContext).substitute(entries);
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
		return toContext;
	}

	@Override
	public void setNegative(boolean negative) {
		// do nothing
	}

	@Override
	public Context getScope() {
		return fromContext;
	}

	@Override
	public void setScope(Context context) {
		fromContext = context;
	}

	@Override
	public void substitute(Arg... entries) throws SetterException {
        ((ServiceContext)toContext).substitute(entries);
	}

	@Override
	public Object execute(Arg... args) throws MogramException, RemoteException {
		Domain cxt = Arg.selectDomain(args);
		if (cxt != null) {
			fromContext = (Context) cxt;
			return evaluate(args);
		}
		return null;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Object getId() {
		return name;
	}

	public String getName() {
		return name;
	}

}