package sorcer.service;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.co.tuple.ExecPath;
import sorcer.core.SorcerConstants;
import sorcer.core.monitor.MonitoringSession;
import sorcer.core.signature.NetSignature;
import sorcer.core.signature.ServiceSignature;
import sorcer.security.util.SorcerPrincipal;

import javax.security.auth.Subject;
import java.io.Serializable;
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

    protected Mogram parent;

    protected Uuid parentId;

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

    protected Integer scopeCode = PRIVATE_SCOPE;

    protected String description;

    protected String projectName;

    protected boolean isRevaluable = false;

    protected boolean isChanged = false;

    // true if the exertion has to be initialized (to original state)
    // or used as is after resuming from suspension or failure
    protected boolean isInitializable = true;

    protected String dbUrl;

    // service fidelities for this exertions
    protected Map<String, Fidelity> serviceFidelities;

    protected Fidelity<Signature> serviceFidelity = new Fidelity<Signature>();

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
        scopeCode = new Integer(PRIVATE_SCOPE);
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

    @Override
    public void setParentId(Uuid parentId) {
        this.parentId = parentId;
    }

    public Uuid getParentId() {
        return parentId;
    }

    public Uuid getMogramId() {
        return mogramId;
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

    public void setScopeCode(int value) {
        scopeCode = new Integer(value);
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
        if (serviceFidelity.type == Fidelity.Type.COMPOSITE) {
            for (Object obj : serviceFidelity.selects) {
                if (obj instanceof Fidelity) {
                    if (((Fidelity) obj).type == Fidelity.Type.COMPONENT)
                        selectComponentFidelity((Fidelity) obj);
                    else
                        selectFidelity((Fidelity) obj);
                }
            }
        }

        if (serviceFidelitySelector != null) {
            serviceFidelity = serviceFidelities.get(serviceFidelitySelector);
        }
        for (Signature s : serviceFidelity.getSelects()) {
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
                if (obj instanceof Fidelity)
                    trimNotSerializableSignatures((Fidelity) obj);
            }
        }
    }

    private void trimNotSerializableSignatures(Fidelity<Signature> fidelity) {
        Signature sig = getProcessSignature();
        if (sig instanceof NetSignature) {
            Iterator i = fidelity.getSelects().iterator();
            while (i.hasNext()) {
                Object obj = i.next();
                if (obj instanceof Signature
                        && !(Serializable.class.isAssignableFrom(((Signature) obj).getServiceType())
                        || ((Signature) obj).getServiceType().isInterface())) {
                    i.remove();
                    logger.warn("removed not remote signature: {}", obj);
                }
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

    public Integer getScopeCode() {
        return scopeCode;
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
    public Fidelity<Signature> getFidelity() {
        return serviceFidelity;
    }

    @Override
    public void setFidelity(Fidelity fidelity) {
        serviceFidelity =  fidelity;
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
    public Map<String, Fidelity> getFidelities() {
        return serviceFidelities;
    }

    public void setFidelities(Map<String, Fidelity> fidelities) {
        this.serviceFidelities = fidelities;
    }

    public String getSelectedFidelitySelector() {
        return serviceFidelitySelector;
    }

    public void setSelectedFidelitySelector(String selectedFidelitySelector) {
        this.serviceFidelitySelector = selectedFidelitySelector;
    }

    public Map<String, Fidelity> getServiceFidelities() {
        return serviceFidelities;
    }

    public void setServiceFidelities(Map<String, Fidelity> serviceFidelities) {
        this.serviceFidelities = serviceFidelities;
    }

    @Override
    public Signature getBuilder(Arg... args) throws ContextException {
        return builder;
    }

    public void setBuilder(Signature builder) {
        this.builder = builder;
    }

    public void addFidelity(Fidelity fidelity) {
        putFidelity(fidelity.getName(), fidelity);
        serviceFidelitySelector = fidelity.getName();
        this.serviceFidelity = fidelity;
    }

    public void setFidelity(String name, Fidelity fidelity) {
        this.serviceFidelity = new Fidelity(name, fidelity);
        putFidelity(name, serviceFidelity);
        serviceFidelitySelector = name;
    }

    public void putFidelity(Fidelity fidelity) {
        if (serviceFidelities == null)
            serviceFidelities = new HashMap<String, Fidelity>();
        serviceFidelities.put(fidelity.getName(), fidelity);
    }

    public void putFidelity(String name, Fidelity fidelity) {
        if (serviceFidelities == null)
            serviceFidelities = new HashMap<String, Fidelity>();
        serviceFidelities.put(name, new Fidelity(fidelity));
    }

    public void addFidelity(String name, Fidelity fidelity) {
        Fidelity nf = new Fidelity(name, fidelity);
        putFidelity(name, nf);
    }

    public Fidelity selectFidelity(Arg... entries)  {
        Fidelity fi = null;
        if (entries != null && entries.length > 0) {
            for (Arg a : entries)
                if (a instanceof Fidelity && ((Fidelity)a).type == Fidelity.Type.EMPTY) {
                    fi = selectFidelity(a.getName());
                } else if (a instanceof Fidelity && ((Fidelity)a).type == Fidelity.Type.COMPONENT) {
                    fi = selectComponentFidelity((Fidelity) a);
                } else if (a instanceof Fidelity && ((Fidelity)a).type == Fidelity.Type.COMPOSITE) {
                    fi = selectCompositeFidelity((Fidelity) a);
                }
        }
        return fi;
    }

    public Fidelity selectFidelity(String selector) {
        Fidelity sf = null;
        if (selector != null && serviceFidelities != null
                && serviceFidelities.containsKey(selector)) {
            sf = serviceFidelities.get(selector);

            if (sf == null)
                logger.warn("no such service fidelity: {} for: {}", selector, this);
        }
        if (sf.type == Fidelity.Type.EXERT) {
            serviceFidelity = sf;
            serviceFidelitySelector = selector;
        } else if (sf.type == Fidelity.Type.COMPOSITE) {
            selectCompositeFidelity(sf);
        } else if (sf.type == Fidelity.Type.EMPTY) {
            selectFidelity(sf.getName());
        }
        trimAllNotSerializableSignatures();
        return serviceFidelity;
    }

    public Fidelity selectComponentFidelity(Fidelity componentFidelity) {
        Mogram ext = getComponentMogram(componentFidelity.getPath());
        String fn = (String) componentFidelity.getSelects().get(0);
        Fidelity cf = ext.getFidelities().get(fn);
        if (cf != null) {
            ext.setFidelity(cf);
            setSelectedFidelitySelector(fn);
        } else {
            logger.warn("no such fidelity for {}" + componentFidelity);
        }
        trimNotSerializableSignatures(cf);
        return cf;

//
//        if (ext != null && ext.getFidelity() != null
//                && serviceFidelities.containsKey(componetFidelty.getName())) {
//            Fidelity<Signature> sf = null;
//            if (componetFidelty.selects != null && componetFidelty.selects.size() > 0)
//                sf = new Fidelity(ext.getFidelities().get(componetFidelty.getName()), componetFidelty.selects);
//            else
//                sf = ext.getFidelities().get(componetFidelty.getName());
//
//            if (sf == null)
//                throw new ExertionException("no such service fidelity: " + fn + " at: " + ext);
//            ((ServiceExertion)ext).setFidelity(sf);
//            ((ServiceExertion)ext).setSelectedFidelitySelector(fn);
//        }
    }

    public Fidelity selectCompositeFidelity(Fidelity fidelity) {
        if (fidelity.type == Fidelity.Type.COMPOSITE) {
            for (Object obj : fidelity.selects) {
                if (obj instanceof Fidelity) {
                    if (((Fidelity) obj).type == Fidelity.Type.COMPONENT)
                        selectComponentFidelity((Fidelity) obj);
                    else
                        selectFidelity(((Fidelity) obj).getName());
                }
            }
        }
        return fidelity;
    }

    public Fidelity  selectFidelity()  {
        if (serviceFidelitySelector != null && serviceFidelities != null
                && serviceFidelities.containsKey(serviceFidelitySelector)) {
            Fidelity sf = serviceFidelities.get(serviceFidelitySelector);
            if (sf == null)
                logger.warn("no such service fidelity: {}", serviceFidelitySelector);
            serviceFidelity = sf;
        }
        trimNotSerializableSignatures(serviceFidelity);
        return serviceFidelity;
    }

    public void removeSignature(int index) {
        serviceFidelity.selects.remove(index);
    }


    public void addSignatures(Fidelity<Signature> fidelity) {
        if (this.serviceFidelity != null)
            this.serviceFidelity.selects.addAll(fidelity.selects);
        else {
            this.serviceFidelity = new Fidelity();
            this.serviceFidelity.selects.addAll(fidelity.selects);
        }
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

}
