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

package sorcer.service.modeling;

import sorcer.service.Arg;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;
import sorcer.service.modeling.EvaluationComponent;
import sorcer.service.modeling.SupportComponent;

import java.rmi.RemoteException;


/**
 * The functionality for all value objects.
 * 
 * @author Mike Sobolewski
 */
public interface Valuation<T> extends Entrance<T> {

	/**
	 * Returns the current value of this valuation.
	 * 
	 * @return the current value of this valuation
	 */
	public T value();

	/**
	 * Sets the current value of this valuation.
	 **/
	public void set(T value) ;
}
