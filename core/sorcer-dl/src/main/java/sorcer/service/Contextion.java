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
public interface Contextion<T> extends Mogram, FederatedRequest, Identifiable {

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

	/**
	 * Returns the value at a given returnPath.
	 * 
	 * @param path
	 *            an attribute-based returnPath
	 * @return the value at the returnPath
	 * @throws ContextException
	 */
	public T getValue(String path, Arg... args)
			throws ContextException, RemoteException;

	public T getValue(Path path, Arg... args)
		throws ContextException, RemoteException;
	/**
	 * Returns a value at the returnPath as-is with no execution of the service at the returnPath.
	 * 
	 * @param path
	 *            the attribute-based returnPath
	 * @return the value as-is at the returnPath
	 * @throws ContextException
	 */
	public T asis(String path) throws ContextException;

	public T asis(Path path) throws ContextException;

	/**
	 * Associated a given value with a given returnPath
	 *
	 * @param path the attribute-based returnPath
	 * @param value the value to be associated with a given returnPath
	 * 
	 * @return the previous value at the returnPath
	 * 
	 * @throws ContextException
	 */
	public T putValue(String path, T value) throws ContextException;

	public T putValue(Path path, T value) throws ContextException;

	public Context<T> appendContext(Context<T> context)
		throws ContextException, RemoteException;

	public Context<T> getContext(Context<T> contextTemplate)
		throws RemoteException, ContextException;

	/**
	 * Appends an argument context to this context for a given returnPath.
	 * @param context a context to be appended
	 * @param path an offset returnPath of the argument context
	 * @return an appended context
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public Context<T> appendContext(Context<T> context, String path)
		throws ContextException, RemoteException;

	/**
	 * Returns a subcontext at a given returnPath.
	 * @param path a returnPath in this context
	 * @return a subcontext of this context at <code>returnPath</code>
	 * @throws ContextException
	 * @throws RemoteException
	 */
	public Context<T> getContext(String path) throws ContextException,
		RemoteException;

}
