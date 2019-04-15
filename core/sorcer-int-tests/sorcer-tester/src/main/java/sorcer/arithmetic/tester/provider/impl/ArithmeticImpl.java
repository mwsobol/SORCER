package sorcer.arithmetic.tester.provider.impl;

import java.rmi.RemoteException;

import sorcer.arithmetic.tester.provider.Arithmetic;
import sorcer.service.Context;
import sorcer.service.ContextException;

@SuppressWarnings("rawtypes")
public class ArithmeticImpl implements Arithmetic {

//public class ArithmeticImpl implements Arithmetic, Adder {
	
	private Arithmometer arithmometer = new Arithmometer();

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.sorcer.core.provider.Adder#add(sorcer.service.Context)
	 */
	@Override
	public Context add(Context context) throws RemoteException,
			ContextException {
		return arithmometer.add(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * junit.sorcer.core.provider.Subtractor#subtract(sorcer.service.Context)
	 */
	@Override
	public Context subtract(Context context) throws RemoteException,
			ContextException {
		return arithmometer.subtract(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * junit.sorcer.core.provider.Multiplier#multiply(sorcer.service.Context)
	 */
	@Override
	public Context multiply(Context context) throws RemoteException,
			ContextException {
		return arithmometer.multiply(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.sorcer.core.provider.Divider#divide(sorcer.service.Context)
	 */
	@Override
	public Context divide(Context context) throws RemoteException,
			ContextException {
		return arithmometer.divide(context);
	}

}
