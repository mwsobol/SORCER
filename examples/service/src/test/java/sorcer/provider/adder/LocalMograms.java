package sorcer.provider.adder;

import net.jini.id.Uuid;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.provider.ServiceProvider;
import sorcer.core.provider.SessionBeanProvider;
import sorcer.provider.adder.impl.AdderImpl;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.mo.operator.*;
import static sorcer.po.operator.ent;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/service")
public class LocalMograms {
	private final static Logger logger = LoggerFactory.getLogger(LocalMograms.class);

	@Test
	public void exertImplTask() throws Exception {

		// a service bean AdderImpl exerted directy
		Task t5 = task("t5", sig("add", AdderImpl.class),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0)));

		Task out = exert(t5);
		Context cxt = context(out);
		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + get(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ calculated/provider: " + value(cxt, "calculated/provider"));
		logger.info("context @ result/eval: " + value(cxt, "eval/result"));
		// same as "eval/result"
		logger.info("context @ result/eval: " + value(cxt, Adder.RESULT_PATH));

		// get a single context argument
		assertEquals(100.0, value(cxt, "eval/result"));

		// get the subcontext output from the context
		assertTrue(context(ent("arg/x1", 20.0), ent("eval/result", 100.0)).equals(
				value(cxt, outPaths("arg/x1", "eval/result"))));
	}

	@Test
	public void exertBeanTask() throws Exception {

		// a service bean AdderImpl exerted by the ServiceProvider container
		Task t5 = task("t5", sig("add", AdderImpl.class, ServiceProvider.class),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0)));

		Task out = exert(t5);
		Context cxt = context(out);
		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + get(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ calculated/provider: " + value(cxt, "calculated/provider"));
		logger.info("context @ result/eval: " + value(cxt, "eval/result"));
		// same as "eval/result"
		logger.info("context @ result/eval: " + value(cxt, Adder.RESULT_PATH));

		// get a single context argument
		assertEquals(100.0, value(cxt, "eval/result"));

		// get the subcontext output from the context
		assertTrue(context(ent("arg/x1", 20.0), ent("eval/result", 100.0)).equals(
				value(cxt, outPaths("arg/x1", "eval/result"))));
	}

	@Test
	public void exertSessionBeanTask() throws Exception  {

		// a service bean AdderImpl exerted by the SessionBeanProvider container
		Task t5 = task("t5", trgSig("add", AdderImpl.class, SessionBeanProvider.class),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));

		Uuid cid = id(context(t5));
		Context out = context(exert(t5));
		assertTrue(value(out, "result/y").equals(100.0));
		assertTrue(value(out, "bean/session") == null);
		assertTrue(id(out).equals(cid));

		out = context(exert(t5));
		assertTrue(value(out, "result/y").equals(100.0));
		assertTrue(value(out, "bean/session").equals(cid));
		assertTrue(id(out).equals(cid));
	}

    @Test
    public void sessionUpdateTasks() throws Exception  {

        Object provider = SessionBeanProvider.class.newInstance();
        Task t5 = task("t5", sig("add", AdderImpl.class, provider),
                cxt("t5-add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0),
                        result("result/y", session(append("arg/x1", "arg/x2"),
								read("arg/x1", "arg/x2"), write("result/y")))));

        Uuid cid = id(context(t5));
        Context out = context(exert(t5));
        logger.info("out: " + out);
        assertTrue(value(out, "result/y").equals(100.0));
        // no sessions yet
        assertTrue(value(out, "bean/session") == null);

        Task t6 = task("t6", sig("nothing", AdderImpl.class, provider),
                cxt("t6-add", result(session(state("arg/x1", "arg/x2", "result/y")))));

        setId(context(t6), id(out));
        out = context(exert(t6));
        logger.info("out: " + out);
        assertTrue(value(out, "result/y").equals(100.0));
        // session from the previous call
        assertTrue(value(out, "bean/session").equals(cid));

        Task t7 = task("t7", sig("nothing", AdderImpl.class, provider),
                cxt("t7-add", result(session(state("*")))));

        setId(context(t7), id(out));
        out = context(exert(t7));
        logger.info("out: " + out);
        assertTrue(value(out, "result/y").equals(100.0));
        // session from the previous call
        assertTrue(value(out, "bean/session").equals(cid));
    }

	@Test
	public void evaluateTask() throws Exception {

		Task t5 = task("t5", sig("add", AdderImpl.class),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), result("result/y")));

		// get the result eval
		assertEquals(100.0, exec(t5));

		// get the output subcontext from the context
		assertTrue(context(ent("arg/x1", 20.0), ent("result/y", 100.0)).equals(
				exec(t5, result(outPaths("arg/x1", "result/y")))));
	}

	@Test
	public void filterTaskContext() throws Exception {

		Task t5 = task("t5", sig("add", AdderImpl.class),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), outVal("out/val"),
						outPaths("out/val", "calculated/provider")));

		// get the subcontext output from the exertion
		assertTrue(context(ent("calculated/provider", AdderImpl.class.getName()),
				ent("out/val", 100.0)).equals(exec(t5)));
	}

	@Test
	public void singletonOutPathContext() throws Exception {

		Task t5 = task("t5", sig("add", AdderImpl.class),
				cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0), outVal("out/val"),
						outPaths("out/val")));

		// get the result eval
		assertEquals(100.0, exec(t5));
	}

	@Test
	public void evaluateLocalModel() throws Exception {

		// three entry model
		Model mod = model(inVal("arg/x1", 10.00), inVal("arg/x2", 90.00),
				ent(sig("add", AdderImpl.class, result("result/y", inPaths("arg/x1", "arg/x2")))),
					response("add", "arg/x1", "arg/x2"));

		Context out = response(mod);

		assertTrue(get(out, "add").equals(100.0));
		assertTrue(get(mod, "result/y").equals(100.0));
	}

}
	
	
