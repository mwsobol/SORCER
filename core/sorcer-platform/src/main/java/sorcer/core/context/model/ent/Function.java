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

import static sorcer.eo.operator.add;

/**
 * @author Mike Sobolewski
 */
public class Function<T> extends Entry<T> implements Evaluation<T>, Dependency, Comparable<T>,
		EvaluationComponent, SupportComponent, Scopable, Setter {

	private static final long serialVersionUID = 5168783170981015779L;

	public int index;

	protected boolean negative;

	protected Object annotation;

	// dependency management for this Entry
	protected List<Evaluation> dependers = new ArrayList<Evaluation>();

	public Function() {
	}

	public Function(final String path) {
		if(path==null)
			throw new IllegalArgumentException("path must not be null");
		this.key = path;
	}

	public Function(final String path, final T value) {
        if(path==null)
            throw new IllegalArgumentException("path must not be null");
        this.key = path;
        if (value instanceof Fi) {
            multiFi = (Fi) item;
            this.item = (T)  multiFi.get(0);
        } else {
            this.key = path;
            this.item = value;
        }
	}

	public Function(final String path, final T value, final int index) {
		this(path, value);
		this.index = index;
	}

	public Function(final String path, final T value, final String annotation) {
		this(path, value);
		this.annotation = annotation;
	}

	@Override
	public T getValue(Arg... args) throws EvaluationException, RemoteException {
		T val = this.item;
		URL url = null;
		try {
			substitute(args);
			if (isPersistent) {
				if (SdbUtil.isSosURL(val)) {
					Object out = ((URL)val).getContent();
					if (out instanceof UuidObject)
						val = (T) ((UuidObject) val).getObject();
				} else {
					if (val instanceof UuidObject) {
						url = SdbUtil.store(val);
					} else {
						UuidObject uo = new UuidObject(val);
						uo.setName(key);
						url = SdbUtil.store(uo);
					}
					this.item = (T)url;
				}
			} else if (val instanceof Invocation) {
				Context cxt = (Context) Arg.selectDomain(args);
				if (val instanceof Scopable) {
                    ((Scopable)val).setScope(scope);
                }
				val = (T) ((Invocation) val).invoke(cxt, args);
			} else if (val instanceof Evaluation) {
				val = ((Evaluation<T>) val).getValue(args);
			} else if (val instanceof ServiceFidelity) {
				// return the selected fidelity of this entry
				for (Arg arg : args) {
					if (arg instanceof Fidelity) {
						if (((Fidelity)arg).getPath().equals(key)) {
							((ServiceFidelity)val).setSelect(arg.getName());
							break;
						}
					}
				}
				val = (T) ((Entry)((ServiceFidelity) val).getSelect()).get(args);
			} else if (val instanceof Callable) {
				val = (T) ((Callable)val).call(args);
			} else if (val instanceof Service) {
				val = (T) ((Service)val).execute(args);
			}
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
		if (contextSelector != null) {
			try {
				val = (T) contextSelector.doSelect(val);
			} catch (ContextException e) {
				throw new EvaluationException(e);
			}
		}
		if (val instanceof Number && negative) {
			Number result = (Number) val;
			Double rd = result.doubleValue() * -1;
			val = (T) rd;
		}
		return val;
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
					this.item = (T) value;
				} else if (SdbUtil.isSosURL(this.item)) {
					if (((URL) this.item).getRef() == null) {
						this.item = (T) SdbUtil.store(value);
					} else {
						SdbUtil.update((URL) this.item, value);
					}
				}
			} catch (Exception e) {
				throw new SetterException(e);
			}
		} else {
			this.item = (T) value;
		}
	}

	public int index() {
		return index;
	}

	public Object annotation() {
		return annotation;
	}

	public void annotation(Object annotation) {
		this.annotation = annotation;
	}

	public boolean isAnnotated() {
		return annotation != null;
	}


	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(T o) {
		if (o == null)
			throw new NullPointerException();
		if (o instanceof Function<?>)
			return key.compareTo(((Function<?>) o).getName());
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
		if (object instanceof Function) {
			if (item != null && ((Function) object).item == null) {
				return false;
			} else if (item == null && ((Function) object).item != null) {
				return false;
			} else if (((Function) object).key.equals(key)
					&& ((Function) object).item == item) {
				return true;
			} else if (((Function) object).key.equals(key)
					&& ((Function) object).item.equals(item)) {
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
			if (item instanceof Evaluation && ((Evaluation) item).asis() != null) {
				if (this == item) {
					return "[" + key + "=" + ((Identifiable)item).getName() + "x]";  // loop
				}
				en = ((Evaluation) item).asis().toString();
			} else {
				en = "" + item;
			}
		}catch (EvaluationException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return "[" + key + "=" + en + "]";
	}

	public Function(String path, T value, boolean isPersistant, int index) {
		this(path, value, index);
		this.isPersistent = isPersistant;
	}

	public Mogram exert(Mogram mogram, Transaction txn, Arg... args) throws TransactionException,
			MogramException, RemoteException {
		Context cxt = null;
		Context out = new ServiceContext();
		if (mogram instanceof ProcModel) {
			if (item != null && item != Context.none)
				add((Context)mogram, this);
			((ServiceContext)mogram).getMogramStrategy().getResponsePaths().add(new Path(key));
			out = (Context) ((Model)mogram).getResponse();
		} else if (mogram instanceof ServiceContext) {
			if (item == null || item == Context.none) {
				out.putValue(key, ((Context)mogram).getValue(key));
			} else {
				if (item instanceof Evaluation) {
					this.setReactive(true);
					((ServiceContext)mogram).putValue(key, this);
				} else {
					((ServiceContext)mogram).putValue(key, item);
				}
				out.putValue(key, ((ServiceContext) mogram).getValue(key));
			}
		} else if (mogram instanceof Exertion) {
			if (item != null && item != Context.none)
				mogram.getContext().putValue(key, item);
			cxt =  mogram.exert(txn).getContext();
			out.putValue(key, cxt.getValue(key));
		}
		return out;
	}

	public boolean isNegative() {
		return negative;
	}

	public void setNegative(boolean negative) {
		this.negative = negative;
	}

	public ServiceFidelity getServiceFidelity() {
		return (ServiceFidelity)item;
	}

	public String fiName() {
		return ((Fidelity)getSelectedFidelity()).getName();
	}

	public Object getSelectedFidelity() {
		return getServiceFidelity().getSelect();
	}

	public ArgSet getArgs() {
		return null;
	}

	@Override
	public T call(Arg... args) throws EvaluationException, RemoteException {
		return getValue(args);
	}

}
