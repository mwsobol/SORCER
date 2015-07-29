/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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
 * An top-level interface for any kinds of updates by this object.
 * 
 * @author Mike Sobolewski
 */
public interface Updatable {

	/**
	 * A generic request for required updates.
	 * 
	 * @param updates
	 *            required updates by this object
	 * @throws EvaluationException
	 *             if updates failed for any reason
	 * @throws RemoteException
	 */
	public boolean update(Object[] updates) throws EvaluationException, RemoteException;

}
