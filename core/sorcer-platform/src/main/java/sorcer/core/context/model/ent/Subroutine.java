/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import sorcer.core.context.ContextSelection;
import sorcer.core.context.ServiceContext;
import sorcer.service.*;
import sorcer.service.modeling.*;
import sorcer.util.bdb.objects.UuidObject;
import sorcer.util.url.sos.SdbUtil;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static sorcer.mo.operator.add;

/**
 * @author Mike Sobolewski
 */
public class Subroutine<T> extends Entry<T> implements Functionality<T>, Evaluation<T>, Perturbation<T>, Dependency, Comparable<T>,
		EvaluationComponent, SupportComponent, Scopable {

	private static final long serialVersionUID = 1L;

	public int index;

	protected boolean negative;

	protected String name;

	protected boolean isValueCurrent;

	protected ArgSet args;

	// dependency management for this Entry
	protected List<Evaluation> dependers = new ArrayList<Evaluation>();

	public Subroutine() {
	}

	public Subroutine(final String path) {
		if(path==null)
			throw new IllegalArgumentException("path must not be null");
		this.key = path;
        this.name = path;
	}

	public Subroutine(final String path, final T value) {
        if(path==null)
            throw new IllegalArgumentException("path must not be null");
        this.key = path;
        this.name = key;
        if (value instanceof Fi) {
            multiFi = (Fi) value;
            this.impl = (T)  multiFi.get(0);
        } else {
            this.key = path;
            this.impl = value;
        }
	}

	public Subroutine(final String path, final T value, final int index) {
		this(path, value);
		this.index = index;
	}

	public Subroutine(final String path, final T value, final String annotation) {
		this(path, value);
		this.annotation = annotation;
	}

	@Override
	public String getName() {
		if (type.equals(Functionality.Type.PROC) && domain != null) {
			//used for procedural attchemnt with entry names name$domain
			return name + "$" + domain;
		} else {
			return name;
		}
	}

	@Override
	public T evaluate(Arg... args) throws EvaluationException, RemoteException {
		Object val = this.impl;
		URL url = null;
		try {
			substitute(args);
			if (isPersistent) {
				if (SdbUtil.isSosURL(val)) {
					Object out = ((URL)val).getContent();
					if (out instanceof UuidObject)
						out = (T) ((UuidObject) val).getObject();
				} else {
					if (val instanceof UuidObject) {
						url = SdbUtil.store(val);
					} else {
						UuidObject uo = new UuidObject(val);
						uo.setName(key);
						url = SdbUtil.store(uo);
					}
					out = (T)url;
				}
			} else if (val instanceof Invocation) {
				Context cxt = (Context) Arg.selectDomain(args);
				if (val instanceof Scopable) {
                    ((Scopable)val).setScope(scope);
                }
				out = (T) ((Invocation) val).invoke(cxt, args);
			} else if (val instanceof Evaluation) {
				out = ((Evaluation<T>) val).evaluate(args);
			} else if (val instanceof ServiceFidelity) {
				// return the selected fidelity of this entry
				for (Arg arg : args) {
					if (arg instanceof Fidelity) {
						if (((Fidelity)arg).getPath().equals(key)) {
							((ServiceFidelity)val).selectSelect(arg.getName());
							break;
						}
					}
				}
				out = (T) ((Entry)((ServiceFidelity) val).getSelect()).getValue(args);
			} else if (val instanceof Callable) {
				out = (T) ((Callable)val).call(args);
			} else if (val instanceof Service) {
				out = (T) ((Service)val).execute(args);
			}
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
		if (contextSelector != null) {
			try {
				out = (T) contextSelector.doSelect(val);
			} catch (ContextException e) {
				throw new EvaluationException(e);
			}
		}
		if (val instanceof Number && negative) {
			Number result = (Number) val;
			Double rd = result.doubleValue() * -1;
			out = (T) rd;
		}
		return out;
	}

	@Override
	public void substitute(Arg... entries) throws SetterException {
		if (entries != null) {
			for (Arg a : entries) {
				if (a instanceof ContextSelection) {
					setContextSelector((ContextSelection) a);
				}
			}
		}
	}

	@Override
	public void setValue(Object value) throws SetterException {
		if (isPersistent) {
			try {
				if (SdbUtil.isSosURL(value)) {
					this.impl = (T) value;
				} else if (SdbUtil.isSosURL(this.impl)) {
					if (((URL) this.impl).getRef() == null) {
						this.impl = (T) SdbUtil.store(value);
					} else {
						SdbUtil.update((URL) this.impl, value);
					}
				}
			} catch (Exception e) {
				throw new SetterException(e);
			}
		} else {
			this.out = (T) value;
		}
	}

	public int index() {
		return index;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(T o) {
		if (o == null)
			throw new NullPointerException();
		if (o instanceof Subroutine<?>)
			return key.compareTo(((Subroutine<?>) o).getName());
		else
			return -1;
	}

	@Override
	public int hashCode() {
		int hash = key.length() + 1;
		return hash * 31 + key.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Subroutine) {
			if (impl != null && ((Subroutine) object).impl == null) {
				return false;
			} else if (impl == null && ((Subroutine) object).impl != null) {
				return false;
			} else if (((Subroutine) object).key.equals(key)
					&& ((Subroutine) object).impl == impl) {
				return true;
			} else if (((Subroutine) object).key.equals(key)
					&& ((Subroutine) object).impl.equals(impl)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void addDependers(Evaluation... dependers) {
		if (this.dependers == null)
			this.dependers = new ArrayList<Evaluation>();
		for (Evaluation depender : dependers)
			this.dependers.add(depender);
	}

	@Override
	public List<Evaluation> getDependers() {
		return dependers;
	}

	public ContextSelection getContextSelector() {
		return contextSelector;
	}

	public void setContextSelector(ContextSelection contextSelector) {
		this.contextSelector = contextSelector;
	}

	@Override
	public String toString() {
		String en = "";
		try {
			if (impl instanceof Evaluation && ((Evaluation) impl).asis() != null) {
				if (this == impl) {
					return "[" + key + "=" + ((Identifiable) impl).getName() + "x]";  // loop
				}
				en = ((Evaluation) impl).asis().toString();
			} else {
				en = "" + impl;
			}
		}catch (EvaluationException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return "[" + key + "=" + en + "]";
	}

	public Subroutine(String path, T value, boolean isPersistant, int index) {
		this(path, value, index);
		this.isPersistent = isPersistant;
	}

	public Mogram exert(Mogram mogram, Transaction txn, Arg... args) throws TransactionException,
			MogramException, RemoteException {
		Context cxt = null;
		Context out = new ServiceContext();
		if (mogram instanceof Model) {
			if (impl != null && impl != Context.none)
				add((Domain) mogram, this);
			((ServiceContext)mogram).getMogramStrategy().getResponsePaths().add(new Path(key));
			out = (Context) ((Model)mogram).getResponse();
		} else if (mogram instanceof ServiceContext) {
			if (impl == null || impl == Context.none) {
				out.putValue(key, ((Context)mogram).getValue(key));
			} else {
				if (impl instanceof Evaluation) {
					this.setReactive(true);
					((ServiceContext)mogram).putValue(key, this);
				} else {
					((ServiceContext)mogram).putValue(key, impl);
				}
				out.putValue(key, ((ServiceContext) mogram).getValue(key));
			}
		} else if (mogram instanceof Routine) {
			if (impl != null && impl != Context.none)
				mogram.getContext().putValue(key, impl);
			cxt =  mogram.exert(txn).getContext();
			out.putValue(key, cxt.getValue(key));
		}
		return out;
	}

    @Override
    public boolean isReactive() {
        return true;
    }

    public boolean isNegative() {
		return negative;
	}

	public void setNegative(boolean negative) {
		this.negative = negative;
	}

	public Object getSelectedFidelity() {
		return multiFi.getSelect();
	}

	@Override
	public Class<?> getValueType() {
		return Object.class;
	}

	@Override
	public T getValue(Arg... args) throws EvaluationException {
		try {
			return evaluate(args);
		} catch (RemoteException e) {
			throw new EvaluationException(e);
		}
	}

	public ArgSet getArgs() {
		return args;
	}

	@Override
	public void addArgs(ArgSet set) throws EvaluationException {
		args.addAll(set);
	}

	@Override
	public Object getArg(String varName) throws ContextException {
		return args.getArg(varName);
	}

	@Override
	public boolean isValueCurrent() {
		return isValueCurrent;
	}

	public boolean setValueCurrent(boolean state) {
		return isValueCurrent = state;
	}

	@Override
	public void valueChanged(Object obj) throws EvaluationException {

	}

	@Override
	public void valueChanged() throws EvaluationException {

	}

	@Override
	public T call(Arg... args) throws EvaluationException, RemoteException {
		return evaluate(args);
	}

	@Override
	public T getPerturbedValue(String varName) throws EvaluationException, RemoteException, ConfigurationException {
		return null;
	}

	@Override
	public double getPerturbation() {
		return 0;
	}
}
