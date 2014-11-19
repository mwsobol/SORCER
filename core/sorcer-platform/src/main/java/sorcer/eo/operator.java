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

import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.transaction.Transaction;
import sorcer.co.operator.DataEntry;
import sorcer.co.tuple.*;
import sorcer.core.ComponentFidelityInfo;
import sorcer.core.SorcerConstants;
import sorcer.core.context.*;
import sorcer.core.context.model.PoolStrategy;
import sorcer.core.context.model.par.Par;
import sorcer.core.context.model.par.ParModel;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.exertion.*;
import sorcer.core.provider.*;
import sorcer.core.provider.exerter.ExertionDispatcher;
import sorcer.core.provider.rendezvous.ServiceConcatenator;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.core.provider.rendezvous.ServiceRendezvous;
import sorcer.core.provider.rendezvous.ServiceSpacer;
import sorcer.core.signature.EvaluationSignature;
import sorcer.core.signature.NetSignature;
import sorcer.core.signature.ObjectSignature;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.*;
import sorcer.service.Signature.*;
import sorcer.service.Strategy.*;
import sorcer.service.modeling.Variability;
import sorcer.util.Loop;
import sorcer.util.ObjectCloner;
import sorcer.util.ServiceAccessor;
import sorcer.util.Sorcer;
import sorcer.util.url.sos.SdbUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Logger;


