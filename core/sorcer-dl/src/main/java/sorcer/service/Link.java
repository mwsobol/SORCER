/* 
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

import java.io.Serializable;

/**
 * The <code>Link</code> interface allows path values of service contexts to be
 * linked to other service contexts using instances implementing this interface.
 * The interface provides for linking service contexts to persisted subcontexts.
 * Context links are references to an offset (path) in a context, which allows
 * the reuse of other existing service context objects.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public interface Link extends Serializable {

	/**
	 * Returns a name of this link.
	 * 
	 * @return a link name
	 */
	public String getName();

	/**
	 * Assign a link name for its linked context.
	 * 
	 * @param name
	 *            a name for this context link
	 */
	public void setName(String name);

	/**
	 * Returns the path offset into this linked context.
	 * 
	 * @return an offset of this link
	 */
	public String getOffset();

	/**
	 * Sets the offset in this linked context. If the offset itself is obtained
	 * by traversing a link (meaning there is a redundant link), the offset is
	 * recalculated and the link object is reset to point to the owning context
	 * (removing the redundancy).
	 * 
	 * @param offset
	 * @throws ContextException
	 */
	public void setOffset(String offset) throws ContextException;

	/**
	 * Assigns a link service context associated with this link.
	 * 
	 * @param ctxt
	 */
	public void setContext(Context ctxt);

	/**
	 * Returns the context associated with this link.
	 */
	public Context getContext() throws ContextException;
	
	/**
	 * Returns true if the linked context associated with this link is enclosed
	 * into the link, otherwise false.
	 * 
	 * @return true if the linked context is enclosed in this link
	 */
	public boolean isLocal();

	/**
	 * Returns a boolean indicating whether the linked context is retrieved from
	 * a persistent context storage into this link - <code>true</code>
	 * indicating if the linked context is already fetched; <code>false</code>
	 * otherwise.
	 * 
	 * @return a boolean indicating if the linked context is already fetched
	 */
	public boolean isFetched();

	/**
	 * Assigns a boolean indicating whether the linked context is retrieved
	 * from a persistent context storage into this link.
	 * 
	 * @param state
	 *            <code>true</code> indicating if the linked context is
	 *            already fetched; <code>false</code> otherwise
	 */
	public void isFetched(boolean state);

}
