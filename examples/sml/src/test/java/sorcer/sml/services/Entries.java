package sorcer.sml.services;

import net.jini.core.transaction.TransactionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.core.context.model.ent.Entry;
import sorcer.eo.operator;
import sorcer.service.*;
import sorcer.util.GenericUtil;

import java.rmi.RemoteException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.po.operator.*;
import static sorcer.service.Signature.Direction;
import static sorcer.util.exec.ExecUtils.CmdResult;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class Entries {
	private final static Logger logger = LoggerFactory.getLogger(Entries.class);


	@Test
	public void directionalEntries() throws Exception {

		Entry x1 = ent("arg/x1", 100.0);
		assertEquals(100.0, operator.value(x1));
        assertTrue(direction(x1) == null);

		Entry x2 = inEnt("arg/x2", 20.0);
		assertEquals(20.0, operator.value(x2));
        assertTrue(direction(x2) == Direction.IN);

		Entry x3 = outEnt("arg/x3", 80.0);
		assertEquals(80.0, operator.value(x3));
        assertTrue(direction(x3) == Direction.OUT);

        // entry of entry
		Entry x4 = inoutEnt("arg/x4", x3);
		assertEquals(80.0, operator.value(x4));
        assertTrue(direction(x4) == Direction.INOUT);
		assertEquals(operator.name(asis(x4)), "arg/x3");
        assertTrue(direction((Entry) asis(x4)) == Direction.OUT);
	}

    @Test
    public void entFidelities() throws Exception {
        Entry mfi = inEnt("by", operator.eFi(inEnt("by-10", 10.0), inEnt("by-20", 20.0)));

        assertTrue(operator.value(mfi, operator.fi("by", "by-20")).equals(20.0));
        assertTrue(operator.value(mfi, operator.fi("by", "by-10")).equals(10.0));
    }

	@Test
	public void expressionEntry() throws Exception {

		Entry z1 = ent("z1", expr("x1 + 4 * x2 + 30",
					args("x1", "x2"),
					operator.context(ent("x1", 10.0), ent("x2", 20.0))));

		assertEquals(120.0, operator.value(z1));
	}

	@Test
	public void bindingEntryArgs() throws Exception {

		Entry y = ent("y", expr("x1 + x2", args("x1", "x2")));

		assertTrue(operator.value(y, ent("x1", 10.0), ent("x2", 20.0)).equals(30.0));
	}


	public static class Doer implements Invocation<Double> {

        @Override
        public Double invoke(Context cxt, Arg... entries) throws RemoteException, ContextException {
            Entry<Double> x = ent("x", 20.0);
            Entry<Double> y = ent("y", 30.0);
            Entry<Double> z = ent("z", invoker("x - y", x, y));

            if (operator.value(cxt, "x") != null)
                setValue(x, value(cxt, "x"));
            if (operator.value(cxt, "y") != null)
                setValue(y, value(cxt, "y"));
            return operator.value(y) + operator.value(x) + operator.value(z);
        }

        @Override
        public Object exec(Arg... args) throws MogramException, RemoteException, TransactionException {
            return invoke(Arg.getContext(args), args);
        }

        @Override
        public String getName() {
            return getClass().getName();
        }
    };

	@Test
	public void methodEntry() throws Exception {

        Object obj = new Doer();

        // no scope for invocation
        Entry m1 = ent("m1", methodInvoker("invoke", obj));
        assertEquals(operator.value(m1), 40.0);

        // method invocation with a scope
        Context scope = operator.context(ent("x", 200.0), ent("y", 300.0));
        m1 = ent("m1", methodInvoker("invoke", obj, scope));
        assertEquals(operator.value(m1), 400.0);
    }

    @Test
    public void systemCmdInvoker() throws Exception {
        Args args;
        if (GenericUtil.isLinuxOrMac()) {
            args = args("sh", "-c", "echo $USER");
        } else {
            args = args("cmd",  "/C", "echo %USERNAME%");
        }

        Entry cmd = ent("cmd", invoker(args));

        CmdResult result = (CmdResult) operator.value(cmd);
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

        Entry y1 = ent("y1", operator.sig("add", AdderImpl.class, operator.result("add/out",
                        inPaths("x1", "x2"))),
                    operator.context(inEnt("x1", 10.0), inEnt("x2", 20.0)));

        assertEquals(30.0, operator.value(y1));
    }

    @Test
    public void getValueyWithSelector() throws Exception {

        Entry y1 = ent("y1", operator.sig("add", AdderImpl.class),
                operator.context(inEnt("x1", 10.0), inEnt("x2", 20.0)));

//        logger.info("out value: {}", value(y1, selector("result/value")));
        assertEquals(30.0,  operator.value(y1, operator.selector("result/value")));
    }

    @Test
    public void valueOfentryWithSelector() throws Exception {

        Entry y1 = ent("y1", operator.sig("add", AdderImpl.class),
                operator.context(inEnt("x1", 10.0), inEnt("x2", 20.0)),
                operator.selector("result/value"));

//        logger.info("out value: {}", value(y1));
        assertEquals(30.0,  operator.value(y1));
    }
}
