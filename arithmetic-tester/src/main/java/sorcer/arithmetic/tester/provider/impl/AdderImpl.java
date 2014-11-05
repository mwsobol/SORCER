package sorcer.arithmetic.tester.provider.impl;

import java.rmi.RemoteException;

import sorcer.arithmetic.tester.provider.Adder;
import sorcer.service.Context;
import sorcer.service.ContextException;

public class AdderImpl implements Adder {
	private Arithmometer arithmometer = new Arithmometer();

	@SuppressWarnings("rawtypes")
	public Context add(Context context) throws RemoteException,
			ContextException {
		return arithmometer.add(context);
	}

}
