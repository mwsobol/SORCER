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
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.Proc;
import sorcer.core.context.model.ent.ProcModel;
import sorcer.eo.operator;
import sorcer.service.*;

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
 * The semantics for how parameters can be declared and how the arguments get
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
 * expressed by a path of attributes like directories in paths of a file system.
 * Paths define a namespace of the context parameters. A context argument is any
 * object referenced by its path or returned by a context invoker referenced by
 * its path inside the context. An ordered list of parameters is usually
 * included in the definition of an invoker, so that, each time the invoker is
 * called, the context arguments for that call can be assigned to the
 * corresponding parameters of the invoker. The context values for all paths
 * inside the context are defined explicitly by corresponding objects or
 * calculated by corresponding invokers. Thus, requesting a eval for a path in
 * a context is a computation defined by a invoker composition within the scope
 * of the context.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ServiceInvoker<T> extends Observable implements Invocation<T>, Identifiable, Scopable, Evaluator<T>, Reactive<T>, Observer, Serializable {

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

	private boolean valueIsCurrent = false;

	private boolean isReactive = false;

	// indication that eval has been calculated with recent arguments
	private boolean valueIsValid = false;

	protected Context invokeContext;

	protected ValueCallable lambda;

	// set of dependent variables for this evaluator
	protected ArgSet args = new ArgSet();

	/** Logger for logging information about instances of this type */
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
		invokeContext = new ProcModel("model/proc");
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
		if (context == null)
			invokeContext = new ProcModel("model/proc");
		else {
			if (context instanceof ServiceContext) {
				invokeContext = context;
			} else {
				try {
					invokeContext = new ProcModel(context);
				} catch (Exception e) {
					throw new InvocationException("Failed to create invoker!", e);
				}
			}
		}
		this.args = args;
		this.lambda = lambda;
	}

	public ServiceInvoker(ProcModel context) {
		this(context.getName());
		invokeContext = context;
	}
	
	public ServiceInvoker(ProcModel context, Evaluator evaluator, Proc... procEntries) {
		this(context);
		this.evaluator = evaluator;
		this.args = new ArgSet(procEntries);
	}
	
	public ServiceInvoker(ProcModel context, Evaluator evaluator, ArgSet args) {
		this(context);
		this.evaluator = evaluator;
		this.args = args;
	}
	
	public ServiceInvoker(Evaluator evaluator, ArgSet args) {
		this(((Identifiable)evaluator).getName());
		this.evaluator = evaluator;
		this.args = args;
	}
	
	public ServiceInvoker(Evaluator evaluator, Proc... procEntries) {
		this(((Identifiable)evaluator).getName());
		this.evaluator = evaluator;
		this.args = new ArgSet(procEntries);
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
	
	/**
	 * <p>
	 * Return the valid eval
	 * </p>
	 * 
	 * @return the valid eval
	 * @throws EvaluationException 
	 * @throws RemoteException 
	 */
	@Override
	public T getValue(Arg... entries) throws EvaluationException, RemoteException {
			if (lambda != null || evaluator != null)
				return (T) invokeEvaluator(entries);
			else
				throw new EvaluationException("Evaluation#execute() not implemented by: " + this.getClass().getName());
			
	}

	public void valueValid(boolean state) {
		valueIsValid = state;
	}
	
	public boolean valueValid() {
		return valueIsValid;
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
		valueValid(false);
		
		// set eval to null so getValueAsIs returns null
		value = null;
		setChanged();
		notifyObservers(this);
	}
	
	/**
	 * Adds a new proc to the invoker. This must be done before calling
	 * {@link #invoke} so the invoker is aware that the new proc may be added to
	 * the model.
	 * 
	 * @param par
	 *            the variable to be added
	 *
	 * @throws RemoteException
	 * @throws EvaluationException
	 * @throws RemoteException
	 */
	public ServiceInvoker addPar(Object par) throws EvaluationException {
		if (par instanceof Proc) {
			((ServiceContext)invokeContext).put(((Proc) par).getName(), par);
			if (((Proc) par).asis() instanceof ServiceInvoker) {
				try {
					((ServiceInvoker) ((Proc) par).getValue()).addObserver(this);
					args.add((Proc) par);
					value = null;
					setChanged();
					notifyObservers(this);
					valueValid(false);
				} catch (RemoteException e) {
					throw new EvaluationException(e);
				}
			}
		} else if (par instanceof Identifiable) {
			try {
				Proc p = new Proc(((Identifiable) par).getName(), par, invokeContext);
				invokeContext.putValue(p.getName(), p);
			} catch (ContextException e) {
				throw new EvaluationException(e);
			}
		}
		return this;
	}

	synchronized public void addPars(ArgSet parSet) throws EvaluationException {
		for (Arg p : parSet) {
			addPar(p);
		}
	}
	
	synchronized public void addPars(List<Proc> procEntryList)
			throws EvaluationException, RemoteException {
		for (Proc p : procEntryList) {
			addPar(p);
		}
	}
	
	synchronized public void addPars(Proc... procEntries) throws EvaluationException,
			RemoteException {
		for (Proc p : procEntries) {
			addPar(p);
		}
	}

	synchronized public void addPars(ArgList args) throws EvaluationException,
			RemoteException {
		if (args != null)
			for (Arg p : args) {
				addPar(p);
			}
	}

	synchronized public void addPars(ArgList... parLists)
			throws EvaluationException, RemoteException {
		for (ArgList pl : parLists) {
			addPars(pl);
		}
	}
	
	public T invoke(Context context, Arg... args)
			throws RemoteException, InvocationException {
		try {
			if (invokeContext == null)
				invokeContext = context;
			else {
				invokeContext.append(context);
			}
		} catch (ContextException e) {
			throw new InvocationException(e);
		}
		if (evaluator != null)
			return (T) invokeEvaluator();
		else
			return invoke(args);
	}
	
	public T invoke(Arg... entries) throws RemoteException, InvocationException {
		try {
			if (entries != null && entries.length > 0) {
				valueIsValid = false;
				if (invokeContext == null)
					invokeContext = new ProcModel("model/proc");
					
				((ServiceContext)invokeContext).substitute(entries);
			}
			if (((ServiceContext)invokeContext).isChanged()) {
				valueIsValid = false;
				if (args != null)
					args.clearArgs();
			}
			if (valueIsValid)
				return value;
			else {
				if (lambda != null) {
					   value = (T) lambda.call(invokeContext);
				} else if (evaluator != null)
					value = (T) invokeEvaluator(entries);
				else
					value = getValue(entries);

				valueIsValid = true;
			}
		} catch (Exception e) {
			throw new InvocationException(e);
		}
		return value;
	}
	
	private Object invokeEvaluator(Arg... entries)
			throws InvocationException {
		try {
			init(this.args);
			if (lambda != null) {
				return lambda.call(invokeContext);
			} else if (evaluator != null) {
				evaluator.addArgs(this.args);
				return evaluator.getValue(entries);
			}
		} catch (Exception e) {
			throw new InvocationException(e);
		}
		throw new InvocationException("No evaluator available!");
	}
	
	private void init(ArgSet set){
		if (set != null) {
			for (Arg p : set) {
				if (p instanceof Proc && ((Proc) p).getScope() == null)
					((Proc) p).setScope(invokeContext);
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
	public void substitute(Arg... entries)
			throws SetterException {
		for (Arg e : entries) {
			if (e instanceof Entry) {
				try {
					invokeContext.putValue(e.getName(), ((Entry) e).get());
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
				((Proc) p).setValue(null);
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

	public void setName(String name) {
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Evaluator#evaluate(sorcer.service.Arg[])
	 */
	@Override
	public T evaluate(Arg... entries) throws EvaluationException, RemoteException {
		return invoke(entries);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluator#addArgs(sorcer.core.context.model.proc.EntrySet)
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

	@Override
	public void setValueIsCurrent(boolean state) {
		valueIsCurrent = state;
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
		return valueIsCurrent;
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
	public Object execute(Arg... args) throws MogramException, RemoteException {
		Context cxt = (Context)Arg.selectDomain(args);
		if (cxt !=null) {
			invokeContext = cxt;
			return getValue(args);
		}
		return null;
	}

}
