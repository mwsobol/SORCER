package sorcer.arithmetic.provider.impl;

import sorcer.arithmetic.provider.Divider;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

public class DividerImpl implements Divider {
	Arithmometer arithmometer = new Arithmometer();

	public Context divide(Context context) throws RemoteException,
			ContextException {
		return arithmometer.divide(context);
	}
}
