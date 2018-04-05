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
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.Proc;
import sorcer.service.*;

import java.io.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GroovyInvoker<T> extends ServiceInvoker<T> {

	private static final long serialVersionUID = -2821704426312928422L;

	private static String defaultName = "gvyIvoker-";

	// counter for unnamed instances
	protected static int count;

	private static StringBuilder staticImports;

	/**
	 * expression to be evaluated
	 */
	protected String expression;

	/**
	 * The evaluator
	 */
	transient private GroovyShell shell;

	private File scriptFile = null;

	public GroovyInvoker() {
		super(defaultName + count++);
		if (staticImports == null) {
			staticImports = readStaticImports();
		}
	}

	public GroovyInvoker(String expression) {
		this();
		this.expression = expression;
	}

	public GroovyInvoker(String expression, ArgSet parameters) {
		this(null, expression, parameters);
	}

	public GroovyInvoker(String name, String expression, ArgSet parameters) {
		this();
		if (name != null && name.length() > 0)
			this.name = name;
		this.expression = expression;
		this.args = parameters;
	}

	public GroovyInvoker(String expression, Arg... parameters) {
		this(null, expression, parameters);
	}

	public GroovyInvoker(String name, String expression, Arg... parameters) {
		this(name, expression, new ArgSet(parameters));
	}

	public GroovyInvoker(File scriptFile, Proc... parameters)
			throws EvaluationException {
		this();
		this.scriptFile = scriptFile;
		this.args = new ArgSet(parameters);
	}

	@Override
	public T getValue(Arg... entries) throws InvocationException,
			RemoteException {
		Object result = null;
		shell = new GroovyShell(Thread.currentThread().getContextClassLoader());
		if (entries != null) {
			for (Arg a : entries)
				try {
					if (a instanceof Evaluation) {
						invokeContext.putValue(a.getName(), ((Evaluation) a).getValue());
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
					StringBuilder sb = new StringBuilder(staticImports.toString());
					sb.append(expression);
					logger.debug(sb.toString());
					synchronized (shell) {
						result = shell.evaluate(sb.toString());
					}
				}
			}
//			TODO testing
//			printedEntries(args);
		} catch (Exception e) {
			logger.error("Error Occurred in Groovy Shell: " + e.getMessage());
		}
		return (T) result;
	}

	private void printedEntries(Arg... entries) throws EvaluationException {
		if(expression.equals("_print_")) {
			List<Path> paths = new ArrayList<Path>();
			for (Arg a : entries) {
				if (a instanceof Path) {
					paths.add((Path) a);
				}
			}
			for (Path p : paths)
				try {
					System.out.println("entry: " + p + "="
                            + invokeContext.getValue(p.getName()));
				} catch (ContextException e) {
					throw new EvaluationException(e);
				}
		}
	}

	private void initBindings() throws RemoteException, ContextException {
//		logger.info("invokeContext keys: " + invokeContext.keySet() + "\nfor: " + expression);
		if (invokeContext != null) {
			if (args != null && args.size() > 0) {
				for (Arg p : args) {
					Object obj = invokeContext.getValue(p.getName());
					if (obj == null || obj == Context.none) {
						// try extended path
						obj = ((ServiceContext)invokeContext).getValueEndsWith(p.getName());
					}
					if (obj != null && obj != Context.none) {
						((Setter)p).setValue(obj);
					} else if (((Evaluation)p).asis() != null) {
						invokeContext.putValue(p.getName(), ((Evaluation) p).asis());
					}
				}
			}
		}
		Iterator<Arg> i = args.iterator();
		Object val = null;
		String key = null;
		while (i.hasNext()) {
			Arg entry = i.next();
			val = ((Evaluation)entry).getValue();
			key = entry.getName();
			if (val instanceof Evaluation) {
				val = ((Evaluation) val).getValue();
			}
			shell.setVariable(key, val);
		}
	}

	private StringBuilder readStaticImports() {
		StringBuilder sb = new StringBuilder();
		sb.append("import static sorcer.eo.operator.*;\n")
				.append("import static sorcer.co.operator.*;\n")
				.append("import static sorcer.po.operator.*;\n")
				.append("//import static sorcer.vo.operator.*;\n")
				.append("//import static sorcer.tools.shell.NetworkShell.nshUrl;\n")
				.append("//common SORCER classes\n")
				.append("import sorcer.service.*;\n")
				.append("import sorcer.service.Signature.*;\n")
				.append("import sorcer.core.exertion.*;\n")
				.append("import sorcer.service.Strategy.*;\n")
				.append("import sorcer.service.Strategy.Flow;\n")
				.append("import sorcer.service.Strategy.Access;\n")
				.append("import sorcer.service.Strategy.Provision;\n")
				.append("import sorcer.service.Strategy.Monitor;\n")
				.append("import sorcer.service.Strategy.Wait;\n")
				.append("import sorcer.service.*;\n")
				.append("import sorcer.core.context.model.*;\n")
				.append("//import sorcer.vfe.util.*;\n")
				.append("//import sorcer.vfe.*;\n")
				.append("import java.io.*;")
				.append("\n");

		return sb;
	}

	public void clean() {
		shell = null;
	}

	@Override
	public String toString() {
		return getClass().getName() + ":" + name + ":" + expression;
	}

}