/**
 * Operators used for the Exertion-Oriented Language (EOL).
 *
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class operator {

	protected static int count = 0;

	protected static final Logger logger = Logger.getLogger(operator.class.getName());

	public static void requestTime(Exertion exertion) {
		((ServiceExertion)exertion).setExecTimeRequested(true);
	}

	public static String path(List<String> attributes) {
		if (attributes.size() == 0)
			return null;
		if (attributes.size() > 1) {
			StringBuilder spr = new StringBuilder();
			for (int i = 0; i < attributes.size() - 1; i++) {
				spr.append(attributes.get(i)).append(SorcerConstants.CPS);
			}
			spr.append(attributes.get(attributes.size() - 1));
			return spr.toString();
		}
		return attributes.get(0);
	}

	public static Object revalue(Evaluation evaluation, String path,
								 Arg... entries) throws ContextException {
		Object obj = value(evaluation, path, entries);
		if (obj instanceof Evaluation) {
			obj = value((Evaluation) obj, entries);
		}
		return obj;
	}

	public static Object revalue(Object object, Arg... entries)
			throws EvaluationException {
		Object obj = null;
		if (object instanceof Evaluation) {
			obj = value((Evaluation) object, entries);
		}
		if (obj == null) {
			obj = object;
		}
		return obj;
	}

	public static String path(String... attributes) {
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

//	public static Context add(Context context, Tuple2<String, ?>... entries)
//			throws ContextException {
//		for (int i = 0; i < entries.length; i++) {
//			if (context instanceof PositionalContext) {
//				((PositionalContext)context).putValueAt(((Tuple2<String, ?>) entries[i])._1,
//						((Tuple2<String, ?>) entries[i])._2, ((PositionalContext)context).getTally()+1);
//			} else {
//				context.putValue(((Tuple2<String, ?>) entries[i])._1,
//					((Tuple2<String, ?>) entries[i])._2);
//			}
//		}
//		return context;
//	}

//	public static Context put(Context context, Tuple2... entries)
//			throws ContextException {
//		for (int i = 0; i < entries.length; i++) {
//			context.putValue(((Tuple2<String, ?>) entries[i])._1,
//					((Tuple2<String, ?>) entries[i]));
//		}
//		return context;
//	}

	public static void add(Exertion exertion, Identifiable... entries)
			throws ContextException, RemoteException {
		add(exertion.getContext(), entries);
	}

	public static void put(Exertion exertion, Identifiable... entries)
			throws ContextException, RemoteException {
		put(exertion.getContext(), entries);
	}

	public static Exertion setContext(Exertion exertion, Context context) {
		((ServiceExertion) exertion).setContext(context);
		return exertion;
	}

	public static ControlContext control(Exertion exertion)
			throws ContextException {
		return ((ServiceExertion)exertion).getControlContext();
	}

	public static ControlContext control(Exertion exertion, String childName)
			throws ContextException {
		return (ControlContext)exertion.getExertion(childName).getControlContext();
	}

	public static <T extends Object> Context cxt(T... entries)
			throws ContextException {
		return context(entries);
	}

	public static Context serviceContext(Service job) throws ContextException {
		return ((Job) job).getJobContext();
	}

	public static Context taskContext(String path, Service service) throws ContextException {
		if (service instanceof ServiceExertion) {
			return ((CompoundExertion) service).getComponentContext(path);
		} else
			throw new ContextException("Service not a compunt exetion: " + service);
	}

	public static FidelityContext fiContext(FidelityInfo... fidelityInfos)
			throws ContextException {
		return fiContext(null, fidelityInfos);
	}

	public static FidelityContext fiContext(String name, FidelityInfo... fidelityInfos)
			throws ContextException {
		FidelityContext fiCxt = new FidelityContext(name);
		for (FidelityInfo e : fidelityInfos) {
			if (e instanceof FidelityInfo) {
				try {
					fiCxt.put(e.getName(), e);
				} catch (Exception ex) {
					if (ex instanceof ContextException)
						throw (ContextException) ex;
					else
						throw new ContextException(ex);
				}
			}
		}
		return fiCxt;
	}

	public static <T extends Object> Context entModel(T... entries)
			throws ContextException {
		if (entries != null && entries.length == 1 && entries[0] instanceof Context) {
			((Context)entries[0]).setModeling(true);
			return (Context)entries[0];
		}
		Context cxt = context(entries);
		cxt.setModeling(true);
		return cxt;
	}

	public static Context target(Context context, String targetPath) {
		((ServiceContext)context).setTargetPath(targetPath);
		return context;
	}

	public static Object target(Context context) throws ContextException {
		return ((ServiceContext)context).getTarget();
	}

	public static <T extends Object> Context context(T... entries)
			throws ContextException {
		Context cxt = null;
		if (entries[0] instanceof Exertion) {
			Exertion xrt = (Exertion) entries[0];
			if (entries.length >= 2 && entries[1] instanceof String)
				xrt = ((Job) xrt).getComponentExertion((String) entries[1]);
			return xrt.getContext();
		} else if (entries[0] instanceof Link) {
			return ((Link)entries[0] ).getContext();
		} else if (entries.length == 1 && entries[0] instanceof String) {
			return new PositionalContext((String) entries[0]);
		} else if (entries.length == 2 && entries[0] instanceof String
				&& entries[1] instanceof Exertion) {
			return ((Job) entries[1]).getComponentExertion(
					(String) entries[0]).getContext();
		} else if (entries[0] instanceof Context && entries[1] instanceof List) {
			return ((ServiceContext)entries[0]).getSubcontext((List)entries[1]);
		} else {
			cxt = getPersistedContext(entries);
			if (cxt != null) return cxt;
		}
		String name = getUnknown();
		List<Tuple2<String, ?>> entryList = new ArrayList<Tuple2<String, ?>>();
		List<Par> parList = new ArrayList<Par>();
		List<Context.Type> types = new ArrayList<Context.Type>();
		List<EntryList> entryLists = new ArrayList<EntryList>();
		Complement subject = null;
		ReturnPath returnPath = null;
		ExecPath execPath = null;
		Args cxtArgs = null;
		ParameterTypes parameterTypes = null;
		target target = null;
		PoolStrategy modelStrategy = null;
		for (T o : entries) {
			if (o instanceof Complement) {
				subject = (Complement) o;
			} else if (o instanceof Args
					&& ((Args) o).args.getClass().isArray()) {
				cxtArgs = (Args) o;
			} else if (o instanceof ParameterTypes
					&& ((ParameterTypes) o).parameterTypes.getClass().isArray()) {
				parameterTypes = (ParameterTypes) o;
			} else if (o instanceof target) {
				target = (target) o;
			} else if (o instanceof ReturnPath) {
				returnPath = (ReturnPath) o;
			} else if (o instanceof ExecPath) {
				execPath = (ExecPath) o;
			} else if (o instanceof Tuple2) {
				entryList.add((Tuple2) o);
			} else if (o instanceof Context.Type) {
				types.add((Context.Type) o);
			} else if (o instanceof String) {
				name = (String) o;
			} else if (o instanceof PoolStrategy) {
				modelStrategy = (PoolStrategy) o;
			} else if (o instanceof Par) {
				parList.add((Par) o);
			} else if (o instanceof EntryList) {
				entryLists.add((EntryList) o);
			}
		}

		if (types.contains(Context.Type.ARRAY)) {
			if (subject != null)
				cxt = new ArrayContext(name, subject.path(), subject.value());
			else
				cxt = new ArrayContext(name);
		} else if (types.contains(Context.Type.LIST)) {
			if (subject != null)
				cxt = new ListContext(name, subject.path(), subject.value());
			else
				cxt = new ListContext(name);
		} else if (types.contains(Context.Type.SHARED)
				&& types.contains(Context.Type.INDEXED)) {
			cxt = new SharedIndexedContext(name);
		} else if (types.contains(Context.Type.SHARED)) {
			cxt = new SharedAssociativeContext(name);
		} else if (types.contains(Context.Type.ASSOCIATIVE)) {
			if (subject != null)
				cxt = new ServiceContext(name, subject.path(), subject.value());
			else
				cxt = new ServiceContext(name);
		} else {
			if (subject != null) {
				cxt = new PositionalContext(name, subject.path(),
						subject.value());
			} else {
				cxt = new PositionalContext(name);
			}
		}
		if (cxt instanceof PositionalContext) {
			PositionalContext pcxt = (PositionalContext) cxt;
			if (entryList.size() > 0)
				popultePositionalContext(pcxt, entryList);
		} else {
			if (entryList.size() > 0)
				populteContext(cxt, entryList);
		}
		if (parList != null) {
			for (Par p : parList)
				cxt.putValue(p.getName(), p);
		}
		if (returnPath != null)
			((ServiceContext) cxt).setReturnPath(returnPath);
		if (execPath != null)
			((ServiceContext) cxt).setExecPath(execPath);
		if (cxtArgs != null) {
			if (cxtArgs.path() != null) {
				((ServiceContext) cxt).setArgsPath(cxtArgs.path());
			} else {
				((ServiceContext) cxt).setArgsPath(Context.PARAMETER_VALUES);
			}
			((ServiceContext) cxt).setArgs(cxtArgs.args);
		}
		if (parameterTypes != null) {
			if (parameterTypes.path() != null) {
				((ServiceContext) cxt).setParameterTypesPath(parameterTypes
						.path());
			} else {
				((ServiceContext) cxt)
						.setParameterTypesPath(Context.PARAMETER_TYPES);
			}
			((ServiceContext) cxt)
					.setParameterTypes(parameterTypes.parameterTypes);
		}
		if (target != null) {
			if (target.path() != null) {
				((ServiceContext) cxt).setTargetPath(target.path());
			}
			((ServiceContext) cxt).setTarget(target.target);
		}
		if (entryLists.size() > 0)
			((ServiceContext)cxt).setEntryLists(entryLists);
		return cxt;
	}

	private static Context getPersistedContext(Object... entries) throws ContextException {
		Context cxt = null;
		try {
			if (entries.length == 1 && SdbUtil.isSosURL(entries[0]))
				cxt = (Context)((URL)entries[0]).getContent();
			else if (entries.length == 2 && entries[0] instanceof String && SdbUtil.isSosURL(entries[1])) {
				cxt = (Context)((URL)entries[1]).getContent();
				cxt.setName((String)entries[0]);
			}
		} catch (IOException e) {
			throw new ContextException(e);
		}
		return cxt;
	}

	protected static void popultePositionalContext(PositionalContext pcxt,
												   List<Tuple2<String, ?>> entryList) throws ContextException {
		for (int i = 0; i < entryList.size(); i++) {
			if (entryList.get(i) instanceof InputEntry) {
				Object par = ((InputEntry)entryList.get(i)).value();
				if (par instanceof Scopable) {
					try {
						((Scopable)par).setScope(pcxt);
					} catch (RemoteException e) {
						throw new ContextException(e);
					}
				}
				if (((InputEntry) entryList.get(i)).isPersistent()) {
					setPar(pcxt, (InputEntry) entryList.get(i), i);
				} else {
					pcxt.putInValueAt(((InputEntry) entryList.get(i)).path(),
							((InputEntry) entryList.get(i)).value(), i + 1);
				}
			} else if (entryList.get(i) instanceof OutputEntry) {
				if (((OutputEntry) entryList.get(i)).isPersistent()) {
					setPar(pcxt, (OutputEntry) entryList.get(i), i);
				} else {
					pcxt.putOutValueAt(((OutputEntry) entryList.get(i)).path(),
							((OutputEntry) entryList.get(i)).value(), i + 1);
				}
			} else if (entryList.get(i) instanceof InoutEntry) {
				if (((InoutEntry) entryList.get(i)).isPersistent()) {
					setPar(pcxt, (InoutEntry) entryList.get(i), i);
				} else {
					pcxt.putInoutValueAt(
							((InoutEntry) entryList.get(i)).path(),
							((InoutEntry) entryList.get(i)).value(), i + 1);
				}
			} else if (entryList.get(i) instanceof Entry) {
				if (((Entry) entryList.get(i)).isPersistent()) {
					setPar(pcxt, (Entry) entryList.get(i), i);
				} else {
					pcxt.putValueAt(((Entry) entryList.get(i)).path(),
							((Entry) entryList.get(i)).value(), i + 1);
				}
			} else if (entryList.get(i) instanceof DataEntry) {
				pcxt.putValueAt(Context.DSD_PATH,
						((DataEntry) entryList.get(i)).value(), i + 1);
			}
		}
	}

	protected static void populteContext(Context cxt,
										 List<Tuple2<String, ?>> entryList) throws ContextException {
		for (int i = 0; i < entryList.size(); i++) {
			if (entryList.get(i) instanceof InputEntry) {
				if (((InputEntry) entryList.get(i)).isPersistent()) {
					setPar(cxt, (InputEntry) entryList.get(i));
				} else {
					cxt.putInValue(((Entry) entryList.get(i)).path(),
							((Entry) entryList.get(i)).value());
				}
			} else if (entryList.get(i) instanceof OutputEntry) {
				if (((OutputEntry) entryList.get(i)).isPersistent()) {
					setPar(cxt, (OutputEntry) entryList.get(i));
				} else {
					cxt.putOutValue(((Entry) entryList.get(i)).path(),
							((Entry) entryList.get(i)).value());
				}
			} else if (entryList.get(i) instanceof InoutEntry) {
				if (((InoutEntry) entryList.get(i)).isPersistent()) {
					setPar(cxt, (InoutEntry) entryList.get(i));
				} else {
					cxt.putInoutValue(((Entry) entryList.get(i)).path(),
							((Entry) entryList.get(i)).value());
				}
			} else if (entryList.get(i) instanceof Entry) {
				if (((Entry) entryList.get(i)).isPersistent()) {
					setPar(cxt, (Entry) entryList.get(i));
				} else {
					cxt.putValue(((Entry) entryList.get(i)).path(),
							((Entry) entryList.get(i)).value());
				}
			} else if (entryList.get(i) instanceof DataEntry) {
				cxt.putValue(Context.DSD_PATH,
						((Entry) entryList.get(i)).value());
			}
		}
	}

	public static Context add(Context context, Identifiable... objects)
			throws RemoteException, ContextException {
		boolean isReactive = false;
		for (Identifiable i : objects) {
			if (i instanceof Reactive && ((Reactive)i).isReactive()) {
				isReactive = true;
			}
			if (context instanceof PositionalContext) {
				PositionalContext pc = (PositionalContext)context;
				if (i instanceof InputEntry) {
					if (isReactive) {
						pc.putInValueAt(i.getName(), i, pc.getTally()+1);
					} else {
						pc.putInValueAt(i.getName(), ((Entry) i).value(), pc.getTally()+1);
					}
				} else if (i instanceof OutputEntry) {
					if (isReactive) {
						pc.putOutValueAt(i.getName(), i, pc.getTally()+1);
					} else {
						pc.putOutValueAt(i.getName(), ((Entry) i).value(), pc.getTally()+1);
					}
				} else if (i instanceof InoutEntry) {
					if (isReactive) {
						pc.putInoutValueAt(i.getName(), i, pc.getTally()+1);
					} else {
						pc.putInoutValueAt(i.getName(), ((Entry) i).value(), pc.getTally()+1);
					}
				} else {
					if (isReactive) {
						pc.putValueAt(i.getName(), i, pc.getTally()+1);
					} else {
						pc.putValueAt(i.getName(), ((Entry) i).value(), pc.getTally()+1);
					}
				}
			} else if (context instanceof ServiceContext) {
				if (i instanceof InputEntry) {
					if (i instanceof Reactive) {
						context.putInValue(i.getName(), i);
					} else {
						context.putInValue(i.getName(), ((Entry) i).value());
					}
				} else if (i instanceof OutputEntry) {
					if (isReactive) {
						context.putOutValue(i.getName(), i);
					} else {
						context.putOutValue(i.getName(), ((Entry) i).value());
					}
				} else if (i instanceof InoutEntry) {
					if (isReactive) {
						context.putInoutValue(i.getName(), i);
					} else {
						context.putInoutValue(i.getName(), ((Entry) i).value());
					}
				} else {
					if (isReactive) {
						context.putValue(i.getName(), i);
					} else {
						context.putValue(i.getName(), ((Entry) i).value());
					}
				}
			} else {
				context.putValue(i.getName(), i);
			}
			if (i instanceof Entry) {
				Entry e = (Entry)i;
				if (e.isAnnotated()) context.mark(e.path(), e.annotation());
				if (e.asis() instanceof Scopable) {
					((Scopable)e.asis()).setScope(context);
				}
			}
		}
		return context;
	}

	public static Context put(Context context, String path, Object value)
			throws ContextException {
		Object val = context.asis(path);
		if (SdbUtil.isSosURL(val)) {
			try {
				SdbUtil.update((URL)val, value);
			} catch (Exception e) {
				throw new ContextException(e);
			}
		}
		context.putValue(path, value);
		return context;
	}

	public static Context put(Context context, Identifiable... objects)
			throws RemoteException, ContextException {
		for (Identifiable i : objects) {
			if (context instanceof PositionalContext) {
				PositionalContext pc = (PositionalContext)context;
				if (i instanceof InputEntry) {
					pc.putInValueAt(i.getName(), i, pc.getTally()+1);
				} else if (i instanceof OutputEntry) {
					pc.putOutValueAt(i.getName(), i, pc.getTally()+1);
				} else if (i instanceof InoutEntry) {
					pc.putInoutValueAt(i.getName(), i, pc.getTally()+1);
				} else {
					pc.putValueAt(i.getName(), i, pc.getTally()+1);
				}
			} else if (context instanceof ServiceContext) {
				if (i instanceof InputEntry) {
					context.putInValue(i.getName(), i);
				} else if (i instanceof OutputEntry) {
					context.putOutValue(i.getName(), i);
				} else if (i instanceof InoutEntry) {
					context.putInoutValue(i.getName(), i);
				} else {
					context.putValue(i.getName(), i);
				}
			} else {
				context.putValue(i.getName(), i);
			}
			if (i instanceof Entry) {
				Entry e = (Entry)i;
				if (e.isAnnotated()) context.mark(e.path(), e.annotation());
				if (e.asis() instanceof Scopable) {
					((Scopable)e.asis()).setScope(context);
				}
			}
		}
		return context;
	}

	protected static void setPar(PositionalContext pcxt, Tuple2 entry, int i)
			throws ContextException {
		Par p;
		try {
			p = new Par(entry.path(), entry.value());
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
		p.setPersistent(true);
		if (entry instanceof InputEntry)
			pcxt.putInValueAt(entry.path(), p, i + 1);
		else if (entry instanceof OutputEntry)
			pcxt.putOutValueAt(entry.path(), p, i + 1);
		else if (entry instanceof InoutEntry)
			pcxt.putInoutValueAt(entry.path(), p, i + 1);
		else
			pcxt.putValueAt(entry.path(), p, i + 1);
	}

	protected static void setPar(Context cxt, Tuple2 entry)
			throws ContextException {
		Par p;
		try {
			p = new Par(entry.path(), entry.value());
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
		p.setPersistent(true);
		if (entry instanceof InputEntry)
			cxt.putInValue(entry.path(), p);
		else if (entry instanceof OutputEntry)
			cxt.putOutValue(entry.path(), p);
		else if (entry instanceof InoutEntry)
			cxt.putInoutValue(entry.path(), p);
		else
			cxt.putValue(entry.path(), p);
	}

	public static List<String> names(List<? extends Identifiable> list) {
		List<String> names = new ArrayList<String>(list.size());
		for (Identifiable i : list) {
			names.add(i.getName());
		}
		return names;
	}

	public static String name(Object identifiable) {
		if (identifiable instanceof Identifiable)
			return ((Identifiable) identifiable).getName();
		else
			return null;
	}

	public static List<String> names(Identifiable... array) {
		List<String> names = new ArrayList<String>(array.length);
		for (Identifiable i : array) {
			names.add(i.getName());
		}
		return names;
	}

	public static List<Entry> attributes(Entry... entries) {
		List<Entry> el = new ArrayList<Entry>(entries.length);
		for (Entry e : entries)
			el.add(e);
		return el;
	}

	/**
	 * Makes this Paradigm a model, so its return values of Evaluation type are
	 * evaluated. Given entries update the model to be evaluated,
	 *
	 * @param paradigm
	 *            to be marked as a model
	 * @return a Paradigm being a model
	 * @throws EvaluationException
	 * @throws RemoteException
	 */
	public static <T> T evaluate(Paradigmatic paradigm, Arg... entries)
			throws EvaluationException, RemoteException {
		if (entries != null && entries.length > 0) {
			if (paradigm instanceof Evaluation)
				return ((Evaluation<T>) paradigm).getValue(entries);
		}
		return null;
	}

	public static Paradigmatic model(Paradigmatic paradigm) {
		paradigm.setModeling(true);
		return paradigm;
	}

	public static Paradigmatic modeling(Paradigmatic paradigm, boolean modeling) {
		paradigm.setModeling(modeling);
		return paradigm;
	}

	/**
	 * Returns the Evaluation with a realized substitution for its arguments.
	 *
	 * @param evaluation
	 * @param entries
	 * @return an evaluation with a realized substitution
	 * @throws EvaluationException
	 * @throws RemoteException
	 */
	public static Evaluation substitute(Evaluation evaluation, Arg... entries)
			throws SetterException, RemoteException {
		return evaluation.substitute(entries);
	}

	public static Class type(Signature signature) {
		return signature.getServiceType();
	}

	public static String selector(Signature signature) {
		return signature.getSelector();
	}

