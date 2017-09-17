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
import sorcer.core.context.ModelStrategy;
import sorcer.core.context.PositionalContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.service.*;
import sorcer.service.Domain;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.Functionality;
import sorcer.util.Response;
import sorcer.service.Signature.ReturnPath;

import java.rmi.RemoteException;
import java.util.*;

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
 * The ProcModel is an active shared service context as a map of parameters (Pars),
 * parameter name and its argument <name, argument> is the definition of a
 * independent and dependent arguments. Arguments that dependent on other
 * arguments are subroutines (invokers), so that, each time the subroutine is
 * called, its arguments for that call can be assigned to the corresponding
 * parameters of invokers.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({"unchecked", "rawtypes"  })
public class ProcModel extends PositionalContext<Object> implements Model, Invocation<Object>,
		Mappable<Object>, Contexter<Object> {

    private static final long serialVersionUID = -6932730998474298653L;

	public static ProcModel instance(Signature builder) throws SignatureException {
		ProcModel model = (ProcModel) sorcer.co.operator.instance(builder);
		model.setBuilder(builder);
		return model;
	}

    public ProcModel() {
		super();
		name = PROC_MODEL;
		setSubject("proc/model", new Date());
		isRevaluable = true;
	}

    public ProcModel(String name) {
        super(name);
		isRevaluable = true;
	}

	public ProcModel(String name, Signature builder) {
		this(name);
		this.builder = builder;
	}

    public ProcModel(Context context) throws RemoteException, ContextException {
        super(context);
        name = PROC_MODEL;
        setSubject("proc/model", new Date());
		isRevaluable = true;
	}

    public ProcModel(Identifiable... objects) throws RemoteException,
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
				else if (((ModelStrategy)mogramStrategy).getResponsePaths() != null
						&& ((ModelStrategy)mogramStrategy).getResponsePaths().size() == 1) {
					val = asis(((ModelStrategy)mogramStrategy).getResponsePaths().get(0).getName());
				} else {
					val = super.getValue(path, args);
				}
			}

			if ((val instanceof Proc) && (((Proc) val).asis() instanceof Function)) {
				bindEntry((Function) ((Proc)val).asis());
			} else if (val instanceof Scopable && ((Scopable)val).getScope() != null) {
				((Scopable)((Scopable)val).getScope()).setScope(this);
			} else if (val instanceof Function && (((Function)val).asis() instanceof Scopable)) {
				((Scopable) ((Function)val).asis()).setScope(this);
			}

			if (val != null && val instanceof Evaluation) {
				return ((Evaluation) val).getValue(args);
			}   if (val instanceof ServiceFidelity) {
				return new Function(path, val).getValue(args);
			} else if (path == null && val == null
					&& ((ModelStrategy)mogramStrategy).getResponsePaths() != null) {
				if (((ModelStrategy)mogramStrategy).getResponsePaths().size() == 1)
					return getValue(((ModelStrategy)mogramStrategy).getResponsePaths().get(0).getName(), args);
				else
					return getResponse();
			} else {
				if (val == null && scope != null && scope != this) {
					Object o = scope.getValue(path);
					if (o != Context.none && o != null)
						return o;
					else
						return ((ServiceContext)scope).getSoftValue(path);
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
	 * @see sorcer.service.Evaluation#getValue(sorcer.core.context.Path.Entry[])
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

	public Proc getPar(String name) throws ContextException {
		Object obj = get(name);
		if (obj instanceof Proc)
			return (Proc) obj;
		else
			return new Proc(name, asis(name), this);
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
	
	public ProcModel add(List<Identifiable> objects) throws RemoteException, ContextException {
		Identifiable[] objs = new Identifiable[objects.size()];
		objects.toArray(objs);
		add(objs);
		return this;
	}

	public ProcModel append(Arg... objects) throws ContextException,
			RemoteException {
		Proc p = null;
		boolean changed = false;
		for (Arg obj : objects) {
			if (obj instanceof Proc) {
				p = (Proc) obj;
			} else if (obj instanceof Entry) {
				putValue((String) ((Entry) obj).key(),
						((Entry) obj).get());
			} else if (obj instanceof Identifiable) {
				String pn = obj.getName();
				p = new Proc(pn, obj, new ProcModel(pn).append(this));
			}

			if (p != null) {
				appendPar(p);
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
			} else if (obj instanceof Function) {
				putValue(pn, ((Function)obj).asis());
				if (((Function)obj).annotation() != null) {
					mark(obj.getName(), ((Function)obj).annotation().toString());
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
			if (val instanceof Entry && ((Entry)val).getItem() instanceof Evaluation) {
				((Entry) val).isValid(false);
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
						val = new Response(Path.getPathList(rp.outPaths), vals);
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
							result = ((Agent) o).getValue(entries);
						} else {
							Object i = ((Proc) get(((ServiceContext) context)
									.getExecPath().path())).asis();
							if (i instanceof ServiceInvoker) {
								result = ((ServiceInvoker) i).invoke(entries);
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
				val = new Response(Path.getPathList(rp.outPaths), vals);
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
	public Function entry(String path) {
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
						((Evaluation) depender).getValue(entries);
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
			obj = sc.getValue(paths[i]);
			if (obj != null && obj instanceof Functionality)
				nodes.add(obj);
		}
		Functionality[] nodeArray = new Functionality[nodes.size()];
		nodes.toArray(nodeArray);
		return nodeArray;
	}
	
	/**
	 * setValue context type as variable
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
