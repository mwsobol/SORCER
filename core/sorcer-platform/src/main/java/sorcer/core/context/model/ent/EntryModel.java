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
package sorcer.core.context.model.ent;

import sorcer.core.context.Contexts;
import sorcer.core.context.PositionalContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.service.*;
import sorcer.service.Domain;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.Functionality;
import sorcer.util.Row;
import sorcer.service.Signature.ReturnPath;
import sorcer.util.bdb.objects.UuidObject;
import sorcer.util.url.sos.SdbUtil;

import java.io.IOException;
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
 * The EntryModel is an active shared service context as a map of associations,
 * key and its value (argument). The association <key, argument> is the definition
 * of an independent or a dependent argument. Arguments that dependent on other
 * arguments are subroutines (evaluators, invokers), so that, each time the
 * subroutine is called, its arguments for that call can be assigned to
 * the corresponding parameters of evaluators and invokers.
 *
 * @author Mike Sobolewski
 */
@SuppressWarnings({"unchecked", "rawtypes"  })
public class EntryModel extends PositionalContext<Object> implements Model, Invocation<Object>, Contexter<Object> {

    private static final long serialVersionUID = -6932730998474298653L;

	public static EntryModel instance(Signature builder) throws SignatureException {
		EntryModel model = (EntryModel) sorcer.co.operator.instance(builder);
		model.setBuilder(builder);
		return model;
	}

    public EntryModel() {
		super();
		key = PROC_MODEL;
		out = new Date();
		setSubject("proc/model", new Date());
		isRevaluable = true;
	}

    public EntryModel(String name) {
        super(name);
		isRevaluable = true;
	}

	public EntryModel(String name, Signature builder) {
		this(name);
		this.builder = builder;
	}

    public EntryModel(Context context) throws RemoteException, ContextException {
        super(context);
        key = PROC_MODEL;
        setSubject("proc/model", new Date());
		isRevaluable = true;
	}

    public EntryModel(Identifiable... objects) throws RemoteException,
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
			} else if (val instanceof Proc) {
				if (((Proc) val).isCached()) {
					return ((Proc) val).getOut();
				} else if (((Proc) val).isPersistent) {
					return ((Proc) val).evaluate();
				} else if ((((Proc) val).asis() instanceof Subroutine)) {
					bindEntry((Subroutine) ((Proc) val).asis());
				}
			}

			if (val instanceof Scopable && ((Scopable)val).getScope() != null) {
				((Scopable)val).getScope().setScope(this);
			} else if (val instanceof Entry && (((Entry)val).asis() instanceof Scopable)) {
				((Scopable) ((Entry)val).asis()).setScope(this);
			}
			if (val != null && val instanceof Proc) {
				Context inCxt = (Context) Arg.selectDomain(args);
				if (inCxt != null) {
					isChanged = true;
				}
				Object impl = ((Proc)val).getImpl();
				if (impl instanceof Mogram) {
					return exec((Mogram)impl, args);
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
					return ((Proc)val).getValue(args);
				}
			} else if (val instanceof Evaluation) {
				return ((Evaluation) val).evaluate(args);
			}  else if (val instanceof Mogram) {
				return exec((Mogram)val, args);
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
					if (o != Context.none && o != null)
						return o;
					else
						return scope.getSoftValue(path);
				} else {
					if (val == null)
						return getSoftValue(path);
					else
						return val;
				}
			}
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
			} catch (RemoteException | ExertionException e) {
				throw new EvaluationException(e);
			}
			return getValue(null, entries);
		} catch (ContextException e) {
			throw new EvaluationException(e);
		}
	}

	@Override
	public Object putValue(String path, Object value) throws ContextException {
		isChanged = true;
		Object obj = get(path);
		if (obj instanceof Proc) {
			((Proc) obj).setValue(value);
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

	public Proc getProc(String name) throws ContextException {
		Object obj = get(name);
		if (obj instanceof Proc)
			return (Proc) obj;
		else
			return new Proc(name, asis(name), this);
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

	public EntryModel add(List<Identifiable> objects) throws RemoteException, ContextException {
		Identifiable[] objs = new Identifiable[objects.size()];
		objects.toArray(objs);
		add(objs);
		return this;
	}

	public EntryModel append(Arg... objects) throws ContextException {
		Proc p = null;
		boolean changed = false;
		for (Arg obj : objects) {
			if (obj instanceof Fi) {
				continue;
			} else if (obj instanceof Proc) {
				p = (Proc) obj;
			} else if (obj instanceof Entry) {
				putValue((String) ((Entry) obj).key(),
						((Entry) obj).getOut());
			}
//			restrict identifiables
//			else if (obj instanceof Identifiable) {
//				String pn = obj.getName();
//				p = new Proc(pn, obj, new EntryModel(pn).append(this));
//			}

			if (p != null) {
				appendProc(p);
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
		Proc p = null;
		boolean changed = false;
		for (Identifiable obj : objects) {
			String pn = obj.getName();
			if (obj instanceof Proc) {
				p = (Proc) obj;
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
				addProc(p);
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
				if (rp != null && rp.outPaths.length > 0) {
					Object val = null;
					if (rp.outPaths.length == 1)
						val = getValue(rp.outPaths[0].path);
					else {
						List vals = new ArrayList(rp.outPaths.length);
						for (int j = 0; j < rp.outPaths.length; j++)   {
							vals.add(getValue(rp.outPaths[j].path));
						}
						val = new Row(Path.getPathList(rp.outPaths), vals);
					}
					((ServiceContext)context).setFinalized(true);
					return val;
				}
				if (((ServiceContext) context).getExecPath() != null) {
					Object o = get(((ServiceContext) context).getExecPath()
							.path());
					if (o instanceof Proc) {
						if (o instanceof Agent) {
							if (((Agent) o).getScope() == null)
								((Agent) o).setScope(this);
							else
								((Agent) o).getScope().append(this);
							result = ((Agent) o).evaluate(entries);
						} else {
							Object i = ((Proc) get(((ServiceContext) context)
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
		if (rp != null && rp.outPaths.length > 0) {
			if (rp.outPaths.length == 1)
				val = getValue(rp.outPaths[0].path);
			else {
				List vals = new ArrayList(rp.outPaths.length);
				for (int j = 0; j < rp.outPaths.length; j++) {
					vals.add(getValue(rp.outPaths[j].path));
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

	private Proc putVar(String path, Functionality value) throws ContextException {
		putValue(path, value);
		markVar(this, path, value);
		return new Proc(path, value, this);
	}

	private void realizeDependencies(Arg... entries) throws RemoteException,
			ExertionException {
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
					throw new ExertionException(e);
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
	 * set context type as variable
	 * In ServiceContexr#init()
	 * DATA_NODE_TYPE + APS + VAR + APS + type + APS
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