//	public static Signature sig(Class<?> serviceType, String providerName,
//			Arg... parameters) throws SignatureException {
//		return sig(null, serviceType, new Sorcer.getActualName(providerName),
//				parameters);
//	}

//	public static Signature sig(String operation, Class serviceType,
//			String providerName) throws SignatureException {
//		return sig(operation, serviceType, providerName, (Arg[])null);
//	}

//	public static Signature sig(String operation, Class serviceType, 
//			String providerName, Arg... args)
//			throws SignatureException {

	public static Signature sig(String operation, Class serviceType)
			throws SignatureException {
		return sig(operation, serviceType, new Arg[] {});
	}

	public static Signature sig(String operation, Class serviceType,
								String initSetector) throws SignatureException {
		try {
			return new ObjectSignature(operation, serviceType, initSetector,
					(Class<?>[])null, (Object[])null);
		} catch (Exception e) {
			throw new SignatureException(e);
		}
	}

	public static Signature sig(String operation, Class serviceType,  Arg... args)
			throws SignatureException {
		String providerName = null;
		if (args != null) {
			for (Object o : args) {
				if (o instanceof ProviderName)
					providerName = Sorcer.getActualName(((ProviderName)o).getName());
			}
		}
		Signature sig = null;
		if (serviceType.isInterface()) {
			sig = new NetSignature(operation, serviceType, providerName);
		} else {
			sig = new ObjectSignature(operation, serviceType);
			sig.setProviderName(providerName);
		}

		if (args.length > 0) {
			Provision p = null;
			for (Object o : args) {
				if (o instanceof Type) {
					sig.setType((Type) o);
				} else if (o instanceof Operating) {
					((ServiceSignature)sig).setActive((Operating) o);
				} else if (o instanceof Provision) {
					p = (Provision)o;
					((ServiceSignature)sig).setProvisionable((Provision) o);
				} else if (o instanceof ReturnPath) {
					sig.setReturnPath((ReturnPath) o);
				} else if (o instanceof ServiceDeployment) {
					if (p != null)
						((ServiceDeployment)o).setProvisionable(p);
					((ServiceSignature)sig).setDeployment((ServiceDeployment)o);
				}
			}
		}
		return sig;
	}

	public static ProviderName prvName(String name) {
		return new ProviderName(name);
	}

	public static String actualName(String name) {
		return 	Sorcer.getActualName(name);
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
		return signture;
	}

