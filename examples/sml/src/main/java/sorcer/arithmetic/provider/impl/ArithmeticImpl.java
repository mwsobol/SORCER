package sorcer.arithmetic.provider.impl;

import sorcer.arithmetic.provider.Arithmetic;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.MonitorException;

import java.rmi.RemoteException;

import static sorcer.eo.operator.value;

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

	@Override
	public Context add2(Context context) throws RemoteException, ContextException, MonitorException {
		Context out = arithmometer.add(context);
		out.putValue("result/eval", (double)value(out, "result/eval") + 100.0);
		return out;
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

	/* (non-Javadoc)
	 * @see sorcer.arithmetic.provider.Arithmetic#calculate(sorcer.service.Context)
	 */
	@Override
	public Context calculate(Context context) throws RemoteException,
			ContextException, MonitorException {
		return arithmometer.calculate(context);
	}

}
