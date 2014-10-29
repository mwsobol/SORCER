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

package sorcer.core.context;

import java.security.Principal;

import net.jini.id.Uuid;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.Context;
import sorcer.service.ContextException;

/**
 * The ContextAccessor interface defines storage and retrieval of service
 * contexts.
 */
@SuppressWarnings("rawtypes")
public interface ContextAccessor {

	/**
	 * Performs a save operation, storing and returning the context with a
	 * incremented version. The id remains unchanged.
	 * 
	 * @param cntxt
	 *            a context to place in the data store.
	 * @return the stored ServiceContext with incremented version
	 * @throws ContextException
	 *             thrown if context is not already in data store or if the
	 *             context's GAppPrincipal does not provide the required
	 *             authorization.
	 * @see Context.setPrincipal
	 */
	public Context save(Context context) throws ContextException;

	/**
	 * Saves context to the data store, assigning a unique id with version set
	 * to 1.0 to the returned context.
	 * 
	 * @param cntxt
	 *            a context to persist in the data store.
	 * @return the stored ServiceContext with new id
	 * @throws ContextException
	 *             thrown if context's GAppPrincipal does not provide the
	 *             required authorization.
	 * @see Context.setPrincipal
	 */
	public Context saveAs(Context context) throws ContextException;

	/**
	 * Returns the ServiceContext with given id and the most current version.
	 * 
	 * @param id
	 *            the context identification
	 * @param prin
	 *            provides authorization
	 * @throws ContextException
	 *             thrown if given id doesn't exist in context storage or if
	 *             SorcerPrincipal does not provide the required authorization.
	 * 
	 */
	public Context getContext(String id, SorcerPrincipal principal)
			throws ContextException;

	/**
	 * Returns the ServiceContext with given id and version.
	 * 
	 * @param id
	 *            the context identification
	 * @param version
	 *            the version number for this context; if < 0, latest version
	 *            returned.
	 * @param prin
	 *            provides authorization
	 * @throws ContextException
	 *             thrown if id and/or version doesn't exist in context storage
	 *             or SorcerPrincipal does not provide the required authorization.
	 * 
	 */
	public Context getContext(Uuid id, float version,
			Principal principal) throws ContextException;

	/**
	 * Returns revision history of context with given id.
	 */
	public String getHistory(String id);

}
