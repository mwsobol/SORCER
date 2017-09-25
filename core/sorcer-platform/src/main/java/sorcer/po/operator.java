/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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
package sorcer.po;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.Operator;
import sorcer.co.tuple.*;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.*;
import sorcer.core.context.model.ent.Proc;
import sorcer.core.context.model.srv.Srv;
import sorcer.core.invoker.*;
import sorcer.core.plexus.MorphFidelity;
import sorcer.core.plexus.MultiFiMogram;
import sorcer.service.*;
import sorcer.service.Domain;
import sorcer.service.modeling.Model;
import sorcer.eo.operator.Args;
import sorcer.service.modeling.SupportComponent;
import sorcer.service.modeling.Functionality;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static sorcer.eo.operator.context;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class operator extends Operator {

	private static final Logger logger = LoggerFactory.getLogger(operator.class.getName());

	public static Neo neo(String path, double signal) {
		return new Neo(path, signal);
	}

    public static Neo neo(String path, Args signals) {
        return new Neo(path, signals);
    }

    public static Neo neo(String path, Context<Float> weights,  Args signals) {
        return new Neo(path, weights, signals);
    }

    public static Neo neo(String path, Context<Float> weights, double signal, Args signals) {
        return new Neo(path, weights, signals);
    }

    public static Neo neo(String path, ServiceFidelity fidelities) {
		return new Neo(path, fidelities);
	}

    public static Function th(String path, double threshold) {
	    Function e = new Function(path, threshold);
	    e.setType(Functionality.Type.THRESHOLD);
        return e;
    }

    public static Function bias(String path, double bias) {
        Function e = new Function(path, bias);
        e.setType(Functionality.Type.BIAS);
        return e;
    }

    public static <T> Proc<T> proc(String path, T argument) throws EvaluationException, RemoteException {
		return new Proc(path, argument);
	}

	public static Proc dbEnt(String path, Object argument) throws EvaluationException, RemoteException {
		Proc p = new Proc(path, argument);
		p.setPersistent(true);
		p.getValue();
		return p;
	}

	public static Proc proc(Identifiable identifiable, Context context) throws EvaluationException, RemoteException {
		Proc p = new Proc(identifiable.getName(), identifiable);
		if (identifiable instanceof Scopable) {
			((Scopable) identifiable).setScope(context);
		}
		p.setScope(context);
		return p;
	}

	public static Proc as(Proc proc, Service traget) {
		proc.setMappable((Mappable)traget);
		return proc;
	}

	public static Proc proc(Mappable argument, String name, String path) {
		Proc p = new Proc(argument, name, path);
		return p;
	}

	public static Proc proc(String path, Object argument, Object object) throws ContextException, RemoteException {
		Proc p = null;
		if (object instanceof Context) {
			p = new Proc(path, argument);
			p.setScope(object);
		} else if (object instanceof Function) {
			p = new Proc(path, argument);
			p.setScope(context((Function)object));
		} else if (object instanceof Service) {
			p = new Proc(path, argument, object);
		}
		return p;
	}

	public static Srv srv(ServiceFidelity fidelity) {
		Srv service = new Srv(fidelity.getName(), fidelity);
		return service;
	}

	public static Srv srv(String name, ServiceFidelity fidelity) {
		Srv service = new Srv(name, fidelity);
		return service;
	}

	public static Srv srv(String name, MorphFidelity fidelity) {
		Srv service = new Srv(name, fidelity);
		return service;
	}

	public static Srv srv(String name, Identifiable item) {
		return srv(name,  item,  null);
	}

	public static Srv srv(Identifiable item, Context context) {
		return srv(null,  item,  context);
	}

	public static Srv srv(String name, Identifiable item, Context context, Arg... args) {
		String srvName = item.getName();
		Srv srv = null;
		if (name != null) {
            srvName = name;
        }
		if (item instanceof Signature) {
			srv = new Srv(srvName,
					new SignatureEntry(item.getName(), (Signature) item, context));
		} else if (item instanceof Mogram) {
			srv = new Srv(srvName,
					new MogramEntry(item.getName(), (Mogram) item));
		} else {
			srv = new Srv(srvName, item);
		}
		try {
			srv.substitute(args);
		} catch (SetterException e) {
			e.printStackTrace();
		}
		return srv;
	}

	public static Srv srv(Identifiable item) {
		return srv(null, item);
	}

	public static Srv srv(String name, String path, Model model) {
		return new Srv(path, model, name);
	}

	public static Srv srv(String name, String path, Model model, Functionality.Type type) {
		return new Srv(path, model, name, type);
	}

	public static Srv aka(String name, String path) {
		return new Srv(name, null, path);
	}

	public static Srv alias(String name, String path) {
		return new Srv(path, null, name);
	}
	public static Proc dPar(Identifiable identifiable, Context context) throws EvaluationException, RemoteException {
		Proc p = new Proc(identifiable.getName(), identifiable);
		p.setPersistent(true);
		p.setScope(context);
		return p;
	}

	public static Proc dbEnt(String path, Object argument, Context context) throws EvaluationException, RemoteException {
		Proc p = new Proc(path, argument);
		p.setPersistent(true);
		p.setScope(context);
		return p;
	}

	public static Proc pipe(Mappable in, String name, String path, Service out) throws ContextException {
		Proc p = new Proc(name, path, out);
		add(p, in);
		return p;
	}

	public static Proc storeUrl(Proc procEntry, URL url) {
		procEntry.setDbURL(url);
		return procEntry;
	}

	public static Proc proc(ProcModel pm, String name) throws ContextException, RemoteException {
		Proc parameter = new Proc(name, pm.asis(name));
		parameter.setScope(pm);
		return parameter;
	}

    public static Fidelity pFi(String name) {
        Fidelity fi =  new Fidelity(name);
        fi.setType(Fi.Type.PROC);
        return fi;
    }

    public static ServiceFidelity pFi(Function... entries) {
        ServiceFidelity fi = new ServiceFidelity(entries);
        fi.fiType = ServiceFidelity.Type.PROC;
        return fi;
    }

	public static Invocation invoker(Mappable mappable, String path)
			throws ContextException {
		Object obj = mappable.asis(path);
		while (obj instanceof Mappable || obj instanceof Proc) {
			try {
				obj = ((Evaluation) obj).asis();
			} catch (RemoteException e) {
				throw new ContextException(e);
			}
		}
		if (obj instanceof Invocation)
			return (Invocation) obj;
		else if (obj != null) {
			if (obj instanceof Double)
				return new DoubleIncrementor(path, null, (Double) obj);
			if (obj instanceof Integer)
				return new IntegerIncrementor(path, null, (Integer) obj);
		}
		throw new NoneException("No such invoker at: " + path + " in: " + mappable.getName());
	}

	public static void clearPars(Object invoker) throws EvaluationException {
		if (invoker instanceof ServiceInvoker)
			((ServiceInvoker)invoker).clearPars();
	}

	public static ProcModel procModel(Identifiable... objects)
			throws ContextException, RemoteException {
		return new ProcModel(objects);
	}

	public static ProcModel add(ProcModel procModel, Identifiable... objects)
			throws RemoteException, ContextException {
		procModel.add(objects);
		return procModel;
	}

	public static ProcModel append(ProcModel parContext, Arg... objects)
			throws RemoteException, ContextException {
		parContext.append(objects);
		return parContext;
	}

	public static Proc put(ProcModel procModel, String name, Object value) throws ContextException, RemoteException {
		procModel.putValue(name, value);
		procModel.setContextChanged(true);
		return proc(procModel, name);
	}

	public static ProcModel put(ProcModel procModel, Entry... entries) throws ContextException {
		for (Entry e : entries) {
			procModel.putValue(e.getName(), e.getItem());
		}
		procModel.setContextChanged(true);
		return procModel;
	}

	public static Proc add(Proc procEntry, Object to)
			throws ContextException {
		if (to instanceof Exertion) {
			((ServiceExertion)to).addPersister(procEntry);
			return procEntry;
		}
		return procEntry;
	}

	public static Proc connect(Object to, Proc procEntry)
			throws ContextException {
		return add(procEntry, to);
	}

	public static Proc proc(Object object) throws EvaluationException, RemoteException {
		if (object instanceof String)
			return new Proc((String)object);
		else if (object instanceof Identifiable)
			return new Proc(((Identifiable) object).getName(), object);
		return null;
	}

	public static Proc func() {
		GroovyInvoker gi = new GroovyInvoker();
		return new Proc(gi.getName(), gi);
	}

    public static Proc func(String expression,  Arg... parameters) {
        return fun(null, expression, null, parameters);
    }

	public static Proc func(String expression, Context context,  Arg... parameters) {
		return fun(null, expression, context, parameters);
	}

	public static Proc fun(String path, String expression, Context context,  Arg... parameters) {
		GroovyInvoker gi = new GroovyInvoker(expression, parameters);
		if (context != null) {
            gi.setScope(context);
        }
		String name = path;
		if (path == null) {
			name = gi.getName();
		}
		return new Proc(name, gi);
	}

	public static Proc proc(Invocation invoker) {
		return new Proc(((ServiceInvoker)invoker).getName(), invoker);
	}

	public static Proc proc(String path, Invocation invoker) {
		return new Proc(path, invoker);
	}

	public static Object invoke(Invocation invoker, Arg... parameters)
			throws ContextException, RemoteException {
		return invoker.invoke(null, parameters);
	}

	public static Object invoke(Invocation invoker, Context context, Arg... parameters)
			throws ContextException, RemoteException {
		return invoker.invoke(context, parameters);
	}

    public static Object activate(ProcModel procModel, String parname, Arg... parameters)
            throws RemoteException, InvocationException {
	    return invoke(procModel, parname, parameters);
    }

	public static Object invoke(ProcModel procModel, String parname, Arg... parameters)
			throws RemoteException, InvocationException {
		try {
			Object obj = procModel.asis(parname);
			Context scope = null;
			// assume that the first argument is always context if provided
			if (parameters.length > 0 && parameters[0] instanceof Context)
				scope = (Context)parameters[0];
			if (obj instanceof Proc
					&& ((Proc) obj).asis() instanceof Invocation) {
				Invocation invoker = (Invocation) ((Proc) obj).asis();
				//return invoker.invoke(procModel, parameters);
				if (scope != null)
					return invoker.invoke(scope, parameters);
				else
					return invoker.invoke(procModel, parameters);
			} else if (obj instanceof Invocation) {
				Object out;
				if (scope != null)
					out = ((Invocation) obj).invoke(scope, parameters);
				else
					out = ((Invocation) obj).invoke(null, parameters);
//				if (procModel.getScope() == null)
//					procModel.setScope(new ServiceContext());
//				procModel.getScope().putValue(parname, out);
				return out;
			} else if (obj instanceof Agent) {
				return ((Agent)obj).getValue(parameters);
			} else {
				throw new InvocationException("No invoker for: " + parname);
			}
		} catch (ContextException e) {
			throw new InvocationException(e);
		}
	}

	public static ArgSet args(ServiceInvoker invoker) {
		return invoker.getArgs();
	}

//	public static Arg[] args(String... parnames)
//			throws ContextException {
//		ArgSet ps = new ArgSet();
//		for (String name : parnames) {
//			ps.add(new Proc(name));
//		}
//		return ps.toArray();
//	}

	public static Arg[] args(ProcModel pm, String... parnames)
			throws ContextException {
		ArgSet ps = new ArgSet();
		for (String name : parnames) {
			ps.add(pm.getPar(name));
		}
		return ps.toArray();
	}

	public static ServiceInvoker invoker(Evaluator evaluator, ArgSet pars) {
		return new ServiceInvoker(evaluator,pars);
	}

//	public static ServiceInvoker invoker(Evaluator evaluator, Proc... procEntries) {
//		return new ServiceInvoker(evaluator, procEntries);
//	}

	public static <T> ServiceInvoker invoker(ValueCallable<T> lambda, Args args) throws InvocationException {
		return new ServiceInvoker(null, lambda, null, args.argSet());
	}

	public static <T> ServiceInvoker invoker(ValueCallable<T> lambda, Context scope, Args args) throws InvocationException {
		try {
			return new ServiceInvoker(null, lambda, scope, args.argSet());
		} catch (Exception e) {
			throw new InvocationException("Failed to create invoker!", e);
		}
	}

	public static <T> ServiceInvoker invoker(String name, ValueCallable<T> lambda) throws InvocationException {
		return new ServiceInvoker(name, lambda, null, null);
	}

	public static <T> ServiceInvoker invoker(String name, ValueCallable<T> lambda, Args args) throws InvocationException {
		return new ServiceInvoker(name, lambda, args.argSet());
	}

	public static <T> ServiceInvoker invoker(String name, ValueCallable<T> lambda, Context scope, Args args) throws InvocationException {
		return new ServiceInvoker(name, lambda, scope, args.argSet());
	}

	public static ServiceInvoker invoker(String name, String expression, Args args) {
		return new GroovyInvoker(name, expression, args.argSet());
	}

	public static ServiceInvoker invoker(String name, String expression, Context scope, Args args) throws ContextException {
		GroovyInvoker invoker = new GroovyInvoker(name, expression, args.argSet());
		invoker.setScope(scope);
		return invoker;
	}

	public static ServiceInvoker expr(String expression, Context scope,  Args args) throws ContextException {
		return invoker(expression, scope, args);
	}

	public static ServiceInvoker invoker(String expression, Context scope, Args args) throws ContextException {
		GroovyInvoker invoker = new GroovyInvoker(expression, args.argSet());
		invoker.setScope(scope);
		return invoker;
	}

	public static ServiceInvoker expr(String expression) {
		return new GroovyInvoker(expression);
	}

	public static ServiceInvoker expr(String name, String expression) {
		return new GroovyInvoker(name, expression);
	}

	public static ServiceInvoker expr(String expression, Args args) {
		return 	invoker(expression, args);
		}

	public static ServiceInvoker invoker(String expression, Args args) {
		return new GroovyInvoker(expression, args.args());
	}

	public static ServiceInvoker invoker(String expression, Arg... args) {
		return new GroovyInvoker(expression, args);
	}

	public static ServiceInvoker invoker(String name, String expression, Arg... args) {
		GroovyInvoker gi = new GroovyInvoker(expression, args);
		gi.setName(name);
		return gi;
	}

    public static SysCall sysCall(String name, Context context) throws ContextException {
        return new SysCall(name, context);
    }

	public static ServiceInvoker print(String path) {
		return new GroovyInvoker("_print_", new Path(path));
	}

	public static ServiceInvoker invoker(String expression) {
		return new GroovyInvoker(expression);
	}

	public static ServiceInvoker invoker(Exertion exertion) {
        return new ExertInvoker(exertion);
    }

    public static ServiceInvoker invoker(Args args) {
        return new CmdInvoker(args.getNameArray());
    }
    public static IncrementInvoker inc(String path) {
		return new IntegerIncrementor(path, 1);
	}

	public static IncrementInvoker inc(String path, int increment) {
		return new IntegerIncrementor(path, increment);
	}

	public static IncrementInvoker inc(Invocation invoker, int increment) {
		if (invoker instanceof IntegerIncrementor) {
			((IntegerIncrementor) invoker).setIncrement(increment);
			return (IntegerIncrementor) invoker;
		} else {
			return new IntegerIncrementor(invoker, increment);
		}
	}

	public static IncrementInvoker inc(Invocation<Integer> invoker) {
		return new IntegerIncrementor(invoker, 1);
	}

	public static IncrementInvoker dinc(String path) {
		return new DoubleIncrementor(path, 1.0);
	}

	public static IncrementInvoker inc(String path, double increment) {
		return new DoubleIncrementor(path, increment);
	}


	public static IncrementInvoker inc(Invocation invoker, double increment) {
		if (invoker instanceof IntegerIncrementor) {
			((DoubleIncrementor) invoker).setIncrement(increment);
			return (DoubleIncrementor) invoker;
		} else {
			return new DoubleIncrementor(invoker, increment);
		}
	}

	public static IncrementInvoker dinc(Invocation<Double> invoker) {
		return new DoubleIncrementor(invoker, 1.0);
	}

	public static sorcer.service.Incrementor reset(sorcer.service.Incrementor incrementor) {
		incrementor.reset();
		return incrementor;
	}

	public static <T> T next(Incrementor<T> incrementor) {
		return incrementor.next();
	}

	public static <T> T next(ProcModel model, String name) throws ContextException {
		Incrementor<T> inceremntor = (Incrementor<T>)invoker(model, name);
		return inceremntor.next();
	}

	public static MethodInvoker methodInvoker(String selector, Object methodObject, Args... args) {
		return methodInvoker(selector, methodObject, null, args);
	}

	public static MethodInvoker methodInvoker(String selector, Object methodObject,
											  Context context, Args... args) {
		MethodInvoker mi = new MethodInvoker(selector, methodObject, selector, args);
		Context cxt = context;
		if (context == null) {
			cxt = new ServiceContext();
		}
		mi.setArgs(new Class[]{Context.class});
		mi.setContext(cxt);
		return mi;
	}

	public static ExertInvoker exertInvoker(String name, Exertion exertion, String path, Proc... procEntries) {
		return new ExertInvoker(name, exertion, path, procEntries);
	}

	public static ExertInvoker exertInvoker(Exertion exertion, String path, Proc... procEntries) {
		return new ExertInvoker(exertion, path, procEntries);
	}

	public static ExertInvoker exertInvoker(Exertion exertion, Proc... procEntries) {
		return new ExertInvoker(exertion, procEntries);
	}

	public static CmdInvoker cmdInvoker(String name, String cmd, Proc... procEntries) {
		return new CmdInvoker(name, cmd, procEntries);
	}

	public static RunnableInvoker runnableInvoker(String name, Runnable runnable, Proc... procEntries) {
		return new RunnableInvoker(name, runnable, procEntries);
	}

	public static CallableInvoker callableInvoker(String name, Callable callable, Proc... procEntries) {
		return new CallableInvoker(name, callable, procEntries);
	}

	public static <T> OptInvoker<T> opt(T value) {
		return new OptInvoker(value);
	}

	public static OptInvoker opt(Condition condition, ServiceInvoker target) {
		return new OptInvoker(null, condition, target);
	}

	public static OptInvoker opt(String name, ServiceInvoker target) {
		return new OptInvoker(name, target);
	}

	public static OptInvoker opt(String name, Condition condition, ServiceInvoker target) {
		return new OptInvoker(name, condition, target);
	}

	public static AltInvoker alt(OptInvoker...  invokers) {
		return new AltInvoker(null, invokers);
	}

	public static AltInvoker alt(String name, OptInvoker...  invokers) {
		return new AltInvoker(name, invokers);
	}

	public static LoopInvoker loop(Condition condition, Invocation target) {
		return new LoopInvoker(null, condition, target);
	}

	public static LoopInvoker loop(Condition condition, Invocation target, Context context) throws ContextException {
		LoopInvoker invoker = new LoopInvoker(null, condition, target);
		invoker.setScope(context);
		return invoker;
	}

	public static LoopInvoker loop(String name, Condition condition, Invocation target) {
		return new LoopInvoker(name, condition, target);
	}

	public static LoopInvoker loop(String name, Condition condition, Proc target)
			throws EvaluationException, RemoteException {
		return new LoopInvoker(name, condition, (ServiceInvoker) target.asis());
	}

	public static OptInvoker get(AltInvoker invoker, int index) {
		return invoker.getInvoker(index);
	}

	public static Agent agent(String name, String classNme, URL agentJar)
			throws EvaluationException, RemoteException {
		return new Agent(name, classNme, agentJar);
	}

	public static ExecPath invoker(String name, ServiceInvoker invoker) {
		return new ExecPath(name, invoker);
	}

	public static Domain scope(Proc procEntry) {
		return procEntry.getScope();
	}

	public static Context invokeScope(Proc procEntry) throws EvaluationException,
			RemoteException {
		Object obj = procEntry.asis();
		if (obj instanceof ServiceInvoker)
			return ((ServiceInvoker) obj).getScope();
		else
			return null;
	}

	public static Function ent(Model model, String path) throws ContextException {
        return new Function(path, model.asis(path));
    }

	public static <T extends Service> Srv ent(String name, MorphFidelity fidelity) {
        fidelity.setPath(name);
        fidelity.getFidelity().setPath(name);
        return srv(name, fidelity);
    }

	public static Srv ent(String name, ServiceFidelity fidelity) {
        return srv(name, fidelity);
    }

	public static Srv ent(ServiceFidelity fidelity) {
        return srv(fidelity);
    }

	public static Entry ent(Path path, Object value, Arg... args) {
		Entry entry = ent(path.getName(), value, args);
		entry.annotation(path.info.toString());
		return entry;
	}

	public static <T> Ref<T> ref(SupportComponent component) {
		Ref cr = new Ref();
		cr.set(component);
		return cr;
	}

	public static <T> Ref<T> ref(String path, Arg... args) {
		Ref cr = new Ref(path, args);
		return cr;
	}

    public static <T> Entry<T> ent(String path, T value, Arg... args) {
		Entry<T> entry = null;
		if (value instanceof Number ||  value instanceof String ||
				value instanceof Date) {
			return new Value(path, value);
		} else if (value instanceof Context && args != null && args.length > 0) {
			return (Entry<T>) new Neo(path, (Context)value, new Args(args));
		} else if (value instanceof Signature) {
			Mogram mog = Arg.selectMogram(args);
			Context cxt = null;
			if (mog instanceof Context) {
				cxt = (Context)mog;
			}
			if (cxt != null) {
				entry = (Entry<T>) srv(path, (Identifiable) value, cxt, args);
			} else {
				entry =  (Entry<T>) srv(path, (Identifiable) value, null, args);
			}
			entry.setType(Functionality.Type.SRV);
		} else if (value instanceof Fidelity) {
			if (((Fi)value).getType() == Fi.Type.VAL) {
				entry = new Value(path, value);
			} else if (((Fi)value).getType() == Fi.Type.PROC) {
				entry = new Proc(path, value);
			} else if (((Fi)value).getType() == Fi.Type.ENTRY) {
                ((Fidelity)value).setName(path);
				entry = new Entry(path, value);
			} else if (((Fi)value).getType() == Fi.Type.SRV) {
				entry = (Entry<T>) new Srv(path, value);
			}
		} else if (value instanceof MultiFiMogram) {
			try {
				((MultiFiMogram)value).setUnifiedName(path);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			entry = (Entry<T>) new Srv(path, value);
		} else if (value instanceof List && ((List)value).get(0) instanceof Path) {
			entry =  (Entry<T>) new ExecDependency(path, (List)value);
		} else if (value instanceof ServiceMogram) {
			entry = (Entry<T>) new MogramEntry(path, (Mogram) value);
			entry.setType(Functionality.Type.MOGRAM);
		} else if (value instanceof Service) {
			entry = (Entry<T>) new Proc(path, value);
			entry.setType(Functionality.Type.PROC);
		} else if (value.getClass() == Tuple2.class) {
			entry = (Entry<T>) new Function(path, value);
			entry.setType(Functionality.Type.CONSTANT);
		} else {
			entry = new Entry<T>(path, value);
			entry.setType(Functionality.Type.ENT);
		}

		Context cxt = null;
		for (Arg arg : args) {
			cxt = (Context) Arg.selectDomain(args);
		}
		try {
			// special cases of procedural attachmnet
			if (entry instanceof Proc) {
				if (cxt != null) {
					entry.setScope(cxt);
				} else if (args.length == 1 && args[0] instanceof Function) {
					entry.setScope(context((Function) args[0]));
				} else if (args.length == 1 && args[0] instanceof Service) {
					entry = new Proc(path, value, args[0]);
				}
			}
		} catch (ContextException e) {
			e.printStackTrace();
		}
		return entry;
	}

	public static Srv ent(Signature sig) {
		return srv(sig);
	}

	public static <T> Tuple2<Fidelity, Fidelity> ent(Fidelity selectFi, Fidelity srvFi) throws ConfigurationException {
		if (!srvFi.isValid()) {
			String msg = "Misconfigured entry fidelity: " + srvFi + " for: " + selectFi;
			logger.warn(msg);
//			throw new ConfigurationException("Misconfigured fidelity: " + srvFi + " for: " + selectFi);
		}
		Tuple2<Fidelity, Fidelity> assoc =  new Tuple2<>(selectFi, srvFi);
		if (srvFi.getType().equals(Fi.Type.GRADIENT)) {
			// if no path set use its name - no multifidelities
			if (selectFi.getPath().equals("")) {
				selectFi.setPath(selectFi.getName());
			}
			// use a select gradient name if declared
			if (selectFi.getSelect() == null) {
				if (srvFi.getSelect() != null) {
					selectFi.setSelect(srvFi.getSelect());
				} else {
					selectFi.setSelect((T) selectFi.getName());
					srvFi.setSelect((T) selectFi.getName());
				}
			}
		}
		srvFi.setName(selectFi.getName());
		srvFi.setPath(selectFi.getPath());
//		srvFi.setSelect((T) selectFi.getSelect());
		selectFi.setType(srvFi.getType());
		return assoc;
	}

	public static Function ent(String path) {
		return new Function(path, null);
	}

	public static <T> TagEntry<T> ent(String path, T value, String association) {
		return new TagEntry(path, value, association);
	}

	public static Arg[] ents(String... entries)
			throws ContextException {
		ArgSet as = new ArgSet();
		for (String name : entries) {
			as.add(new Function(name, Context.none));
		}
		return as.toArray();
	}

	public static Arg[] ents(Function... entries)
			throws ContextException {
		ArgSet as = new ArgSet();
		for (Function e : entries) {
			as.add(e);
		}
		return as.toArray();
	}

	public static Function inout(Function entry) {
		entry.setType(Functionality.Type.INOUT);
		return entry;
	}

	public static InputValue inoutVal(String path) {
		return new InputValue(path, null, 0);
	}

	public static <T> InoutValue<T> inoutVal(String path, T value) {
		return new InoutValue(path, value, 0);
	}

	public static <T> InoutValue<T> inoutVal(String path, T value, int index) {
		return new InoutValue(path, value, index);
	}

	public static <T> InoutValue<T> inoutVal(String path, T value, String annotation) {
		InoutValue<T> ie = inoutVal(path, value);
		ie.annotation(annotation);
		return ie;
	}

    public static Srv lmbd(String path, Args args) {
        Srv srv = new Srv(path, path);
        srv.setType(Functionality.Type.LAMBDA);
        return srv;
    }

    public static Srv lmbd(String path, Service service, Args args) {
        Srv srv = new Srv(path, path, service, args.getNameArray());
        srv.setType(Functionality.Type.LAMBDA);
        return srv;
    }

    public static Srv lambda(String path, Service service, Args args) {
		Srv srv = new Srv(path, path, service, args.getNameArray());
		srv.setType(Functionality.Type.LAMBDA);
		return srv;
	}

	public static Srv lambda(String path, Service service, String name, Args args) {
		Srv srv = new Srv(name, path, service,  args.getNameArray());
		srv.setType(Functionality.Type.LAMBDA);
		return srv;
	}

	public static Srv lambda(String name, String path, Client client) {
		Srv srv = new Srv(name, path, client);
		srv.setType(Functionality.Type.LAMBDA);
		return srv;
	}

	public static <T> Srv lambda(String path, Callable<T> call) {
		Srv srv = new Srv(path, call);
		srv.setType(Functionality.Type.LAMBDA);
		return srv;
	}

	public static <T> Srv lambda(String path, ValueCallable<T> call) {
		Srv srv = new Srv(path, call);
		srv.setType(Functionality.Type.LAMBDA);
		return srv;
	}

	public static <T> Srv lambda(String path, ValueCallable<T> call, Args args) {
		Srv srv = new Srv(path, call, args.getNameArray());
		srv.setType(Functionality.Type.LAMBDA);
		return srv;
	}

	public static <T> Srv lambda(String path, ValueCallable<T> lambda, Context context, Args args)
			throws InvocationException {
		Srv srv = new Srv(path, invoker(lambda, context, args));
		srv.setType(Functionality.Type.LAMBDA);
		return srv;
	}

	public static <T> Srv lambda(String path, EntryCollable call) {
		Srv srv = new Srv(path, call);
		srv.setType(Functionality.Type.LAMBDA);
		return srv;
	}

	public static <T> Srv lambda(String path, ValueCallable<T> call, Signature.ReturnPath returnPath) {
		Srv srv = new Srv(path, call, returnPath);
		srv.setType(Functionality.Type.LAMBDA);
		return srv;
	}
}
