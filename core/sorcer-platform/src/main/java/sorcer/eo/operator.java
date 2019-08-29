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
package sorcer.eo;

import groovy.lang.Closure;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.Operator;
import sorcer.co.operator.DataSlot;
import sorcer.co.tuple.*;
import sorcer.core.SorcerConstants;
import sorcer.core.context.*;
import sorcer.core.context.model.DataContext;
import sorcer.core.context.model.EntModel;
import sorcer.core.context.model.QueueStrategy;
import sorcer.core.context.model.ent.*;
import sorcer.core.context.model.srv.Srv;
import sorcer.core.context.model.srv.SrvModel;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.dispatch.SortingException;
import sorcer.core.dispatch.SrvModelAutoDeps;
import sorcer.core.exertion.*;
import sorcer.core.invoker.Activator;
import sorcer.core.invoker.IncrementInvoker;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.core.plexus.*;
import sorcer.core.provider.*;
import sorcer.core.provider.exerter.Binder;
import sorcer.core.provider.rendezvous.ServiceConcatenator;
import sorcer.core.provider.rendezvous.ServiceModeler;
import sorcer.core.requestor.ServiceConsumer;
import sorcer.core.service.Projection;
import sorcer.core.signature.*;
import sorcer.netlet.ServiceScripter;
import sorcer.service.*;
import sorcer.service.DisciplineFidelity;
import sorcer.service.Signature.*;
import sorcer.service.Strategy.*;
import sorcer.service.modeling.*;
import sorcer.util.*;
import sorcer.util.url.sos.SdbUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.Collections;

import static sorcer.co.operator.path;
import static sorcer.co.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.ent.operator.srv;

/**
 * Operators defined for the Service Modeling Language (SML).
 *
 * @author Mike Sobolewski
 */
