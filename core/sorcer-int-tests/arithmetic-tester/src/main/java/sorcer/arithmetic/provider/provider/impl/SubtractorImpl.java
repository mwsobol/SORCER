package sorcer.arithmetic.tester.provider.impl;

import java.rmi.RemoteException;

import sorcer.arithmetic.tester.provider.Subtractor;
import sorcer.service.Context;
import sorcer.service.ContextException;

@SuppressWarnings("rawtypes")
public class SubtractorImpl implements Subtractor {
	Arithmometer arithmometer = new Arithmometer();

	public Context subtract(Context context) throws RemoteException,
			ContextException {
		return arithmometer.subtract(context);
	}
}