//	public static Signature sig(String operation, Class serviceType,
//			List<net.jini.core.entry.Entry> attributes)
//			throws SignatureException {
//		NetSignature op = new NetSignature();
//		op.setAttributes(attributes);
//		op.setServiceType(serviceType);
//		op.setSelector(operation);
//		return op;
//	}

	public static Signature sig(Class<?> serviceType) throws SignatureException {
		if (serviceType == ServiceJobber.class ||
				serviceType == ServiceSpacer.class ||
				serviceType == ServiceConcatenator.class ||
				serviceType == ServiceRendezvous.class) {
			return sig("execute", serviceType);
		} else
			return sig(serviceType, (ReturnPath) null);
	}

	public static Signature sig(Class<?> serviceType, ReturnPath returnPath, ServiceDeployment deployment)
			throws SignatureException {
		Signature signature = sig(serviceType, returnPath);
		((ServiceSignature)signature).setDeployment(deployment);
		return signature;
	}

	public static Signature sig(Class<?> serviceType, ReturnPath returnPath)
			throws SignatureException {
		Signature sig = null;
		if (serviceType.isInterface()) {
			sig = new NetSignature("service", serviceType);
		} else if (Executor.class.isAssignableFrom(serviceType)) {
			sig = new ObjectSignature("execute", serviceType);
		} else {
			sig = new ObjectSignature(serviceType);
		}
		if (returnPath != null)
			sig.setReturnPath(returnPath);
		return sig;
	}

	public static EvaluationSignature sig(Evaluator evaluator,
										  ReturnPath returnPath) throws SignatureException {
		EvaluationSignature sig = null;
		if (evaluator instanceof Scopable) {
			sig = new EvaluationSignature(new Par((Identifiable)evaluator));
		} else {
			sig = new EvaluationSignature(evaluator);
		}
		sig.setReturnPath(returnPath);
		return sig;
	}

	public static EvaluationSignature sig(Evaluator evaluator) throws SignatureException {
		return new EvaluationSignature(evaluator);
	}

	public static Signature sig(Exertion exertion, String componentExertionName) {
		Exertion component = exertion.getExertion(componentExertionName);
		return component.getProcessSignature();
	}


	public static EvaluationTask task(EvaluationSignature signature)
			throws ExertionException {
		return new EvaluationTask(signature);
	}

	public static EvaluationTask task(Evaluation evaluator) throws ExertionException, SignatureException {
		return new EvaluationTask(evaluator);
	}

	public static Signature type(Signature signature, Signature.Type type) {
		signature.setType(type);
		return signature;
	}

	public static EvaluationTask task(EvaluationSignature signature,
									  Context context) throws ExertionException {
		return new EvaluationTask(signature, context);
	}

