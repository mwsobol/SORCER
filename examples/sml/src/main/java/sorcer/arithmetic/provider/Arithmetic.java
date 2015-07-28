package sorcer.arithmetic.provider;

import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.MonitorException;

import java.rmi.RemoteException;

public interface Arithmetic extends Adder, Subtractor, Multiplier, Divider {

	@SuppressWarnings("rawtypes")
	public Context calculate(Context context) throws RemoteException,
			ContextException, MonitorException;
}
