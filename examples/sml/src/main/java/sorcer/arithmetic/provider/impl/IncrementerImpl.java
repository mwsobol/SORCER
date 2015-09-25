package sorcer.arithmetic.provider.impl;

import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.MainUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Incrementer;
import sorcer.arithmetic.provider.ui.CalculatorUI;
import sorcer.core.provider.Provider;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.MonitorException;
import sorcer.serviceui.UIComponentFactory;
import sorcer.serviceui.UIDescriptorFactory;
import sorcer.util.Sorcer;

import java.net.URL;
import java.rmi.RemoteException;

@SuppressWarnings("rawtypes")
public class IncrementerImpl implements Incrementer {
    private static final long serialVersionUID = 1;
	private Provider provider;
	private Logger logger = LoggerFactory.getLogger(IncrementerImpl.class.getName());
	
	public void init(Provider provider) {
		this.provider = provider;
	}

	Double value = 0.0;

	public IncrementerImpl() {
	}

	public IncrementerImpl(Double base) {
		value = base;
	}

	public Context increment(Context context) throws RemoteException, ContextException {
		Double incremnet = (Double)context.getValue("by");
		value = value + incremnet;
		context.putValue("value", value);
		context.setReturnValue(value);
		return context;
	}

}
