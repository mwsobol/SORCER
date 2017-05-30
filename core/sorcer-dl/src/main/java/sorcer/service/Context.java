/*
 * Copyright 2009 the original author or authors.
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

import sorcer.core.SorcerConstants;
import sorcer.core.provider.Provider;

import sorcer.service.modeling.Model;

import java.io.Serializable;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

/**
 * Service context classes that implement this interface provide SORCER generic
 * metacomputing data structures for storage, retrieval, and propagation of
 * heterogeneous information across all SORCER service providers. Two generic
 * implementations are provided: <code>ServiceContext</code> and
 * <code>sorcer.core.context.PositionalContext</code>. Usually the former is used by
 * service requestors and the latter with more functionality is used by service
 * providers. The ServiceContextImpl class implements the ProviderContext
 * interface that extends ServiceContext. An example of a specific service
 * context is illustrated by <code>ArrayContext</code>.
 * <p>
 * A service context is a tree-like structure with two types of nodes. Leaf
 * nodes are called data (value) nodes and the remaining nodes are called
 * context attribute nodes. Context attributes define a namespace for the data
 * in a uniform way for use by all related services. A path of context
 * attributes leading from the root of the context to any leaf node along with
 * its data node is called a context element (path/data). Thus a path is a
 * hierarchical attribute of data contained in the leaf node. A data node can
 * contain any Java object; in particular a generic {@see ContextNode} is
 * available.
 * <p>
 * Context paths can be marked with attributes set by using the method
 * {@link #setAttribute(String)}, which allow for efficient search of related
 * data by service providers (service context clients). The search issue becomes
 * critical when a namespace in the context may change or when data nodes
 * contain remote references, for example a URL. It is usually assumed that
 * provider-enforced data associations are more stable than user-oriented paths.
 * Each direct service invocation {@link Server#service} requires data in the
 * ServiceContext format.
 * <p>
 * Service contexts are defined by this common interface with efficient
 * service-oriented APIs. However, there are some similarities with XML
 * terminology. The root element in XML contains all other XML elements.
 * Similarly, in each service context, the root extends to all data nodes. A
 * path with its data node (context element) is conceptually similar to an XML
 * element. XML tags (markup) correspond to context attributes. A context data
 * node can be considered the analog of data in XML, however a service context
 * data node has rich OO semantics. Finally, data attributes in service contexts
 * can be compared to element attributes in XML. However, data attributes have
 * more meta-attribute meaning than XML attributes. While context attributes
 * provide a name space for direct access to data nodes via
 * {@link Context#getValue}, data attributes specify the data node for indirect
 * efficient retrieval (search) by service providers.
 *
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public interface Context<T> extends Model, Mappable<T>, Serializable, Contexter<T>, Paradigmatic {

	/** parameter (proc) */
	final static String PATH_PAR = "proc";

	/** context parameter (cp) */
	final static String CONTEXT_PARAMETER = "cp";

	/** context pipe (pipe) */
	final static String PIPE = "pipe";

	/** directional attribute (da) */
	final static String DIRECTION = "da";

	/** entry value class (vc) */
	final static String VAL_CLASS = "vc";

	/** operand positioning (OPP) for operators (direction with index) */
	final static String OPP = "opp";

	/** index attribute (i) */
	final static String INDEX = "i";

	final static String PATH = "path";

	/** context nameID (cid) */
	final static String CONTEXT_ID = "cid";

	/** SORCER type (ft) */
	final static String DATA_NODE_TYPE = "dnt";

	/** SORCER type */
	final static String VAR_NODE_TYPE = "vnt";

	final static String APPLICATION = "appl";

	final static String FORMAT = "format";

	final static String MODIFIER = "modifier";

	/** action (act) */
	final static String ACTION = "action";

	/** provider name (pn) */
	final static String PROVIDER_NAME = "pn";

	/** interface (if) */
	final static String INTERFACE = "if";

	/** selector (sl) */
	final static String SELECTOR = "sl";

	/** a variable mark */
	final static String VAR = "var";

	/** a type variable type */
	final static String VT = "vt";

	/** directional attribute values */
	final static String DA_IN = "in";

	/** directional attribute values */
	final static String DA_OUT = "out";

	/** directional attribute values */
	final static String DA_INOUT = "inout";

	/** in and out synonyms - paths equivalents */
	final static String DA_ININ = "inin";

	/** in and out synonyms - paths equivalents */
	final static String DA_OUTOUT = "outout";

	final static String SERVICE_CONTEXT = "cxt";

	final static String SRV_MODEL = "Service Model";

	final static String PROC_MODEL = "Procedural Model";

	final static String PAR_MODEL = "Parametric Model";

	final static String DATA_MODEL = "Data Model";

	/** EMPTY LEAF NODE i.e. node with no data and not empty string */
	final static String EMPTY_LEAF = ":Empty";

	final static String JOB = "job";

	final static String JOB_ = "job" + SorcerConstants.CPS;

	final static String TASK = "task";

	final static String TASK_ = "task" + SorcerConstants.CPS;

	final static String ID = SorcerConstants.CPS + "id";

	final static String JOB_COMMENTS = "job" + SorcerConstants.CPS + "comments";

	final static String JOB_FEEDBACK = "job" + SorcerConstants.CPS + "feedback";

	// Domain Specific Initialization Path
	final static String INIT_PATH = "model" + SorcerConstants.CPS + "initialization"
			+ SorcerConstants.CPS + "data";

	// Domain Specific Data Path
	final static String DSD_PATH = "domain" + SorcerConstants.CPS + "specific"
			+ SorcerConstants.CPS + "data";

	/**
	 * An object to specify no context value.
	 */
	public static class none implements Serializable {
		private static final long serialVersionUID = -6152257095701812950L;

		private none() {

		}

		@Override
		public boolean equals(Object obj) {
			return obj.toString().equals("none");
		}

		public String toString() {
			return "none";
		}
	}
	final public static Object none = new none();

	/**
	 * Returns a value at the path if exists, otherwise a value of the path that
	 * ends with the last attribute of the given path.
	 *
	 * @param path
	 *            The path of a context value.
	 */
	public T getSoftValue(String path) throws ContextException;

	/**
	 */
	public String getParentPath();

	/**
	 * @param path
	 *            The parentPath to set.
	 */
	public void setParentPath(String path);

	/**
	 * @param accessClass
	 *            The accessClass to set.
	 */
	public void setAccessClass(String accessClass);

	/**
	 */
	public String getAccessClass();

	public boolean containsPath(String path);

	public Map<String, Map<String, String>> getMetacontext();

	/**
	 * @param metacontext
	 *            The metacontext to set.
	 */
	public void setMetacontext(
			Map<String, Map<String, String>> metacontext);

	/**
	 * Returns the mogram associated with this context.
	 *
	 * @return Exertion
	 */
	public Exertion getMogram();

	/**
	 * Returns the service provider associated with this context
	 *
	 * @return Provider
	 */
	public Provider getProvider() throws SignatureException;

	/**
	 * Returns the result value associated with this context.
	 *
	 * @param entries data for closing free variables
	 *
	 * @return The context path
	 */
	public T getReturnValue(Arg... entries) throws ContextException,
			RemoteException;

	/**
	 * @param task
	 *            The task to set.
	 */
	public void setExertion(Exertion task);

	/**
	 * Returns the subject path in this context. A subject is a path/value
	 * association and is interpreted as the context constituent about which
	 * something is predicated. Other path/value associations of this context
	 * are interpreted as complements of the context subject.
	 *
	 * @return the subject path
	 */
	public String getSubjectPath();

	/**
	 * Returns the subject value in this context. A subject is a path/value
	 * association and is interpreted as the context constituent about which
	 * something is predicated. Other path/value associations of this context
	 * are interpreted as complements of the context subject.
	 *
	 * @return the subject value
	 */
	public Object getSubjectValue();

	/**
	 * Assigns the subject <code>value</code> at <code>path</code> in this
	 * context. A subject is a path/value association and is interpreted as the
	 * context constituent about which something is predicated. Other path/value
	 * associations of this context are interpreted as complements of the
	 * context subject.
	 *
	 * @param path
	 *            the subject path
	 * @param value
	 *            the subject value
	 */
	public void setSubject(String path, Object value);

	public void reportException(Throwable t);

	public void reportException(String message, Throwable t);

	public void reportException(String message, Throwable t, ProviderInfo info);

	public void reportException(String message, Throwable t, Provider provider);

	public void reportException(String message, Throwable t, Provider provider,  ProviderInfo info);

	public void appendTrace(String footprint);

	public Context append(Context context) throws ContextException;

	public Context<T> updateEntries(Model context) throws ContextException;
	/**
	 * Returns this context within its cuureent scope.
	 *
	 * @return this context
	 * @throws ContextException
	 */
	public Context getCurrentContext() throws ContextException;

	/**
	 * Returns a value of the object at the path as is
	 * (no evaluation or invocation on this object).
	 *
	 * @param path
	 *            the key (path)
	 * @return this context value at the path
	 * @throws ContextException
	 */
	public T asis(String path);

	public void setReturnValue(Object value) throws ContextException;

	public Object putDbValue(String path, Object value) throws ContextException, RemoteException;

	public Object putDbValue(String path, Object value, URL datastoreUrl)
			throws ContextException, RemoteException;

	public Object addValue(Identifiable value) throws ContextException;

	public Arg addProc(Arg value) throws ContextException;

	public Arg addProc(String path, Object value) throws ContextException;

	public Arg getPar(String path) throws ContextException, RemoteException;

	public Object putValue(String path, Object value, String association)
			throws ContextException;

	public Object putLink(String path, Context cntxt, String offset)
			throws ContextException;

	public Object putLink(String name, String path, Context cntxt, String offset)
			throws ContextException;

	public Object remove(Object path);

	/**
	 * Creates a context pipe between output parameter at outPath in this
	 * context to an input parameter at inPath in outContext. The pipe allow for
	 * passing on parameters between different contexts within the same
	 * exertion.
	 *
	 * @param outPath
	 *            a location of output parameter
	 * @param inPath
	 *            a location of input parameter
	 * @param inContext
	 *            an input parameter context
	 * @throws ContextException
	 */
	public void connect(String outPath, String inPath, Context inContext)
			throws ContextException;

	public void pipe(String inPath, String outPath, Context outContext)
			throws ContextException;

	/**
	 * Marks mapping between output parameter at fromParh in this context to an
	 * input parameter at toPath in toContext. Mappings allow for passing on
	 * parameters between different contexts within the same exertion.
	 *
	 * @param fromPath
	 *            a location of output parameter
	 * @param toPath
	 *            a location of input parameter
	 * @param toContext
	 *            an input parameter context
	 * @throws ContextException
	 */
	public void map(String fromPath, String toPath, Context toContext)
			throws ContextException;

	/**
	 * Removes the <code>ContextLink</code> object pointed to by path. If object is
	 * not a context link, a ContextException will be thrown.
	 *
	 * @throws ContextException
	 * @see #removePath
	 */
	public void removeLink(String path) throws ContextException;

	/**
	 * Returns an <code>Object</code> array containing the ServiceContext in
	 * which path belongs and the absolute path in that context.
	 *
	 * @param path
	 *            the location in the context
	 * @return <code>Object</code> array of length two. The first element is the
	 *         ServiceContext; the second element is the path in the
	 *         ServiceContext. Note, a context node is not required at the
	 *         returned path in the returned context--the results merely
	 *         indicate the mapping (getValue on the resulting context at the
	 *         resulting path will yield the contents)
	 *
	 * @throws ContextException
	 * @see #getValue
	 */
	public Object[] getContextMapping(String path) throws ContextException;

	/**
	 * Records this context in related monitoring session.
	 **/
	public void checkpoint() throws ContextException;

	/**
	 * Annotates the path with the tuple (value sequence) specified by a
	 * relation in the domain of attribute product related to the context data
	 * nodes.The relation can be either a single search attribute (property) or
	 * attribute product. The search attribute-value sequence must be separated
	 * by the <code>Context.APS</code>. Marked data nodes can be found by their
	 * tuples (@link #getMarkedPaths} independently of associative context paths
	 * defined by context attributes {@link #getValue}. It is usually assumed
	 * that search relations are more stable than user friendly context paths.
	 *
	 * Tuple attributes and relations must first be registered with the context
	 * with (addAttribute}, addProperty, and addRelation) before they are used
	 * in contexts.
	 *
	 * @param path
	 *            the location in the context namespace
	 * @param tuple
	 *            the domain-value sequence
	 * @return self
	 * @throws ContextException
	 */
	public Context mark(String path, String tuple) throws ContextException;

	/**
	 * Returns the List of all context paths matching the given
	 * association.
	 *
	 * @param association
	 *            the association of this context to be matched
	 * @return the enumeration of matches for the given association
	 * @throws ContextException
	 */
	public List<String> markedPaths(String association)
			throws ContextException;

	/**
	 * Returns the List of all values with paths matching the given association.
	 *
	 * @param association
	 *            the association of this context to be matched
	 * @return the List of matches for the given association
	 * @throws ContextException
	 */
	public List<T> getMarkedValues(String association) throws ContextException;

	/**
	 * Returns the List of tagged path with the given association.
	 *
	 * @param association
	 *            the association of this context to be matched
	 * @return the List of paths for the given association
	 * @throws ContextException
	 */
	public String[] getMarkedPaths(String association) throws ContextException;

	/**
	 * Register an attribute with a ServiceContext's metacontext
	 *
	 * @param attribute
	 *            the attribute descriptor
	 */
	public void setAttribute(String attribute) throws ContextException;

	/**
	 * Returns boolean value designating if <code>attributeName</code> is an
	 * attribute in the top-level context. Does not descend into linked
	 * contexts. Is true if attribute is singleton or metaattribute type.
	 *
	 * @see #isLocalSingletonAttribute
	 * @see #isLocalMetaattribute
	 */
	public boolean isLocalAttribute(String attributeName);

	/**
	 * Returns boolean value designating if <code>attributeName</code> is a
	 * singleton attribute in the top-level context. Does not descend into
	 * linked contexts.
	 *
	 * @see #isLocalAttribute
	 * @see #isLocalMetaattribute
	 */
	public boolean isLocalSingletonAttribute(String attributeName);

	/**
	 * Returns boolean value designating if <code>attributeName</code> is a meta
	 * attribute in the top-level context. Does not descend into linked
	 * contexts.
	 *
	 * @see #isLocalAttribute
	 * @see #isLocalSingletonAttribute
	 */
	public boolean isLocalMetaattribute(String attributeName);

	/**
	 * Returns a {@link boolean} value designating if <code>attributeName</code>
	 * is an attribute in this context including any linked contexts
	 *
	 * @return <code>boolean</code>
	 * @throws ContextException
	 * @see #isLocalAttribute
	 * @see #setAttribute
	 * @see #isSingletonAttribute
	 * @see #isMetaattribute
	 */
	public boolean isAttribute(String attributeName) throws ContextException;

	/**
	 * Returns a {@link boolean} value designating if <code>attributeName</code>
	 * is a singleton attribute in this context including any linked contexts
	 *
	 * @return <code>boolean</code>
	 * @throws ContextException
	 * @see #isLocalAttribute
	 * @see #setAttribute
	 * @see #isAttribute
	 * @see #isMetaattribute
	 */
	public boolean isSingletonAttribute(String attributeName)
			throws ContextException;

	/**
	 * Returns a {@link boolean} value designating if <code>attributeName</code>
	 * is a meta attribute in this context including any linked contexts
	 *
	 * @return <code>boolean</code>
	 * @throws ContextException
	 * @see #isLocalAttribute
	 * @see #setAttribute
	 * @see #isAttribute
	 * @see #isSingletonAttribute
	 */
	public boolean isMetaattribute(String attributeName)
			throws ContextException;

	/**
	 * Returns the value part of the specified attribute that has been assigned
	 * to this context node. The attribute can be either a singleton attribute
	 * or a meta-attribute, which is itself a collection of attributes.
	 *
	 * @param path
	 *            the location in the context
	 * @param attributeName
	 *            the name of the metaattribute
	 *
	 * @return <code>String</code> the attribute value
	 * @throws ContextException
	 * @see #getSingletonAttributeValue
	 * @see #getMetaattributeValue
	 */
	public String getAttributeValue(String path, String attributeName)
			throws ContextException;

	/**
	 * Returns the value part of the specified singleton attribute that has been
	 * assigned to this context node. A singleton attribute is a single
	 * attribute with a single value as distinguished from a metaattribute which
	 * designates multiple attribute-value pairs.
	 *
	 * @param path
	 *            the location in the context
	 * @param attributeName
	 *            the name of the metaattribute
	 *
	 * @return <code>String</code> the attribute value
	 * @throws ContextException
	 * @see #getAttributeValue
	 * @see #getMetaattributeValue
	 */
	public String getSingletonAttributeValue(String path, String attributeName)
			throws ContextException;

	/**
	 * Returns the value part of the specified metaattribute that has been
	 * assigned to this context node. The attribute value is a concatenation of
	 * the individual attribute values, separated by the context meta-path
	 * separator character (CMPS).
	 *
	 * @param path
	 *            the location in the context
	 * @param attributeName
	 *            the name of the metaattribute
	 *
	 * @return <code>String</code> the meta-attribute value
	 * @throws ContextException
	 * @see #getAttributeValue
	 * @see #getSingletonAttributeValue
	 */
	public String getMetaattributeValue(String path, String attributeName)
			throws ContextException;

	public void removeAttributeValue(String path, String attributeValue)
			throws ContextException;

	/**
	 * Returns the metapath for the given metaattribute in the top-level
	 * context. Meta-definitions in linked contexts are not examined, and in
	 * general can be different. To examine metapaths in linked contexts, call
	 * getLocalMetapath operating on the <code>ServiceContext</code> that is
	 * linked (which can be obtained, for example, from the getContext method of
	 * {@link ContextLink} objects. Returns <code>null</code> if not defined.
	 *
	 * Metapaths are set using {@link #getLocalMetapath}
	 *
	 * @param metaattributeName
	 *            the name of the metaattribute
	 *
	 * @return the metapath or <code>null</code> if not defined
	 * @throws ContextException
	 * @see ContextLink
	 * @see #setAttribute
	 * @see #getAttributes
	 */
	public String getLocalMetapath(String metaattributeName)
			throws ContextException;

	public Context getDirectionalSubcontext(Path... paths) throws ContextException;

	public boolean isValid(Signature method) throws ContextException;

	/**
	 * Returns a list of all paths of this context.
	 *
	 * @return <code>List</code>
	 * @throws ContextException
	 * @see ContextLink
	 */
	public List<String> getPaths() throws ContextException;

	/**
	 * Returns the enumeration of all context paths matching the given regular
	 * expression.
	 *
	 * @param regex
	 *            the regular expression to which paths of this context are to
	 *            be matched
	 * @return a List of matches for the given regular expression
	 * @throws ContextException
	 */
	public List<String> paths(String regex) throws ContextException;

	/**
	 * Returns an enumeration of all values of this service context.
	 *
	 * @return an enumeration of all context values
	 * @throws ContextException
	 */
	public Enumeration<?> contextValues() throws ContextException;

	/**
	 * Returns an {@link Enumeration} of the locations of the first-level
	 * {@link ContextLink} objects in this context. The enumeration does not
	 * include ContextLink objects that reside in linked contexts.
	 *
	 * @return <code>Enumeration</code>
	 * @throws ContextException
	 * @see ContextLink
	 */
	public List<String> localLinkPaths() throws ContextException;

	/**
	 * Returns an {@link Enumeration} of the locations of the
	 * {@link ContextLink} objects in this context. The enumeration includes
	 * ContextLink objects that reside in linked contexts.
	 *
	 * @return <code>Enumeration</code>
	 * @throws ContextException
	 * @see ContextLink
	 */
	public Enumeration<?> linkPaths() throws ContextException;

	/**
	 * Returns an {@link Enumeration} of all the {@link ContextLink} objects in
	 * this context including any ContextLinks that reside in linked contexts.
	 *
	 * @return <code>Enumeration</code>
	 * @throws ContextException
	 * @see ContextLink
	 */
	public Enumeration<?> links() throws ContextException;

	/**
	 * Returns an {@link Enumeration} of the top-level {@link ContextLink}
	 * objects in this context. Does not include ContextLinks that reside in
	 * linked contexts.
	 *
	 * @return <code>Enumeration</code>
	 * @throws ContextException
	 * @see ContextLink
	 */
	public List<Link> localLinks() throws ContextException;

	/**
	 * Links the argument <code>context</code> to this context at a given path
	 * with its offset path. The last attribute of the path is the root of the
	 * linked subcontext of the <code>context</code>.
	 *
	 * @param context
	 * @param atPath
	 * @param offset
	 * @throws ContextException
	 */
	public Object link(Context context, String atPath, String offset)
			throws ContextException;

	/**
	 * Returns the {@link ContextLink} object that resides at path in the
	 * context. This method is necessary since ContextLink objects are otherwise
	 * transparent. For example, getValue(path) returns a value in the linked
	 * context, not the LinkedContext object.
	 *
	 * @param path
	 *            the location in the context
	 * @return <code>ContextLink</code> if a ContextLink object resides at path;
	 *         <code>null</code> otherwise.
	 * @throws ContextException
	 * @see ContextLink
	 */
	public Link getLink(String path) throws ContextException;

	/**
	 * Allocates a value in the context with the directional attribute set to
	 * DA_IN.
	 */
	public T putInValue(String path, T value) throws ContextException;

	/**
	 * Allocates a value in the context with the directional attribute set to
	 * DA_OUT.
	 */
	public T putOutValue(String path, T value)
			throws ContextException;

	/**
	 * Allocates a value in the context with the directional attribute set to
	 * DA_INOUT.
	 */
	public T putInoutValue(String path, T value)
			throws ContextException;

	/**
	 * Allocates a value in the context with the directional attribute set to
	 * DA_IN.
	 */
	public T putInValue(String path, T value, String association)
			throws ContextException;

	/**
	 * Allocates a value in the context with the directional attribute set to
	 * DA_OUT.
	 */
	public T putOutValue(String path, T value, String association)
			throws ContextException;

	/**
	 * Allocates a value in the context with the directional attribute set to
	 * DA_INOUT.
	 */
	public T putInoutValue(String path, T value, String association)
			throws ContextException;

	/**
	 * Sets directional attribute to DA_IN.
	 */
	public Context setIn(String path) throws ContextException;

	/**
	 * Sets directional attribute to DA_OUT.
	 */
	public Context setOut(String path) throws ContextException;

	/**
	 * Sets directional attribute to DA_INOUT.
	 */
	public Context setInout(String path) throws ContextException;

	/**
	 * Removes a context node from the context. If the designated path points to
	 * a {@link ContextLink} object, a {@link ContextException} will be thrown.
	 * Use {@link #removeLink} to remove link contexts.
	 *
	 * @see #removeLink
	 * @throws ContextException
	 */
	public void removePath(String path) throws ContextException;

	/**
	 * Returns a string representation of this context.
	 *
	 * @return a string representation
	 */
	public String toString();

	/**
	 * Returns path of first occurrence of object in the context. Returns null
	 * if not found. This method is useful for orphaned objects, but should be
	 * used with caution, since the same object can appear in the context in
	 * multiple places, and the location may have relevance to interpretation.
	 * If an object is orphaned, it is best to re-think how the object was
	 * obtained. It is best to refer to the object with the unique path.
	 */
	public String getPath(Object obj) throws ContextException;

	/**
	 * Returns all singleton attributes for the top-level context. Does not
	 * descend into linked contexts to retrieve attributes (see
	 * {@link #getAttributes} which does look in linked contexts).
	 *
	 * @return List of singleton attributes (all of type
	 *         <code>String</code>)
	 * @see #getAttributes
	 */
	public List<String> localSimpleAttributes();

	/**
	 * Returns all singleton attributes for the context. Descends into linked
	 * contexts to retrieve underlying singleton attributes (see
	 * {@link #getAttributes} which does not look in linked contexts).
	 *
	 * @return List of meta attributes (all of type <code>String</code>)
	 * @throws ContextException
	 * @see #getAttributes
	 */
	public List<String> simpleAttributes() throws ContextException;

	/**
	 * Get all meta associations (meta attribute-meta value pairs) at the
	 * specified context node.
	 *
	 * @param path
	 *            the location in the context
	 * @return List of meta associations (of type <code>String</code>)
	 * @throws ContextException
	 */
	public List<String> metaassociations(String path) throws ContextException;

	/**
	 * Returns all locally defined attributes in this context (metacontext).
	 *
	 * @return Set of local attributes (all of type <code>String</code>)
	 * @see #getAttributes
	 */
	public Set<String> localAttributes();

	/**
	 * Returns all composite attributes for the top-level context. Does not
	 * descend into linked contexts to retrieve meta attributes (see
	 * {@link #compositeAttributes} which does look in linked contexts).
	 *
	 * @return List of meta attributes (all of type <code>String</code>)
	 * @see #compositeAttributes
	 */
	public List<String> localCompositeAttributes();

	/**
	 * Returns all meta attributes for the context. Descends into linked
	 * contexts to retrieve underlying meta attributes (see
	 * {@link #localCompositeAttributes} which does not look in linked
	 * contexts).
	 *
	 * @return List of meta attributes (all of type <code>String</code>)
	 * @throws ContextException
	 * @see #getAttributes
	 */
	public List<String> compositeAttributes() throws ContextException;

	/**
	 * Returns all attributes (singleton and meta) for the context. Descends
	 * into linked contexts to retrieve underlying attributes (see
	 * {@link #localCompositeAttributes} or {@link #getAttributes} which do not
	 * look in linked contexts).
	 *
	 * @return List of attributes (all of type <code>String</code>)
	 * @throws ContextException
	 */
	public List<String> getAttributes() throws ContextException;

	/**
	 * Returns all attributes (simple and composite) at path in the context.
	 *
	 * @param path
	 *            the location in the context
	 * @return List of attributes (all of type <code>String</code>)
	 * @throws ContextException
	 */
	public List<String> getAttributes(String path) throws ContextException;

//	public Map<String, T> getData();

	public int size();

	Context getDomain(String name) throws ContextException;

	SignatureReturnPath getReturnPath();

	public boolean compareTo(Object context);

	public boolean compareTo(Object context, double delta);

	Context setReturnPath(SignatureReturnPath returnPath);

	public enum Type {
		ASSOCIATIVE, SHARED, POSITIONAL, LIST, SCOPE, INDEXED, ARRAY
	}

	final static String PARAMETER_TYPES = "context/parameter/types";
	final static String PARAMETER_VALUES = "context/parameter/values/";
	final static String TARGET = "context/target";
	final static String RETURN = "context/result";

}
