package sorcer.provider.adder.impl;

import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.MainUI;
import sorcer.core.context.PositionalContext;
import sorcer.core.context.ServiceContext;
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
import java.util.List;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
public class AdderImpl implements Adder {
	public static final String RESULT_PATH = "result/value";
	private Provider provider;
	private static Logger logger = Logger.getLogger(AdderImpl.class.getName());
	
	public void init(Provider provider) {
		this.provider = provider;
		try {
			logger = provider.getLogger();
		} catch (RemoteException e) {
			// ignore it, local call
		}
	}
	
	public Context add(Context context) throws RemoteException, ContextException {
		// get inputs and outputs from the service context
		PositionalContext cxt = (PositionalContext) context;
		List<Double> inputs = cxt.getInValues();
		logger.info("inputs: " + inputs);
		List<String> outpaths = cxt.getOutPaths();
		logger.info("outpaths: " + outpaths);

		// calculate the result
		Double result = 0.0;
		for (Double value : inputs)
			result += value;
		logger.info("result: " + result);
		
		// update the service context
		if (provider != null)
			cxt.putValue("calculated/provider", provider.getProviderName());
		else
			cxt.putValue("calculated/provider", getClass().getName());
		if (((ServiceContext)context).getReturnPath() != null) {
			((ServiceContext)context).setReturnValue(result);
		} else if (outpaths.size() == 1) {
			// put the result in the existing output path
			cxt.putValue(outpaths.get(0), result);
		} else {
			cxt.putValue(RESULT_PATH, result);
		}

		// get a custom provider property
		if (provider != null) {
			try {
				int st = new Integer(provider.getProperty("provider.sleep.time"));
				Thread.sleep(st);
				logger.info("slept for: " + st);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return cxt;
	}
	
	public static UIDescriptor getCalculatorDescriptor() {
		String sorcerVersion = System.getProperty("sorcer.version");
		String relativeRepoPath = System.getProperty("relative.repo.path");
		UIDescriptor uiDesc = null;
		try {
			uiDesc = UIDescriptorFactory.getUIDescriptor(MainUI.ROLE,
					new UIComponentFactory(new URL[] { new URL(Sorcer
							.getWebsterUrl()
							+"/"+relativeRepoPath+"adder-"+sorcerVersion+"ui.jar") }, AdderUI.class
							.getName()));
		} catch (Exception ex) {
			// do nothing
		}
		return uiDesc;
	}
}
