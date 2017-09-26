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
import sorcer.core.context.ApplicationDescription;
import sorcer.core.invoker.Activator;
import sorcer.eo.operator;
import sorcer.service.*;
import sorcer.service.modeling.Activation;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.func;


import java.rmi.RemoteException;
import java.util.Iterator;

/**
 * In service-based modeling, a service neuron (for short a neo) is a special kind of
 * function, used in a service model {@link ProcModel} to refer to one of the
 * pieces of data provided as input to other neurons.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({"unchecked", "rawtypes" })
public class Neo extends Function<Double> implements Functionality<Double>, Invocation<Double>,
		Setter, Scopable, Comparable<Double>, func<Double> {

	private static final long serialVersionUID = 1L;

	private static Logger logger = LoggerFactory.getLogger(Neo.class.getName());

	protected double bias;

	protected Activator activator;

	protected ServiceFidelity fidelities;


	public Neo(String name) {
		super(name);
        activator = new Activator(name);
		type = Type.NEURON;
	}

       public Neo(String name, double value) {
        this(name);
        item = value;
    }

    public Neo(String name, operator.Args args) {
        this(name);
        activator.setArgs(args.argSet());
    }

    public Neo(String name, Context<Float> weights, operator.Args args) {
        this(name, args);
        activator.setWeights(weights);
    }

    public Neo(String name, double value, Context<Function> signals) {
        this(name);
        item = value;
        activator.setScope(signals);
    }

	public Neo(String name, Context<Value> signals, Context<Float> weights, operator.Args args) {
		this(name, args);
		activator.setScope(signals);

	}

	public Neo(String name, Context<Value> signals, Context<Float> weights) {
		this(name);
		activator.setScope(signals);

	}

	public Neo(String name, double value, Context<Value> signals, Context<Float> weights) {
        this(name);
		item = value;
        activator.setScope(signals);

    }

	public Neo(String name, Context<Float> weights, Arg... args) {
		this(name, new operator.Args(args));
		activator.setWeights(weights);
	}

	public Neo(String name, ServiceFidelity fidelities) {
		this(name);
		this.fidelities= fidelities;
	}

    /* (non-Javadoc)
	 * @see sorcer.service.Evaluation#substitute(sorcer.co.tuple.Parameter[])
	 */
	@Override
	public void substitute(Arg... parameters) throws SetterException {
		if (parameters == null)
			return;
		for (Arg p : parameters) {
			try {
				if (p instanceof Neo) {
					if (key.equals(((Neo) p).key)) {
						if (((Neo) p).getScope() != null)
							scope.append(((Neo) p).getScope());

					}
				} else if (p instanceof Fidelity && fidelities != null) {
                    fidelities.setSelect(p.getName());
				} else if (p instanceof Context) {
					if (scope == null)
						scope = (Context) p;
					else
						scope.append((Context) p);
				}
			} catch (ContextException e) {
				e.printStackTrace();
				throw new SetterException(e);
			}
		}
	}

	private boolean isFidelityValid(Object fidelity) throws EvaluationException {
		if (fidelity == null || fidelity == Context.none)
			return false;
		if (fidelity instanceof Function) {
			Object obj = null;
			obj = ((Function)fidelity).asis();
			if (obj == null || obj == Context.none) return false;
		}
		 return true;
	}

	public Context getScope() {
		return scope;
	}

	public void setScope(Context scope) {
		if (scope != null) {
            this.scope = scope;
            if (activator != null) {
                activator.setScope(scope);
            }
        }
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Double o) {
		if (o == null)
			throw new NullPointerException();
		if (o instanceof Double)
			return out.compareTo(o);
		else
			return -1;
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
		return item.getClass();
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
	public Double getArg(String varName) throws ArgException {
		try {
			return (Double) scope.getValue(varName);
		} catch (ContextException e) {
			throw new ArgException(e);
		}
	}

    @Override
    public Double getValue(Arg... args) throws EvaluationException, RemoteException {
	    if (activator.getArgs().size() > 0) {
            out = activator.activate(args);
        }
        return out;
    }

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

	/* (non-Javadoc)
	 * @see sorcer.service.Invocation#invoke(sorcer.service.Context, sorcer.service.Arg[])
	 */
	@Override
    public Double invoke(Context context, Arg... args) throws RemoteException,
            InvocationException {
        try {
            if (context != null) {
                if (activator.getScope() == null)
                    activator.setScope(context);
                else {
                    activator.getScope().append(context);
                }
            }
            if (fidelities != null) {
               activator.setFidelities(fidelities);
            } else if (activator.getArgs().size() == 0) {
                return out;
            }
            item = activator.activate(args);
            return out;
        } catch (Exception e) {
            throw new InvocationException(e);
        }
    }

	/* (non-Javadoc)
	 * @see sorcer.core.context.model.Variability#addArgs(ArgSet set)
	 */
	@Override
	public void addArgs(ArgSet set) throws EvaluationException {
		Iterator<Arg> i = set.iterator();
		while (i.hasNext()) {
			Neo procEntry = (Neo)i.next();
			try {
				activator.getScope().putValue(procEntry.getName(), procEntry.asis());
			} catch (Exception e) {
				throw new EvaluationException(e);
			} 
		}
		
	}

	@Override
	public int hashCode() {
		int hash = key.length() + 1;
		return hash * 31 + key.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Neo
				&& ((Neo) object).key.equals(key))
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
	public Double execute(Arg... args) throws MogramException, RemoteException {
		Context cxt = (Context) Arg.selectDomain(args);
		if (cxt != null) {
			scope = cxt;
			return getValue(args);
		} else {
			return getValue(args);
		}
	}

	public ServiceFidelity getFidelities() {
			return fidelities;
	}

	public void setFidelities(ServiceFidelity fidelities) {
			this.fidelities = fidelities;
    }

    @Override
	public Double getPerturbedValue(String varName) throws EvaluationException, RemoteException {
        return (Double)(activator.getScope().get(varName)) + bias;
    }

    @Override
    public double getPerturbation() {
        return bias;
    }
}
