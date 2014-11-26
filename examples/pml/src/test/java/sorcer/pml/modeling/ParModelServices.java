package sorcer.pml.modeling;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.model.par.ParModel;
import sorcer.pml.model.ParModeler;
import sorcer.service.Invocation;
import sorcer.service.Task;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.from;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.invoke;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/pml")
public class ParModelServices {
	private final static Logger logger = Logger.getLogger(ParModelServices.class
			.getName());
	
	@Test
	public void parModelerTest() throws Exception {

		ParModel pm = ParModeler.getParModel();
		logger.info("result: " + invoke(pm, "expr"));
		assertEquals(invoke(pm, "expr"), 60.0);

	}

	@Ignore
	@Test
	public void parObjectModelServiceTest() throws Exception {

		ParModel pm = ParModeler.getParModel();
		Task pmt = task(sig("invoke", pm),
				context(result("invoke/result", from("expr"))));

		assertEquals(value(pmt), 60.0);

		assertEquals(get(exert(pmt), "invoke/result"), 60.0);

	}

	@Ignore
	@Test
	public void parNetModelServiceTest() throws Exception {

		// the provider in ex6/bin parmodel-prv-run.xml
		Task pmt = task(sig("invoke", Invocation.class, prvName("ParModel Service")),
				context(result("invoke/result", from("expr"))));

		assertEquals(value(pmt), 60.0);
		
		assertEquals(get(exert(pmt), "invoke/result"), 60.0);

	}
	
}
