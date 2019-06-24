package sorcer.arithmetic.provider.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.arithmetic.provider.Incrementer;
import sorcer.service.Exerter;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

@SuppressWarnings("rawtypes")
public class IncrementerImpl implements Incrementer {
    private static final long serialVersionUID = 1;
	private Exerter provider;
	private Logger logger = LoggerFactory.getLogger(IncrementerImpl.class.getName());
	
	public void init(Exerter provider) {
		this.provider = provider;
	}

	Double value = 0.0;

	public IncrementerImpl() {
	}

	public IncrementerImpl(Double base) {
		value = base;
	}

	public Context increment(Context context) throws RemoteException, ContextException {
		Double incremnet = (Double)context.getValue("by");
		value = value + incremnet;
		context.putValue("eval", value);
		context.setReturnValue(value);
		return context;
	}

}
