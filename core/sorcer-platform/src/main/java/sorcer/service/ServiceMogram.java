package sorcer.service;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.co.tuple.ExecPath;
import sorcer.core.SorcerConstants;
import sorcer.core.context.ContextSelector;
import sorcer.core.monitor.MonitoringSession;
import sorcer.core.plexus.FidelityManager;
import sorcer.core.signature.ServiceSignature;
import sorcer.security.util.SorcerPrincipal;

import javax.security.auth.Subject;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.*;

/**
 * Created by sobolemw on 5/4/15.
 */
public abstract class ServiceMogram implements Mogram, Exec, Serializable, SorcerConstants {

    protected final static Logger logger = LoggerFactory.getLogger(ServiceMogram.class.getName());

    static final long serialVersionUID = 1L;

    protected String name;

    protected Uuid mogramId;

    protected Uuid parentId;

    protected Mogram parent;

    protected String parentPath = "";

    protected ExecPath execPath;

    protected Uuid sessionId;

    protected String subjectId;

    protected Subject subject;

    protected String ownerId;

    protected String runtimeId;

    protected Long lsbId;

    protected Long msbId;

    protected String domainId;

    protected String subdomainId;

    protected String domainName;

    protected String subdomainName;

    protected FidelityManagement fiManager;

    protected FidelityList fiConfig;

    protected MogramStrategy mogramStrategy;

    protected ContextSelector contextSelector;
    /**
     * position of component Mogram in a compund mogram
     */
    protected Integer index = new Integer(-1);

    /**
     * execution status: INITIAL|DONE|RUNNING|SUSPENDED|HALTED
     */
    protected Integer status = Exec.INITIAL;

    protected Integer priority;

    // the mogram's scope
    protected Context scope;

    protected String description;

    protected String projectName;

    protected boolean isRevaluable = false;

    protected boolean isChanged = false;

    // true if the exertion has to be initialized (to original state)
    // or used as is after resuming from suspension or failure
    protected boolean isInitializable = true;

    protected String dbUrl;

    // service metafidelities for this mogram
    protected Map<String, ServiceFidelity> serviceMetafidelities;

    protected ServiceFidelity<ServiceFidelity> serviceMetafidelity;

    // service fidelities for this mogram
    protected Map<String, ServiceFidelity> serviceFidelities;

    protected ServiceFidelity<Signature> serviceFidelity = new ServiceFidelity(ServiceFidelity.Type.SIG);

    protected SorcerPrincipal principal;

    // the current fidelity alias, as it is named in 'fidelities'
    // its original name might be different if aliasing is used
    // for already existing names
    protected String serviceFidelitySelector;

//    // fidelity Contexts for its component exertions
//    protected Map<String, FidelityContext> fidelityContexts;

    // Date of creation of this Exertion
    protected Date creationDate = new Date();

    protected Date lastUpdateDate;

    protected Date goodUntilDate;

    protected String accessClass;

    protected Boolean isExportControlled;

    protected static String defaultName = "mogram-";

    public static boolean debug = false;

    // sequence number for unnamed mogram instances
    protected static int count = 0;

    protected MonitoringSession monitorSession;

    protected Signature builder;

    protected ServiceMogram() {
        this(null);
    }

    public ServiceMogram(String name) {
        if (name == null || name.length() == 0)
            this.name = defaultName + count++;
        else
            this.name = name;
        init();
    }

    public ServiceMogram(String name, Signature builder) {
        this(name);
        this.builder = builder;
    }

    protected void init() {
        mogramId = UuidFactory.generate();
        domainId = "0";
        subdomainId = "0";
        accessClass = PUBLIC;
        isExportControlled = Boolean.FALSE;
        status = new Integer(INITIAL);
        principal = new SorcerPrincipal(System.getProperty("user.name"));
        principal.setId(principal.getName());
        setSubject(principal);
    }

    /**
     * Returns the index assigned by the container.
     */
    @Override
    public int getIndex() {
        return (index == null) ? -1 : index;
    }

    @Override
    public void setIndex(int i) {
        index = i;
    }

    public Uuid getMogramId() {
        return mogramId;
    }

