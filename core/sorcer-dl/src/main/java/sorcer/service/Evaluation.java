/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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

package sorcer.service;

import sorcer.service.modeling.EvaluationComponent;
import sorcer.service.modeling.SupportComponent;

import java.rmi.RemoteException;


/**
 * A functionality required by all evaluations in SORCER.
 * 
 * @author Mike Sobolewski
 */
public interface  Evaluation <T> extends Substitutable, Scopable, EvaluationComponent, SupportComponent {

	/**
	 * Returns the current execute of this evaluation. The current execute can be
	 * exiting execute with no need to evaluate it if it's still valid.
	 *
	 * @return the current execute of this evaluation
	 * @throws EvaluationException
	 * @throws RemoteException
	 */
	public T evaluate(Arg... args) throws EvaluationException, RemoteException;

	/**
	 * Returns the execute of the existing execute of this evaluation that might be invalid.
	 * 
	 * @return the execute as is
	 * @throws EvaluationException
	 * @throws RemoteException
	 */
	public Object asis() throws EvaluationException, RemoteException;

	/**
	 * Returns a Context.Return to the return execute by this signature.
	 *
	 * @return Context.Return to the return execute
	 */
	public Context.Return getContextReturn();

	/**
	 * Assigns a request return of this signature with a given return path.
	 *
	 * @param contextReturn
	 * 			a context return
	 */
	public void setContextReturn(Context.Return contextReturn);

	public void setNegative(boolean negative);

	public void setName(String name);

}
