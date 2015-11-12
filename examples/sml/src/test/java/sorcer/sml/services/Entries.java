package sorcer.sml.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.invoker.Invocable;
import sorcer.service.*;

import java.rmi.RemoteException;

import static java.lang.Math.pow;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.*;


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
	public void expressionEntry() throws Exception {

		Entry z1 = ent("z1", expr("x1 + 4 * x2 + 30",
					args("x1", "x2"),
					context(ent("x1", 10.0), ent("x2", 20.0))));

		assertEquals(120.0, value(z1));
	}

	@Test
	public void bindingEntryArgs() throws Exception {

		Entry y = ent("y", expr("x1 + x2", args("x1", "x2")));

		assertTrue(value(y, ent("x1", 10.0), ent("x2", 20.0)).equals(30.0));
	}


	public static class Doer implements Invocation<Double> {

		@Override
		public Double invoke(Context cxt, Arg... entries) throws RemoteException, ContextException {
			Entry<Double> x = ent("x", 20.0);
			Entry<Double> y = ent("y", 30.0);
			Entry<Double> z = ent("z", invoker("x - y", x, y));

			if (value(cxt, "x") != null)
				set(x, value(cxt, "x"));
			if (value(cxt, "y") != null)
				set(y, value(cxt, "y"));
			return value(y) + value(x) + value(z);
		}
	};

	@Test
	public void methodEntry() throws Exception {

		Object obj = new Doer();

		// no scope for invocation
		Entry m1 = ent("m1", methodInvoker("invoke", obj));
		assertEquals(value(m1), 40.0);

		// method invocation with a scope
		Context scope = context(ent("x", 200.0), ent("y", 300.0));
		m1 = ent("m1", methodInvoker("invoke", obj, scope));
		assertEquals(value(m1), 400.0);
	}

	@Test
	public void lambdaEntries() throws Exception {

		Entry y1 = lambda("add", () -> 20.0 * pow(0.5, 6) + 10.0);

		assertEquals(10.3125, value(y1));

		Entry y2 = lambda("add", (Context<Double> cxt) ->
						value(cxt, "x1") + value(cxt, "x2"),
				context(ent("x1", 10.0), ent("x2", 20.0)));

		assertEquals(30.0, value(y2));
	}

}
