package sorcer.provider.adder;

import net.jini.id.Uuid;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.provider.SessionManagement;
import sorcer.service.*;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Wait;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.so.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.mo.operator.*;
import static sorcer.ent.operator.ent;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/service")
public class NetMograms {
	private final static Logger logger = LoggerFactory.getLogger(NetMograms.class.getName());
	
	@Test
	public void exertTask() throws Exception  {

		Task t5 = task("t5", sig("add", Adder.class),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));

		Routine out = exert(t5);
		Context cxt = context(out);
		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + value(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ result/y: " + value(cxt, "result/y"));

		// getValue a single context argument
		assertEquals(100.0, value(cxt, "result/y"));

		// getValue the subcontext output from the context
		assertTrue(context(ent("arg/x1", 20.0), ent("result/y", 100.0)).equals(
				value(cxt, result("result/context", outPaths("arg/x1", "result/y")))));
	}

	@Test
	public void valueTask() throws Exception  {

		Task t5 = task("t5", sig("add", Adder.class),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));

		// getValue the result eval
		assertTrue(exec(t5).equals(100.0));

		// getValue the subcontext output from the exertion
		assertTrue(context(ent("arg/x1", 20.0), ent("result/y", 100.0)).equals(
				exec(t5, outPaths("arg/x1", "result/y"))));

	}

    @Test
    public void sessionTask() throws Exception  {

        Task sum = task("t6", sig("sum", Adder.class, prvName("Adder")),
                cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));

        assertTrue(exec(sum).equals(100.0));
        assertTrue(exec(sum).equals(200.0));
        assertTrue(exec(sum).equals(300.0));
    }

    @Test
	public void beanValueTask() throws Exception  {

		Task t5 = task("t5", sig("add", Adder.class, prvName("Session Adder")),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));

        Uuid cid = id(context(t5));
		Context out = context(exert(t5));
		assertTrue(value(out, "result/y").equals(100.0));
		assertTrue(id(out).equals(cid));
	}

	@Test
	public void beanMultipleSessions() throws Exception  {

		SessionManagement provider = (SessionManagement) provider(sig(Adder.class, prvName("Session Adder")));
		provider.clearSessions();

		Task t5 = task("t5", sig("add", Adder.class, prvName("Session Adder")),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));

		t5.setContext(cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));
		Context out = context(exert(t5));
		assertTrue(value(out, "result/y").equals(100.0));
		assertTrue(provider.getSessionIds().size() == 1);

		t5.setContext(cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));
		exert(t5);
		assertTrue(provider.getSessionIds().size() == 2);

		t5.setContext(cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));
		exert(t5);
		assertTrue(provider.getSessionIds().size() == 3);

		provider.clearSessions();
		assertTrue(provider.getSessionIds().size() == 0);
	}

	@Test
	public void beanSessionIdTask() throws Exception  {

		SessionManagement provider = (SessionManagement) provider(sig(Adder.class, prvName("Session Adder")));
		provider.clearSessions();

		Task t5 = task("t5", sig("add", Adder.class, prvName("Session Adder")),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));

		Uuid cid = id(context(t5));
		Context out = context(exert(t5));
		assertTrue(value(out, "result/y").equals(100.0));
		assertTrue(id(out).equals(cid));
		assertTrue(provider.getSessionIds().size() == 1);

		context(exert(t5));
		assertTrue(provider.getSessionIds().size() == 1);

		context(exert(t5));
		assertTrue(provider.getSessionIds().size() == 1);

		provider.clearSessions();
		assertTrue(provider.getSessionIds().size() == 0);
	}

    @Test
	public void spaceTask() throws Exception {

		Task t5 = task(
				"t5",
				sig("add", Adder.class),
				context("add", inVal("arg/x1", 20.0),
						inVal("arg/x2", 80.0), result("result/y")),
				strategy(Access.PULL, Wait.YES));

		t5 = exert(t5);
		logger.info("t5 context: " + context(t5));
		logger.info("t5 eval: " + get(t5, "result/y"));
		assertEquals(get(t5, "result/y"), 100.0);
	}

	@Test
	public void evaluateRemoteModel() throws Exception {

		// three entry model
		Model mod = model(inVal("arg/x1", 10.00), inVal("arg/x2", 90.00),
				ent(sig("add", Adder.class, prvName("Adder"), result("result/y", inPaths("arg/x1", "arg/x2")))),
				response("add", "arg/x1", "arg/x2"));

		Context out = response(mod);

		logger.info("out: " +out );
		assertTrue(get(out, "add").equals(100.0));
		assertTrue(get(mod, "result/y").equals(100.0));

	}
}
	
	
