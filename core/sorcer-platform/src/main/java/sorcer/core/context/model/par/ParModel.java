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
package sorcer.core.context.model.par;

import sorcer.core.context.Contexts;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.EntModel;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.invoker.ServiceInvoker;
import sorcer.service.Scopable;
import sorcer.service.*;
import sorcer.service.modeling.Variability;
import sorcer.util.Response;

import java.rmi.RemoteException;
import java.util.*;

import static sorcer.eo.operator.returnPath;

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
 * The ParModel is an active shared service context as a map of parameters (Pars),
 * parameter name and its argument <name, argument> is the definition of a
 * independent and dependent arguments. Arguments that dependent on other
 * arguments are subroutines (invokers), so that, each time the subroutine is
 * called, its arguments for that call can be assigned to the corresponding
 * parameters of invokers.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({"unchecked", "rawtypes"  })
public class ParModel<T> extends EntModel<T> implements Invocation<T>, Mappable<T>, ParModeling {

    private static final long serialVersionUID = -6932730998474298653L;

	public static ParModel instance(Signature builder) throws SignatureException {
		ParModel model = (ParModel) sorcer.co.operator.instance(builder);
		model.setBuilder(builder);
		return model;
	}

    public ParModel() {
        super();
        name = PAR_MODEL;
        setSubject("par/model", new Date());

    }

    public ParModel(String name) {
        super(name);
    }

	public ParModel(String name, Signature builder) {
		this(name);
		this.builder = builder;
	}

    public ParModel(Context context) throws RemoteException, ContextException {
        super(context);
        name = PAR_MODEL;
        setSubject("par/model", new Date());
    }

    public ParModel(Identifiable... objects) throws RemoteException,
            ContextException {
        this();
        add(objects);
    }

