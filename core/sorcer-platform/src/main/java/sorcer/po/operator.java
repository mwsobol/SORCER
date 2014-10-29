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

import java.net.URL;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import sorcer.co.tuple.Entry;
import sorcer.co.tuple.ExecPath;
import sorcer.co.tuple.InputEntry;
import sorcer.co.tuple.Tuple2;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.par.Agent;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.invoker.AltInvoker;
import sorcer.core.invoker.CallableInvoker;
import sorcer.core.invoker.CmdInvoker;
import sorcer.core.invoker.ExertInvoker;
import sorcer.core.invoker.GroovyInvoker;
import sorcer.core.invoker.InvokeDoubleIncrementor;
import sorcer.core.invoker.InvokeIncrementor;
import sorcer.core.invoker.LoopInvoker;
import sorcer.core.invoker.MethodInvoker;
import sorcer.core.invoker.OptInvoker;
import sorcer.core.invoker.RunnableInvoker;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.service.Arg;
import sorcer.service.ArgSet;
import sorcer.service.Condition;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Evaluation;
import sorcer.service.EvaluationException;
import sorcer.service.Evaluator;
import sorcer.service.Exertion;
import sorcer.service.Identifiable;
import sorcer.service.Incrementor;
import sorcer.service.Invocation;
import sorcer.service.InvocationException;
import sorcer.service.Mappable;
import sorcer.service.NoneException;
import sorcer.service.ServiceExertion;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class operator {

	private static final Logger logger = Logger.getLogger(operator.class.getName());


	public static <T> Par<T> par(String path, T argument) {
		return new Par<T>(path, argument);
	}
	
	public static <T> Par<T> dbPar(String path, T argument) {
		Par p = new Par(path, argument);
		p.setPersistent(true);
		return p;
	}
	
	public static <T> Par<T> par(Context context, String path, T argument) {
		Par p = new Par(path, argument);
		p.setScope(context);
		return p;
	}
	
	public static <T> Par<T> dbPar(Context context, String path, T argument) {
		Par p = new Par(path, argument);
		p.setPersistent(true);
		p.setScope(context);
		return p;
	}
	
	public static <T> Par<T> par(String name, String path, Mappable argument) {
		Par p = new Par(name, path, argument);
		return p;
	}
	
	public static <T> Par<T> pipe(Mappable in, String name, String path, Mappable out) throws ContextException {
		Par p = new Par(name, path, out);
		add(p, in);
		return p;
	}
	
	public static Par url(Par par, URL url) {
		par.setDbURL(url);
		return par;
	}
	
	public static Par par(ParModel pm, String name) throws ContextException {
		Par parameter = new Par(name, pm.asis(name));
		parameter.setScope(pm);
		return parameter;
	}
	
	public static ParModel parModel(String name, Identifiable... Objects)
			throws EvaluationException, RemoteException, ContextException {
		ParModel pm = new ParModel(name);
		pm.add(Objects);
		return pm;
	}
	
	public static ParModel parModel(Identifiable... Objects)
			throws EvaluationException, RemoteException, ContextException {
		ParModel pm = new ParModel();
		pm.add(Objects);
		return pm;
	}
	
	public static Object value(Par par) throws EvaluationException,
			RemoteException {
		return par.getValue();
	}
	
	public static Object get(ParModel pm, String parname, Arg... parametrs)
			throws ContextException, RemoteException {
		Object obj = pm.asis(parname);
		if (obj instanceof Par)
			obj = ((Par)obj).getValue(parametrs);
		return obj;
	}
	
	public static Object asis(ParModel pm, String parname)
			throws ContextException {
		return pm.asis(parname);
	}
		
	public static Invocation invoker(Mappable mappable, String path)
			throws ContextException {
		Object obj = ((ServiceContext) mappable).asis(path);
		while (obj instanceof Mappable || obj instanceof Par) {
			try {
				obj = ((Evaluation) obj).asis();
			} catch (RemoteException e) {
				throw new ContextException(e);
			}
		}
		if (obj instanceof Invocation)
			return (Invocation) obj;
		else
			throw new NoneException("No such invoker at: " + path + " in: " + mappable.getName());
	}
	
	public static Object asis(Par par) throws EvaluationException,
			RemoteException {
		return par.asis();
	}
	
	public static void clearPars(Object invoker) throws EvaluationException {
		if (invoker instanceof ServiceInvoker)
			((ServiceInvoker)invoker).clearPars();
	}
	
	public static ParModel parContext(Identifiable... objects)
			throws ContextException, RemoteException {
		return new ParModel(objects);
	}
	
	public static Context result(Context model, String parname)
			throws ContextException, RemoteException {
		((ServiceContext)model).setReturnPath(parname);
		return model;
	}

	public static ParModel add(ParModel parContext, Identifiable... objects)
			throws RemoteException, ContextException {
		parContext.add(objects);
		return parContext;
	}

	public static ParModel append(ParModel parContext, Identifiable... objects)
			throws RemoteException, ContextException {
		parContext.append(objects);
		return parContext;
	}
	
	public static Par put(ParModel parModel, String name, Object value) throws ContextException {
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
	
	public static Par set(Par par, Object value)
			throws ContextException {
		try {
			par.setValue(value);
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
		if (par.getScope() != null && par.getContextable() == null) {
			par.getScope().putValue(par.getName(), value);
		}
		return par;
	}
	
	public static Par set(ParModel context, String parname, Object value)
			throws ContextException {
		Par par = context.getPar(parname);
		if (par == null)
			par = context.addPar(parname, value);
		else
			try {
				par.setValue(value);
			} catch (RemoteException e) {
				throw new ContextException(e);
			}
		if (par.getScope() != null && par.getContextable() == null) {
			par.getScope().putValue(par.getName(), value);
		}
		return par;
	}
	
	public static Par add(Par par, Object to)
			throws ContextException {
		if (to instanceof Exertion) {
			((ServiceExertion)to).addPersister(par);
			return par;
		}
		return par;
	}
	
	public static Par connect(Object to, Par par)
			throws ContextException {
		return add(par, to);
	}
	
	public static Par par(Object object) {
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
				if (scope != null)
					return ((Invocation) obj).invoke(scope, parameters);
				else
					return ((Invocation) obj).invoke(null, parameters);
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
	
	public static ServiceInvoker invoker(Evaluator evaluator, Par... pars) {
		return new ServiceInvoker(evaluator,pars);
	}
	
	public static ServiceInvoker invoker(String name, String expression, Arg... pars) {
		return new GroovyInvoker(name, expression, pars);
	}
	
	public static ServiceInvoker invoker(String name, String expression, Par... pars) {
		return new GroovyInvoker(name, expression, pars);
	}
	
	public static ServiceInvoker invoker(String expression, Arg... pars) {
		return new GroovyInvoker(expression, pars);
	}
	
	public static ServiceInvoker invoker(String expression) { 
		return new GroovyInvoker(expression);
	}
	
	public static ServiceInvoker invoker(Exertion exertion) {
		return new ExertInvoker(exertion);
	}
	
	public static InvokeIncrementor inc(String name, Invocation invoker) {
		return new InvokeIncrementor(name, invoker, 1);
	}
	
	public static InvokeIncrementor inc(String name, Invocation invoker, int increment) {
		return new InvokeIncrementor(name, invoker, increment);
	}
	
	public static InvokeDoubleIncrementor inc(String name, Invocation invoker, double increment) {
			return new InvokeDoubleIncrementor(name, invoker, increment);
	}
	
	public static Incrementor reset(Incrementor incrementor) {
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
	
	public static MethodInvoker methodInvoker(String selector, Object methodObject, Par... pars) {
		return new MethodInvoker(selector, methodObject, selector, pars);
	}
	
	public static MethodInvoker methodInvoker(String selector, Object methodObject,
			Context context, Par... pars) {
		MethodInvoker mi = new MethodInvoker(selector, methodObject, selector,
				pars);
		mi.setArgs(new Class[] { Context.class });
		mi.setContext(context);
		return mi;
	}
	
	public static ExertInvoker exertInvoker(String name, Exertion exertion, String path, Par... pars) {
		return new ExertInvoker(name, exertion, path, pars);
	}
	
	public static ExertInvoker exertInvoker(Exertion exertion, String path, Par... pars) {
		return new ExertInvoker(exertion, path, pars);
	}
	
	public static ExertInvoker exertInvoker(Exertion exertion, Par... pars) {
		return new ExertInvoker(exertion, pars);
	}
	
	public static CmdInvoker cmdInvoker(String name, String cmd, Par... pars) {
		return new CmdInvoker(name, cmd, pars);
	}
	
	public static RunnableInvoker runnableInvoker(String name, Runnable runnable, Par... pars) {
		 return new RunnableInvoker(name, runnable, pars);
	}
	
	public static CallableInvoker callableInvoker(String name, Callable callable, Par... pars) {
		 return new CallableInvoker(name, callable, pars);
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
	
	public static LoopInvoker loop(String name, Condition condition, ServiceInvoker target) {
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
	
	public static InputEntry input(Par par) {
		return new InputEntry(par.getName(), par, 0);
	}

	public static InputEntry in(Par par) {
		return input(par);
	}
	
	public static Context scope(Par par) {
		return par.getScope();
	}
	
	public static Context invokeScope(Par par) throws EvaluationException,
			RemoteException {
		Object obj = par.asis();
		if (obj instanceof ServiceInvoker)
			return ((ServiceInvoker) obj).getScope();
		else
			return null;
	}
}