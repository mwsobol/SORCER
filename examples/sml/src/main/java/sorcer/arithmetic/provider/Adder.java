package sorcer.arithmetic.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.MonitorException;

import java.rmi.RemoteException;

@SuppressWarnings("rawtypes")
public interface Adder {

	public Context add(Context context) throws RemoteException,
			ContextException, MonitorException;

	public Context add2(Context context) throws RemoteException,
			ContextException, MonitorException;
}
