package junit.sorcer.core.provider;

import java.io.Serializable;
import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public class AveragerImpl implements Averager, Serializable {

	private Arithmometer arithmometer = new Arithmometer();

	@SuppressWarnings("rawtypes")
	public Context average(Context context) throws RemoteException,
			ContextException {
		return arithmometer.average(context);
	}

}
