package sorcer.provider.adder.impl;

import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.MainUI;
import sorcer.core.provider.Provider;
import sorcer.provider.adder.Adder;
import sorcer.provider.adder.ui.AdderUI;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.serviceui.UIComponentFactory;
import sorcer.serviceui.UIDescriptorFactory;
import sorcer.util.Sorcer;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
public class AdderImpl implements Adder {
	private Arithmometer arithmometer = new Arithmometer();
	private Provider provider;
	private Logger logger = Logger.getLogger(Arithmometer.class.getName());
	
	public void init(Provider provider) {
		this.provider = provider;
		try {
			logger = provider.getLogger();
		} catch (RemoteException e) {
			// ignore it, local call
		}
	}
	
	public Context add(Context context) throws RemoteException, ContextException {
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
//		out.checkpoint();
//		Logger remoteLogger =  provider.getRemoteLogger();
//		remoteLogger.info("remote logging; add result: " + out);
		
		return out;
	}
	
	public static UIDescriptor getCalculatorDescriptor() {
		UIDescriptor uiDesc = null;
		try {
			uiDesc = UIDescriptorFactory.getUIDescriptor(MainUI.ROLE,
					new UIComponentFactory(new URL[] { new URL(Sorcer
							.getWebsterUrl()
							+ "/calculator-ui.jar") }, AdderUI.class
							.getName()));
		} catch (Exception ex) {
			// do nothing
		}
		return uiDesc;
	}
}
