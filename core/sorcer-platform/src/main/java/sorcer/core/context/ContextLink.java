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
import net.jini.id.UuidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Link;
import sorcer.util.SorcerUtil;

/**
 * Provides for service context linking. Context links are references to an
 * offset (path) in a context, which allows the reuse of context objects.
 * 
 */
@SuppressWarnings("rawtypes")
public class ContextLink implements SorcerConstants, Link {

	private static Logger logger = LoggerFactory.getLogger(ContextLink.class.getName());

	private static final long serialVersionUID = -7115324059076651991L;

	private String name;
	
	private String offset = "";

	private Uuid contextId;
	
	private float version;

	private boolean fetched;

	// runtime variables:
	private Context linkedContext;

	private Principal linkPrincipal;
	
	private static ContextAccessor cntxtAccessor;
	
	/**
	 * Add a context link given the context id and offset. Note the context must
	 * have already been persisted in database.
	 */
	public ContextLink(Uuid id, float version, String offset,
					   SorcerPrincipal principal) throws ContextException {
		contextId = id;
		this.version = version;
		linkedContext = getContext();
		if (linkedContext == null) {
		} else if (offset == null) {
			this.name = linkedContext.getName();
			fetched = true;
		} else {
			this.name = linkedContext.getName();
			setOffset(offset);
			fetched = true;
		}
	}

	/**
	 * Add a context link given the context and offset. Public access to this
	 * method is probably temporary, as the preferred constructor is
	 * {@link #contextLink(String, SorcerPrincipal, String)}, since that method
	 * requires the context has already been persisted.
	 * 
	 */
	public ContextLink(Context context, String offset) throws ContextException {
		this(UuidFactory.generate(), 1.0f, "", null);
		linkedContext = context;
	}

	public ContextLink(Context context) throws ContextException {
		this(context, "");
	}
	
	/**
	 * Returns a context link identifier.
	 * 
	 * @return a link identifier
	 */
	public Uuid getId() {
		return contextId;
	}

	/**
	 * Assigns a persistent datastore identifier for this linked context.
	 * 
	 * @param id a link identifier
	 */
	public void setId(Uuid id) {
		contextId = id;
	}
	
	/*
	 * public String rootName() { return SorcerUtil.firstToken(offset, CPS); }
	 */
	public String getName() {
		String result = name == null ? SorcerUtil.firstToken(offset, CPS) : name;
		if (result.equals(""))
			result = linkedContext.getSubjectPath(); // assuming we have the context...
		return result;
	}

	public String getOffset() {
		return offset;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the offset in this linked context. If the offset itself is obtained
	 * by traversing a link (meaning there is a redundant link), the offset is
	 * recalculated and the link object is reset to point to the owning context
	 * (removing the redundancy).
	 * <P>
	 * Note: when links are originally setValue in ServiceContext, checks are
	 * performed.
	 */
	public void setOffset(String offset) throws ContextException {
		// validate offset is in this context
		Object[] result = linkedContext.getContextMapping(offset);

		if ((((String) result[1]).trim()).equals("")) {
			if (!isSameContext(result[0], result[1])) {
				// the alternative is throwing an exception:
				// throw new ContextException("Failed in setOffset: offset=
				// \""+offset+"\" is not in this context, but in the context
				// with name=\""+context.getName()+"\". Link and
				// offset=\""+result[1]+"\" should be setValue in this context
				// instead");
				this.offset = offset;
			}
			return;
		}

		if (((Context) result[0]).getValue((String) result[1]) == null) {
			if (Contexts.checkIfPathBeginsWith((ServiceContext) result[0],
					(String) result[1])) {
				if (!isSameContext(result[0], result[1])) {
					this.offset = offset;
				}
			} else {
				this.offset = (String) result[1];
				return;
			}
			return;
		} else if (result[0] != linkedContext) {
			this.offset = (String) result[1];
			this.linkedContext = (Context) result[0];
			this.contextId = linkedContext.getId();
			this.fetched = true;
			// status = BROKEN_LINK;

			// the alternative is throwing an exception:
			// throw new ContextException("Failed in setOffset: offset=
			// \""+offset+"\" is not in this context, but in the context with
			// name=\""+context.getName()+"\". Link and offset=\""+result[1]+"\"
			// should be setValue in this context instead");
		} else
			this.offset = offset;
	}

	public String toString() {
//		String str = "Link:\"" + name + "\":" + offset;
		return "\n" + linkedContext;
	}

	public boolean isSameContext(Object cntxt, Object offset) {
		if (cntxt != linkedContext) {
			this.offset = (String) offset;
			this.linkedContext = (Context) cntxt;
			this.contextId = linkedContext.getId();
			this.fetched = true;
			return true;
		}
		return false;
	}

	/**
	 * Return the context. The {@link SorcerPrincipal} for this principal if not
	 * fetched yet.
	 */
	public Context getContext() throws ContextException {
		return linkedContext;
	}

	public void setContext(Context ctxt) {
		linkedContext = ctxt;
	}

	public boolean isRemote() {
		return contextId != null;
	}

	public boolean isLocal() {
		return linkedContext != null;
	}

	public boolean isFetched() {
		return fetched;
	}

	public void isFetched(boolean state) {
		fetched = state;
	}
}
