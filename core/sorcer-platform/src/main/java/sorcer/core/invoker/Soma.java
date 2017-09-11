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

import sorcer.core.context.model.ent.Entry;
import sorcer.service.*;
import sorcer.service.modeling.Variability;

import java.rmi.RemoteException;
import java.util.List;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Soma extends ServiceInvoker<Double> {

	private static final long serialVersionUID = 1L;

	{
		defaultName = "soma-";
	}

	// linear transformation of the input vector
    double bias = 0.0;

	// for step function
    double threshold = 0.0;

    Context<Float> weights;

    boolean rectified = false;

    public Soma() {
        super();
    }

    public Soma(String name) {
        super(name);
    }
	
	@Override
	public Double invoke(Context context, Arg... entries)
			throws RemoteException, InvocationException {
		try {
			return activate(entries);
		} catch (Exception e) {
			throw new InvocationException(e);
		}
	}
	
	@Override
	public Double invoke(Arg... entries) throws InvocationException, RemoteException,
			InvocationException {
        try {
            return activate(entries);
        } catch (EvaluationException e) {
            throw new InvocationException(e);
        }
    }

    public boolean isRectified() {
        return rectified;
    }

    public void setRectified(boolean rectified) {
        this.rectified = rectified;
    }

    public Context<Float> getWeights() {
        return weights;
    }

    public void setWeights(Context<Float> weights) {
        this.weights = weights;
    }

	public Double activate(Arg... entries) throws EvaluationException, RemoteException {
        List<String> names = args.getNames();
        for (Arg arg : entries) {
            if (arg instanceof Entry) {
                if (((Entry) arg).getType() == Variability.Type.THRESHOLD
                        && name.equals(arg.getName())) {
                    threshold = (double) ((Entry) arg).get();
                } else if (((Entry) arg).getType() == Variability.Type.BIAS
                        && name.equals(arg.getName())) {
                    bias = (double) ((Entry) arg).get();
                }
            }
        }
        double sum = 0.0;
        for (String name : args.getNames()) {
            double in = (double) ((Entry)invokeContext.get(name)).getValue();
            double wt = (double) weights.get(name);
            sum = sum + (in * wt);
        }
		sum = sum + bias;

        if (rectified) {
            // if rectified linear soma
            if (sum < 0.0) {
                return 0.0;
            }
        } else if (threshold != 0.0) {
            if (sum > threshold) {
                return 1.0;
            } else {
                return -1.0;
            }
        }
        return sum;
	}
}
