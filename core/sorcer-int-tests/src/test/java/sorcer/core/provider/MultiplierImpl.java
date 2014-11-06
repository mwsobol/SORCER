package junit.sorcer.core.provider;

import java.io.Serializable;
import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public class MultiplierImpl implements Multiplier, Serializable {
	Arithmometer arithmometer = new Arithmometer();

	public Context multiply(Context context) throws RemoteException,
			ContextException {
		return arithmometer.multiply(context); 
	}
}
