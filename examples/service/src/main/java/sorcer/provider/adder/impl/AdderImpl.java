package sorcer.provider.adder.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.PositionalContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.provider.Provider;
import sorcer.core.provider.ServiceProvider;
import sorcer.provider.adder.Adder;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.ServiceSession;

import java.rmi.RemoteException;
import java.util.List;

@SuppressWarnings("rawtypes")
public class AdderImpl implements Adder {
	public static final String RESULT_PATH = "result/value";
	private ServiceProvider provider;
	private static Logger logger = LoggerFactory.getLogger(AdderImpl.class.getName());
	
	public void init(Provider provider) {
		this.provider = (ServiceProvider)provider;
	}

    @Override
	public Context add(Context context) throws RemoteException, ContextException {
		// get inputs and outputs from the service context
		PositionalContext cxt = (PositionalContext) context;
		List<Double> inputs = cxt.getInValues();
		logger.info("inputs: " + inputs);
		List<String> outpaths = cxt.getOutPaths();
		logger.info("outpaths: " + outpaths);

		// calculate the result
		Double result = 0.0;
		for (Double value : inputs)
			result += value;
		logger.info("result: " + result);
		
		// update the service context
		if (provider != null)
			cxt.putValue("calculated/provider", provider.getProviderName());
		else
			cxt.putValue("calculated/provider", getClass().getName());
		if (((ServiceContext)context).getReturnPath() != null) {
			((ServiceContext)context).setReturnValue(result);
		} else if (outpaths.size() == 1) {
			// put the result in the existing output path
			cxt.putValue(outpaths.get(0), result);
		} else {
			cxt.putValue(RESULT_PATH, result);
		}

		// get a custom provider property
		if (provider != null) {
			try {
				int st = new Integer(provider.getProperty("provider.sleep.time"));
				if (st > 0) {
					Thread.sleep(st);
					logger.info("slept for: " + st);
					cxt.putValue("provider/slept/ms", st);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return cxt;
	}

    @Override
    public Context sum(Context context) throws RemoteException, ContextException {
        ServiceSession ss = provider.getSession(context);
        // add request values
        Context cxt = add(context);
        
        // get previous 'add' value
        Double previous = 0.0;
        if (ss.getAttribute("sum") != null)
            previous = (Double)ss.getAttribute("sum");

        // get 'sum' value
        Double result = (Double)cxt.getReturnValue() + previous;
        
        // save it in the session
        ss.setAttribute("sum", result);
        // set it in the returned context
        ((ServiceContext)cxt).setReturnValue(result);
        return context;
    }

}