//	public static ObjectSignature sig(String operation, Object serviceType, ServiceDeployment deployment,
//			Class... types) throws SignatureException {
//		ObjectSignature signature = sig(operation, serviceType, types);
//		signature.setDeployment(deployment);
//		return signature;
//	}

	public static FidelityInfo srvFi(String name) {
		return new FidelityInfo(name);
	}

	public static FidelityInfo srvFi(String name, String... selectors) {
		return new FidelityInfo(name, selectors);
	}

	public static ComponentFidelityInfo csFi(String path, String name) {
		return new ComponentFidelityInfo(name, path);
	}


	public static ComponentFidelityInfo csFi(String path, String name, String... selectors) {
		return new ComponentFidelityInfo(name, path, selectors);
	}


	public static ServiceFidelity srvFi(Exertion exertion) {
		return exertion.getFidelity();
	}

	public static Map<String, ServiceFidelity> srvFis(Exertion exertion) {
		return exertion.getFidelities();
	}

	public static ServiceFidelity srvFi(Signature... signatures) {
		return new ServiceFidelity(signatures);
	}

	public static ServiceFidelity srvFi(String name, Signature... signatures) {
		return new ServiceFidelity(name, signatures);
	}

	public static ObjectSignature sig(String operation, Object object)
			throws SignatureException {
		return sig(operation, object, (String) null, null, null);
	}

	public static ObjectSignature sig(String operation, Object object,
									  Class[] types, Object... args) throws SignatureException {
		if (args == null || args.length == 0)
			return sig(operation, object, (String)null, types);
		else
			return sig(operation, object, (String)null, types, args);
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

	public static ObjectSignature sig(String selector, Object object, String initSelector,
									  Class[] types, Object[] args) throws SignatureException {
		try {
			return new ObjectSignature(selector, object, initSelector, types, args);
		} catch (Exception e) {
			e.printStackTrace();
			throw new SignatureException(e);
		}
	}

	public static ObjectTask task(ObjectSignature signature)
			throws SignatureException {
		return new ObjectTask(signature.getSelector(),
				(ObjectSignature) signature);
	}

	public static ObjectTask task(ObjectSignature signature, Context context)
			throws SignatureException {
		return new ObjectTask(signature.getSelector(),
				(ObjectSignature) signature, context);
	}

	public static Task task(String name, Signature signature, Context context)
			throws SignatureException {
		Task task = task(signature, context);
		task.setName(name);
		return task;
	}

	public static Task task(Signature signature, Context context)
			throws SignatureException {
		Task task = null;
		if (signature instanceof NetSignature) {
			task = new NetTask((NetSignature) signature, context);
		} else if (signature instanceof ObjectSignature) {
			task = new ObjectTask((ObjectSignature) signature, context);
		} else if (signature instanceof EvaluationSignature) {
			task = new EvaluationTask((EvaluationSignature) signature,
					context);
		} else {
			task = new Task(signature, context);
		}
		return task;
	}

	public static <T> Task batch(String name, T... elems)
			throws ExertionException {
		Task batch = task(name, elems);
		if (batch.getFidelity().size() > 1)
			return batch;
		else
			throw new ExertionException(
					"A batch should comprise of more than one signature.");
	}

	public static <T> Task task(String name, T... elems)
			throws ExertionException {
		Context context = null;
		List<Signature> ops = new ArrayList<Signature>();
		String tname;
		if (name == null || name.length() == 0)
			tname = getUnknown();
		else
			tname = name;
		Task task = null;
		Access access = null;
		Flow flow = null;
		List<ServiceFidelity> fidelities = null;
		ControlContext cc = null;
		for (Object o : elems) {
			if (o instanceof ControlContext) {
				cc = (ControlContext) o;
			} else if (o instanceof Context) {
				context = (Context) o;
			} else if (o instanceof Signature) {
				ops.add((Signature) o);
			} else if (o instanceof String) {
				tname = (String) o;
			} else if (o instanceof Access) {
				access = (Access) o;
			} else if (o instanceof Flow) {
				flow = (Flow) o;
			} else if (o instanceof ServiceFidelity) {
				if (fidelities == null)
					fidelities = new ArrayList<ServiceFidelity>();
				fidelities.add((ServiceFidelity) o);
			}
		}
		Signature ss = null;
		if (ops.size() == 1) {
			ss = ops.get(0);
		} else if (ops.size() > 1) {
			for (Signature s : ops) {
				if (s.getType() == Signature.SRV) {
					ss = s;
					break;
				}
			}
		}
		if (ss != null) {
			if (ss instanceof NetSignature) {
				try {
					task = new NetTask(tname, (NetSignature) ss);
				} catch (SignatureException e) {
					throw new ExertionException(e);
				}
			} else if (ss instanceof ObjectSignature) {
				task = new ObjectTask(ss.getSelector(), (ObjectSignature) ss);
				task.setName(tname);
			} else if (ss instanceof EvaluationSignature) {
				task = new EvaluationTask(tname, (EvaluationSignature) ss);
			} else if (ss instanceof ServiceSignature) {
				task = new Task(tname, ss);
			}
			ops.remove(ss);
		}
		if (fidelities != null && fidelities.size() > 0) {
			task = new Task(tname);
			for (int i = 0; i < fidelities.size(); i++) {
				task.addFidelity(fidelities.get(i));
			}
			task.setFidelity(fidelities.get(0));
			task.setSelectedFidelitySelector(fidelities.get(0).getName());
		} else {
			for (Signature signature : ops) {
				task.addSignature(signature);
			}
		}

		if (context == null) {
			context = new ServiceContext();
		}
		task.setContext(context);

		if (access != null) {
			task.setAccess(access);
		}
		if (flow != null) {
			task.setFlow(flow);
		}
		if (cc != null) {
			task.updateStrategy(cc);
		}
		return task;
	}

	public static <T extends Object, E extends Exertion> E srv(String name,
															   T... elems) throws ExertionException, ContextException,
			SignatureException {
		return (E) exertion(name, elems);
	}

	public static <T extends Object, E extends Exertion> E service(T... items)
			throws ExertionException, ContextException, SignatureException {
		String name = "unknown" + count++;
		for (T i : items) {
			if (i instanceof String) {
				name = (String)i;
			}
		}
		return (E) exertion(name, items);
	}

//	public static <T extends Object, E extends Exertion> E service(String name,
//			T... elems) throws ExertionException, ContextException,
//			SignatureException {
//		return (E) exertion(name, elems);
//	}

	public static <T extends Object, E extends Exertion> E xrt(String name,
															   T... elems) throws ExertionException, ContextException,
			SignatureException {
		return (E) exertion(name, elems);
	}

	public static <T extends Object, E extends Exertion> E exertion(
			String name, T... elems) throws ExertionException,
			ContextException, SignatureException {
		List<Exertion> exertions = new ArrayList<Exertion>();
		for (int i = 0; i < elems.length; i++) {
			if (elems[i] instanceof Exertion) {
				exertions.add((Exertion) elems[i]);
			}
		}
		if (exertions.size() > 0) {
			Job j = job(elems);
			j.setName(name);
			return (E) j;
		} else {
			return (E)task(name, elems);
		}
	}

	public static <T> Job job(T... elems) throws ExertionException,
			ContextException, SignatureException {
		String name = getUnknown();
		Signature signature = null;
		ControlContext control = null;
		Context<?> data = null;
		ReturnPath rp = null;
		List<Exertion> exertions = new ArrayList<Exertion>();
		List<Pipe> pipes = new ArrayList<Pipe>();
		List<ServiceFidelity> fidelities = null;
		List<FidelityContext> fiContexts = null;

		for (int i = 0; i < elems.length; i++) {
			if (elems[i] instanceof String) {
				name = (String) elems[i];
			} else if (elems[i] instanceof Exertion) {
				exertions.add((Exertion) elems[i]);
			} else if (elems[i] instanceof ControlContext) {
				control = (ControlContext) elems[i];
			} else if (elems[i] instanceof Context) {
				data = (Context<?>) elems[i];
			} else if (elems[i] instanceof Pipe) {
				pipes.add((Pipe) elems[i]);
			} else if (elems[i] instanceof Signature) {
				signature = ((Signature) elems[i]);
			} else if (elems[i] instanceof ReturnPath) {
				rp = ((ReturnPath) elems[i]);
			} else if (elems[i] instanceof ServiceFidelity) {
				if (fidelities == null)
					fidelities = new ArrayList<ServiceFidelity>();
				fidelities.add((ServiceFidelity) elems[i]);
			} else if (elems[i] instanceof FidelityContext) {
				if (fiContexts == null)
					fiContexts = new ArrayList<FidelityContext>();
				fiContexts.add((FidelityContext) elems[i]);
			}

		}
		Job job = null;
		boolean defaultSig = false;
		if (signature == null && fidelities == null) {
			signature = sig("service", Jobber.class);
			defaultSig = true;
		}
		if (signature instanceof NetSignature) {
			job = new NetJob(name);
		} else if (signature instanceof ObjectSignature) {
			job = new ObjectJob(name);
		}
		if (fidelities == null) {
			if (!defaultSig) {
				job.getFidelity().clear();
				job.addSignature(signature);
			} else {
				job.addSignature(signature);
			}
		} else {
			job = new Job(name);
			for (int i = 0; i < fidelities.size(); i++) {
				job.addFidelity(fidelities.get(i));
			}
			job.setFidelity(fidelities.get(0));
			job.setSelectedFidelitySelector(fidelities.get(0).getName());
		}

		if (data != null)
			job.setContext(data);

		if (rp != null) {
			((ServiceContext) job.getDataContext()).setReturnPath(rp);
		}

		if (job instanceof NetJob && control != null) {
			job.setControlContext(control);
			if (control.getAccessType().equals(Access.PULL)) {
				Signature procSig = job.getProcessSignature();
				procSig.setServiceType(Spacer.class);
				job.getFidelity().clear();
				job.addSignature(procSig);
				if (data != null)
					job.setContext(data);
				else
					job.getDataContext().setExertion(job);
			}
		}
		if (fiContexts != null) {
			Map<String, FidelityContext> fiMap = new HashMap<String, FidelityContext>();
			for (FidelityContext fiCxt : fiContexts) {
				fiMap.put(fiCxt.getName(), fiCxt);
				job.setFidelityContexts(fiMap);
			}
		}
		if (exertions.size() > 0) {
			for (Exertion ex : exertions) {
				job.addExertion(ex);
			}
			for (Pipe p : pipes) {
//				logger.finer("from context: "
//						+ ((Exertion) p.in).getDataContext().getName()
//						+ " path: " + p.inPath);
//				logger.finer("to context: "
//						+ ((Exertion) p.out).getDataContext().getName()
//						+ " path: " + p.outPath);
				// find component exertions for thir paths
				if (!p.isExertional()) {
					p.out = job.getComponentExertion(p.outComponentPath);
					p.in = job.getComponentExertion(p.inComponentPath);
				}
				((Exertion) p.out).getDataContext().connect(p.outPath,
						p.inPath, ((Exertion) p.in).getContext());
			}
		} else
			throw new ExertionException("No component exertion defined.");

		return job;
	}

	public static Object get(Context context) throws ContextException,
			RemoteException {
		return ((ServiceContext) context).getReturnValue();
	}

	public static Object get(Context context, int index)
			throws ContextException {
		if (context instanceof PositionalContext)
			return ((PositionalContext) context).getValueAt(index);
		else
			throw new ContextException("Not PositionalContext, index: " + index);
	}

	public static Object get(Exertion exertion) throws ContextException,
			RemoteException {
		return ((ServiceContext) exertion.getContext()).getReturnValue();
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
					"asis value can only be determined for objects of the "
							+ Evaluation.class + " type");
		}
	}

	public static <V> V take(Variability<V> variability)
			throws EvaluationException {
		try {
			synchronized (variability) {
				variability.valueChanged(null);
				V val = variability.getValue();
				variability.valueChanged(null);
				return val;
			}
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
	}

	public static Object get(Exertion exertion, String component, String path)
			throws ExertionException {
		Exertion c = ((Exertion) exertion).getExertion(component);
		return get((Exertion) c, path);
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

	public static <V> V pathValue(Mappable<V> map, String path, Arg... args) throws ContextException {
		return map.getValue(path, args);
	}

	public static <T> T value(Evaluation<T> evaluation)
			throws EvaluationException {
		try {
			return evaluation.getValue();
		} catch (RemoteException e) {
			throw new EvaluationException(e);
		}
	}

//	public static <T> T value(Mappable<T> map, String path) throws ContextException {
//		return map.getValue(path);
//	}

	public static Object content(URL url) throws EvaluationException {
		if (url instanceof URL) {
			try {
				return ((URL) url).getContent();
			} catch (Exception e) {
				throw new EvaluationException(e);
			}
		} else {
			throw new EvaluationException("Expected URL for its content");
		}
	}



	public static Object value(Object obj) throws EvaluationException {
		try {
			if (obj instanceof URL)
				return ((URL)obj).getContent();
			else  if(obj instanceof Evaluation)
				return ((Evaluation)obj).getValue();
			else
				return obj;
		} catch (IOException e) {
			throw new EvaluationException(e);
		}
	}

	public static <T> T value(Evaluation<T> evaluation, Arg... entries)
			throws EvaluationException {
		try {
			synchronized (evaluation) {
				if (evaluation instanceof ParModel) {
					return ((ParModel<T>) evaluation).getValue(entries);
				} else if (evaluation instanceof Exertion) {
					ReturnPath rp = ((ServiceContext)((Exertion)evaluation).getDataContext()).getReturnPath();
					return (T) execExertion((Exertion) evaluation, rp,
							entries);
				} else if (evaluation instanceof Par){
					return ((Par<T>)evaluation).getValue(entries);
				} else if (evaluation instanceof Entry){
					return ((Entry<T>)evaluation).getValue(entries);
				} else {
					return evaluation.getValue(entries);
				}
			}
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
	}

	public static <T> T value(Evaluation<T> evaluation, String evalSelector,
							  Arg... entries) throws EvaluationException {
		if (evaluation instanceof ParModel) {
			try {
				return (T) ((ParModel) evaluation).getValue(evalSelector,
						entries);
			} catch (ContextException e) {
				throw new EvaluationException(e);
			}
		} else if (evaluation instanceof Exertion) {
			try {
				return (T) execExertion((Exertion) evaluation, new ReturnPath(evalSelector),
						entries);
			} catch (Exception e) {
				e.printStackTrace();
				throw new EvaluationException(e);
			}
		} else if (evaluation instanceof Context) {
			try {
				Object val = ((Context) evaluation).getValue(evalSelector,
						entries);
				if (SdbUtil.isSosURL(val)) {
					return (T) ((URL) val).getContent();
				} else {
					return (T)val;
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new EvaluationException(e);
			}
		}
		return null;
	}

	/**
	 * Assigns the maker for this context, for example "triplet|one|two|three" is a
	 * marker (relation) named 'triplet' as a product of three "places" _1, _2, _3.
	 *
	 * @param context
	 * @param marker
	 * @throws ContextException
	 */
	public static void marker(Context context, String marker)
			throws ContextException {
		context.setAttribute(marker);
	}

	/**
	 * Associates a given path in this context with a tuple of a marker
	 * defined for this context. If a marker, for example, is
	 * "triplet|one|two|three" then its tuple can be "triplet|mike|w|sobol" where
	 * 'triplet' is the name of relation and its proper tuple is 'mike|w|sobol'.
	 *
	 * @param context
	 * @param path
	 * @param tuple
	 * @return
	 * @throws ContextException
	 */
	public static Context mark(Context context, String path, String tuple)
			throws ContextException {
		return context.mark(path, tuple);
	}

	public static <T> List<T> valuesAt(Context<T> context, String tuple) throws ContextException {
		return context.getMarkedValues(tuple);
	}

	public static <T> T getAt(Context<T> context, String tuple) throws ContextException {
		return valuesAt(context, tuple).get(0);
	}

	public static <T> List<T> inValues(Context<T> context) throws ContextException {
		return ((ServiceContext)context).getInValues();
	}

	public static <T> List<T> inPaths(Context<T> context) throws ContextException {
		return ((ServiceContext)context).getInPaths();
	}

	public static <T> List<T> outValues(Context<T> context) throws ContextException {
		return ((ServiceContext)context).getOutValues();
	}

	public static <T> List<T> outPaths(Context<T> context) throws ContextException {
		return ((ServiceContext)context).getOutPaths();
	}

	public static <T> T getAt(Context<T> context, int i) throws ContextException {
		if (!(context instanceof Positioning))
			throw new ContextException("Not positional Context: " + context.getName());
		return ((List<T>)context.getMarkedValues("i|" + i)).get(0);
	}

	public static <T> List<T> select(Context<T> context, int... positions) throws ContextException {
		List<T> values = new ArrayList<T>(positions.length);
		for (int i : positions) {
			values.add(getAt(context, i));
		}
		return values;
	}

	public static Object asis(Mappable mappable, String path)
			throws ContextException {
		return  mappable.asis(path);
	}

	public static <T> T get(Service<T> service, String path)
			throws ContextException, ExertionException {
		if (service instanceof Exertion)
			return (T) get((Exertion) service, path);
		Object obj = ((ServiceContext) service).asis(path);
		if (obj != null) {
			while (obj instanceof Mappable ||
					(obj instanceof Reactive && ((Reactive)obj).isReactive())) {
				try {
					obj = ((Evaluation) obj).asis();
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
			}
		} else {
			obj = ((ServiceContext) service).getValue(path);
		}
		return (T)obj;
	}

	public static List<Exertion> exertions(Exertion xrt) {
		return xrt.getAllExertions();
	}

	public static Exertion exertion(Exertion xrt, String componentExertionName) {
		return xrt.getComponentExertion(componentExertionName);
	}

	public static List<String> trace(Exertion xrt) {
		return ((ControlContext) xrt.getControlContext()).getTrace();
	}

	public static void print(Object obj) {
		System.out.println(obj.toString());
	}

	public static Object exec(Context context, Arg... args)
			throws ExertionException, ContextException {
		try {
			context.substitute(args);
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
		ReturnPath returnPath = ((ServiceContext)context).getReturnPath();
		if (returnPath != null) {
			return context.getValue(returnPath.path, args);
		} else
			throw new ExertionException("No return path in the context: "
					+ context.getName());
	}

	public static Object execExertion(Exertion exertion, ReturnPath rPath,
									  Arg... args) throws ExertionException, ContextException,
			RemoteException {
		Exertion xrt;
		try {
			if (exertion.getClass() == Task.class) {
				if (((Task) exertion).getDelegate() != null)
					xrt = exert(((Task) exertion).getDelegate(), null, args);
				else
					xrt = exertOpenTask(exertion, args);
			} else {
				xrt = exert(exertion, null, args);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ExertionException(e);
		}
		Context cxt = xrt.getContext();
		Context dcxt = xrt.getDataContext();
		if (rPath != null) {
			// if Path.argPaths.length > 1 return subcontext
			if (rPath.argPaths != null && rPath.argPaths.length == 1) {
				Object val = cxt.getValue(rPath.argPaths[0]);
				dcxt.putValue(rPath.path, val);
				return val;
			} else {
				ReturnPath rp = ((ServiceContext) dcxt).getReturnPath();
				if (rp != null && rPath.path != null) {
					Object result = cxt.getValue(rp.path);
					if (result instanceof Context)
						return ((Context) cxt.getValue(rp.path))
								.getValue(rPath.path);
					else
						return result;
				} else {
					return xrt.getContext().getValue(rPath.path);
				}
			}
		}
		Object obj = ((ServiceExertion) xrt).getReturnValue(args);
		if (obj == null) {
			ReturnPath returnPath = ((ServiceContext)xrt.getDataContext()).getReturnPath();
			if (returnPath != null) {
				return ((ServiceExertion) xrt).getReturnValue(args);
			} else {
				return xrt.getContext();
			}
		} else {
			return obj;
		}
	}

	public static Exertion exertOpenTask(Exertion exertion, Arg... args)
			throws ExertionException {
		Exertion closedTask = null;
		List<Arg> params = Arrays.asList(args);
		List<Object> items = new ArrayList<Object>();
		for (Arg param : params) {
			if (param instanceof ControlContext
					&& ((ControlContext) param).getSignatures().size() > 0) {
				List<Signature> sigs = ((ControlContext) param).getSignatures();
				ControlContext cc = (ControlContext) param;
				cc.setSignatures(null);
				Context tc;
				try {
					tc = exertion.getContext();
				} catch (ContextException e) {
					throw new ExertionException(e);
				}
				items.add(tc);
				items.add(cc);
				items.addAll(sigs);
				closedTask = task(exertion.getName(), items.toArray());
			}
		}
		try {
			closedTask = closedTask.exert(args);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ExertionException(e);
		}
		return closedTask;
	}

	public static Object get(Exertion xrt, String path)
			throws ExertionException {
		try {
			return ((ServiceExertion) xrt).getValue(path);
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
	}

	public static List<ThrowableTrace> exceptions(Exertion exertion) {
		return exertion.getExceptions();
	}

	public static <T extends Service> T exert(T input, Arg... args)
			throws ExertionException {
		try {
			return  (T)((Exertion)input).exert(null, args);
		} catch (Exception e) {
			throw new ExertionException(e);
		}
	}

	public static <T extends Exertion> T exert(Exerter exerter, Exertion input,
											   Arg... entries) throws ExertionException {
		try {
			return (T) exerter.exert(input, null, entries);
		} catch (Exception e) {
			throw new ExertionException(e);
		}
	}

//	public static <T extends Exertion> T exert(Exertion input, Arg... entries)
//			throws ExertionException {
//		try {
//			return (T) exert(input, null, entries);
//		} catch (Exception e) {
//			throw new ExertionException(e);
//		}
//	}

	public static <T extends Exertion> T exert(T input,
											   Transaction transaction, Arg... entries) throws ExertionException {
		try {
			ExertionDispatcher se = new ExertionDispatcher(input);
			Exertion result = null;
			try {
				result = se.exert(transaction, null, entries);
			} catch (Exception e) {
				e.printStackTrace();
				if (result != null)
					((ServiceExertion) result).reportException(e);
			}
			return (T) result;
		} catch (Exception e) {
			throw new ExertionException(e);
		}
	}

	public static OutputEntry output(Object value) {
		return new OutputEntry(null, value, 0);
	}

	public static ReturnPath self() {
		return new ReturnPath();
	}

	public static ReturnPath result(String path, String... paths) {
		return new ReturnPath(path, paths);
	}

	public static ReturnPath result(String path, Direction direction,
									String... paths) {
		return new ReturnPath(path, direction, paths);
	}

	public static ReturnPath result(String path, Class type, String... paths) {
		return new ReturnPath(path, Direction.OUT, type, paths);
	}

	protected static String getUnknown() {
		return "unknown" + count++;
	}


	public static class Range extends Tuple2<Integer, Integer> {
		private static final long serialVersionUID = 1L;
		public Integer[] range;

		public Range(Integer from, Integer to) {
			this._1 = from;
			this._2 = to;
		}

		public Range(Integer[] range) {
			this.range = range;
		}

		public Integer[] range() {
			return range;
		}

		public int from() {
			return _1;
		}

		public int to() {
			return _2;
		}

		public String toString() {
			if (range != null)
				return Arrays.toString(range);
			else
				return "[" + _1 + "-" + _2 + "]";
		}
	}

	// putLink(String name, String path, Context linkedContext, String offset)
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
		return (ContextLink)context.getLink(path);
	}

	public static <T> ControlContext strategy(T... entries) {
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
			} else if (o instanceof Wait) {
				cc.isWait((Wait) o);
			} else if (o instanceof Signature) {
				sl.add((Signature) o);
			} else if (o instanceof Opti) {
				cc.setOpti((Opti) o);
			} else if (o instanceof Exec.State) {
				cc.setExecState((Exec.State) o);
			}
		}
		cc.setSignatures(sl);
		return cc;
	}

	public static Flow flow(Entry entry) throws EvaluationException {
		return ((Strategy)value(entry)).getFlowType();
	}

	public static Access access(Entry entry) throws EvaluationException {
		return ((Strategy)value(entry)).getAccessType();
	}

	public static Flow flow(Strategy strategy) {
		return strategy.getFlowType();
	}

	public static Access access(Strategy strategy) {
		return strategy.getAccessType();
	}

	public static EntryList inputs(Entry...  entries) {
		return initialDesign(entries);
	}

	public static EntryList initialDesign(Entry...  entries) {
		EntryList el = new EntryList(entries);
		el.setType(EntryList.Type.INITIAL_DESIGN);
		return el;
	}

	public static Object target(Object object) {
		return new target(object);
	}

	public static class target extends Path {
		private static final long serialVersionUID = 1L;
		public Object target;

		target(Object target) {
			this.target = target;
		}

		target(String path, Object target) {
			this.target = target;
			this._1 = path;
		}

		@Override
		public String toString() {
			return "target: " + target;
		}
	}

	public static class result extends Tuple2 {

		private static final long serialVersionUID = 1L;

		Class returnType;

		result(String path) {
			this._1 = path;
		}

		result(String path, Class returnType) {
			this._1 = path;
			this._2 = returnType;
		}

		public Class returnPath() {
			return (Class) this._2;
		}

		@Override
		public String toString() {
			return "return path: " + _1;
		}
	}

	public static ParameterTypes parameterTypes(Class... parameterTypes) {
		return new ParameterTypes(parameterTypes);
	}

	public static class ParameterTypes extends Path {
		private static final long serialVersionUID = 1L;
		public Class[] parameterTypes;

		public ParameterTypes(Class... parameterTypes) {
			this.parameterTypes = parameterTypes;
		}

		public ParameterTypes(String path, Class... parameterTypes) {
			this.parameterTypes = parameterTypes;
			this._1 = path;
		}

		@Override
		public String toString() {
			return "parameterTypes: " + Arrays.toString(parameterTypes);
		}
	}

	public static Args parameterValues(Object... args) {
		return new Args(args);
	}

	public static Args args(Object... args) {
		return new Args(args);
	}

	public static Args args(String path, Object... args) {
		return new Args(path, args);
	}

	public static class Args extends Path {
		private static final long serialVersionUID = 1L;

		public Object[] args;

		public Args(Object... args) {
			this.args = args;
		}

		public Args(String path, Object... args) {
			this.args = args;
			this._1 = path;
		}

		@Override
		public String toString() {
			return "args: " + Arrays.toString(args);
		}
	}

	public static class Pipe {
		String inPath;
		String outPath;
		Mappable in;
		Mappable out;
		String outComponentPath;
		String inComponentPath;

		Par par;

		Pipe(Exertion out, String outPath, Mappable in, String inPath) {
			this.out = out;
			this.outPath = outPath;
			this.in = in;
			this.inPath = inPath;
			if ((in instanceof Exertion) && (out instanceof Exertion)) {
				par = new Par(outPath, inPath, (Exertion)in);
				((ServiceExertion) out).addPersister(par);
			}
		}

		Pipe(OutEndPoint outEndPoint, InEndPoint inEndPoint) {
			this.out = outEndPoint.out;
			this.outPath = outEndPoint.outPath;
			this.outComponentPath = outEndPoint.outComponentPath;
			this.in = inEndPoint.in;
			this.inPath = inEndPoint.inPath;
			this.inComponentPath = inEndPoint.inComponentPath;

			if ((in instanceof Exertion) && (out instanceof Exertion)) {
				par = new Par(outPath, inPath, (Exertion)in);
				((ServiceExertion) out).addPersister(par);
			}
		}

		public boolean isExertional() {
			return in != null && out != null;
		}
	}

	public static Par persistent(Pipe pipe) {
		pipe.par.setPersistent(true);
		return pipe.par;
	}

	private static class InEndPoint {
		String inPath;
		Mappable in;
		String inComponentPath;

		InEndPoint(Mappable in, String inDataPath) {
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
		public Mappable out;
		public String outComponentPath;

		OutEndPoint(Mappable out, String outDataPath) {
			this.outPath = outDataPath;
			this.out = out;
		}

		OutEndPoint(String outComponentPath, String outDataPath) {
			this.outPath = outDataPath;
			this.outComponentPath = outComponentPath;
		}
	}

	public static OutEndPoint output(String outComponent, String outPath) {
		return new OutEndPoint(outComponent, outPath);
	}

	public static OutEndPoint out(String outComponent, String outPath) {
		return new OutEndPoint(outComponent, outPath);
	}

	public static OutEndPoint output(Service outExertion, String outPath) {
		return new OutEndPoint((Exertion)outExertion, outPath);
	}

	public static OutEndPoint out(Service outExertion, String outPath) {
		return new OutEndPoint((Exertion)outExertion, outPath);
	}

	public static InEndPoint input(String inComponent, String inPath) {
		return new InEndPoint(inComponent, inPath);
	}

	public static InEndPoint in(String inComponent, String inPath) {
		return new InEndPoint(inComponent, inPath);
	}

	public static InEndPoint input(Service inExertion, String inPath) {
		return new InEndPoint((Exertion)inExertion, inPath);
	}

	public static InEndPoint in(Service inExertion, String inPath) {
		return new InEndPoint((Exertion)inExertion, inPath);
	}

	public static Pipe pipe(OutEndPoint outEndPoint, InEndPoint inEndPoint) {
		Pipe p = new Pipe(outEndPoint, inEndPoint);
		return p;
	}


	public static class Complement<T2> extends Entry<T2> {
		private static final long serialVersionUID = 1L;

		Complement(String path, T2 value) {
			this._1 = path;
			this._2 = value;
		}
	}

	public static List<Service> providers(Signature signature)
			throws SignatureException {
		ServiceTemplate st = new ServiceTemplate(null,
				new Class[] { signature.getServiceType() }, null);
		ServiceItem[] sis = ServiceAccessor.getServiceItems(st, null,
				Sorcer.getLookupGroups());
		if (sis == null)
			throw new SignatureException("No available providers of type: "
					+ signature.getServiceType().getName());
		List<Service> servicers = new ArrayList<Service>(sis.length);
		for (ServiceItem si : sis) {
			servicers.add((Service) si.service);
		}
		return servicers;
	}

	public static List<Class<?>> interfaces(Object obj) {
		if (obj == null)
			return null;
		return Arrays.asList(obj.getClass().getInterfaces());
	}

	public static Object provider(Signature signature)
			throws SignatureException {
		Object target = null;
		Service provider = null;
		Class<?> providerType = null;
		if (signature.getClass() == NetSignature.class) {
			providerType = ((NetSignature) signature).getServiceType();
		} else if (signature.getClass() == ObjectSignature.class) {
			providerType = ((ObjectSignature) signature).getProviderType();
			target = ((ObjectSignature) signature).getTarget();
		}
		try {
			if (signature.getClass() == NetSignature.class) {
				provider = ((NetSignature) signature).getService();
				if (provider == null) {
					provider = Accessor.getService(signature);
					((NetSignature) signature).setProvider(provider);
				}
			} else if (signature.getClass() == ObjectSignature.class) {
				if (target != null) {
					return target;
				} else if (Provider.class.isAssignableFrom(providerType)) {
					target = providerType.newInstance();
					return target;
				} else {
					target = instance((ObjectSignature) signature);
					// utility class returns a utility (class) method
					if (target instanceof Method)
						return ((ObjectSignature)signature).getProviderType();
					else
						return target;
				}
			} else if (signature instanceof EvaluationSignature) {
				return ((EvaluationSignature) signature).getEvaluator();
			}
		} catch (Exception e) {
			throw new SignatureException("No signature provider avaialable", e);
		}
		return provider;
	}

	/**
	 * Returns an instance by constructor method initialization or by
	 * instance/class method initialization.
	 *
	 * @param signature
	 * @return object created
	 * @throws SignatureException
	 */
	public static Object instance(Signature signature)
			throws SignatureException {
		if (signature.getSelector() == null
				|| signature.getSelector().equals("new"))
			return ((ObjectSignature) signature).newInstance();
		else
			return ((ObjectSignature) signature).initInstance();
	}

	/**
	 * Returns an instance by class method initialization with a service
	 * context.
	 *
	 * @param signature
	 * @return object created
	 * @throws SignatureException
	 */
	public static Object instance(ObjectSignature signature, Context context)
			throws SignatureException {
		return signature.build(context);
	}

	public static Condition condition(ParModel parcontext, String expression,
									  String... pars) {
		return new Condition(parcontext, expression, pars);
	}

	public static Condition condition(String expression,
									  String... pars) {
		return new Condition(expression, pars);
	}

	public static Condition condition(boolean condition) {
		return new Condition(condition);
	}

	public static OptExertion opt(String name, Exertion target) {
		return new OptExertion(name, target);
	}

	public static OptExertion opt(Condition condition,
								  Exertion target) {
		return new OptExertion(condition, target);
	}


	public static OptExertion opt(String name, Condition condition,
								  Exertion target) {
		return new OptExertion(name, condition, target);
	}

	public static AltExertion alt(OptExertion... exertions) {
		return new AltExertion(exertions);
	}

	public static AltExertion alt(String name, OptExertion... exertions) {
		return new AltExertion(name, exertions);
	}


	public static LoopExertion loop(Condition condition,
									Exertion target) {
		return new LoopExertion(null, condition, target);
	}

	public static LoopExertion loop(String name, Condition condition,
									Exertion target) {
		return new LoopExertion(name, condition, target);
	}

	public static Exertion exertion(Mappable mappable, String path)
			throws ContextException {
		Object obj = ((ServiceContext) mappable).asis(path);
		while (obj instanceof Mappable || obj instanceof Par) {
			try {
				obj = ((Evaluation) obj).asis();
			} catch (RemoteException e) {
				throw new ContextException(e);
			}
		}
		if (obj instanceof Exertion)
			return (Exertion) obj;
		else
			throw new NoneException("No such exertion at: " + path + " in: "
					+ mappable.getName());
	}

	public static Signature dispatcher(Signature signature) {
		((ServiceSignature)signature).addRank(Kind.DISPATCHER);
		return signature;
	}

	public static Signature model(Signature signature) {
		((ServiceSignature)signature).addRank(Kind.MODEL, Kind.TASKER);
		return signature;
	}

	public static Signature modelManager(Signature signature) {
		((ServiceSignature)signature).addRank(Kind.MODEL, Kind.MODEL_MANAGER);
		return signature;
	}

	public static Signature optimizer(Signature signature) {
		((ServiceSignature)signature).addRank(Kind.OPTIMIZER, Kind.TASKER);
		return signature;
	}

	public static Signature explorer(Signature signature) {
		((ServiceSignature)signature).addRank(Kind.EXPLORER, Kind.TASKER);
		return signature;
	}

	public static Block block(Exertion... exertions) throws ExertionException {
		return block(null, null, null, exertions);
	}

	public static Block block(Signature signature,
							  Exertion... exertions) throws ExertionException {
		return block(signature,  null, exertions);
	}

	public static Block block(String name,
							  Exertion... exertions) throws ExertionException {
		return block(name, null,  null, exertions);
	}

	public static Block block(String name, Signature signature,
							  Exertion... exertions) throws ExertionException {
		return block(name, signature,  null, exertions);
	}

	public static Block block(String name, Context context,
							  Exertion... exertions) throws ExertionException {
		return block(name, null, context, exertions);
	}

	public static Block block(Context context,
							  Exertion... exertions) throws ExertionException {
		return block(null, null, context, exertions);
	}

	public static Block block(Signature signature, Context context,
							  Exertion... exertions) throws ExertionException {
		return block(null, signature, context, exertions);
	}

	public static Block block(String name, Signature signature, Context context,
							  Exertion... exertions) throws ExertionException {
		Block block;
		try {
			if (signature != null) {
				if (signature instanceof ObjectSignature)
					block = new ObjectBlock(name);
				else
					block = new NetBlock(name);
			} else {
				// default signature
				block = new NetBlock(name);
			}

			if (context != null)
				block.setContext(context);
			block.setExertions(exertions);
		} catch (Exception se) {
			throw new ExertionException(se);
		}
		//make sure it has ParModel as the data context
		ParModel pm = null;
		Context cxt;
		try {
			cxt = block.getDataContext();
			if (cxt == null) {
				cxt = new ParModel();
				block.setContext(cxt);
			}
			if (cxt instanceof ParModel) {
				pm = (ParModel)cxt;
			} else {
				pm = new ParModel("block context: " + cxt.getName());
				pm.append(cxt);
				block.setContext(pm);
			}
			for (Exertion e : exertions) {
				if (e instanceof AltExertion) {
					List<OptExertion> opts = ((AltExertion) e).getOptExertions();
					for (OptExertion oe : opts) {
						oe.getCondition().setConditionalContext(pm);
					}
				} else if (e instanceof OptExertion) {
					((OptExertion)e).getCondition().setConditionalContext(pm);
				} else if (e instanceof LoopExertion) {
					((LoopExertion)e).getCondition().setConditionalContext(pm);
					Exertion target = ((LoopExertion)e).getTarget();
					if (target instanceof EvaluationTask && ((EvaluationTask)target).getEvaluation() instanceof Par) {
						Par p = (Par)((EvaluationTask)target).getEvaluation();
						p.setScope(pm);
						if (((ServiceContext)target.getContext()).getReturnPath() == null)
							((ServiceContext)target.getContext()).setReturnPath(p.getName());
					}
//				} else if (e instanceof VarTask) {
//					pm.append(((VarSignature)e.getProcessSignature()).getVariability());
				} else if (e instanceof EvaluationTask) {
					((EvaluationTask)e).setContext(pm);
					if (((EvaluationTask)e).getEvaluation() instanceof Par) {
						Par p = (Par)((EvaluationTask)e).getEvaluation();
						pm.addPar(p);
					}
				}
			}
		} catch (Exception ex) {
			throw new ExertionException(ex);
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
		public int multiplicity;
		public int maxPerCybernode;

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

	public static Multiplicity maintain(int multiplicity) {
		return new Multiplicity(multiplicity);
	}

	public static Multiplicity maintain(int multiplicity, int maxPerCybernode) {
		return new Multiplicity(multiplicity, maxPerCybernode);
	}

	public static Multiplicity maintain(int multiplicity, PerNode perNode) {
		return new Multiplicity(multiplicity, perNode);
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

	public static IP ips_exclude(String... ips) {
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
				deployment.setMultiplicity(((Multiplicity) o).multiplicity);
				deployment.setMaxPerCybernode(((Multiplicity) o).maxPerCybernode);
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
						if(inetAddress.isReachable(1000)) {
							logger.warning(getWarningBanner("The signature declares an ip address or hostname.\n" +
									ipAddress+" is not reachable on the current network"));
						}
					} catch (Exception e) {
						logger.warning(getWarningBanner(ipAddress+" is not found on the current network.\n"
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

	public static Exertion add(Exertion compound, Exertion component)
			throws ExertionException {
		compound.addExertion(component);
		return compound;
	}

	public static Block block(Loop loop, Exertion exertion)
			throws ExertionException, SignatureException {
		List<String> names = loop.getNames(exertion.getName());
		Block block;
		if (exertion instanceof NetTask || exertion instanceof NetJob
				|| exertion instanceof NetBlock) {
			block = new NetBlock(exertion.getName() + "-block");
		} else {
			block = new ObjectBlock(exertion.getName() + "-block");
		}
		Exertion xrt = null;
		for (String name : names) {
			xrt = (Exertion) ObjectCloner.cloneAnnotatedWithNewIDs(exertion);
			((ServiceExertion) xrt).setName(name);
			block.addExertion(xrt);
		}
		return block;
	}

	private static String getWarningBanner(String message) {
		StringBuilder builder = new StringBuilder();
		builder.append("\n****************************************************************\n");
		builder.append(message).append("\n");
		builder.append("****************************************************************\n");
		return builder.toString();
	}

}
