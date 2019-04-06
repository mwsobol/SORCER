package sorcer.arithmetic.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.MonitorException;

import java.rmi.RemoteException;

public interface Adder {

	public Context add(Context context) throws RemoteException,
			ContextException, MonitorException;

}
