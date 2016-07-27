package sorcer.core.context.model.par;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.impl.ParModelImpl;
import sorcer.core.context.model.ent.ProcModel;
import sorcer.service.*;

import java.rmi.RemoteException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.outPaths;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.invoke;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class ParModelServices {
	private final static Logger logger = LoggerFactory.getLogger(ParModelServices.class);
	public static String sorcerVersion = System.getProperty("sorcer.version");


	@Test
	public void parModelerTest() throws RemoteException, ContextException,
			ExertionException, SignatureException {
		ProcModel pm = ParModelImpl.getParModel();
		logger.info("result: " + invoke(pm, "expr"));
		assertEquals(invoke(pm, "expr"), 60.0);
	}

	@Test
	public void parObjectModelServiceTest() throws Exception {
		ProcModel pm = ParModelImpl.getParModel();
		Task pmt = task(sig("invoke", pm),
				context(result("invoke/result", outPaths("expr"))));

		eval(pmt);
		logger.info("result: " + eval(pmt));
		assertTrue(eval(pmt).equals(60.0));

		logger.info("result: " + exert(pmt));
		assertEquals(get(exert(pmt), "invoke/result"), 60.0);
	}

	@Test
	public void parNetModelServiceTest() throws Exception,
			ExertionException, SignatureException {
		// the provider in ex6/bin parmodel-prv-run.xml
		Task pmt = task(sig("invoke", Invocation.class, prvName("Arithmetic ProcModel")),
				context(result("invoke/result", outPaths("expr"))));

		logger.info("result: " + eval(pmt));
		assertTrue(eval(pmt).equals(60.0));

		logger.info("result: " + exert(pmt));
		assertEquals(get(exert(pmt), "invoke/result"), 60.0);
	}
	
//	@Test
//	public void parObjectModelAgentTest() throws RemoteException, ContextException, ExertionException,
//			SignatureException, MalformedURLException {
//
//		ProcModel pm = ParModelImpl.getParModel();
//		Task pmt = task(sig("invoke", pm), context(
//				invoker("getSphereVolume"),
//				result("sphere/volume"),
//				ent("sphere/radius", 20.0),
//				agent("getSphereVolume",
//						"sorcer.arithmetic.tester.volume.Volume",
//						new URL(Sorcer.getWebsterUrl()
//								+ "/sorcer-tester-"+sorcerVersion+".jar"))));
//
//		logger.info("result: " + eval(pmt));
//		assertEquals(eval(pmt), 33510.32163829113);
//	}
//
//
//	@Test
//	public void parObjectModelMultiAgentTest() throws RemoteException, ContextException, ExertionException,
//			SignatureException, MalformedURLException {
//
//		ProcModel pm = ParModelImpl.getParModel();
//
//		// invoking non existing agent and the return eval specified
//		Task pmt = task(sig("invoke", pm), context(
//				invoker("getSphereVolume"),
//				result("sphere/volume"),
//				ent("sphere/radius", 20.0),
//				agent("getSphereVolume",
//						"sorcer.arithmetic.tester.volume.Volume",
//						new URL(Sorcer.getWebsterUrl()
//								+ "/sorcer-tester-"+sorcerVersion+".jar")),
//				agent("getCylinderSurface",
//						"sorcer.arithmetic.tester.volume.Volume",
//						new URL(Sorcer.getWebsterUrl()
//								+ "/sorcer-tester-"+sorcerVersion+".jar"))));
//
//		logger.info("result: " + eval(pmt));
//		assertEquals(eval(pmt), 33510.32163829113);
//
//		// the existing agent and the return eval specified
//		pmt = task(sig("invoke", pm), context(
//				invoker("getCylinderSurface"),
//				result("cylinder/surface"),
//				ent("cylinder/radius", 1.0),
//				ent("cylinder/height", 2.0)));
//
//		assertEquals(eval(pmt), 18.84955592153876);
//
//		// the existing agent and no return eval specified
//		pmt = task(sig("invoke", pm), context(
//				invoker("getCylinderSurface"),
//				ent("cylinder/radius", 1.0),
//				ent("cylinder/height", 2.0)));
//
//		assertEquals(get((Context)eval(pmt), "cylinder/surface"), 18.84955592153876);
//	}
//
//	@Test
//	public void parNetModelAgentTest() throws RemoteException, ContextException, ExertionException,
//			SignatureException, MalformedURLException, TransactionException {
//		// the provider in ex6/bin parmodel-prv-run.xml
//		Task pmt = task(sig("invoke", Invocation.class, prvName("Arithmetic ProcModel")),
//				context(invoker("getSphereVolume"),
////						result("sphere/volume"),
//						ent("sphere/radius", 20.0),
//						agent("getSphereVolume",
//								"sorcer.arithmetic.tester.volume.Volume",
//								new URL(Sorcer.getWebsterUrl()
//										+ "/sorcer-tester-"+sorcerVersion+".jar"))));
//
//
//
////		logger.info("result: " + pmt.exert());
//		Context cxt = (Context)eval(pmt);
//		logger.info("result cxt: " + cxt);
//		assertEquals(get(cxt, "sphere/radius"), 20.0);
//		assertEquals(get(cxt, "sphere/volume"), 33510.32163829113);
//
//		pmt = task(sig("invoke", Invocation.class, prvName("Arithmetic ProcModel")),
//				context(invoker("getSphereVolume"),
//						result("sphere/volume"),
//						ent("sphere/radius", 20.0),
//						agent("getSphereVolume",
//							"sorcer.arithmetic.tester.volume.Volume",
//							new URL(Sorcer.getWebsterUrl()
//									+ "/sorcer-tester-"+sorcerVersion+".jar"))));
//
////		logger.info("result: " + eval(pmt));
//		assertEquals(eval(pmt), 33510.32163829113);
//	}
//
//	@Test
//	public void parNetModelMultiAgentTest() throws RemoteException, ContextException, ExertionException,
//			SignatureException, MalformedURLException, TransactionException {
//		// the provider in ex6/bin parmodel-prv-run.xml
//		Task pmt = task(sig("invoke", Invocation.class, prvName("Arithmetic ProcModel")),
//				context(invoker("getSphereVolume"),
//						result("sphere/volume"),
//						ent("sphere/radius", 20.0),
//						agent("getSphereVolume",
//								"sorcer.arithmetic.tester.volume.Volume",
//								new URL(Sorcer.getWebsterUrl()
//										+ "/sorcer-tester-" + sorcerVersion + ".jar")),
//						agent("getCylinderSurface",
//								"sorcer.arithmetic.tester.volume.Volume",
//								new URL(Sorcer.getWebsterUrl()
//										+ "/sorcer-tester-" + sorcerVersion + ".jar"))));
//
//		assertEquals(eval(pmt), 33510.32163829113);
//
//		pmt = task(sig("invoke", Invocation.class, prvName("Arithmetic ProcModel")),
//				context(invoker("getCylinderSurface"),
//						result("cylinder/surface"),
//						invoker("getCylinderSurface"),
//						result("cylinder/surface"),
//						ent("cylinder/radius", 1.0),
//						ent("cylinder/height", 2.0)));
//
//		assertEquals(eval(pmt), 18.84955592153876);
//	}
}
