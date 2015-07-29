package sorcer.pml.modeling;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.model.par.ParModel;
import sorcer.pml.model.ParModeler;
import sorcer.service.Invocation;
import sorcer.service.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@ProjectContext("examples/pml")
public class ParModelServices {
	private final static Logger logger = LoggerFactory.getLogger(ParModelServices.class
			.getName());
	
	@Test
	public void parModelerTest() throws Exception {

		ParModel pm = ParModeler.getParModel();
		logger.info("result: " + invoke(pm, "expr"));
		assertTrue(invoke(pm, "expr").equals(60.0));

	}

	@Test
	public void parObjectModelServiceTest() throws Exception {

		ParModel pm = ParModeler.getParModel();
		Task pmt = task(sig("invoke", pm),
				context(result("invoke/result", outPaths("expr"))));

		assertTrue(value(pmt).equals(60.0));

		assertTrue(get(exert(pmt), "invoke/result").equals(60.0));

	}

	@Test
	public void parNetModelServiceTest() throws Exception {

		// the provider in ex6/bin parmodel-prv-run.xml
		Task pmt = task(sig("invoke", Invocation.class, prvName("ParModel Service")),
				context(result("invoke/result", outPaths("expr"))));

		assertTrue(value(pmt).equals(60.0));
		
//		assertTrue(get(exert(pmt), "invoke/result").equals(60.0));

	}
	
}
