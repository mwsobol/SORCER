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
import net.jini.core.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.par.Par;
import sorcer.core.exertion.AltMogram;
import sorcer.core.exertion.LoopMogram;
import sorcer.core.exertion.OptMogram;
import sorcer.core.invoker.GroovyInvoker;
import sorcer.core.invoker.ServiceInvoker;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * A Condition specifies a conditional eval in a given service context for its free variables
 * in the form of path/eval pairs with paths being guard parameters of a closure expression.
 * 
 * @see LoopMogram
 * @see OptMogram
 * @see AltMogram

 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
 public class Condition implements Evaluation<Object>, Conditional, Serializable {

	final static public String _closure_ = "_closure_";

	private static final long serialVersionUID = -7310117070480410642L;

	protected final static Logger logger = LoggerFactory.getLogger(Condition.class
			.getName());

	public static String CONDITION_VALUE = "condition/eval";
	public static String CONDITION_TARGET = "condition/target";

	private String name;

	protected Context<?> conditionalContext;

	protected String evaluationPath;

	protected String closureExpression;

	transient protected ConditionCallable lambda;

	transient private Closure closure;

	protected String[] pars;

	private Boolean status = null;

	public Condition() {
		// do nothing
	}

	public Condition(ConditionCallable lambda) {
		this.lambda = lambda;
	}

	public Condition(Closure closure) {
		this.closure = closure;
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

	public Condition(ConditionCallable closure, String... parameters) {
		this.lambda = closure;
		this.pars = parameters;
	}

	public Condition(Context<?> context, ConditionCallable closure, String... parameters) {
		this.lambda = closure;
		conditionalContext = context;
		this.pars = parameters;
	}

	/**
	 * The isTrue method is responsible for evaluating the underlying contextual
	 * condition.
	 *
	 * @return boolean true or false depending on given contexts
	 * @throws ExertionException if there is any problem within the isTrue method.
	 * @throws ContextException
	 */
	synchronized public boolean isTrue() throws ContextException {
		// always constant true or false condition		
		if (status instanceof Boolean)
			return status;

		Object obj = null;
		Object[] args = null;
		try {
			if (lambda != null) {
				return evaluateLambda(lambda);
			} else if (closure != null) {
				if (closureExpression != null) {
					// old version for textual closure in a conditon
					obj = evaluateTextualClosure(closure);
				} else {
					return (Boolean) closure.call(conditionalContext);
				}
			} else if (evaluationPath != null && conditionalContext != null) {
				obj = conditionalContext.getValue(evaluationPath);
			} else if (closureExpression != null && conditionalContext != null) {
				ArgSet ps = new ArgSet();
				for (String name : pars) {
					ps.add(new Par(name));
				}
				ServiceInvoker invoker = new GroovyInvoker(closureExpression, ps.toArray());
				invoker.setScope(conditionalContext);
				conditionalContext.putValue(_closure_, invoker);
				closure = (Closure) conditionalContext.getValue(_closure_);
				args = new Object[pars.length];
				for (int i = 0; i < pars.length; i++) {
					args[i] = ((ServiceContext) conditionalContext).getValueEndsWith(pars[i]);
					if (args[i] instanceof Evaluation)
						args[i] = ((Evaluation) args[i]).getValue();
				}
				obj = closure.call(args);
			}
		} catch (Exception e) {
			status = false;
			throw new ContextException(e);
		}

		if (obj instanceof Boolean)
			return (Boolean) obj;
		else if (obj != null)
			return true;
		else
			return false;
	}

	private boolean evaluateLambda(ConditionCallable lambda) throws MogramException {
		return lambda.call(conditionalContext);
	}

	private Object evaluateTextualClosure(Closure closure) throws ContextException {
		Object obj = null;
		Object[] args = null;
		args = new Object[pars.length];
		for (int i = 0; i < pars.length; i++) {
			args[i] = conditionalContext.getValue(pars[i]);
			if (args[i] instanceof Evaluation)
				try {
					args[i] = ((Evaluation) args[i]).getValue();
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
		}
		obj =  closure.call(args);
		if (obj instanceof Boolean)
			return obj;
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

    @Override
    public void substitute(Arg... entries)
			throws SetterException {
        ((ServiceContext)conditionalContext).substitute(entries);
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

	public ConditionCallable getLambda() {
		return lambda;
	}

	public void setLambda(ConditionCallable lambda) {
		this.lambda = lambda;
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
		if (exertion == null)
			return;
		clenupContextScripts(exertion.getContext());
		for (Mogram e : exertion.getMograms()) {
			if (e instanceof Exertion) {
				clenupContextScripts(((Exertion) e).getContext());
				clenupExertionScripts((Exertion) e);
			}
		}
	}

	static public void clenupContextScripts(Context context) {
		context.remove(Condition._closure_);
		Iterator i = ((ServiceContext) context).entryIterator();
		while (i.hasNext()) {
			Map.Entry entry = (Map.Entry) i.next();
			// now check entries
			if (entry.getValue() instanceof ServiceInvoker) {
				clenupContextScripts(((ServiceInvoker) entry.getValue())
						.getScope());
			} else if (entry.getValue() instanceof Par) {
				Context cxt =  ((Par) entry.getValue()).getScope();
				if (cxt != null) cxt.remove(Condition._closure_);
				cxt = ((Par)entry.getValue()).getScope();
				if (cxt != null) cxt.remove(Condition._closure_);
			} else if (entry.getValue() instanceof ServiceContext) {
				ServiceContext cxt = (ServiceContext)entry.getValue();
				if (cxt != null) cxt.remove(Condition._closure_);

				cxt = (ServiceContext) ((ServiceContext) entry.getValue()).getScope();
				if (cxt != null) cxt.remove(Condition._closure_);
			}
		}
	}

	public static void clenupExertionScripts(Exertion exertion)
			throws ContextException {
		if (exertion instanceof ConditionalMogram) {
			List<Conditional> cs = ((ConditionalMogram) exertion)
					.getConditions();
			for (Conditional c : cs) {
				((Condition) c).setClosure(null);
			}
			List<Mogram> tl = ((ConditionalMogram) exertion).getTargets();
			for (Mogram vt : tl) {
				if (vt != null && vt instanceof Exertion)
					clenupContextScripts(((Exertion)vt).getContext());
			}
		}
	}

	@Override
	public Object exec(Arg... args) throws MogramException, RemoteException, TransactionException {
		Context cxt = Arg.getContext(args);
		if (cxt != null) {
			conditionalContext = cxt;
			return isTrue();
		}
		return null;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
}