    @Override
    public void setParentId(Uuid parentId) {
        this.parentId = parentId;
    }

    public Uuid getParentId() {
        return parentId;
    }

    public List<Mogram> getAllMograms() {
        List<Mogram> exs = new ArrayList<Mogram>();
        getMograms(exs);
        return exs;
    }

    public List<Mogram> getMograms(List<Mogram> exs) {
        exs.add(this);
        return exs;
    }

    public List<String> getAllMogramIds() {
        List<String> mogIdsList = new ArrayList<String>();
        for (Mogram mo : getAllMograms()) {
            mogIdsList.add(mo.getId().toString());
        }
        return mogIdsList;
    }

    public void trimAllNotSerializableSignatures() {
        trimNotSerializableSignatures();
        for (Mogram m : getAllMograms()) {
            ((ServiceMogram)m).trimNotSerializableSignatures();
        }
    }

    public Mogram getMogram(String componentMogramName) {
        if (name.equals(componentMogramName)) {
            return this;
        } else {
            List<Mogram> mograms = getAllMograms();
            for (Mogram m : mograms) {
                if (m.getName().equals(componentMogramName)) {
                    return m;
                }
            }
            return null;
        }
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int value) {
        status = value;
    }

    @Override
    public Uuid getId() {
        return mogramId;
    }

