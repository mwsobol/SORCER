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

import sorcer.core.context.Contexts;
import sorcer.core.context.PositionalContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.*;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.service.*;
import sorcer.service.Domain;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.Functionality;
import sorcer.util.Row;
import sorcer.service.Signature.ReturnPath;
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
 * key and its get (argument). The association <key, argument> is the definition
 * of an independent or a dependent argument. Arguments that dependent on other
 * arguments are subroutines (evaluators, invokers), so that, each time the
 * subroutine is called, its arguments for that call can be assigned to
 * the corresponding parameters of evaluators and invokers.
 *
 * @author Mike Sobolewski
 */
@SuppressWarnings({"unchecked", "rawtypes"  })
public class EntModel extends PositionalContext<Object> implements Model, Invocation<Object>, Contexter<Object> {

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
		setSubject("call/model", new Date());
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
        setSubject("call/model", new Date());
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
				ReturnPath rp = Arg.getReturnPath(args);
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
			} else if (val instanceof Pro) {
				if (((Pro) val).isCached()) {
					return ((Pro) val).getOut();
				} else if (((Pro) val).isPersistent()) {
					return ((Pro) val).evaluate();
				} else if ((((Pro) val).asis() instanceof Subroutine)) {
					bindEntry((Subroutine) ((Pro) val).asis());
				}
			}

			if (val instanceof Scopable && ((Scopable)val).getScope() != null) {
				((Scopable)val).getScope().setScope(this);
			} else if (val instanceof Entry && (((Entry)val).asis() instanceof Scopable)) {
				((Scopable) ((Entry)val).asis()).setScope(this);
			}
			if (val != null && val instanceof Pro) {
				Context inCxt = (Context) Arg.selectDomain(args);
				if (inCxt != null) {
					isChanged = true;
				}
				Object impl = ((Pro)val).getImpl();
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
					return ((Pro)val).getValue(args);
				}
			} else if (val instanceof Evaluation) {
				return ((Evaluation) val).evaluate(args);
			}  else if (val instanceof Mogram) {
				return exec((Service)val, args);
			}  else  if (val instanceof ServiceFidelity) {
				return new Subroutine(path, val).evaluate(args);
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
			try {
				realizeDependencies(entries);
			} catch (RemoteException | RoutineException e) {
				throw new EvaluationException(e);
			}
			return getValue((String)null, entries);
		} catch (ContextException e) {
			throw new EvaluationException(e);
		}
	}

	@Override
	public Object putValue(String path, Object value) throws ContextException {
		isChanged = true;
		Object obj = get(path);
		if (obj instanceof Pro) {
			((Pro) obj).setValue(value);
			return value;
		} else {
			if (value instanceof Scopable) {
				Object scope = ((Scopable) value).getScope();
				if (scope != null && ((Context) scope).size() > 0) {
					((Context) scope).append(this);
				} else {
					((Scopable) value).setScope(this);
				}
			}
		}
		return super.put(path, value);
	}

	public Pro getCall(String name) throws ContextException {
		Object obj = get(name);
		if (obj instanceof Pro)
			return (Pro) obj;
		else
			return new Pro(name, asis(name), this);
	}

	public Subroutine bindEntry(Subroutine ent) throws ContextException, RemoteException {
		ArgSet args = ent.getArgs();
		if (args != null) {
			for (Arg v : args)
				if (get(v.getName()) != null) {
					((Subroutine) v).setValue(getValue(v.getName()));
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
		Pro p = null;
		boolean changed = false;
		for (Arg obj : objects) {
			if (obj instanceof Fi) {
				continue;
			} else if (obj instanceof Pro) {
				p = (Pro) obj;
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
				appendCall(p);
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
	public Domain add(Identifiable... objects) throws ContextException, RemoteException {
		Pro p = null;
		boolean changed = false;
		for (Identifiable obj : objects) {
			String pn = obj.getName();
			if (obj instanceof Pro) {
				p = (Pro) obj;
			} else if (obj instanceof Functionality || obj instanceof Setup) {
				putValue(pn, obj);
			} else if (obj instanceof Entry) {
				putValue(pn, ((Entry)obj).asis());
				if (((Entry)obj).annotation() != null) {
					mark(obj.getName(), ((Entry)obj).annotation().toString());
				}
			} else {
				putValue(pn, obj);
			}

			if (p != null) {
				addCall(p);
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
				ReturnPath rp = ((ServiceContext)context).getReturnPath();
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
					if (o instanceof Pro) {
						if (o instanceof Agent) {
							if (((Agent) o).getScope() == null)
								((Agent) o).setScope(this);
							else
								((Agent) o).getScope().append(this);
							result = ((Agent) o).evaluate(entries);
						} else {
							Object i = ((Pro) get(((ServiceContext) context)
									.getExecPath().path())).asis();
							if (i instanceof ServiceInvoker) {
								result = ((ServiceInvoker) i).compute(entries);
							} else
								throw new InvocationException(
										"No such invoker at: "
												+ ((ServiceContext) context)
												.getReturnPath().path);
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

	private Object getReturnValue(ReturnPath rp) throws ContextException {
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
		} else if (rp != null && rp.path != null) {
			val = getValue(rp.path);
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
		if (entry instanceof Subroutine) {
			return (Subroutine)entry;
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

	private Pro putVar(String path, Functionality value) throws ContextException {
		putValue(path, value);
		markVar(this, path, value);
		return new Pro(path, value, this);
	}

	private void realizeDependencies(Arg... entries) throws RemoteException,
			RoutineException {
		List<Evaluation> dependers = getDependers();
		if (dependers != null && dependers.size() > 0) {
			for (Evaluation<Object> depender : dependers) {
				try {
					if (depender instanceof Invocation) {
						((Invocation) depender).invoke(this, entries);
					} else {
						((Evaluation) depender).evaluate(entries);
					}
				} catch (Exception e) {
					throw new RoutineException(e);
				}
			}
		}
	}

	/**
	 * Returns an enumeration of all path marking variable nodes.
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
