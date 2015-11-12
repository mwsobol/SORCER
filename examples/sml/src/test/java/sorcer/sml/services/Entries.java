package sorcer.sml.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.model.ent.Entry;
import sorcer.service.Context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.args;
import static sorcer.eo.operator.asis;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.invoke;
import static sorcer.po.operator.invoker;


/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" } )
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Entries {
	private final static Logger logger = LoggerFactory.getLogger(Entries.class);


	@Test
	public void directionalEntries() throws Exception {

		Entry x0 = ent("arg/x1", 100.0);
		assertEquals(100.0, value(x0));

		Entry x1 = inEnt("arg/x1", 20.0);
		assertEquals(20.0, value(x1));

		Entry x2 = outEnt("arg/x2", 80.0);
		assertEquals(80.0, value(x2));

		Entry x3 = inoutEnt("arg/x3", x2);
		assertEquals(80.0, value(x3));
		assertEquals(name(asis(x3)), "arg/x2");
	}

	@Test
	public void expressionEntries() throws Exception {

		Entry z1 = ent("z1", invoker("expr", "x1 + x2 + 30",
					args("x1", "x2"),
					context(ent("x1", 10.0), ent("x2", 20.0))));

		assertEquals(60.0, value(z1));
	}

	@Test
	public void invocationEntry() throws Exception {

		Entry y = ent("y", invoker("x1 + x2", args("x1", "x2")));

//		logger.info("y: " + value(y));
//		assertEquals(value(y), 3.0);

		invoke(y, ent("x1", 10.0), ent("x2", 20.0));
		assertTrue(value(y).equals(30.0));
	}

	@Test
	public void lambdaEntries() throws Exception {

		Entry y1 = lambda("add", () -> 20.0 * 5.0 + 10.0);
		assertEquals(110.0, value(y1));

		Entry y2 = lambda("add", (Context<Double> cxt) ->
						value(cxt, "x1") + value(cxt, "x2"),
				context(ent("x1", 10.0), ent("x2", 20.0)));

		assertEquals(30.0, value(y2));
	}

}
