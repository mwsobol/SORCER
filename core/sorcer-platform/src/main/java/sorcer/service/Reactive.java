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

package sorcer.service;

import java.rmi.RemoteException;

/**
 * @author Mike Sobolewski
 */
public interface Reactive<T> {

	/**
	 * Returns the current value of this evaluation. The current value can be
	 * exiting value with no need to evaluate it if it's still valid.
	 * 
	 * @return the current value of this evaluation
	 * @throws EvaluationException
	 * @throws RemoteException
	 */
	public boolean isReactive();

	/**
	 * Assigns repeatedly reactive feature, that can be reversed any time.
	 *
	 * @param isReactive
	 * @return
	 */
	public Reactive<T> setReactive(boolean isReactive);


}
