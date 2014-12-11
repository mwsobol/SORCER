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

package sorcer.service;

import groovy.lang.Closure;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.par.Par;
import sorcer.core.exertion.AltExertion;
import sorcer.core.exertion.LoopExertion;
import sorcer.core.exertion.OptExertion;
import sorcer.core.invoker.GroovyInvoker;
import sorcer.core.invoker.ServiceInvoker;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * A Condition specifies a conditional value in a given service context for its free variables
 * in the form of path/value pairs with paths being guards's parameters.
 * 
 * @see LoopExertion
 * @see OptExertion
 * @see AltExertion

 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
 public class Condition implements Evaluation<Object>, Conditional, Serializable {

	final static public String _closure_ = "_closure_";
	
	private static final long serialVersionUID = -7310117070480410642L;
	 
	protected final static Logger logger = Logger.getLogger(Condition.class
			.getName());

	
	public static String CONDITION_VALUE = "condition/value";
	public static String CONDITION_TARGET = "condition/target";
	
	protected Context<?> conditionalContext;

	protected String evaluationPath;
	
	protected String closureExpression;

	private Closure closure;
	
	protected String[] pars;
	
	private Boolean status = null;
	
	public Condition() {
		// do nothing
	}
	
	public Condition(Boolean status) {
		this.status = status;
	}
	
	public Condition(Context<?> context) {
		conditionalContext = context;
	}
	
	public Condition(Context<?> context, String parPath) {
		evaluationPath = parPath;
		conditionalContext = context;
	}
	
	public Condition(String closure, String... parameters) {
		this.closureExpression = closure;
		this.pars = parameters;
	}
	
	public Condition(Context<?> context, String closure, String... parameters) {
		this.closureExpression = closure;
		conditionalContext = context;
		this.pars = parameters;
	}
	
	/**
	 * The isTrue method is responsible for evaluating the underlying contextual
	 * condition.
	 * 
	 * @return boolean true or false depending on given contexts
	 * @throws ExertionException
	 *             if there is any problem within the isTrue method.
	 * @throws ContextException
	 */
	synchronized public boolean isTrue() throws ContextException {
		// always constant true or false condition		
		if (status instanceof Boolean)
			return status;
	
		Object obj = null;
		Object[] args = null;
		if (closure != null) {
			args = new Object[pars.length];
			for (int i = 0; i < pars.length; i++) {
				args[i] = conditionalContext.getValue(pars[i]);
				if (args[i] instanceof Evaluation)
					try {
						args[i] = ((Evaluation)args[i]).getValue();
					} catch (RemoteException e) {
						throw new ContextException(e);
					}
			}
			obj = closure.call(args);
		} else if (evaluationPath != null && conditionalContext != null) {
			obj = conditionalContext.getValue(evaluationPath);
		} else if (closureExpression !=  null && conditionalContext != null) {
			ArgSet ps = new ArgSet();
			for (String name : pars) {
				ps.add(new Par(name));
			}			
			ServiceInvoker invoker = new GroovyInvoker(closureExpression, ps.toArray());
			try {
				invoker.setScope(conditionalContext);
			} catch (RemoteException re) {
				throw new ContextException(re);
			}
			conditionalContext.putValue(_closure_, invoker);
			closure = (Closure)conditionalContext.getValue(_closure_);
			args = new Object[pars.length];
			for (int i = 0; i < pars.length; i++) {
				try {
					args[i] = ((ServiceContext)conditionalContext).getValueEndsWith(pars[i]);
					if (args[i] instanceof Evaluation)
						args[i] = ((Evaluation)args[i]).getValue();
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
			}
			obj = closure.call(args);
		}
		if (obj instanceof Boolean)
			return (Boolean)obj;
		else if (obj != null)
			return true;
		else 
			return false;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#asis()
	 */
	@Override
	public Object asis() throws EvaluationException, RemoteException {
		return getValue();
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getValue(sorcer.service.Parameter[])
	 */
	@Override
	public Object getValue(Arg... entries) throws EvaluationException,
			RemoteException {
		try {
			return isTrue();
		} catch (ContextException e) {
			throw new EvaluationException(e);
		}
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#substitute(sorcer.service.Parameter[])
	 */
	@Override
	public Evaluation<Object> substitute(Arg... entries)
			throws SetterException, RemoteException {
		conditionalContext.substitute(entries);
		return this;
	}
	
	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	public Closure getClosure() {
		return closure;
	}

	public void setClosure(Closure closure) {
		this.closure = closure;
	}

	public Context<?> getConditionalContext() {
		return conditionalContext;
	}

	public void setConditionalContext(Context conditionaContext) {
		this.conditionalContext = conditionaContext;
	}
	
	public String getClosureExpression() {
		return closureExpression;
	}
	
	static public void cleanupScripts(Exertion exertion) throws ContextException {
		clenupContextScripts(exertion.getContext());
		for (Exertion e : exertion.getExertions()) {
					clenupExertionScripts(e);
					clenupContextScripts((ServiceContext)e.getContext());
		}
	}
	
	static public void clenupContextScripts(Context context) {
		Iterator i = ((ServiceContext) context).entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry entry = (Map.Entry) i.next();
			String path = (String) entry.getKey();
			if (path.equals(_closure_)) {
				i.remove();
			}
			if (entry.getValue() instanceof ServiceInvoker) {
				clenupContextScripts(((ServiceInvoker) entry.getValue())
						.getScope());
			} else if (entry.getValue() instanceof Par
					&& ((ServiceContext) ((Par) entry.getValue()).getScope())
							.containsKey(Condition._closure_)) {
				((ServiceContext) ((Par) entry.getValue()).getScope())
						.remove(Condition._closure_);
			}
		}
	}
	
	public static void clenupExertionScripts(Exertion exertion)
			throws ContextException {
		if (exertion instanceof ConditionalExertion) {
			List<Conditional> cs = ((ConditionalExertion) exertion)
					.getConditions();
			for (Conditional c : cs) {
				((Condition)c).setClosure(null);
			}
			List<Exertion> tl = ((ConditionalExertion) exertion).getTargets();
			for (Exertion vt : tl) {
                if(vt!=null)
                    clenupContextScripts(vt.getContext());
            }
        }
	}

}
