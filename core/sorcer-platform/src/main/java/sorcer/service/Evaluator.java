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

import java.rmi.RemoteException;


/**
 * A functionality required by all evaluators in SORCER.
 * 
 * @author Mike Sobolewski
 */
public interface  Evaluator <T> extends Evaluation<T> {

	
	/**
	 * Returns the current eval of this evaluation.
	 * 
	 * @return the current eval of this evaluation
	 * @throws EvaluationException
	 * @throws RemoteException
	 */
	public T evaluate(Arg... entries) throws EvaluationException, RemoteException;
	
	
	public void addArgs(ArgSet set) throws EvaluationException, RemoteException;
	
	public ArgSet getArgs();
	
	public void setParameterTypes(Class<?>[] types);
	
	public void setParameters(Object... args);

	public void setValueIsCurrent(boolean state);

	public void update(Setup... entries) throws ContextException;

	public void setNegative(boolean negative);
}
