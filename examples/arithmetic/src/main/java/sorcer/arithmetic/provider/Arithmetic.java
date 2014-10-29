package sorcer.arithmetic.provider;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.MonitorException;

public interface Arithmetic extends Adder, Subtractor, Multiplier, Divider {

	@SuppressWarnings("rawtypes")
	public Context calculate(Context context) throws RemoteException,
			ContextException, MonitorException;
}
