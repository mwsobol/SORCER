package sorcer.service;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.context.FidelityContext;
import sorcer.core.monitor.MonitoringSession;
import sorcer.core.signature.ServiceSignature;
import sorcer.security.util.SorcerPrincipal;

import javax.security.auth.Subject;
import java.io.Serializable;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by sobolemw on 5/4/15.
 */
public abstract class ServiceMogram implements Mogram, Exec, Serializable, SorcerConstants {

    static final long serialVersionUID =  1L;

    protected String name;

    protected Uuid mogramId;

    protected Mogram parent;

    protected Uuid parentId;

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

    /** position of component Mogram in a compund mogram */
    protected Integer index = new Integer(-1);

    /** execution status: INITIAL|DONE|RUNNING|SUSPENDED|HALTED */
    protected Integer status = Exec.INITIAL;

    protected Integer priority;

    // the exertions's dependency scope
    protected Context scope;

    protected Integer scopeCode = PRIVATE_SCOPE;

    protected String description;

    protected String projectName;

    protected boolean isRevaluable = false;

    // service fidelities for this exertions
    protected Map<String, Fidelity<Signature>> serviceFidelities;

    protected Fidelity<Signature> serviceFidelity = new Fidelity<Signature>();

    protected SorcerPrincipal principal;

    // the current fidelity alias, as it is named in 'fidelities'
    // its original name might be different if aliasing is used
    // for already existing names
    protected String namedServiceFidelity;

    // fidelity Contexts for its component exertions
    protected Map<String, FidelityContext> fidelityContexts;

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
    public void setScope(Context scope) throws ContextException {

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
        for (Signature s : serviceFidelity.getSelects()) {
            if (s.getType() == Signature.Type.SRV)
                return s;
        }
        return null;
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


}
