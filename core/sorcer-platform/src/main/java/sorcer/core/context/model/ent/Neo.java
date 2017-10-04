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
import sorcer.core.context.ServiceContext;
import sorcer.core.invoker.Soma;
import sorcer.eo.operator;
import sorcer.service.*;
import sorcer.service.modeling.Variability;


import java.net.URL;
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
public class Neo extends Entry<Double> implements Variability<Double>, Invocation<Double>, Setter, Scopable, Comparable<Double>{

	private static final long serialVersionUID = 1L;

	private static Logger logger = LoggerFactory.getLogger(Neo.class.getName());

	protected double bias;

	protected Soma soma;

	protected ServiceFidelity<NeoFidelity> fidelities;


	public Neo(String name) {
		super(name);
        soma = new Soma(name);
		type = Type.NEURON;
	}

       public Neo(String name, double value) {
        this(name);
        _2 = value;
    }

    public Neo(String name, operator.Args args) {
        this(name);
        soma.setArgs(args.argSet());
    }

    public Neo(String name, operator.Args args, Context<Float> weights) {
        this(name, args);
        soma.setWeights(weights);
    }

    public Neo(String name, double value, Context<Entry> signals) {
        this(name);
        _2 = value;
        soma.setScope(signals);
    }

    public Neo(String name, double value, Context<Entry> signals, Context<Float> weights) {
        this(name);
        _2 = value;
        soma.setScope(signals);

    }

	public Neo(String name, ServiceFidelity<NeoFidelity> fidelities) {
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
					if (_1.equals(((Neo) p)._1)) {
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
		if (fidelity instanceof Entry) {
			Object obj = null;
			try {
				obj = ((Entry)fidelity).asis();
			} catch (RemoteException e) {
				throw new EvaluationException(e);
			}
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
            if (soma != null) {
                soma.setScope(scope);
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
			return _2.compareTo(o);
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

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getDescription()
	 */
	@Override
	public ApplicationDescription getDescription() {
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getValueType()
	 */
	@Override
	public Class getValueType() {
		return _2.getClass();
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
	    if (soma.getArgs().size() > 0) {
            _2 = soma.activate(args);
        }
        return _2;
    }

	@Override
	public boolean isValueCurrent() {
		return false;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#valueChanged(java.lang.Object)
	 */
	@Override
	public void valueChanged(Object obj) throws EvaluationException,
			RemoteException {		
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
                if (soma.getScope() == null)
                    soma.setScope(context);
                else {
                    soma.getScope().append(context);
                }
            }
            if (fidelities != null) {
               soma.setFidelities(fidelities);
            } else if (soma.getArgs().size() == 0) {
                return _2;
            }
            _2 = soma.activate(args);
            return _2;
        } catch (Exception e) {
            throw new InvocationException(e);
        }
    }

	/* (non-Javadoc)
	 * @see sorcer.core.context.model.Variability#addArgs(ArgSet setValue)
	 */
	@Override
	public void addArgs(ArgSet set) throws EvaluationException {
		Iterator<Arg> i = set.iterator();
		while (i.hasNext()) {
			Neo procEntry = (Neo)i.next();
			try {
				soma.getScope().putValue(procEntry.getName(), procEntry.asis());
			} catch (Exception e) {
				throw new EvaluationException(e);
			} 
		}
		
	}

	@Override
	public int hashCode() {
		int hash = _1.length() + 1;
		return hash = hash * 31 + _1.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Neo
				&& ((Neo) object)._1.equals(_1))
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
	public boolean isReactive() {
		return true;
	}

	@Override
	public Double exec(Arg... args) throws MogramException, RemoteException {
		Context cxt = (Context) Arg.getServiceModel(args);
		if (cxt != null) {
			scope = cxt;
			return getValue(args);
		} else {
			return getValue(args);
		}
	}

	public ServiceFidelity<NeoFidelity> getFidelities() {
			return fidelities;
	}

	public void setFidelities(ServiceFidelity<NeoFidelity> fidelities) {
			this.fidelities = fidelities;
    }

    @Override
	public Double getPerturbedValue(String varName) throws EvaluationException, RemoteException {
        return (Double)(soma.getScope().get(varName)) + bias;
    }

    @Override
    public double getPerturbation() {
        return bias;
    }
}
