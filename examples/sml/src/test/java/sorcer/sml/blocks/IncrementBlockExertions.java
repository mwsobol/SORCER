package sorcer.sml.blocks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.core.SorcerConstants;
import sorcer.service.Context;
import sorcer.service.Incrementor;
import sorcer.service.Task;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.inVal;
import static sorcer.eo.operator.*;
import static sorcer.ent.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.so.operator.eval;
import static sorcer.so.operator.exec;


/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class IncrementBlockExertions implements SorcerConstants {
	private final static Logger logger = LoggerFactory.getLogger(IncrementBlockExertions.class);

	@Test
	public void entryIncrementor() throws Exception {
		Context cxt = context("add", inVal("arg/x1", 20),
						inVal("arg/x2", 80.0), result("result++"));

		Incrementor z2 = inc(invoker(cxt, "arg/x1"), 2);
		assertEquals(next(z2), 22);
		assertEquals(next(z2), 24);
	}

	@Test
	public void taskIncrement() throws Exception {
		Task t = task(sig("add", AdderImpl.class),
				context("add", inVal("arg/x1", inc("arg/x2", 2.0)),
						inVal("arg/x2", 80.0), result("task/result")));

//		logger.info("result: " + eval(t));
		assertEquals(exec(t), 162.00);
	}

}