public class
operator extends Operator {

    protected static int count = 0;

    protected static final Logger logger = LoggerFactory.getLogger(operator.class.getName());

    public static void requestTime(Subroutine exertion) {
        ((ServiceRoutine) exertion).setExecTimeRequested(true);
    }

    public static ServiceConsumer consumer(Class consumerType, String... args) {
        return  new ServiceConsumer(consumerType, args);
    }

    public static ServiceConsumer consumer(Class consumerType, Context inContext, Arg... args) {
        return  new ServiceConsumer(consumerType, inContext, args);
    }

    public static Evaluation neg(Evaluation evaluation) {
        evaluation.setNegative(true);
        return evaluation;
    }

    public static Object revalue(Context context, String path,
                                 Arg... entries) throws ContextException {
        Object obj = sorcer.mo.operator.value(context, path, entries);
        if (obj instanceof Evaluation) {
            try {
                obj = ((Evaluation) obj).evaluate(entries);
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        }
        return obj;
    }

    public static Object revalue(Object object, String path,
                                 Arg... entries) throws ContextException {
        Object obj = null;
        if (object instanceof Entry) {
            obj = ((Entry) object).getValue(entries);
        } else if (object instanceof Context) {
            obj = sorcer.mo.operator.value((Context) object, path, entries);
            obj = sorcer.mo.operator.value((Context) obj, entries);
        } else {
            obj = object;
        }
        return obj;
    }

    public static Object revalue(Object object, Arg... entries)
        throws EvaluationException {
        Object obj = null;
        try {
            if (object instanceof Entry) {
                obj =  ((Entry) object).getValue(entries);
            } else if (object instanceof Context) {
                obj = sorcer.mo.operator.value((Context) object, entries);
            }
        } catch (ContextException e) {
            throw new EvaluationException(e);
        }
        if (obj != null) {
            return obj;
        } else {
            return object;
        }
    }

    public static String attPath(String... attributes) {
        if (attributes.length == 0)
            return null;
        if (attributes.length > 1) {
            StringBuilder spr = new StringBuilder();
            for (int i = 0; i < attributes.length - 1; i++) {
                spr.append(attributes[i]).append(SorcerConstants.CPS);
            }
            spr.append(attributes[attributes.length - 1]);
            return spr.toString();
        }
        return attributes[0];
    }

    public static <T> Complement<T> subject(String path, T value)
        throws SignatureException {
        return new Complement<T>(path, value);
    }

    public static void add(Subroutine exertion, Identifiable... entries)
        throws ContextException, RemoteException {
        sorcer.mo.operator.add(exertion.getContext(), entries);
    }

    public static void put(Subroutine exertion, Identifiable... entries)
        throws ContextException, RemoteException {
        put(exertion.getContext(), entries);
    }

    public static Subroutine setContext(Subroutine exertion, Context context) {
        exertion.setContext(context);
        return exertion;
    }

    public static Prc setContext(Prc procedure, Context context) {
        procedure.setScope(context);
        return procedure;
    }

    public static <T> Evaluator<T> setContext(Evaluator<T> evaluator, Context context) {
        if (evaluator.getContextReturn() == null) {
            evaluator.setContextReturn(new Context.Return());
        }
        evaluator.getContextReturn().setDataContext(context);
        return evaluator;
    }

    public static Signature setContext(Signature signature, Context context) {
        if (signature.getContextReturn() == null) {
            signature.setContextReturn(new Context.Return());
        }
        signature.getContextReturn().setDataContext(context);
        return signature;
    }

    public static ControlContext control(Subroutine exertion)
        throws ContextException {
        return ((ServiceRoutine) exertion).getControlContext();
    }

    public static ControlContext control(Subroutine exertion, String childName)
        throws ContextException {
        return (ControlContext) ((Subroutine) exertion.getMogram(childName)).getControlContext();
    }

    public static Context ccxt(Subroutine exertion) throws ContextException {
        return ((ServiceRoutine) exertion).getControlContext();
    }

    public static Context upcxt(Subroutine mogram) throws ContextException {
        return snapshot(mogram);
    }

    public static Context selfContext(Mogram mogram) throws ContextException {
        return mogram.getDataContext();
    }

    public static Context dataContext(Mogram mogram) throws ContextException {
        return mogram.getDataContext();
    }

    public static Context upcontext(Mogram mogram) throws ContextException {
        if (mogram instanceof Transroutine)
            return mogram.getContext();
        else
            return  mogram.getDataContext();
    }

    public static Context snapshot(Subroutine mogram) throws ContextException {
        return upcontext(mogram);
    }

    public static Context taskContext(String path, Subroutine service) throws ContextException {
        if (service instanceof ServiceRoutine) {
            return ((Transroutine) service).getComponentContext(path);
        } else
            throw new ContextException("Service not an exertion: " + service);
    }

    public static Context subcontext(Context context, List<Path> paths) throws ContextException {
        return context.getDirectionalSubcontext(paths);
    }

    public static Context scope(Object... entries) throws ContextException {
        Object[] args = new Object[entries.length + 1];
        System.arraycopy(entries, 0, args, 1, entries.length);
        args[0] = Context.Type.SCOPE;
        return context(args);
    }

    public static Context cxt(Object... entries) throws ContextException {
        return context(entries);
    }
    public static Context data(Object... entries) throws ContextException {
        for (Object obj : entries) {
            if (!(obj instanceof String) || !(obj instanceof Function && ((Function)obj).getType().equals(Functionality.Type.VAL))) {
                throw new ContextException("Not execute entry " + obj.toString());
            }
        }
        return context(entries);
    }

    public static Context<Float> weights(Entry... entries) throws ContextException {
        return context((Object[])entries);
    }

    public static ServiceContext context(Object... entries) throws ContextException {
        // do not create a context from Context, jut return
        if (entries == null || entries.length == 0) {
            return new ServiceContext();
        } else if (entries.length == 1 && entries[0] instanceof Context) {
            return (ServiceContext) entries[0];
        } else if (entries.length == 1 && entries[0] instanceof Mogram) {
            return (ServiceContext)((Mogram)entries[0]).getContext();
        } else if (entries.length == 1 && entries[0] instanceof List) {
            return (ServiceContext)contextFromList((List) entries[0]);
        } else if (entries.length == 1 && entries[0] instanceof Row) {
            return rowContext((Row) entries[0]);
        } else if (entries.length == 1 && entries[0] instanceof Args) {
            return argsContext((Args)entries[0]);
        }

        ServiceContext cxt = null;
        List<ServiceContext> cxts = new ArrayList<ServiceContext>();;
        List<MapContext> connList = new ArrayList<MapContext>();
        Strategy.Access accessType = null;
        Strategy.Flow flowType = null;
        Strategy.FidelityManagement fm = null;
        FidelityManager fiManager = null;
        Projection projection = null;
        if (entries[0] instanceof Subroutine) {
            Subroutine xrt = (Subroutine) entries[0];
            if (entries.length >= 2 && entries[1] instanceof String)
                xrt = (Subroutine) (xrt).getComponentMogram((String) entries[1]);
            return (ServiceContext) xrt.getDataContext();
        } else if (entries[0] instanceof Link) {
            return (ServiceContext) ((Link) entries[0]).getContext();
        } else if (entries.length == 1 && entries[0] instanceof String) {
            return new PositionalContext((String) entries[0]);
        } else if (entries.length == 2 && entries[0] instanceof String
            && entries[1] instanceof Subroutine) {
            return (ServiceContext) ((Transroutine) entries[1]).getComponentMogram(
                (String) entries[0]).getContext();
        } else if (entries[0] instanceof Context && entries[1] instanceof List) {
            return (ServiceContext) ((ServiceContext) entries[0]).getDirectionalSubcontext((List)entries[1]);
        } else if (entries[0] instanceof Context) {
            cxt = (ServiceContext) entries[0];
        } else if (Context.class.isAssignableFrom(entries[0].getClass())) {
            try {
                cxt = (ServiceContext) ((Class) entries[0]).newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new ContextException(e);
            }
        } else {
            cxt = (ServiceContext) getPersistedContext(entries);
            if (cxt != null) return cxt;
        }
        String name = getUnknown();
        List<Entry> entryList = new ArrayList();
        List<Slot> slotList = new ArrayList();
        List<Function> funcEntryList = new ArrayList();
        List<Context.Type> types = new ArrayList();
        List<EntryList> entryLists = new ArrayList();
        List<ExecDependency> depList = new ArrayList();
        Complement subject = null;
        Context.Return contextReturn = null;
        ExecPath execPath = null;
        Args cxtArgs = null;
        ParameterTypes parTypes = null;
        PathResponse response = null;
        QueueStrategy modelStrategy = null;
        Signature sig = null;
        Class customContextClass = null;
        Context.Out outPaths = null;
        Context.In inPaths = null;
        Paths paths = null;
        boolean autoDeps = true;
        for (Object o : entries) {
            if (o instanceof Complement) {
                subject = (Complement) o;
            } else if (o instanceof Args) {
                cxtArgs = (Args) o;
            } else if (o instanceof ParameterTypes)  {
                parTypes = (ParameterTypes) o;
            } else if (o instanceof PathResponse) {
                response = (PathResponse) o;
            } else if (o instanceof Context.Return) {
                contextReturn = (Context.Return) o;
            } else if (o instanceof ExecPath) {
                execPath = (ExecPath) o;
            } else if (o instanceof Function) {
                funcEntryList.add((Function) o);
            } else if (o instanceof Entry) {
                entryList.add((Entry) o);
            } else if (o.getClass() == Slot.class) {
                slotList.add((Slot) o);
            } else if (o instanceof Context.Type) {
                types.add((Context.Type) o);
            } else if (o instanceof String) {
                name = (String) o;
            } else if (o instanceof EntryList) {
                entryLists.add((EntryList) o);
            } else if (o instanceof MapContext) {
                connList.add((MapContext) o);
            } else if (o instanceof ExecDependency) {
                depList.add((ExecDependency) o);
            } else if (o instanceof Signature) {
                sig = (Signature) o;
            } else if (o instanceof Class) {
                customContextClass = (Class) o;
            } else if (o instanceof Strategy.Access) {
                accessType = (Strategy.Access)o;
            } else if (o instanceof Strategy.Flow) {
                flowType = (Strategy.Flow)o;
            } else if (o instanceof Strategy.FidelityManagement) {
                fm = (Strategy.FidelityManagement)o;
            } else if (o instanceof FidelityManager) {
                fiManager = ((FidelityManager)o);
            } else if (o instanceof Projection) {
                projection = ((Projection)o);
            } else if (Strategy.Flow.EXPLICIT.equals(o)) {
                autoDeps = false;
            } else if (o instanceof Context.Out) {
                outPaths = (Context.Out)o;
            } else if (o instanceof Context.In) {
                inPaths = (Context.In)o;
            } else if (o instanceof Paths) {
                paths = (Paths)o;
            } else if (o instanceof Context) {
                cxts.add((ServiceContext) o);
            }
        }

        if (cxt == null) {
            if (types.contains(Context.Type.ARRAY)) {
                if (subject != null)
                    cxt = new ArrayContext(name, subject.getName(), subject.getImpl());
                else
                    cxt = new ArrayContext(name);
            } else if (types.contains(Context.Type.LIST)) {
                if (subject != null)
                    cxt = new ListContext(name, subject.getName(), subject.getImpl());
                else
                    cxt = new ListContext(name);
            } else if (types.contains(Context.Type.SCOPE)) {
                cxt = new ScopeContext(name);
            } else if (types.contains(Context.Type.SHARED)
                && types.contains(Context.Type.INDEXED)) {
                cxt = new SharedIndexedContext(name);
            } else if (types.contains(Context.Type.SHARED)) {
                cxt = new SharedAssociativeContext(name);
            } else if (types.contains(Context.Type.ASSOCIATIVE)) {
                if (subject != null)
                    cxt = new DataContext(name, subject.getName(), subject.getImpl());
                else
                    cxt = new DataContext(name);
            } else if (customContextClass != null) {
                try {
                    cxt = (ServiceContext) customContextClass.newInstance();
                } catch (Exception e) {
                    throw new ContextException(e);
                }
                if (subject != null)
                    cxt.setSubject(subject.getName(), subject.getImpl());
                else
                    cxt.setName(name);
            } else {
                if (subject != null) {
                    cxt = new DataContext(name, subject.getName(),
                        subject.getImpl());
                } else {
                    cxt = new DataContext(name);
                }
            }
        } else if (cxts.size() > 1){
            cxt = new ServiceContext("Bag Context");
            for (ServiceContext context : cxts) {
                cxt.append(context);
            }
        }

        if (cxt instanceof PositionalContext) {
            PositionalContext pcxt = (PositionalContext) cxt;
            if (entryList.size() > 0) {
                popultePositionalContext(pcxt, entryList);
            }
            if (slotList.size() > 0) {
                usePositionalSlots(pcxt, slotList);
            }
        } else {
            if (entryList.size() > 0) {
                populteContext(cxt, entryList);
            }
            if (slotList.size() > 0) {
                useSlots(cxt, slotList);
            }
        }
        if (funcEntryList.size() > 0) {
            for (Entry p : funcEntryList) {
                cxt.putValue(p.getName(), p);
                if (p.getImpl()  instanceof Evaluator) {
                    // preserve invokeContext of the invoker
                    if (((Evaluator)p.getImpl()).getScope() != null
                        && ((Evaluator) p.getImpl()).getScope().size() > 0) {
                        ((Evaluator) p.getImpl()).getScope().setScope(cxt);
                    } else {
                        if (p.getImpl() instanceof ServiceInvoker) {
                            if (((ServiceInvoker) p.getImpl()).getInvokeContext() == null) {
                                ((ServiceInvoker) p.getImpl()).setInvokeContext(cxt);
                            } else {
                                ((ServiceInvoker) p.getImpl()).setScope(cxt);
                            }
                        } else {
                            ((Evaluator) p.getImpl()).setScope(cxt);
                        }
                    }
                } else if (p.getImpl()  instanceof Entry) {
                    ((Entry) p.getImpl()).initScope(context(entryList));
                }
            }
        }
        if (contextReturn != null)
            cxt.setContextReturn(contextReturn);
        if (execPath != null)
            cxt.setExecPath(execPath);
        if (cxtArgs != null) {
            if (cxtArgs.getName() != null) {
                cxt.setArgsPath(cxtArgs.getName());
            } else {
                cxt.setArgsPath(Context.PARAMETER_VALUES);
            }
            cxt.setArgs(cxtArgs.args);
        }
        if (parTypes != null) {
            if (parTypes.getName() != null) {
                cxt.setParameterTypesPath(parTypes
                    .getName());
            } else {
                cxt.setParameterTypesPath(Context.PARAMETER_TYPES);
            }
            cxt.setParameterTypes(parTypes.parameterTypes);
        }
        if (response != null) {
            if (response.getName() != null) {
                cxt.getMogramStrategy().getResponsePaths().add(new Path(response.getName()));
            }
            cxt.getMogramStrategy().setResult(response.getName(), response.target);
        }
        if (entryLists.size() > 0) {
            cxt.setEntryLists(entryLists);
        }
        if (connList.size() > 0) {
            for (MapContext conn : connList) {
                if (conn.direction == MapContext.Direction.IN) {
                    cxt.getMogramStrategy().setInConnector(conn);
                } else {
                    cxt.getMogramStrategy().setOutConnector(conn);
                }
            }
        }
        if (depList.size() > 0) {
            Map<String, List<ExecDependency>> dm = ((ServiceContext) cxt).getMogramStrategy().getDependentPaths();
            String path = null;
            List<ExecDependency> dependentPaths = null;
            for (ExecDependency e : depList) {
                path = e.getName();
                if (dm.get(path) != null) {
                    ((List)dm.get(path)).add(e);
                } else {
                    List<ExecDependency> del = new ArrayList();
                    del.add(e);
                    dm.put(path, del);
                }
            }
        }
        if (outPaths instanceof Context.Out) {
            if (cxt.getContextReturn() == null) {
                cxt.setContextReturn(new Context.Return(outPaths));
            } else {
                cxt.getContextReturn().outPaths = outPaths;
            }
        }
        if (inPaths instanceof Context.In) {
            if (cxt.getContextReturn() == null) {
                cxt.setContextReturn(new Context.Return(inPaths));
            } else {
                cxt.getContextReturn().inPaths = new Context.In(paths);
            }
        }

        if (paths != null) {
            if (cxt.getContextReturn() == null) {
                cxt.setContextReturn(new Context.Return(paths.toPathArray()));
            } else {
                cxt.getContextReturn().inPaths = new Context.In(paths);
            }
        }

        if (accessType != null)
            cxt.getMogramStrategy().setAccessType(accessType);
        if (flowType != null)
            cxt.getMogramStrategy().setFlowType(flowType);
        try {
            if (sig != null)
                cxt.setSubject(sig.getSelector(), sig.getServiceType());
            if (cxt instanceof SrvModel && autoDeps) {
                cxt = new SrvModelAutoDeps((SrvModel) cxt).get();
            }
        } catch (SignatureException | SortingException e) {
            throw new ContextException(e);
        }

        if (cxt.getFidelityManager() == null && fm == Strategy.FidelityManagement.YES) {
            cxt.setFidelityManager(new FidelityManager(cxt));
            setupFiManager(cxt);
        } else if (fiManager != null) {
            cxt.setFidelityManager(fiManager);
            setupFiManager(cxt);
        } else if (cxt.getFidelityManager() != null) {
            setupFiManager(cxt);
        }
        if (projection != null)
            cxt.setProjection(projection);

        return cxt;
    }

    public static Context contextFromList(List<Entry> entries) throws ContextException {
        ServiceContext cxt = new ServiceContext();
        for (Object i : entries) {
            cxt.put(((Entry)i).getName().toString(), ((Entry)i).getValue());
        }
        return cxt;
    }

    private static FidelityManager setupFiManager(Context cxt) throws ContextException {
        if (cxt.getFidelityManager() == null) {
            ((ServiceContext)cxt).setFidelityManager(new FidelityManager(cxt));
        }
        try {
            Map<String, ServiceFidelity> fiMap =
                fiMap = cxt.getFidelityManager().getFidelities();

            Map.Entry<String,Object> e;
            Object val = null;
            Iterator<Map.Entry<String,Object>> i = ((ServiceContext)cxt).entryIterator();
            while(i.hasNext()) {
                e = i.next();
                val = e.getValue();
                if (val instanceof Srv && ((Srv)val).asis() instanceof ServiceFidelity) {
                    fiMap.put(e.getKey(), (ServiceFidelity)((Srv)val).asis());
                }
            }
            if (((ServiceContext)cxt).getProjection() != null)
                cxt.reconfigure(((ServiceContext)cxt).getProjection().toFidelityArray());
        } catch (Exception ex) {
            throw new ContextException(ex);
        }

        return (FidelityManager)cxt.getFidelityManager();
    }

    private static Context getPersistedContext(Object... entries) throws ContextException {
        ServiceContext cxt = null;
        try {
            if (entries.length == 1 && SdbUtil.isSosURL(entries[0]))
                cxt = (ServiceContext) ((URL) entries[0]).getContent();
            else if (entries.length == 2 && entries[0] instanceof String && SdbUtil.isSosURL(entries[1])) {
                cxt = (ServiceContext) ((URL) entries[1]).getContent();
                cxt.setName((String) entries[0]);
            }
        } catch (IOException e) {
            throw new ContextException(e);
        }
        return cxt;
    }


    protected static void useSlots(ServiceContext pcxt,
                                   List<Slot> slotList) throws ContextException {
        for (int i = 0; i < slotList.size(); i++) {
            Slot ent = slotList.get(i);
            pcxt.put(ent.getName(), ent.getOut());
        }
    }

    protected static void usePositionalSlots(PositionalContext pcxt,
                                             List<Slot> slotList) throws ContextException {
        int k = pcxt.getIndex();
        for (int i = 0; i < slotList.size(); i++) {
            Slot ent = slotList.get(i);
            pcxt.putValueAt(ent.getName(), ent.getOut(), k + i + 1);
        }
    }

    protected static void popultePositionalContext(PositionalContext pcxt,
                                                   List<Entry> entryList) throws ContextException {
        for (int i = 0; i < entryList.size(); i++) {
            Entry ent = entryList.get(i);
            if (ent instanceof Srv) {
                if (ent.asis() instanceof Scopable) {
                    if (((Scopable) ent.getImpl()).getScope() != null)
                        ((Scopable) ent.getImpl()).getScope().setScope(pcxt);
                    else
                        ((Scopable) ent.getImpl()).setScope(pcxt);
                }
                pcxt.putInoutValueAt(ent.getName(), ent, i + 1);
            } else if (ent instanceof InputValue || ent.getType() == Functionality.Type.INPUT) {
                Object par = ent.getImpl();
                if (par instanceof Scopable) {
                    if (ent.getImpl() instanceof ServiceInvoker) {
                        ((ServiceInvoker)ent.getImpl()).setInvokeContext(pcxt);
                    } else {
                        ((Scopable) par).setScope(pcxt);
                    }
                }
                if (ent.isPersistent()) {
                    setProc(pcxt, ent, i);
                } else {
                    if (ent.getMultiFi() != null) {
                        pcxt.putInValueAt(ent.getName(), ent, i + 1);
                    } else {
                        pcxt.putInValueAt(ent.getName(), ent.getImpl(), i + 1);
                    }
                }
            } else if (ent instanceof OutputValue || ent.getType() == Functionality.Type.OUTPUT) {
                if (ent.isPersistent()) {
                    setProc(pcxt, ent, i);
                } else {
                    if (ent.getValClass() != null) {
                        pcxt.putOutValueAt(ent.getName(), ent.getOut(), ent.getValClass(), i + 1);
                    } else if (ent.getMultiFi() != null) {
                        pcxt.putOutValueAt(ent.getName(), ent, i + 1);
                    } else {
                        pcxt.putOutValueAt(ent.getName(), ent.getImpl(), i + 1);
                        if (ent.getImpl() instanceof ServiceInvoker) {
                            ((ServiceInvoker)ent.getImpl()).setInvokeContext(pcxt);
                        }
                    }
                }
            } else if (ent instanceof InoutValue || ent.getType() == Functionality.Type.INOUT) {
                if (ent.isPersistent()) {
                    setProc(pcxt, ent, i);
                } else {
                    if (ent.getMultiFi() != null) {
                        pcxt.putInoutValueAt(ent.getName(), ent, i + 1);
                    } else {
                        pcxt.putInoutValueAt(ent.getName(), ent.getImpl(), i + 1);

                    }
                }
            } else if (ent instanceof Entry) {
                if (ent instanceof Prc && ((Prc)ent).getImpl() instanceof Invocation) {
                    ((ServiceInvoker)((Prc)ent).getImpl()).setInvokeContext(pcxt);
                }
                if (ent.isPersistent()) {
                    setProc(pcxt, entryList.get(i), i);
                } else {
                    if  (ent.getMultiFi() != null) {
                        pcxt.putValueAt(ent.getName(), ent, i + 1);
                    } else {
                        pcxt.putValueAt(ent.getName(), ent.getImpl(), i + 1);
                        if (ent.getImpl() instanceof Scopable) {
                            ((Scopable) ent.getImpl()).setScope(pcxt);
                        }
                    }
                }

            } else if ((Slot)ent instanceof DataSlot) {
                pcxt.putValueAt(Context.DSD_PATH, ent.getImpl(), i + 1);
            }
        }
    }

    public static void populteContext(Context cxt,
                                      List<Entry> entryList) throws ContextException {
        for (int i = 0; i < entryList.size(); i++) {
            Entry ent = entryList.get(i);
            if (entryList.get(i) instanceof InputValue || ent.getType().equals(Functionality.Type.INPUT)) {
                Object val = null;
                val = entryList.get(i).asis();
                if (val instanceof Incrementor &&
                    ((IncrementInvoker)val).getTarget() == null) {
                    ((IncrementInvoker)val).setInvokeContext(cxt);
                }
                if (ent.isPersistent()) {
                    setProc(cxt, ent);
                } else {
                    cxt.putInValue(ent.getName(), ent.getImpl());
                }
            } else if (ent instanceof OutputValue || ent.getType().equals(Functionality.Type.OUTPUT)) {
                if (ent.isPersistent()) {
                    setProc(cxt, ent);
                } else {
                    cxt.putOutValue(ent.getName(), ent.getImpl());
                }
            } else if (entryList.get(i) instanceof InoutValue || ent.getType().equals(Functionality.Type.INOUT)) {
                if (ent.isPersistent()) {
                    setProc(cxt, ent);
                } else {
                    cxt.putInoutValue(ent.getName(), ent.getImpl());
                }
            } else if (entryList.get(i) instanceof Function) {
                if (ent.isPersistent()) {
                    setProc(cxt, ent);
                } else {
                    cxt.putValue(ent.getName(), ent.getImpl());
                }
            } else if (entryList.get(i) instanceof Entry) {
                cxt.putValue(entryList.get(i).getName(), ent.getOut());
            }
        }
    }

    public Context rm(Model model, String path) {
        return remove(model, path);
    }

    public Context remove(Model model, String path) {
        ServiceContext context = (ServiceContext) model;
        context.getData().remove(path);
        return context;
    }

    public static Context put(Context context, String path, Object value)
        throws ContextException {
        try {
            Object val = context.get(path);
            if (val instanceof Prc && ((Prc)val).isPersistent())
                val = ((Prc)val).asis();
            if (SdbUtil.isSosURL(val)) {
                SdbUtil.update((URL) val, value);
            } else {
                context.putValue(path, value);
            }
        } catch (Exception e) {
            throw new ContextException(e);
        }
        return context;
    }

    public static Context set(Context context, Identifiable... objects)
        throws RemoteException, ContextException {
        for (Identifiable obj : objects) {
            // just replace the eval
            ((ServiceContext)context).put(obj.getName(), obj);
        }
        return context;
    }

    public static Context put(Context context, Identifiable... objects)
        throws RemoteException, ContextException {
        for (Identifiable i : objects) {
            // just replace the eval
            if (context.containsPath(i.getName())) {
                context.putValue(i.getName(), i);
                continue;
            }

            if (context instanceof PositionalContext) {
                PositionalContext pc = (PositionalContext) context;
                if (i instanceof InputValue) {
                    pc.putInValueAt(i.getName(), i, pc.getTally() + 1);
                } else if (i instanceof OutputValue) {
                    pc.putOutValueAt(i.getName(), i, pc.getTally() + 1);
                } else if (i instanceof InoutValue) {
                    pc.putInoutValueAt(i.getName(), i, pc.getTally() + 1);
                } else {
                    pc.putValueAt(i.getName(), i, pc.getTally() + 1);
                }
            } else if (context instanceof ServiceContext) {
                if (i instanceof InputValue) {
                    context.putInValue(i.getName(), i);
                } else if (i instanceof OutputValue) {
                    context.putOutValue(i.getName(), i);
                } else if (i instanceof InoutValue) {
                    context.putInoutValue(i.getName(), i);
                } else {
                    context.putValue(i.getName(), i);
                }
            } else {
                context.putValue(i.getName(), i);
            }
            if (i instanceof Entry) {
                Entry e = (Entry) i;
                if (e.isAnnotated()) {
                    context.mark(e.getName(), e.annotation().toString());
                }
            }

            if (i instanceof Evaluator) {
                // preserve invokeContext of the invoker
                if (((Evaluator)i).getScope() != null
                    && ((Evaluator) i).getScope().size() > 0) {
                    ((Evaluator) i).getScope().setScope(context);
                } else {
                    if (i instanceof ServiceInvoker) {
                        if (((ServiceInvoker)i).getInvokeContext() == null) {
                            ((ServiceInvoker)i).setInvokeContext(context);
                        } else {
                            ((ServiceInvoker) i).setScope(context);
                        }
                    } else {
                        ((Evaluator)i).setScope(context);
                    }
                }
            }
        }

        return context;
    }

    protected static void setProc(PositionalContext pcxt, Entry entry, int i)
        throws ContextException {
        Prc p = new Prc(entry.getName(), entry.getImpl());
        p.setPersistent(true);
        if (entry instanceof InputValue)
            pcxt.putInValueAt(entry.getName(), p, i + 1);
        else if (entry instanceof OutputValue)
            pcxt.putOutValueAt(entry.getName(), p, i + 1);
        else if (entry instanceof InoutValue)
            pcxt.putInoutValueAt(entry.getName(), p, i + 1);
        else
            pcxt.putValueAt(entry.getName(), p, i + 1);
    }

    protected static void setProc(Context cxt, Entry entry)
        throws ContextException {
        Prc p = new Prc(entry.getName(), entry.getImpl());
        p.setPersistent(true);
        if (entry instanceof InputValue)
            cxt.putInValue(entry.getName(), p);
        else if (entry instanceof OutputValue)
            cxt.putOutValue(entry.getName(), p);
        else if (entry instanceof InoutValue)
            cxt.putInoutValue(entry.getName(), p);
        else
            cxt.putValue(entry.getName(), p);
    }

    public static List<String> names(List<? extends Identifiable> list) {
        List<String> names = new ArrayList<String>(list.size());
        for (Identifiable i : list) {
            names.add(i.getName());
        }
        return names;
    }

    public static List<String> names(Identifiable... array) {
        List<String> names = new ArrayList<String>(array.length);
        for (Identifiable i : array) {
            names.add(i.getName());
        }
        return names;
    }

    public static List<Function> attributes(Function... entries) {
        List<Function> el = new ArrayList<Function>(entries.length);
        for (Function e : entries)
            el.add(e);
        return el;
    }

    /**
     * Returns the Evaluation with a realized substitution for its arguments.
     *
     * @param model
     * @param entries
     * @return an evaluation with a realized substitution
     * @throws EvaluationException
     * @throws RemoteException
     */
    public static Object bind(Object model, Arg... entries)
        throws ContextException {
        if (model instanceof Substitutable) {
            Binder binder = new Binder((Mogram) model);
            binder.bind(entries);
        }
        return model;
    }

    public static Class type(Signature signature) throws SignatureException {
        return signature.getServiceType();
    }


    public static ContextSelector selector(String componentName, List<Path> paths) {
        ContextSelector cs = new ContextSelector(Path.getNameList(paths));
        cs.setComponentName(componentName);
        return cs;
    }

    public static ContextSelector selector(String... paths) {
        List<String> pathList = Arrays.asList(paths);
        return new ContextSelector(pathList);
    }

    public static Selfable self(Selfable selfable, boolean state) {
        if (selfable instanceof Selfable) {
            selfable.setSelf(state);
        }
        return selfable;
    }

    public static Signature sig(String name, String operation, Class serviceType)
        throws SignatureException {
        ServiceSignature signature = sig(operation, serviceType, new Object[]{});
        signature.setName(name);
        return signature;
    }

    public static SignatureDeployer deployer(String operation, Class serviceType)
        throws SignatureException {
        ObjectSignature builder = (ObjectSignature) sig(operation, serviceType, new Object[]{});
        SignatureDeployer dpl = new SignatureDeployer(builder);
        return dpl;
    }

    public static SignatureDeployer deployer(Signature... builders)
        throws SignatureException {
        return new SignatureDeployer(builders);
    }

    public static MultiFiSignature mfSig(Signature... signatures)
            throws SignatureException {
        MultiFiSignature mfi = new MultiFiSignature(signatures);
        return mfi;
    }

    public static ServiceSignature sig(String operation, Class serviceType)
        throws SignatureException {
        return sig(operation, serviceType, new Object[]{});
    }

    public static Signature trgSig(String operation, Class serviceType, Class target)
        throws SignatureException {
        Signature ts = sig(operation, serviceType, new Object[]{});
        try {
            Object provider = target.newInstance();
            if (provider instanceof ServiceExerter) {
                Object bean = serviceType.newInstance();
                ((ServiceExerter)provider).setBean(bean);
            }
            ((ObjectSignature)ts).setTarget(provider);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new SignatureException(e);
        }
        return ts;
    }

    public static Signature sig(Class serviceType, String initSelector) throws SignatureException {
        try {
            Method selectorMethod = serviceType.getDeclaredMethod(initSelector, Context.class);
            if (!Modifier.isStatic(selectorMethod.getModifiers()))
                return sig(initSelector, serviceType);
        } catch (NoSuchMethodException e) {
            // skip
        }
        return sig(initSelector, serviceType, initSelector);
    }

    public static Signature sig(String operation, Class serviceType,
                                String initSelector) throws SignatureException {
        try {
            return new ObjectSignature(operation, serviceType, initSelector,
                (Class<?>[]) null, (Object[]) null);
        } catch (Exception e) {
            throw new SignatureException(e);
        }
    }

    public static String selector(Signature signature) {
        return signature.getSelector();
    }

    public static Signature sig(String operation, Object provider, Object... args) throws SignatureException {
        ObjectSignature sig = new ObjectSignature();
        sig.setName(operation);
        sig.setSelector(operation);
        sig.setTarget(provider);
        if (args.length > 0) {
            for (Object o : args) {
                if (o instanceof Type) {
                    sig.setType((Type) o);
                } else if (o instanceof Operating) {
                    sig.setActive((Operating) o);
                } else if (o instanceof Strategy.Shell) {
                    sig.setShellRemote((Strategy.Shell) o);
                } else if (o instanceof Context.Return) {
                    sig.setContextReturn((Context.Return) o);
                } else if (o instanceof ServiceDeployment) {
                    sig.setProvisionable(true);
                    sig.setDeployment((ServiceDeployment) o);
                }
            }
        }
        return sig;
    }

    public static Signature sig(String name, String operation, Class serviceType, Object... args) throws SignatureException {
        ServiceSignature s = sig(operation, serviceType, args);
        s.setName(name);
        return s;
    }

    public static Signature sig(Multitype multitype, Object... items) throws SignatureException {
        if (items == null || items.length == 0) {
            if (multitype.providerType != null) {
                return defaultSig(multitype.providerType);
            } else if (multitype.typeName != null) {
                ObjectSignature os = new ObjectSignature();
                os.setMultitype(multitype);
                os.getServiceType();
                return os;
            }
        }
        Operation operation = null;
        String selector = null;
        Args args = null;
        Strategy.Provision provision = null;
        Deployment deployment = null;
        ProviderName prvName = null;
        ParameterTypes parTypes = null;
        Context.In inPaths = null;
        Context.Out outPaths = null;
        ServiceContext context = null;
        Context.Return returnPath = null;
        for (Object item : items) {
            if (item instanceof String) {
                selector = (String) item;
            } else if (item instanceof Operation) {
                operation = (Operation)item;
            } else if (item instanceof Args) {
                args = (Args)item;
            } else if (item instanceof ParameterTypes) {
                parTypes = (ParameterTypes)item;
            } else if (item instanceof Provision) {
                provision = (Provision)item;
            } else if (item instanceof ServiceContext) {
                context = (ServiceContext)item;
            } else if (item instanceof Context.In) {
                inPaths = (Context.In)item;
            } else if (item instanceof Context.Out) {
                outPaths = (Context.Out)item;
            } else if (item instanceof Deployment ) {
                deployment = (Deployment)item;
            } else if (item instanceof ProviderName ) {
                prvName = (ProviderName) item;
                if (!(prvName instanceof ServiceName)) {
                    prvName.setName(Sorcer.getActualName(prvName.getName()));

                }
            } else if (item instanceof Context.Return) {
                returnPath = (Context.Return)item;
            }
        }
        ServiceSignature signature = null;
        if (args != null && parTypes != null) {
            ObjectSignature os = new ObjectSignature();
            os.setMultitype(multitype);
            os.getServiceType();
            os.setArgs(args.args);
            os.setParameterTypes(parTypes.parameterTypes);
            return os;
        } else if (operation != null) {
            signature = sig(operation.getName(), multitype.getProviderType());
            signature.setMultitype(multitype);
            signature.setOperation(operation);
            if (prvName != null) {
                signature.setProviderName(prvName);
            }
        } else if (operation == null && selector == null) {
            signature = sig("?", multitype.providerType, items);
        } else {
            Object[] dest = new Object[items.length+2];
            System.arraycopy(items,  0, dest,  2, items.length);
            dest[0] = operation;
            dest[1] = multitype;
            signature = sig(selector, multitype.providerType, dest);
        }
        if (provision != null) {
            signature.setProvisionable(provision);
        }
        if (deployment != null) {
            signature.setDeployment((ServiceDeployment) deployment);
        }
        // if context is provided for created signature
        if (context instanceof ServiceContext
            // not applied to connectors in Signatures
            && context.getClass() != MapContext.class) {
            if (signature.getContextReturn() == null) {
                signature.setContextReturn(new Context.Return());
            }
            signature.getContextReturn().setDataContext(context);
        }

        // handle return contextReturn
        if (returnPath != null) {
            signature.setContextReturn(returnPath);
        }

        // handle input output paths
        if (inPaths != null) {
            if (signature.getContextReturn() == null) {
                signature.setContextReturn(new Context.Return(inPaths));
            } else {
                signature.getContextReturn().inPaths = inPaths;
            }
        }
        if (outPaths != null) {
            if (signature.getContextReturn() == null) {
                signature.setContextReturn(new Context.Return(outPaths));
            } else {
                signature.getContextReturn().outPaths = outPaths;
            }
        }

        return signature;
    }

    public static Signature sig(Signature signature, Object... items) throws SignatureException {
        Multitype multitype = ((ServiceSignature) signature).getMultitype();
        Operation operation = ((ServiceSignature) signature).getOperation();
        Args args = null;
        Strategy.Provision provision = Provision.NO;
        ParameterTypes parTypes = null;
        Context.In inPaths = null;
        Context.Out outPaths = null;
        ServiceContext context = null;
        for (Object item : items) {
            if (item instanceof Args) {
                args = (Args) item;
            } else if (item instanceof ParameterTypes) {
                parTypes = (ParameterTypes) item;
            } else if (item instanceof Provision) {
                provision = (Provision) item;
            } else if (item instanceof ServiceContext) {
                context = (ServiceContext) item;
            } else if (item instanceof Context.In) {
                inPaths = (Context.In) item;
            } else if (item instanceof Context.Out) {
                outPaths = (Context.Out) item;
            }
        }
        ServiceSignature newSig = null;
        if (args != null && parTypes != null) {
            ObjectSignature os = new ObjectSignature();
            os.setMultitype(multitype);
            os.getServiceType();
            os.setArgs(args.args);
            os.setParameterTypes(parTypes.parameterTypes);
            os.setProvisionable(provision);
            return os;
        } else if (operation != null) {
            newSig = sig(operation.getName(), multitype.getProviderType());
            newSig.setMultitype(multitype);
            newSig.setOperation(operation);
        }

        if (provision != null) {
            newSig.setProvisionable(provision);
        }

        // if context is provided for created signature
        if (context instanceof ServiceContext
            // not applied to connectors in Signatures
            && context.getClass() != MapContext.class) {
            if (newSig.getContextReturn() == null) {
                newSig.setContextReturn(new Context.Return());
            }
            newSig.getContextReturn().setDataContext(context);
        }

        // handle input output paths
        if (inPaths != null) {
            if (newSig.getContextReturn() == null) {
                newSig.setContextReturn(new Context.Return(inPaths));
            } else {
                newSig.getContextReturn().inPaths = inPaths;
            }
        }
        if (outPaths != null) {
            if (newSig.getContextReturn() == null) {
                newSig.setContextReturn(new Context.Return(outPaths));
            } else {
                newSig.getContextReturn().outPaths = outPaths;
            }
        }
        return newSig;
    }

    public static Signature sig(Class classType, Object... items) throws SignatureException {
        Multitype serviceType = new Multitype(classType);
        return sig(serviceType, items);
    }

    public static Signature sig(Signature signature, String operation) throws SignatureException {
        ((ServiceSignature)signature).setSelector(operation);
        return signature;
    }

    public static Signature sig(Signature signature, Operation operation) throws SignatureException {
        ((ServiceSignature)signature).setOperation(operation);
        return signature;
    }

    public static ServiceSignature op(String operation, Class serviceType, Object... items) throws SignatureException {
        return sig(operation, serviceType, items);
    }

    public static ServiceSignature sig(String operation, Class serviceType, Object... items) throws SignatureException {
        ProviderName providerName = null;
        Provision p = null;
        List<MapContext> connList = new ArrayList<MapContext>();
        Multitype srvType = null;
        List<Class> sigTypes = new ArrayList<>();
        Args args = null;
        Exerter provider = null;
        if (items != null) {
            for (Object o : items) {
                if (o instanceof ProviderName) {
                    providerName = (ProviderName) o;
                    if (!(providerName instanceof ServiceName))
                        providerName.setName(Sorcer.getActualName(providerName.getName()));
                } else if (o instanceof Provision) {
                    p = (Provision) o;
                } else if (o instanceof MapContext) {
                    connList.add(((MapContext) o));
                } else if (o instanceof Multitype) {
                    srvType = (Multitype) o;
                } else if (o instanceof Exerter) {
                    provider = (Exerter) o;
                } else if (o instanceof Args) {
                    args = (Args) o;
                } else if (o instanceof Class) {
                    sigTypes.add((Class) o);
                }
            }
        }
        if (providerName == null)
            providerName = new ProviderName();
        Signature sig = null;
        if (srvType != null && srvType.providerType == null) {
            sig = new ServiceSignature(operation, srvType, providerName);
        } else if (serviceType != null) {
            if (serviceType.isInterface()) {
                sig = new NetSignature(operation, serviceType, providerName);
            } else {
                sig = new ObjectSignature(operation, serviceType);
                if (provider != null) {
                    // loclal SessionProvider
                    if (provider instanceof SessionProvider) {
                        Object bean = null;
                        try {
                            bean = sig.getServiceType().newInstance();
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new SignatureException(e);
                        }
                        ((SessionProvider)provider).setBean(bean);
                    }
                    ((ObjectSignature)sig).setTarget(provider);
                }
                sig.setProviderName(providerName);
                if (args != null) {
                    ((ObjectSignature)sig).setArgs(args.args);
                }
            }
        }
        ((ServiceSignature) sig).setName(operation);

        if (connList != null) {
            for (MapContext conn : connList) {
                if (conn.direction == MapContext.Direction.IN)
                    ((ServiceSignature) sig).setInConnector(conn);
                else
                    ((ServiceSignature) sig).setOutConnector(conn);
            }
        }

        if (p != null)
            ((ServiceSignature) sig).setProvisionable(p);

        if (items.length > 0) {
            for (Object o : items) {
                if (o instanceof Type) {
                    sig.setType((Type) o);
                } else if (o instanceof Operating) {
                    ((ServiceSignature) sig).setActive((Operating) o);
                } else if (o instanceof Provision) {
                    ((ServiceSignature) sig).setProvisionable((Provision) o);
                } else if (o instanceof Strategy.Shell) {
                    ((ServiceSignature) sig).setShellRemote((Strategy.Shell) o);
                } else if (o instanceof Context.Return) {
                    sig.setContextReturn((Context.Return) o);
                } else if (o instanceof ParameterTypes) {
                    ((ServiceSignature)sig).setMatchTypes(((ParameterTypes) o).parameterTypes);
                } else if (o instanceof Context.In) {
                    if (sig.getContextReturn() == null) {
                        sig.setContextReturn(new Context.Return((Context.In) o));
                    } else {
                        sig.getContextReturn().inPaths = (Context.In) o;
                    }
                } else if (o instanceof Context.Out) {
                    if (sig.getContextReturn() == null) {
                        sig.setContextReturn(new Context.Return((Context.Out) o));
                    } else {
                        sig.getContextReturn().outPaths = (Context.Out) o;
                    }
                } else if (o instanceof ServiceDeployment) {
                    ((ServiceSignature) sig).setDeployment((ServiceDeployment) o);
                } else if (o instanceof Version && sig instanceof NetSignature) {
                    ((NetSignature) sig).setVersion(((Version) o).getName());
                } else if (o instanceof ServiceSignature && sig instanceof ObjectSignature) {
                    ((ObjectSignature) sig).setTargetSignature(((ServiceSignature) o));
                } else if (o instanceof ServiceContext
                    // not applied to connectors in Signatures
                    && o.getClass() != MapContext.class) {
                    if (sig.getContextReturn() == null) {
                        sig.setContextReturn(new Context.Return());
                    }
                    ((Context.Return) sig.getContextReturn()).setDataContext((Context) o);
                }
            }
        }

        if (sigTypes.size() > 0) {
            Class[] typeArray = new Class[sigTypes.size()];
            sig.getMultitype().matchTypes = sigTypes.toArray(typeArray);
        }
        return (ServiceSignature) sig;
    }

    public static Operation op(Signature sig) {
        return ((ServiceSignature)sig).getOperation();
    }

    public static Operation op(String selector,  Arg... args) {
        Operation sop = new Operation();
        sop.selector = selector;
        for (Arg arg : args) {
            if (arg instanceof Path) {
                sop.path = arg.getName();
            } else if (arg instanceof Strategy.Access) {
                sop.accessType = (Strategy.Access) arg;
            } else if (arg instanceof Strategy.Flow) {
                sop.flowType = (Strategy.Flow) arg;
            } else if (arg instanceof Strategy.Monitor) {
                sop.toMonitor = (Strategy.Monitor) arg;
            } else if (arg instanceof Strategy.FidelityManagement) {
                sop.toManageFi = (Strategy.FidelityManagement) arg;
            } else if (arg instanceof Strategy.Wait) {
                sop.toWait = (Strategy.Wait) arg;
            } else if (arg instanceof Strategy.Shell) {
                sop.isShellRemote = (Strategy.Shell) arg;
            } else if (arg instanceof Strategy.Provision) {
                sop.isProvisionable = Strategy.isProvisionable((Strategy.Provision) arg);
            }  else if (arg instanceof Tokens) {
                sop.setMatchTokens((Tokens)arg);
            }
        }
        return sop;
    }

    public static Operation op(String path, String selector, Arg... args) {
        Operation sop = new Operation();
        sop.path = path;
        sop.selector = selector;
        for (Arg arg : args) {
            if (arg instanceof Strategy.Access) {
                sop.accessType = (Strategy.Access)arg;
            } else if (arg instanceof Strategy.Provision) {
                sop.isProvisionable = Strategy.isProvisionable((Strategy.Provision)arg);
            }
        }
        return sop;
    }

    public static Multitype mt(Class... types) {
        return multitype(types);
    }

    public static Multitype multitype(Class... types) {
        Multitype mType = new Multitype(types[0]);
        if (types.length > 1) {
            Class[] matchTypes = new Class[types.length - 1];
            System.arraycopy(types, 1, matchTypes, 0, types.length - 1);
            mType.matchTypes = matchTypes;
        }
        return mType;
    }

    public static Multitype mt(String typeName, Class... types) {
        return multitype(typeName, types);
    }

    public static Multitype multitype(String typeName, Class... types) {
        Multitype mType = new Multitype(typeName);
        if (types != null && types.length > 0) {
            mType.matchTypes = types;
        }
        return mType;
    }

    public static String property(String property) {
        return System.getProperty(property);
    }

    public static String property(String property, String value) {
        return System.setProperty(property, value);
    }

    public static String home() {
        return Sorcer.getHome();
    }

    public static ProviderName prvName(String name) {
        return new ProviderName(name);
    }

    public static ServiceName srvName(String name, String... group) {
        return new ServiceName(name, group);
    }

    public static ServiceName srvName(String name, ArgList locators, String... group) {
        return new ServiceName(name, locators.getNameArray(), group);
    }

    public static ArgList locators(String... locators) {
        ArgList argList = new ArgList();
        if (locators == null || locators.length == 0) {
            try {
                argList = new ArgList(InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else {
            argList = new ArgList(locators);
        }
        argList.setType(Functionality.Type.LOCATOR);
        return argList;
    }

    public static String actualName(String name) {
        return Sorcer.getActualName(name);
    }

    public static Signature sig(String selector) throws SignatureException {
        return new ServiceSignature(selector);
    }

    public static Signature sig(String name, String selector)
        throws SignatureException {
        return new ServiceSignature(name, selector);
    }

    public static Signature sig(String name, String selector, ServiceDeployment deployment)
        throws SignatureException {
        ServiceSignature signture = new ServiceSignature(name, selector);
        signture.setDeployment(deployment);
        signture.setProvisionable(true);
        return signture;
    }

    public static Signature defaultSig(Class<?> serviceType) throws SignatureException {
        if (Modeling.class.isAssignableFrom(serviceType)) {
            return sig("compute", serviceType);
        } else if (Service.class.isAssignableFrom(serviceType)) {
            return sig("exert", serviceType);
        } else {
            return sig(serviceType, (Context.Return) null);
        }
    }

    public static Signature sig(Class<?> serviceType, Context.Return returnPath, ServiceDeployment deployment)
        throws SignatureException {
        Signature signature = sig(serviceType, returnPath);
        ((ServiceSignature) signature).setDeployment(deployment);
        ((ServiceSignature) signature).setProvisionable(true);
        return signature;
    }

    public static Signature sig(Class<?> serviceType, Context.Return returnPath)
        throws SignatureException {
        Signature sig = null;
        if (serviceType.isInterface()) {
            sig = new NetSignature("exert", serviceType);
        } else if (Executor.class.isAssignableFrom(serviceType)) {
            sig = new ObjectSignature("exert", serviceType);
        } else {
            sig = new ObjectSignature(serviceType);
        }
        if (returnPath != null)
            sig.setContextReturn(returnPath);
        return sig;
    }

    public static EvaluationSignature sig(String name, Evaluator evaluator,
                                          Context.Return requestPath) throws SignatureException {
        EvaluationSignature sig = null;
        if (evaluator instanceof Scopable) {
            sig = new EvaluationSignature(new Prc(evaluator));
        } else {
            sig = new EvaluationSignature(evaluator);
        }
        sig.setContextReturn(requestPath);
        if (name != null) {
            sig.setName(name);
        }
        return sig;
    }

    public static EvaluationSignature sig(Evaluator evaluator,
                                          Context.Return requestPath) throws SignatureException {
        return sig(null, evaluator, requestPath);
    }

    public static EvaluationSignature sig(Evaluator evaluator) throws SignatureException {
        return new EvaluationSignature(evaluator);
    }

    public static EvaluationSignature sig(String name, Evaluator evaluator) throws SignatureException {
        return new EvaluationSignature(name, evaluator);
    }

    public static Signature sig(Subroutine exertion, String componentExertionName) {
        Subroutine component = (Subroutine) exertion.getMogram(componentExertionName);
        return component.getProcessSignature();
    }

    public static Signature sig(Path source) {
        return new NetletSignature(source);
    }

    public static Signature builder(Signature signature) {
        signature.setType(Type.BUILDER);
        return signature;
    }

    public static Signature pre(Signature signature) {
        signature.setType(Type.PRE);
        return signature;
    }

    public static Signature post(Signature signature) {
        signature.setType(Type.POST);
        return signature;
    }

    public static Signature pro(Signature signature) {
        signature.setType(Type.PROC);
        return signature;
    }

    public static Signature apd(Signature signature) {
        signature.setType(Type.APD_DATA);
        return signature;
    }

    public static Signature type(Signature signature, Signature.Type type) {
        signature.setType(type);
        return signature;
    }

    public static Metafidelity metaFi(String name, Fidelity... selectors) {
        Metafidelity fi = new Metafidelity(name, selectors);
        fi.fiType = Fi.Type.META;
        return fi;
    }

    public static List<Service>  fis(Mogram mogram) {
        if (mogram.getMultiFi() != null) {
            return mogram.getMultiFi().getSelects();
        } else {
            return null;
        }
    }

    public static Fidelity fi(Mogram mogram) {
        return mogram.getSelectedFidelity();
    }

    public static Fidelity fi(String name, String path) {
        Fidelity fi = new Fidelity(name, path);
        fi.fiType = Fi.Type.SELECT;
        return fi;
    }

    public static Fidelity fi(String name, String path, String select) {
        Fidelity fi = new Fidelity(name, path, select);
        fi.fiType = Fi.Type.SELECT;
        return fi;
    }

    public static DisciplineFidelity discFi(Fidelity... fidelities) {
        DisciplineFidelity fi = new DisciplineFidelity(fidelities);
        fi.fiType = Fi.Type.DISCIPLINE;
        return fi;
    }
    public static DisciplineFidelity discFi(String name, Fidelity... fidelities) {
        DisciplineFidelity fi = new DisciplineFidelity(name, fidelities);
        fi.fiType = Fi.Type.DISCIPLINE;
        return fi;
    }

    public static Fidelity cxtFi(String name) {
        Fidelity fi = new Fidelity(name);
        fi.fiType = Fi.Type.CONTEXT;
        return fi;
    }

    public static Fidelity cxtFi(String name, Object select) {
        Fidelity fi = new Fidelity(name);
        fi.setSelect(select);
        fi.fiType = Fi.Type.CONTEXT;
        return fi;
    }

    public static Fidelity dsptFi(String name) {
        Fidelity fi = new Fidelity(name);
        fi.fiType = Fi.Type.DISPATCHER;
        return fi;
    }

    public static Fidelity dsptFi(String name, Object select) {
        Fidelity fi = new Fidelity(name);
        fi.setSelect(select);
        fi.fiType = Fi.Type.DISPATCHER;
        return fi;
    }

    public static Fidelity procFi(String name) {
        Fidelity fi = new Fidelity(name);
        fi.fiType = Fi.Type.GOVERNANCE;
        return fi;
    }

    public static Fidelity procFi(String name, Object select) {
        Fidelity fi = new Fidelity(name);
        fi.setSelect(select);
        fi.fiType = Fi.Type.GOVERNANCE;
        return fi;
    }

    public static String fiName(Mogram mogram) {
        return ((Identifiable) mogram.getMultiFi().getSelect()).getName();
    }

    public static Fi srvFis(Subroutine exertion) {
        return exertion.getMultiFi();
    }

    public static MorphFidelity mphFi(Morpher morpher, Service... services) {
        MorphFidelity morphFi = new MorphFidelity(new ServiceFidelity(services));
        morphFi.setMorpher(morpher);
        return morphFi;
    }

    public static MorphFidelity mphFi(String name, Morpher morpher, Service... services) {
        MorphFidelity morphFi = new MorphFidelity(new ServiceFidelity(name, services));
        morphFi.setMorpher(morpher);
        morphFi.setPath(name);
        return morphFi;
    }

    public static MorphFidelity mphFi(Service... services) {
        MorphFidelity morphFi = new MorphFidelity(services);
        return morphFi;
    }

    public static MorphFidelity mphFi(String name, Service... services) {
        MorphFidelity morphFi = new MorphFidelity(new ServiceFidelity(name, services));
        return morphFi;
    }

    public static ServiceFidelity rFi(Service... services) {
        ServiceFidelity srvFi = new ServiceFidelity(services);
        srvFi.fiType = Fi.Type.REQUEST;
        return srvFi;
    }

    public static ServiceFidelity rFi(String name, Service... services) {
        ServiceFidelity srvFi = new ServiceFidelity(services);
        srvFi.setPath(name);
        srvFi.fiType = Fi.Type.REQUEST;
        return srvFi;
    }

    public static void selectFi(Mogram mogram, String selection) throws ConfigurationException {
        mogram.selectFidelity(selection);
    }

    public static MultiFiMogram mogFi(Metafidelity fidelity) {
        return new MultiFiMogram(fidelity.getName(), fidelity);
    }

    public static MultiFiMogram mogFi(MorphFidelity fidelity) {
        return new MultiFiMogram(fidelity.getName(), fidelity);
    }

    public static MultiFiMogram mogFi(Morpher  morpher, Service... mograms) {
        MorphFidelity morphFi = new MorphFidelity(new ServiceFidelity(mograms));
        morphFi.setMorpher(morpher);
        return new MultiFiMogram(morphFi.getName(), morphFi);
    }

    public static MultiFiMogram mogFi(String name, Metafidelity fidelity) {
        return new MultiFiMogram(name, fidelity);
    }

    public static MultiFiMogram mogFi(String name, ServiceFidelity fidelity) {
        return new MultiFiMogram(name, fidelity);
    }

    public static MultiFiMogram mogFi(String name, MorphFidelity fidelity) {
        return new MultiFiMogram(name, fidelity);
    }

    public static MultiFiMogram mogFi(Metafidelity fidelity, Context context) {
        return new MultiFiMogram(context, fidelity);
    }

    public static MultiFiMogram mogFi(ServiceFidelity fidelity, Context context) {
        MultiFiMogram mfr = new MultiFiMogram(fidelity);
        mfr.setScope(context);
        mfr.setName(fidelity.getName());
        return mfr;
    }

    public static MultiFiMogram mogFi(String name, ServiceFidelity fidelity, Context context) {
        MultiFiMogram mfr = new MultiFiMogram(name, fidelity);
        mfr.setScope(context);
        mfr.setName(fidelity.getName());
        return mfr;
    }

    public static MultiFiMogram mogFi(String name, MorphFidelity fidelity, Context context) {
        MultiFiMogram mfr = new MultiFiMogram(context, fidelity);
        mfr.setName(fidelity.getName());
        return mfr;
    }

    public static MultiFiMogram mogFi(MorphFidelity fidelity, Context context) {
        MultiFiMogram mfr = new MultiFiMogram(context, fidelity);
        mfr.setName(fidelity.getName());
        return mfr;
    }

    public static MorphFidelity mrphFi(Signature... signatures) {
        MorphFidelity multiFi = new MorphFidelity(new ServiceFidelity(signatures));
        return multiFi;
    }

    public static ServiceFidelity sigFi(Signature... signatures) {
        ServiceFidelity fi = new ServiceFidelity(signatures);
        fi.fiType = Fi.Type.SIG;
        return fi;
    }

    public static ServiceFidelity entFi(String fiName, Entry... entries) {
        ServiceFidelity fi = new ServiceFidelity(fiName, entries);
        fi.fiType = Fi.Type.ENTRY;
        return fi;
    }

    public static ServiceFidelity mnFi(Activator... activations) {
        ServiceFidelity fi = new ServiceFidelity(activations);
        fi.fiType = Fi.Type.ANE;
        return fi;
    }

    public static Activator nFi(String name, Args args, Context<Float> weights, Value... entries) {
        Activator activator = new Activator(name, args, weights, entries);
        return activator;
    }

    public static Activator nFi(String name, Args args, Value... entries) {
        Activator activator = new Activator(name, args, null, entries);
        return activator;
    }

    public static Activator nFi(String name, Context<Float> weights, Value... entries) {
        Activator fi = new Activator(name, null, weights, entries);
        return fi;
    }

    public static Fidelity<Entry> entFi(Entry... entries) {
        Fidelity<Entry> fi = new Fidelity(entries);
        fi.setSelect(entries[0]);
        fi.fiType = Fi.Type.ENTRY;
        return fi;
    }

    public static Fidelity fi(String name$path) {
        Fidelity fi = null;
        if (name$path.indexOf('$') > 1) {
            String name = null;
            String path = null;
            int ind = name$path.indexOf('$');
            name = name$path.substring(0, ind);
            path = name$path.substring(ind + 1);
            fi = new Fidelity(name, path);
        } else {
            fi = new Fidelity(name$path);
        }
        fi.fiType = Fi.Type.SELECT;
        return fi;
    }

    public static Fidelity mdaFi(String name, String path) {
        Fidelity fi = new Fidelity(name, path);
        fi.fiType = Fi.Type.MDA;
        fi.setOption(Fi.Type.SELECT);
        return fi;
    }

    public static Fidelity ifFi(String name) {
        Fidelity fi = new Fidelity(name);
        fi.fiType = Fi.Type.SELECT;
        fi.setOption(Fi.Type.IF);
        return fi;
    }

    public static Fidelity ifFi(String name, String path) {
        Fidelity fi = new Fidelity(name, path);
        fi.fiType = Fi.Type.SELECT;
        fi.setOption(Fi.Type.IF);
        return fi;
    }

    public static Fidelity ifSoaFi(String name) {
        Fidelity fi = new Fidelity(name);
        fi.fiType = Fi.Type.SELECT;
        fi.setOption(Fi.Type.IF_SOA);
        return fi;
    }

    public static Fidelity ifSoaFi(String name, String path) {
        Fidelity fi = new Fidelity(name, path);
        fi.fiType = Fi.Type.SELECT;
        fi.setOption(Fi.Type.IF_SOA);
        return fi;
    }

    public static Projection po(String name, Fidelity... fidelities) {
        return projection(name, fidelities);
    }

    public static Projection projection(String name, Fidelity... fidelities) {
        Projection p = new Projection(fidelities);
        p.setName(name);
        return p;
    }

    public static Projection po(Fidelity... fidelities) {
        return new Projection(fidelities);
    }

    // projection of
    public static Projection po(ServiceFidelity fidelity) {
        return new Projection(fidelity);
    }

    public static Projection po(String name, ServiceFidelity fidelity) {
        Projection p = new Projection(fidelity);
        p.setName(name);
        return p;
    }

    public static Fidelity fis(String fidelity, List<Path> paths, String gradient) {
        FidelityList  fl = new FidelityList(paths.size());
        for (Path path : paths) {
            fl.add(gFi(fidelity, path.getName(), gradient));
        }
        return new Projection(fl);
    }

    public static Fidelity fis(String fidelity, List<Path> paths, Fidelity subFi) {
        FidelityList  fl = new FidelityList(paths.size());
        for (Path path : paths) {
            fl.add(fi(fidelity, path.getName(), subFi));
        }
        return new Projection(fl);
    }

    public static FidelityList fis(Fidelity... fidelities) {
        return new FidelityList(fidelities);
    }

    public static ServiceFidelityList fiList(Fidelity... fidelities) {
        return new ServiceFidelityList(fidelities);
    }

    public static ServiceFidelityList srvFis(ServiceFidelity... fidelities) {
        return new ServiceFidelityList(fidelities);
    }

    public static ServiceFidelityList fis(Arg... args) {
        ServiceFidelityList fl = new ServiceFidelityList();
        for (Arg arg : args) {
            if (arg instanceof ServiceFidelity) {
                fl.add((ServiceFidelity)arg);
            } else if (arg instanceof ServiceFidelityList) {
                fl.addAll((ServiceFidelityList)arg);
            }
        }
        return fl;
    }

    public static FiEntry fiEnt(int index, FidelityList fiList) {
        return new FiEntry(index, fiList);
    }

    public static Fidelity fi(String name, String path, Fi.Type type) {
        Fidelity fi = new Fidelity(name, path);
        fi.fiType = type;
        return fi;
    }

    public static Fidelity fi(String name, String path, int type) {
        Fidelity fi = new Fidelity(name, path);
        fi.fiType = Fi.Type.type(type);
        return fi;
    }

    public static Fidelity<String> gFi(String name, String path, String gradient) {
        Fidelity<String> fi = new Fidelity(name, path, gradient);
        fi.fiType = Fi.Type.GRADIENT;
        return fi;
    }

    public static Fidelity soaFi(String name, String path) {
        Fidelity fi = new Fidelity(name, path);
        fi.fiType = Fi.Type.SOA;
        return fi;
    }


    public static Fidelity gFi(String name, String path, String gradientName, Fidelity subFi) throws ConfigurationException {
        Fidelity fi = new Fidelity(name, path);
        fi.fiType = Fi.Type.GRADIENT;
        fi.selectSelect(gradientName);
        fi.setOption(subFi);
        return fi;
    }

    public static Fidelity fi(String name, String path, Fidelity subFi) {
        Fidelity fi = new Fidelity(name, path);
        fi.fiType = Fi.Type.SELECT;
        fi.setOption(subFi);
        return fi;
    }


    public static ServiceFidelity rFi(String name, String path) throws ConfigurationException {
        ServiceFidelity fi = new ServiceFidelity(name, (List<Service>) path(path));
        fi.setPath(path);
        fi.selectSelect(path);
        fi.fiType = Fi.Type.SELECT;
        return fi;
    }

    public static Fidelity<String> mphFi(String name, String path) {
        Fidelity<String> fi = new Fidelity(name);
        fi.setPath(path);
        fi.fiType = Fi.Type.MORPH;
        return fi;
    }

    public static ServiceFidelity sigFi(String name, Signature... signatures) {
        ServiceFidelity fi = new ServiceFidelity(name, signatures);
        fi.setSelect(signatures[0]);
        fi.fiType = Fi.Type.SIG;
        return fi;
    }

    public static ServiceFidelity sigFi(String name, Ref... references) {
        ServiceFidelity fi = new ServiceFidelity(name, references);
        fi.setSelect(references[0]);
        fi.fiType = Fi.Type.REF;
        return fi;
    }

    public static ServiceFidelity sigFi(Ref... references) {
        ServiceFidelity fi = new ServiceFidelity(references);
        fi.fiType = Fi.Type.REF;
        return fi;
    }

    public static Signature sig(String operation, Object object)
        throws SignatureException {
        return sig(operation, object, null, null, null);
    }

    public static Signature sig(String operation, Object object,
                                Class[] types, Object... args) throws SignatureException {
        if (args == null || args.length == 0)
            return sig(operation, object, (String) null, types);
        else
            return sig(operation, object, null, types, args);
    }

    public static ObjectSignature sig(String operation, Object object, String initOperation,
                                      Class[] types) throws SignatureException {
        try {
            if (object instanceof Class && ((Class) object).isInterface()) {
                if (initOperation != null)
                    return new NetSignature(operation, (Class) object, Sorcer.getActualName(initOperation));
                else
                    return new NetSignature(operation, (Class) object);
            } else if (object instanceof Class) {
                return new ObjectSignature(operation, object, initOperation,
                    types == null || types.length == 0 ? null : types);
            } else {
                return new ObjectSignature(operation, object,
                    types == null || types.length == 0 ? null : types);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SignatureException(e);
        }
    }

    public static ObjectSignature sig(Object object, String initSelector,
                                      Class[] types, Object[] args) throws SignatureException {
        try {
            return new ObjectSignature(object, initSelector, types, args);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SignatureException(e);
        }
    }

    public static Signature sig(String selector, Object object, String initSelector,
                                Class[] types, Object[] args) throws SignatureException {
        try {
            if (object instanceof NetSignature) {
                ((NetSignature)object).setSelector(selector);
                return (Signature)object;
            } else {
                return new ObjectSignature(selector, object, initSelector, types, args);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SignatureException(e);
        }
    }

    public static Task sigTask(Signature signature, Object... items) throws SignatureException {
        Operation operation = null;
        String name = null;
        String selector = null;
        List<String> strings = new ArrayList();
        Context context = null;
        for (Object item : items) {
            if (item instanceof String) {
                strings.add((String) item);
            } else if (item instanceof Operation) {
                operation = ((Operation) item);
            } else if (item instanceof Context) {
                context =  ((Context) item);
            }
        }
        Task task = null;
        if (operation != null) {
            selector = operation.selector;
        } else {
            selector = signature.getSelector();
        }
        if (strings.size()==1) {
            name = strings.get(0);
        } else if (strings.size()==2) {
            // if both string then the first one is key
            name = strings.get(0);
            selector = strings.get(0);
        }
        if (selector != null) {
            ((ServiceSignature)signature).setSelector(selector);
        }

        if (name == null) {
            name = signature.getSelector();
        }

        if (signature.getClass() == ObjectSignature.class) {
            task = new ObjectTask(name, signature);
        } else if (signature.getClass() == NetSignature.class) {
            task = new NetTask(name, signature);
        } else if (signature.getClass() == EvaluationSignature.class) {
            task = new EvaluationTask(name, (EvaluationSignature)signature);
        } else if (signature.getClass() == NetSignature.class) {
            task = new Task(name, signature);
        }
        if (context != null) {
            task.setContext(context);
        }
        return task;
    }

    public static ModelingTask modelerTask(String name, Signature signature)
        throws SignatureException {
        ModelingTask task = null;
        if (signature instanceof NetSignature) {
            task = new ModelerNetTask(name, signature);
        } else if (signature instanceof ObjectSignature) {
            task = new ModelerTask(name, signature);
        }
        return task;
    }

    public static ModelingTask modelerTask(Signature signature, Context context)
        throws SignatureException {
        ModelingTask task = null;
        if (signature instanceof NetSignature) {
            task = new ModelerNetTask(signature, context);
        } else if (signature instanceof ObjectSignature) {
            task = new ModelerTask(signature, context);
        }
        return task;
    }

    public static Task task(Object... items) throws EvaluationException {
        if (items.length == 1 &&  items[0] instanceof Evaluation) {
            // evaluation task for a single evalution
            try {
                return new EvaluationTask((Evaluation) items[0]);
            } catch (RemoteException | ContextException e) {
                throw new EvaluationException(e);
            }
        }

        Operation operation = null;
        String name = null;
        String selector = null;
        List<String> strings = new ArrayList();
        List<Service> sigs = new ArrayList();
        Service srvSig = null;
        Context context = null;
        ControlContext cc = null;
        // structural pass
        for (Object item : items) {
            if (item instanceof String) {
                strings.add((String) item);
            } else if (item instanceof Operation) {
                operation = ((Operation) item);
            } if (item instanceof ControlContext) {
                cc = (ControlContext) item;
            } else if (item instanceof Context) {
                context = ((Context) item);
            } else if (item instanceof Signature) {
                sigs.add((Signature) item);
            }
        }

        if (sigs.size() == 1) {
            srvSig = sigs.get(0);
        } else if (sigs.size() > 1) {
            for (Object s : sigs) {
                if (s instanceof Signature && ((Signature)s).getExecType() == Signature.SRV) {
                    srvSig = (Signature)s;
                    break;
                }
            }
        }

        if (srvSig != null) {
            if (operation != null) {
                selector = operation.selector;
            } else {
                selector = ((Signature)srvSig).getSelector();
            }

            if (name == null) {
                name = ((Signature)srvSig).getSelector();
            }
        }
        if (strings.size()==1) {
            name = strings.get(0);
        } else if (strings.size()==2) {
            // if both string then the first one is key
            name = strings.get(0);
            selector = strings.get(0);
        }
        if (selector != null) {
            ((ServiceSignature)srvSig).setSelector(selector);
        }

        ServiceFidelity sigFi = null;
        Task task = null;
        // construction phase
        if (srvSig != null) {
            try {
                if (((ServiceSignature)srvSig).isModelerSignature()) {
                    task = (Task) modelerTask(name, (Signature)srvSig);
                } else if (srvSig.getClass() == ObjectSignature.class) {
                    task = new ObjectTask(name, (Signature) srvSig);
                } else if (srvSig.getClass() == NetSignature.class) {
                    task = new NetTask(name,  (Signature) srvSig);
                } else if (srvSig.getClass() == EvaluationSignature.class) {
                    task = new EvaluationTask(name, (EvaluationSignature)srvSig);
                } else if (srvSig.getClass() == ServiceSignature.class) {
                    task = new Task(name,  (Signature) srvSig);
                }
            } catch (SignatureException e) {
                throw new EvaluationException(e);
            }
            sigFi = new ServiceFidelity(name, sigs);
            sigFi.setSelect(srvSig);
        }
        if (operation != null) {
            task.setAccess(operation.accessType);
        }

        FidelityManager fiManager = null;
        Strategy.FidelityManagement fm = null;
        Access access = null;
        Flow flow = null;
        List<Service> fis = new ArrayList<>();
        MorphFidelity mFi = null;
        // configuration pass
        for (Object o : items) {
            if (o instanceof Access) {
                access = (Access) o;
            } else if (o instanceof Flow) {
                flow = (Flow) o;
            } else if (o instanceof FidelityManager) {
                fiManager = ((FidelityManager) o);
            } else if (o instanceof MorphFidelity) {
                mFi = (MorphFidelity) o;
            } else if (o instanceof ServiceFidelity) {
                fis.add(((ServiceFidelity) o));
            } else if (o instanceof Strategy.FidelityManagement) {
                fm = (Strategy.FidelityManagement) o;
            }
        }

        if (context == null) {
            context = new PositionalContext();
        }

        if (fis.size() > 0 || mFi != null) {
            task = new Task(name);
        }

        task.setContext(context);
        if (cc != null) {
            task.setControlContext(cc);
        }

        ServiceFidelity srvFi = null;
        if (fis.size() > 0) {
            srvFi = new ServiceFidelity(name, fis);
        }

        if (sigFi != null) {
            sigFi.setName(name);
            sigFi.setPath(name);
            task.getMultiFi().addSelect(sigFi);
            task.setSelectedFidelity(sigFi);
        }

        if (srvFi != null) {
            srvFi.setName(name);
            srvFi.setPath(name);
            srvFi.setSelect(fis.get(0));
            task.setMultiFi(srvFi);
            for (Service fi : fis) {
                ((ServiceFidelity)fi).setPath(task.getName());
            }
        }

        if (mFi != null) {
            List<Service> sList = mFi.getFidelity().getSelects();
            ServiceFidelity first = (ServiceFidelity) mFi.getFidelity().getSelects().get(0);
            mFi.setName(task.getName());
            mFi.setPath(task.getName());
            mFi.getFidelity().setPath(name);
            mFi.getFidelity().setSelect(first);
            for (Object fi : sList) {
                ((ServiceFidelity)fi).setPath(name);
            }
            task.setMultiFi((ServiceFidelity) mFi.getFidelity());
            task.setServiceMorphFidelity(mFi);
            task.setSelectedFidelity(first);
            task.setSelectedFidelity(first);
        }

        if (fm == Strategy.FidelityManagement.YES && task.getFidelityManager() == null
            || mFi != null) {
            fiManager = new FidelityManager(task);
            task.setFidelityManager(fiManager);
        }

        if (fiManager != null) {
            task.setFidelityManager(fiManager);
//			fiManager.setFidelities(task.getServiceFidelities());
            fiManager.addFidelity(task.getName(), (Fidelity) task.getMultiFi());
//			fiManager.setMetafidelities(task.getServiceMetafidelities());
            fiManager.addMetaFidelity(task.getName(), (Metafidelity) task.getMultiMetaFi());
            if (mFi != null) {
                fiManager.getMorphFidelities().put(mFi.getName(), mFi);
                mFi.addObserver(fiManager);
                if (mFi.getMorpherFidelity() != null) {
                    // set the default morpher
                    try {
                        mFi.setMorpher((Morpher) ((Entry) mFi.getMorpherFidelity().get(0)).getValue());
                    } catch (ContextException e) {
                        throw new EvaluationException(e);
                    }
                }
            }
        }

        if (access != null) {
            task.setAccess(access);
        }
        if (flow != null) {
            task.setFlow(flow);
        }
        if (cc != null) {
            task.updateStrategy(cc);
        }
        if (srvSig != null && ((ServiceSignature) srvSig).isProvisionable()) {
            task.setProvisionable(true);
        }

        return task;
    }


    public static List<Mogram> mograms(Mogram mogram) {
        if (mogram instanceof Subroutine)
            return ((Subroutine)mogram).getAllMograms();
        else
            return new ArrayList();
    }

    public static Mogram mog(Object... items) throws MogramException {
        return mogram(items);
    }

    public static <M extends Mogram> M mogram(Object... items) throws MogramException {
        String name = "unknown" + count++;
        if (items.length == 1 && items[0] instanceof NetletSignature) {
            String source = ((NetletSignature)items[0]).getServiceSource();
            if(source != null) {
                try {
                    ServiceScripter se = new ServiceScripter(System.out, null, Sorcer.getWebsterUrl(), true);
                    se.readFile(new File(source));
                    return (M)se.interpret();
                } catch (Throwable e) {
                    throw new MogramException(e);
                }
            }
        }
        boolean hasEntry = false;
        boolean hasExertion = false;
        boolean hasContext = false;
        boolean hasSignature = false;
        for (Object i : items) {
            if (i instanceof String) {
                name = (String) i;
            } else if (i instanceof Subroutine) {
                hasExertion = true;
            } else if (i instanceof Context) {
                hasContext = true;
            } else if (i instanceof Signature) {
                hasSignature = true;
            } else if (i instanceof Function) {
                hasEntry = true;
            }
        }
        try {
            if ((hasSignature && hasContext || hasExertion) && !hasEntry) {
                return (M) xrt(name, items);
            } else {
                return (M) model(items);
            }
        } catch(Exception e) {
            throw new MogramException("do not know what mogram to create");

        }
    }

    public static Subroutine xrt(String name, Object... items) throws RoutineException,
        ContextException, SignatureException {
        return exertion(name, items);
    }

    public static <E extends Subroutine> E exertion(String name, Object... items) throws RoutineException,
        ContextException, SignatureException {
        List<Mogram> exertions = new ArrayList<Mogram>();
        Signature sig = null;
        Context cxt = null;
        boolean isBlock =false;
        for (int i = 0; i < items.length; i++) {
            if (items[i] instanceof Subroutine || items[i] instanceof EntModel) {
                exertions.add((Mogram) items[i]);
                if (items[i] instanceof ConditionalTask)
                    isBlock = true;
            } else if (items[i] instanceof Signature) {
                sig = (Signature) items[i];
            } else if (items[i] instanceof String) {
                name = (String) items[i];
            }
        }
        if (isBlock || exertions.size() > 0 && sig != null
            && (sig.getServiceType() == Concatenator.class
            || sig.getServiceType() == ServiceConcatenator.class)) {
            return (E) block(items);
        } else if (exertions.size() > 1) {
            Job j = job(items);
            j.setName(name);
            return (E) j;
        } else {
            return (E)task(items);
        }
    }

    public static Job job(Object... elems) throws RoutineException,
        ContextException, SignatureException {
        String name = "job-" + count++;
        Signature signature = null;
        ControlContext controlStrategy = null;
        Context data = null;
        Context.Return rp = null;
        List<Subroutine> exertions = new ArrayList();
        List<Pipe> pipes = new ArrayList();
        FidelityManager fiManager = null;
        Strategy.FidelityManagement fm = null;
        List<Service> fis = new ArrayList<>();
        List<Fidelity> metaFis = new ArrayList();
        MorphFidelity mFi = null;
        List<MapContext> connList = new ArrayList();
        Context.Out outPaths = null;
        Context.In inPaths = null;


        for (int i = 0; i < elems.length; i++) {
            if (elems[i] instanceof String) {
                name = (String) elems[i];
            } else if (elems[i] instanceof Subroutine) {
                exertions.add((Subroutine) elems[i]);
            } else if (elems[i] instanceof ControlContext) {
                controlStrategy = (ControlContext) elems[i];
            } else if (elems[i] instanceof MapContext) {
                connList.add(((MapContext) elems[i]));
            } else if (elems[i] instanceof Context) {
                data = (Context<?>) elems[i];
            } else if (elems[i] instanceof Pipe) {
                pipes.add((Pipe) elems[i]);
            } else if (elems[i] instanceof Signature) {
                signature = ((Signature) elems[i]);
            } else if (elems[i] instanceof Context.Return) {
                rp = ((Context.Return) elems[i]);
            } else if (elems[i] instanceof FidelityManager) {
                fiManager = ((FidelityManager) elems[i]);
            } else if (elems[i] instanceof MorphFidelity) {
                mFi = (MorphFidelity) elems[i];
            } else if (elems[i] instanceof Fidelity) {
                if (((Fidelity) elems[i]).getFiType().equals(Fidelity.Type.META)) {
                    metaFis.add((Fidelity) elems[i]);
                } else if (elems[i] instanceof ServiceFidelity) {
                    fis.add(((ServiceFidelity) elems[i]));
                }
            } else if (elems[i] instanceof Strategy.FidelityManagement) {
                fm = (Strategy.FidelityManagement) elems[i];
            } else if (elems[i] instanceof Context.Out) {
                outPaths = (Context.Out) elems[i];
            } else if (elems[i] instanceof Context.In) {
                inPaths = (Context.In) elems[i];
            }
        }
        Job job = null;
        if (signature instanceof NetSignature) {
            job = new NetJob(name, signature);
        } else if (signature instanceof ObjectSignature) {
            job = new ObjectJob(name, signature);
        } else {
            if (fis != null && fis.size() > 0) {
                job = new Job(name);
            } else{
                job = new NetJob(name);
            }
        }
        if ((inPaths != null || outPaths != null) && signature.getContextReturn() == null) {
            signature.setContextReturn(new Context.Return(name));
        }
        if (inPaths != null) {
            ((Context.Return)signature.getContextReturn()).setInputPaths(inPaths);
        }
        if (outPaths != null) {
            ((Context.Return)signature.getContextReturn()).outPaths = outPaths;
        }

        ServiceFidelity srvFi = null;
        if (fis.size() > 0) {
            srvFi = new ServiceFidelity(name, fis);
        }
        if (srvFi != null) {
            srvFi.setName(name);
            srvFi.setPath(name);
			job.setMultiFi(srvFi);
            job.setSelectedFidelity((ServiceFidelity)fis.get(0));
            for (Service fi : fis) {
                ((ServiceFidelity)fi).setPath(job.getName());
            }
        }

        if (data != null)
            job.setContext(data);

        if (rp != null) {
            ((ServiceContext) job.getDataContext()).setContextReturn(rp);
        }

        if (controlStrategy != null) {
            job.setControlContext(controlStrategy);

            if (controlStrategy.getAccessType().equals(Access.PULL)) {
                Signature procSig = job.getProcessSignature();
                procSig.setServiceType(Spacer.class);
                job.getDataContext().setRoutine(job);
            }
        }

        if (mFi != null) {
            List<Service> sList = mFi.getFidelity().getSelects();
            ServiceFidelity first = (ServiceFidelity) mFi.getFidelity().getSelects().get(0);
            mFi.setName(job.getName());
            mFi.setPath(job.getName());
            mFi.getFidelity().setPath(job.getName());
            mFi.getFidelity().setSelect(first);
            for (Object fi : sList) {
                ((ServiceFidelity)fi).setPath(job.getName());
            }
            job.getMultiFi().addSelect(mFi.getFidelity());
            job.setServiceMorphFidelity(mFi);
            job.setSelectedFidelity(first);
            job.setSelectedFidelity(first);
        }

        if (metaFis.size() > 0) {
            Metafidelity metaFi = new Metafidelity(name, metaFis);
            Fidelity first = metaFis.get(0);
            metaFi.setSelect(first);
            metaFi.setName(job.getName());
            metaFi.setPath(job.getName());
            for (Object fi : metaFis) {
                ((Fidelity)fi).setPath(name);
            }
            metaFi.setSelect(first);
            job.setMultiMetaFi(metaFi);
        }

        if (fm == Strategy.FidelityManagement.YES && job.getFidelityManager() == null
            || mFi != null) {
            fiManager = new FidelityManager(job);
            job.setFidelityManager(fiManager);
        }

        if (fiManager != null) {
            job.setFidelityManager(fiManager);
            fiManager.addFidelity(job.getName(), (Fidelity) job.getMultiFi());
            fiManager.addMetafidelity(job.getName(), (Metafidelity) job.getMultiMetaFi());
            if (mFi != null) {
                fiManager.getMorphFidelities().put(mFi.getName(), mFi);
                mFi.addObserver(fiManager);
                if (mFi.getMorpherFidelity() != null) {
                    // set the default morpher
                    mFi.setMorpher((Morpher) ((Entry) mFi.getMorpherFidelity().get(0)).getValue());
                }
            }
        }

        if (connList != null) {
            for (MapContext conn : connList) {
                if (conn.direction == MapContext.Direction.IN)
                    ((ServiceContext)job.getDataContext()).getMogramStrategy().setInConnector(conn);
                else
                    ((ServiceContext)job.getDataContext()).getMogramStrategy().setOutConnector(conn);
            }
        }

        if (exertions.size() > 0) {
            for (Subroutine ex : exertions) {
                job.addMogram(ex);
            }
            for (Pipe p : pipes) {
//				logger.debug("from context: "
//						+ ((Subroutine) p.in).getDataContext().getName()
//						+ " contextReturn: " + p.inPath);
//				logger.debug("to context: "
//						+ ((Subroutine) p.out).getDataContext().getName()
//						+ " contextReturn: " + p.outPath);
                // find component disciplines for thir paths
                if (!p.isExertional()) {
                    p.out = (Subroutine)job.getComponentMogram(p.outComponentPath);
                    p.in = (Subroutine)job.getComponentMogram(p.inComponentPath);
                }
                ((Subroutine) p.out).getDataContext().connect(p.outPath,
                    p.inPath, ((Subroutine) p.in).getContext());
            }
        } else
            throw new RoutineException("No component exertion defined.");

        unifyFiManager(job);
        return job;
    }

    static private void unifyFiManager(Job job) {
        List<Mogram> Mogram = job.getMograms();
        FidelityManager root = (FidelityManager)job.getFidelityManager();
        if (root != null) {
            FidelityManager child = null;
            for (Mogram m : Mogram) {
                child = (FidelityManager) m.getFidelityManager();
                root.getFidelities().putAll(child.getFidelities());
                root.getMetafidelities().putAll(child.getMetafidelities());
                root.getMorphFidelities().putAll(child.getMorphFidelities());
                ((ServiceMogram) m).setFidelityManager(root);
            }
        }
    }

    public static Object get(Context context) throws ContextException,
        RemoteException {
        return context.getReturnValue();
    }

    public static Object get(Context context, int index)
        throws ContextException {
        if (context instanceof PositionalContext)
            return ((PositionalContext) context).getValueAt(index);
        else
            throw new ContextException("Not PositionalContext, index: " + index);
    }

    public static Object returnValue(Mogram mogram) throws ContextException,
        RemoteException {
        return mogram.getContext().getReturnValue();
    }

    public static <T extends Evaluation> Object asis(T evaluation) throws EvaluationException {
        if (evaluation instanceof Evaluation) {
            try {
                synchronized (evaluation) {
                    return evaluation.asis();
                }
            } catch (RemoteException e) {
                throw new EvaluationException(e);
            }
        } else {
            throw new EvaluationException(
                "asis eval can only be determined for objects of the "
                    + Evaluation.class + " fiType");
        }
    }

    public static <V> V take(Function<V> variability)
        throws EvaluationException {
        try {
            synchronized (variability) {
                variability.valueChanged(null);
                V val = variability.evaluate();
                variability.valueChanged(null);
                return val;
            }
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
    }

    public static Mogram sub(Subroutine mogram, String path) {
        return mogram.getComponentMogram(path);
    }


    public static Object get(Service service, String path)
        throws ContextException {
        Object obj = null;
        if (service instanceof Context) {
            obj = ((ServiceContext) service).get(path);
            if (obj != null && obj instanceof Contextion) {
                while (obj instanceof Contextion ||
                    (obj instanceof Reactive && ((Reactive) obj).isReactive())) {
                    try {
                        obj = ((Evaluation) obj).asis();
                    } catch (RemoteException e) {
                        throw new ContextException(e);
                    }
                }
            }
        } else if (service instanceof Mogram) {
            obj = (((Mogram) service).getContext()).asis(path);
        }
        return obj;
    }

    public static Object get(Subroutine exertion, String component, String path)
        throws RoutineException {
        Subroutine c = (Subroutine) exertion.getMogram(component);
        try {
            return get((ServiceRoutine) c, path);
        } catch (Exception e) {
            throw new RoutineException(e);
        }
    }

    public static <T> T softValue(Context<T> context, String path) throws ContextException {
        return context.getSoftValue(path);
    }

    public static <K, V> V keyValue(Map<K, V> map, K path) throws ContextException {
        return map.get(path);
    }

    public static <K, V> V pathValue(Map<K, V> map, K path) throws ContextException {
        return map.get(path);
    }

    public static <V> V pathValue(Context<V> map, String path, Arg... args) throws ContextException {
        try {
            return map.getValue(path, args);
        } catch (RemoteException e) {
            throw new ContextException(e);
        }
    }

    public static Object content(URL url) throws EvaluationException {
        try {
            if (url instanceof URL) {
                return url.getContent();
            } else {
                throw new EvaluationException("Expected URL for its content");
            }
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
    }

    /**
     * Associates a given contextReturn in this context with a tag
     * defined for this context. If a tag, for example, is
     * "triplet|one|two|three" then its tuple can be "triplet|mike|w|sobol" where
     * 'triplet' is the key of relation and its proper tuple is 'mike|w|sobol'.
     *
     * @param context
     * @param path
     * @param association
     * @return
     * @throws ContextException
     */
    public static Context tag(Context context, String path, String association)
        throws ContextException {
        return context.mark(path, association);
    }

    public static Context tag(Context context, Path path)
        throws ContextException {
        return context.mark(path.path, path.info.toString());
    }

    public static <T> List<T> valuesAt(Context<T> context, String association) throws ContextException {
        return context.getMarkedValues(association);
    }

    public static String[] pathsAt(Context context, String association) throws ContextException {
        return context.getMarkedPaths(association);
    }

    public static <T> T valueAt(Context<T> context, String association) throws ContextException {
        return valuesAt(context, association).get(0);
    }

    public static <T> T valueAt(Context<T> context, int i) throws ContextException {
        if (!(context instanceof Positioning))
            throw new ContextException("Not positional Context: " + context.getName());
        return context.getMarkedValues("i|" + i).get(0);
    }

    public static <T> List<T> select(Context<T> context, int... positions) throws ContextException {
        List<T> values = new ArrayList<T>(positions.length);
        for (int i : positions) {
            values.add(valueAt(context, i));
        }
        return values;
    }

    public static Mogram exertion(Subroutine exertion, String componentExertionName) {
        return exertion.getComponentMogram(componentExertionName);
    }
    public static Mogram xrt(Subroutine exertion, String componentExertionName) {
        return exertion.getComponentMogram(componentExertionName);
    }

    public static Mogram tracable(Mogram mogram) {
        List<Mogram> mograms = ((ServiceMogram) mogram).getAllMograms();
        for (Mogram m : mograms) {
            ((Subroutine) m).getControlContext().setTracable(true);
        }
        return mogram;
    }

    public static List<String> trace(Mogram mogram) {
        List<Mogram> mograms = ((ServiceMogram)mogram).getAllMograms();
        List<String> trace = new ArrayList<String>();
        for (Mogram m : mograms) {
            trace.addAll(((Subroutine) m).getControlContext().getTrace());
        }
        return trace;
    }

    public static List<ServiceFidelity>  fiTrace(Mogram mogram) {
        try {
            return mogram.getFidelityManager().getFiTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void print(Object obj) {
        System.out.println(obj.toString());
    }

    public static OutputValue output(Object value) {
        return new OutputValue(null, value, 0);
    }

    public static Signature space(Signature signature) {
        ((ServiceSignature)signature).setAccessType(Access.PULL);
        return signature;
    }

    public static Context.Return result(String path) {
        return new Context.Return(path);
    }

    public static Context.Return result(Context.Out paths) {
        return new Context.Return(null, paths);
    }

    public static Context.Return result(SessionPaths sessionPaths) {
        return new Context.Return(null, sessionPaths);
    }

    public static Context.Return result(String path, SessionPaths sessionPaths) {
        return new Context.Return(path, sessionPaths);
    }

    public static SessionPaths session(Paths... pathsArray) {
        return new SessionPaths(pathsArray);
    }

    public static Context.Return self() {
        return new Context.Return();
    }

    public static Context.Return result(String path, Context.Out outPaths) {
        return new Context.Return(path, outPaths);
    }

    public static Context.Return result(String path, Context.In inPaths) {
        return new Context.Return(path, inPaths);
    }

    public static Context.Return result(Path path, Context.In inPaths) {
        return new Context.Return(path, inPaths);
    }
    public static Context.Return result(Context.In inPaths) {
        return new Context.Return(Signature.SELF, inPaths);
    }

    public static Context.Return result(String path, Context.In inPaths, Context.Out outPaths) {
        return new Context.Return(path, inPaths, outPaths);
    }

    public static Context.Return result(String path, Context.In inPaths, Context.Out outPaths, SessionPaths sessionPaths) {
        return new Context.Return(path, inPaths, outPaths, sessionPaths);
    }

    public static Context.Return result(String path, Direction direction) {
        return new Context.Return(path, direction);
    }

    public static Context.Return result(String path, Direction direction,
                                               Path[] paths) {
        return new Context.Return(path, direction, paths);
    }

    public static Context.Return result(String path, Class type, Path[] paths) {
        return new Context.Return(path, Direction.OUT, type, paths);
    }

    public static String getUnknown() {
        return "unknown" + count++;
    }


    public static class Range extends Slot<Integer, Integer> {
        private static final long serialVersionUID = 1L;
        public Integer[] range;

        public Range(Integer from, Integer to) {
            this.key = from;
            this.out = to;
        }

        public Range(Integer[] range) {
            this.range = range;
        }

        public Integer[] range() {
            return range;
        }

        public int from() {
            return key;
        }

        public int to() {
            return out;
        }

        public String toString() {
            if (range != null)
                return Arrays.toString(range);
            else
                return "[" + key + "-" + out + "]";
        }
    }

    // putLink(String key, String contextReturn, Context linkedContext, String offset)
    public static Object link(Context context, String path,
                              Context linkedContext, String offset) throws ContextException {
        context.putLink(null, path, linkedContext, offset);
        return context;
    }

    public static Context link(Context context, String path,
                               Context linkedContext) throws ContextException {
        context.putLink(null, path, linkedContext, "");
        return context;
    }

    public static Link getLink(Context context, String path) throws ContextException {
        return context.getLink(path);
    }

    public static <T> ControlContext strategy(T... entries) throws ContextException {
        ControlContext cc = new ControlContext();
        List<Signature> sl = new ArrayList<Signature>();
        for (Object o : entries) {
            if (o instanceof Access) {
                cc.setAccessType((Access) o);
            } else if (o instanceof Flow) {
                cc.setFlowType((Flow) o);
            } else if (o instanceof Monitor) {
                cc.isMonitorable((Monitor) o);
            } else if (o instanceof Provision) {
                if (o.equals(Provision.YES))
                    cc.setProvisionable(true);
                else
                    cc.setProvisionable(false);
            } else if (o instanceof Strategy.Shell) {
                if (o.equals(Strategy.Shell.REMOTE))
                    cc.setShellRemote(true);
                else
                    cc.setShellRemote(false);
            } else if (o instanceof Wait) {
                cc.isWait((Wait) o);
            } else if (o instanceof Signature) {
                sl.add((Signature) o);
            } else if (o instanceof Opti) {
                cc.setOpti((Opti) o);
            } else if (o instanceof Exec.State) {
                cc.setExecState((Exec.State) o);
            } else if (o instanceof Entry) {
                cc.put(((Entry)o).getName(), ((Entry)o).getValue());
            }
        }
        cc.setSignatures(sl);
        return cc;
    }

    public static Flow flow(Entry entry) throws ContextException {
        return ((Strategy) entry.getData()).getFlowType();
    }

    public static Access access(Entry entry) throws ContextException {
        return ((Strategy) entry.getData()).getAccessType();
    }

    public static Flow flow(Strategy strategy) {
        return strategy.getFlowType();
    }

    public static Access access(Strategy strategy) {
        return strategy.getAccessType();
    }

    public static EntryList inputs(Value...  entries) {
        return designInputs(entries);
    }

    public static EntryList designInputs(Value...  entries) {
        EntryList el = new EntryList(entries);
        el.setType(Functionality.Type.INITIAL_DESIGN);
        return el;
    }

    public static EntryList initialDesign(Value...  entries) {
        return designInputs(entries);
    }

    public static PathResponse target(Object object) {
        return new PathResponse(object);
    }

    public static class PathResponse extends Path {

        private static final long serialVersionUID = 1L;

        public Object target;

        public PathResponse(Object target) {
            this.target = target;
        }

        public PathResponse(String path, Object target) {
            this.target = target;
            this.path = path;
        }

        @Override
        public String toString() {
            return "target: " + target;
        }
    }

    public static class result extends Entry {

        private static final long serialVersionUID = 1L;

        Class returnType;

        result(String path) {
            this.key = path;
        }

        result(String path, Class returnType) {
            this.key = path;
            this.impl = returnType;
        }

        public Class returnPath() {
            return (Class) this.impl;
        }

        @Override
        public String toString() {
            return "return contextReturn: " + impl;
        }
    }

    public static ParameterTypes types(Class... parameterTypes) {
        return new ParameterTypes(parameterTypes);
    }

    public static class ParameterTypes extends Path {
        private static final long serialVersionUID = 1L;
        public Class[] parameterTypes;

        public ParameterTypes(Class... parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        public ParameterTypes(Class basicType, Class... parameterTypes) {
            Class[] types = new Class[parameterTypes.length+1];
            types[0] = basicType;
            for (int i = 0; i < parameterTypes.length; i++) {
                types[i+1] = parameterTypes[i];
            }
            this.parameterTypes = types;
        }

        public ParameterTypes(String path, Class... parameterTypes) {
            this.parameterTypes = parameterTypes;
            this.path = path;
        }

        @Override
        public String toString() {
            return "types: " + Arrays.toString(parameterTypes);
        }
    }

    public static Args signals(Object... args) {
        return new Args(args);
    }

    public static Args args(Object... args) {
        if (args == null) {
            return new Args();
        }
        Paths paths = null;
        List<Object> objs = new ArrayList();
        Paths ps = new Paths();
        for (Object o : args) {
            if (o instanceof Paths) {
                paths = (Paths) o;
            } else {
                // a contextReturn in args is used as dependent entry
                // and also as a  contextReturn in context
                // as all args are appended to context defined by a Paths
                if (o instanceof Path) {
                    ps.add((Path) o);
                    objs.add(((Path) o).path);
                } else {
                    objs.add(o);
                }
            }
        }

        if (ps.size() > 0) {
            if (paths == null) {
                paths = ps;
            } else {
                paths.addAll(ps);
            }
        }

        return new Args(objs, paths);
    }

    public static class Args extends Path implements SupportComponent {
        private static final long serialVersionUID = 1L;

        public Object[] args = new Object[0];

        // paths of a model as inputs in a service context used
        // in exertions evaluators associated with a signature
        public Paths paths;

        public Args() { }

        public Args(Object[] args) {
            this.args = args;
        }

        public Args(List<Object> args, Paths paths) {
            Object[] objs = new Object[args.size()];
            args.toArray(objs);
            this.args = objs;
            this.paths = paths;
        }
        public Args(String path, Object... args) {
            this.args = args;
            this.path = path;
        }

        public Arg[] args() {
            Arg[] as = new Arg[args.length];
            for (int i = 0; i < args.length; i++) {
                Function sub = new Function(args[i].toString());
                if (paths != null) {
                    Path p = paths.getPath(args[i].toString());
                    if (p != null && p.type.equals(Type.PROC)) {
                        sub.setDomain((p.domain));
                        sub.setType(Functionality.Type.PROC);
                    }
                }
                as[i] = sub;
            }
            return as;
        }

        public ArgSet argSet() {
            ArgSet as = new ArgSet();
            for (int i = 0; i < args.length; i++) {
                Function sub = new Function(args[i].toString());
                if (paths != null) {
                    Path p = paths.getPath(args[i].toString());
                    if (p != null && p.type.equals(Type.PROC)) {
                        sub.setDomain((p.domain));
                        sub.setType(Functionality.Type.PROC);
                    }
                }
                as.add(sub);
            }
            as.paths = this.paths;
            return as;
        }

        public String[] getNameArray() {
            String[] as = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                as[i] = args[i].toString();
            }
            return as;
        }

        public List<String> getNameList() {
            List<String>  sl = new ArrayList(args.length);
            for (int i = 0; i < args.length; i++) {
                sl.add(args[i].toString());
            }
            return sl;
        }

        public int size() {
            return args.length;
        }

        @Override
        public String toString() {
            return "args: " + Arrays.toString(args);
        }
    }

    public static class Pipe {
        String inPath;
        String outPath;
        Contextion in;
        Contextion out;
        String outComponentPath;
        String inComponentPath;

        Prc callEntry;

        Pipe(Subroutine out, String outPath, Contextion in, String inPath) {
            this.out = out;
            this.outPath = outPath;
            this.in = in;
            this.inPath = inPath;
            if ((in instanceof Subroutine) && (out instanceof Subroutine)) {
                try {
                    callEntry = new Prc(outPath, inPath, in);
                } catch (ContextException e) {
                    e.printStackTrace();
                }
                ((ServiceRoutine) out).addPersister(callEntry);
            }
        }

        Pipe(OutEndPoint outEndPoint, InEndPoint inEndPoint) {
            this.out = outEndPoint.out;
            this.outPath = outEndPoint.outPath;
            this.outComponentPath = outEndPoint.outComponentPath;
            this.in = inEndPoint.in;
            this.inPath = inEndPoint.inPath;
            this.inComponentPath = inEndPoint.inComponentPath;

            if ((in instanceof Subroutine) && (out instanceof Subroutine)) {
                try {
                    callEntry = new Prc(outPath, inPath, in);
                } catch (ContextException e) {
                    e.printStackTrace();
                }
                ((ServiceRoutine) out).addPersister(callEntry);
            }
        }

        public boolean isExertional() {
            return in != null && out != null;
        }
    }

    public static Prc persistent(Pipe pipe) {
        pipe.callEntry.setPersistent(true);
        return pipe.callEntry;
    }

    private static class InEndPoint {
        String inPath;
        Contextion in;
        String inComponentPath;

        InEndPoint(Contextion in, String inDataPath) {
            this.inPath = inDataPath;
            this.in = in;
        }

        InEndPoint(String inComponentPath, String inDataPath) {
            this.inPath = inDataPath;
            this.inComponentPath = inComponentPath;
        }
    }

    private static class OutEndPoint {
        public String outPath;
        public Contextion out;
        public String outComponentPath;

        OutEndPoint(Contextion out, String outDataPath) {
            this.outPath = outDataPath;
            this.out = out;
        }

        OutEndPoint(String outComponentPath, String outDataPath) {
            this.outPath = outDataPath;
            this.outComponentPath = outComponentPath;
        }
    }

    public static OutEndPoint outPoint(String outComponent, String outPath) {
        return new OutEndPoint(outComponent, outPath);
    }

    public static OutEndPoint outPoint(Mogram outExertion, String outPath) {
        return new OutEndPoint((Subroutine)outExertion, outPath);
    }

    public static InEndPoint inPoint(String inComponent, String inPath) {
        return new InEndPoint(inComponent, inPath);
    }

    public static InEndPoint inPoint(Mogram inExertion, String inPath) {
        return new InEndPoint((Subroutine)inExertion, inPath);
    }

    public static Pipe pipe(OutEndPoint outEndPoint, InEndPoint inEndPoint) {
        Pipe p = new Pipe(outEndPoint, inEndPoint);
        return p;
    }

    public static class Complement<T> extends Entry<T> {
        private static final long serialVersionUID = 1L;

        Complement(String path, T value) {
            this.key = path;
            this.out = value;
        }
    }

    public static List<Exerter> providers(Signature signature)
        throws SignatureException {
        ServiceTemplate st = new ServiceTemplate(null, new Class[] { signature.getServiceType() }, null);
        ServiceItem[] sis = Accessor.get().getServiceItems(st, null);
        if (sis == null)
            throw new SignatureException("No available providers of fiType: "
                + signature.getServiceType().getName());
        List<Exerter> servers = new ArrayList<Exerter>(sis.length);
        for (ServiceItem si : sis) {
            servers.add((Exerter) si.service);
        }
        return servers;
    }

    public static List<Class<?>> interfaces(Object obj) {
        if (obj == null)
            return null;
        return Arrays.asList(obj.getClass().getInterfaces());
    }

    public static Object provider(Signature signature)
        throws SignatureException {
        return prv(signature);
    }

    public static Object prv(Signature signature)
        throws SignatureException {
        if (signature instanceof ObjectSignature && ((ObjectSignature)signature).getTarget() != null)
            return  ((ObjectSignature)signature).getTarget();
        else if (signature instanceof NetletSignature) {
            String source = ((NetletSignature)signature).getServiceSource();
            if(source != null) {
                try {
                    ServiceScripter se = new ServiceScripter(System.out, null, Sorcer.getWebsterUrl(), true);
                    se.readFile(new File(source));
                    return se.interpret();
                } catch (Throwable e) {
                    throw new SignatureException(e);
                }
            }
        }
        Object target = null;
        Object provider = null;
        Signature targetSignatue = null;
        Class providerType = signature.getServiceType();
        if (signature.getClass() == ObjectSignature.class) {
            target = ((ObjectSignature) signature).getTarget();
            targetSignatue = ((ObjectSignature) signature).getTargetSignature();
        }
        try {
            if (signature.getClass() == NetSignature.class) {
                provider = ((NetSignature) signature).getService();
                if (provider == null) {
                    provider = Accessor.get().getService(signature);
                    ((NetSignature) signature).setProvider((Service)provider);
                }
            } else if (signature.getClass() == ObjectSignature.class) {
                if (target != null) {
                    provider = target;
                } else if (targetSignatue != null) {
                    provider = instance(targetSignatue);
                    ((ObjectSignature)signature).setTarget(provider);
                } else if (Exerter.class.isAssignableFrom(providerType)) {
                    provider = providerType.newInstance();
                } else {
                    if (signature.getSelector() == null &&
                        (((ObjectSignature)signature).getInitSelector())== null) {
                        provider = ((ObjectSignature) signature).getProviderType().newInstance();
                    } else if (signature.getSelector().equals(((ObjectSignature)signature).getInitSelector())) {
                        // utility class returns a utility (class) method
                        provider = ((ObjectSignature) signature).getProviderType();
                    } else {
                        provider = sorcer.co.operator.instance(signature);
                        ((ObjectSignature)signature).setTarget(provider);
                    }
                }
            } else if (signature instanceof ModelSignature) {
                if (target != null) {
                    provider = target;
                } else if (ServiceModeler.class.isAssignableFrom(providerType)) {
                    provider = providerType.newInstance();
                } else if (providerType.isInterface()
                    && (Modeler.class.isAssignableFrom(providerType))) {
                    provider = Accessor.create().getService(signature);
                }
            } else if (signature instanceof EvaluationSignature) {
                provider = ((EvaluationSignature) signature).getEvaluator();
            }
        } catch (Exception e) {
            throw new SignatureException("No provider available", e);
        }
        return provider;
    }

    public static Condition condition(EntModel parcontext, String expression,
                                      String... pars) {
        return new Condition(parcontext, expression, pars);
    }

    public static Condition condition(Closure condition) {
        return new Condition(condition);
    }

    public static <T> Condition condition(ConditionCallable<T> lambda) {
        return new Condition(lambda);
    }

    public static Condition condition(String expression,
                                      String... pars) {
        return new Condition(expression, pars);
    }

    public static Condition condition(boolean condition) {
        return new Condition(condition);
    }

    public static OptTask opt(String name, Mogram target) {
        return new OptTask(name, target);
    }

    public static OptTask opt(Condition condition,
                              Mogram target) {
        return new OptTask(condition, target);
    }


    public static OptTask opt(String name, Condition condition,
                              Mogram target) {
        return new OptTask(name, condition, target);
    }

    public static AltTask alt(OptTask... mograms) {
        return new AltTask(mograms);
    }

    public static AltTask alt(String name, OptTask... mograms) {
        return new AltTask(name, mograms);
    }


    public static LoopTask loop(Condition condition,
                                Mogram target) {
        return new LoopTask(null, condition, target);
    }

    public static LoopTask loop(int from, int to, Condition condition,
                                Mogram target) {
        return new LoopTask(null, from, to, condition, target);
    }

    public static LoopTask loop(int from, int to, Mogram target) {
        return new LoopTask(null, from, to, null, target);
    }

    public static LoopTask loop(String name, Condition condition,
                                Subroutine target) {
        return new LoopTask(name, condition, target);
    }

    public static Subroutine xrt(Contextion mappable, String path)
        throws ContextException {
        Object obj = ((ServiceContext) mappable).asis(path);
        while (obj instanceof Contextion || obj instanceof Prc) {
            try {
                obj = ((Evaluation) obj).asis();
            } catch (RemoteException e) {
                throw new ContextException(e);
            }
        }
        if (obj instanceof Subroutine)
            return (Subroutine) obj;
        else
            throw new NoneException("No such exertion at: " + path + " in: "
                + mappable.getName());
    }

    public static Signature dispatcherSig(Signature signature) {
        ((ServiceSignature)signature).addRank(Kind.DISPATCHER);
        return signature;
    }

    public static Signature modelSig(Signature signature) {
        ((ServiceSignature)signature).addRank(new Kind[]{Kind.MODEL, Kind.TASKER});
        return signature;
    }

    public static Signature modelManagerSig(Signature signature) {
        ((ServiceSignature)signature).addRank(Kind.MODEL, Kind.MODEL_MANAGER);
        return signature;
    }

    public static Signature optiSig(Signature signature) {
        ((ServiceSignature)signature).addRank(Kind.OPTIMIZER, Kind.TASKER);
        return signature;
    }

    public static Signature driverSig(Signature signature) {
        ((ServiceSignature)signature).addRank(Kind.DRIVER);
        return signature;
    }

    public static Signature solverSig(Signature signature) {
        ((ServiceSignature)signature).addRank(Kind.SOLVER);
        return signature;
    }

    public static Signature optimizerSig(Signature signature) {
        ((ServiceSignature)signature).addRank(Kind.OPTIMIZER, Kind.TASKER);
        return signature;
    }

    public static Signature explorerSig(Signature signature) {
        ((ServiceSignature)signature).addRank(Kind.EXPLORER, Kind.TASKER);
        return signature;
    }

    public static Block block(Object...  items) throws RoutineException {
        List<Mogram> mograms = new ArrayList<>();
        List<Evaluator> evaluators = new ArrayList<>();
        String name = null;
        Signature sig = null;
        Context context = null;
        Evaluator evaluator = null;
        for (int i = 0; i < items.length; i++) {
            if (items[i] instanceof Subroutine || items[i] instanceof EntModel) {
                mograms.add((Mogram) items[i]);
            } else if (items[i] instanceof Evaluation) {
                evaluators.add((Evaluator)items[i]);
            } else if (items[i] instanceof Context) {
                context = (Context)items[i];
            } else if (items[i] instanceof Signature) {
                sig = (Signature)items[i];
            } else if (items[i] instanceof String) {
                name = (String)items[i];
            }
        }

        Block block;
        try {
            if (sig != null) {
                if (sig instanceof ObjectSignature)
                    block = new ObjectBlock(name);
                else
                    block = new NetBlock(name);
            } else {
                // default signature
//				block = new NetBlock(key);
                block = new ObjectBlock(name);
            }

            if (context != null) {
                // block scope is its own context
                block.setContext(context);
                ((ServiceContext)context).setScope(context);
                // context for resetting to initial state after cleaning scopes
                ((ServiceContext)context).setInitContext((Context)ObjectCloner.clone(context));
            }

            for (int j = 0; j < mograms.size(); j++) {
                Mogram m = mograms.get(j);
                m.setIndex(j);
                block.addMogram(m);
            }
            for (Evaluator e :evaluators) {
                block.addMogram(new EvaluationTask(e));
            }
        } catch (Exception se) {
            throw new RoutineException(se);
        }
        //make sure it has EntModel as the data context
        EntModel pm = null;
        Context cxt = null;
        try {
            cxt = block.getDataContext();
            if (cxt == null) {
                cxt = new EntModel();
                block.setContext(cxt);
            }
            if (cxt instanceof EntModel) {
                pm = (EntModel)cxt;
            } else {
                pm = new EntModel("block context: " + cxt.getName());
                block.setContext(pm);
                pm.append(cxt);
                pm.setScope(pm);
                pm.setInitContext(context);
            }
            for (Mogram e : mograms) {
                if (e instanceof AltTask) {
                    List<OptTask> opts = ((AltTask) e).getOptExertions();
                    for (OptTask oe : opts) {
                        oe.getCondition().setConditionalContext(pm);
                    }
                } else if (e instanceof OptTask) {
                    ((OptTask)e).getCondition().setConditionalContext(pm);
                } else if (e instanceof LoopTask) {
                    if (((LoopTask)e).getCondition() != null) {
                        ((LoopTask) e).getCondition().setConditionalContext(pm);
                    }

                    Mogram target = ((LoopTask)e).getTarget();
                    if (target instanceof EvaluationTask && ((EvaluationTask)target).getEvaluation() instanceof Entry) {
                        ServiceContext ltcxt = (ServiceContext) ((EvaluationTask)target).getDataContext();
                        if (ltcxt == null || ltcxt.size() == 0) {
                            ((EvaluationTask)target).setContext(pm);
                        }
                        Entry p = (Entry) ((EvaluationTask)target).getEvaluation();
                        if (p.getImpl() instanceof ServiceInvoker) {
                            ((ServiceInvoker)p.getImpl()).setInvokeContext(pm);
                        } else {
                            p.setScope(pm);
                        }
                        if (target instanceof Subroutine && target.getContext().getContextReturn() == null)
                            ((ServiceContext)target.getContext()).setContextReturn(p.getName());
                    }
                } else if (e instanceof EvaluationTask) {
                    e.setScope(pm.getScope());
                    if (((EvaluationTask)e).getEvaluation() instanceof Prc) {
                        Prc p = (Prc)((EvaluationTask)e).getEvaluation();
                        pm.getScope().addPrc(p);
                    }
                } else if (e instanceof Subroutine) {
                    e.getDataContext().setScope(pm.getScope());
                    e.getDataContext().updateEntries(pm.getScope());
                }
            }
        } catch (Exception ex) {
            throw new RoutineException(ex);
        }
        return block;
    }

    public static class Jars {
        public String[] jars;

        Jars(String... jarNames) {
            jars = jarNames;
        }
    }

    public static class CodebaseJars  {
        public String[] jars;

        CodebaseJars(String... jarNames) {
            jars = jarNames;
        }
    }

    public static class Impl {
        public String className;

        Impl(String className) {
            this.className = className;
        }
    }

    public static class Configuration {
        public String configuration;

        Configuration(final String configuration) {
            this.configuration = configuration;
        }
    }

    public static class WebsterUrl {
        public String websterUrl;

        WebsterUrl(String websterUrl) {
            this.websterUrl = websterUrl;
        }
    }

    public static class Multiplicity {
        int multiplicity;
        int maxPerCybernode;
        boolean fixed;

        Multiplicity(int multiplicity) {
            this.multiplicity = multiplicity;
        }

        Multiplicity(int multiplicity, PerNode perNode) {
            this(multiplicity, perNode.number);
        }

        Multiplicity(int multiplicity, int maxPerCybernode) {
            this.multiplicity = multiplicity;
            this.maxPerCybernode = maxPerCybernode;
        }

        Multiplicity(int multiplicity, Fixed fixed) {
            this.multiplicity = multiplicity;
            this.fixed = fixed!=null;
        }
    }

    public static class Idle {
        public final int idle;

        Idle(final int idle) {
            this.idle = idle;
        }

        Idle(final String idle) {
            this.idle = ServiceDeployment.parseInt(idle);
        }
    }

    public static class PerNode {
        public final int number;

        PerNode(final int number) {
            this.number = number;
        }
    }

    public static class IP {
        final Set<String> ips = new HashSet<String>();
        boolean exclude;

        public IP(final String... ips) {
            Collections.addAll(this.ips, ips);
        }

        void setExclude(final boolean exclude) {
            this.exclude = exclude;
        }

        public String[] getIps() {
            return ips.toArray(new String[ips.size()]);
        }
    }

    public static class Arch {
        final String arch;

        public Arch(final String arch) {
            this.arch = arch;
        }

        public String getArch() {
            return arch;
        }
    }

    public static class OpSys {
        final Set<String> opSys = new HashSet<String>();

        public OpSys(final String... opSys) {
            Collections.addAll(this.opSys, opSys);
        }

        public String[] getOpSys() {
            return opSys.toArray(new String[opSys.size()]);
        }
    }

    static class Fixed {
        Fixed() {}
    }

    public static Configuration configFile(String filename) {
        return new Configuration(filename);
    }

    public static PerNode perNode(int number) {
        return new PerNode(number);
    }

    public static Jars classpath(String... jarNames) {
        return new Jars(jarNames);
    }

    public static CodebaseJars codebase(String... jarNames) {
        return new CodebaseJars(jarNames);
    }

    public static Impl implementation(String className) {
        return new Impl(className);
    }

    public static WebsterUrl webster(String WebsterUrl) {
        return new WebsterUrl(WebsterUrl);
    }

    public static Configuration configuration(String configuration) {
        return new Configuration(configuration);
    }

    public static Multiplicity maintain(int planned) {
        return new Multiplicity(planned);
    }

    public static Multiplicity maintain(int planned, int maxPerCybernode) {
        return new Multiplicity(planned, maxPerCybernode);
    }

    public static Multiplicity maintain(int planned, PerNode perNode) {
        return new Multiplicity(planned, perNode);
    }

    public static Multiplicity maintain(int planned, Fixed fixed) {
        return new Multiplicity(planned, fixed);
    }

    public static Idle idle(String idle) {
        return new Idle(idle);
    }

    public static Idle idle(int idle) {
        return new Idle(idle);
    }

    public static IP ips(String... ips) {
        return new IP(ips);
    }

    public static IP ipsExclude(String... ips) {
        IP ip = new IP(ips);
        ip.exclude = true;
        return ip;
    }

    public static Arch arch(String arch) {
        return new Arch(arch);
    }

    public static OpSys opsys(String... opsys) {
        return new OpSys(opsys);
    }

    public static Fixed fixed() {
        return new Fixed();
    }

    public static <T> ServiceDeployment deploy(T... elems) {
        ServiceDeployment deployment = new ServiceDeployment();
        for (Object o : elems) {
            if (o instanceof Jars) {
                deployment.setClasspathJars(((Jars) o).jars);
            } else if (o instanceof CodebaseJars) {
                deployment.setCodebaseJars(((CodebaseJars) o).jars);
            } else if (o instanceof Configuration) {
                deployment.setConfig(((Configuration) o).configuration);
            } else if (o instanceof Impl) {
                deployment.setImpl(((Impl) o).className);
            } else if (o instanceof Multiplicity) {
                Multiplicity m = (Multiplicity)o;
                deployment.setMultiplicity(m.multiplicity);
                deployment.setMaxPerCybernode(m.maxPerCybernode);
                if(m.fixed)
                    deployment.setStrategy(Deployment.Strategy.FIXED);
            } else if(o instanceof Fixed) {
                deployment.setStrategy(Deployment.Strategy.FIXED);
            } else if(o instanceof ServiceDeployment.Type) {
                deployment.setType(((ServiceDeployment.Type) o));
            } else if (o instanceof Idle) {
                deployment.setIdle(((Idle) o).idle);
            } else if (o instanceof PerNode) {
                deployment.setMaxPerCybernode(((PerNode)o).number);
            } else if (o instanceof IP) {
                IP ip = (IP)o;
                for(String ipAddress : ip.getIps()) {
                    try {
                        InetAddress inetAddress = InetAddress.getByName(ipAddress);
                        if(!inetAddress.isReachable(1000)) {
                            logger.warn(getWarningBanner("The signature declares an ip address or hostname.\n" +
                                ipAddress+" is not reachable on the current network"));
                        }
                    } catch (Exception e) {
                        logger.warn(getWarningBanner(ipAddress+" is not found on the current network.\n"
                            +e.getClass().getName()+": "+e.getMessage()));
                    }
                }
                if(ip.exclude) {
                    deployment.setExcludeIps(ip.getIps());
                } else {
                    deployment.setIps(ip.getIps());
                }
            } else if (o instanceof Arch) {
                deployment.setArchitecture(((Arch)o).getArch());
            } else if (o instanceof OpSys) {
                deployment.setOperatingSystems(((OpSys) o).getOpSys());
            } else if (o instanceof WebsterUrl) {
                deployment.setWebsterUrl(((WebsterUrl)o).websterUrl);
            }
        }
        return deployment;
    }

    public static Subroutine add(Subroutine compound, Subroutine component)
        throws RoutineException {
        compound.addMogram(component);
        return compound;
    }

    public static Block block(Loop loop, Subroutine exertion)
        throws RoutineException, SignatureException {
        List<String> names = loop.getNames(exertion.getName());
        Block block;
        if (exertion instanceof NetTask || exertion instanceof NetJob
            || exertion instanceof NetBlock) {
            block = new NetBlock(exertion.getName() + "-block");
        } else {
            block = new ObjectBlock(exertion.getName() + "-block");
        }
        Subroutine xrt = null;
        for (String name : names) {
            xrt = (Subroutine) ObjectCloner.cloneAnnotatedWithNewIDs(exertion);
            ((ServiceRoutine) xrt).setName(name);
            block.addMogram(xrt);
        }
        return block;
    }

    public static Version version(String ver) {
        return new Version(ver);
    }


    private static String getWarningBanner(String message) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n****************************************************************\n");
        builder.append(message).append("\n");
        builder.append("****************************************************************\n");
        return builder.toString();
    }

}
