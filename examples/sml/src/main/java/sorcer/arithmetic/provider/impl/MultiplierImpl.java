package sorcer.arithmetic.provider.impl;

import sorcer.arithmetic.provider.Multiplier;
import sorcer.service.Context;
import sorcer.service.ContextException;

import java.rmi.RemoteException;

public class MultiplierImpl implements Multiplier {
	Arithmometer arithmometer = new Arithmometer();

	public Context multiply(Context context) throws RemoteException,
			ContextException {
		return arithmometer.multiply(context); 
	}

}
