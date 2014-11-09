package sorcer.arithmetic.tester.provider.impl;

import java.rmi.RemoteException;

import sorcer.arithmetic.tester.provider.Divider;
import sorcer.service.Context;
import sorcer.service.ContextException;

public class DividerImpl implements Divider {
	Arithmometer arithmometer = new Arithmometer();

	@SuppressWarnings("rawtypes")
	public Context divide(Context context) throws RemoteException,
			ContextException {
		return arithmometer.divide(context);
	}
}