	public T getValue(String path, Arg... entries) throws ContextException {
		try {
			append(entries);
			T val = null;
			if (path != null) {
				val = (T) get(path);
			} else {
				Signature.ReturnPath rp = returnPath(entries);
				if (rp != null)
					val = (T) getReturnValue(rp);
				else
					val = (T) super.getValue(path, entries);
			}

			if ((val instanceof Par) && (((Par) val).asis() instanceof Variability)) {
				bindVar((Variability) ((Par) val).asis());
			} else if (val instanceof Scopable) {
				((Scopable)val).getScope().setScope(this);
			} else if (val instanceof Entry && (((Entry)val).asis() instanceof Scopable)) {
				((Scopable) ((Entry)val).asis()).setScope(this);
			}

			if (val != null && val instanceof Evaluation) {
				return (T) ((Evaluation) val).getValue(entries);
			} else if (path == null && val == null && runtime.getResponsePaths() != null) {
				if (runtime.getResponsePaths().size() == 1)
					return (T) getValue(runtime.getResponsePaths().get(0), entries);
				else
					return (T) getResponse();
			} else {
				return (T) val;
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
	public T getValue(Arg... entries) throws EvaluationException {
		try {
			return (T) getValue(null, entries);
		} catch (ContextException e) {
			throw new EvaluationException(e);
		}
	}
	
	@Override
	public T putValue(String path, Object value) throws ContextException {
		isChanged = true;
		Object obj = get(path);
		try {
			if (obj instanceof Par) {
				((Par) obj).setValue(value);
				return (T) value;
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
			return super.putValue(path, value);
		} catch (RemoteException e) {
			throw new ContextException(e);
		}
	}

	public Par getPar(String name) throws ContextException {
		Object obj = get(name);
		if (obj instanceof Par)
			return (Par) obj;
		else
			return new Par(name, asis(name), this);
	}
	
	public Variability bindVar(Variability var) throws EvaluationException,
			ContextException, RemoteException {
		ArgSet args = var.getArgs();
		for (Arg v : args)
			if (get(v.getName()) != null) {
				((Variability) v).setValue(getValue(v.getName()));
			}
		return var;
	}
	
	public ParModel add(List<Identifiable> objects) throws EvaluationException,
			RemoteException, ContextException {
		Identifiable[] objs = new Identifiable[objects.size()];
		objects.toArray(objs);
		add(objs);
		return this;
	}

	public ParModel append(Arg... objects) throws ContextException,
			RemoteException {
		Par p = null;
		for (Arg obj : objects) {
			if (obj instanceof Par) {
				p = (Par) obj;
				addPar(p);
			} else if (obj instanceof Entry) {
				putValue((String) ((Entry) obj).key(),
						((Entry) obj).value());
			} else if (obj instanceof Identifiable) {
				String pn = ((Identifiable) obj).getName();
				p = new Par(pn, obj, new ParModel(pn).append(this));
			}

			if (p != null)
				appendPar(p);
		}
		isChanged = true;
		return this;
	}
	
	public ParModel add(Identifiable... objects) throws ContextException,
			RemoteException {
		Par p = null;
		for (Identifiable obj : objects) {
			if (obj instanceof Par) {
				p = (Par) obj;
			} else if (obj instanceof Entry) {
				putValue((String) ((Entry) obj).key(), obj);
			} else {
				String pn = obj.getName();
				putValue(pn, obj);
			}
			
			if (p != null)
				addPar(p);
		}
		isChanged = true;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.service.Invocation#invoke(sorcer.service.Context,
	 * sorcer.service.Args[])
	 */
	@Override
	public T invoke(Context context, Arg... entries) throws RemoteException,
			InvocationException {
		Object result = null;
		try {
			if (context != null) {
				Signature.ReturnPath rp = ((ServiceContext)context).getReturnPath();
				this.append(context);
				// check for multiple responses of this model
				if (rp != null && rp.outPaths.length > 0) {
					Object val = null;
					if (rp.outPaths.length == 1)
						val = getValue(rp.outPaths[0]);
					else {
						List vals = new ArrayList(rp.outPaths.length);
						for (int j = 0; j < rp.outPaths.length; j++)   {
							vals.add(getValue(rp.outPaths[j]));
						}
						val = new Response(Arrays.asList(rp.outPaths), vals);
					}
					((ServiceContext)context).setFinalized(true);
					return (T) val;
				}
				if (((ServiceContext) context).getExecPath() != null) {
					Object o = get(((ServiceContext) context).getExecPath()
							.path());
					if (o instanceof Par) {
						if (o instanceof Agent) {
							if (((Agent) o).getScope() == null)
								((Agent) o).setScope(this);
							else
								((Agent) o).getScope().append(this);
							result = ((Agent) o).getValue(entries);
						} else {
							Object i = ((Par) get(((ServiceContext) context)
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
			return (T) result;
		} catch (ContextException e) {
			throw new InvocationException(e);
		}
	}

	private Object getReturnValue(Signature.ReturnPath rp) throws ContextException {
		Object val = null;
		// check for multiple responses of this model
		if (rp != null && rp.outPaths.length > 0) {
			if (rp.outPaths.length == 1)
				val = getValue(rp.outPaths[0]);
			else {
				List vals = new ArrayList(rp.outPaths.length);
				for (int j = 0; j < rp.outPaths.length; j++) {
					vals.add(getValue(rp.outPaths[j]));
				}
				val = new Response(Arrays.asList(rp.outPaths), vals);
			}
		} else if (rp != null && rp.path != null) {
			val = getValue(rp.path);
		}
		return val;
	}

	public boolean isContextChanged() {
		return isChanged;
	}

	public void setContextChanged(boolean contextChanged) {
		this.isChanged = contextChanged;
	}
	
	public Variability getVar(String name) throws ContextException {
		String key;
		Object val = null;
		Iterator e = keyIterator();
		while (e.hasNext()) {
			key = (String) e.next();
			val = getValue(key);
			if (val instanceof Variability) {
				if (((Variability) val).getName().equals(name))
					return (Variability) val;
			}
		}
		throw new ContextException("No such variability in context: " + name);
	}
	
	private Par putVar(String path, Variability value) throws ContextException {
		putValue(path, value);
		markVar(this, path, value);
		return new Par(path, value, this);
	}
	
	/**
	 * Returns an enumeration of all path marking variable nodes.
	 * 
	 * @return enumeration of marked variable nodes.
	 * @throws ContextException
	 */
	public Enumeration getVarPaths(Variability var) throws ContextException {
		String assoc = VAR_NODE_TYPE + APS + var.getName() + APS + var.getType();
		String[] paths = Contexts.getMarkedPaths(this, assoc);
		Vector outpaths = new Vector();
		if (paths != null)
			for (int i = 0; i < paths.length; i++)
				outpaths.add(paths[i]);

		return outpaths.elements();

	}
	
	public static Variability[] getMarkedVariables(Context sc,
			String association) throws ContextException {
		String[] paths = Contexts.getMarkedPaths(sc, association);
		java.util.Set nodes = new HashSet();
		Object obj = null;
		for (int i = 0; i < paths.length; i++) {
			obj = sc.getValue(paths[i]);
			if (obj != null && obj instanceof Variability)
				nodes.add(obj);
		}
		Variability[] nodeArray = new Variability[nodes.size()];
		nodes.toArray(nodeArray);
		return nodeArray;
	}
	
	/**
	 * set context type as variable
	 * In ServiceContexr#init()
	 * DATA_NODE_TYPE + APS + VAR + APS + type + APS
	 */
	public static Context markVar(Context cntxt, String path, Variability var)
			throws ContextException {
		return cntxt.mark(path, Context.VAR_NODE_TYPE + APS
				+ var.getName() + APS + var.getType());
	}
	
	public Context<T> appendNew(Context<T> context)
			throws ContextException, RemoteException {
		ServiceContext cxt = (ServiceContext) context;
		Iterator<Map.Entry<String, Object>> i = cxt.entryIterator();
		while (i.hasNext()) {
			Map.Entry<String, Object> e = i.next();
			if (!containsKey(e.getKey()) && e.getKey().equals("script")) {
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
