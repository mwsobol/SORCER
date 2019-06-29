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

package sorcer.core.invoker;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.EntModel;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.Prc;
import sorcer.eo.operator;
import sorcer.service.*;
import sorcer.service.modeling.Data;
import sorcer.service.modeling.evr;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;

/**
 * @author Mike Sobolewski
 */

/**
 * The Invoker defines context driven invocations on a parameter context
 * (invoke context) containing its parameter names (paths) and arguments
 * (values). The requested invocation is specified by the own invoke context and
 * eventual context of parameters (Proc).
 * 
 * The semantics for how parameters can be declared and how the arguments getValue
 * passed to the parameters of callable unit are defined by the language, but
 * the details of how this is represented in any particular computing system
 * depend on the calling conventions of that system. A context-driven computing
 * system defines callable unit called invokers used within a scope of service
 * contexts, data structures defined in SORCER.
 * 
 * An invoke context is dictionary (associative array) composed of a collection
 * of (key, eval) pairs, such that each possible key appears at most once in
 * the collection. Keys are considered as parameters and values as arguments of
 * the service invokers accepting service contexts as their input data. A key is
 * expressed by a contextReturn of attributes like directories in paths of a file system.
 * Paths define a namespace of the context parameters. A context argument is any
 * object referenced by its contextReturn or returned by a context invoker referenced by
 * its contextReturn inside the context. An ordered list of parameters is usually
 * included in the definition of an invoker, so that, each time the invoker is
 * called, the context arguments for that prc can be assigned to the
 * corresponding parameters of the invoker. The context values for all paths
 * inside the context are defined explicitly by corresponding objects or
 * calculated by corresponding invokers. Thus, requesting a eval for a contextReturn in
 * a context is a computation defined by a invoker composition within the scope
 * of the context.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ServiceInvoker<T> extends Observable implements Evaluator<T>, Invocation<T>, Identifiable, Scopable, Reactive<T>, Observer, evr<T>, Serializable {

	private static final long serialVersionUID = -2007501128660915681L;
	
	protected String name;

	protected String defaultName = "invoker-";
	
	// counter for unnamed instances
	protected static int count;
	
	protected Uuid id = UuidFactory.generate();

	//the cached eval
	protected T value;

	protected boolean negative;

	// invocation delegate to
	Evaluator evaluator;

	protected boolean isFunctional = false;

	private boolean isCurrent = false;

	private boolean isReactive = false;

	// indication that eval has been calculated with recent arguments
	protected boolean isValid = false;

	protected Context invokeContext;

	protected ValueCallable lambda;

	// set of dependent variables for this evaluator
	protected ArgSet args = new ArgSet();

	protected Fi multiFi;

	protected Morpher morpher;

	// default instance new Return(Context.RETURN);
	protected Context.Return contextReturn;

	/** Logger for logging information about instances of this multitype */
	static final Logger logger = LoggerFactory.getLogger(ServiceInvoker.class
			.getName());

	public ServiceInvoker() {
		this((String) null);
	}
	
	public ServiceInvoker(String name) {
		if (name == null)
			this.name = defaultName + count++;
		else
			this.name = name;
		invokeContext = new EntModel("model/prc");
	}

	public ServiceInvoker(ValueCallable lambda) throws InvocationException {
		this(null, lambda, null, null);
	}

	public ServiceInvoker(ValueCallable lambda, ArgSet args) throws InvocationException {
		this(null, lambda, null, args);
	}

	public ServiceInvoker(String name, ValueCallable lambda) throws InvocationException {
		this(name, lambda, null, null);
	}

	public ServiceInvoker(String name, ValueCallable lambda, ArgSet args) throws InvocationException {
		this(name, lambda, null, args);
	}

	public ServiceInvoker(String name, ValueCallable lambda, Context context) throws InvocationException {
		this(name, lambda, context, null);
	}

	public ServiceInvoker(String name, ValueCallable lambda, Context context, ArgSet args) throws InvocationException {
		this.name = name;
		invokeContext = context;
//		if (context == null)
//			invokeContext = new EntModel("model/prc");
//		else {
//			if (context instanceof ServiceContext) {
//				invokeContext = context;
//			} else {
//				try {
//					invokeContext = new EntModel(context);
//				} catch (Exception e) {
//					throw new InvocationException("Failed to create invoker!", e);
//				}
//			}
//		}
		this.args = args;
		this.lambda = lambda;
	}

	public ServiceInvoker(EntModel context) {
		this(context.getName());
		invokeContext = context;
	}
	
	public ServiceInvoker(EntModel context, Evaluator evaluator, Prc... callEntries) {
		this(context);
		this.evaluator = evaluator;
		this.args = new ArgSet(callEntries);
	}
	
	public ServiceInvoker(EntModel context, Evaluator evaluator, ArgSet args) {
		this(context);
		this.evaluator = evaluator;
		this.args = args;
	}
	
	public ServiceInvoker(Evaluator evaluator, ArgSet args) {
		this(((Identifiable)evaluator).getName());
		this.evaluator = evaluator;
		this.args = args;
	}
	
	public ServiceInvoker(Evaluator evaluator, Prc... callEntries) {
		this(((Identifiable)evaluator).getName());
		this.evaluator = evaluator;
		this.args = new ArgSet(callEntries);
	}

	/**
	 * <p>
	 * Returns a set of parameters (args) of this invoker that are a a subset of
	 * parameters of its invokeContext.
	 * </p>
	 * 
	 * @return the args of this invoker
	 */
	@Override
	public ArgSet getArgs() {
		return args;
	}

	/**
	 * <p>
	 * Assigns a set of parameters (args) for this invoker.
	 * </p>
	 * 
	 * @param args
	 *            the args to set
	 */
	public ServiceInvoker setArgs(ArgSet args) {
		this.args = args;
		return this;
	}

	public ServiceInvoker setArgs(operator.Args args) {
		this.args = new ArgSet(args.args());
		return this;
	}

	public ServiceInvoker setArgs(Arg[] args) {
		this.args = new ArgSet(args);
		return this;
	}

	public void setValid(boolean state) {
		isValid = state;
	}
	
	public boolean isValid() {
		return isValid;
	}
	
	public void valueChanged() throws EvaluationException {
		setChanged();
		try {
			notifyObservers(this);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new EvaluationException(e.toString());
		}
	}
	
	@Override 
	public void update(Observable observable, Object obj) throws EvaluationException, RemoteException {
		// one of my dependent args changed
		// the 'observable' is the dependent invoker that has changed as indicated by 'obj'
		// ignore updates from itself
		setValid(false);
		
		// set eval to null so getValueAsIs returns null
		value = null;
		setChanged();
		notifyObservers(this);
	}
	
	/**
	 * Adds a new prc to the invoker. This must be done before calling
	 * {@link #invoke} so the invoker is aware that the new prc may be added to
	 * the model.
	 * 
	 * @param call
	 *            the variable to be added
	 *
	 * @throws RemoteException
	 * @throws EvaluationException
	 * @throws RemoteException
	 */
	public ServiceInvoker addCall(Object call) throws EvaluationException {
		if (call instanceof Prc) {
			((ServiceContext)invokeContext).put(((Prc) call).getName(), call);
			if (((Prc) call).asis() instanceof ServiceInvoker) {
				try {
					((ServiceInvoker) ((Prc) call).evaluate()).addObserver(this);
					args.add((Prc) call);
					value = null;
					setChanged();
					notifyObservers(this);
					setValid(false);
				} catch (RemoteException e) {
					throw new EvaluationException(e);
				}
			}
		} else if (call instanceof Identifiable) {
			try {
				Prc p = new Prc(((Identifiable) call).getName(), call, invokeContext);
				invokeContext.putValue(p.getName(), p);
			} catch (ContextException e) {
				throw new EvaluationException(e);
			}
		}
		return this;
	}

	synchronized public void addPars(ArgSet parSet) throws EvaluationException {
		for (Arg p : parSet) {
			addCall(p);
		}
	}
	
	synchronized public void addPars(List<Prc> callEntryList)
			throws EvaluationException, RemoteException {
		for (Prc p : callEntryList) {
			addCall(p);
		}
	}
	
	synchronized public void addPars(Prc... callEntries) throws EvaluationException,
			RemoteException {
		for (Prc p : callEntries) {
			addCall(p);
		}
	}

	synchronized public void addPars(ArgList argList) throws EvaluationException,
			RemoteException {
		if (args != null)
			for (Arg p : argList) {
				addCall(p);
			}
	}

	synchronized public void addPars(ArgList... argList)
			throws EvaluationException, RemoteException {
		for (ArgList pl : argList) {
			addPars(pl);
		}
	}

	@Override
	public T invoke(Context context, Arg... args)
		throws RemoteException, EvaluationException {
		try {
			if (context != null) {
				if (invokeContext == null || invokeContext.size() == 0) {
					invokeContext = context;
				} else {
					invokeContext.append(context);
				}
			}
		} catch (ContextException e) {
			throw new InvocationException(e);
		}
		return evaluate(args);
	}

	@Override
	public T evaluate(Arg... args) throws EvaluationException, RemoteException {
		try {
			if (args != null && args.length > 0) {
				isValid = false;
				Context cxt = (Context) Arg.selectDomain(args);
				if (invokeContext == null) {
					if (cxt != null) {
						invokeContext = cxt;
					} else {
						invokeContext = new EntModel("model/prc");
					}
				} else if (cxt != null) {
					invokeContext.append(cxt);
				}
				((ServiceContext)invokeContext).substitute(args);
			}
			if (invokeContext.isChanged()) {
				isValid = false;
				if (this.args != null)
					this.args.clearArgs();
			}
			if (isValid)
				return value;
			else {
				value = (T) invoke(args);
				isValid = true;
			}
		} catch (Exception e) {
			throw new InvocationException(e);
		}
		return value;
	}
	
	private Object invoke(Arg... args)
			throws InvocationException {
		try {
			init(this.args);
			if (lambda != null) {
				if (isFunctional) {
					return lambda;
				} else {
					return lambda.call(invokeContext);
				}
			} else if (evaluator != null) {
				evaluator.addArgs(this.args);
				if (isFunctional) {
					return evaluator;
				} else {
					return evaluator.evaluate(args);
				}
			}
		} catch (Exception e) {
			throw new InvocationException(e);
		}
		throw new InvocationException("No evaluator available!");
	}
	
	private void init(ArgSet set){
		if (set != null) {
			for (Arg p : set) {
				if (p instanceof Prc && ((Prc) p).getScope() == null)
					((Prc) p).setScope(invokeContext);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Evaluation#getAsis()
	 */
	@Override
	public T asis() throws EvaluationException, RemoteException {
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Evaluation#substitute(sorcer.co.tuple.Parameter[])
	 */
	@Override
	public void substitute(Arg... args)
			throws SetterException {
		for (Arg e : args) {
			if (e instanceof Entry) {
				try {
					invokeContext.putValue(e.getName(), ((Entry) e).getValue());
				} catch (ContextException ex) {
					throw new SetterException(ex);
				}
			}

		}
	}

	public Context getScope() {
		return invokeContext;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Scopable#setScope(java.lang.Object)
	 */
	@Override
	public void setScope(Context scope) {
		invokeContext = scope;
	}
	
	/**
	 * <p>
	 * Returns this invoker's evaluator.
	 * </p>
	 * 
	 * @return the evaluator
	 */
	public Evaluator getEvaluator() {
		return evaluator;
	}

	/**
	 * <p>
	 * Assigns an evaluator used by this invoker.
	 * </p>
	 * 
	 * @param evaluator
	 *            the evaluator to set
	 * @throws RemoteException 
	 * @throws ContextException 
	 */
	public ServiceInvoker setEvaluator(Evaluator evaluator) {
		this.evaluator = evaluator;
		return this;
	}

	public void clearPars() throws EvaluationException {
		for (Arg p : args) {
			try {
				((Prc) p).setValue(null);
			} catch (Exception e) {
				throw new EvaluationException(e);
			}
		}
	}
	
	@Override
	public String toString() {
		return getClass().getName() + ":" + name;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Identifiable#getId()
	 */
	@Override
	public Object getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Identifiable#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		if (name != null) {
			this.name = name;
		} else {
			this.name = defaultName + count++;
		}
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluator#addArgs(sorcer.core.context.model.prc.EntrySet)
	 */
	@Override
	public void addArgs(ArgSet set) throws EvaluationException {
		addPars(set);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluator#setParameterTypes(java.lang.Class[])
	 */
	@Override
	public void setParameterTypes(Class<?>[] types) {
		// implemented by subclasses
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluator#setParameters(java.lang.Object[])
	 */
	@Override
	public void setParameters(Object... args) {
		// implemented by subclasses
	}

	public void setValueCurrent(boolean state) {
		isCurrent = state;
	}

	@Override
	public void update(Setup... entries) throws ContextException {
		// implement in subclasses
	}

	public boolean isNegative() {
		return negative;
	}

	@Override
	public void setNegative(boolean negative) {
		this.negative = negative;
	}

	public boolean isValueCurrent() {
		return isCurrent;
	}

	@Override
	 public boolean isReactive() {
		return true;
	}

	@Override
	public Reactive<T> setReactive(boolean isReactive) {
		this.isReactive = isReactive;
		return this;
	}

	public ValueCallable getLambda() {
		return lambda;
	}

	public void setLambda(ValueCallable lambda) {
		this.lambda = lambda;
	}

	@Override
	public Object execute(Arg... args) throws EvaluationException {
		Context cxt = (Context)Arg.selectDomain(args);
		if (cxt !=null) {
			invokeContext = cxt;
			try {
				return evaluate(args);
			} catch (RemoteException e) {
				throw new EvaluationException(e);
			}
		}
		return null;
	}

	@Override
	public Context.Return getContextReturn() {
		return contextReturn;
	}

	@Override
	public void setContextReturn(Context.Return contextReturn) {
		this.contextReturn = contextReturn;
	}


	public boolean isFunctional() {
		return isFunctional;
	}

	public void setFunctional(boolean functional) {
		isFunctional = functional;
	}

	@Override
	public Data act(Arg... args) throws ServiceException, RemoteException {
		return null;
	}

	@Override
	public Data act(String entryName, Arg... args) throws ServiceException, RemoteException {
		return null;
	}

	@Override
	public T getValue(Arg... args) throws ContextException {
		Context cxt = (Context)Arg.selectDomain(args);
		if (cxt !=null) {
			invokeContext = cxt;
			try {
				return evaluate(args);
			} catch (RemoteException e) {
				throw new EvaluationException(e);
			}
		}
		return null;
	}

	@Override
	public Fi getMultiFi() {
		return null;
	}

	@Override
	public Morpher getMorpher() {
		return null;
	}
}
