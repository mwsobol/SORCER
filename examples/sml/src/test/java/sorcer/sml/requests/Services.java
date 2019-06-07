package sorcer.sml.requests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.Subroutine;
import sorcer.core.context.model.ent.Value;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.evr;
import sorcer.service.modeling.func;
import sorcer.util.GenericUtil;

import java.rmi.RemoteException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.co.operator.setValue;
import static sorcer.ent.operator.alt;
import static sorcer.ent.operator.*;
import static sorcer.ent.operator.loop;
import static sorcer.ent.operator.opt;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.args;
import static sorcer.eo.operator.fi;
import static sorcer.eo.operator.result;
import static sorcer.mo.operator.model;
import static sorcer.mo.operator.*;
import static sorcer.mo.operator.v;
import static sorcer.mo.operator.value;
import static sorcer.service.Signature.Direction;
import static sorcer.so.operator.eval;
import static sorcer.so.operator.exec;
import static sorcer.util.exec.ExecUtils.CmdResult;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Services {
	private final static Logger logger = LoggerFactory.getLogger(Services.class);


	@Test
	public void serviceEntries() throws Exception {

        assertTrue(exec(prc("arg/x1", 100.0)).equals(100.0));

		assertTrue(exec(inVal("arg/x2", 20.0)).equals(20.0));

		assertTrue(exec(outVal("arg/x3", 80.0)).equals(80.0));

		assertTrue(exec(inoutVal("arg/x4", 60.0)).equals(60.0));
    }

    @Test
    public void setup1() throws Exception {
        Setup cxtEnt = setup("context/execute", context(val("arg/x1", 100.0), val("arg/x2", 20.0)));
        assertEquals(100.0, val(cxtEnt, "arg/x1"));
    }

    @Test
    public void setup2() throws Exception {
        Setup cxtEnt = setup("context/execute", val("arg/x1", 100.0), val("arg/x2", 20.0));
        assertEquals(100.0, val(cxtEnt, "arg/x1"));
    }

    @Test
    public void setValueOfSetup1() throws Exception {
        Setup cxtEnt = setup("context/execute", val("arg/x1", 100.0), val("arg/x2", 20.0));
        setValue(cxtEnt, "arg/x1", 80.0);
        assertEquals(80.0, val(cxtEnt, "arg/x1"));
    }

    @Test
    public void setValueOfSetup2() throws Exception {
        Setup cxtEnt = setup("context/execute", val("arg/x1", 100.0), val("arg/x2", 20.0));
        setValue(cxtEnt, val("arg/x1", 80.0), val("arg/x2", 10.0));
        assertEquals(80.0, val(cxtEnt, "arg/x1"));
        assertEquals(10.0, val(cxtEnt, "arg/x2"));
    }

    @Test
    public void entFidelities() throws Exception {
        Entry mfiEnt = inVal("by", entFi(inVal("by-10", 10.0), inVal("by-20", 20.0)));

        assertTrue(exec(mfiEnt, fi("by-20", "by")).equals(20.0));
        assertTrue(exec(mfiEnt, fi("by-10", "by")).equals(10.0));
    }

	@Test
	public void expressionEntry() throws Exception {

		Subroutine z1 = prc("z1", expr("x1 + 4 * x2 + 30",
					context(prc("x1", 10.0), prc("x2", 20.0)),
                    args("x1", "x2")));

		assertEquals(120.0, exec(z1));
	}

	@Test
	public void bindingEntryArgs() throws Exception {

		Subroutine y = prc("y", expr("x1 + x2", args("x1", "x2")));

		assertTrue(exec(y, val("x1", 10.0), val("x2", 20.0)).equals(30.0));
	}


	public static class Doer implements Invocation<Double> {

        @Override
        public Double invoke(Context<Double> cxt, Arg... args) throws RemoteException, ContextException {
            Entry<Double> x = val("x", 20.0);
            Entry<Double> y = val("y", 30.0);
            Entry<Double> z = prc("z", invoker("x - y", x, y));

            if (value(cxt, "x") != null)
                setValue(x, value(cxt, "x"));
            if (value(cxt, "y") != null)
                setValue(y, value(cxt, "y"));
            return exec(y) + exec(x) + exec(z);
        }

        public Object execute(Arg... args) throws ServiceException, RemoteException {
            return invoke(null, args);
        }

//        @Override
//        public Data execEnt(Arg... args) throws ServiceException, RemoteException {
//            return ent(getClass().getSimpleName(), invoke(null, args));
//        }
//
//        @Override
//        public Data execEnt(String entryName, Arg... args) throws ServiceException, RemoteException {
//            return ent(entryName, invoke(null, args));
//        }

    }

    @Test
    public void methodInvokerContext() throws Exception {

        Object obj = new Doer();

        // no scope for invocation
        Entry m1 = prc("m1", methodInvoker("invoke", obj));
        assertEquals(exec(m1), 40.0);

        // method invocation with a scope
        Context scope = context(val("x", 200.0), val("y", 300.0));
        m1 = prc("m1", methodInvoker("invoke", obj, scope));
        assertEquals(exec(m1), 400.0);
    }


    @Test
	public void methodInvokerModel() throws Exception {

        Object obj = new Doer();

        // no scope for invocation
        Entry m1 = prc("m1", methodInvoker("invoke", obj));
        assertEquals(exec(m1), 40.0);

        // method invocation with a scope
        Context scope = context(val("x", 200.0), val("y", 300.0));
        m1 = prc("m1", methodInvoker("invoke", obj, scope));
        assertEquals(exec(m1), 400.0);
    }

    @Test
    public void systemCmdInvoker() throws Exception {
        Args args;
        if (GenericUtil.isLinuxOrMac()) {
            args = args("sh", "-c", "echo $USER");
        } else {
            args = args("cmd",  "/C", "echo %USERNAME%");
        }

        Subroutine cmd = prc("cmd", invoker(args));

        CmdResult result = (CmdResult) exec(cmd);
        logger.info("result: " + result);

        logger.info("result out: " + result.getOut());
        logger.info("result err: " + result.getErr());
        if (result.getExitValue() != 0)
            throw new RuntimeException();
        String userName = (GenericUtil.isWindows() ? System.getenv("USERNAME") : System.getenv("USER"));

        logger.info("User: " + userName);
        logger.info("Got: " + result.getOut());
        assertEquals(userName.toLowerCase(), result.getOut().trim().toLowerCase());
    }

    @Test
    public void signatureEntry() throws Exception {

        Subroutine y1 = srv("y1", sig("add", AdderImpl.class, result("add/out",
                        inPaths("x1", "x2"))),
                    context(inVal("x1", 10.0), inVal("x2", 20.0)));

        assertEquals(30.0, exec(y1));
    }

    @Test
    public void getEntryValueWithArgSelector() throws Exception {

        Subroutine y1 = srv("y1", sig("add", AdderImpl.class),
                context(inVal("x1", 10.0), inVal("x2", 20.0)));

//        logger.info("out eval: {}", eval(y1, selector("result/eval")));
        assertEquals(30.0,  exec(y1, selector("result/eval")));
    }

    @Test
    public void getEntryValueWithSelector() throws Exception {

        Subroutine y1 = srv("y1", sig("add", AdderImpl.class),
                context(inVal("x1", 10.0), inVal("x2", 20.0)),
                selector("result/eval"));

//        logger.info("out eval: {}", eval(y1));
        assertEquals(30.0,  exec(y1));
    }

	@Test
	public void getConditionalCallValueContextScope() throws Exception {

		func y1 = prc("y1", alt(opt(condition((Context<Double> cxt)
                        -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 * x2", args("x1", "x2"))),
			opt(condition((Context<Double> cxt) -> value(cxt, "x1")
                    <= v(cxt, "x2")), expr("x1 + x2", args("x1", "x2")))),
			context(val("x1", 10.0), val("x2", 20.0)));

//        logger.info("out eval: {}", eval(y1));
		assertEquals(30.0,  exec(y1));
	}

    @Test
    public void getConditionalCallValueModelScopel() throws Exception {

        func y1 = prc("y1", alt(opt(condition((Context<Double> cxt)
                        -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 * x2", args("x1", "x2"))),
                opt(condition((Context<Double> cxt) -> value(cxt, "x1")
                        <= v(cxt, "x2")), expr("x1 + x2", args("x1", "x2")))),
                model(prc("x1", 10.0), prc("x2", 20.0)));

//        logger.info("out eval: {}", eval(y1));
        assertEquals(30.0,  exec(y1));
    }

	@Test
	public void getConditionalCall2Value() throws Exception {

		func y1 = prc("y1", alt(opt(condition((Context<Double> cxt)
                        -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 * x2", args("x1", "x2"))),
			opt(condition((Context<Double> cxt) -> v(cxt, "x1")
                    <= v(cxt, "x2")), expr("x1 + x2", args("x1", "x2")))),
			model(prc("x1", 20.0), prc("x2", 10.0)));

//        logger.info("out eval: {}", eval(y1));
		assertEquals(200.0,  exec(y1));
	}

	@Test
	public void getConditionalCall3Value() throws Exception {

		func y1 = prc("y1", alt(opt(condition((Context<Double> cxt)
                        -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 * x2", args("x1", "x2"))),
			opt(30.0)),
			model(prc("x1", 10.0), prc("x2", 20.0)));

//        logger.info("out eval: {}", eval(y1));
		assertEquals(30.0,  exec(y1));
	}

	@Test
	public void getConditionalValueEntModel() throws Exception {

		Model mdl = model(
			val("x1", 10.0), val("x2", 20.0),
			prc("y1", alt(opt(condition((Context<Double> cxt)
                            -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 * x2", args("x1", "x2"))),
				opt(condition((Context<Double> cxt)
                        -> v(cxt, "x1") <= v(cxt, "x2")), expr("x1 + x2", args("x1", "x2"))))));

//        logger.info("out eval: {}", eval(mdl, "y1"));
		assertEquals(30.0,  exec(mdl, "y1"));
	}

    @Test
    public void getConditionalBlockSrvValue() throws Exception {

        Subroutine y1 = srv("y1", block(context(prc("x1", 10.0), prc("x2", 20.0)),
            alt(opt(condition((Context<Double> cxt)
                            -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 + x2", args("x1", "x2"))),
                opt(condition((Context<Double> cxt)
                        -> v(cxt, "x1") <= v(cxt, "x2")), expr("x1 + x2", args("x1", "x2"))))));

//        logger.info("out eval: {}", eval(y1));
        assertEquals(30.0,  exec(y1));
    }

    @Test
    public void getConditionalValueBlockSrvModel() throws Exception {

        Model mdl = model(
            val("x1", 10.0), val("x2", 20.0),
            srv("y1", block(alt(opt(condition((Context<Double> cxt)
                            -> v(cxt, "x1") > v(cxt, "x2")), expr("x1 * x2", args("x1", "x2"))),
                    opt(condition((Context<Double> cxt) -> v(cxt, "x1")
                            <= v(cxt, "x2")), expr("x1 + x2", args("x1", "x2")))))));

//        logger.info("out eval: {}", eval(mdl, "y1"));
        assertEquals(30.0,  exec(mdl, "y1"));
    }

	@Test
	public void getConditionalFunctionalModel() throws Exception {

		Model mdl = model(
			val("x1", 10.0), val("x2", 20.0),
			srv("y1", alt(opt(condition((Context<Double> cxt)
					-> v(cxt, "x1") > v(cxt, "x2")), func(expr("x1 * x2", args("x1", "x2")))),
				opt(condition((Context<Double> cxt) -> v(cxt, "x1")
					<= v(cxt, "x2")), func(expr("x1 + x2", args("x1", "x2")))))));

//        logger.info("out: {}", exec(mdl, "y1"));
        evr ev1 = (evr) exec(mdl, "y1");
//		logger.info("out: {}", eval(ev1, mdl));

		assertEquals(30.0,  eval(ev1, mdl));
		assertEquals(30.0,  exec(ev1, mdl));
	}

	@Test
	public void getConditionalLoopSrvValue() throws Exception {

		func y1 = prc("y1",
			loop(condition((Context<Double> cxt) -> v(cxt, "x1") < v(cxt, "x2")),
				invoker("lambda",
                        (Context<Double> cxt) -> { putValue(cxt, "x1", v(cxt, "x1") + 1.0);
                            return v(cxt, "x1") * v(cxt, "x3"); },
				    context(val("x1", 10.0), val("x2", 20.0), val("x3", 40.0)),
                    args("x1", "x2", "x3"))));

//        logger.info("out eval: {}", eval(y1));
		assertEquals(800.0,  exec(y1));
	}

	@Test
	public void getConditionalLoopSrvModel() throws Exception {

		Model mdl = model(
			val("x1", 10.0), val("x2", 20.0), val("x3", 40.0),
			prc("y1",
				loop(condition((Context<Double> cxt) -> v(cxt, "x1") < v(cxt, "x2")),
					invoker("lambda",
						(Context<Double> cxt) -> { putValue(cxt, "x1", v(cxt, "x1") + 1.0);
							return v(cxt, "x1") * v(cxt, "x3"); },
                        args("x1", "x2", "x3")))));

//        logger.info("out eval: {}", eval(mdl, "y1"));
		assertEquals(800.0,  exec(mdl, "y1"));
	}
}
