package sorcer.arithmetic.provider.impl;

import sorcer.arithmetic.provider.Averager;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

public class AveragerImpl implements Averager {

	private Arithmometer arithmometer = new Arithmometer();

	@SuppressWarnings("rawtypes")
	public Context average(Context context) throws RemoteException,
			ContextException {
		return arithmometer.average(context);
	}

}
