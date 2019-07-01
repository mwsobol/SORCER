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
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.EntModel;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.service.*;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.Variability;
import sorcer.service.modeling.func;
import sorcer.util.bdb.objects.UuidObject;
import sorcer.util.url.sos.SdbUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.*;

import static sorcer.mo.operator.setValues;
import static sorcer.so.operator.eval;
import static sorcer.so.operator.exec;

/**
 * In service-based modeling, a parameter (for short a prc) is a special kind of
 * variable, used in a service context {@link EntModel} to refer to one of the
 * pieces of data provided as input to the invokers (subroutines of the
 * context). These pieces of data are called arguments.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({"unchecked", "rawtypes" })
public class Prc<T> extends Subroutine<T> implements Invocation<T>,
	Setter, Scopable, Comparable<T>, Reactive<T>, func<T> {

	private static final long serialVersionUID = 7495489980319169695L;
	 
	private static Logger logger = LoggerFactory.getLogger(Prc.class.getName());
	
	private Principal principal;

	// data store URL for this prc
	private URL dbURL;

	public Prc(String name) {
		super(name);
		this.name = name;
		type = Functionality.Type.PROC;
	}
	
	public Prc(Identifiable identifiable) {
		this(identifiable.getName());
		impl = (T)identifiable;
	}

	public Prc(String path, Object entity) {
		super(path);
		name = path;
		if (entity instanceof  Number || entity instanceof  String || entity instanceof  Date
                || entity instanceof  URL || entity instanceof  List || entity instanceof  Map
                || entity.getClass().isArray()) {
		    out = (T) entity;
        }

		if (entity instanceof Evaluation || entity instanceof Invocation) {
			if (entity instanceof ConditionalInvocation) {
				Context cxt = ((ServiceInvoker) entity).getInvokeContext();
				if (cxt != null) {
					scope = cxt;
					Condition condition = ((ConditionalInvocation) entity).getCondition();
					if (condition != null) {
						condition.setConditionalContext(cxt);
					}
				}
			}
            impl = entity;
		} else if ((entity instanceof Fidelity) && ((Fidelity)entity).getFiType().equals(Fi.Type.ENTRY)) {
			multiFi = (Fi) entity;
			impl = multiFi.getSelects().get(0);
		}
		isValid = true;
	}

	public Prc(String path, Object entity, Object scope)
			throws ContextException {
		this(path);
        if (entity instanceof  Number || entity instanceof  String || entity instanceof  Date
                || entity instanceof  List || entity instanceof  Map || entity.getClass().isArray()) {
            out = (T) entity;
        }
		if (entity instanceof String && scope instanceof Service) {
			if (scope instanceof Context) {
				if (((ServiceContext) scope).containsPath(Condition._closure_))
					((Context) scope).remove(Condition._closure_);
				this.scope = (Context) scope;

			}
		}
		if (entity instanceof Scopable) {
			((Scopable) entity).setScope(this.scope);
		}
		this.impl = entity;
		isValid = true;
	}
	
	public Prc(Contextion map, String name, String path) {
		this(name);
		impl =  path;
	}

	public void setValue(Object value) throws SetterException {
		try {
			if (isPersistent) {
				if (SdbUtil.isSosURL(value)) {
					impl = (T) value;
				} else if (SdbUtil.isSosURL(impl)) {
					if (((URL) impl).getRef() == null) {
						impl = (T) SdbUtil.store(value);
					} else {
						SdbUtil.update((URL) impl, value);
					}
				}
				return;
			}
			if (value instanceof Evaluation) {
				this.out = (T) ((Evaluation)value).evaluate();
			} else {
				this.out = (T) value;
			}
		} catch (Exception e) {
			throw new SetterException(e);
		}
	}

//	public T get(Arg... args) {
//		try {
//			Domain context = Arg.selectDomain(args);
//			if (context != null) {
//				return invoke((ServiceContext) context, args);
//			} else {
//				return evaluate(args);
//			}
//		} catch (EvaluationException | RemoteException e) {
//			logger.warn("Proc evaluation failed", e);
//		}
//		return null;
//	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#execute(sorcer.co.tuple.Parameter[])
	 */
	@Override
	public T evaluate(Arg... args) throws EvaluationException, RemoteException {
		// check for a constant or cached eval
		if (impl == null && out != null && !isPersistent) {
			return out;
		} else if (out instanceof Number &&  isValid && impl == null && !isPersistent) {
			return (T) out;
		} else if (impl instanceof Incrementor || ((impl instanceof ServiceInvoker) &&
			scope != null && scope.isChanged())) {
			isValid = false;
		}

		Object val = null;
		try {
            substitute(args);
			if (!isValid || isCached) {
				out = null;
			}
			if (multiFi != null) {
				val = ((Evaluation)multiFi.getSelect()).evaluate(args);
				if (val instanceof String) {
					return (T) scope.asis(val.toString());
				}
				return (T) val;
			}

			if (impl == null && scope != null) {
				val = (T) ((ServiceContext<T>) scope).get(name);
			} else if (out == null) {
				val = impl;
			} else {
				val = out;
			}
			if (val instanceof Prc && ((Prc)val).asis() == null && out == null) {
				logger.warn("undefined prc: " + val);
				return null;
			}
			// direct scope
			if (val instanceof Scopable) {
				if (((Scopable)val).getScope() == null || ((Scopable)val).getScope().size() == 0) {
					((Scopable)val).setScope(scope);
				} else {
					((Scopable) val).getScope().append(scope);
				}
			}
			if (val instanceof Entry) {
				// indirect scope for entry values
				Object ev = ((Entry) val).asis();
				if (ev instanceof Scopable && ((Scopable) ev).getScope() != null) {
					if (scope instanceof Variability) {
						((Scopable) ev).getScope().setScope(scope);
					} else {
						((Scopable) ev).getScope().append(scope);
					}
				}
			}
			if (val instanceof Invocation) {
				Context cxt = (Context) Arg.selectDomain(args);
				if (cxt == null) {
					cxt = this.scope;
				} else {
                    cxt.setScope(this.scope);
                }
				val = (T) ((Invocation) val).invoke(cxt, args);
			} else if (val instanceof Evaluation) {
				val = ((Evaluation<T>) val).evaluate(args);
			}  else if (val instanceof Mogram) {
				try {
					val = exec((Service)val, args);
				} catch (ServiceException e) {
					throw new EvaluationException(e);
				}
			}
			out = (T) val;
			isValid = true;


			if (isPersistent) {
				if (out == null && impl != null) {
					val = impl;
				} else {
					val = out;
				}
				if (SdbUtil.isSosURL(val)) {
					val = ((URL) val).getContent();
					if (val instanceof UuidObject) {
						val = ((UuidObject) val).getObject();
					}
				} else {
					URL url = SdbUtil.store(val);
					impl = url;
					out = null;
				}
			}
		} catch (IOException | MogramException | SignatureException e) {
			// make the cache invalid
			out = null;
			isValid = false;
			e.printStackTrace();
			throw new EvaluationException(e);
		}
		return (T) val;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#substitute(sorcer.co.tuple.Parameter[])
	 */
	@Override
	public void substitute(Arg... args) throws SetterException {
		if (args == null)
			return;
		for (Arg arg : args) {
			try {
				if (arg instanceof Entry) {
					if (name.equals(arg.getName())) {
                        out = ((Entry<T>) arg).getData();
                    } else {
					    if (scope == null) {
					        scope = new EntModel();
                        }
                        ((ServiceContext)scope).put(arg.getName(), ((Entry)arg).getData());
					}
				} else if (arg instanceof Fidelity && multiFi != null) {
					try {
						multiFi.selectSelect(arg.getName());
					} catch (ConfigurationException e) {
						throw new SetterException(e);
					}
					multiFi.setChanged(true);
					isValid = false;
				} else if (arg instanceof Context) {
					if (scope == null)
						scope = (Context) arg;
					else {
						setValues(scope, (Context) arg);
					}
					isValid = false;
					isChanged = false;
				}
			} catch (ContextException e) {
				throw new SetterException(e);
			}
		}
	}

	public Context getScope() {
		return scope;
	}

	public void setScope(Context scope) {
		if (scope != null && scope.containsPath(Condition._closure_))
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
		if (o instanceof Prc<?>)
			return name.compareTo(((Prc<?>) o).getName());
		else
			return -1;
	}

	@Override
	public String toString() {
        String ps = "";
        if (out instanceof Evaluation) {
            try {
                ps = "" + ((Evaluation) out).asis();
            } catch (EvaluationException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            ps = "" + out;
        }

        return "prc [key: " + name + ", eval: " + ps + ", path: " + key + "]";
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
	 * @see sorcer.vfe.Variability#getFiType()
	 */
	@Override
	public Type getType() {
		return type;
	}

	@Override
	public Class getValueType() {
		return impl.getClass();
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
	public Object getArg(String varName) throws ArgException {
		try {
			return (T) scope.getValue(varName);
		} catch (ContextException | RemoteException e) {
			throw new ArgException(e);
		}
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#isValueCurrent()
	 */
	@Override
	public boolean isValueCurrent() {
		return false;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#valueChanged(java.lang.Object)
	 */
	@Override
	public void valueChanged(Object obj) throws EvaluationException {
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
				return (URL) impl;
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
	
	/* (non-Javadoc)
	 * @see sorcer.service.Invocation#invoke(sorcer.service.Context, sorcer.service.Arg[])
	 */
	@Override
	public T invoke(Context context, Arg... args) throws RemoteException, InvocationException {
		try {
			Context inCxt = (Context) Arg.selectDomain(args);
			if (inCxt != null) {
				return (T) ((Invocation)impl).invoke(inCxt, args);
			} else {
				return (T) ((Evaluation)impl).evaluate(args);
			}
		} catch (Exception e) {
			throw new InvocationException(e);
		}
	}

	public T getValue(String path, Arg... args) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				try {
					return (T) evaluate(args);
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
		}
		return null;
	}

	public T asis(String path) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				return out;
		}
		return null;
	}

	public T putValue(String path, Object value) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				this.out = (T)value;
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
			Prc callEntry = (Prc)i.next();
			try {
				putValue(callEntry.getName(), callEntry.asis());
			} catch (Exception e) {
				throw new EvaluationException(e);
			} 
		}
		
	}
	
	@Override
	public int hashCode() {
		int hash = name.length() + 1;
		return hash * 31 + name.hashCode();
	}
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof Prc
				&& ((Prc) object).name.equals(name))
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

	@Override
	public boolean isReactive() {
		return true;
	}

	@Override
	public Object execute(Arg... args) throws MogramException, RemoteException {
		Context cxt = (Context) Arg.selectDomain(args);
		if (cxt != null) {
			scope = cxt;
			return evaluate(args);
		} else {
			return evaluate(args);
		}
	}

	public String gePath() {
		return key;
	}
}
