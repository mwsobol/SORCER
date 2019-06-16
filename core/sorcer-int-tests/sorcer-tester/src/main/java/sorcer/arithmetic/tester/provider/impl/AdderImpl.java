package sorcer.arithmetic.tester.provider.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.service.Provider;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

public class AdderImpl implements Adder {
	private Arithmometer arithmometer = new Arithmometer();

	private Logger logger = LoggerFactory.getLogger(AdderImpl.class.getName());

	// a refrence to a provider running this service bean
	private Provider provider;
	
	public void init(Provider provider) {
		this.provider = provider;
	}

	@SuppressWarnings("rawtypes")
	public Context add(Context context) throws RemoteException,
			ContextException {
		Context out = arithmometer.add(context);
		out.checkpoint();
		return out;
	}

}
