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
import sorcer.service.EvaluationException;
import sorcer.service.modeling.EvaluationComponent;
import sorcer.service.modeling.SupportComponent;

import java.rmi.RemoteException;


/**
 * A functionality for all object values of the constant function type.
 * 
 * @author Mike Sobolewski
 */
public interface Valuation<T> {

	/**
	 * Returns the current item of this valuation.
	 * 
	 * @return the current item of this valuation
	 */
	public T get() ;

	public void set(T item) ;
}
