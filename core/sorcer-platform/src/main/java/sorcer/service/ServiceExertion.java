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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.*;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.par.Par;
import sorcer.core.deploy.DeploymentIdFactory;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.invoker.ExertInvoker;
import sorcer.core.provider.*;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.core.signature.NetSignature;
import sorcer.core.signature.ServiceSignature;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.Signature.ReturnPath;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;

import javax.security.auth.Subject;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.*;

import static sorcer.eo.operator.value;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public abstract class ServiceExertion extends ServiceMogram implements Exertion {

    static final long serialVersionUID = -3907402419486719293L;

    protected final static Logger logger = LoggerFactory.getLogger(ServiceExertion.class.getName());

    // The service provider for this job service bean
    protected ServiceProvider provider;

    protected ServiceContext dataContext;

    /**
     * A form of service context that describes the control strategy of this
     * exertion.
     */
    protected ControlContext controlContext;

    protected List<Setter> setters;

    // if isProxy is true then the identity of returned exertion
    // after exerting it is preserved
    protected boolean isProxy = false;


    // dependency management for this exertion
    protected List<Evaluation> dependers = new ArrayList<Evaluation>();

    public ServiceExertion() {
        super("xrt" +  count++);
    }

    public ServiceExertion(String name) {
        super(name);
    }

    public Exertion newInstance() throws SignatureException {
        return (Exertion) sorcer.co.operator.instance(builder);
    }

    protected void init() {
        super.init();
        dataContext = new PositionalContext(name);
        controlContext = new ControlContext(this);
    }

    /*
   *  Initialization for a service bean of this type
   */
    public void init(Provider provider) {
        this.provider = (ServiceProvider)provider;
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Service#service(sorcer.service.Mogram)
     */
    public <T extends Mogram> T  service(T mogram) throws TransactionException,
            MogramException, RemoteException {
        if (mogram == null)
            return exert();
        else
            return (T) mogram.exert();
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Service#service(sorcer.service.Exertion,
     * net.jini.core.transaction.Transaction)
     */
    public <T extends Mogram> T exert(T mogram, Transaction txn, Arg... args)
            throws TransactionException, MogramException, RemoteException {
        try {
            if (mogram instanceof Exertion) {
                ServiceExertion exertion = (ServiceExertion) mogram;
                Class serviceType = exertion.getServiceType();
                if (Invocation.class.isAssignableFrom(serviceType)) {
                    Object out = this.invoke(exertion.getContext(), args);
                    handleExertOutput(exertion, out);
                    return (T) exertion;
                } else if (Evaluation.class.isAssignableFrom(serviceType)) {
                    Object out = this.getValue(args);
                    handleExertOutput(exertion, out);
                    return (T) exertion;
                }
            }
            getContext().appendContext(mogram.getContext());
            return (T) exert(txn);
        } catch (Exception e) {
            e.printStackTrace();
            mogram.getContext().reportException(e);
            if (e instanceof Exception)
                mogram.setStatus(FAILED);
            else
                mogram.setStatus(ERROR);

            throw new ExertionException(e);
        }
    }

    private void handleExertOutput(ServiceExertion exertion, Object result ) throws ContextException {
        ServiceContext dataContext = (ServiceContext) exertion.getDataContext();
        if (result instanceof Context)
            dataContext.updateEntries((Context)result);

        ReturnPath rp = dataContext.getReturnPath();
        if (rp == null)
            rp = (ReturnPath)exertion.getProcessSignature().getReturnPath();
        else
            exertion.getProcessSignature().setReturnPath(rp);

        if (rp != null) {
            if (((Context) result).getValue(rp.path) != null) {
                dataContext.setReturnValue(((Context) result).getValue(rp.path));
                dataContext.setFinalized(true);
            }
        } else {
            dataContext.setReturnValue(result);
        }
        exertion.setStatus(DONE);

    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Invoker#invoke()
     */
    public Object invoke() throws InvocationException {
        return invoke(new Arg[]{});
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Invoker#invoke(sorcer.service.Arg[])
     */
    public Object invoke(Arg[] entries) throws InvocationException {
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
                else if (rp.path.equals(Signature.SELF))
                    obj = xrt;
                else  if (rp.outPaths != null) {
                    obj = ((ServiceContext)cxt).getDirectionalSubcontext(rp.outPaths);
                } else {
                    obj = cxt.getValue(rp.path);
                }
            }
            return obj;
        } catch (Exception e) {
            throw new InvocationException(e);
        }
    }

    public Object invoke(Context context) throws InvocationException {
        return invoke(context, new Arg[] {});
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Invoker#invoke(sorcer.service.Context,
     * sorcer.service.Arg[])
     */
    public Object invoke(Context context, Arg[] entries) throws InvocationException {
        try {
            substitute(entries);
            if (context != null) {
                if (((ServiceContext) context).isLinked()) {
                    List<Mogram> exts = getAllMograms();
                    for (Mogram e : exts) {
                        Object link = context.getLink(e.getName());
                        if (link instanceof ContextLink) {
                            e.getContext().append(
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
    public Exertion exert(Transaction txn, Arg... entries)
            throws TransactionException, MogramException, RemoteException {
        try {
            substitute(entries);
        } catch (SetterException e) {
            logger.error("Error in exertion {}", mogramId, e);
            throw new MogramException(e);
        }
        String prvName = null;
        for(Arg arg : entries) {
            if (arg instanceof ProviderName) {
                prvName = arg.getName();
                break;
            }
        }
        ServiceShell se = new ServiceShell(this);
        return se.exert(txn, prvName, entries);
    }

    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Exertion#exert(sorcer.core.context.Path.Entry[])
     */
    public <T extends Mogram> T  exert(Arg... entries) throws TransactionException,
            MogramException, RemoteException {
        try {
            substitute(entries);
        } catch (SetterException e) {
            e.printStackTrace();
            throw new ExertionException(e);
        }

        ServiceShell se = new ServiceShell(this);
        return se.exert(entries);
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

    public void setAccess(Access access) {
        controlContext.setAccessType(access);
    }

    public void setFlow(Flow type) {
        controlContext.setFlowType(type);
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

    public String getDeploymentId(List<Signature> list) throws NoSuchAlgorithmException {
        return DeploymentIdFactory.create(list);
    }

    public String getDeploymentId() throws NoSuchAlgorithmException {
        return getDeploymentId(getAllNetTaskSignatures());
    }

    public String getRendezvousName() {
        return controlContext.getRendezvousName();
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

    public void setSessionId(Uuid id) {
        sessionId = id;
        if (this instanceof CompoundExertion) {
            List<Mogram> v = ((CompoundExertion) this).getMograms();
            for (int i = 0; i < v.size(); i++) {
                ((ServiceExertion) v.get(i)).setSessionId(id);
            }
        }
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

    public Class getServiceType() {
        Signature signature = getProcessSignature();
        return (signature == null) ? null : signature.getServiceType();
    }

    public String getSelector() {
        Signature method = getProcessSignature();
        return (method == null) ? null : method.getSelector();
    }

    public boolean isExecutable() {
        if (getServiceType() != null)
            return true;
        else
            return false;
    }

    public List<Mogram> getAllMograms() {
        List<Mogram> exs = new ArrayList<Mogram>();
        getMograms(exs);
        return exs;
    }

    public String contextToString() {
        return "";
    }

    public int getExceptionCount() {
        return controlContext.getExceptions().size();
    }

    @Override
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

    public ControlContext getMogramStrategy() {
        return controlContext;
    }

    public Context getContext() throws ContextException {
        return getDataContext();
    }

    public Context getContext(String componentExertionName)
            throws ContextException {
        Exertion component = (Exertion)getMogram(componentExertionName);
        if (component != null)
            return ((Exertion)getMogram(componentExertionName)).getContext();
        else
            return null;
    }

    public Context getControlContext(String componentExertionName) {
        Exertion component = (Exertion)getMogram(componentExertionName);
        if (component != null)
            return ((Exertion)getMogram(componentExertionName)).getControlContext();
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
        if (dataContext.getMogramStrategy().getOutConnector() != null) {
            dataContext.updateContextWith(dataContext.getMogramStrategy().getOutConnector());
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
                        putValue(((Entry) e).path(), ((Entry) e).value());
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
        Context xrtScope = getScope();
        if (xrtScope != null && xrtScope.size() > 0) {
            try {
                getDataContext().updateEntries(xrtScope);
            } catch (ContextException e) {
                throw new SetterException(e);
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
        ReturnPath returnPath = (ReturnPath)getDataContext().getReturnPath();
        if (returnPath != null) {
            if (returnPath.path == null || returnPath.path.equals(Signature.SELF))
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
        info.append(", exertion ID=").append(mogramId);
        String time = getControlContext().getExecTime();
        if (time != null && time.length() > 0) {
            info.append("\n  Execution Time = " + time);
        }
        return info.toString();
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
                ((NetSignature) sig).setSelector("exert");
                sig.setProviderName(ANY);
                sig.setType(Signature.Type.PROC);
                getControlContext().setAccessType(access);
            } else if (Access.PUSH == access
                    && !getProcessSignature().getServiceType()
                    .isAssignableFrom(Jobber.class)) {
                if (sig.getServiceType().isAssignableFrom(Spacer.class)) {
                    sig.setServiceType(Jobber.class);
                    ((NetSignature) sig).setSelector("exert");
                    sig.setProviderName(ANY);
                    sig.setType(Signature.Type.PROC);
                    getControlContext().setAccessType(access);
                }
            }
        }
        return sig;
    }

    public void reset(int state) {
        status = state;
    }

    public Mogram clearScope() throws MogramException {
        getDataContext().clearScope();
        return this;
    }

    public Object getValue(Arg... entries) throws EvaluationException,
            RemoteException {
       if (provider == null) {
           return evaluate(entries);
       } else {
           return ((Evaluation)provider).getValue(entries);
       }
    }
    /*
     * (non-Javadoc)
     *
     * @see sorcer.service.Evaluation#getValue()
     */
    public Object evaluate(Arg... entries) throws EvaluationException,
            RemoteException {
        Context cxt = null;
        try {
            substitute(entries);
            Exertion evaluatedExertion = exert(entries);
            ReturnPath rp = (ReturnPath)evaluatedExertion.getDataContext()
                    .getReturnPath();
            if (evaluatedExertion instanceof Job) {
                cxt = ((Job) evaluatedExertion).getJobContext();
            } else {
                cxt = evaluatedExertion.getContext();
            }

            if (rp != null) {
                if (rp.path == null)
                    return cxt;
                else if (rp.path.equals(Signature.SELF))
                    return this;
                else if (rp.path != null) {
                    cxt.setReturnValue(cxt.getValue(rp.path));
                    Context out = null;
                    if (rp.outPaths != null && rp.outPaths.length > 0) {
                        out = ((ServiceContext)cxt).getDirectionalSubcontext(rp.outPaths);
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
        ServiceContext cxt = null;
        if (isJob()) {
            cxt = (ServiceContext)((Job) this).getJobContext();
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

    @Override
    public void appendTrace(String info) {
        getControlContext().appendTrace(info);
    }

    @Override
    public Object exec(Arg... args) throws MogramException, RemoteException {
        Context cxt = Arg.getContext(args);
        if (cxt != null) {
              dataContext = (ServiceContext) cxt;
              return value(this, args);
        }
        return null;
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
                .append("\tExertion ID:          " + mogramId + "\n")
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
                .append("\tDescription:          " + description + "\n")
                .append("\tProject:              " + projectName + "\n")
                .append("\tGood Until Date:      " + goodUntilDate + "\n")
                .append("\tAccess Class:         " + accessClass + "\n")
                .append("\tIs Export Controlled: " + isExportControlled + "\n")
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
