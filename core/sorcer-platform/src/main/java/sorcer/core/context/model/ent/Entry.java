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
import sorcer.co.tuple.Tuple2;
import sorcer.core.context.ServiceContext;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.service.*;
import sorcer.service.modeling.Model;
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
@SuppressWarnings("unchecked")
public class Entry<T> extends Tuple2<String, T> implements Service, Dependency, Comparable<T>, Setter, Evaluation<T>, Reactive<T>, Arg {
	private static final long serialVersionUID = 5168783170981015779L;

	public int index;
	
	protected String annotation;

	// its arguments are always evaluated if active (either Evaluataion or Invocation type)
	protected boolean isReactive = false;

    // dependency management for this Entry
    protected List<Evaluation> dependers = new ArrayList<Evaluation>();

	public Entry() {
	}

	public Entry(final String path) {
		if(path==null)
			throw new IllegalArgumentException("path must not be null");
		_1 = path;
	}
	
	public Entry(final String path, final T value) {
		if(path==null)
			throw new IllegalArgumentException("path must not be null");
		T v = value;
		if (v == null)
			v = (T)Context.none;

		_1 = path;
		if (SdbUtil.isSosURL(v)) {
			isPersistent = true;
		}
		this._2 = v;
	}
	
	public Entry(final String path, final T value, final int index) {
		this(path, value);
		this.index = index;
	}

	public Entry(final String path, final T value, final String association) {
		this(path, value);
		this.annotation = association;
	}

	@Override
	public T getValue(Arg... entries) throws EvaluationException, RemoteException {
		T val = this._2;
		URL url = null;
		try {
//			substitute(entries);
			if (isPersistent) {
				if (SdbUtil.isSosURL(val)) {
					val = (T) ((URL) val).getContent();
					if (val instanceof UuidObject)
						val = (T) ((UuidObject) val).getObject();
				} else {
					if (val instanceof UuidObject) {
						url =  (URL) SdbUtil.store(val);
					} else {
						UuidObject uo = new UuidObject(val);
						uo.setName(_1);
						url =  (URL)  SdbUtil.store(uo);
					}
					this._2 = (T)url;
				}
			} else if (val instanceof ServiceInvoker) {
				return ((ServiceInvoker<T>) val).invoke(entries);
			} else if (val instanceof Evaluation) {
				return ((Evaluation<T>) val).getValue(entries);
			}
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
		return val;
	}

	@Override
	public void setValue(Object value) throws SetterException, RemoteException {
		if (isPersistent) {
			try {
				if (SdbUtil.isSosURL(value)) {
					this._2 = (T) value;
				} else if (SdbUtil.isSosURL(this._2)) {
					if (((URL) this._2).getRef() == null) {
						this._2 = (T) SdbUtil.store(value);
					} else {
						SdbUtil.update((URL) this._2, value);
					}
				}
			} catch (Exception e) {
				throw new SetterException(e);
			}
		} else {
			this._2 = (T) value;
		}
	}

	public int index() {
		return index;
	}

	public String annotation() {
		return annotation;
	}
	
	public void annotation(String annotation) {
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
		if (o instanceof Entry<?>)
			return _1.compareTo(((Entry<?>) o).getName());
		else
			return -1;
	}
	
	@Override
	public int hashCode() {
		int hash = _1.length() + 1;
		return hash = hash * 31 + _1.hashCode();
	}
	
	@Override
	public boolean equals(Object object) {
		if ((object instanceof Entry<?>
				&& ((Entry<?>) object)._1.equals(_1)
				&&   ((Entry<?>) object)._2.equals(_2)))
			return true;
		else
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

	@Override
	public String toString() {
		String en = "";
		try {
			if (_2 instanceof Evaluation && ((Evaluation) _2).asis() != null) {
				en = ((Evaluation) _2).asis().toString();
			} else {
				en = "" + _2;
			}
		}catch (EvaluationException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return "[" + _1 + ":" + en + ":" + index + "]";
	}

	@Override
	public boolean isReactive() {
		return isReactive;
	}

	public Entry<T> setReactive(boolean isReactive) {
		this.isReactive = isReactive;
		return this;
	}

	@Override
	public Mogram service(Mogram mogram, Transaction txn) throws TransactionException,
			MogramException, RemoteException {
		Context cxt = null;
		Context out = new ServiceContext();
		if (mogram instanceof EntModel) {
			if (_2 != null && _2 != Context.none)
				add((Context)mogram, this);
			((ServiceContext)mogram).getModelStrategy().getResponsePaths().add(_1);
			out = (Context) ((Model)mogram).getResponse();
		} else if (mogram instanceof ServiceContext) {
			if (_2 == null || _2 == Context.none) {
				out.putValue(_1, ((Context)mogram).getValue(_1));
			} else {
				if (_2 instanceof Evaluation) {
					this.setReactive(true);
					((ServiceContext)mogram).putValue(_1, this);
				} else {
					((ServiceContext)mogram).putValue(_1, _2);
				}
				out.putValue(_1, ((ServiceContext) mogram).getValue(_1));
			}
		} else if (mogram instanceof Exertion) {
			if (_2 != null && _2 != Context.none)
				((Exertion) mogram).getContext().putValue(_1, _2);
			cxt = ((Exertion) mogram.exert(txn)).getContext();
			out.putValue(_1, cxt.getValue(_1));
		}
		return out;
	}

	@Override
	public Mogram service(Mogram mogram) throws TransactionException,
			MogramException, RemoteException {
		return service(mogram, null);
	}
}