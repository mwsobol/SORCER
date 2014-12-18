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

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.co.tuple.Entry;
import sorcer.co.tuple.EntryList;
import sorcer.co.tuple.ExecPath;
import sorcer.co.tuple.Tuple2;
import sorcer.core.SorcerConstants;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParList;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.context.node.ContextNode;
import sorcer.core.context.node.ContextNodeException;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.provider.Provider;
import sorcer.core.provider.ServiceProvider;
import sorcer.core.signature.NetSignature;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.*;
import sorcer.service.Exec.State;
import sorcer.service.Signature.Direction;
import sorcer.service.Signature.ReturnPath;
import sorcer.util.ProviderInfo;
import sorcer.util.SorcerUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Implements the base-level service context interface {@link Context}.
 */
@SuppressWarnings({ "unchecked", "rawtypes"})
public class ServiceContext<T> extends Hashtable<String, T> implements
		Context<T>, AssociativeContext<T>, Evaluation<T>, Invocation<T>,
		Contexter<T>, SorcerConstants {

	private static final long serialVersionUID = 3311956866023311727L;

	protected Uuid contextId;

	private static String defaultName = "cxt-";

	private static int count = 0;

	protected String name;

	protected String subjectPath = "";

	protected Object subjectValue = "";

	// default value new ReturnPath(Context.RETURN);
	protected ReturnPath<T> returnPath;
	
	protected ReturnPath<T> returnJobPath;
	
	protected ExecPath execPath;
	
	protected boolean contextChanged = false;

	// for calls by reflection for 'args' Object[] set the path
	// or use the default one: Context.ARGS
	//protected String argsPath = Context.ARGS;
	protected String argsPath;
	
	protected String parameterTypesPath;

	protected String targetPath;

	protected String parentPath = "";

	protected Uuid parentId;

	// a flag for the context to be shared
	// for data piping see: map. connect, pipe
	protected boolean isShared = false;

	protected String creationDate;

	protected String lastUpdateDate;

	protected String description;

	protected int scopeCode = PRIVATE_SCOPE;

	protected String ownerId = "0";

	protected String subjectId = "0";

	protected String project;

	protected String accessClass;

	protected String exportControl;

	protected String goodUntilDate;

	protected String domainId = null;

	protected String subdomainId = null;

	protected String domainName;

	protected String subdomainName;
	
	protected boolean isModeling = false;
	
	protected String dbUrl;

	protected String prefix = "";

	protected List<EntryList> entryLists;

	protected float version;

	/**
	 * An additional hashtable to handle the ids
	 * (context_data_id,data_version_id)
	 */
	protected Hashtable delPathIds;

	/**
	 * metacontext: key is a metaattribute and value is a hashtable of
	 * path/metapath entries
	 */
	protected Hashtable metacontext;

	protected Context blockScope;
	
	/** The exertion that uses this context */
	protected ServiceExertion exertion;

	protected String currentSelector;

	protected String currentPrefix;

	protected boolean isFinalized = false;

	// dependency management for this Context
	protected List<Evaluation> dependers = new ArrayList<Evaluation>();

	/**
	 * For persistence layers to differentiate with saved context already
	 * associated to task or not.
	 */
	public boolean isPersistantTaskAssociated = false;

	protected SorcerPrincipal principal = null;

    private final String userName = System.getProperty("user.name");

	public static ContextAccessor cntxtAccessor;

	/** EMPTY LEAF NODE ie. node with no data and not empty string */
	public final static String EMPTY_LEAF = ":Empty";

	// this class logger
	protected static Logger logger = Logger.getLogger(ServiceContext.class
			.getName());

	/**
	 * Default constructor for the ServiceContext class. The constructor calls the method init, 
	 * defines the the service context name sets the root name to a blank string creates a new 
	 * hash tables for path identifications, delPath, and linked paths. The constructor creates the 
	 * context identification number via the UUID factory generate method.
	 */
	public ServiceContext() {
		init();
		name = defaultName + count++;
		delPathIds = new Hashtable();
		contextId = UuidFactory.generate();
	}

	/**
	 * Constructor for Service Context class. It calls on the default constructor
	 * @param name
	 * @see ServiceContext
	 */
	public ServiceContext(String name) {
		this();
		if (name == null || name.length() == 0) {
			this.name = defaultName + count++;
		} else {
			this.name = name;
		}
	}

	public ServiceContext(String subjectPath, Object subjectValue) {
		this(subjectPath);
		this.subjectPath = subjectPath;
		this.subjectValue = subjectValue;
	}

	public ServiceContext(String name, String subjectPath, Object subjectValue) {
		this(name);
		this.subjectPath = subjectPath;
		this.subjectValue = subjectValue;
	}

	public ServiceContext(Context<T> cntxt) throws ContextException {
		this(cntxt.getSubjectPath(), cntxt.getSubjectValue());
		// Note, I'm not making new objects here, only creating
		// new references
		String path;
		T obj;
		Enumeration e = ((ServiceContext) cntxt).keys();
		while (e.hasMoreElements()) {
			path = (String) e.nextElement();
			obj = cntxt.getValue(path);
			if (obj instanceof ContextLink
					&& ((ContextLink) obj).isFetched())
				updateLinkedContext((ContextLink) obj);
			if (obj == null)
				put(path, (T)none);
			else
				put(path, obj);
		}
		setMetacontext(cntxt.getMetacontext());
		// copy instance vars
		contextId = cntxt.getId();
		parentPath = cntxt.getParentPath();
		parentId = cntxt.getParentID();
		creationDate = cntxt.getCreationDate();
		lastUpdateDate = cntxt.getLastUpdateDate();
		description = cntxt.getDescription();
		scopeCode = cntxt.getScope();
		ownerId = cntxt.getOwnerID();
		subjectId = cntxt.getSubjectID();
		project = cntxt.getProject();
		accessClass = cntxt.getAccessClass();
		exportControl = cntxt.getExportControl();
		goodUntilDate = cntxt.getGoodUntilDate();
		domainId = cntxt.getDomainID();
		subdomainId = cntxt.getSubdomainID();
		domainName = cntxt.getDomainName();
		subdomainName = cntxt.getSubdomainName();
		version = cntxt.getVersion();
		exertion = (ServiceExertion) cntxt.getExertion();
		principal = (SorcerPrincipal)cntxt.getPrincipal();
		isPersistantTaskAssociated = ((ServiceContext) cntxt).isPersistantTaskAssociated;
	}

	public ServiceContext(Object object) throws ContextException {
		this((object instanceof Identifiable) ? ((Identifiable)object).getName() : null);
		setArgsPath(Context.PARAMETER_VALUES);
		setArgs(new Object[] { object });
		setParameterTypesPath(Context.PARAMETER_TYPES);
		setParameterTypes(new Class[] { object.getClass() });
	}

	/**
	 * Initializes the service context class by allocating storage for all
	 * simple and composite attributes and their associations. It creates system
	 * data attributes: SORCER_TYPE - dnt, CONTEXT_PARAMETER - cp, ACTION -
	 * action.
	 * <p>
	 * A 'metacontext' map stores all data attribute definitions in an internal
	 * 'metacontext' map with a key being an attribute
	 * mapped to the corresponding attribute descriptor. The attribute
	 * descriptor for a simple attribute is the attribute name itself, and for a
	 * composite attribute the descriptor is an APS (association path separator)
	 * separated list of component attributes. A 'metacontext' map contains all
	 * simple attributes and component attributes (keys) associations with the
	 * corresponding map holding associations between between a path (key) and
	 * the value of attribute (key in 'metacontext').
	 * <p>
	 * The usage of metacontext is illustrated as follows:
	 * a single attribute - 'tag'; cxt.mark("arg/x1", "tag|stress");
	 * and get tagged value at arg/x1: cxt.getMarkedValues("tag|stress"));
	 * relation - 'triplet|_1|_2|_3', 'triplet' is a relation name and _1, _3, and _3
	 * are component attributes; cxt.mark("arg/x3", "triplet|mike|w|sobol");
	 * and get tagged value at arg/x3: cxt.getMarkedValues("triplet|mike|w|sobol"));
	 */
	private void init() {
		metacontext = new Hashtable();
		metacontext.put(SorcerConstants.CONTEXT_ATTRIBUTES, new Hashtable());

		// specify four SORCER standard composite attributes
		try {
			// default relation tags: tag, duo, and triplet
			setAttribute("tag");
			setAttribute("duo|_1|_2");
			setAttribute("triplet|_1|_2|_3");
			// context path tag
			setAttribute(PATH_PAR);
			// annotating input output files associated with source applications
			setCompositeAttribute(DATA_NODE_TYPE + APS + APPLICATION + APS
					+ FORMAT + APS + MODIFIER);
			// directional attributes with the context ID
			setCompositeAttribute(CONTEXT_PARAMETER + APS + DIRECTION + APS
					+ PATH + APS + CONTEXT_ID);
			// relationship to providers
			setCompositeAttribute(ACTION + APS + PROVIDER_NAME + APS
					+ INTERFACE + APS + SELECTOR);
			// operand positioning (OOP) for operators by index
			setCompositeAttribute(OPP + APS + DIRECTION + APS + INDEX);
			// the variable node type relationship (var name and its type) in
			// Analysis Models: vnt|var|vt
			setCompositeAttribute(VAR_NODE_TYPE + APS + VAR + APS + VT);
			dbUrl = "sos://sorcer.service.DatabaseStorer";
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    public String getUserName() {
        return userName;
    }
    
	public void setName(String name) {
		this.name = name;
	}

	public Uuid getId() {
		return contextId;
	}

	public void setId(Uuid id) {
		contextId = id;
	}

	public String getParentPath() {
		return parentPath;
	}

	public void setParentPath(String path) {
		parentPath = path;
	}

	public Uuid getParentID() {
		return parentId;
	}

	public void setParentID(Uuid id) {
		parentId = id;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String date) {
		creationDate = date;
	}

	public String getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(String date) {
		lastUpdateDate = date;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String text) {
		description = text;
	}

	public int getScope() {
		return scopeCode;
	}

	public void setScopeCode(int scope) {
		scopeCode = scope;
	}

	public String getOwnerID() {
		return ownerId;
	}

	public void setOwnerID(String id) {
		ownerId = id;
	}

	public void setSubjectID(String id) {
		subjectId = id;
	}

	public String getSubjectID() {
		return subjectId;
	}

	public Exertion getExertion() {
		return exertion;
	}

	public void setExertion(Exertion exertion) {
		if (exertion == null || exertion instanceof Exertion)
			this.exertion = (ServiceExertion) exertion;
	}

	public void setProject(String projectName) {
		project = projectName;
	}

	public String getProject() {
		return project;
	}

	public void setAccessClass(String acessClass) {
		this.accessClass = acessClass;
	}

	public String getAccessClass() {
		return accessClass;
	}

	public void setExportControl(String exportControl) {
		this.exportControl = exportControl;
	}

	public String getExportControl() {
		return exportControl;
	}

	public String getGoodUntilDate() {
		return goodUntilDate;
	}

	public void setGoodUntilDate(String date) {
		goodUntilDate = date;
	}

	public String getDomainID() {
		return domainId;
	}

	public void setDomainID(String id) {
		domainId = id;
	}

	public String getSubdomainID() {
		return subdomainId;
	}

	public void setSubdomainID(String id) {
		subdomainId = id;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String name) {
		domainName = name;
	}

	public String getSubdomainName() {
		return subdomainName;
	}

	public void setSubdomainName(String name) {
		subdomainName = name;
	}

	public SorcerPrincipal getPrincipal() {
		return principal;
	}

	public void setPrincipal(Principal principal) {
		this.principal = (SorcerPrincipal)principal;
	}

	public Hashtable getDelPathIds() {
		return delPathIds;
	}

	public float getVersion() {
		return version;
	}

	public void setVersion(float version) {
		this.version = version;
	}
	
	public T getReturnValue(Arg... entries) throws RemoteException,
			ContextException {
		T val = null;
		ReturnPath rp = returnPath;
		for (Arg a : entries) {
			if (a instanceof ReturnPath)
				rp = (ReturnPath)a;
		}
		if (rp != null) {
			try {
				if (rp.path == null || rp.path.equals("self")) {
					return (T) this;
				} else if (rp.argPaths != null) {
					 val = (T)getSubcontext(rp.argPaths);
				} else {
					if (rp.type != null) {
						val = (T) rp.type.cast(getValue(rp.path));
					}  else
						val= (T) getValue0(rp.path);
				}
			} catch (Exception e) {
				throw new ContextException(e);
			}
		}
		if (val instanceof Evaluation && isModeling) {
			val = ((Evaluation<T>) val).getValue(entries);
		} else if ((val instanceof Paradigmatic)
				&& ((Paradigmatic) val).isModeling()) {
			val = ((Evaluation<T>) val).getValue(entries);
		}
		return val;
	}
		
	/**
	 * {@inheritDoc}
	 * 
	 * @throws
	 */
	public Object getValue0(String path) throws ContextException {
		Object result = get(path);
		if (result instanceof ContextLink) {
			String offset = ((ContextLink) result).getOffset();
			Context linkedCntxt = null;
			try {
				linkedCntxt = ((ContextLink) result).getContext(principal);
			} catch (Exception ex) {
				throw new ContextException(ex);
			}
			result = linkedCntxt.getValue(offset);
		}
		if (result == null) {
			// could be in a linked context
			Enumeration e = localLinkPaths();
			String linkPath;
			int len;
			while (e.hasMoreElements()) {
				linkPath = (String) e.nextElement();
				ContextLink link = null;
				link = (ContextLink) get(linkPath);
				String offset = link.getOffset();
				int index = offset.lastIndexOf(CPS);
				String extendedLinkPath = linkPath;
				if (index < 0) {
					if (offset.length() > 0)
						extendedLinkPath = linkPath + CPS + offset;
				} else
					extendedLinkPath = linkPath + offset.substring(index);
				len = extendedLinkPath.length();
				if (path.startsWith(extendedLinkPath)
						&& (path.indexOf(CPS, len) == len || path.length() > len)) {
					// looking for something in this linked context
					String keyInLinkedCntxt = path.substring(len + 1);
					if (offset.length() > 0)
						keyInLinkedCntxt = offset + path.substring(len);
					Context linkedCntxt;
					linkedCntxt = getLinkedContext(link);
					result = linkedCntxt.getValue(keyInLinkedCntxt);
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Returns an enumeration of all paths marking input data nodes.
	 * 
	 * @return enumeration of marked input paths
	 * @throws ContextException
	 */
	public Enumeration inPaths() throws ContextException {
		String inAssoc = DIRECTION + SorcerConstants.APS + DA_IN;
		String inoutAssoc = DIRECTION + SorcerConstants.APS + DA_INOUT;
		String[] inPaths = Contexts.getMarkedPaths(this, inAssoc);
		String[] inoutPaths = Contexts.getMarkedPaths(this, inoutAssoc);
		Vector inpaths = new Vector();
		if (inPaths != null)
			for (int i = 0; i < inPaths.length; i++)
				inpaths.add(inPaths[i]);
		if (inoutPaths != null)
			for (int i = 0; i < inoutPaths.length; i++)
				inpaths.add(inoutPaths[i]);
		return inpaths.elements();
	}

	/**
	 * Returns a enumeration of all paths marking output data nodes.
	 * 
	 * @return enumeration of marked output paths
	 * @throws ContextException
	 */
	public Enumeration outPaths() throws ContextException {
		String outAssoc = DIRECTION + SorcerConstants.APS + DA_OUT;
		String[] outPaths = Contexts.getMarkedPaths(this, outAssoc);

		Vector outpaths = new Vector();
		if (outPaths != null)
			for (int i = 0; i < outPaths.length; i++)
				outpaths.add(outPaths[i]);

		return outpaths.elements();
	}

	@Override
	public T getSoftValue(String path) throws ContextException {
		T val = getValue(path);
		if (val == null) {
			try {
				int index = path.lastIndexOf(SorcerConstants.CPS);
				String attribute = path.substring(index+1);
				return getValueEndsWith(attribute);
			} catch (Exception e) {
				throw new ContextException(e);
			}
		} else {
			return val;
		}
	}
	
	// we assume that a path ending with name refers to its value
	public T getValueEndsWith(String name) throws EvaluationException,
			RemoteException {
		T val = null;
		Set<Map.Entry<String, T>> es = entrySet();
		Iterator<Map.Entry<String, T>> i = es.iterator();
		Map.Entry<String, T> entry;
		while (i.hasNext()) {
			entry = i.next();
			if (entry.getKey().endsWith(name)) {
				val = entry.getValue();
				if (val instanceof Evaluation && isModeling)
					val = ((Evaluation<T>) val).getValue();
			}
		}
		return val;
	}

	public Object getValueStartsWith(String name) throws EvaluationException,
			RemoteException {
		Object val = null;
		Set<Map.Entry<String, T>> es = entrySet();
		Iterator<Map.Entry<String, T>> i = es.iterator();
		Map.Entry<String, T> entry;
		while (i.hasNext()) {
			entry = i.next();
			if (entry.getKey().startsWith(name)) {
				val = entry.getValue();
				if (val instanceof Evaluation && isModeling)
					val = ((Evaluation) val).getValue();
			}
		}
		return val;
	}
	
//	/* (non-Javadoc)
//	 * @see sorcer.service.Mappable#getValue(java.lang.String, java.lang.Object)
//	 */
//	@Override
//	public Object getValue(String path, Object defaultValue)
//			throws ContextException {
//		T obj;
//		try {
//			obj = getValue(path);
//		} catch (Exception e) {
//			throw new ContextException(e);
//		}
//		if (obj != null)
//			return obj;
//		else
//			return defaultValue;
//	}

	/* (non-Javadoc)
	 * @see sorcer.service.AssociativeContext#putValue(java.lang.String, java.lang.Object)
	 */
	@Override
	public T putValue(final String path, Object value) throws ContextException {
		if(path==null)
			throw new IllegalArgumentException("path must not be null");
		// first test if path is in a linked context
		Enumeration e = null;
		e = localLinkPaths();
		String linkPath;
		int len;
		while (e.hasMoreElements()) {
			linkPath = (String) e.nextElement();
			// path has to start with linkPath+last_piece_of_offset
			ContextLink link = null;
			link = (ContextLink) get(linkPath);
			String offset = link.getOffset();
			int index = offset.lastIndexOf(CPS);
			// extendedLinkPath is the linkPath + the last piece of
			// the offset. We drop down in the link only if there is
			// a match here. This is required to distinguish from,
			// say, linkPath + m (where m is not the last piece of the
			// offset), which should not go into the linked context,
			// but in the linking context.
			//
			// be sure to handle these cases:
			// offset = "" -- the whole context linked
			// offset has no CPS, as in offset="abc"
			// offset has a CPS, as in offset="ab/c"
			String extendedLinkPath = linkPath;
			if (index < 0) {
				if (offset.length() > 0)
					extendedLinkPath = linkPath + CPS + offset;
			} else
				extendedLinkPath = linkPath + offset.substring(index);
			len = extendedLinkPath.length();
			if (path.startsWith(extendedLinkPath)
					&& (path.indexOf(CPS, len) == len || path.length() == len)) {
				String keyInLinkedCntxt;
				// for this path, find path in linked context
				if (offset.equals(""))
					keyInLinkedCntxt = path.substring(len + 1);
				else
					keyInLinkedCntxt = offset + path.substring(len);
				Context linkedCntxt = getLinkedContext(link);
				return (T)linkedCntxt.putValue(keyInLinkedCntxt, value);
			}
		}
		T obj = null;
		if (value == null)
			obj = put(path, (T)none);
		else {
			obj = get(path);
//			if (SdbUtil.isSosURL(obj)) {
//				try {
//				SdbUtil.update((URL)obj, value);
//			} catch (Exception ex) {
//				throw new ContextException(ex);
//			}
//		} else
			if (obj instanceof Reactive && obj instanceof Setter) {
				try {
					((Setter)obj).setValue(value);
				} catch (RemoteException ex) {
					throw new ContextException(ex);
				}
			} else {
				obj = put(path, (T)value);
			}
		}
		return obj;

	}

	public Object putValue(String path, Object value, String association)
			throws ContextException {
		// for the special case where the attribute-value pair or
		// (meta)association can be represented as a single string
		T obj = putValue(path, value);
		mark(path, association);

		if ((value instanceof ContextNode)
				&& association.startsWith(CONTEXT_PARAMETER))
			((ContextNode) value).setDA(SorcerUtil
					.secondToken(association, APS));
		return obj;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.base.ServiceContext#map(java.lang.String, java.lang.String,
	 * sorcer.base.ServiceContext)
	 */
	public void map(String fromPath, String toPath, Context toContext)
			throws ContextException {
		isShared = true;
		Contexts.map(fromPath, this, toPath, toContext);
	}

	/**
	 * <p>
	 * Contexts with mapped paths {@link #map} are indicated by the shared flag.
	 * </p>
	 * 
	 * @return the isShared
	 */
	public boolean isShared() {
		return isShared;
	}

	public void removeLink(String path) throws ContextException {
		// locate the context and context path for this key
		Object[] map = getContextMapping(path);// , true); // don't descend
		ServiceContext cntxt = (ServiceContext) map[0];
		String mappedKey = (String) map[1];
		if (cntxt.get(mappedKey) instanceof ContextLink) {
			cntxt.remove(mappedKey);
			cntxt.put(mappedKey, Context.EMPTY_LEAF);
		} else
			throw new ContextException("path = \"" + path
					+ "\" does not point to a ContextLink object");
	}

	public Object putLink(String name, String path, Context cntxt, String offset)
			throws ContextException {
		// insert a ContextLink (a.k.a. a symbolic link) to cntxt
		// this makes this.getValue(path) == cntxt.getValue(offset)
		if (path == null)
			throw new ContextException("ERROR: path is null");

		/*
		 * Allow adding to non context-leaf nodes // Check if this is a
		 * context-leaf node; throw exception otherwise; // this policy ensures
		 * namespace uniqueness; otherwise, rules for // aliased or shadowed
		 * paths must be devised. Enumeration e = getPaths(); String path; int
		 * len; while (e.hasMoreElements()) { path = (String)e.nextElement(); if
		 * (path.startsWith(key)) { len = path.length(); if
		 * (path.indexOf(CPS,len) == len) { throw new ContextException("ERROR:
		 * path \""+path+"\" is not a context-leaf node; remove dependent
		 * context-leaf nodes or choose another path in \""+getName()+"\"
		 * first"); } } }
		 */

		if (cntxt == null)
			throw new ContextException(
					"Failed to create ContextLink:  context to link is null");
		if (offset == null)
			throw new ContextException(
					"Failed to create ContextLink:  offset is null");
		
		String extendedLinkPath = path;
		if (offset.length() > 0)
			extendedLinkPath = path + CPS + offset;
		Enumeration paths = contextPaths();
		while (paths.hasMoreElements()) {
			if (((String) paths.nextElement()).startsWith(extendedLinkPath))
				throw new ContextException(
						"Failed to create ContextLink:  a path already exists that starts with \""
								+ extendedLinkPath
								+ "\".  This link cannot be added here.");
		}
		Object[] map = cntxt.getContextMapping(offset);
		if (map[0] == null || map[1] == null)
			throw new ContextException("ERROR: path \"" + offset
					+ "\" in context \"" + cntxt.getName() + "\" is invalid");

		// check if this cntxt is already loaded in memory.
		// ...

		// using map will collapse redundant links
		ContextLink link = new ContextLink((Context) map[0], (String) map[1]);
		// Put the link count against the path in the context
		if (name == null || name.length() == 0)
			link.setName(cntxt.getName());
		else
			link.setName(name);
		return putValue(path, (T)link);
	}

	public Object putLink(String path, Context cntxt, String offset)
			throws ContextException {
		return putLink("", path, cntxt, offset);
	}

	public Object putLink(String path, Context cntxt)
			throws ContextException {
		return putLink("", path, cntxt, "");
	}
	
	public Object putLink(String name, String path, String id, String offset)
			throws ContextException {
		// insert link to the most recent version context with
		// identification==id
		float version = (float) -1.0;
		return putLink(name, path, id, version, offset);
	}

	public Object putLink(String name, String path, String id, float version,
			String offset) throws ContextException {
		// insert a ContextLink (a.k.a. a symbolic link) to cntxt
		// this makes this.getValue(path) == cntxt.getValue(offset)

		// retrieve context from data store
		Context cntxt = null;
		// cntxt = cntxtAccessor.getContext(id, version, principal);
		// temporary
		cntxt = cntxtAccessor.getContext(id, principal);
		return putLink(name, path, cntxt, offset);
	}

	public Link getLink(String path) throws ContextException {
		ContextLink result = null;
		Object value;
		if (path == null)
			return null;
		value = get(path);
		if (value != null) {
			if (value instanceof ContextLink)
				result = (ContextLink) value;
		} else if (value == null) {
			// could be in a linked context
			Enumeration e = localLinkPaths();
			String linkPath;
			int len;
			while (e.hasMoreElements()) {
				linkPath = (String) e.nextElement();
				ContextLink link = (ContextLink) get(linkPath);
				String offset = link.getOffset();
				int index = offset.lastIndexOf(CPS);
				String extendedLinkPath = linkPath;
				if (index < 0) {
					if (offset.length() == 0)
						extendedLinkPath = linkPath + CPS + offset;
				} else
					extendedLinkPath = linkPath + offset.substring(index);
				len = extendedLinkPath.length();
				if (path.startsWith(extendedLinkPath)
						&& (path.indexOf(CPS, len) == len || path.length() == len)) {
					// looking for something in this linked context
					String keyInLinkedCntxt;
					if (offset.equals(""))
						keyInLinkedCntxt = path.substring(len + 1);
					else
						keyInLinkedCntxt = offset + path.substring(len);
					Context linkedCntxt = getLinkedContext(link);
					result = (ContextLink) linkedCntxt
							.getLink(keyInLinkedCntxt);
					break;
				}
			}
		}
		return result;
	}

	/*
	 * Returns array containing the ServiceContext in which path is found and
	 * the absolute path in that context.
	 */
	public Object[] getContextMapping(String path) throws ContextException {
		Object[] result = new Object[2];
		Object value;
		if (path == null)
			return null;
		value = get(path);
		if (value != null) {
			result[0] = this;
			result[1] = path;
		} else if (value == null) {
			Enumeration e = localLinkPaths();
			String linkPath;
			int len;
			while (e.hasMoreElements()) {
				linkPath = (String) e.nextElement();
				ContextLink link = (ContextLink) get(linkPath);
				String offset = link.getOffset();
				int index = offset.lastIndexOf(CPS);
				String extendedLinkPath;
				if (index < 0) {
					extendedLinkPath = linkPath + CPS + offset;
				} else
					extendedLinkPath = linkPath + offset.substring(index);
				len = extendedLinkPath.length();
				if (path.startsWith(extendedLinkPath)
						&& (path.indexOf(CPS, len) == len || path.length() == len)) {
					String keyInLinkedCntxt;
					if (offset.equals(""))
						keyInLinkedCntxt = path.substring(len + 1);
					else
						keyInLinkedCntxt = offset + path.substring(len);

					Context linkedCntxt = getLinkedContext(link);
					result = linkedCntxt.getContextMapping(keyInLinkedCntxt);
					break;
				}
			}
		}
		if (result[0] == null) {
			// the path belongs in this context, but is not in the
			// hashtable. We'll return the map anyway.
			// System.out.println("getContextMap: no mapping");
			result[0] = this;
			result[1] = path; // this is null
		}
		return result;
	}

	private Hashtable getDataAttributeMap() {
		return (Hashtable) metacontext.get(SorcerConstants.CONTEXT_ATTRIBUTES);
	}

	public Enumeration localAttributes() {
		return ((Hashtable) metacontext.get(SorcerConstants.CONTEXT_ATTRIBUTES))
				.keys();
	}

	protected Hashtable getDataAttributeMap(String attributeName) {
		if (isLocalAttribute(attributeName))
			return (Hashtable) metacontext.get(attributeName);
		else
			return null;
	}

	public void setAttribute(String descriptor) throws ContextException {
		String[] tokens = SorcerUtil.tokenize(descriptor, APS);
		if (tokens.length == 1)
			setComponentAttribute(descriptor);
		else
			setCompositeAttribute(descriptor);
	}

	public void setComponentAttribute(String attribute) {
		if (attribute.startsWith(PRIVATE) && attribute.endsWith(PRIVATE))
			return;
		getDataAttributeMap().put(attribute, attribute);
	}

	public void setCompositeAttribute(String descriptor)
			throws ContextException {
		// Register a composite ("composite|<component attributes>")
		// with this ServiceContext
		String composite = SorcerUtil.firstToken(descriptor, APS);
		if (composite.startsWith(PRIVATE) && composite.endsWith(PRIVATE))
			throw new ContextException("Illegal metaattribute name");
		String components = descriptor.substring(composite.length() + 1);
		getDataAttributeMap().put(composite, components);
		StringTokenizer st = new StringTokenizer(components, APS);
		String attribute;
		while (st.hasMoreTokens()) {
			attribute = st.nextToken();
			if (!isSingletonAttribute(attribute))
				setComponentAttribute(attribute);
		}
	}

	public boolean isLocalAttribute(String attribute) {
		// All Attributes are stored in this hashtable
		if (attribute.startsWith(PRIVATE) && attribute.endsWith(PRIVATE))
			return false;
		return getDataAttributeMap().containsKey(attribute);
	}

	public boolean isLocalSingletonAttribute(String attributeName) {
		// All Attributes are stored in the localContextAttributes hashtable
		// and singletons have key equal to the value
		return isLocalAttribute(attributeName)
				&& getDataAttributeMap().get(attributeName).equals(
						attributeName);
	}

	public boolean isLocalMetaattribute(String attributeName) {
		// Metaattributes are stored in the localContextAttributes
		// hashtable and have key equal to the attribute set, not the
		// value as with singleton attributes
		return isLocalAttribute(attributeName)
				&& !getDataAttributeMap().get(attributeName).equals(
						attributeName);
	}

	public boolean isAttribute(String attributeName) throws ContextException {
		boolean result = isLocalAttribute(attributeName);
		if (!result) {
			// not an attribute of the top-level context; check all
			// top-level linked contexts (which in turn will check
			// their top-level contexts, etc. until a match is found or
			// all contexts are exhausted )
			Enumeration e = null;
			e = localLinks();
			ContextLink link;
			while (e.hasMoreElements()) {
				link = (ContextLink) e.nextElement();
				result = getLinkedContext(link).isAttribute(attributeName);
				if (result)
					break;
			}
		}
		return result;
	}

	public boolean isSingletonAttribute(String attributeName)
			throws ContextException {
		// All Attributes are stored in the localContextAttributes hashtable
		// and singletons have key equal to the value
		boolean result = isLocalAttribute(attributeName)
				&& getDataAttributeMap().get(attributeName).equals(
						attributeName);
		if (!result) {
			// not an attribute of the top-level context; check all
			// top-level linked contexts (which in turn will check
			// their top-level contexts, etc. until a match is found or
			// all contexts are exhausted)
			Enumeration e = localLinks();
			ContextLink link;
			while (e.hasMoreElements()) {
				link = (ContextLink) e.nextElement();
				result = getLinkedContext(link).isSingletonAttribute(
						attributeName);
				if (result)
					break;
			}
		}
		return result;
	}

	public boolean isMetaattribute(String attributeName)
			throws ContextException {
		// Metaattributes are stored in the localContextAttributeisLos
		// hashtable and have key equal to the attribute set, not the
		// value as with singleton attributes
		boolean result = isLocalAttribute(attributeName)
				&& !getDataAttributeMap().get(attributeName).equals(
						attributeName);
		if (!result) {
			// not an attribute of the top-level context; check all
			// top-level linked contexts (which in turn will check
			// their top-level contexts, etc. until a match is found or
			// all contexts are exhausted)
			Enumeration e = localLinks();
			ContextLink link;
			while (e.hasMoreElements()) {
				link = (ContextLink) e.nextElement();
				result = getLinkedContext(link).isMetaattribute(attributeName);
				if (result)
					break;
			}
		}
		return result;
	}

	public String getAttributeValue(String path, String attributeName)
			throws ContextException {
		String attr;
		attr = getSingletonAttributeValue(path, attributeName);
		if (attr != null)
			return attr;
		return getMetaattributeValue(path, attributeName);
	}

	public String getSingletonAttributeValue(String path, String attributeName)
			throws ContextException {
		String val = null;
		Hashtable table;

		// locate the context and context path for this key
		Object[] map = getContextMapping(path);
		ServiceContext cntxt = (ServiceContext) map[0];
		String mappedKey = (String) map[1];

		if (cntxt.isSingletonAttribute(attributeName)) {
			table = (Hashtable) cntxt.metacontext.get(attributeName);
			if (table != null) {
				val = (String) table.get(mappedKey);
			}
		}
		return val;
	}

	public String getMetaattributeValue(String path, String attributeName)
			throws ContextException {
		String attrValue, result = null;

		// locate the context and context path for this key
		Object[] map = getContextMapping(path);
		Context cntxt = (Context) map[0];
		String mappedKey = (String) map[1];

		String metapath = cntxt.getLocalMetapath(attributeName);

		if (metapath != null) {
			String[] attrs = SorcerUtil.tokenize(metapath, APS);
			StringBuffer sb = new StringBuffer();
			int count = 0;
			for (int i = 0; i < attrs.length; i++) {
				attrValue = cntxt.getAttributeValue(mappedKey, attrs[i]);
				if (attrValue == null)
					count++;
				sb.append(attrValue);
				if (i + 1 < attrs.length)
					sb.append(APS);
			}
			if (count < attrs.length)
				result = sb.toString();
		}
		return result;
	}

	public Context mark(String path, String association) throws ContextException {
		int firstAPS = association.indexOf(APS);
		if (firstAPS <= 0)
			throw new ContextException(
					"No attribute or metaattribute specified in: "
							+ association);

		String[] attributes = SorcerUtil.tokenize(association, APS);
		String values = association.substring(attributes[0].length() + 1);
		if (attributes.length == 2)
			return addComponentAssociation(path, attributes[0], values);
		else
			return addCompositeAssociation(path, attributes[0], values);
	}

	public Context addComponentAssociation(String path, String attribute,
			String attributeValue) throws ContextException {
		Hashtable values;
		// locate the context and context path for this key
		Object[] map = getContextMapping(path);
		ServiceContext cntxt = (ServiceContext) map[0];
		String mappedKey = (String) map[1];

		if (cntxt.isSingletonAttribute(attribute)) {
			values = (Hashtable) cntxt.metacontext.get(attribute);
			if (values == null) {
				// the creation of this hashtable was delayed until now
				values = new Hashtable();
				cntxt.metacontext.put(attribute, values);
			}
			values.put(mappedKey, attributeValue);
		} else if (cntxt.isMetaattribute(attribute))
			cntxt.addCompositeAssociation(mappedKey, attribute, attributeValue);
		else
			throw new ContextException("No attribute defined: \"" + attribute
					+ "\" in this context (name=\"" + cntxt.getName() + "\"");
		return this;
	}

	public Context addCompositeAssociation(String path, String metaattribute,
			String metaattributeValue) throws ContextException {

		// locate the context and context path for this path
		Object[] map = getContextMapping(path);
		Context cntxt = (Context) map[0];
		String mappedKey = (String) map[1];

		if (!cntxt.isMetaattribute(metaattribute))
			throw new ContextException("No metaattribute defined: "
					+ metaattribute + " in context " + cntxt.getName());
		String[] attrs = SorcerUtil.tokenize(
				cntxt.getLocalMetapath(metaattribute), APS);
		String[] vals = SorcerUtil.tokenize(metaattributeValue, APS);
		if (attrs.length != vals.length)
			throw new ContextException("Invalid:  The metavalue of \""
					+ metaattributeValue + "\" for metaattribute \""
					+ metaattribute + APS + getLocalMetapath(metaattribute)
					+ "\" is invalid in this context (name=\""
					+ cntxt.getName() + "\"");
		for (int i = 0; i < attrs.length; i++)
			((ServiceContext) cntxt).addComponentAssociation(mappedKey,
					attrs[i], vals[i]);
		return this;
	}

	public Enumeration markedPaths(String association) throws ContextException {
		String attr, value;
		Map values;
		// java 1.4.0 regex
		// Pattern p;
		// Matcher m;
		if (association == null)
			return null;
		int index = association.indexOf(SorcerConstants.APS);
		if (index < 0)
			return null;

		attr = association.substring(0, index);
		value = association.substring(index + 1);
		if (!isAttribute(attr))
			throw new ContextException("No Attribute defined: " + attr);

		Vector keys = new Vector();
		if (isSingletonAttribute(attr)) {
			values = (Map)getMetacontext().get(attr);
			if (values != null) { // if there are no attributes set,
				// values==null;
				for (Object key : values.keySet()) {
					/*
					 * java 1.4.0 regex p = Pattern.compile(value); m =
					 * p.matcher((String)values.get(key)); if (m.find())
					 * keys.addElement(key);
					 */
					if (values.get(key).equals(value))
						keys.addElement(key);
				}
			}
		} else {
			// it is a metaattribute
			String metapath = getLocalMetapath(attr);
			if (metapath != null) {
				String[] attrs = SorcerUtil.tokenize(metapath,
						SorcerConstants.APS);
				String[] vals = SorcerUtil.tokenize(value, SorcerConstants.APS);
				if (attrs.length != vals.length)
					throw new ContextException("Invalid association: \""
							+ association + "\"  metaattribute \"" + attr
							+ "\" is defined with metapath =\"" + metapath
							+ "\"");
				Object[][] paths = new Object[attrs.length][];
				Enumeration ps;
				int ii = -1;
				for (int i = 0; i < attrs.length; i++) {
					ps = markedPaths(attrs[i] + SorcerConstants.APS + vals[i]);
					paths[i] = SorcerUtil.makeArray(ps);
					if (paths[i] == null) {
						ii = -1;
						break; // i.e. no possible match
					}
					if (ii < 0 || paths[i].length > paths[ii].length) {
						ii = i;
					}
				}
				if (ii >= 0) {
					// The common paths across the paths[][] array are
					// matches. Said another way, the paths[][] array
					// contains all the paths that match attributes in the
					// metapath. paths[0][] are the matches for the first
					// element of the metapath, paths[1][] for the next,
					// etc. Therefore, the matches that are common for
					// each element of the metapath are the ones in which
					// we have interest.
					String candidate;
					int match, thisMatch;
					// go through each element of one with most matches
					for (int i = 0; i < paths[ii].length; i++) {
						candidate = (String) paths[ii][i];
						// now look for paths.length-1 matches...
						match = 0;
						for (int j = 0; j < paths.length; j++) {
							if (j == ii)
								continue;
							thisMatch = 0;
							for (int k = 0; k < paths[j].length; k++)
								if (candidate.equals(paths[j][k])) {
									match++;
									thisMatch++;
									break;
								}
							if (thisMatch == 0)
								break; // no possible match for this candidate
						}
						// System.out.println("candidate="+candidate+"
						// match="+match+" required maches="+(paths.length-1));
						if (match == paths.length - 1)
							keys.addElement(candidate);
					}
				}
			}
		}
		// above we just checked the top-level context; next, check
		// all the top-level LINKED contexts (which in turn will check
		// all their top-level linked contexts, etc.)
		Enumeration e = localLinkPaths();
		ContextLink link;
		String linkPath;
		Enumeration keysInLinks;
		while (e.hasMoreElements()) {
			linkPath = (String) e.nextElement();
			link = (ContextLink) get(linkPath);
			ServiceContext lcxt = (ServiceContext) getLinkedContext(link);
			keysInLinks = lcxt.markedPaths(association);
			if (keysInLinks != null)
				while (keysInLinks.hasMoreElements()) {
					keys.addElement(linkPath + SorcerConstants.CPS
							+ keysInLinks.nextElement());
				}
		}
		return keys.elements();
	}

	public void removeAttributeValue(String path, String attributeValue)
			throws ContextException {
		String attr;
		// accept also metaassociation
		if (attributeValue.indexOf(APS) > 0)
			attr = SorcerUtil.firstToken(attributeValue, APS);
		else
			attr = attributeValue;

		// locate the context and context path for this key
		Object[] map = null;
		map = getContextMapping(path);

		ServiceContext cntxt = (ServiceContext) map[0];
		String mappedKey = (String) map[1];

		if (cntxt.isSingletonAttribute(attr)) {
			Hashtable metavalues = (Hashtable) cntxt.getMetacontext().get(attr);

			if (metavalues == null)
				return;
			metavalues.remove(mappedKey);
			// remove Hashtable if it is now empty
			if (metavalues.size() == 0)
				metacontext.remove(attr);
		} else if (cntxt.isMetaattribute(attr)) {
			String[] attrs = SorcerUtil.tokenize(cntxt.getLocalMetapath(attr),
					APS);
			for (int i = 0; i < attrs.length; i++)
				cntxt.removeAttributeValue(mappedKey, attrs[i]);
		} else
			throw new ContextException("No attribute defined: " + attr
					+ " in context " + cntxt.getName());
	}

	public String getLocalMetapath(String metaattribute)
			throws ContextException {
		// return the metapath (attribute-name n-tuple) equivalent to
		// this metaattribute; format is a String with attributes
		// separated by the CMPS (context metapath separator)
		if (isMetaattribute(metaattribute))
			return (String) getDataAttributeMap().get(metaattribute);
		else
			return null;
	}

	public boolean isValid(Signature signature) throws ContextException {
		Provider provider = getProvider();
		if (provider != null)
			return ((ServiceProvider) provider).isContextValid(this, signature);
		else {
			return true;
		}
	}

	public Enumeration<String> paths(String regex) throws ContextException {
		Enumeration e = contextPaths();
		Vector list = new Vector();
		Pattern p = Pattern.compile(regex);
		String path;
		while (e.hasMoreElements()) {
			path = (String) e.nextElement();
			if (p.matcher(path).matches())
				list.add(path);
		}
		return list.elements();
	}

	public List<String> getPaths() throws ContextException {
		ArrayList<String> paths = new ArrayList<String>();
		Enumeration e = keys();
		String key, path;
		ContextLink link;
		Context subcntxt;
		while (e.hasMoreElements()) {
			key = (String) e.nextElement();
			if (get(key) instanceof ContextLink) {
				// follow link, add paths
				link = (ContextLink) get(key);
				try {
					subcntxt = getLinkedContext(link)
							.getContext(link.getOffset());
				} catch (RemoteException ex) {
					throw new ContextException(ex);
				}
				// getSubcontext cuts above, which is what we want
				Enumeration el = subcntxt.contextPaths();
				while (el.hasMoreElements()) {
					path = (String) el.nextElement();						
					paths.add(key + CPS +path);
				}
			}
			paths.add(key);
		}
		Collections.sort(paths);
		return paths;
	}
	
	public Enumeration<String> contextPaths() throws ContextException {
		Vector keys = new Vector(getPaths());
		return keys.elements();
	}

	public Enumeration contextValues() throws ContextException {
		Enumeration e = contextPaths();
		Vector vec = new Vector();
		while (e.hasMoreElements())
			try {
				vec.addElement(getValue((String) e.nextElement()));
			} catch (Exception ex) {
				throw new ContextException(ex);
			}
		return vec.elements();
	}

	public Enumeration<String> localLinkPaths() throws ContextException {
		Vector keys = new Vector();
		Enumeration e = keys();
		String key;

		while (e.hasMoreElements()) {
			key = (String) e.nextElement();
			if (get(key) instanceof ContextLink)
				keys.addElement(key);
		}
		SorcerUtil.bubbleSort(keys);
		return keys.elements();
	}

	/**
	 * Returns a list of all paths marked as data input.
	 * 
	 * @return list of all paths marked as input
	 * @throws ContextException
	 */
	public List<String> getInPaths() throws ContextException {
		return Contexts.getInPaths(this);
	}

	public ParList getPars() {
		ParList pl = new ParList();
		Set<Map.Entry<String, T>> es = entrySet();
		Iterator<Map.Entry<String, T>> i = es.iterator();
		Map.Entry<String, T> entry;
		while (i.hasNext()) {
			entry = i.next();
			if (entry.getValue() instanceof Par) {
				pl.add((Par)entry.getValue());
			}
		}
		return pl;
	}
	
	/**
	 * Returns a list of all paths marked as data output.
	 * 
	 * @return list of all paths marked as data output
	 * @throws ContextException
	 */
	public List<String> getOutPaths() throws ContextException {
		return Contexts.getOutPaths(this);
	}

	/**
	 * Returns a list of input context values marked as data input.
	 * 
	 * @return a list of input values of this context
	 * @throws ContextException
	 * @throws ContextException
	 */
	public List<Object> getInValues() throws ContextException {
		List<?> inpaths = Contexts.getInPaths(this);
		List<Object> list = new ArrayList<Object>(inpaths.size());
		for (Object path : inpaths)
			try {
				list.add(getValue((String) path));
			} catch (Exception e) {
				throw new ContextException(e);
			}
		return list;
	}

	/**
	 * Returns a list of output context values marked as data input.
	 * 
	 * @throws ContextException
	 * 
	 * @return list of output values of this context
	 * @throws ContextException
	 */
	public List<Object> getOutValues() throws ContextException {
		List<?> outpaths = Contexts.getOutPaths(this);
		List<Object> list = new ArrayList<Object>(outpaths.size());
		for (Object path : outpaths)
			try {
				list.add(getValue((String) path));
			} catch (Exception e) {
				throw new ContextException(e);
			}

		return list;
	}

	public Enumeration<String> linkPaths() throws ContextException {
		// returns paths to all ContextLink objects
		Vector<String> keys = new Vector<String>();
		Enumeration<String> e = keys();
		String key, path;
		ContextLink link;
		Context subcntxt = null;

		while (e.hasMoreElements()) {
			key = e.nextElement();
			if (get(key) instanceof ContextLink) {
				keys.addElement(key);
				link = (ContextLink) get(key);
				// get subcontext for recursion
				try {
					subcntxt = getLinkedContext(link)
							.getContext(link.getOffset());
				} catch (RemoteException ex) {
					throw new ContextException(ex);
				}
				// getSubcontext cuts above, which is what we want
				Enumeration<?> el = subcntxt.linkPaths();
				while (el.hasMoreElements()) {
					path = (String) el.nextElement();
					keys.addElement(key + CPS + path);
				}
			} else {
				keys.addElement(key);
			}
		}// end of instance of ContextLink
		SorcerUtil.bubbleSort(keys);
		return keys.elements();
	}

	public Enumeration<Link> links() throws ContextException {
		Enumeration<String> e = linkPaths();
		String path;
		Vector<Link> links = new Vector<Link>();
		while (e.hasMoreElements()) {
			path = (String) e.nextElement();
			links.addElement(getLink(path));
		}
		return links.elements();
	}

	public Enumeration<Link> localLinks() throws ContextException {
		Enumeration<String> e = localLinkPaths();
		String path;
		Vector<Link> links = new Vector<Link>();
		while (e.hasMoreElements()) {
			path = e.nextElement();
			links.addElement(getLink(path));
		}
		return links.elements();
	}

	public Context getSubcontext() {
		// bare-bones subcontext
		Context subcntxt = new ServiceContext();
		subcntxt.setSubject(subjectPath, subjectValue);
		subcntxt.setName(getName() + " subcontext");
		subcntxt.setDomainID(getDomainID());
		subcntxt.setSubdomainID(getSubdomainID());
		return subcntxt;
	}

	public Context getSubcontext(String[] paths) {
		ServiceContext subcntxt = (ServiceContext)getSubcontext();
		for (String path : paths) {
			subcntxt.put(path, get(path));
		}
		return subcntxt;
	}

	public Context getSubcontext(List<String> paths) {
		ServiceContext subcntxt = (ServiceContext)getSubcontext();
		for (String path : paths) {
			subcntxt.put(path, get(path));
		}
		return subcntxt;
	}
	
	public Context getContext(String path) throws ContextException, RemoteException {
		Context subcntxt = this.getSubcontext();
		return subcntxt.appendContext(this, path);
	}

	public Context getTaskContext(String path) throws ContextException {
		// needed for ContextFilter
		return null;
	}
	
	public Context appendNewEntries(Context context) throws ContextException {
		if (context != null) {
			Map<String, Object> contextMap = (Map) context;
			Iterator it = contextMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Object> pairs = (Map.Entry) it.next();
				if (!containsKey(pairs.getKey())) {
					put(pairs.getKey(), (T)pairs.getValue());
				}
			}
			if (containsKey(Condition._closure_))
				remove(Condition._closure_);
		}
		return this;
	}
	/* (non-Javadoc)
	 * @see sorcer.service.Context#append(sorcer.service.Context)
	 */
	public Context append(Context context) throws ContextException {
		if (context != null) {
			putAll((ServiceContext) context);
			if (containsKey(Condition._closure_))
				remove(Condition._closure_);
		}

//		Iterator<Map.Entry<String, T>> i = this.entrySet().iterator();
//		List<String> pl = new ArrayList<String>();
//		Object val;
//		while (i.hasNext()) {
//			Map.Entry<String, T> e = i.next();
//			val = e.getValue();
//			if (val != null && val instanceof Par) {
//				ServiceContext cxt = (ServiceContext) ((Par) val).getScope();
//				if (cxt != null && cxt.containsKey(Condition._closure_)) 
//					pl.add(e.getKey());
//			}
//		}
//		for (String key : pl) {
//			((Par)get(key)).getScope().remove(Condition._closure_);
//			// remove potential loops
//			((Par)get(key)).getScope().remove(key);
//			((ServiceContext)((Par)get(key)).getScope()).clear();
//		}
		return this;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Contexter#appendContext(sorcer.service.Context)
	 */
	@Override
	public Context<T> appendContext(Context<T> context) throws ContextException,
			RemoteException {
		// get the whole context, with the context root name as the
		// path prefix
		String key;
		Vector vec = new Vector();
		int index;
		// pick off all top-level nodes to append
		Enumeration e = context.contextPaths();
		while (e.hasMoreElements()) {
			key = (String) e.nextElement();
			index = key.indexOf(CPS);
			if (index != -1)
				key = key.substring(0, index);
			if (!vec.contains(key))
				vec.addElement(key);
		}
		e = vec.elements();
		while (e.hasMoreElements()) {
			appendContext(context, (String) e.nextElement(), true);
		}
		return this;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Contexter#appendContext(sorcer.service.Context, java.lang.String)
	 */
	public Context appendContext(Context cntxt, String path)
			throws ContextException, RemoteException {
		return appendContext(cntxt, path, false);
	}

	public Context appendContext(Context cntxt, String path,
			boolean prefixContextName) throws ContextException,  RemoteException {
		// appendContext snips the context (passed in as the first
		// argument) BEFORE the requested node and returns it appended
		// to the context object. Said another way: if the context, ctx,
		// has the following paths
		//
		// a/b
		// a/b/c
		// d/e
		//
		// appendSubcontext(ctx, "a/b") returns context with keys
		// b
		// b/c
		//
		// appendSubcontext(ctx, "a") returns context with keys
		// a/b
		// a/b/c

		// path should not have a trailing slash

		String newKey, oldKey, cntxtKey;
		int index;
		Enumeration e1;
		if (path == null)
			throw new ContextException("null path");
		if (path.equals("")) {
			// append entire context
			return appendContext(cntxt);
		}

		Object[] map = null;
		map = cntxt.getContextMapping(path);
		ServiceContext mappedCntxt = (ServiceContext) map[0];
		String mappedKey = (String) map[1];
		// System.out.println("path="+path);
		// System.out.println("mappedKey="+mappedKey);
		// System.out.println("orig context name="+cntxt.getName());
		// System.out.println("mapped context name="+mappedCntxt.getName());

		int len = mappedKey.length();
		String prefix;
		Enumeration e = ((Hashtable) mappedCntxt).keys();
		while (e.hasMoreElements()) {
			cntxtKey = (String) e.nextElement();
			if (cntxtKey.startsWith(mappedKey)) {
				// we could still have the case key="a/b"
				// cntxtKey="a/bc", which should fail, but
				// cntxtKey="a/b" or cntxtKey="a/b/*" passes.
				// This next conditional should do the trick:
				if (cntxtKey.length() == len
						|| cntxtKey.indexOf(CPS, len) == len) {
					index = mappedKey.lastIndexOf(CPS, len - 1);
					if (index > 0)
						newKey = cntxtKey.substring(index + 1);
					else
						newKey = cntxtKey;
					oldKey = cntxtKey;
					// should we test for clobber protection?
					// i.e. these new keys could be dropped on old ones
					if (prefixContextName) {
						prefix = "";
						if (mappedCntxt.getSubjectPath().length() > 0)
							prefix = mappedCntxt.getSubjectPath() + CPS;
						putValue(prefix + newKey,
								(T)((Hashtable) mappedCntxt).get(oldKey));
					}
					else
						putValue(newKey, (T)((Hashtable) mappedCntxt).get(oldKey));
				}
			}
		}
		// replicate subcontext attributes and metaattributes
		Hashtable table, attrTable;
		attrTable = ((ServiceContext) mappedCntxt).metacontext;
		// note the metacontext contains only singleton attributes
		// AND the SORCER.CONTEXT_ATTRIBUTES table
		e = attrTable.keys();
		String attr, val, metapath;
		while (e.hasMoreElements()) {
			attr = (String) e.nextElement();
			// make sure we don't enumerate over the CONTEXT_ATTRIBUTES
			if (attr.equals(CONTEXT_ATTRIBUTES))
				continue;
			table = (Hashtable) attrTable.get(attr);
			e1 = table.keys();
			while (e1.hasMoreElements()) {
				cntxtKey = (String) e1.nextElement();
				if (cntxtKey.startsWith(mappedKey)) {
					if (cntxtKey.length() == len
							|| cntxtKey.indexOf(CPS, len) == len) {
						index = mappedKey.lastIndexOf(CPS, len - 1);
						if (index > 0)
							newKey = cntxtKey.substring(index + 1);
						else
							newKey = cntxtKey;
						oldKey = cntxtKey;
						val = (String) table.get(oldKey);
						if (!isSingletonAttribute(attr))
							setComponentAttribute(attr);
						if (prefixContextName)
							addComponentAssociation(mappedCntxt.getName() + CPS
									+ newKey, attr, val);
						else
							addComponentAssociation(newKey, attr, val);
					}
				}
			}
		}

		// now all attributes are set, and metaattributes are set
		// implicitly IF the metaattribute definitions are set in the
		// new context. So, next we set the definitions, or at least
		// try...

		String metapath_target, metapath_source;
		// enumerate over local metaattributes
		e = mappedCntxt.getDataAttributeMap().keys();
		while (e.hasMoreElements()) {
			attr = (String) e.nextElement();
			if (!mappedCntxt.isLocalMetaattribute(attr))
				continue;
			// is this also an attribute in the current context?
			if (isSingletonAttribute(attr)) {
				logger.info("The attribute \""
						+ attr
						+ "\" has conflicting definitions; it is a metaattribute in the source context and a singleton attribute in the target context.  Please correct before performing this operation");
				logger.info("Src metacontext="
						+ mappedCntxt.metacontext);
				logger.info("this metacontext=" + metacontext);
				throw new ContextException("The attribute \"" + attr
						+ "\" has conflicting definitions;");// it
				// is a metaattribute in the source context and a singleton
				// attribute in the target context.
				// Please correct before performing this operation");
			}
			// is this also a metaattribute in the current context?
			if (isMetaattribute(attr)) {
				// check to see the definitions are the same
				metapath_source = (String) mappedCntxt
						.getDataAttributeMap().get(attr);
				metapath_target = (String) getDataAttributeMap().get(attr);
				if (!metapath_target.equals(metapath_source))
					throw new ContextException("The metaattribute \"" + attr
							+ "\" has conflicting definitions");// in
				// the source and target contexts; in the source
				// context, it has metapath = \""+metapath_source+"\",
				// while in the target context it
				// has metapath = \""+metapath_target+"\".
				// Please correct befe performing this operation.");
			}
			metapath = (String) mappedCntxt
					.getDataAttributeMap().get(attr);
			setCompositeAttribute(attr + APS + metapath);
		}
		return this;
	}

	public void removePath(String path) throws ContextException {
		// locate the context and context path for this key
		Object[] map = null;
		map = getContextMapping(path);
		ServiceContext cntxt = (ServiceContext) map[0];
		String mappedKey = (String) map[1];
		cntxt.remove(mappedKey);
		// Remove the path if it exists in metaAttribute also.
		Enumeration e = cntxt.metacontext.keys();
		String tmpKey;
		Hashtable attributeHash;
		while (e.hasMoreElements()) {
			tmpKey = (String) e.nextElement();
			if (tmpKey.startsWith(PRIVATE) && tmpKey.endsWith(PRIVATE))
				continue;
			attributeHash = (Hashtable) cntxt.metacontext.get(tmpKey);
			if (attributeHash.containsKey(mappedKey))
				attributeHash.remove(mappedKey);
		}
	}

	public String toString(String cr, StringBuilder sb, boolean withMetacontext) {
		sb.append(subjectPath.length() == 0 ? "" : "\n  subject: "
				+ subjectPath + ":" + subjectValue + cr);
		Enumeration e = null;
		try {
			e = contextPaths(); // sorted enumeration
		} catch (ContextException ex) {
			sb.append("ERROR: ContextException thrown: " + ex.getMessage());
			return sb.toString();
		}
		String path;
		Object val;
		int count = 0;
		while (e.hasMoreElements()) {
			path = (String) e.nextElement();
			val = get(path);					
			if (!(val instanceof ContextLink)) {
				if (count >= 1)
					sb.append(cr);
				sb.append("  " + path).append(" = ");
			}
			// if (val instanceof ContextLink) {
			// sb.append(val.toString() + " ");
			// }
			try {
				if (val instanceof Par) 
					val = "par: " + ((Par)val).getName();
				else
					val = getValue(path);
			} catch (Exception ex) {
				sb.append("\nUnable to retrieve value: " + ex.getMessage());
				ex.printStackTrace();
				val = Context.none;;
//				continue;
			}
			// if (val == null)
			// sb.append("null");
			if (val != null) {
				if (val.getClass().isArray())
					sb.append(SorcerUtil.arrayToString(val));
				else if (val instanceof ContextNode
						&& ((ContextNode) val).isURL()) {
					URL url;
					try {
						url = ((ContextNode) val).getURL();
						sb.append("<a href=").append(url).append(">")
								.append(url).append("</a>");
					} catch (MalformedURLException e2) {
						e2.printStackTrace();
					} catch (ContextNodeException e2) {
						e2.printStackTrace();
					}
				} else if (val instanceof Exertion) {
					sb.append(((ServiceExertion) val).info());
				} else
					sb.append(val.toString());
			}
			count++;
		}
		if (returnPath != null) {
			sb.append("\n  return/path = " + returnPath);
		}
		if (returnJobPath != null) {
			sb.append("\n  return/job/path = " + returnJobPath);
		}
		if (withMetacontext)
			sb.append("metacontext: " + metacontext);
		
		// sb.append(cr);
		// sb.append(cr);
		if (cr.equals("<br>"))
			sb.append("</html>");
		return sb.toString();
	}

	public String toStringComplete(String cr, StringBuffer sb) {
		sb.append("Domain:").append(domainId);
		sb.append(" SubDomain:" + subdomainId);
		sb.append(" Scope:" + scopeCode);
		sb.append(" ID:" + contextId);
		sb.append("\nPaths: \n");
		Enumeration e = null;
		try {
			e = contextPaths(); // sorted enumeration
		} catch (ContextException ex) {
			sb.append("ERROR: ContextException thrown: " + ex.getMessage());
			return sb.toString();
		}
		Enumeration e1;
		String path;
		Object val;
		while (e.hasMoreElements()) {
			path = (String) e.nextElement();
			// System.out.print(path);
			sb.append(path).append(" = ");
			try {
				// System.out.println(" = "+getValue(path));
				val = getValue(path);
			} catch (Exception ex) {
				sb.append("\nUnable to retrieve value: " + ex.getMessage());
				ex.printStackTrace();
				continue;
			}
			if (val == null)
				sb.append("null");
			else if (val.getClass().isArray())
				sb.append(SorcerUtil.arrayToString(val));
			else
				sb.append(val.toString());
			// report attributes
			try {
				e1 = Contexts.getSimpleAssociations(this, path);
			} catch (ContextException ex) {
				sb.append("Unable to retrieve associations: " + ex.getMessage());
				continue;
			}
			if (e1 != null && e1.hasMoreElements()) {
				sb.append(" {");
				while (e1.hasMoreElements()) {
					sb.append(e1.nextElement());
					if (e1.hasMoreElements())
						sb.append(", ");
				}
				sb.append("}");
			}
			try {
				e1 = metaassociations(path);
			} catch (ContextException ex) {
				sb.append("Unable to retrieve metaassociations: "
						+ ex.getMessage());
				continue;
			}
			if (e1 != null && e1.hasMoreElements()) {
				sb.append(" {");
				while (e1.hasMoreElements()) {
					sb.append(e1.nextElement());
					if (e1.hasMoreElements())
						sb.append(", ");
				}
				sb.append("}");
			}
			sb.append(cr);
		}
		return sb.toString();
	}

	public String toString() {
		return toString(false, false);
	}

	public String toString(boolean isHTML) {
		if (isHTML)
			return toString(isHTML, false);
		else
			return toString(isHTML, true);
	}

	public String toString(boolean isHTML, boolean withMetacontext) {
		String cr; // Carriage return
		StringBuilder sb; // String buffer
		if (isHTML) {
			cr = "<br>";
			// sb = new StringBuilder("<html>\nContext name: ");
			sb = new StringBuilder("<html>\n");
		} else {
			cr = "\n";
			sb = new StringBuilder(name != null ? "Context name: " + name
					+ "\n" : "");
			// sb = new StringBuilder();
		}
		// sb.append(name).append("\n");
		return toString(cr, sb, withMetacontext);
	}

	public Hashtable getMetacontext() {
		return metacontext;
	}

	public boolean isExportControlled() {
		if ("1".equals(exportControl))
			return true;
		else
			return false;
	}

	public void connect(String outPath, String inPath, Context inContext)
			throws ContextException {
		Contexts.markIn(inContext, inPath);
		Contexts.markOut(this, outPath);
		map(outPath, inPath, inContext);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Context#pipe(java.lang.String, java.lang.String,
	 * sorcer.service.Context)
	 */
	@Override
	public void pipe(String inPath, String outPath, Context outContext)
			throws ContextException {
		Contexts.markInPipe(this, inPath);
		Contexts.markOutPipe(outContext, outPath);
		map(outPath, inPath, outContext);

	}

	public T putInValue(String path, T value) throws ContextException {
		putValue(path, value);
		Contexts.markIn(this, path);
		return value;
	}

	public T putOutValue(String path, T value)
			throws ContextException {
		putValue(path, value);
		Contexts.markOut(this, path);
		return value;
	}

    public Object putErrValue(String path, T value)
            throws ContextException {
        putValue(path, value);
        Contexts.markOut(this, path);
        return value;
    }

	public Object[] getArgs() throws ContextException {
		if (argsPath == null)
			return null;
		else
			return (Object[])get(argsPath);
	}

	public ServiceContext setArgs(Object... args) throws ContextException {
		if (argsPath == null) 
			argsPath = Context.PARAMETER_VALUES;
		putInValue(argsPath, (T)args);
		return this;
	}

	public String getArgsPath() {
		return argsPath;
	}

	public ServiceContext setArgsPath(String targetPath)
			throws ContextException {
		argsPath = targetPath;
		return this;
	}

	public Class[] getParameterTypes() throws ContextException {
		if (parameterTypesPath != null)
			return (Class[]) getValue(parameterTypesPath);
		else
			return null;
	}

	public ServiceContext setParameterTypes(Class... types) throws ContextException {
		if (parameterTypesPath == null) 
			parameterTypesPath = Context.PARAMETER_TYPES;
		putValue(parameterTypesPath, (T)types);
		return this;
	}
	
	public String getParameterTypesPath() {
		return parameterTypesPath;
	}

	public ServiceContext setParameterTypesPath(String targetPath)
			throws ContextException {
		parameterTypesPath = targetPath;
		return this;
	}
	
	public Object getTarget() throws ContextException {
		try {
			return getValue(targetPath);
		} catch (Exception e) {
			throw new ContextException(e);
		}
	}

	public ServiceContext setTarget(Object target) throws ContextException {
		putValue(targetPath, (T)target);
		return this;
	}

	public String getTargetPath() {
		return targetPath;
	}

	public ServiceContext setTargetPath(String targetPath) {
		this.targetPath = targetPath;
		return this;
	}

	public ReturnPath getReturnPath() {
		return returnPath;
	}

	public ServiceContext setReturnPath() throws ContextException {
		this.returnPath = new ReturnPath();
		return this;
	}
	
	public ServiceContext setReturnPath(String path) throws ContextException {
		this.returnPath = new ReturnPath(path);
		return this;
	}

	public ServiceContext setReturnPath(ReturnPath returnPath) {
		this.returnPath = returnPath;
		return this;
	}

	public ServiceContext setExecPath(ExecPath execPath)
			throws ContextException {
		this.execPath = execPath;
		return this;
	}
	
	public ExecPath getExecPath() {
		return execPath;
	}

	public void setReturnValue(Object value) throws ContextException {
		if (returnPath == null)
			returnPath = new ReturnPath(Context.RETURN);

		if (value == null)
			putValue(returnPath.path, (T)none);
		else
			putValue(returnPath.path, value);

		if (returnPath.direction == Direction.IN)
			Contexts.markIn(this, returnPath.path);
		else if (returnPath.direction == Direction.OUT)
			Contexts.markOut(this, returnPath.path);
		if (returnPath.direction == Direction.INOUT)
			Contexts.markInout(this, returnPath.path);
	}

	public ReturnPath getReturnJobPath() {
		return returnJobPath;
	}

	public ServiceContext setReturnJobPath() throws ContextException {
		this.returnJobPath = new ReturnPath();
		return this;
	}
	
	public ServiceContext setReturnJobPath(String path) throws ContextException {
		this.returnJobPath = new ReturnPath(path);
		return this;
	}

	public ServiceContext setReturnJobPath(ReturnPath returnPath)
			throws ContextException {
		this.returnJobPath = returnPath;
		return this;
	}

	public T setReturnJobValue(T value) throws ContextException {
		if (returnJobPath == null)
			returnJobPath = new ReturnPath(Context.RETURN);

		if (value == null)
			putValue(returnJobPath.path, (T)none);
		else
			putValue(returnJobPath.path, value);

		if (returnJobPath.direction == Direction.IN)
			Contexts.markIn(this, returnJobPath.path);
		else if (returnJobPath.direction == Direction.OUT)
			Contexts.markOut(this, returnJobPath.path);
		if (returnJobPath.direction == Direction.INOUT)
			Contexts.markInout(this, returnJobPath.path);

		return value;
	}
	
	public T putInoutValue(String path, T value)
			throws ContextException {
		putValue(path, value);
		Contexts.markInout(this, path);
		return value;
	}

	public T putInValue(String path, T value, String association)
			throws ContextException {
		putValue(path, value);
		Contexts.markIn(this, path);
		mark(path, association);
		return value;
	}

	public T putOutValue(String path, T value, String association)
			throws ContextException {
		putValue(path, value);
		Contexts.markOut(this, path);
		mark(path, association);
		return value;
	}
	
	public T putInoutValue(String path, T value, String association)
			throws ContextException {
		putValue(path, value);
		Contexts.markInout(this, path);
		mark(path, association);
		return value;
	}

	public Context setIn(String path) throws ContextException {
		return Contexts.markIn(this, path);
	}

	public Context setOut(String path) throws ContextException {
		return Contexts.markOut(this, path);
	}

	public Context setInout(String path) throws ContextException {
		return Contexts.markInout(this, path);
	}

	public void isExportControlled(boolean b) {
		if (b)
			exportControl = "1";
		else
			exportControl = "0";
	}

	public void removePathWithoutDeleted(String path) {
		this.remove(path);
		// Remove the path if it exists in metaAttribute also.
		for (Enumeration e = metacontext.elements(); e.hasMoreElements();) {
			Hashtable attributeHash = (Hashtable) e.nextElement();
			if (attributeHash.containsKey(path))
				attributeHash.remove(path);
		}
	}

	public String getTitle() {
		return name + ", " + (domainName == null ? "" : domainName + ", ")
				+ (subdomainName == null ? "" : subdomainName);
	}

	public boolean isLinked() {
		Set entries = entrySet(); 
		Iterator i = entries.iterator();
		while (i.hasNext()) {
			Map.Entry e = (Map.Entry)i.next();
			if (e.getValue() instanceof ContextLink)
				return true;
		}
		return false;
	}

	public boolean isLinkedContext(Object path) {
		Object result;
		// System.out.println("getValue: path = \""+path+"\"");
		result = get(path);
		if (result instanceof ContextLink) {
			return true;
		} else
			return false;
	}

	public boolean isLinkedPath(String path) throws ContextException {
		if (!(getValue(path) instanceof ContextLink))
			return false;
		Object result[] = getContextMapping(path);
		if (result[0] == null)
			return false;

		return true;
	}

	protected Context getLinkedContext(ContextLink link)
			throws ContextException {
		return updateLinkedContext(link);
	}

	protected Context updateLinkedContext(ContextLink link)
			throws ContextException {
		// return the linked context, converting it to a ServiceContextImpl if
		// necessary
		Context sc = link.getContext(principal);
		if (sc.getClass() != this.getClass()) {
			// logger.fine("converting linked context to " + this.getClass());
			sc = new ServiceContext(sc);
			link.setContext(sc);
		}
		return sc;
	}

	public String getPath(Object obj) throws ContextException {
		Enumeration e = contextPaths();
		String key;
		Object tmp = null;
		while (e.hasMoreElements()) {
			key = (String) e.nextElement();
			try {
				tmp = getValue(key);
			} catch (Exception ex) {
				throw new ContextException(ex);
			}
			if (tmp == obj)
				return key;
		}
		return null;
	}

	public Object putLink(String path, String id, float version, String offset)
			throws ContextException {
		// insert a ContextLink (a.k.a. a symbolic link) to cntxt
		// this makes this.getValue(key) == cntxt.getValue(offset)

		// retrieve context from data store
		Context cntxt = null;
		// cntxt = cntxtAccessor.getContext(id, version, principal);
		// temporary
		cntxt = new ServiceContext(cntxtAccessor.getContext(id, principal));
		return putLink(SERVICE_CONTEXT, path, cntxt, offset);
	}

	public Enumeration localSimpleAttributes() {
		Enumeration e = getDataAttributeMap().keys();
		Vector attributes = new Vector();
		String key;
		if (e != null) {
			while (e.hasMoreElements()) {
				key = (String) e.nextElement();
				if (isLocalSingletonAttribute(key)) {
					attributes.addElement(key);
				}
			}
			return attributes.elements();
		} else
			return null;
	}

	public Enumeration simpleAttributes() throws ContextException {
		Enumeration e = links();
		Enumeration e0, e1;
		ContextLink link;
		ServiceContext linkedCntxt;
		Vector attrs = new Vector();
		String attr;

		// get local singleton attributes
		e0 = localSimpleAttributes();
		while (e0.hasMoreElements())
			attrs.addElement(e0.nextElement());

		while (e.hasMoreElements()) {
			link = (ContextLink) e.nextElement();
			linkedCntxt = (ServiceContext) link.getContext(principal);
			e1 = linkedCntxt.getDataAttributeMap().keys();
			while (e1.hasMoreElements()) {
				attr = (String) e1.nextElement();
				if (!linkedCntxt.isLocalSingletonAttribute(attr))
					continue;
				if (!attrs.contains(attr)) // this probably doesn't work as
					// I
					// would like
					attrs.addElement(attr);
			}
		}
		return attrs.elements();
	}

	public Enumeration<String> localCompositeAttributes() {
		Enumeration<String> e = getDataAttributeMap().keys();
		Vector<String> attributes = new Vector<String>();
		String key;
		if (e != null) {
			while (e.hasMoreElements()) {
				key = e.nextElement();
				if (isLocalMetaattribute(key)) {
					attributes.addElement(key);
				}
			}
			return attributes.elements();
		} else
			return null;
	}

	public Enumeration compositeAttributes() throws ContextException {
		Enumeration e = links();
		Enumeration e0, e1;
		ContextLink link;
		ServiceContext linkedCntxt;
		Vector attrs = new Vector();
		String attr;

		// get local meta attributes
		e0 = localCompositeAttributes();
		while (e0.hasMoreElements())
			attrs.addElement(e0.nextElement());

		while (e.hasMoreElements()) {
			link = (ContextLink) e.nextElement();
			linkedCntxt = (ServiceContext) link.getContext(principal);
			e1 = linkedCntxt.getDataAttributeMap().keys();
			while (e1.hasMoreElements()) {
				attr = (String) e1.nextElement();
				if (!linkedCntxt.isLocalMetaattribute(attr))
					continue;
				if (!attrs.contains(attr))
					// this probably doesn't work as
					// I would like
					attrs.addElement(attr);
			}
		}
		return attrs.elements();
	}

	public Enumeration getAttributes() throws ContextException {
		Enumeration es = simpleAttributes();
		Enumeration em = compositeAttributes();
		Vector attrs = new Vector();
		while (es.hasMoreElements())
			attrs.addElement(es.nextElement());
		while (em.hasMoreElements())
			attrs.addElement(em.nextElement());
		return attrs.elements();
	}

	public Enumeration getAttributes(String path) throws ContextException {
		String attr;
		Vector values = new Vector();
		Enumeration e = getAttributes();
		while (e.hasMoreElements()) {
			attr = (String) e.nextElement();
			if (getAttributeValue(path, attr) != null)
				values.addElement(attr);
		}
		return values.elements();
	}

	public String getNodeType(Object obj) throws ContextException {
		// deprecated. If this object appears in the context more
		// than once, there is no guarantee that the correct context
		// type will be returned. Best not to have an orphaned
		// object.
		String path = getPath(obj);
		if (path == null)
			return null;
		return getAttributeValue(path, DATA_NODE_TYPE);
	}

	public Enumeration metaassociations(String path) throws ContextException {
		Object val;
		String attributeName;
		Vector values = new Vector();

		// locate the context and context path for this key
		Object[] map = getContextMapping(path);
		Context cntxt = (Context) map[0];
		String mappedKey = (String) map[1];

		Enumeration e = localCompositeAttributes();
		if (e != null) {
			while (e.hasMoreElements()) {
				attributeName = (String) e.nextElement();
				val = cntxt.getMetaattributeValue(mappedKey, attributeName);
				if (val != null)
					values.addElement(attributeName + APS + val);
			}
			return values.elements();
		} else
			return null;
	}

	public boolean containsPath(String path) {
		return containsKey(path);
	}

	public boolean containsAssociation(String association)
			throws ContextException {
		return (getPathsWithAssociation(association).length > 0);
	}

	public String[] getPathsWithAssociation(String association)
			throws ContextException {
		return Contexts.getMarkedPaths(this, association);
	}

	/** {@inheritDoc} */
	public String getSubjectPath() {
		return subjectPath;
	}

	/** {@inheritDoc} */
	public Object getSubjectValue() {
		return subjectValue;
	}

	/** {@inheritDoc} */
	public void setSubject(String path, Object value) {
		subjectPath = path;
		subjectValue = value;
	}

	public void updateValue(Object value) throws ContextException {
		Object initValue = null;
		T newVal = null;
		Object id = null;
		if (value instanceof Tuple2) {
			initValue = ((Tuple2) value).key();
			newVal = ((Tuple2<?, T>) value).value();
			updateValue(initValue, newVal, id);
		} else if (value instanceof Identifiable) {
			id = ((Identifiable) value).getId();
			updateValue(initValue, newVal, id);
		} else if (value instanceof Tuple2[]) {
			for (int i = 0; i < ((Tuple2[]) value).length; i++) {
				updateValue(((Tuple2[]) value)[i]);
			}
		} else if (value instanceof Identifiable[]) {
			for (int i = 0; i < ((Identifiable[]) value).length; i++) {
				updateValue(((Identifiable[]) value)[i]);
			}
		}
	}

	/**
	 * @param initValue
	 * @param newVal
	 * @param id
	 * @throws ContextException
	 */
	private void updateValue(Object initValue, T newVal, Object id)
			throws ContextException {
		Enumeration en = keys();
		while (en.hasMoreElements()) {
			String key = (String) en.nextElement();
			T val = (T)get(key);
			if (id == null) {
				// logger.info("initValue= "+initVal+" val = "+val);
				if (initValue.equals(val)) {
					if (initValue.getClass() != val.getClass())
						throw new ContextException(
								"The type of initial and new value does not mach: "
										+ initValue.getClass() + ":"
										+ val.getClass());
					logger.info("init val = " + initValue + " swapping from "
							+ val + " to " + newVal + " at key = " + key);
					put(key, newVal);
				}
			} else {
				if (val instanceof Identifiable
						&& id.equals(((Identifiable) val).getId()))
					logger.info("id = " + id + " value changed to " + newVal);
					put(key, newVal);
			}
		}
	}

	public void reportException(Throwable t) {
		exertion.getControlContext().addException(t);
	}

	public void reportException(String message, Throwable t) {
		exertion.getControlContext().addException(message, t);
	}

	public void reportException(String message, Throwable t, ProviderInfo info) {
		exertion.getControlContext().addException(new ServiceException(message, t, info));
	}

	public void reportException(String message, Throwable t, ServiceProvider provider) {
		exertion.getControlContext().addException(new ServiceException(message, t,
				new ProviderInfo(provider.getDelegate().getServiceInfo())));
	}

	public void reportException(String message, Throwable t, ServiceProvider provider,  ProviderInfo info) {
		exertion.getControlContext().addException(new ServiceException(message, t,
				new ProviderInfo(provider.getDelegate().getServiceInfo()).append(info)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Context#appendTrace(java.lang.String)
	 */
	@Override
	public void appendTrace(String footprint) {
		exertion.getControlContext().appendTrace(footprint);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Context#getProvider()
	 */
	@Override
	public Provider getProvider() {
		if (exertion != null)
			return (Provider) ((NetSignature) exertion.getProcessSignature())
					.getService();
		else
			return null;
	}

	public ServiceContext substitute(Arg... entries) throws SetterException, RemoteException {
		if (entries == null)
			return this;
		ReturnPath rPath = null;
		for (Arg a : entries) {
			if (a instanceof ReturnPath) {
				rPath = (ReturnPath) a;
				break;
			}
		}
		if (rPath != null) setReturnPath(rPath);

		try {
			for (Arg e : entries) {
				if (e instanceof Tuple2) {
					T val = null;
					
					if (((Tuple2) e)._2 instanceof Evaluation)
						val = (T)((Evaluation) ((Tuple2) e)._2).getValue();
					else
						val = (T)((Tuple2) e)._2;
			
					if (((Tuple2) e)._1 instanceof String) {
						if (asis((String) ((Tuple2) e)._1) instanceof Setter) {
							((Setter) asis((String) ((Tuple2) e)._1))
									.setValue(val);
						} else {
							putValue((String) ((Tuple2) e)._1, val);
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new SetterException(ex);
		}
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Context#getMarkedValues(java.lang.String)
	 */
	@Override
	public List<T> getMarkedValues(String association) throws ContextException {
		Enumeration e = markedPaths(association);
		List<T> values = new ArrayList<T>();
		String path = null;
		while (e.hasMoreElements()) {
			path = (String) e.nextElement();
			try {
				values.add(getValue(path));
			} catch (Exception ex) {
				throw new ContextException(ex);
			}
		}
		return values;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Context#setMetacontext(java.util.Hashtable)
	 */
	@Override
	public void setMetacontext(Hashtable<String, Map<String, String>> mc) {
		metacontext = mc;
	}

	public int hashCode() {
		return contextId.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Context#get(java.lang.String)
	 */
	@Override
	public T get(String path) {
		return super.get(path);
	}

	/**
	 * Record this context as updated if the related exertion is monitored.
	 * 
	 * @throws RemoteException
	 * @throws MonitorException
	 */
	public void checkpoint() throws ContextException {
		ServiceExertion mxrt = (ServiceExertion) getExertion();
		if (mxrt != null && mxrt.isMonitorable()
				&& mxrt.getMonitorSession() != null) {
			try {
				putValue("context/checkpoint/time", SorcerUtil.getDateTime());
				mxrt.getMonitorSession().changed(this, State.UPDATED);
			} catch (Exception e) {
				throw new ContextException(e);
			}
		}
	}

	/**
	 * Record this context acording to the corresponding aspect if the related
	 * exertion is monitored.
	 * 
	 * @throws RemoteException
	 * @throws MonitorException
	 */
	public void changed(State aspect) throws RemoteException,
			MonitorException {
		ServiceExertion mxrt = (ServiceExertion) getExertion();
		if (mxrt != null && mxrt.isMonitorable()
				&& mxrt.getMonitorSession() != null) {
			mxrt.getMonitorSession().changed(this, aspect);
		}
	}

	public T asis(String path) throws ContextException {
		T val;
		synchronized (this) {
			if (isModeling == true) {
				isModeling = false;
				val = get(path);
				isModeling = true;
			} else {
				val = get(path);
			}
		}
		return val;
	}

	public Context setOutValues(Context<T> context) throws ContextException,
			RemoteException {
		List<String> pl = ((ServiceContext) context).getOutPaths();
		for (String p : pl) {
			putValue(p, context.getValue(p));
		}
		return this;
	}

	public Par getPar(String path) throws ContextException, RemoteException {
		return new Par(path, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Evaluation#getValue(sorcer.core.context.Path.Entry[])
	 */
	@Override
	public T getValue(Arg... entries) throws EvaluationException, RemoteException {
		try {
			return getValue(null, entries);
		} catch (ContextException e) {
			throw new EvaluationException(e);
		}
	}

	public T getValue(String path, Arg... entries)
			throws ContextException {
		// first managed dependencies
		String currentPath = path;
		if (dependers != null && dependers.size() > 0) {
			for (Evaluation eval : dependers)  {
				try {
					eval.getValue(entries);
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
			}
		}
		T obj = null;
		try {
			substitute(entries);
			if (currentPath == null) {
				if (targetPath != null)
					currentPath = targetPath;
				else if (returnPath != null)
					return getReturnValue(entries);
				else
					return (T)this;
			}
			if (currentPath.startsWith("super")) {
				obj = (T) exertion.getContext().getValue(currentPath.substring(6));
			} else {
				obj = (T) getValue0(currentPath);
				if (obj instanceof Evaluation && isModeling) {
					if (obj instanceof Scopable) {
						Object scope = ((Scopable)obj).getScope();
						if (scope == null) {
							((Scopable)obj).setScope(this);
						} else {
							((Context)((Scopable)obj).getScope()).append(this);
						}
					}
					obj = ((Evaluation<T>)obj).getValue(entries);
				} else if ((obj instanceof Paradigmatic)
						&& ((Paradigmatic) obj).isModeling()) {
					obj = ((Evaluation<T>)obj).getValue(entries);
				}
			}
			if (obj instanceof Reactive && ((Reactive)obj).isReactive())
				return (T) ((Evaluation)obj).getValue(entries);
			else
				return (T) obj;
		} catch (Throwable e) {
			logger.warning(e.getMessage());
			e.printStackTrace();
			return (T) Context.none;
//			throw new EvaluationException(e);
		}
	}
	
	public String getCurrentSelector() {
		return currentSelector;
	}

	public void setCurrentSelector(String currentSelector) {
		this.currentSelector = currentSelector;
	}

	public String getCurrentPrefix() {
		return currentPrefix;
	}

	public void setCurrentPrefix(String currentPrefix) {
		this.currentPrefix = currentPrefix;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Evaluation#getAsIs()
	 */
	@Override
	public T asis() throws EvaluationException, RemoteException {
		return getValue();
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Context#getData()
	 */
	@Override
	public Object getData() {
		// to reimplemented in subclasses
		return null;
	}
  public String getPrefix() {
        if (prefix != null && prefix.length() > 0)
            return prefix + CPS;
        else
            return "";
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

	/* (non-Javadoc)
	 * @see sorcer.service.Context#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Context#link(sorcer.service.Context, java.lang.String, java.lang.String)
	 */
	@Override
	public Object link(Context context, String atPath, String offset)
			throws ContextException {
		return putLink(atPath, context, offset);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Context#addValue(sorcer.service.Identifiable)
	 */
	@Override
	public Object addValue(Identifiable value) throws ContextException {
		if (value instanceof Entry && !((Entry)value).isPersistent()) {
			return putValue(value.getName(), ((Entry)value).value());
		}
		return putValue(value.getName(), value);		
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Context#putDbValue(java.lang.String, java.lang.Object)
	 */
	@Override
	public Object putDbValue(String path, Object value) throws ContextException, RemoteException {
		Par par = new Par(path, value == null ? Context.none : value);
		par.setPersistent(true);
		return putValue(path, par);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Context#putDbValue(java.lang.String, java.lang.Object, java.net.URL)
	 */
	@Override
	public Object putDbValue(String path, Object value, URL datastoreUrl)
			throws ContextException, RemoteException {
		Par par = new Par(path, value == null ? Context.none : value);
		par.setPersistent(true);
		par.setDbURL(datastoreUrl);
		return putValue(path, par);
	}
	
	public String getDbUrl() {
		return dbUrl;
	}

	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}

	public List<EntryList> getEntryLists() {
		return entryLists;
	}

	public void setEntryLists(List<EntryList> entryLists) {
		this.entryLists = entryLists;
	}
	
	public EntryList getEntryList(EntryList.Type type) {
		if (entryLists != null) {
			for (EntryList el : entryLists) {
				if (el.getType().equals(type))
					return el;
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Contexter#getContext(sorcer.service.Context)
	 */
	@Override
	public Context<T> getContext(Context<T> contextTemplate)
			throws RemoteException, ContextException {
		Object val = null;
		for (String path : contextTemplate.getPaths()) {
			val = asis(path);
			if (val != null && val != Context.none)
				contextTemplate.putValue(path, asis(path));
		}
		return contextTemplate;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Context#addPar(sorcer.core.context.model.par.Par)
	 */
	@Override
	public Arg addPar(Arg par) throws ContextException {
		Par p = (Par)par;
		put(p.getName(), (T)p);
		if (p.getScope() == null || p.getScope().size() == 0)
			p.setScope(this);
		try {
			if (p.asis() instanceof ServiceInvoker) {
				((ServiceInvoker) p.asis()).setScope(this);
			}
		} catch (RemoteException e) {
			throw new ContextException(e);
		} 
		contextChanged = true;
		return p;
	}
	
	public Par appendPar(Par p) throws ContextException {
		put(p.getName(), (T)p);
		if (p.getScope() == null)
			p.setScope(new ParModel(p.getName()).append(this));
		try {
			if (p.asis() instanceof ServiceInvoker) {
				((ServiceInvoker) p.asis()).setScope(this);
			}
		} catch (RemoteException e) {
			throw new ContextException(e);
		} 
		contextChanged = true;
		return p;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Context#addPar(java.lang.String, java.lang.Object)
	 */
	@Override
	public Par addPar(String path, Object value) throws ContextException {
		Par par;
		try {
			par = new Par(path, value, this);
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
		return par;
	}
	
	/**
	 * <p>
	 * Returns <code>true</code> if this context is for modeling, otherwise
	 * <code>false</code>. If context is for modeling then the values of this
	 * context that implement the {@link Evaluation} interface are evaluated for
	 * its requested evaluated values.
	 * </p>
	 * 
	 * @return the <code>true</code> if this context is revaluable.
	 */
	public boolean isModeling() {
		return isModeling;
	}

	/**
	 * <p>
	 * Assign revaluability of this context.
	 * </p>
	 * 
	 * @param isRevaluable
	 */
	public void setModeling(boolean isRevaluable) {
		this.isModeling = isRevaluable;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Invocation#invoke(sorcer.service.Context, sorcer.service.Arg[])
	 */
	@Override
	public T invoke(Context<T> context, Arg... entries) throws RemoteException,
			InvocationException {
		try {
			appendContext(context);
			return getValue(entries);
		} catch (Exception e) {
			throw new InvocationException(e);
		}
	}

	public Context getBlockScope() {
		return blockScope;
	}

	public void setBlockScope(Context blockScope) {
		this.blockScope = blockScope;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Service#service(sorcer.service.Exertion, net.jini.core.transaction.Transaction)
	 */
	@Override
	public Exertion service(Exertion exertion, Transaction txn)
			throws TransactionException, ExertionException, RemoteException {
		try {
			((ServiceExertion)exertion).getContext().appendContext(this);
		} catch (Exception e) {
			throw new ExertionException(e);
		}
		return exertion.exert(txn);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Service#service(sorcer.service.Exertion)
	 */
	@Override
	public Exertion service(Exertion exertion) throws TransactionException,
			ExertionException, RemoteException {
		return service(exertion, null);
	}

	@Override
	public String[] getMarkedPaths(String association)
			throws ContextException {
		return Contexts.getMarkedPaths(this, association);
	}

	@Override
	public Evaluation addDepender(Evaluation depender) {
		if (this.dependers == null)
			this.dependers = new ArrayList<Evaluation>();
		dependers.add(depender);
		return this;
	}

	public Evaluation addDependers(Evaluation... dependers) {
		if (this.dependers == null)
			this.dependers = new ArrayList<Evaluation>();
		for (Evaluation depender : dependers)
			this.dependers.add(depender);
		return this;
	}

	@Override
	public List<Evaluation> getDependers() {
		return dependers;
	}

	public boolean isFinalized() {
		return isFinalized;
	}

	public void setFinalized(boolean isFinalized) {
		this.isFinalized = isFinalized;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Context))
			return false;

		if (keySet().size() != ((ServiceContext)object).keySet().size())
			return false;

		for (String  path : keySet()) {
			try {
				if (!asis(path).equals(((ServiceContext) object).asis(path)))
                    return false;
			} catch (ContextException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
}
