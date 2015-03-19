package sorcer.arithmetic.tester.provider.impl;

import sorcer.arithmetic.tester.provider.Adder;
import sorcer.core.provider.Provider;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;
import java.util.logging.Logger;

public class AdderImpl implements Adder {
	private Arithmometer arithmometer = new Arithmometer();

	private Logger logger = Logger.getLogger(AdderImpl.class.getName());

	// a refrence to a provider running this service bean
	private Provider provider;
	
	public void init(Provider provider) {
		this.provider = provider;
		try {
			logger = provider.getLogger();
		} catch (RemoteException e) {
			// ignore it, local call
		}
	}

	@SuppressWarnings("rawtypes")
	public Context add(Context context) throws RemoteException,
			ContextException {
		return arithmometer.add(context);
	}

}
