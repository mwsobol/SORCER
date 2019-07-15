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
package sorcer.core.context.model;

import sorcer.co.tuple.ExecDependency;
import sorcer.core.context.Contexts;
import sorcer.core.context.ModelStrategy;
import sorcer.core.context.PositionalContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.*;
import sorcer.core.context.model.srv.Srv;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.service.*;
import sorcer.service.ContextDomain;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.Functionality;
import sorcer.util.Row;
import sorcer.util.bdb.objects.UuidObject;
import sorcer.util.url.sos.SdbUtil;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

import static sorcer.mo.operator.setValues;
import static sorcer.so.operator.exec;

/*
 * Copyright 2013 the original author or authors.
 * Copyright 20 SorcerSoft.org.
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

/**
 * The EntModel is an active shared service context as a map of associations,
 * key and its getValue (argument). The association <key, argument> is the definition
 * of an independent or a dependent argument. Arguments that dependent on other
 * arguments are subroutines (evaluators, invokers), so that, each time the
 * subroutine is called, its arguments for that prc can be assigned to
 * the corresponding parameters of evaluators and invokers.
 *
 * @author Mike Sobolewski
 */
@SuppressWarnings({"unchecked", "rawtypes"  })
public class EntModel extends PositionalContext<Object> implements Model, Invocation<Object> {

    private static final long serialVersionUID = -6932730998474298653L;

	public static EntModel instance(Signature builder) throws SignatureException {
		EntModel model = (EntModel) sorcer.co.operator.instance(builder);
		model.setBuilder(builder);
		return model;
	}

    public EntModel() {
		super();
		key = PROC_MODEL;
		out = new Date();
		setSubject("prc/model", new Date());
		isRevaluable = true;
	}

    public EntModel(String name) {
        super(name);
		isRevaluable = true;
	}

	public EntModel(String name, Signature builder) {
		this(name);
		this.builder = builder;
	}

    public EntModel(Context context) throws RemoteException, ContextException {
        super(context);
        key = PROC_MODEL;
        setSubject("prc/model", new Date());
		isRevaluable = true;
	}

    public EntModel(Identifiable... objects) throws RemoteException,
            ContextException {
        this();
        add(objects);
    }

