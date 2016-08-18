/*
 * Copyright 2016 the original author or authors.
 * Copyright 2016 SorcerSoft.org.
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

import sorcer.service.*;

import java.rmi.RemoteException;

/**
 * Created by Mike Sobolewski on 7/26/16.
 */
public interface ServiceModel extends Arg {

	/**
	 * Returns the context of all responses of this model with a provided configuration.
	 *
	 * @param args optional configuration arguments
	 * @return
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public Object getResponse(Arg... args) throws ContextException, RemoteException;


	/**
	 * Returns the input context of this model.
	 *
	 * @return the input context
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public Context getInputs() throws ContextException, RemoteException;

	/**
	 * Returns the input context of this model with all inputs (in and inout directions).
	 *
	 * @return the input context
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public Context getAllInputs() throws ContextException, RemoteException;

	/**
	 * Returns the output context of this model.
	 *
	 * @return the output context
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public Context getOutputs() throws ContextException, RemoteException;

	/**
	 * Returns a input connector as a map of input paths of tis model mapped to output paths of the sender.
	 * An input connector specifies a map of an input context of this model.
	 *
	 * @param args optional configuration arguments
	 * @return
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public Context getInConnector(Arg... args) throws ContextException, RemoteException;

	/**
	 * Returns a output connector as a map of output paths of tis model mapped to input paths of the receiver.
	 * An output connector specifies a map of an output context of this model.
	 *
	 * @param args optional configuration arguments
	 * @return
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public Context getOutConnector(Arg... args) throws ContextException, RemoteException;

	/**
	 * Returns a value of the object at the path
	 * (evaluation or invocation on this object if needed).
	 *
	 * @param path
	 *            the variable name
	 * @return this model value at the path
	 * @throws ModelException
	 */
	public Object getValue(String path, Arg... args) throws ContextException, RemoteException;

	/**
	 * Returns a value of the object at the path as is
	 * (no evaluation or invocation on this object).
	 *
	 * @param path
	 *            the variable name
	 * @return this model value at the path
	 * @throws ModelException
	 */
	public Object asis(String path);

	public ServiceModel add(Identifiable... objects) throws ContextException,
			RemoteException;

}
