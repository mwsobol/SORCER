package junit.sorcer.core.provider;

import java.io.Serializable;
import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public class AdderImpl implements Adder, Serializable {
	private Arithmometer arithmometer = new Arithmometer();

	public Context add(Context context) throws RemoteException,
			ContextException {
		return arithmometer.add(context);
	}

}
