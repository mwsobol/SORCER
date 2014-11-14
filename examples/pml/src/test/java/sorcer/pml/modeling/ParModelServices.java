package sorcer.pml.modeling;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.model.par.ParModel;
import sorcer.pml.model.ParModeler;
import sorcer.service.*;
import sorcer.util.Sorcer;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.ent;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.invoke;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/eol")
public class ParModelServices {
	private final static Logger logger = Logger.getLogger(ParModelServices.class
			.getName());

	static {
		ServiceExertion.debug = true;
//		URL.setURLStreamHandlerFactory(new SdbURLStreamHandlerFactory());
		System.setProperty("java.util.logging.config.file",
				Sorcer.getHome() + "/configs/sorcer.logging");
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "ju-invoker-beans.jar" });
	}
	
	@Test
	public void parModelerTest() throws RemoteException, ContextException,
			ExertionException, SignatureException {
		ParModel pm = ParModeler.getParModel();
		logger.info("result: " + invoke(pm, "expr"));
		assertEquals(invoke(pm, "expr"), 60.0);
	}
	
	@Test
	public void parObjectModelServiceTest() throws RemoteException, ContextException, ExertionException, SignatureException {
		ParModel pm = ParModeler.getParModel();
		Task pmt = task(sig("invoke", pm), 
				context(ent("par", "expr"), result("invoke/result")));
		
		logger.info("result: " + value(pmt));
		assertEquals(value(pmt), 60.0);
		
		logger.info("result: " + exert(pmt));
		assertEquals(get(exert(pmt), "invoke/result"), 60.0);
	}

	@Test
	public void parNetModelServiceTest() throws RemoteException, ContextException, 
			ExertionException, SignatureException {
		// the provider in ex6/bin parmodel-prv-run.xml
		Task pmt = task(sig("invoke", Invocation.class, prvName("ParModel Service")),
				context(ent("par", "expr"), result("invoke/result")));
		
//		logger.info("result: " + value(pmt));
		assertEquals(value(pmt), 60.0);
		
//		logger.info("result: " + exert(pmt));
		assertEquals(get(exert(pmt), "invoke/result"), 60.0);
	}
	
}
