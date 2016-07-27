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
import sorcer.co.tuple.ExecPath;
import sorcer.co.tuple.InputEntry;
import sorcer.co.tuple.Tuple2;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.*;
import sorcer.core.context.model.ent.Proc;
import sorcer.core.invoker.*;
import sorcer.service.*;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;

import static sorcer.eo.operator.context;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class operator {

	private static final Logger logger = LoggerFactory.getLogger(operator.class.getName());


	public static <T> Proc<T> proc(String path, T argument) throws EvaluationException, RemoteException {
		return new Proc(path, argument);
	}

	public static Proc dbPar(String path, Object argument) throws EvaluationException, RemoteException {
		Proc p = new Proc(path, argument);
		p.setPersistent(true);
		p.getValue();
		return p;
	}

	public static Proc proc(Identifiable identifiable, Context context) throws EvaluationException, RemoteException {
		Proc p = new Proc(identifiable.getName(), identifiable);
		if (identifiable instanceof Scopable)
			try {
				((Scopable)identifiable).setScope(context);
			} catch (ContextException e) {
				throw new EvaluationException(e);
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
		} else if (object instanceof Entry) {
			p = new Proc(path, argument);
			p.setScope(context((Entry)object));
		} else if (object instanceof Service) {
			p = new Proc(path, argument, object);
		}
		return p;
	}

	public static Proc dPar(Identifiable identifiable, Context context) throws EvaluationException, RemoteException {
		Proc p = new Proc(identifiable.getName(), identifiable);
		p.setPersistent(true);
		p.setScope(context);
		return p;
	}

	public static Proc dbPar(String path, Object argument, Context context) throws EvaluationException, RemoteException {
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

	public static EntryList parFi(String name, Entry... entries) {
		return new EntryList(name, entries);
	}

	public static EntryList parFi(Entry... entries) {
		return new EntryList(entries);
	}

	public static ServiceFidelity<Arg> parFi(String name) {
		return new ServiceFidelity(name);
	}

	public static Entry parFi(Proc procEntry) {
		Entry fi = new Entry(procEntry.getSelectedFidelity(), procEntry.getFidelities()
				.get(procEntry.getSelectedFidelity()));
		return fi;
	}

	public static ProcModel procModel(String name, Object... objects)
			throws RemoteException, ContextException {
		ProcModel pm = new ProcModel(name);
		for (Object o : objects) {
			if (o instanceof Identifiable)
				pm.add((Identifiable)o);
		}
		return pm;
	}

	public static Object get(ProcModel pm, String parname, Arg... parametrs)
			throws ContextException, RemoteException {
		Object obj = pm.asis(parname);
		if (obj instanceof Proc)
			obj = ((Proc)obj).getValue(parametrs);
		return obj;
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

	public static ProcModel put(ProcModel procModel, Tuple2... entries) throws ContextException {
		for (Tuple2 e : entries) {
			procModel.putValue((String)e.key(), e.value());
		}
		procModel.setContextChanged(true);
		return procModel;
	}

	public static Proc set(Proc procEntry, Object value)
			throws ContextException {
		try {
			procEntry.setValue(value);
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
		if (procEntry.getScope() != null && procEntry.getContextable() == null) {
			procEntry.getScope().putValue(procEntry.getName(), value);
		}
		return procEntry;
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

	public static Proc proc(Invocation invoker) {
		return new Proc(invoker.getName(), invoker);
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

	public static ArgSet pars(ServiceInvoker invoker) {
		return invoker.getPars();
	}

	public static Arg[] pars(String... parnames)
			throws ContextException {
		ArgSet ps = new ArgSet();
		for (String name : parnames) {
			ps.add(new Proc(name));
		}
		return ps.toArray();
	}

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

	public static ServiceInvoker invoker(Evaluator evaluator, Proc... procEntries) {
		return new ServiceInvoker(evaluator, procEntries);
	}

	public static ServiceInvoker invoker(ValueCallable lambda) throws InvocationException {
		return new ServiceInvoker(null, lambda, null);
	}

	public static ServiceInvoker invoker(ValueCallable lambda, Context scope) throws InvocationException {
		try {
			return new ServiceInvoker(null, lambda, scope);
		} catch (Exception e) {
			throw new InvocationException("Failed to create invoker!", e);
		}
	}

	public static <T> ServiceInvoker invoker(String name, ValueCallable<T> lambda) throws InvocationException {
		return new ServiceInvoker(name, lambda, null);
	}

	public static <T> ServiceInvoker invoker(String name, ValueCallable<T> lambda, Context scope) throws InvocationException {
		return new ServiceInvoker(name, lambda, scope);
	}

	public static ServiceInvoker invoker(String name, String expression, sorcer.eo.operator.Args args) {
		return new GroovyInvoker(name, expression, args.args());
	}

	public static ServiceInvoker invoker(String name, String expression, sorcer.eo.operator.Args args, Context scope) throws ContextException {
		GroovyInvoker invoker = new GroovyInvoker(name, expression, args.args());
		invoker.setScope(scope);
		return invoker;
	}

	public static ServiceInvoker expr(String expression, sorcer.eo.operator.Args args, Context scope) throws ContextException {
		return invoker(expression, args,  scope);
	}

	public static ServiceInvoker invoker(String expression, sorcer.eo.operator.Args args, Context scope) throws ContextException {
		GroovyInvoker invoker = new GroovyInvoker(expression, args.args());
		invoker.setScope(scope);
		return invoker;
	}

	public static ServiceInvoker expr(String expression, sorcer.eo.operator.Args args) {
		return 	invoker(expression, args);
		}

	public static ServiceInvoker invoker(String expression, sorcer.eo.operator.Args args) {
		return new GroovyInvoker(expression, args.args());
	}

	public static ServiceInvoker invoker(String expression, Arg... args) {
		return new GroovyInvoker(expression, args);
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

	public static ServiceInvoker expr(String expression) {
		return new GroovyInvoker(expression);
	}

	public static ServiceInvoker invoker(Exertion exertion) {
        return new ExertInvoker(exertion);
    }

    public static ServiceInvoker invoker(sorcer.eo.operator.Args args) {
        return new CmdInvoker(args.argsToStrings());
    }
    public static InvokeIncrementor inc(String path) {
		return new IntegerIncrementor(path, 1);
	}

	public static InvokeIncrementor inc(String path, int increment) {
		return new IntegerIncrementor(path, increment);
	}

	public static InvokeIncrementor inc(Invocation invoker, int increment) {
		if (invoker instanceof IntegerIncrementor) {
			((IntegerIncrementor) invoker).setIncrement(increment);
			return (IntegerIncrementor) invoker;
		} else {
			return new IntegerIncrementor(invoker, increment);
		}
	}

	public static InvokeIncrementor inc(Invocation<Integer> invoker) {
		return new IntegerIncrementor(invoker, 1);
	}

	public static InvokeIncrementor dinc(String path) {
		return new DoubleIncrementor(path, 1.0);
	}

	public static InvokeIncrementor inc(String path, double increment) {
		return new DoubleIncrementor(path, increment);
	}


	public static InvokeIncrementor inc(Invocation invoker, double increment) {
		if (invoker instanceof IntegerIncrementor) {
			((DoubleIncrementor) invoker).setIncrement(increment);
			return (DoubleIncrementor) invoker;
		} else {
			return new DoubleIncrementor(invoker, increment);
		}
	}

	public static InvokeIncrementor dinc(Invocation<Double> invoker) {
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

	public static MethodInvoker methodInvoker(String selector, Object methodObject, Proc... procEntries) {
		return methodInvoker(selector, methodObject, null, procEntries);
	}

	public static MethodInvoker methodInvoker(String selector, Object methodObject,
											  Context context, Proc... procEntries) {
		MethodInvoker mi = new MethodInvoker(selector, methodObject, selector,
				procEntries);
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

	public static InputEntry input(Proc procEntry) {
		return new InputEntry(procEntry.getName(), procEntry, 0);
	}

	public static InputEntry in(Proc procEntry) {
		return input(procEntry);
	}

	public static Context scope(Proc procEntry) {
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
}
