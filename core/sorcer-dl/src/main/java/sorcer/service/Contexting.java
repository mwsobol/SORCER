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
 * Contexting associates attribute-based paths with values or evaluations.
 * 
 * @author Mike Sobolewski
 */
public interface Contexting<T> extends Identifiable, Mogram {

	/**
	 * Returns the value at a given path.
	 * 
	 * @param path
	 *            an attribute-based path
	 * @return the value at the path
	 * @throws ContextException
	 */
	public T getValue(String path, Arg... args)
			throws ContextException, RemoteException;

	/**
	 * Returns a value at the path as-is with no execution of the service at the path.
	 * 
	 * @param path
	 *            the attribute-based path
	 * @return the value as-is at the path
	 * @throws ContextException
	 */
	public T asis(String path) throws ContextException;

	public T asis(Path path) throws ContextException;

	/**
	 * Associated a given value with a given path
	 *
	 * @param path the attribute-based path
	 * @param value the value to be associated with a given path
	 * 
	 * @return the previous value at the path
	 * 
	 * @throws ContextException
	 */
	public T putValue(String path, T value) throws ContextException;

	public T putValue(Path path, T value) throws ContextException;

}
