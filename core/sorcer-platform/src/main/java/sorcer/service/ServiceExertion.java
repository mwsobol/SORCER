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

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.ComponentSelectionFidelity;
import sorcer.core.SorcerConstants;
import sorcer.core.context.*;
import sorcer.core.context.model.par.Par;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.invoker.ExertInvoker;
import sorcer.core.monitor.MonitoringSession;
import sorcer.core.provider.Jobber;
import sorcer.core.provider.Spacer;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.core.signature.NetSignature;
import sorcer.core.signature.ServiceSignature;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.Signature.ReturnPath;
import sorcer.service.Signature.Type;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;

import javax.security.auth.Subject;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public abstract class ServiceExertion implements Exertion, SorcerConstants, Exec, Serializable {

    static final long serialVersionUID = -3907402419486719293L;

    protected final static Logger logger = Logger
            .getLogger(ServiceExertion.class.getName());

    protected Uuid exertionId;

    protected String runtimeId;

    protected Uuid parentId;

    protected Exertion parent;

    protected String ownerId;

    protected String subjectId;

    protected Subject subject;

    protected String domainId;

    protected String subdomainId;

    protected Long lsbId;

    protected Long msbId;

    protected Uuid sessionId;

    protected MonitoringSession monitorSession;

    /** position of Exertion in a job */
    protected Integer index;

    protected String name;

    protected String description;

    protected String project;

    protected String goodUntilDate;

    protected String accessClass;

    protected Boolean isExportControlled;

    protected Integer scopeCode;

    // Date of creation of this Exertion
    protected Date creationDate = new Date();

    /** execution status: INITIAL|DONE|RUNNING|SUSPENDED|HALTED */
    protected Integer status = Exec.INITIAL;

    protected Integer priority;

    // service fidelities for this exertions
    protected Map<String, ServiceFidelity> fidelities;

    protected ServiceFidelity fidelity = new ServiceFidelity();

    // the current fidelity alias, as it is named in 'fidelities'
    // its original name is different if aliasing is used for already
    // existing names
    protected String selectedFidelitySelector;

    // fidelity Contexts for its component exertions
    protected Map<String, FidelityContext> fidelityContexts;

    protected ServiceContext dataContext;

    public static boolean debug = false;

    private static String defaultName = "xrt-";

    // sequence number for unnamed Exertion instances
    public static int count = 0;

    /**
     * A form of service context that describes the control strategy of this
     * exertion.
     */
    protected ControlContext controlContext;

    protected List<Setter> setters;

    protected SorcerPrincipal principal;

    protected boolean isRevaluable = false;

    // if isProxy is true then the identity of returned exertion
    // after exerting it is preserved
    protected boolean isProxy = false;

    // the exertions's dependency scope
    protected Context scope;

    // dependency management for this exertion
    protected List<Evaluation> dependers = new ArrayList<Evaluation>();

    public ServiceExertion() {
        this(defaultName + count++);
    }

    public ServiceExertion(String name) {
        init(name);
    }

    protected void init(String name) {
        if (name == null || name.length() == 0)
            this.name = defaultName + count++;
        else
            this.name = name;
        exertionId = UuidFactory.generate();
        domainId = "0";
        subdomainId = "0";
        index = new Integer(-1);
        accessClass = PUBLIC;
        isExportControlled = Boolean.FALSE;
        scopeCode = new Integer(PRIVATE_SCOPE);
        status = new Integer(INITIAL);
        dataContext = new PositionalContext(name);
        controlContext = new ControlContext(this);
        principal = new SorcerPrincipal(System.getProperty("user.name"));
        principal.setId(principal.getName());
        setSubject(principal);

        Calendar c = new GregorianCalendar();
        c.roll(Calendar.YEAR, true);
        goodUntilDate = Integer.toString(c.get(Calendar.MONTH)) + "/"
                + Integer.toString(c.get(Calendar.DAY_OF_MONTH)) + "/"
                + Integer.toString(c.get(Calendar.YEAR));
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Service#service(sorcer.service.Exertion)
     */
    public <T extends Mogram> T  service(T exertion) throws TransactionException,
            ExertionException, RemoteException {
        if (exertion == null)
            return exert();
        else
            return (T) exertion.exert();
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Service#service(sorcer.service.Exertion,
     * net.jini.core.transaction.Transaction)
     */
    public <T extends Mogram> T service(T exertion, Transaction txn)
            throws TransactionException, ExertionException, RemoteException {
        if (exertion == null)
            return exert();
        else
            return (T) exertion.exert(txn);
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Invoker#invoke()
     */
    public Object invoke() throws RemoteException,
            InvocationException {
        return invoke(new Arg[] {});
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Invoker#invoke(sorcer.service.Arg[])
     */
    public Object invoke(Arg[] entries) throws RemoteException,
            InvocationException {
        ReturnPath rp = null;
        for (Arg a : entries) {
            if (a instanceof ReturnPath) {
                rp = (ReturnPath) a;
                break;
            }
        }
        try {
            Object obj = null;
            Exertion xrt = exert(entries);
            if (rp == null) {
                obj =  xrt.getReturnValue();
            } else {
                Context cxt = xrt.getContext();
                if (rp.path == null)
                    obj = cxt;
                else if (rp.path.equals("self"))
                    obj = xrt;
                else  if (rp.outPaths != null) {
                    obj = ((ServiceContext)cxt).getSubcontext(rp.outPaths);
                } else {
                    obj = cxt.getValue(rp.path);
                }
            }
            return obj;
        } catch (Exception e) {
            throw new InvocationException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Invoker#invoke(sorcer.service.Context,
     * sorcer.service.Arg[])
     */
    public Object invoke(Context context, Arg[] entries)
            throws RemoteException, InvocationException {
        try {
            substitute(entries);
            if (context != null) {
                if (((ServiceContext) context).isLinked()) {
                    List<Mogram> exts = getAllMograms();
                    for (Mogram e : exts) {
                        Object link = context.getLink(e.getName());
                        if (link instanceof ContextLink) {
                            ((Exertion)e).getContext().append(
                                    ((ContextLink) link).getContext());
                        }
                    }

                }
                // else {
                // dataContext.append(context);
                // }
            }
            return invoke(entries);
        } catch (Exception e) {
            throw new InvocationException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Exertion#exert(net.jini.core.transaction.Transaction,
     * sorcer.servie.Arg[])
     */
    public <T extends Mogram> T exert(Transaction txn, Arg... entries)
            throws TransactionException, ExertionException, RemoteException {
        ServiceShell se = new ServiceShell(this);
        Exertion result = null;
        try {
            result = se.exert(txn, null, entries);
        } catch (Exception e) {
            e.printStackTrace();
            if (result != null)
                ((ServiceExertion) result).reportException(e);
        }
        return (T) result;
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Exertion#exert(sorcer.core.context.Path.Entry[])
     */
    public <T extends Mogram> T  exert(Arg... entries) throws TransactionException,
            ExertionException, RemoteException {
        try {
            substitute(entries);
        } catch (SetterException e) {
            e.printStackTrace();
            throw new ExertionException(e);
        }

        ServiceShell se = new ServiceShell(this);
        return se.exert(entries);
    }

    public Exertion exert(Transaction txn, String providerName, Arg... entries)
            throws TransactionException, ExertionException, RemoteException {
        try {
            substitute(entries);
        } catch (SetterException e) {
            e.printStackTrace();
            throw new ExertionException(e);
        }
        ServiceShell se = new ServiceShell(this);
        return se.exert(txn, providerName);
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

    public String getPrincipalID() {
        SorcerPrincipal p = getSorcerPrincipal();
        if (p != null)
            return getSorcerPrincipal().getId();
        else
            return null;
    }

    public void setPrincipalID(String id) {
        SorcerPrincipal p = getSorcerPrincipal();
        if (p != null)
            p.setId(id);
    }

    public void removeSignature(int index) {
        fidelity.remove(index);
    }

    public void setAccess(Access access) {
        controlContext.setAccessType(access);
    }

    public void setFlow(Flow type) {
        controlContext.setFlowType(type);
    }

    public ServiceFidelity getFidelity() {
        return fidelity;
    }

    public void addSignatures(ServiceFidelity signatures) {
        if (this.fidelity != null)
            this.fidelity.addAll(signatures);
        else {
            this.fidelity = new ServiceFidelity();
            this.fidelity.addAll(signatures);
        }
    }

    public boolean isBatch() {
        for (Signature s : fidelity) {
            if (s.getType() != Signature.Type.SRV)
                return false;
        }
        return true;
    }

    public void setFidelity(ServiceFidelity fidelity) {
        this.fidelity = fidelity;
    }

    public void putFidelity(ServiceFidelity fidelity) {
        if (fidelities == null)
            fidelities = new HashMap<String, ServiceFidelity>();
        fidelities.put(fidelity.getName(), fidelity);
    }

    public void addFidelity(ServiceFidelity fidelity) {
        putFidelity(fidelity.getName(), fidelity);
        selectedFidelitySelector = name;
        this.fidelity = fidelity;
    }

    public void setFidelity(String name, ServiceFidelity fidelity) {
        this.fidelity = new ServiceFidelity(name, fidelity);
        putFidelity(name, fidelity);
        selectedFidelitySelector = name;
    }

    public void putFidelity(String name, ServiceFidelity fidelity) {
        if (fidelities == null)
            fidelities = new HashMap<String, ServiceFidelity>();
        fidelities.put(name, new ServiceFidelity(name, fidelity));
    }

    public void addFidelity(String name, ServiceFidelity fidelity) {
        ServiceFidelity nf = new ServiceFidelity(name, fidelity);
        putFidelity(name, nf);
        selectedFidelitySelector = name;
        fidelity = nf;
    }

    public void selectFidelity(Arg... entries) throws ExertionException {
        if (entries != null && entries.length > 0) {
            for (Arg a : entries)
                if (a instanceof ComponentSelectionFidelity) {
                    selectComponentFidelity((ComponentSelectionFidelity) a);
                } else if (a instanceof SelectionFidelity) {
                    selectFidelity(((SelectionFidelity) a).getName());
                } else if (a instanceof FidelityContext) {
                    if (((FidelityContext) a).size() == 0
                            && ((FidelityContext) a).getName() != null)
                        applyFidelityContext(fidelityContexts
                                .get(((FidelityContext) a).getName()));
                    else
                        applyFidelityContext((FidelityContext) a);
                }
        }
    }

    public void selectFidelity(String selector) throws ExertionException {
        if (selector != null && fidelities != null
                && fidelities.containsKey(selector)) {
            ServiceFidelity sf = fidelities.get(selector);

            if (sf == null)
                throw new ExertionException("no such service fidelity: " + selector + " at: " + this);
            fidelity = sf;
            selectedFidelitySelector = selector;
        }
    }

    public void selectComponentFidelity(ComponentSelectionFidelity componetFiInfo) throws ExertionException {
        Exertion ext = (Exertion) getComponentMogram(componetFiInfo.getPath());
        String fn = componetFiInfo.getName();
        if (ext != null && ext.getFidelity() != null
                && fidelities.containsKey(componetFiInfo.getName())) {
            ServiceFidelity sf = null;
            if (componetFiInfo.getSelectors() != null && componetFiInfo.getSelectors().length > 0)
                sf = new ServiceFidelity(ext.getFidelities().get(componetFiInfo.getName()), componetFiInfo.getSelectors());
            else
                sf = ext.getFidelities().get(componetFiInfo.getName());

            if (sf == null)
                throw new ExertionException("no such service fidelity: " + fn + " at: " + ext);
            ((ServiceExertion)ext).setFidelity(sf);
            ((ServiceExertion)ext).setSelectedFidelitySelector(fn);
        }
    }

    public void applyFidelityContext(FidelityContext fiContext) throws ExertionException {
        throw new ExertionException("is not implemented by this CompoundExertion");
    }

    public void selectFidelity() throws ExertionException {
        if (selectedFidelitySelector != null && fidelities != null
                && fidelities.containsKey(selectedFidelitySelector)) {
            ServiceFidelity sf = fidelities.get(selectedFidelitySelector);
            if (sf == null)
                throw new ExertionException("no such service fidelity: "
                        + selectedFidelitySelector);
            fidelity = sf;
        }
    }

    public void setProcessSignature(Signature signature) {
        for (Signature sig : this.fidelity) {
            if (sig.getType() != Type.SRV) {
                this.fidelity.remove(sig);
            }
        }
        this.fidelity.add(signature);
    }

    public void setService(Service provider) {
        NetSignature ps = (NetSignature) getProcessSignature();
        ps.setProvider(provider);
    }

    public Service getService() {
        NetSignature ps = (NetSignature) getProcessSignature();
        return ps.getService();
    }

    public Flow getFlowType() {
        return controlContext.getFlowType();
    }

    public void setFlowType(Flow flowType) {
        controlContext.setFlowType(flowType);
    }

    public Access getAccessType() {
        return controlContext.getAccessType();
    }

    public void setAccessType(Access accessType) {
        controlContext.setAccessType(accessType);
    }

    public int getScopeCode() {
        return (scopeCode == null) ? -1 : scopeCode.intValue();
    }

    public void setScopeCode(int value) {
        scopeCode = new Integer(value);
    }

    public SorcerPrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(SorcerPrincipal principal) {
        this.principal = principal;
    }

    public Uuid getParentId() {
        return parentId;
    }

    public void setParentId(Uuid parentId) {
        this.parentId = parentId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String id) {
        ownerId = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int value) {
        status = value;
    }

    public void setSubjectId(String id) {
        subjectId = id;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public void setProject(String projectName) {
        project = projectName;
    }

    public String getProject() {
        return project;
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

    public String getGoodUntilDate() {
        return goodUntilDate;
    }

    public void setGoodUntilDate(String date) {
        goodUntilDate = date;
    }

    public Uuid getId() {
        return exertionId;
    }

    public String getDeploymentId(List<Signature> list) throws NoSuchAlgorithmException {
        StringBuilder ssb = new StringBuilder();
        for (Signature s : list) {
            ssb.append(s.getProviderName());
            ssb.append(s.getServiceType());
        }
        return ServiceDeployment.createDeploymentID(ssb.toString());
    }

    public String getDeploymentId() throws NoSuchAlgorithmException {
        return getDeploymentId(getAllNetTaskSignatures());
    }

    public String getRuntimeId() {
        return runtimeId;
    }

    public void setRuntimeId(String id) {
        runtimeId = id;
    }

    public void setId(Uuid id) {
        exertionId = id;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRendezvousName() {
        return controlContext.getRendezvousName();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getIndex() {
        return (index == null) ? -1 : index;
    }

    public void setIndex(int i) {
        index = i;
    }

    public boolean isMonitorable() {
        return controlContext.isMonitorable();
    }

    public void setMonitored(boolean state) {
        controlContext.setMonitorable(state);
    }

    public boolean isWaitable() {
        return controlContext.isWaitable();
    }

    public void setWait(boolean state) {
        controlContext.setWaitable(state);
    }

    // should be implemented in subclasses accordingly
    public boolean hasChild(String childName) {
        return false;
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

    public void setSessionId(Uuid id) {
        sessionId = id;
        if (this instanceof Job) {
            List<Mogram> v = ((Job) this).getMograms();
            for (int i = 0; i < v.size(); i++) {
                ((ServiceExertion) v.get(i)).setSessionId(id);
            }
        }
    }

    public Uuid getSessionId() {
        return sessionId;
    }

    public ServiceExertion setContext(Context context) {
        this.dataContext = (ServiceContext) context;
        if (context != null)
            ((ServiceContext) context).setExertion(this);
        return this;
    }

    public ServiceExertion setControlContext(ControlContext context) {
        controlContext = context;
        return this;
    }

    public ServiceExertion updateStrategy(ControlContext context) {
        controlContext.setAccessType(context.getAccessType());
        controlContext.setFlowType(context.getFlowType());
        controlContext.setProvisionable(context.isProvisionable());
        controlContext.setShellRemote(context.isShellRemote());
        controlContext.setMonitorable(context.isMonitorable());
        controlContext.setWaitable(context.isWaitable());
        controlContext.setSignatures(context.getSignatures());
        return this;
    }

    public void setPriority(int p) {
        priority = p;
    }

    public int getPriority() {
        return (priority == null) ? MIN_PRIORITY : priority;
    }

    public Signature getProcessSignature() {
        for (Signature s : fidelity) {
            if (s.getType() == Signature.Type.SRV)
                return s;
        }
        return null;
    }

    public List<Signature> getApdProcessSignatures() {
        List<Signature> sl = new ArrayList<Signature>();
        for (Signature s : fidelity) {
            if (s.getType() == Signature.Type.APD_DATA)
                sl.add(s);
        }
        return sl;
    }

    public List<Signature> getPreprocessSignatures() {
        List<Signature> sl = new ArrayList<Signature>();
        for (Signature s : fidelity) {
            if (s.getType() == Signature.Type.PRE)
                sl.add(s);
        }
        return sl;
    }

    public List<Signature> getPostprocessSignatures() {
        List<Signature> sl = new ArrayList<Signature>();
        for (Signature s : fidelity) {
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
        ((ServiceSignature) signature).setOwnerId(getOwnerId());
        fidelity.add(signature);
    }

    /**
     * Removes a signature <code>signature</code> for this exertion.
     *
     * @see #addSignature
     */
    public void removeSignature(Signature signature) {
        fidelity.remove(signature);
    }

    public Class getServiceType() {
        Signature signature = getProcessSignature();
        return (signature == null) ? null : signature.getServiceType();
    }

    @Override
    public Context getScope() {
        return dataContext.getScope();
    }

    public String getSelector() {
        Signature method = getProcessSignature();
        return (method == null) ? null : method.getSelector();
    }

    public int compareByIndex(Exertion e) {
        if (this.getIndex() > ((ServiceExertion) e).getIndex())
            return 1;
        else if (this.getIndex() < ((ServiceExertion) e).getIndex())
            return -1;
        else
            return 0;
    }

    public boolean isExecutable() {
        if (getServiceType() != null)
            return true;
        else
            return false;
    }

    public Exertion getParent() {
        return parent;
    }

    public void setParent(Exertion parent) {
        this.parent = parent;
    }

    public String contextToString() {
        return "";
    }

    public int getExceptionCount() {
        return controlContext.getExceptions().size();
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Exertion#getTrace()
     */
    public List<String> getTrace() {
        return controlContext.getTrace();
    }

    /** {@inheritDoc} */
    public boolean isTree() {
        return isTree(new HashSet());
    }

    public Context getDataContext() throws ContextException {
        return dataContext;
    }

    public ControlContext getControlContext() {
        return controlContext;
    }

    public Context getContext() throws ContextException {
        return getDataContext();
    }

    public Context getContext(String componentExertionName)
            throws ContextException {
        Exertion component = getExertion(componentExertionName);
        if (component != null)
            return getExertion(componentExertionName).getContext();
        else
            return null;
    }

    public Context getControlContext(String componentExertionName) {
        Exertion component = getExertion(componentExertionName);
        if (component != null)
            return getExertion(componentExertionName).getControlContext();
        else
            return null;
    }

    public Context getControlInfo() {
        return controlContext;
    }

    public void startExecTime() {
        if (controlContext.isExecTimeRequested())
            controlContext.startExecTime();
    }

    public void stopExecTime() {
        if (controlContext.isExecTimeRequested())
            controlContext.stopExecTime();
    }

    public String getExecTime() {
        if (controlContext.isExecTimeRequested()
                && controlContext.getStopwatch() != null)
            return controlContext.getExecTime();
        else
            return "";
    }

    public void setExecTimeRequested(boolean state) {
        controlContext.setExecTimeRequested(state);
    }

    public boolean isExecTimeRequested() {
        return controlContext.isExecTimeRequested();
    }

    public Par getPar(String path) throws EvaluationException, RemoteException {
        return new Par(path, this);
    }

    abstract public Context linkContext(Context context, String path)
            throws ContextException;

    abstract public Context linkControlContext(Context context, String path)
            throws ContextException;

    public Context finalizeOutDataContext() throws ContextException {
        if (dataContext.getOutConnector() != null) {
            dataContext.updateContextWith(dataContext.getOutConnector());
        }
        return dataContext;
    }

    /*
     * Subclasses implement this to support the isTree() algorithm.
     */
    public abstract boolean isTree(Set visited);

    public void reportException(Throwable t) {
        controlContext.addException(t);
    }

    public void addException(ThrowableTrace et) {
        controlContext.addException(et);
    }

    public ExertInvoker getInoker() {
        return new ExertInvoker(this);
    }

    public ExertInvoker getInvoker(String name) {
        ExertInvoker invoker = new ExertInvoker(this);
        invoker.setName(name);
        return invoker;
    }

    @Override
    public ServiceExertion substitute(Arg... entries)
            throws SetterException {
        if (entries != null && entries.length > 0) {
            for (Arg e : entries) {
                if (e instanceof Entry) {
                    try {
                        putValue((String) ((Entry) e).path(),
                                ((Entry) e).value());
                    } catch (ContextException ex) {
                        ex.printStackTrace();
                        throw new SetterException(ex);
                    }
                    // check for control strategy
                } else if (e instanceof ControlContext) {
                    updateControlContect((ControlContext)e);
                }
            }
        }
        return this;
    }

    protected void updateControlContect(ControlContext startegy) {
        Access at = startegy.getAccessType();
        if (at != null)
            controlContext.setAccessType(at);
        Flow ft = startegy.getFlowType();
        if (ft != null)
            controlContext.setFlowType(ft);
        if (controlContext.isProvisionable() != startegy.isProvisionable())
            controlContext.setProvisionable(startegy.isProvisionable());
        if (controlContext.isShellRemote() != startegy.isShellRemote())
            controlContext.setShellRemote(startegy.isShellRemote());
        if (controlContext.isWaitable() != (startegy.isWaitable()))
            controlContext.setWaitable(startegy.isWaitable());
        if (controlContext.isMonitorable() != startegy.isMonitorable())
            controlContext.setMonitorable(startegy.isMonitorable());
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Exertion#getReturnValue(sorcer.service.Arg[])
     */
    public Object getReturnValue(Arg... entries) throws ContextException,
            RemoteException {
        ReturnPath returnPath = ((ServiceContext)getDataContext()).getReturnPath();
        if (returnPath != null) {
            if (returnPath.path == null || returnPath.path.equals("self"))
                return getContext();
            else
                return getContext().getValue(returnPath.path, entries);
        } else {
            return getContext();
        }
    }

    public List<Setter> getPersisters() {
        return setters;
    }

    public void addPersister(Setter persister) {
        if (setters == null)
            setters = new ArrayList<Setter>();
        setters.add(persister);
    }

    // no control context
    public String info() {
        StringBuffer info = new StringBuffer()
                .append(this.getClass().getName()).append(": " + name);
        info.append("\n  process sig=").append(getProcessSignature());
        info.append("\n  status=").append(status);
        info.append(", exertion ID=").append(exertionId);
        String time = getControlContext().getExecTime();
        if (time != null && time.length() > 0) {
            info.append("\n  Execution Time = " + time);
        }
        return info.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Evaluation#isEvaluable()
     */
    public boolean isModeling() {
        return isRevaluable;
    }

    public void setModeling(boolean isRevaluable) {
        this.isRevaluable = isRevaluable;
    }

    public String toString() {
        if (debug)
            return describe();

        StringBuffer info = new StringBuffer()
                .append(this.getClass().getName()).append(": " + name);
        info.append("\n  status=").append(status);
        info.append(", exertion ID=").append(exertionId);
        String time = getControlContext().getExecTime();
        if (time != null && time.length() > 0) {
            info.append("\n  Execution Time = " + time);
        }
        info.append("\n  [Control Context]\n");
        info.append(getControlContext() + "\n");
        return info.toString();
    }

    public List<Mogram> getAllMograms() {
        List<Mogram> exs = new ArrayList<Mogram>();
        getMograms(exs);
        return exs;
    }

    public List<ServiceDeployment> getDeployments() {
        List<Signature> nsigs = getAllNetSignatures();
        List<ServiceDeployment> deploymnets = new ArrayList<ServiceDeployment>();
        for (Signature s : nsigs) {
            ServiceDeployment d = ((ServiceSignature)s).getDeployment();
            if (d != null)
                deploymnets.add(d);
        }
        return deploymnets;
    }

    @Override
    public List<Signature> getAllNetSignatures() {
        List<Signature> allSigs = getAllSignatures();
        List<Signature> netSignatures = new ArrayList<Signature>();
        for (Signature s : allSigs) {
            if (s instanceof NetSignature)
                netSignatures.add((NetSignature)s);
        }
        Collections.sort(netSignatures);
        return netSignatures;
    }

    @Override
    public List<Signature> getAllNetTaskSignatures() {
        List<Signature> allSigs = getAllTaskSignatures();
        List<Signature> netSignatures = new ArrayList<Signature>();
        for (Signature s : allSigs) {
            if (s instanceof NetSignature)
                netSignatures.add((NetSignature)s);
        }
        Collections.sort(netSignatures);
        return netSignatures;
    }

    public List<ServiceDeployment> getDeploymnets() {
        List<Signature> nsigs = getAllNetSignatures();
        List<ServiceDeployment> deploymnets = new ArrayList<ServiceDeployment>();
        for (Signature s : nsigs) {
            ServiceDeployment d = ((ServiceSignature)s).getDeployment();
            if (d != null)
                deploymnets.add(d);
        }
        return deploymnets;
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Exertion#getExceptions()
     */
    @Override
    public List<ThrowableTrace> getExceptions() {
        if (controlContext != null)
            return controlContext.getExceptions();
        else
            return new ArrayList<ThrowableTrace>();
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Exertion#getExceptions()
     */
    @Override
    public List<ThrowableTrace> getAllExceptions() {
        List<ThrowableTrace> exceptions = new ArrayList<ThrowableTrace>();
        return getExceptions(exceptions);
    }

    public List<ThrowableTrace> getExceptions(List<ThrowableTrace> exs) {
        if (controlContext != null)
            exs.addAll(controlContext.getExceptions());
        return exs;
    }

    public List<Signature> getAllSignatures() {
        List<Signature> allSigs = new ArrayList<Signature>();
        List<Mogram> allExertions = getAllMograms();
        for (Mogram e : allExertions) {
            allSigs.add(e.getProcessSignature());
        }
        return allSigs;
    }

    public List<Signature> getAllTaskSignatures() {
        List<Signature> allSigs = new ArrayList<Signature>();
        List<Mogram> allExertions = getAllMograms();
        for (Mogram e : allExertions) {
            if (e instanceof Task)
                allSigs.add(((Exertion)e).getProcessSignature());
        }
        return allSigs;
    }

    public List<ServiceDeployment> getAllDeployments() {
        List<ServiceDeployment> allDeployments = new ArrayList<ServiceDeployment>();
        List<Signature> allSigs = getAllNetTaskSignatures();
        for (Signature s : allSigs) {
            allDeployments.add((ServiceDeployment)s.getDeployment());
        }
        return allDeployments;
    }

    abstract public List<Mogram> getMograms(List<Mogram> exs);

    public void updateValue(Object value) throws ContextException {
        List<Mogram> exertions = getAllMograms();
        // logger.info(" value = " + value);
        // logger.info(" this exertion = " + this);
        // logger.info(" exertions = " + exertions);
        for (Mogram e : exertions) {
            if (e instanceof Exertion && !((Exertion)e).isJob()) {
                // logger.info(" exertion i = "+ e.getName());
                Context cxt = ((Exertion)e).getContext();
                ((ServiceContext) cxt).updateValue(value);
            }
        }
    }

    public Exertion getExertion(String componentExertionName) {
        if (name.equals(componentExertionName)) {
            return this;
        } else {
            List<Mogram> exertions = getAllMograms();
            for (Mogram e : exertions) {
                if (e.getName().equals(componentExertionName)) {
                    return (Exertion)e;
                }
            }
            return null;
        }
    }

    public String state() {
        return controlContext.getRendezvousName();
    }

    // Check if this is a Job that will be performed by Spacer
    public boolean isSpacable() {
        return  (controlContext.getAccessType().equals(Access.PULL));
    }

    public Signature correctProcessSignature() {
        Signature sig = getProcessSignature();
        if (sig != null) {
            Access access = getControlContext().getAccessType();

            if (Access.PULL == access
                    && !getProcessSignature().getServiceType()
                    .isAssignableFrom(Spacer.class)) {
                sig.setServiceType(Spacer.class);
                ((NetSignature) sig).setSelector("service");
                sig.setProviderName(ANY);
                sig.setType(Signature.Type.SRV);
                getControlContext().setAccessType(access);
            } else if (Access.PUSH == access
                    && !getProcessSignature().getServiceType()
                    .isAssignableFrom(Jobber.class)) {
                if (sig.getServiceType().isAssignableFrom(Spacer.class)) {
                    sig.setServiceType(Jobber.class);
                    ((NetSignature) sig).setSelector("service");
                    sig.setProviderName(ANY);
                    sig.setType(Signature.Type.SRV);
                    getControlContext().setAccessType(access);
                }
            }
        }
        return sig;
    }

    public void reset(int state) {
        status = state;
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

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Evaluation#getValue()
     */
    public Object getValue(Arg... entries) throws EvaluationException,
            RemoteException {
        Context cxt = null;
        try {
            substitute(entries);
            Exertion evaluatedExertion = exert(entries);
            ReturnPath rp = ((ServiceContext)evaluatedExertion.getDataContext())
                    .getReturnPath();
            if (evaluatedExertion instanceof Job) {
                cxt = ((Job) evaluatedExertion).getJobContext();
            } else {
                cxt = evaluatedExertion.getContext();
            }

            if (rp != null) {
                if (rp.path == null)
                    return cxt;
                else if (rp.path.equals("self"))
                    return this;
                else if (rp.path != null) {
                    cxt.setReturnValue(cxt.getValue(rp.path));
                    Context out = null;
                    if (rp.outPaths != null && rp.outPaths.length > 0) {
                        out = ((ServiceContext)cxt).getSubcontext(rp.outPaths);
                        cxt.setReturnValue(out);
                        return out;
                    }
                    return cxt.getReturnValue();
                } else {
                    return cxt.getReturnValue();
                }
            }
        } catch (Exception e) {
            throw new InvocationException(e);
        }
        return cxt;
    }

    public Object getExertionScope() throws RemoteException {
        return scope;
    }

    /**
     * Assigns the dependency scope for this exertion.
     *
     * @param scope
     *            the scope to set
     */
    public void setScope(Context scope) {
        dataContext.setScope((Context)scope);
    }

    /**
     * Assigns the dependency scope for this exertion.
     *
     * @param scope
     *            the scope to set
     */
    public void setExertionScope(Object scope) throws RemoteException, ContextException {
        this.scope = (Context)scope;
    }

    /**
     * Return a list of dependent agents.
     *
     * @return the dependers
     */
    public List<Evaluation> getDependers() {
        return dependers;
    }

    /**
     * <p>
     * Assigns a list of dependent agents.
     * </p>
     *
     * @param dependers
     *            the dependers to set
     */
    public void setDependers(List<Evaluation> dependers) {
        this.dependers = dependers;
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Evaluation#getAsIs()
     */
    public Object asis() throws EvaluationException, RemoteException {
        return getValue();
    }

    public Object asis(String path) throws ContextException {
        Context cxt = null;
        if (isJob()) {
            cxt = ((Job) this).getJobContext();
        } else {
            cxt = dataContext;
        }
        return cxt.get(path);
    }

    public Object putValue(String path, Object value) throws ContextException {
        Context cxt = null;
        if (isJob()) {
            cxt = ((Job) this).getJobContext();
        } else {
            cxt = dataContext;
        }
        return cxt.putValue(path, value);
    }

    public Map<String, ServiceFidelity> getFidelities() {
        return fidelities;
    }

    public void setFidelities(Map<String, ServiceFidelity> fidelities) {
        this.fidelities = fidelities;
    }

    public String getSelectedFidelitySelector() {
        return selectedFidelitySelector;
    }

    public void setSelectedFidelitySelector(String selectedFidelitySelector) {
        this.selectedFidelitySelector = selectedFidelitySelector;
    }

    public Map<String, FidelityContext> getFidelityContexts() {
        return fidelityContexts;
    }

    public void setFidelityContexts(Map<String, FidelityContext> fidelityContexts) {
        this.fidelityContexts = fidelityContexts;
    }

    public List<Setter> getSetters() {
        return setters;
    }

    public void setSetters(List<Setter> setters) {
        this.setters = setters;
    }

    public boolean isConditional() {
        return false;
    }

    /* (non-Javadoc)
     * @see sorcer.service.Exertion#isCompound()
     */
    @Override
    public boolean isCompound() {
        return false;
    }

    public boolean isJob() {
        return false;
    }

    public boolean isTask() {
        return false;
    }

    public boolean isBlock() {
        return false;
    }

    public boolean isCmd() {
        return false;
    }

    public boolean isProvisionable() {
        return controlContext.isProvisionable();
    }

    public void setProvisionable(boolean state) {
        controlContext.setProvisionable(state);
    }

    public void setShellRemote(boolean state) {
        controlContext.setShellRemote(state);
    }

    public boolean isProxy() {
        return isProxy;
    }

    public void setProxy(boolean isProxy) {
        this.isProxy = isProxy;
    }

    public Exertion addDepender(Evaluation depender) {
        if (this.dependers == null)
            this.dependers = new ArrayList<Evaluation>();
        dependers.add(depender);
        return this;
    }

    public void addDependers(Evaluation... dependers) {
        if (this.dependers == null)
            this.dependers = new ArrayList<Evaluation>();
        for (Evaluation depender : dependers)
            this.dependers.add(depender);
    }

    public Context updateContext() throws ContextException {
        return ((ServiceContext)getDataContext()).updateContext();
    }

    protected Context getCurrentContext() throws ContextException {
        return getDataContext().getCurrentContext();
    }

    public Exertion clearScope() throws ContextException {
        Signature.ReturnPath rp = dataContext.getReturnPath();
        if (rp != null && rp.path != null)
            dataContext.removePath(rp.path);

        return this;
    }

    /* (non-Javadoc)
     * @see sorcer.service.Exertion#getComponentMogram(java.lang.String)
     */
    @Override
    public Mogram getComponentMogram(String path) {
        return this;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public String describe() {
        if (!debug)
            return info();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        String stdoutSep = "================================================================================\n";
        StringBuffer info = new StringBuffer();
        info.append("\n" + stdoutSep)
                .append("[SORCER Service Exertion]\n")
                .append("\tExertion Type:        " + getClass().getName()
                        + "\n")
                .append("\tExertion Name:        " + name + "\n")
                .append("\tExertion Status:      " + status + "\n")
                .append("\tExertion ID:          " + exertionId + "\n")
             	.append("\tCreation Date:        " + sdf.format(creationDate) + "\n")
                .append("\tRuntime ID:           " + runtimeId + "\n")
                .append("\tParent ID:            " + parentId + "\n")
                .append("\tOwner ID:             " + ownerId + "\n")
                .append("\tSubject ID:           " + subjectId + "\n")
                .append("\tDomain ID:            " + domainId + "\n")
                .append("\tSubdomain ID:         " + subdomainId + "\n")
                .append("\tlsb ID:               " + lsbId + "\n")
                .append("\tmsb ID:               " + msbId + "\n")
                .append("\tSession ID:           " + sessionId + "\n")
                .append("\tIndex:                " + index + "\n")
                .append("\tDescription:          " + description + "\n")
                .append("\tProject:              " + project + "\n")
                .append("\tGood Until Date:      " + goodUntilDate + "\n")
                .append("\tAccess Class:         " + accessClass + "\n")
                .append("\tIs Export Controlled: " + isExportControlled + "\n")
                .append("\tScope Code:           " + scopeCode + "\n")
                .append("\tPriority:             " + priority + "\n")
                .append("\tProvider Name:        "
                        + getProcessSignature().getProviderName() + "\n")
                .append("\tService Type:         "
                        + getProcessSignature().getServiceType() + "\n")
                .append("\tException Count:      " + getExceptionCount() + "\n")
                .append("\tPrincipal:            " + principal + "\n")
                .append(stdoutSep).append("[Control Context]\n")
                .append(getControlContext() + "\n").append(stdoutSep);
        String time = getControlContext().getExecTime();
        if (time != null && time.length() > 0) {
            info.append("\nExecution Time = " + time + "\n" + stdoutSep);
        }
        return info.toString();
    }
}
