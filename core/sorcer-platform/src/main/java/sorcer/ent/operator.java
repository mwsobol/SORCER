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
package sorcer.ent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.Operator;
import sorcer.co.tuple.*;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.EntModel;
import sorcer.core.context.model.ent.*;
import sorcer.core.context.model.ent.Pro;
import sorcer.core.context.model.srv.Srv;
import sorcer.core.invoker.*;
import sorcer.core.plexus.FidelityManager;
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
import java.util.Map;
import java.util.concurrent.Callable;

import static sorcer.eo.operator.context;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class operator extends Operator {

	private static final Logger logger = LoggerFactory.getLogger(operator.class.getName());

	public static Neu neu(String path, double signal) {
		return new Neu(path, signal);
	}

    public static Neu neu(String path, Args signals) {
        return new Neu(path, signals);
    }

    public static Neu neu(String path, Context<Float> weights, Args signals) {
        return new Neu(path, weights, signals);
    }

    public static Neu neu(String path, Context<Float> weights, double signal, Args signals) {
        return new Neu(path, weights, signals);
    }

    public static Neu neu(String path, ServiceFidelity fidelities) {
		return new Neu(path, fidelities);
	}

    public static Entry th(String path, double threshold) {
        Entry e = new Entry(path, threshold);
	    e.setType(Functionality.Type.THRESHOLD);
        return e;
    }

    public static Entry bias(String path, double bias) {
        Entry e = new Entry(path, bias);
        e.setType(Functionality.Type.BIAS);
        return e;
    }

    public static <T> Pro<T> pro(String path, T argument) throws EvaluationException, RemoteException {
		return new Pro(path, argument);
	}

	public static Pro dbEnt(String path, Object argument) throws EvaluationException, RemoteException {
		Pro p = new Pro(path, argument);
		p.setPersistent(true);
		p.evaluate();
		return p;
	}

	public static Pro pro(Identifiable identifiable, Context context) throws EvaluationException, RemoteException {
		Pro p = new Pro(identifiable.getName(), identifiable);
		if (identifiable instanceof Scopable) {
			((Scopable) identifiable).setScope(context);
		}
		p.setType(Functionality.Type.PROC);
		return p;
	}

	public static Pro pro(Contexting argument, String name, String path) {
		Pro p = new Pro(argument, name, path);
		return p;
	}

	public static Pro pro(String path, Object argument, Object object) throws ContextException, RemoteException {
		Pro p = null;
		if (object instanceof Context) {
			p = new Pro(path, argument);
			p.setScope(object);
		} else if (object instanceof Entry) {
			p = new Pro(path, argument);
			p.setScope(context((Entry)object));
		} else if (object instanceof Args) {
			p = new Pro(path, argument);
			p.setScope(context((Args)object));
		} else if (object instanceof Service) {
			p = new Pro(path, argument, object);
		}
		return p;
	}

	public static ServiceInvoker func(ServiceInvoker invoker) {
		invoker.setFunctional(true);
		return invoker;
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
	public static Pro dPar(Identifiable identifiable, Context context) throws EvaluationException, RemoteException {
		Pro p = new Pro(identifiable.getName(), identifiable);
		p.setPersistent(true);
		p.setScope(context);
		return p;
	}

	public static Pro dbEnt(String path, Object argument, Context context) throws EvaluationException, RemoteException {
		Pro p = new Pro(path, argument);
		p.setPersistent(true);
		p.setScope(context);
		return p;
	}

	public static Pro pipe(Contexting in, String name, String path, Service out) throws ContextException {
		Pro p = new Pro(name, path, out);
		add(p, in);
		return p;
	}

	public static Pro storeUrl(Pro callEntry, URL url) {
		callEntry.setDbURL(url);
		return callEntry;
	}

	public static Pro pro(EntModel pm, String name) throws ContextException, RemoteException {
		Pro parameter = new Pro(name, pm.asis(name));
		parameter.setScope(pm);
		return parameter;
	}

	public static Invocation invoker(Contexting mappable, String path)
			throws ContextException {
		Object obj = mappable.asis(path);
		while (obj instanceof Contexting || obj instanceof Pro) {
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

	public static EntModel append(EntModel parContext, Arg... objects)
			throws RemoteException, ContextException {
		parContext.append(objects);
		return parContext;
	}

//	public static Proc put(EntModel entModel, String name, Object get) throws ContextException, RemoteException {
//		entModel.putValue(name, get);
//		entModel.setContextChanged(true);
//		return pro(entModel, name);
//	}
//
//	public static EntModel put(EntModel entModel, Entry... entries) throws ContextException {
//		for (Entry e : entries) {
//			entModel.putValue(e.getName(), e.getImpl());
//		}
//		entModel.setContextChanged(true);
//		return entModel;
//	}

	public static Pro add(Pro callEntry, Object to)
			throws ContextException {
		if (to instanceof Routine) {
			((ServiceRoutine)to).addPersister(callEntry);
			return callEntry;
		}
		return callEntry;
	}

	public static Pro connect(Object to, Pro callEntry)
			throws ContextException {
		return add(callEntry, to);
	}

	public static Pro pro(Object object) throws EvaluationException, RemoteException {
		if (object instanceof String)
			return new Pro((String)object);
		else if (object instanceof Identifiable)
			return new Pro(((Identifiable) object).getName(), object);
		return null;
	}

	public static Pro pro() {
		GroovyInvoker gi = new GroovyInvoker();
		return new Pro(gi.getName(), gi);
	}

    public static Pro pro(String expression, Arg... parameters) {
        return pro(null, expression, null, parameters);
    }

	public static Pro pro(String expression, Context context, Arg... parameters) {
		return pro(null, expression, context, parameters);
	}

	public static Pro pro(String path, String expression, Context context, Arg... parameters) {
		GroovyInvoker gi = new GroovyInvoker(expression, parameters);
		if (context != null) {
            gi.setScope(context);
        }
		String name = path;
		if (path == null) {
			name = gi.getName();
		}
		return new Pro(name, gi);
	}

	public static Pro pro(Invocation invoker) {
		return new Pro(((ServiceInvoker)invoker).getName(), invoker);
	}

	public static Pro pro(String path, Invocation invoker) {
		return new Pro(path, invoker);
	}

	public static Object invoke(Invocation invoker, Arg... parameters)
			throws ContextException, RemoteException {
		return invoker.invoke(null, parameters);
	}

	public static Object invoke(Invocation invoker, Context context, Arg... parameters)
			throws ContextException, RemoteException {
		return invoker.invoke(context, parameters);
	}

    public static Object activate(Model model, String path, Arg... args) throws InvocationException {
        try {
			Neu ane = (Neu) model.get(path);
            if (ane.getMultiFi() != null) {
                List<Fidelity> fiList = Arg.selectFidelities(args);
                ((FidelityManager) model.getFidelityManager()).reconfigure(fiList);
                return invoke((EntModel) model, path, args);

            } else {
                return invoke((EntModel) model, path, args);
            }
        } catch (RemoteException | ContextException | ConfigurationException e) {
            throw new InvocationException(e);
        }
    }

	public static Object invoke(EntModel procModel, String parname, Arg... parameters)
			throws RemoteException, InvocationException {
		try {
			Object obj;
			if (parameters.length > 0 && parameters[0] instanceof Agent) {
				obj = parameters[0];
			} else {
				obj = procModel.get(parname);
			}

			Context scope = null;
			// assume that the first argument is always context if provided
			if (parameters.length > 0 && parameters[0] instanceof Context) {
				scope = (Context) parameters[0];
			}
			if (obj instanceof Agent) {
				return ((Agent)obj).evaluate(parameters);
			} else if (obj instanceof Pro
					&& ((Pro) obj).asis() instanceof Invocation) {
				Invocation invoker = (Invocation) ((Pro) obj).asis();
				//return invoker.invoke(entModel, parameters);
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
				return out;
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

	public static Arg[] args(EntModel pm, String... parnames)
			throws ContextException {
		ArgSet ps = new ArgSet();
		for (String name : parnames) {
			ps.add(pm.getCall(name));
		}
		return ps.toArray();
	}

	public static ServiceInvoker invoker(Evaluator evaluator, ArgSet pars) {
		return new ServiceInvoker(evaluator,pars);
	}

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

	public static ServiceInvoker invoker(Routine exertion) {
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

	public static <T> T next(EntModel model, String name) throws ContextException {
		Incrementor<T> inceremntor = (Incrementor<T>)invoker(model, name);
		return inceremntor.next();
	}

	public static MethodInvoker methodInvoker(String selector, Object methodObject, Args... args) {
		return methodInvoker(selector, methodObject, null, args);
	}

	public static MethodInvoker methodInvoker(String selector, Object methodObject,
											  Domain context, Args... args) {
		MethodInvoker mi = new MethodInvoker(selector, methodObject, selector, args);
		Domain cxt = context;
		if (context == null) {
			cxt = new ServiceContext();
		}
		mi.setContext(cxt);
		mi.setParameterTypes(new Class[]{ Context.class });
		return mi;
	}

	public static ExertInvoker exertInvoker(String name, Routine exertion, String path, Pro... callEntries) {
		return new ExertInvoker(name, exertion, path, callEntries);
	}

	public static ExertInvoker exertInvoker(Routine exertion, String path, Pro... callEntries) {
		return new ExertInvoker(exertion, path, callEntries);
	}

	public static ExertInvoker exertInvoker(Routine exertion, Pro... callEntries) {
		return new ExertInvoker(exertion, callEntries);
	}

	public static CmdInvoker cmdInvoker(String name, String cmd, Pro... callEntries) {
		return new CmdInvoker(name, cmd, callEntries);
	}

	public static RunnableInvoker runnableInvoker(String name, Runnable runnable, Pro... callEntries) {
		return new RunnableInvoker(name, runnable, callEntries);
	}

	public static CallableInvoker callableInvoker(String name, Callable callable, Pro... callEntries) {
		return new CallableInvoker(name, callable, callEntries);
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

	public static AltInvoker alt(Evaluator...  invokers) {
		return new AltInvoker(null, invokers);
	}

	public static AltInvoker alt(String name, Evaluator...  invokers) {
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

	public static LoopInvoker loop(String name, Condition condition, Pro target)
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

	public static Domain scope(Pro callEntry) {
		return callEntry.getScope();
	}

	public static Context invokeScope(Pro callEntry) throws EvaluationException,
			RemoteException {
		Object obj = callEntry.asis();
		if (obj instanceof ServiceInvoker)
			return ((ServiceInvoker) obj).getScope();
		else
			return null;
	}

	public static Subroutine pro(Model model, String path) throws ContextException {
        return new Subroutine(path, model.asis(path));
    }

	public static <T extends Service> Srv ent(String name, MorphFidelity fidelity) {
        fidelity.setPath(name);
        fidelity.getFidelity().setPath(name);
		fidelity.getFidelity().setName(name);
		for (Object item : fidelity.getSelects()) {
			if (item instanceof Fidelity) {
				((Fidelity) item).setPath(name);
				if (((Fidelity)item).getName() == null) {
					((Fidelity)item).setName(name);
				}
				for (Object obj : ((Fidelity) item).getSelects()) {
					if (obj instanceof Fi && ((Fidelity) obj).getName() == null) {
						((Fidelity) obj).setPath(name);
						((Fidelity) obj).setName(((Identifiable) obj).getName());
					}
				}
			}
		}
        return srv(name, fidelity);
    }

	public static Srv ent(String name, ServiceFidelity fidelity) {
        return srv(name, fidelity);
    }

	public static Srv ent(ServiceFidelity fidelity) {
        return srv(fidelity);
    }

	public static FreeEntry ent(String path) {
		return new FreeEntry(path);
	}

	public static Entry ent(Path path, Object value, Arg... args) {
		Entry entry = ent(path.getName(), value, args);
		entry.annotation(path.info.toString());
		return entry;
	}

	public static Entry inv(Entry ent) {
		ent.setCached(true);
		return ent;
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

	public static Entry ent(Identifiable object) {
		return new Entry(object.getName(), object);
	}

    public static Entry ent(String path, Object value, Arg... args) {
		Entry entry = null;
		if (value instanceof List) {
			if  (((List)value).get(0) instanceof Path) {
				entry = new ExecDependency(path, (List) value);
			} else {
				entry = new Value(path, value);
			}
		} else if (value instanceof  Number || value instanceof  String || value instanceof  Date
				|| value instanceof Map || value.getClass().isArray()) {
			return new Value(path, value);
		} else if (value instanceof Context && args != null && args.length > 0) {
			return new Neu(path, (Context)value, new Args(args));
		} else if (value instanceof Signature) {
			Mogram mog = Arg.selectMogram(args);
			Context cxt = null;
			if (mog instanceof Context) {
				cxt = (Context)mog;
			}
			if (cxt != null) {
				entry = srv(path, (Identifiable) value, cxt, args);
			} else {
				entry = srv(path, (Identifiable) value, null, args);
			}
			entry.setType(Functionality.Type.SRV);
		} else if (value instanceof Fidelity) {
			if (((Fi)value).getFiType() == Fi.Type.VAL) {
				entry = new Value(path, value);
			} else if (((Fi)value).getFiType() == Fi.Type.PROC) {
				entry = new Pro(path, value);
			} else if (((Fi)value).getFiType() == Fi.Type.ENTRY) {
                ((Fidelity)value).setName(path);
				entry = new Entry(path, value);
			} else if (((Fi)value).getFiType() == Fi.Type.SRV) {
				entry = new Srv(path, value);
			}
		} else if (value instanceof MultiFiMogram) {
			try {
				((MultiFiMogram)value).setUnifiedName(path);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			entry = new Srv(path, value);
		} else if (value instanceof ServiceMogram) {
			entry = new MogramEntry(path, (Mogram) value);
			entry.setType(Functionality.Type.MOGRAM);
		} else if (value instanceof Service) {
			entry = new Pro(path, value);
			entry.setType(Functionality.Type.PROC);
		} else {
			entry = new Entry(path, value);
		}

		Context cxt = null;
		for (Arg arg : args) {
			cxt = (Context) Arg.selectDomain(args);
		}
		try {
			// special cases of procedural attachmnet
			if (entry instanceof Pro) {
				if (cxt != null) {
					entry.setScope(cxt);
				} else if (args.length == 1 && args[0] instanceof Subroutine) {
					entry.setScope(context((Subroutine) args[0]));
				} else if (args.length == 1 && args[0] instanceof Service) {
					entry = new Pro(path, value, args[0]);
				}
			}
		} catch (ContextException e) {
			e.printStackTrace();
		}
		entry.setValid(true);
		return entry;
	}

	public static Srv ent(Signature sig) {
		return srv(sig);
	}

	public static Slot<Fidelity, Service> ent(Fidelity selectFi, Service service) throws ConfigurationException {
		Slot<Fidelity, Service> assoc = new Slot(selectFi, service);
		if (service instanceof Fidelity) {
			Fidelity fi = (Fidelity)service;
			if (!fi.isValid()) {
				String msg = "Misconfigured entry fidelity: " + service + " for: " + selectFi;
				logger.warn(msg);
			}

			if (fi.getFiType().equals(Fi.Type.GRADIENT)) {
				// if no path set use its key - no multifidelities
				if (selectFi.getPath().equals("")) {
					selectFi.setPath(selectFi.getName());
				}
				// use a select gradient key if declared
				if (selectFi.getSelect() == null) {
					if (fi.getSelect() != null) {
						selectFi.setSelect(fi.getSelect());
					} else {
						selectFi.setSelect(selectFi.getName());
						fi.setSelect(selectFi.getName());
					}
				}
			}

			fi.setName(selectFi.getName());
			fi.setPath(selectFi.getPath());
			selectFi.setType(fi.getFiType());
		}

		return assoc;
	}

	public static Subroutine pro(String path) {
		return new Subroutine(path, null);
	}

	public static <T> TagEntry<T> ent(String path, T value, String association) {
		return new TagEntry(path, value, association);
	}

	public static Arg[] ents(String... entries)
			throws ContextException {
		ArgSet as = new ArgSet();
		for (String name : entries) {
			as.add(new Subroutine(name, Context.none));
		}
		return as.toArray();
	}

	public static Arg[] ents(Subroutine... entries)
			throws ContextException {
		ArgSet as = new ArgSet();
		for (Subroutine e : entries) {
			as.add(e);
		}
		return as.toArray();
	}

	public static Subroutine inout(Subroutine entry) {
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
