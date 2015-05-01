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

package sorcer.core.invoker;

import groovy.lang.GroovyShell;
import sorcer.core.context.model.par.Par;
import sorcer.service.*;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Iterator;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GroovyInvoker<T> extends ServiceInvoker<T> {

	private static final long serialVersionUID = -2821704426312928422L;

	private static String defaultName = "gvyIvoker-";

	// counter for unnamed instances
	protected static int count;

	/** expression to be evaluated */
	protected String expression;

	/** The evaluator */
	transient private GroovyShell shell;

	private File scriptFile = null;

	public GroovyInvoker() {
		super();
		this.name = defaultName + count++;
	}

	public GroovyInvoker(String expression) {
		this.name = defaultName + count++;
		this.expression = expression;
	}
	
	public GroovyInvoker(String expression, ArgSet parameters) {
		this(null, expression, parameters);
	}

	public GroovyInvoker(String name, String expression, ArgSet parameters) {
		if (name == null)
			this.name = defaultName + count++;
		else
			this.name = name;
		this.expression = expression;
		this.pars = parameters;
	}

	public GroovyInvoker(String expression, Arg... parameters) {
		this(null, expression, parameters);
	}
	
	public GroovyInvoker(String name, String expression, Arg... parameters) {
		if (name == null)
			this.name = defaultName + count++;
		else
			this.name = name;
		this.expression = expression;
		this.pars =  ArgSet.asSet(parameters);
	}

	public GroovyInvoker(File scriptFile, Par... parameters)
			throws EvaluationException {
		this.scriptFile = scriptFile;
		this.pars = new ArgSet(parameters);
	}

	@Override
	public T getValue(Arg... entries) throws InvocationException,
			RemoteException {
		Object result = null;
		shell = new GroovyShell();
		if (entries != null) {
			for (Arg a : entries)
				try {
					if (a instanceof Evaluation) {
						invokeContext.putValue(a.getName(), ((Evaluation)a).getValue());
					}
				} catch (Exception e) {
					throw new InvocationException(e);
				} 
		}
		try {
			initBindings();
		} catch (ContextException ex) {
			throw new InvocationException(ex);
		}
		try {
			synchronized (shell) {
				if (scriptFile != null) {
					try {
						result = shell.evaluate(scriptFile);
					} catch (IOException e) {
						throw new InvocationException(e);
					}
				} else {
					result = shell.evaluate(expression);
				}
			}
		} catch (RuntimeException e) {
			logger.error("Error Occurred in Groovy Shell: " + e.getMessage());
		}
		return (T) result;
	}

	private void initBindings() throws RemoteException, ContextException {
		logger.info("invokeContext keys: " + invokeContext.keySet() + "\nfor: " + expression);
		if (invokeContext != null) {
			if (pars != null && pars.size() > 0) {
				for (Arg p : pars) {
					Object obj = invokeContext.getValue(p.getName());
					if (obj == null || obj == Context.none) {
						// try extended path
						obj = invokeContext.getValueEndsWith(p.getName());
					}
					if (obj != null && obj != Context.none) {
						((Setter)p).setValue(obj);
					} else if (((Evaluation)p).asis() != null) {
						invokeContext.putValue(p.getName(), ((Evaluation) p).asis());
					}
				}
			}
		}
		Iterator<Arg> i = pars.iterator();
		Object val = null;
		String key = null;
		while (i.hasNext()) {
			Arg entry = i.next();
			val = ((Evaluation)entry).getValue();
			key = (String) entry.getName();
			if (val instanceof Evaluation) {
				val = ((Evaluation) val).getValue();
			}
			shell.setVariable(key, val);
		}
	}

	public void clean() {
		shell = null;
	}

	@Override
	public String toString() {
		return getClass().getName() + ":" + name + ":" + expression;
	}

}
