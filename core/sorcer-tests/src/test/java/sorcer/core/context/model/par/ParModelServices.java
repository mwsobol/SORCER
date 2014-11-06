package junit.sorcer.core.context.model.par;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.entry;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.exert;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.result;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.task;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.agent;
import static sorcer.po.operator.invoke;
import static sorcer.po.operator.invoker;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.SecurityManager;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import net.jini.core.transaction.TransactionException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import sorcer.core.context.model.par.ParModel;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.ExertionException;
import sorcer.service.Invocation;
import sorcer.service.ServiceExertion;
import sorcer.service.SignatureException;
import sorcer.service.Task;
import sorcer.util.Sorcer;
import sorcer.util.exec.ExecUtils;
import sorcer.util.exec.ExecUtils.CmdResult;
import sorcer.util.url.sos.SdbURLStreamHandlerFactory;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ParModelServices {
	private final static Logger logger = Logger.getLogger(ParModelServices.class
			.getName());

	static {
		ServiceExertion.debug = true;
		URL.setURLStreamHandlerFactory(new SdbURLStreamHandlerFactory());
		System.setProperty("java.util.logging.config.file",
				Sorcer.getHome() + "/configs/sorcer.logging");
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "ju-invoker-beans.jar" });
	}
	
	@BeforeClass 
	public static void setUpOnce() throws IOException, InterruptedException {
		CmdResult result = ExecUtils.execCommand("ant -f " + Sorcer.getHome() 
				+ "/modules/sorcer/src/junit/sorcer/core/invoker/bin/all-model-prv-boot-spawn.xml");
		System.out.println("out: " + result.getOut());
		System.out.println("err: " + result.getErr());
		System.out.println("status: " + result.getExitValue());
				
//		result = ExecUtils.execCommand("ant -f " + Sorcer.getHome() 
//				+ "/modules/sorcer/src/junit/sorcer/core/invoker/bin/all-model-prv-boot-spawn.xml");
		System.out.println("out: " + result.getOut());
		System.out.println("err: " + result.getErr());
		System.out.println("status: " + result.getExitValue());
		Thread.sleep(2000);
	}
	
	@AfterClass 
	public static void cleanup() throws RemoteException, InterruptedException {
		Sorcer.destroyNode(null, Invocation.class);
//		Sorcer.destroy(null, Invocation.class);
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
				context(invoker("expr"), result("invoke/result")));
		
		logger.info("result: " + value(pmt));
		assertEquals(value(pmt), 60.0);
		
		logger.info("result: " + exert(pmt));
		assertEquals(get(exert(pmt), "invoke/result"), 60.0);
	}

	@Test
	public void parNetModelServiceTest() throws RemoteException, ContextException, 
			ExertionException, SignatureException {
		// the provider in ex6/bin parmodel-prv-run.xml
		Task pmt = task(sig("invoke", Invocation.class, "ParModel Service"), 
				context(invoker("expr"), result("invoke/result")));
		
//		logger.info("result: " + value(pmt));
		assertEquals(value(pmt), 60.0);
		
//		logger.info("result: " + exert(pmt));
		assertEquals(get(exert(pmt), "invoke/result"), 60.0);
	}
	
	@Test
	public void parNetVarModelServiceTest() throws RemoteException, ContextException, 
			ExertionException, SignatureException {
		// the provider in ex6/bin varparmodel-prv-run.xml
		Task pmt = task(sig("invoke", Invocation.class, "VarParModel Service"), 
				context(invoker("expr"), result("invoke/result")));

//		logger.info("result: " + value(pmt));
		assertEquals(value(pmt), 60.0);
		
//		logger.info("result: " + exert(pmt));
		assertEquals(get(exert(pmt), "invoke/result"), 60.0);
	}
	
	@Test
	public void parObjectModelAgentTest() throws RemoteException, ContextException, ExertionException, 
			SignatureException, MalformedURLException {
		
		ParModel pm = ParModeler.getParModel();
		Task pmt = task(sig("invoke", pm), context(
				invoker("getSphereVolume"),
				result("sphere/volume"),
				entry("sphere/radius", 20.0),
				agent("getSphereVolume",
					"junit.sorcer.vfe.evaluator.service.Volume",
					new URL(Sorcer.getWebsterUrl()
							+ "/ju-volume-bean.jar"))));
		
		logger.info("result: " + value(pmt));
		assertEquals(value(pmt), 33510.32163829113);
	}
	
	
	@Test
	public void parObjectModelMultiAgentTest() throws RemoteException, ContextException, ExertionException, 
			SignatureException, MalformedURLException {
		
		ParModel pm = ParModeler.getParModel();
		
		// invoking non existing agent and the return value specified
		Task pmt = task(sig("invoke", pm), context(
				invoker("getSphereVolume"),
				result("sphere/volume"),
				entry("sphere/radius", 20.0),
				agent("getSphereVolume",
					"junit.sorcer.vfe.evaluator.service.Volume",
					new URL(Sorcer.getWebsterUrl()
							+ "/ju-volume-bean.jar")),
				agent("getCylinderSurface",
					"junit.sorcer.vfe.evaluator.service.Volume",
					new URL(Sorcer.getWebsterUrl()
								+ "/ju-volume-bean.jar"))));
		
		logger.info("result: " + value(pmt));
		assertEquals(value(pmt), 33510.32163829113);
		
		// the existing agent and the return value specified
		pmt = task(sig("invoke", pm), context(
				invoker("getCylinderSurface"),
				result("cylinder/surface"),
				entry("cylinder/radius", 1.0), 
				entry("cylinder/height", 2.0)));
		
		assertEquals(value(pmt), 18.84955592153876);

		// the existing agent and no return value specified
		pmt = task(sig("invoke", pm), context(
				invoker("getCylinderSurface"),
				entry("cylinder/radius", 1.0), 
				entry("cylinder/height", 2.0)));

		assertEquals(get((Context)value(pmt), "cylinder/surface"), 18.84955592153876);
	}
	
	@Test
	public void parNetModelAgentTest() throws RemoteException, ContextException, ExertionException, 
			SignatureException, MalformedURLException, TransactionException {
		// the provider in ex6/bin parmodel-prv-run.xml
		Task pmt = task(sig("invoke", Invocation.class, "ParModel Service"), 
				context(invoker("getSphereVolume"),
//						result("sphere/volume"),
						entry("sphere/radius", 20.0),
						agent("getSphereVolume",
							"junit.sorcer.vfe.evaluator.service.Volume",
							new URL(Sorcer.getWebsterUrl()
									+ "/ju-volume-bean.jar"))));
	
		

//		logger.info("result: " + pmt.exert());
		Context cxt = (Context)value(pmt);
		logger.info("result cxt: " + cxt);
		assertEquals(get(cxt, "sphere/radius"), 20.0);
		assertEquals(get(cxt, "sphere/volume"), 33510.32163829113);
		
		pmt = task(sig("invoke", Invocation.class, "ParModel Service"), 
				context(invoker("getSphereVolume"),
						result("sphere/volume"),
						entry("sphere/radius", 20.0),
						agent("getSphereVolume",
							"junit.sorcer.vfe.evaluator.service.Volume",
							new URL(Sorcer.getWebsterUrl()
									+ "/ju-volume-bean.jar"))));
		
//		logger.info("result: " + value(pmt));
		assertEquals(value(pmt), 33510.32163829113);
	}

	@Test
	public void parNetModelMultiAgentTest() throws RemoteException, ContextException, ExertionException, 
			SignatureException, MalformedURLException, TransactionException {
		// the provider in ex6/bin parmodel-prv-run.xml
		Task pmt = task(sig("invoke", Invocation.class, "ParModel Service"), 
				context(invoker("getSphereVolume"),
						result("sphere/volume"),
						entry("sphere/radius", 20.0),
						agent("getSphereVolume",
							"junit.sorcer.vfe.evaluator.service.Volume",
							new URL(Sorcer.getWebsterUrl()
									+ "/ju-volume-bean.jar")),
						agent("getCylinderSurface",
							"junit.sorcer.vfe.evaluator.service.Volume",
							new URL(Sorcer.getWebsterUrl()
										+ "/ju-volume-bean.jar"))));
	
		assertEquals(value(pmt), 33510.32163829113);
		
		pmt = task(sig("invoke", Invocation.class, "ParModel Service"), 
				context(invoker("getCylinderSurface"),
						result("cylinder/surface"),
						invoker("getCylinderSurface"),
						result("cylinder/surface"),
						entry("cylinder/radius", 1.0), 
						entry("cylinder/height", 2.0)));
		
		assertEquals(value(pmt), 18.84955592153876);
	}
}
