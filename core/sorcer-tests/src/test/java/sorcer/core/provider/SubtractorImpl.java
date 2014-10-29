package junit.sorcer.core.provider;

import java.io.Serializable;
import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public class SubtractorImpl implements Subtractor, Serializable {
	Arithmometer arithmometer = new Arithmometer();

	public Context subtract(Context context) throws RemoteException,
			ContextException {
		return arithmometer.subtract(context);
	}
}
