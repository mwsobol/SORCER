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
 * An interface related to {@link sorcer.service.Context}
 * evaluation/invocation and accessing contextion services.
 * Service providers that federate in the network
 * exchange input/output data via Contextion.
 *
 * @author Mike Sobolewski
 */
public interface Contextion extends FederatedRequest, Identifiable {

    /**
     * Returns the current context of this evaluation. The current context can be
     * exiting context with no need to evaluate it if it's still valid.
     *
     * @return the current execute of this evaluation
     * @throws EvaluationException
     * @throws RemoteException
     */
    public Context evaluate(Context context, Arg... args) throws EvaluationException, RemoteException;

    /**
	 * Returns the output context.
	 *
	 * @return the output context
	 * @throws ContextException
	*/
	public Context getContext() throws ContextException;

	/**
	 * Sets the input context.
	 *
	 * @throws ContextException
	 */
	public void setContext(Context input) throws ContextException;

	public Context appendContext(Context context)
		throws ContextException, RemoteException;

	public Context getContext(Context contextTemplate)
		throws RemoteException, ContextException;

	/**
	 * Appends an argument context to this context for a given path.
	 * @param context a context to be appended
	 * @param path an offset path of the argument context
	 * @return an appended context
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public Context appendContext(Context context, String path)
		throws ContextException, RemoteException;

	/**
	 * Returns a subcontext at a given path.
	 * @param path a path in this context
	 * @return a subcontext of this context at <code>path</code>
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public Context getContext(String path) throws ContextException,
		RemoteException;

}