    public void setId(Uuid id) {
        mogramId = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRuntimeId() {
        return runtimeId;
    }

    public void setRuntimeId(String id) {
        runtimeId = id;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setSubdomainId(String subdomaindId) {
        this.subdomainId = subdomaindId;
    }

    public String getSubdomainId() {
        return subdomainId;
    }

    public Uuid getSessionId() {
        return sessionId;
    }

    public void setSessionId(Uuid sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public Context getScope() {
        return scope;
    }

    @Override
    public void setScope(Context scope) {
        this.scope = scope;
    }

    public Mogram getParent() {
        return parent;
    }

    public void setParent(Mogram parent) {
        this.parent = parent;
    }

    public SorcerPrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(SorcerPrincipal principal) {
        this.principal = principal;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    private void setSubject(Principal principal) {
        if (principal == null)
            return;
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(principal);
        subject = new Subject(true, principals, new HashSet(), new HashSet());
    }

    public SorcerPrincipal getSorcerPrincipal() {
        if (subject == null)
            return null;
        Set<Principal> principals = subject.getPrincipals();
        Iterator<Principal> iterator = principals.iterator();
        while (iterator.hasNext()) {
            Principal p = iterator.next();
            if (p instanceof SorcerPrincipal)
                return (SorcerPrincipal) p;
        }
        return null;
    }

    public String getPrincipalId() {
        SorcerPrincipal p = getSorcerPrincipal();
        if (p != null)
            return getSorcerPrincipal().getId();
        else
            return null;
    }

    public void setPrincipalId(String id) {
        SorcerPrincipal p = getSorcerPrincipal();
        if (p != null)
            p.setId(id);
    }

    public long getMsbId() {
        return (msbId == null) ? -1 : msbId.longValue();
    }

    public void setLsbId(long leastSig) {
        if (leastSig != -1) {
            lsbId = new Long(leastSig);
        }
    }

    public void setMsbId(long mostSig) {
        if (mostSig != -1) {
            msbId = new Long(mostSig);
        }
    }

    public void setPriority(int p) {
        priority = p;
    }

    public int getPriority() {
        return (priority == null) ? MIN_PRIORITY : priority;
    }

	public Signature getProcessSignature() {
		Signature sig = null;
		if (serviceFidelitySelector != null) {
			serviceFidelity = serviceFidelities.get(serviceFidelitySelector);
		}

		for (Signature s : serviceFidelity.selects) {
			if (s.getType() == Signature.Type.PROC) {
				sig = s;
				break;
			}
		}
		return sig;
	}

    public void trimNotSerializableSignatures() {
        if (serviceFidelities != null) {
            Iterator i = serviceFidelities.keySet().iterator();
            while (i.hasNext()) {
                Object obj = serviceFidelities.get(i.next());
                if (obj instanceof ServiceFidelity)
                    trimNotSerializableSignatures((ServiceFidelity) obj);
            }
        }
    }

	private void trimNotSerializableSignatures(ServiceFidelity<Signature> fidelity) {
		Iterator<Signature> i = fidelity.getSelects().iterator();
		while (i.hasNext()) {
			Class prvType = i.next().getServiceType();
			if (!prvType.isInterface()
				&& !Serializable.class.isAssignableFrom(prvType)) {
				i.remove();
				logger.warn("removed not serializable signature for: {}", prvType);
			}
		}
	}

    public List<Signature> getApdProcessSignatures() {
        List<Signature> sl = new ArrayList<Signature>();
        for (Signature s : serviceFidelity.getSelects()) {
            if (s.getType() == Signature.Type.APD_DATA)
                sl.add(s);
        }
        return sl;
    }

    public List<Signature> getPreprocessSignatures() {
        List<Signature> sl = new ArrayList<Signature>();
        for (Signature s : serviceFidelity.getSelects()) {
            if (s.getType() == Signature.Type.PRE)
                sl.add(s);
        }
        return sl;
    }

    public List<Signature> getPostprocessSignatures() {
        List<Signature> sl = new ArrayList<Signature>();
        for (Signature s : serviceFidelity.getSelects()) {
            if (s.getType() == Signature.Type.POST)
                sl.add(s);
        }
        return sl;
    }

    /**
     * Appends a signature <code>signature</code> for this exertion.
     **/
    public void addSignature(Signature signature) {
        if (signature == null)
            return;
        String id = getOwnerId();
        if (id == null)
            id = System.getProperty("user.name");
        ((ServiceSignature) signature).setOwnerId(id);
        serviceFidelity.getSelects().add(signature);
        this.serviceFidelity.select = signature;
    }

    /**
     * Removes a signature <code>signature</code> for this exertion.
     *
     * @see #addSignature
     */
    public void removeSignature(Signature signature) {
        serviceFidelity.getSelects().remove(signature);
    }

    public void setAccessClass(String s) {
        if (SENSITIVE.equals(s) || CONFIDENTIAL.equals(s) || SECRET.equals(s))
            accessClass = s;
        else
            accessClass = PUBLIC;
    }

    public String getAccessClass() {
        return (accessClass == null) ? PUBLIC : accessClass;
    }

    public void isExportControlled(boolean b) {
        isExportControlled = new Boolean(b);
    }

    public boolean isExportControlled() {
        return isExportControlled.booleanValue();
    }

    public Date getGoodUntilDate() {
        return goodUntilDate;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public void setGoodUntilDate(Date date) {
        goodUntilDate = date;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String id) {
        ownerId = id;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getSubdomainName() {
        return subdomainName;
    }

    public void setSubdomainName(String subdomainName) {
        this.subdomainName = subdomainName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String path) {
        parentPath = path;
    }

    public boolean isInitializable() {
        return isInitializable;
    }

    public void setIsInitializable(boolean isInitializable) {
        this.isInitializable = isInitializable;
    }

    public Mogram setExecPath(ExecPath execPath)
            throws ContextException {
        this.execPath = execPath;
        return this;
    }

    public ExecPath getExecPath() {
        return execPath;
    }

    public boolean isChanged() {
        return isChanged;
    }

    public void setIsChanged(boolean isChanged) {
        this.isChanged = isChanged;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    @Override
    public ServiceFidelity<Signature> getFidelity() {
        return serviceFidelity;
    }

    public void setFidelity(ServiceFidelity fidelity) {
        serviceFidelity =  fidelity;
    }

    public ContextSelector getContextSelector() {
        return contextSelector;
    }

    public void setContextSelector(ContextSelector contextSelector) {
        this.contextSelector = contextSelector;
    }

    public Mogram getComponentMogram(String path) {
        return this;
    }
    abstract public Mogram clearScope() throws MogramException;

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
        return isRevaluable;
    }

    public void setModeling(boolean isRevaluable) {
        this.isRevaluable = isRevaluable;
    }

    public String toString() {
        StringBuffer info = new StringBuffer()
                .append(this.getClass().getName()).append(": " + name);
        info.append("\n  status=").append(status);
        info.append(", mogram ID=").append(mogramId);
        return info.toString();
    }

    /**
     * <p>
     * Returns the monitor session of this exertion.
     * </p>
     *
     * @return the monitorSession
     */
    public MonitoringSession getMonitorSession() {
        return monitorSession;
    }

    /**
     * <p>
     * Assigns a monitor session for this exertions.
     * </p>
     *
     * @param monitorSession
     *            the monitorSession to set
     */
    public void setMonitorSession(MonitoringSession monitorSession) {
        this.monitorSession = monitorSession;
    }

    @Override
    public Map<String, ServiceFidelity> getFidelities() {
        return serviceFidelities;
    }

    public void setFidelities(Map<String, ServiceFidelity> fidelities) {
        this.serviceFidelities = fidelities;
    }

    public String getSelectedFidelitySelector() {
        return serviceFidelitySelector;
    }

    public void setSelectedFidelitySelector(String selectedFidelitySelector) {
        this.serviceFidelitySelector = selectedFidelitySelector;
    }

    public Map<String, ServiceFidelity> getServiceFidelities() {
        return serviceFidelities;
    }

    public void setServiceFidelities(Map<String, ServiceFidelity> serviceFidelities) {
        this.serviceFidelities = serviceFidelities;
    }

    public Map<String, ServiceFidelity> getServiceMetafidelities() {
        return serviceMetafidelities;
    }

    public void setServiceMetafidelities(Map<String, ServiceFidelity> serviceMetafidelities) {
        this.serviceMetafidelities = serviceMetafidelities;
    }

    @Override
    public Signature getBuilder(Arg... args) throws ContextException {
        return builder;
    }

    public void setBuilder(Signature builder) {
        this.builder = builder;
    }

    public void setFidelity(String name, ServiceFidelity fidelity) {
        this.serviceFidelity = new ServiceFidelity(name, fidelity);
        putFidelity(name, serviceFidelity);
        serviceFidelitySelector = name;
    }

    public void putFidelity(ServiceFidelity fidelity) {
        if (serviceFidelities == null)
            serviceFidelities = new HashMap<String, ServiceFidelity>();
        serviceFidelities.put(fidelity.getName(), fidelity);
    }

    public void putFidelity(String name, ServiceFidelity fidelity) {
        if (serviceFidelities == null)
            serviceFidelities = new HashMap<String, ServiceFidelity>();
        serviceFidelities.put(name, new ServiceFidelity(fidelity));
    }

    public void putMetafidelity(ServiceFidelity fidelity) {
        if (serviceFidelities == null)
            serviceFidelities = new HashMap<String, ServiceFidelity>();
        serviceFidelities.put(fidelity.getName(), new ServiceFidelity(fidelity));
    }

    public void putMetafidelity(String name, ServiceFidelity fidelity) {
        if (serviceMetafidelities == null)
            serviceMetafidelities = new HashMap<String, ServiceFidelity>();
        serviceMetafidelities.put(name, new ServiceFidelity(fidelity));
    }

    public void addFidelity(String name, ServiceFidelity fidelity) {
        ServiceFidelity nf = new ServiceFidelity(name, fidelity);
        putFidelity(name, nf);
    }

    public FidelityManagement getFidelityManager() {
        return fiManager;
    }

    public void setFidelityManager(FidelityManager fiManager) {
        this.fiManager = fiManager;
    }

    public FidelityList getFiConfig() {
        return fiConfig;
    }

    public void setFiConfig(FidelityList fiConfig) {
        this.fiConfig = fiConfig;
    }

    public ServiceFidelity selectFidelity(Arg... entries)  {
        ServiceFidelity fi = null;
        if (entries != null && entries.length > 0) {
            for (Arg a : entries)
                if (a instanceof ServiceFidelity && ((ServiceFidelity)a).type == ServiceFidelity.Type.EMPTY) {
                    fi = selectFidelity(a.getName());
                } else if (a instanceof ServiceFidelity && ((ServiceFidelity)a).type == ServiceFidelity.Type.COMPONENT) {
                    fi = selectComponentFidelity((ServiceFidelity) a);
                } else if (a instanceof ServiceFidelity && ((ServiceFidelity)a).type == ServiceFidelity.Type.META) {
                    fi = selectCompositeFidelity((ServiceFidelity) a);
                }
        }
        return fi;
    }

    public ServiceFidelity selectFidelity(String selector) {
        ServiceFidelity sf = null;
        if (selector != null && serviceFidelities != null
                && serviceFidelities.containsKey(selector)) {
            sf = serviceFidelities.get(selector);

            if (sf == null)
                logger.warn("no such service fidelity: {} for: {}", selector, this);
        }
        if (sf != null) {
            if (sf.type == ServiceFidelity.Type.SIG) {
                serviceFidelity = sf;
                serviceFidelitySelector = selector;
            } else if (sf.type == ServiceFidelity.Type.META) {
                selectCompositeFidelity(sf);
            }
        }
        return serviceFidelity;
    }

    public ServiceFidelity selectComponentFidelity(ServiceFidelity componentFidelity) {
        Mogram ext = getComponentMogram(componentFidelity.getPath());
        String fn = ((Arg)componentFidelity.getSelects().get(0)).getName();
        ServiceFidelity cf = ext.getFidelities().get(fn);
        if (cf != null) {
            ((ServiceMogram)ext).setFidelity(cf);
			((ServiceMogram)ext).setSelectedFidelitySelector(fn);
        } else {
            logger.warn("no such fidelity for {}" + componentFidelity);
        }
        return cf;
    }

    public ServiceFidelity selectCompositeFidelity(ServiceFidelity fidelity) {
        if (fidelity.type == ServiceFidelity.Type.META) {
            for (Object obj : fidelity.selects) {
                if (obj instanceof ServiceFidelity) {
                    if (((ServiceFidelity) obj).type == ServiceFidelity.Type.COMPONENT)
                        selectComponentFidelity((ServiceFidelity) obj);
                    else
                        selectFidelity(((ServiceFidelity) obj).getName());
                }
            }
        }
        return fidelity;
    }

    public ServiceFidelity selectFidelity()  {
        if (serviceFidelitySelector != null && serviceFidelities != null
                && serviceFidelities.containsKey(serviceFidelitySelector)) {
            ServiceFidelity sf = serviceFidelities.get(serviceFidelitySelector);
            if (sf == null)
                logger.warn("no such service fidelity: {}", serviceFidelitySelector);
            serviceFidelity = sf;
        }
//        trimNotSerializableSignatures(serviceFidelity);
        return serviceFidelity;
    }

    @Override
    public void reconfigure(ServiceFidelity... fidelities) throws ContextException, RemoteException {
        if (fiManager != null)
            fiManager.reconfigure(fidelities);
    }

    @Override
    public void morph(String... metaFiNames) throws RemoteException {
        if (fiManager != null)
            fiManager.morph(metaFiNames);
    }

    public void removeSignature(int index) {
        serviceFidelity.selects.remove(index);
    }


    public void addSignatures(ServiceFidelity<Signature> fidelity) {
        if (this.serviceFidelity != null)
            this.serviceFidelity.selects.addAll(fidelity.selects);
        else {
            this.serviceFidelity = new ServiceFidelity();
            this.serviceFidelity.selects.addAll(fidelity.selects);
        }
        this.serviceFidelity.select = fidelity.selects.get(0);
    }

    @Override
    public MogramStrategy getMogramStrategy() {
        return mogramStrategy;
    }

    public void setModelStrategy(MogramStrategy strategy) {
        mogramStrategy = strategy;
    }

    public boolean isBatch() {
        return serviceFidelity.selects.size()>1;
    }

    public void correctProcessSignature(Signature signature) {
        for (Signature sig : this.serviceFidelity.selects) {
            if (sig.getType() != Signature.Type.PROC) {
                this.serviceFidelity.selects.remove(sig);
            }
        }
        this.serviceFidelity.selects.add(signature);
    }

    @Override
    public ServiceFidelity<ServiceFidelity> getMetafidelity() {
        return serviceMetafidelity;
    }

    @Override
    public void setMetafidelity(ServiceFidelity<ServiceFidelity> metafidelity) {
        serviceMetafidelity = metafidelity;
    }
}
