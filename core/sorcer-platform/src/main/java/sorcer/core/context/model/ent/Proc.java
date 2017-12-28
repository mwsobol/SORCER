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
import sorcer.core.invoker.ServiceInvoker;
import sorcer.service.*;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.VariabilityModeling;
import sorcer.service.modeling.func;
import sorcer.util.bdb.objects.UuidObject;
import sorcer.util.url.sos.SdbUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.*;

/**
 * In service-based modeling, a parameter (for short a proc) is a special kind of
 * variable, used in a service context {@link EntryModel} to refer to one of the
 * pieces of data provided as input to the invokers (subroutines of the
 * context). These pieces of data are called arguments.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({"unchecked", "rawtypes" })
public class Proc<T> extends Subroutine<T> implements Functionality<T>, Mappable<T>,
		Invocation<T>, Setter, Scopable, Comparable<T>, Reactive<T>, func<T> {

	private static final long serialVersionUID = 7495489980319169695L;
	 
	private static Logger logger = LoggerFactory.getLogger(Proc.class.getName());
	
	private Principal principal;

	// data store URL for this proc
	private URL dbURL;

	// A context returning eval at the path
	// that is this proc key
	// Sorcer Mappable: Context, Exertion, or Entry args
	protected Mappable mappable;

	public Proc(String name) {
		super(name);
		this.name = name;
		type = Functionality.Type.PROC;
	}
	
	public Proc(Identifiable identifiable) {
		this(identifiable.getName());
		out = (T)identifiable;
	}

	public Proc(String path, Object entity) {
		super(path);
		name = path;
		if (entity instanceof  Number || entity instanceof  String || entity instanceof  Date
                || entity instanceof  URL || entity instanceof  List || entity instanceof  Map
                || entity.getClass().isArray()) {
		    out = (T) entity;
        }

		if (entity instanceof Evaluation || entity instanceof Invocation) {
			if (entity instanceof ConditionalInvocation) {
				Context cxt = ((ServiceInvoker) entity).getScope();
				if (cxt != null) {
					scope = cxt;
					Condition condition = ((ConditionalInvocation) entity).getCondition();
					if (condition != null) {
						condition.setConditionalContext(cxt);
					}
				}
			}
		} else if ((entity instanceof Fidelity) && ((Fidelity)entity).getFiType().equals(Fi.Type.PROC)) {
			multiFi = (Fi) entity;
			impl = multiFi.getSelects().get(0);
			return;
		}
		this.impl = entity;
	}

	public Proc(String path, Object entity, Object scope)
			throws ContextException {
		this(path);
        if (entity instanceof  Number || entity instanceof  String || entity instanceof  Date
                || entity instanceof  List || entity instanceof  Map || entity.getClass().isArray()) {
            out = (T) entity;
        }
		if (entity instanceof String && scope instanceof Service) {
			mappable = (Mappable) scope;
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
	}
	
	public Proc(Mappable map, String name, String path) {
		this(name);
		impl =  path;
		mappable = map;
	}

    public void setValue(Object value) throws SetterException {
        if (isPersistent && mappable == null) {
            try {
                if (SdbUtil.isSosURL(this.impl)) {
                    SdbUtil.update((URL) this.impl, value);
                } else {
                    this.impl = (T) SdbUtil.store(value);
                }
            } catch (Exception e) {
                throw new SetterException(e);
            }
            return;
        }

        if (mappable != null) {
            try {
                if (this.impl instanceof String) {
                    Object val = mappable.asis((String) impl);
                    if (val instanceof Proc) {
                        ((Proc) val).setValue(value);
                    } else if (isPersistent) {
                        if (SdbUtil.isSosURL(val)) {
                            SdbUtil.update((URL) val, value);
                        } else {
                            URL url = SdbUtil.store(value);
                            Proc p = new Proc((String) this.out, url);
                            p.setPersistent(true);
                            if (mappable instanceof ServiceContext) {
                                ((ServiceContext) mappable).put((String) this.out, p);
                            } else {
                                mappable.putValue((String) this.out, p);
                            }
                        }
                    } else {
                        mappable.putValue((String) impl, value);
                    }
                } else if (impl instanceof URL) {
                    SdbUtil.update(SdbUtil.getUuid((URL) impl), value);
                }
            } catch (Exception e) {
                throw new SetterException(e);
            }
        } else if (value instanceof Evaluation) {
            this.out = (T) value;
        } else {
            this.out = (T) value;
            impl = (T) value;
        }
    }

	@Override
	public T get(Arg... args) {
		try {
			Domain context = Arg.selectDomain(args);
			if (context != null) {
				return invoke((ServiceContext) context, args);
			} else {
				return evaluate(args);
			}
		} catch (EvaluationException | RemoteException e) {
			logger.warn("Proc evaluation failed", e);
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#execute(sorcer.co.tuple.Parameter[])
	 */
	@Override
	public T evaluate(Arg... args) throws EvaluationException, RemoteException {
		// check for a constant or cached eval
		if (impl instanceof Number && !isPersistent) {
			return (T) impl;
		} else if (impl instanceof Incrementor || ((impl instanceof ServiceInvoker) &&
				scope != null && scope.isChanged())) {
			isValid = false;
		}

		if (impl != null && isValid && args.length == 0 && !isPersistent) {
			try {
				if (impl instanceof String
						&& mappable != null && mappable.getValue((String) impl) != null)
					return (T) mappable.getValue((String) impl);
				else if (impl instanceof Value) {
					return (T) ((Value) impl).getData();
				}
			} catch (ContextException e) {
				throw new EvaluationException(e);
			}
		}
		Object val = null;
		try {
			substitute(args);
			if (multiFi != null) {
			    val = ((Evaluation)multiFi.getSelect()).evaluate(args);
			    if (val instanceof String) {
                    return (T) scope.asis(val.toString());
				}
                return (T) val;
			}
			if (mappable != null && out instanceof String) {
				Object obj = mappable.asis((String) out);
				if (obj instanceof Proc && ((Proc)obj).isPersistent())
					return (T)((Proc)obj).evaluate();
				else
					val = (T) mappable.getValue((String) out);
			} else if (impl == null && scope != null) {
				val = (T) ((ServiceContext<T>) scope).get(name);
			} else {
				val = impl;
			}
			if (val instanceof Evaluation) {
				if (val instanceof Proc && ((Proc)val).asis() == null && out == null) {
					logger.warn("undefined proc: " + val);
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
					Object ev = ((Entry)val).asis();
					if (ev instanceof Scopable && ((Scopable)ev).getScope() != null) {
						if (scope instanceof VariabilityModeling) {
							((Scopable)ev).getScope().setScope(scope);
						} else {
							((Scopable)ev).getScope().append(scope);
						}
					}
				} else if (val instanceof Exertion) {
					// TODO context binding for all mograms, works for tasks only
					Context cxt = ((Exertion)val).getDataContext();
					List<String> paths = cxt.getPaths();
					for (String an : (Set<String>)((ServiceContext)scope).keySet()) {
						for (String p : paths) {
							if (p.endsWith(an)) {
								cxt.putValue(p, scope.getValue(an));
								break;
							}
						}
					}
				}
				out = ((Evaluation<T>) val).evaluate(args);
				isValid = true;
			}

			if (isPersistent) {
				val = impl;
				URL url = null;
				if (SdbUtil.isSosURL(val)) {
					val = ((URL) val).getContent();
					if (val instanceof UuidObject) {
						val = ((UuidObject) val).getObject();
					}
				} else {
					Proc p = null;
                    if (mappable != null && this.out instanceof String
                            && mappable.asis((String) out) != null) {
                        val = mappable.getValue((String) out);
                        url = SdbUtil.store(val);
                        p = new Proc((String) out, url);
                        p.setPersistent(true);
                        mappable.putValue((String) out, p);
                    } else if (out instanceof Identifiable) {
                        url = SdbUtil.store(val);
                        p = new Proc(((Identifiable) out).getName(), url);
                        p.setPersistent(true);
                    } else {
                        url = SdbUtil.store(val);
                    }
                    impl = (T) url;
                    out = null;
                }
				return (T) val;
			}
			return out;
		} catch (IOException | MogramException | SignatureException e) {
			// make the cache invalid
			impl = null;
			isValid = false;
			e.printStackTrace();
			throw new EvaluationException(e);
		}
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
					        scope = new EntryModel();
                        }
                        ((ServiceContext)scope).put(arg.getName(), ((Entry)arg).getData());
					}
				} else if (arg instanceof Fidelity && multiFi != null) {
					multiFi.selectSelect(arg.getName());
					multiFi.setChanged(true);
				} else if (arg instanceof Context) {
					if (scope == null)
						scope = (Context) arg;
					else
						scope.append((Context) arg);
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
		if (o instanceof Proc<?>)
			return name.compareTo(((Proc<?>) o).getName());
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

        return "proc [key: " + name + ", eval: " + ps + ", path: " + key + "]";
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

	/**
	 * <p>
	 * Returns a Contextable (Context or Exertion) of this Proc that by a its
	 * key provides values of this Proc.
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
			if (mappable != null)
				return (URL)mappable.asis((String) out);
			else
				return (URL) out;
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
	
	/* (non-Javadoc)
	 * @see sorcer.service.Invocation#invoke(sorcer.service.Context, sorcer.service.Arg[])
	 */
	@Override
	public T invoke(Context context, Arg... args) throws RemoteException,
			InvocationException {
		try {
			if (context != null)
				scope.append(context);
			return evaluate(args);
		} catch (Exception e) {
			throw new InvocationException(e);
		}
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Mappable#execute(java.lang.String, sorcer.service.Arg[])
	 */
	@Override
	public T getValue(String path, Arg... args) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				try {
					return (T) evaluate(args);
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
			else if (mappable != null)
				try {
					return (T)mappable.getValue(path.substring(name.length()), args);
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
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
				return out;
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
				this.out = (T)value;
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
		return hash * 31 + name.hashCode();
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
