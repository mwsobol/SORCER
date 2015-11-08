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
import sorcer.co.tuple.Path;
import sorcer.co.tuple.Tuple2;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.EntryList;
import sorcer.core.context.model.par.Agent;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.invoker.*;
import sorcer.service.*;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class operator {

	private static final Logger logger = LoggerFactory.getLogger(operator.class.getName());


	public static <T> Par<T> par(String path, T argument) throws EvaluationException, RemoteException {
		return new Par(path, argument);
	}

	public static Par dbPar(String path, Object argument) throws EvaluationException, RemoteException {
		Par p = new Par(path, argument);
		p.setPersistent(true);
		p.getValue();
		return p;
	}

	public static Par par(Identifiable identifiable, Context context) throws EvaluationException, RemoteException {
		Par p = new Par(identifiable.getName(), identifiable);
		if (identifiable instanceof Scopable)
			try {
				((Scopable)identifiable).setScope(context);
			} catch (ContextException e) {
				throw new EvaluationException(e);
			}
		p.setScope(context);
		return p;
	}

	public static Par map(Par par, Service map) {
		par.setMappable((Mappable)map);
		return par;
	}

	public static Par par(Mappable argument, String name, String path) {
		Par p = new Par(argument, name, path);
		return p;
	}

	public static Par par(String path, Object argument, Object object) throws ContextException, RemoteException {
		Par p = null;
		if (object instanceof Context) {
			p = new Par(path, argument);
			p.setScope(object);
		} else if (object instanceof Service) {
			p = new Par(path, argument, object);
		}
		return p;
	}

	public static Par dPar(Identifiable identifiable, Context context) throws EvaluationException, RemoteException {
		Par p = new Par(identifiable.getName(), identifiable);
		p.setPersistent(true);
		p.setScope(context);
		return p;
	}

	public static Par dbPar(String path, Object argument, Context context) throws EvaluationException, RemoteException {
		Par p = new Par(path, argument);
		p.setPersistent(true);
		p.setScope(context);
		return p;
	}

	public static Par pipe(Mappable in, String name, String path, Service out) throws ContextException {
		Par p = new Par(name, path, out);
		add(p, in);
		return p;
	}

	public static Par storeUrl(Par parEntry, URL url) {
		parEntry.setDbURL(url);
		return parEntry;
	}

	public static Par par(ParModel pm, String name) throws ContextException, RemoteException {
		Par parameter = new Par(name, pm.asis(name));
		parameter.setScope(pm);
		return parameter;
	}

	public static EntryList parFi(String name, Entry... entries) {
		return new EntryList(name, entries);
	}

	public static EntryList parFi(Entry... entries) {
		return new EntryList(entries);
	}

	public static Fidelity<Arg> parFi(String name) {
		return new Fidelity(name);
	}

	public static Entry parFi(Par parEntry) {
		Entry fi = new Entry(parEntry.getSelectedFidelity(), parEntry.getFidelities()
				.get(parEntry.getSelectedFidelity()));
		return fi;
	}

	public static ParModel parModel(String name, Object... objects)
			throws RemoteException, ContextException {
		ParModel pm = new ParModel(name);
		for (Object o : objects) {
			if (o instanceof Identifiable)
				pm.add((Identifiable)o);
		}
		return pm;
	}

	public static <T> T get(ParModel<T> pm, String parname, Arg... parametrs)
			throws ContextException, RemoteException {
		Object obj = pm.asis(parname);
		if (obj instanceof Par)
			obj = ((Par)obj).getValue(parametrs);
		return (T)obj;
	}

	public static Invocation invoker(Mappable mappable, String path)
			throws ContextException {
		Object obj = mappable.asis(path);
		while (obj instanceof Mappable || obj instanceof Par) {
			try {
				obj = ((Evaluation) obj).asis();
			} catch (RemoteException e) {
				throw new ContextException(e);
			}
		}
		if (obj instanceof Invocation)
			return (Invocation) obj;
		else if (obj != null)
			return new Entry(path,obj);
		else
			throw new NoneException("No such invoker at: " + path + " in: " + mappable.getName());
	}

	public static void clearPars(Object invoker) throws EvaluationException {
		if (invoker instanceof ServiceInvoker)
			((ServiceInvoker)invoker).clearPars();
	}

	public static ParModel parModel(Identifiable... objects)
			throws ContextException, RemoteException {
		return new ParModel(objects);
	}

	public static ParModel add(ParModel parModel, Identifiable... objects)
			throws RemoteException, ContextException {
		parModel.add(objects);
		return parModel;
	}

	public static ParModel append(ParModel parContext, Arg... objects)
			throws RemoteException, ContextException {
		parContext.append(objects);
		return parContext;
	}

	public static Par put(ParModel parModel, String name, Object value) throws ContextException, RemoteException {
		parModel.putValue(name, value);
		parModel.setContextChanged(true);
		return par(parModel, name);
	}

	public static ParModel put(ParModel parModel, Tuple2... entries) throws ContextException {
		for (Tuple2 e : entries) {
			parModel.putValue((String)e.key(), e.value());
		}
		parModel.setContextChanged(true);
		return parModel;
	}

	public static Par set(Par parEntry, Object value)
			throws ContextException {
		try {
			parEntry.setValue(value);
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
		if (parEntry.getScope() != null && parEntry.getContextable() == null) {
			parEntry.getScope().putValue(parEntry.getName(), value);
		}
		return parEntry;
	}

	public static void set(ParModel context, String parname, Object value)
			throws ContextException {
		Object parEntry = context.asis(parname);
		if (parEntry == null)
			context.addPar(parname, value);
		else if (parEntry instanceof Setter) {
			try {
				((Setter) parEntry).setValue(value);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else if (parEntry instanceof Par) {
			Par par = (Par) parEntry;
			if (par.getScope() != null && par.getContextable() == null)
				par.getScope().putValue(par.getName(), value);
		}
		// just ssetting the value
		else {
			context.putValue(parname, value);
			context.setIsChanged(true);
		}

	}

	public static Par add(Par parEntry, Object to)
			throws ContextException {
		if (to instanceof Exertion) {
			((ServiceExertion)to).addPersister(parEntry);
			return parEntry;
		}
		return parEntry;
	}

	public static Par connect(Object to, Par parEntry)
			throws ContextException {
		return add(parEntry, to);
	}

	public static Par par(Object object) throws EvaluationException, RemoteException {
		if (object instanceof String)
			return new Par((String)object);
		else if (object instanceof Identifiable)
			return new Par(((Identifiable) object).getName(), object);
		return null;
	}

	public static Object invoke(Invocation invoker, Arg... parameters)
			throws InvocationException, RemoteException {
		return invoker.invoke(null, parameters);
	}

	public static Object invoke(Invocation invoker, Context context, Arg... parameters)
			throws InvocationException, RemoteException {
		return invoker.invoke(context, parameters);
	}

	public static Object invoke(ParModel parModel, String parname, Arg... parameters)
			throws RemoteException, InvocationException {
		try {
			Object obj = parModel.asis(parname);
			Context scope = null;
			// assume that the first argument is always context if provided
			if (parameters.length > 0 && parameters[0] instanceof Context)
				scope = (Context)parameters[0];
			if (obj instanceof Par
					&& ((Par) obj).asis() instanceof Invocation) {
				Invocation invoker = (Invocation) ((Par) obj).asis();
				//return invoker.invoke(parModel, parameters);
				if (scope != null)
					return invoker.invoke(scope, parameters);
				else
					return invoker.invoke(parModel, parameters);
			} else if (obj instanceof Invocation) {
				Object out;
				if (scope != null)
					out = ((Invocation) obj).invoke(scope, parameters);
				else
					out = ((Invocation) obj).invoke(null, parameters);
//				if (parModel.getScope() == null)
//					parModel.setScope(new ServiceContext());
//				parModel.getScope().putValue(parname, out);
				return out;
			} else if (obj instanceof Agent) {
				return ((Agent)obj).getValue(parameters);
			}

			else {
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
			ps.add(new Par(name));
		}
		return ps.toArray();
	}

	public static Arg[] args(ParModel pm, String... parnames)
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

	public static ServiceInvoker invoker(Evaluator evaluator, Par... parEntries) {
		return new ServiceInvoker(evaluator, parEntries);
	}

	public static ServiceInvoker invoker(ContextCallable condition) throws InvocationException {
		return new ServiceInvoker(null, condition, null);
	}

	public static ServiceInvoker invoker(ContextCallable condition, Context scope) throws InvocationException {
		try {
			return new ServiceInvoker(null, condition, scope);
		} catch (Exception e) {
			throw new InvocationException("Failed to create invoker!", e);
		}
	}

	public static <T> ServiceInvoker invoker(String name, ContextCallable<T> condition) throws InvocationException {
		return new ServiceInvoker(name, condition, null);
	}

	public static <T> ServiceInvoker invoker(String name, ContextCallable<T> condition, Context scope) throws InvocationException {
		return new ServiceInvoker(name, condition, scope);
	}

	public static ServiceInvoker invoker(String name, String expression, Arg... pars) {
		return new GroovyInvoker(name, expression, pars);
	}

	public static ServiceInvoker invoker(String name, String expression, Par... parEntries) {
		return new GroovyInvoker(name, expression, parEntries);
	}

	public static ServiceInvoker invoker(String expression, Arg... pars) {
		return new GroovyInvoker(expression, pars);
	}

	public static ServiceInvoker print(String path) {
		return new GroovyInvoker("_print_", new Path<>(path));
	}

	public static ServiceInvoker invoker(String expression) {
		return new GroovyInvoker(expression);
	}

	public static ServiceInvoker invoker(Exertion exertion) {
		return new ExertInvoker(exertion);
	}

	public static InvokeIncrementor inc(String path) {
		return new IntegerIncrementor(path, 1);
	}

	public static InvokeIncrementor inc(String path, int increment) {
		return new IntegerIncrementor(path, increment);
	}

	public static InvokeIncrementor inc(Invocation invoker, int increment) {
		return new IntegerIncrementor(invoker, increment);
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
		return new DoubleIncrementor(invoker, increment);
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

	public static <T> T next(ParModel model, String name) throws ContextException {
		Incrementor<T> inceremntor = (Incrementor<T>)invoker(model, name);
		return inceremntor.next();
	}

	public static MethodInvoker methodInvoker(String selector, Object methodObject, Par... parEntries) {
		return new MethodInvoker(selector, methodObject, selector, parEntries);
	}

	public static MethodInvoker methodInvoker(String selector, Object methodObject,
											  Context context, Par... parEntries) {
		MethodInvoker mi = new MethodInvoker(selector, methodObject, selector,
				parEntries);
		mi.setArgs(new Class[] { Context.class });
		mi.setContext(context);
		return mi;
	}

	public static ExertInvoker exertInvoker(String name, Exertion exertion, String path, Par... parEntries) {
		return new ExertInvoker(name, exertion, path, parEntries);
	}

	public static ExertInvoker exertInvoker(Exertion exertion, String path, Par... parEntries) {
		return new ExertInvoker(exertion, path, parEntries);
	}

	public static ExertInvoker exertInvoker(Exertion exertion, Par... parEntries) {
		return new ExertInvoker(exertion, parEntries);
	}

	public static CmdInvoker cmdInvoker(String name, String cmd, Par... parEntries) {
		return new CmdInvoker(name, cmd, parEntries);
	}

	public static RunnableInvoker runnableInvoker(String name, Runnable runnable, Par... parEntries) {
		return new RunnableInvoker(name, runnable, parEntries);
	}

	public static CallableInvoker callableInvoker(String name, Callable callable, Par... parEntries) {
		return new CallableInvoker(name, callable, parEntries);
	}

	public static OptInvoker opt(String name, ServiceInvoker target) {
		return new OptInvoker(name, target);
	}

	public static OptInvoker opt(String name, Condition condition, ServiceInvoker target) {
		return new OptInvoker(name, condition, target);
	}

	public static AltInvoker alt(String name, OptInvoker...  invokers) {
		return new AltInvoker(name, invokers);
	}

	public static LoopInvoker loop(String name, Condition condition, Invocation target) {
		return new LoopInvoker(name, condition, target);
	}

	public static LoopInvoker loop(String name, Condition condition, Par target)
			throws EvaluationException, RemoteException {
		return new LoopInvoker(name, condition, (ServiceInvoker) ((Par) target).asis());
	}

	public static OptInvoker get(AltInvoker invoker, int index) {
		return invoker.getInvoker(index);
	}

	public static Agent agent(String name, String classNme, URL agentJar)
			throws EvaluationException, RemoteException {
		return new Agent(name, classNme, agentJar);
	}

//	public static ExecPath invoker(String name) {
//		return new ExecPath(name);
//	}

	public static ExecPath invoker(String name, ServiceInvoker invoker) {
		return new ExecPath(name, invoker);
	}

	public static InputEntry input(Par parEntry) {
		return new InputEntry(parEntry.getName(), parEntry, 0);
	}

	public static InputEntry in(Par parEntry) {
		return input(parEntry);
	}

	public static Context scope(Par parEntry) {
		return parEntry.getScope();
	}

	public static Context invokeScope(Par parEntry) throws EvaluationException,
			RemoteException {
		Object obj = parEntry.asis();
		if (obj instanceof ServiceInvoker)
			return ((ServiceInvoker) obj).getScope();
		else
			return null;
	}
}
