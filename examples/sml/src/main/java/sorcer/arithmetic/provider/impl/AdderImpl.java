package sorcer.arithmetic.provider.impl;

import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.MainUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.DoubleSrv;
import sorcer.arithmetic.provider.ui.CalculatorUI;
import sorcer.service.Exerter;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.MonitorException;
import sorcer.serviceui.UIComponentFactory;
import sorcer.serviceui.UIDescriptorFactory;
import sorcer.util.Sorcer;

import java.net.URL;
import java.rmi.RemoteException;

import static sorcer.co.operator.get;
import static sorcer.mo.operator.value;

@SuppressWarnings("rawtypes")
public class AdderImpl implements Adder, DoubleSrv {
    private static final long serialVersionUID = -8098772962245123252L;
	private Arithmometer arithmometer = new Arithmometer();
	private Exerter provider;
	private Logger logger = LoggerFactory.getLogger(Arithmometer.class.getName());
	
	public void init(Exerter provider) {
		this.provider = provider;
	}
	
	public Context add(Context context) throws RemoteException,
			ContextException, MonitorException {
		Context out = arithmometer.add(context);
		logger.info("add result: " + out);
		
//		Logger contextLogger = provider.getContextLogger();
//		contextLogger.info("context logging; add result: " + out);
//		
//		Logger providerLogger =  provider.getProviderLogger();
//		providerLogger.info("provider logging; add result: " + out);
//		try {
//			Thread.sleep(1000 * 5);
//			System.out.println("slept: " + 1000 * 5);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		out.checkpoint();
//		Logger remoteLogger =  provider.getRemoteLogger();
//		remoteLogger.info("remote logging; add result: " + out);
		
		return out;
	}


	public Context add2(Context context) throws RemoteException,
			ContextException, MonitorException {
		Context out = arithmometer.add(context);
		out.putValue("result/eval", (double)value(out, "result/eval") + 100.0);
		logger.info("add2 result: " + out);
		return out;
	}

	public static UIDescriptor getCalculatorDescriptor() {
		UIDescriptor uiDesc = null;
		try {
			uiDesc = UIDescriptorFactory.getUIDescriptor(MainUI.ROLE,
					new UIComponentFactory(new URL[] { new URL(Sorcer
							.getWebsterUrl()
							+ "/calculator-ui.jar") }, CalculatorUI.class
							.getName()));
		} catch (Exception ex) {
			// do nothing
		}
		return uiDesc;
	}
}
