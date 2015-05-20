package sorcer.arithmetic.provider.impl;

import sorcer.arithmetic.provider.Subtractor;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

public class SubtractorImpl implements Subtractor {
    private static final long serialVersionUID = -80636936252623513L;
	Arithmometer arithmometer = new Arithmometer();

	public Context subtract(Context context) throws RemoteException,
			ContextException {
		return arithmometer.subtract(context);
	}
}
