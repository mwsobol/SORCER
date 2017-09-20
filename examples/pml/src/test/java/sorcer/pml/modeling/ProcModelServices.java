package sorcer.pml.modeling;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.model.ent.ProcModel;
import sorcer.pml.model.ProcModeler;
import sorcer.service.Invocation;
import sorcer.service.Task;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.outPaths;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.invoke;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/pml")
public class ProcModelServices {
	private final static Logger logger = LoggerFactory.getLogger(ProcModelServices.class
			.getName());
	
	@Test
	public void procModelerTest() throws Exception {

		ProcModel pm = ProcModeler.getProcModel();
		logger.info("result: " + invoke(pm, "expr"));
		assertTrue(invoke(pm, "expr").equals(60.0));

	}

	@Test
	public void procObjectModelServiceTest() throws Exception {

		Model pm = ProcModeler.getProcModel();
		Task pmt = task(sig("invoke", pm),
				context(result("invoke/result", outPaths("expr"))));

		assertTrue(eval(pmt).equals(60.0));

		assertTrue(get(exert(pmt), "invoke/result").equals(60.0));

	}

	@Test
	public void procNetModelServiceTest() throws Exception {

		// the provider in ex6/bin parmodel-prv-run.xml
		Task pmt = task(sig("invoke", Invocation.class, prvName("ProcModel Service")),
				context(result("invoke/result", outPaths("expr"))));

		assertTrue(eval(pmt).equals(60.0));
		
//		assertTrue(get(exert(pmt), "invoke/result").equals(60.0));
	}
	
}
