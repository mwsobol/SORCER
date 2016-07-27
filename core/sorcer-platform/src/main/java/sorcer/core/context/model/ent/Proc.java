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
package sorcer.core.context.model.ent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.context.ApplicationDescription;
import sorcer.core.context.ServiceContext;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.service.*;
import sorcer.service.modeling.Variability;
import sorcer.util.url.sos.SdbUtil;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.*;

/**
 * In service-based modeling, a parameter (for short a proc) is a special kind of
 * variable, used in a service context {@link ProcModel} to refer to one of the
 * pieces of data provided as input to the invokers (subroutines of the
 * context). These pieces of data are called arguments.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({"unchecked", "rawtypes" })
public class Proc<T> extends Entry<T> implements Variability<T>, Mappable<T>,
		Invocation<T>, Setter, Scopable, Comparable<T>, Reactive<T>, Serializable {

	private static final long serialVersionUID = 7495489980319169695L;
	 
	private static Logger logger = LoggerFactory.getLogger(Proc.class.getName());

	protected final String name;
	
	private Principal principal;

	protected T value;

	protected Context<T> scope;

	// data store URL for this proc
	private URL dbURL;

	// A context returning eval at the path
	// that is this proc name
	// Sorcer Mappable: Context, Exertion, or Var args
	protected Mappable mappable;

	protected String selectedFidelity;

	// proc fidelities for this proc
	protected Map<String, Object> fidelities;

	public Proc(String parname) {
		super(parname);
		name = parname;
		value = null;
	}
	
	public Proc(Identifiable identifiable) {
		super(identifiable.getName());
		name = identifiable.getName();
		value = (T)identifiable;
	}

	public Proc(String path, T argument) {
		super(path);
		name = path;

		if (argument instanceof EntryList) {
			if (fidelities == null)
				fidelities = new HashMap<String, Object>();
			for (Entry e : (EntryList) argument) {
				fidelities.put(e.getName(), e);
			}

			Entry first = ((EntryList) argument).get(0);
			selectedFidelity = first.getName();
			value = (T) first;
		} else if (argument instanceof Evaluation || argument instanceof Invocation) {
			if (argument instanceof ConditionalInvocation) {
				Context cxt = ((ServiceInvoker) argument).getScope();
				if (cxt != null) {
					scope = cxt;
					Condition condition = ((ConditionalInvocation) argument).getCondition();
					if (condition != null) {
						condition.setConditionalContext(cxt);
					}
				}
			}
			value = argument;
		} else {
			_2 = argument;
			value = argument;
		}
	}

	public Proc(String path, Object argument, Object scope)
			throws ContextException {
		this(path, (T) argument);
		if (argument instanceof String && scope instanceof Service) {
			mappable = (Mappable) scope;
			if (scope instanceof Context) {
				if (((ServiceContext) scope).containsPath(Condition._closure_))
					((Context) scope).remove(Condition._closure_);
				this.scope = (Context) scope;

			}
		}
		if (argument instanceof Scopable)
			((Scopable) argument).setScope(this.scope);
	}
	
	public Proc(Mappable map, String name, String path) {
		this(name);
		value =  (T)path;
		mappable = map;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	public void setValue(Object value) throws SetterException, RemoteException {
		if (isPersistent && mappable == null) {
			try {
				if (SdbUtil.isSosURL(this.value)) {
					SdbUtil.update((URL)this.value, value);
				} else  {
					this.value = (T)SdbUtil.store(value);
				}
			} catch (Exception e) {
				throw new SetterException(e);
			}
			return;
		}
		if (mappable != null && this._2 instanceof String ) {
			try {
				Object val = mappable.asis((String)_2);
				if (val instanceof Proc) {
					((Proc)val).setValue(value);
				} else if (isPersistent) {
					if (SdbUtil.isSosURL(val)) {
						SdbUtil.update((URL)val, value);
					} else {
						URL url = SdbUtil.store(value);
						Proc p = new Proc((String)this.value, url);
						p.setPersistent(true);
						if (mappable instanceof ServiceContext) {
							((ServiceContext) mappable).put((String) this.value, p);
						} else {
							mappable.putValue((String) this.value, p);
						}
					}
				} else {
					mappable.putValue((String)_2, value);
				}
			} catch (Exception e) {
				throw new SetterException(e);
			}
		} else if (value instanceof Evaluation) {
			this.value = (T) value;
		} else {
			this.value = (T)value;
			_2 = (T) value;
		}
	}

	@Override
	public T value() {
		return value;
	}

	/* (non-Javadoc)
         * @see sorcer.service.Evaluation#getAsis()
         */
	public T asis() throws EvaluationException, RemoteException {
		if (value != null) {
			return value;
		} else {
			return _2;
		}
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getValue(sorcer.co.tuple.Parameter[])
	 */
	@Override
	public T getValue(Arg... args) throws EvaluationException, RemoteException {
		// check for a constant or cached eval
		if (value instanceof Incrementor || ((value instanceof ServiceInvoker) &&
				scope != null && (scope instanceof ProcModel) && ((ProcModel)scope).isChanged()))
			isValid = false;
		if (_2 != null && isValid && args.length == 00 && !isPersistent) {
			try {
				if (_2 instanceof String
						&& mappable != null && mappable.getValue((String)_2) != null)
                    return (T) mappable.getValue((String)_2);
                else
                    return _2;
			} catch (ContextException e) {
				throw new EvaluationException(e);
			}
		}
		T val = null;
		try {
			substitute(args);
			if (selectedFidelity != null) {
				Object obj = fidelities.get(selectedFidelity);
				if (!isFidelityValid(obj)) {
					obj = scope.asis(selectedFidelity);
				}
				value = (T)obj;
			}
			if (mappable != null && value instanceof String) {
				Object obj = mappable.asis((String) value);
				if (obj instanceof Proc && ((Proc)obj).isPersistent())
					return (T)((Proc)obj).getValue();
				else
					val = (T) mappable.getValue((String) value);
			} else if (value == null && scope != null) {
				val = (T) ((ServiceContext<T>) scope).get(name);
			} else {
				val = value;
			}
			if (val instanceof Evaluation) {
				if (val instanceof Proc && ((Proc)val).asis() == null && value == null) {
					logger.warn("undefined proc: " + val);
					return null;
				}
				// direct scope
				if (val instanceof Scopable && ((Scopable)val).getScope() != null) {
					((Scopable)val).getScope().append(scope);
				}

				// indirect scope for entry values
				if (val instanceof Entry) {
					Object ev = ((Entry)val).asis();
					if (ev instanceof Scopable && ((Scopable)ev).getScope() != null) {
						((Scopable)ev).getScope().append(scope);
					}
				}

				if (val instanceof Exertion) {
					// TODO context binding for all mograms, works for tasks only
					Context cxt = ((Exertion)val).getDataContext();
					List<String> paths = ((ServiceContext)cxt).getPaths();
					for (String an : (Set<String>)((ServiceContext)scope).keySet()) {
						for (String p : paths) {
							if (p.endsWith(an)) {
								cxt.putValue(p, scope.getValue(an));
								break;
							}
						}
					}
				}
				val = ((Evaluation<T>) val).getValue(args);
				_2 = val;
				isValid = true;
			}

			if (isPersistent) {
				if (val == null && _2!= null) {
					val = _2;
				}
				if (SdbUtil.isSosURL(val)) {
					val = (T) ((URL) val).getContent();
				} else {
					URL url = SdbUtil.store(val);
					Proc p = null;
					if (mappable != null && this.value instanceof String
							&& mappable.asis((String) this.value) != null) {
						p = new Proc((String) this.value, url);
						p.setPersistent(true);
						mappable.putValue((String) this.value, p);
					} else if (this.value instanceof Identifiable) {
						p = new Proc(((Identifiable) this.value).getName(), url);
						p.setPersistent(true);
					} else {
						this.value = (T)url;
						_2 = null;
					}
				}
			}
		} catch (Exception e) {
			// make the cache invalid
			_2 = null;
			isValid = false;
			e.printStackTrace();
			throw new EvaluationException(e);
		}
		return val;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#substitute(sorcer.co.tuple.Parameter[])
	 */
	@Override
	public void substitute(Arg... parameters) throws SetterException {
		if (parameters == null)
			return;
		for (Arg p : parameters) {
			try {
				if (p instanceof Proc) {
					if (name.equals(((Proc<T>) p).name)) {
						value = ((Proc<T>) p).value;
						if (((Proc<T>) p).getScope() != null)
							scope.append(((Proc<T>) p).getScope());

					}
				} else if (p instanceof ServiceFidelity && fidelities != null) {
					selectedFidelity = p.getName();
				} else if (p instanceof Context) {
					if (scope == null)
						scope = (Context) p;
					else
						scope.append((Context) p);
				}
			} catch (ContextException e) {
				e.printStackTrace();
				throw new SetterException(e);
			}
		}
	}

	private boolean isFidelityValid(Object fidelity) throws EvaluationException {
		if (fidelity == null || fidelity == Context.none)
			return false;
		if (fidelity instanceof Entry) {
			Object obj = null;
			try {
				obj = ((Entry)fidelity).asis();
			} catch (RemoteException e) {
				throw new EvaluationException(e);
			}
			if (obj == null || obj == Context.none) return false;
		}
		 return true;
	}

	public Context getScope() {
		return scope;
	}

	public void setScope(Context scope) {
		if (scope != null && ((ServiceContext)scope).containsPath(Condition._closure_))
			scope.remove(Condition._closure_);
		this.scope = scope;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(T o) {
		if (o == null)
			throw new NullPointerException();
		if (o instanceof Proc<?>)
			return name.compareTo(((Proc<?>) o).getName());
		else
			return -1;
	}

	@Override
	public String toString() {
        String ps = "";
        if (value instanceof Evaluation) {
            try {
                ps = "" + ((Evaluation) value).asis();
            } catch (EvaluationException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            ps = "" + value;
        }

        return "proc [name: " + name + ", eval: " + ps + ", path: " + _1 + "]";
    }

	/* (non-Javadoc)
	 * @see sorcer.service.Perturbation#getPerturbedValue(java.lang.String)
	 */
	@Override
	public T getPerturbedValue(String varName) throws EvaluationException,
			RemoteException {
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Perturbation#getPerturbation()
	 */
	@Override
	public double getPerturbation() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getType()
	 */
	@Override
	public Type getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getDescription()
	 */
	@Override
	public ApplicationDescription getDescription() {
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getValueType()
	 */
	@Override
	public Class getValueType() {
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getArgs()
	 */
	@Override
	public ArgSet getArgs() {
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getArg(java.lang.String)
	 */
	@Override
	public T getArg(String varName) throws ArgException {
		try {
			return (T) scope.getValue(varName);
		} catch (ContextException e) {
			throw new ArgException(e);
		}
	}

	/**
	 * <p>
	 * Returns a Contextable (Context or Exertion) of this Proc that by a its
	 * name provides values of this Proc.
	 * </p>
	 * 
	 * @return the contextable
	 */
	public Mappable getContextable() {
		return mappable;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#isValueCurrent()
	 */
	@Override
	public boolean isValueCurrent() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#valueChanged(java.lang.Object)
	 */
	@Override
	public void valueChanged(Object obj) throws EvaluationException,
			RemoteException {		
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#valueChanged()
	 */
	@Override
	public void valueChanged() throws EvaluationException {		
	}


	public Principal getPrincipal() {
		return principal;
	}

	public URL getDbURL() throws MalformedURLException {
		URL url = null;
		if (dbURL != null)
			url = dbURL;
		else if (((ServiceContext)scope).getDbUrl() != null)
			url = new URL(((ServiceContext)scope).getDbUrl());
		
		return url;
	}

	public URL getURL() throws ContextException {
		if (isPersistent) {
			if (mappable != null)
				return (URL)mappable.asis((String)value);
			else
				return (URL)value;
		}
		return null;
	}
	
	public void setDbURL(URL dbURL) {
		this.dbURL = dbURL;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.vfe.Persister#isPersistable()
	 */
	@Override
	public boolean isPersistent() {
		return isPersistent;
	}

	public void setPersistent(boolean state) {
		isPersistent = state;
	}
	
	public Mappable getMappable() {
		return mappable;
	}

	public void setMappable(Mappable mappable) {
		this.mappable = mappable;
	}
	
	public boolean isMappable() {
		return (mappable != null);
	}

	public Map<String, Object> getFidelities() {
		return fidelities;
	}

	public String getSelectedFidelity() {
		return selectedFidelity;
	}

	public void setSelectedFidelity(String selectedFidelity) {
		this.selectedFidelity = selectedFidelity;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Invocation#invoke(sorcer.service.Context, sorcer.service.Arg[])
	 */
	@Override
	public T invoke(Context context, Arg... args) throws RemoteException,
			InvocationException {
		try {
			if (context != null)
				scope.append(context);
			return getValue(args);
		} catch (Exception e) {
			throw new InvocationException(e);
		}
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Mappable#getValue(java.lang.String, sorcer.service.Arg[])
	 */
	@Override
	public T getValue(String path, Arg... args) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				try {
					return (T)getValue(args);
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
			else if (mappable != null)
				return (T)mappable.getValue(path.substring(name.length()), args);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Mappable#asis(java.lang.String)
	 */
	@Override
	public T asis(String path) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				return value;
			else if (mappable != null)
				return (T)mappable.asis(path.substring(name.length()));
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Mappable#putValue(java.lang.String, java.lang.Object)
	 */
	@Override
	public T putValue(String path, Object value) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				this.value = (T)value;
			else if (mappable != null)
				mappable.putValue(path.substring(name.length()), value);
		}
		return (T)value;	
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.model.Variability#addArgs(ArgSet set)
	 */
	@Override
	public void addArgs(ArgSet set) throws EvaluationException {
		Iterator<Arg> i = set.iterator();
		while (i.hasNext()) {
			Proc procEntry = (Proc)i.next();
			try {
				putValue(procEntry.getName(), procEntry.asis());
			} catch (Exception e) {
				throw new EvaluationException(e);
			} 
		}
		
	}
	
	@Override
	public int hashCode() {
		int hash = name.length() + 1;
		return hash = hash * 31 + name.hashCode();
	}
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof Proc
				&& ((Proc) object).name.equals(name))
			return true;
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Scopable#setScope(java.lang.Object)
	 */
	public void setScope(Object scope) throws RemoteException {
		this.scope = (Context)scope;
		
	}

	public void putFidelity(EntryList fidelity) throws EvaluationException,
			RemoteException {
		if (fidelities == null)
			fidelities = new HashMap<String, Object>();
		for (Entry e : fidelity)
			fidelities.put(e.getName(), e.asis());
	}

	public void addFidelity(EntryList fidelity) throws EvaluationException,
			RemoteException {
		putFidelity(fidelity);
	}

	public void selectFidelity(String name) throws EntException {
		if (fidelities.containsKey(name))
			value = (T) fidelities.get(name);
		else
			throw new EntException("no such service fidelity: " + name + " at: " + this);
	}

	public void setFidelities(Map<String, Object> fidelities) {
		this.fidelities = fidelities;
	}
	

	@Override
	public boolean isReactive() {
		return true;
	}

	@Override
	public Object exec(Arg... args) throws MogramException, RemoteException {
		Context cxt = Arg.getContext(args);
		if (cxt != null) {
			scope = cxt;
			return getValue(args);
		} else {
			return getValue(args);
		}
	}
}