	public Object getValue(String path, Arg... args) throws EvaluationException, ContextException {
		try {
			append(args);
			Object val = null;
			if (path != null) {
				val = get(path);
			} else {
				Context.Return rp = Arg.getReturnPath(args);
				if (rp != null)
					val = getReturnValue(rp);
				else if (mogramStrategy.getResponsePaths() != null
					&& mogramStrategy.getResponsePaths().size() == 1) {
					val = asis(mogramStrategy.getResponsePaths().get(0).getName());
				} else {
					val = super.getValue(path, args);
				}
			}

			if (val instanceof Number || val instanceof String) {
				return val;
			} else if (val instanceof Value) {
				return ((Value) val).valuate();
			} else if (SdbUtil.isSosURL(val)) {
				val = ((URL) val).getContent();
				if (val instanceof UuidObject) {
					val = ((UuidObject) val).getObject();
				}
				return val;
			} else if (val instanceof Prc) {
				if (((Prc) val).isCached()) {
					return ((Prc) val).getOut();
				} else if (((Prc) val).isPersistent()) {
					return ((Prc) val).evaluate();
				} else if ((((Prc) val).asis() instanceof Function)) {
					bindEntry((Function) ((Prc) val).asis());
				}
			}

			if (val instanceof Scopable && ((Scopable)val).getScope() != null) {
				((Scopable)val).getScope().setScope(this);
			} else if (val instanceof Entry && (((Entry)val).asis() instanceof Scopable)) {
				((Scopable) ((Entry)val).asis()).setScope(this);
			}
			if (val != null && val instanceof Prc) {
				Context inCxt = (Context) Arg.selectDomain(args);
				if (inCxt != null) {
					isChanged = true;
				}
				Object impl = ((Prc)val).getImpl();
				if (impl instanceof Mogram) {
					return exec((Service)impl, args);
				} else if (impl instanceof Invocation) {
					if (isChanged) {
						((ServiceInvoker) impl).setValid(false);
					}
					// this is set as the scope for impl
					if (inCxt != null) {
						inCxt.setScope(this);
						return ((Invocation) impl).invoke(inCxt, args);
					} else {
					    Context ic = ((ServiceInvoker)impl).getScope();
					    if (ic != null && ic.size() > 0) {
					        ic.setScope(this);
                        }
						return ((Invocation) impl).invoke(ic, args);
					}
				} else if (impl instanceof Evaluation) {
					return ((Evaluation) impl).evaluate(args);
				} else {
					return ((Prc)val).getValue(args);
				}
			} else if (val instanceof Evaluation) {
				return ((Evaluation) val).evaluate(args);
			}  else if (val instanceof Mogram) {
				return exec((Service)val, args);
			}  else  if (val instanceof ServiceFidelity) {
				return new Function(path, val).evaluate(args);
			} else if (path == null && val == null
				&& mogramStrategy.getResponsePaths() != null) {
				if (mogramStrategy.getResponsePaths().size() == 1) {
					return getValue(mogramStrategy.getResponsePaths().get(0).getName(), args);
				} else {
					return getResponse();
				}
			} else {
				if (val == null && scope != null && scope != this) {
					Object o = scope.getValue(path);
					if (o != Context.none && o != null) {
						return o;
					} else if (isSoft) {
						return scope.getSoftValue(path);
					}
				} else {
					if (val == null & isSoft) {
						return getSoftValue(path);
					} else {
						return val;
					}
				}
			}
			return null;
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see sorcer.service.Evaluation#execute(sorcer.core.context.Path.Entry[])
	 */
	@Override
	public Object getValue(Arg... entries) throws EvaluationException {
		try {
			realizeDependencies(entries);
			return getValue((String)null, entries);
		} catch (ContextException | RoutineException |RemoteException e) {
			throw new EvaluationException(e);
		}
	}

	@Override
	public Object putValue(String path, Object value) throws ContextException {
		isChanged = true;
		Object obj = get(path);
		if (obj instanceof Prc) {
			((Prc) obj).setValue(value);
			return value;
		} else {
			if (value instanceof Scopable) {
                Object scope = ((Scopable) value).getScope();
                if (scope != null && ((Context) scope).size() > 0) {
                    ((Context) scope).append(this);
                }
                if (value instanceof Srv && ((Srv) value).getImpl() instanceof ServiceInvoker) {
                    ((ServiceInvoker) ((Entry) value).getImpl()).setInvokeContext(this);
                } else if (value instanceof ServiceInvoker) {
                    ((ServiceInvoker) value).setInvokeContext(this);
                } else {
                    ((Scopable) value).setScope(this);
                }
            }
		}
		return super.put(path, value);
	}

	public Prc getCall(String name) throws ContextException {
		Object obj = get(name);
		if (obj instanceof Prc)
			return (Prc) obj;
		else
			return new Prc(name, asis(name), this);
	}

	public Function bindEntry(Function ent) throws ContextException, RemoteException {
		ArgSet args = ent.getArgs();
		if (args != null) {
			for (Arg v : args)
				if (get(v.getName()) != null) {
					((Function) v).setValue(getValue(v.getName()));
				}
		}
		return ent;
	}

	public EntModel add(List<Identifiable> objects) throws RemoteException, ContextException {
		Identifiable[] objs = new Identifiable[objects.size()];
		objects.toArray(objs);
		add(objs);
		return this;
	}

	public EntModel append(Arg... objects) throws ContextException {
		Prc p = null;
		boolean changed = false;
		for (Arg obj : objects) {
			if (obj instanceof Fi) {
				continue;
			} else if (obj instanceof Prc) {
				p = (Prc) obj;
			} else if (obj instanceof Entry) {
				putValue((String) ((Entry) obj).key(),
						((Entry) obj).getOut());
			}
//			restrict identifiables
//			else if (obj instanceof Identifiable) {
//				String pn = obj.getName();
//				p = new Proc(pn, obj, new EntModel(pn).append(this));
//			}

			if (p != null) {
				appendPrc(p);
				changed = true;
			}
		}

		if (changed) {
		isChanged = true;
		updateEvaluations();
	}
		return this;
}

	@Override
	public ContextDomain add(Identifiable... objects) throws ContextException, RemoteException {
		Prc p = null;
		boolean changed = false;
		for (Identifiable obj : objects) {
			String pn = obj.getName();
			if (obj instanceof Prc) {
				p = (Prc) obj;
				if (p.getScope() == null) {
					p.setScope(this);
				}
			} else if (obj instanceof Functionality || obj instanceof Setup) {
				putValue(pn, obj);
				if (obj instanceof Functionality  && ((Functionality)obj).getScope() == null) {
					((Functionality)obj).setScope(this);
				}
			} else if (obj instanceof Entry) {
				putValue(pn, ((Entry)obj).asis());
				if (((Entry)obj).annotation() != null) {
					mark(obj.getName(), ((Entry)obj).annotation().toString());
				}
			} else {
				putValue(pn, obj);
			}

			if (p != null) {
				addPrc(p);
				changed = true;
			}
		}
		if (changed) {
			isChanged = true;
			updateEvaluations();
		}
		return this;
	}

	protected void updateEvaluations() {
		Iterator<Map.Entry<String,Object>>  i = entryIterator();
		while (i.hasNext()) {
			Map.Entry<String, Object> entry = i.next();
			Object val = entry.getValue();
			if (val instanceof Entry && ((Entry)val).getImpl() instanceof Evaluation) {
				((Entry) val).setValid(false);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see sorcer.service.Invocation#invoke(sorcer.service.Context,
	 * sorcer.service.Args[])
	 */
	@Override
	public Object invoke(Context context, Arg... entries) throws RemoteException,
			InvocationException {
		Object result = null;
		try {
			if (context != null) {
				Context.Return rp = ((ServiceContext)context).getContextReturn();
				this.append(context);
				// check for multiple response of this model
				if (rp != null && rp.outPaths.size() > 0) {
					Object val = null;
					if (rp.outPaths.size() == 1)
						val = getValue(rp.outPaths.get(0).path);
					else {
						List vals = new ArrayList(rp.outPaths.size());
						for (int j = 0; j < rp.outPaths.size(); j++)   {
							vals.add(getValue(rp.outPaths.get(j).path));
						}
						val = new Row(Path.getPathList(rp.outPaths), vals);
					}
					((ServiceContext)context).setFinalized(true);
					return val;
				}
				if (((ServiceContext) context).getExecPath() != null) {
					Object o = get(((ServiceContext) context).getExecPath()
							.path());
					if (o instanceof Prc) {
						if (o instanceof Agent) {
							if (((Agent) o).getScope() == null)
								((Agent) o).setScope(this);
							else
								((Agent) o).getScope().append(this);
							result = ((Agent) o).evaluate(entries);
						} else {
							Object i = ((Prc) get(((ServiceContext) context)
									.getExecPath().path())).asis();
							if (i instanceof ServiceInvoker) {
								result = ((ServiceInvoker) i).evaluate(entries);
							} else
								throw new InvocationException(
										"No such invoker at: "
												+ ((ServiceContext) context)
												.getContextReturn().returnPath);
						}
					}
				} else {
					result = getValue(entries);
				}
			} else {
				result = getValue(entries);
			}
			return result;
		} catch (ContextException e) {
			throw new InvocationException(e);
		}
	}

	private Object getReturnValue(Context.Return rp) throws ContextException {
		Object val = null;
		// check for multiple response of this model
		if (rp != null && rp.outPaths.size() > 0) {
			if (rp.outPaths.size() == 1)
				val = getValue(rp.outPaths.get(0).path);
			else {
				List vals = new ArrayList(rp.outPaths.size());
				for (int j = 0; j < rp.outPaths.size(); j++) {
					vals.add(getValue(rp.outPaths.get(j).path));
				}
				val = new Row(Path.getPathList(rp.outPaths), vals);
			}
		} else if (rp != null && rp.returnPath != null) {
			val = getValue(rp.returnPath);
		}
		return val;
	}

	public void setContextChanged(boolean contextChanged) {
		this.isChanged = contextChanged;
	}

	@Override
	public Entry entry(String path) {
		Object entry = null;
		if (path != null) {
			entry = data.get(path);
		}
		if (entry instanceof Function) {
			return (Function)entry;
		} else {
			return null;
		}
	}

	public Functionality getVar(String name) throws ContextException {
		String key;
		Object val = null;
		Iterator e = keyIterator();
		while (e.hasNext()) {
			key = (String) e.next();
			val = getValue(key);
			if (val instanceof Functionality) {
				if (((Functionality) val).getName().equals(name))
					return (Functionality) val;
			}
		}
		throw new ContextException("No such variability in context: " + name);
	}

	private Prc putVar(String path, Functionality value) throws ContextException {
		putValue(path, value);
		markVar(this, path, value);
		return new Prc(path, value, this);
	}

	protected void realizeDependencies(Arg... args) throws RemoteException,
			RoutineException {
		List<Evaluation> dependers = getModelDependers();
		if (dependers != null && dependers.size() > 0) {
			for (Evaluation<Object> depender : dependers) {
				try {
					if (depender instanceof Invocation) {
						((Invocation) depender).invoke(this, args);
					} else {
						depender.evaluate(args);
					}
				} catch (Exception e) {
					throw new RoutineException(e);
				}
			}
		}
	}

	public void execDependencies(String path, Arg... args) throws ContextException {
		Map<String, List<ExecDependency>> dpm = ((ModelStrategy)mogramStrategy).getDependentPaths();
		if (dpm != null && dpm.get(path) != null) {
			List<ExecDependency> del = dpm.get(path);
			Entry entry = entry(path);
			if (del != null && del.size() > 0) {
				for (ExecDependency de : del) {
					List<Path> dpl = (List<Path>) de.getImpl();
					if (de.getType().equals(Functionality.Type.FIDELITY)) {
						Fidelity deFi = (Fidelity) de.annotation();
						if (deFi.getOption() == Fi.Type.IF) {
							if (((Fidelity) entry.getMultiFi().getSelect()).getName().equals(deFi.getName())) {
								// apply only to matched fidelity
								if (dpl != null && dpl.size() > 0) {
									for (Path p : dpl) {
										getValue(p.path, args);
									}
								}
							}
							continue;
						} else {
							// first select the requested fidelity
							try {
								entry.getMultiFi().selectSelect(((Fidelity) de.annotation()).getName());
							} catch (ConfigurationException e) {
								throw new ContextException(e);
							}
						}
					} else if (de.getType().equals(Functionality.Type.CONDITION)) {
						Conditional condition = de.getCondition();
						if (condition.isTrue()) {
							// apply only if condition is true
							if (dpl != null && dpl.size() > 0) {
								for (Path p : dpl) {
									getValue(p.path, args);
								}
							}
						}
						continue;
					}
					if (dpl != null && dpl.size() > 0) {
						for (Path p : dpl) {
							getValue(p.path, args);
						}
					}

				}
			}
		}
	}

	protected void execDependencies(Signature sig, Arg... args) throws ContextException {
		execDependencies(sig.getName(), args);
	}
	/**
	 * Returns an enumeration of all contextReturn marking variable nodes.
	 *
	 * @return enumeration of marked variable nodes.
	 * @throws ContextException
	 */
	public Enumeration getVarPaths(Functionality var) throws ContextException {
		String assoc = VAR_NODE_TYPE + APS + var.getName() + APS + var.getType();
		String[] paths = Contexts.getMarkedPaths(this, assoc);
		Vector outpaths = new Vector();
		if (paths != null)
			for (int i = 0; i < paths.length; i++)
				outpaths.add(paths[i]);

		return outpaths.elements();

	}

	public static Functionality[] getMarkedVariables(Context sc,
                                                     String association) throws ContextException {
		String[] paths = Contexts.getMarkedPaths(sc, association);
		java.util.Set nodes = new HashSet();
		Object obj = null;
		for (int i = 0; i < paths.length; i++) {
			try {
				obj = sc.getValue(paths[i]);
			} catch (RemoteException e) {
				throw new ContextException(e);
			}
			if (obj != null && obj instanceof Functionality)
				nodes.add(obj);
		}
		Functionality[] nodeArray = new Functionality[nodes.size()];
		nodes.toArray(nodeArray);
		return nodeArray;
	}

	/**
	 * set context multitype as variable
	 * In ServiceContexr#init()
	 * DATA_NODE_TYPE + APS + VAR + APS + multitype + APS
	 */
	public static Context markVar(Context cntxt, String path, Functionality var)
			throws ContextException {
		return cntxt.mark(path, Context.VAR_NODE_TYPE + APS
				+ var.getName() + APS + var.getType());
	}
	
	public Context<Object> appendNew(Context<Object> context)
			throws ContextException, RemoteException {
		ServiceContext cxt = (ServiceContext) context;
		Iterator<Map.Entry<String, Object>> i = cxt.entryIterator();
		while (i.hasNext()) {
			Map.Entry<String, Object> e = i.next();
			if (!containsPath(e.getKey()) && e.getKey().equals("script")) {
				put(e.getKey(), context.asis(e.getKey()));
			}
		}
		return this;
	}

	@Override
	public String toString() {
		return this.getClass().getName() + ":" + getName() + "\nkeys: " + keySet()
				+ "\n" + super.toString();
	}

}
