package sorcer.sml.lambdas;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.core.context.model.ent.Subroutine;
import sorcer.ent.operator;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.ent;
import sorcer.util.GenericUtil;

import static java.lang.Math.pow;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.mo.operator.*;
import static sorcer.ent.operator.invoker;
import static sorcer.ent.operator.*;
import static sorcer.so.operator.*;
import static sorcer.util.exec.ExecUtils.CmdResult;

/**
 * @author Mike Sobolewski
 */
@ProjectContext("examples/sml")
public class Entries {

	private final static Logger logger = LoggerFactory.getLogger(Entries.class);

    @Test
    public void lambdaValue() throws Exception {

        // the model execute a lambda expression with no model state altered
        Model mdl = model(ent("x1", 10.0), ent("x2", 20.0),
                lambda("x3", (Model model) -> ent("x5", (double)exec(model, "x2") + 100.0)));

        logger.info("x3: " + eval(mdl, "x3"));
        assertEquals(120.0, exec((ent)exec(mdl, "x3")));

    }

    @Test
    public void lambdaEntries() throws Exception {

        // no free variables
        Subroutine y1 = lambda("y1", () -> 20.0 * pow(0.5, 6) + 10.0);

        assertEquals(10.3125, exec(y1));

        // the model itself as a free variable of the lambda y2
        Model mo = model(ent("x1", 10.0), ent("x2", 20.0),
                lambda("y2", (Context<Double> cxt) ->
                        value(cxt, "x1") + value(cxt, "x2")));

        assertEquals(30.0, exec(mo, "y2"));

    }

    @Test
    public void checkSystemCallExitValueWithLambda() throws Exception {

        Args args;
        if (GenericUtil.isLinuxOrMac()) {
            args = args("sh",  "-c", "echo $USER");
        } else {
            args = args("cmd",  "/C", "echo %USERNAME%");
        }

        // a lambda as a EntryCollable used to enhance the behavior of a model
        EntryCollable verifyExitValue = (Model mdl) -> {
            CmdResult out = (CmdResult)exec(mdl, "cmd");
            int code = out.getExitValue();
            ent("cmd/exitValue", code);
            if (code == -1) {
                EvaluationException ex = new EvaluationException();
                mdl.reportException("cmd failed for lambda", ex);
                throw ex;
            } else
                return ent("cmd/out", out.getOut());
        };

        Model m = model(
                inVal("multiply/x1", 10.0), inVal("multiply/x2", 50.0),
                inVal("add/x1", 20.0), inVal("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("multiply/out",
                        inPaths("multiply/x1", "multiply/x2")))),
                ent(sig("add", AdderImpl.class, result("add/out",
                        inPaths("add/x1", "add/x2")))),
                ent("cmd", invoker(args)),
                lambda("lambda", verifyExitValue),
                response("lambda", "cmd", "cmd/out"));

        Context out = response(m);

        String un = property("user.name");
        assertTrue(((String)get(out, "lambda")).trim().equals(un));
        assertTrue(((String)get(out, "cmd/out")).trim().equals(un));
        assertTrue(((CmdResult)get(out, "cmd")).getOut().trim().equals(un));
    }

    @Test
    public void entryAsLambdaInvoker() throws Exception {

        Model mo = model(val("x", 10.0), val("y", 20.0),
                operator.pro(invoker("lambda", (Context<Double> cxt) -> value(cxt, "x")
                        + value(cxt, "y")
                        + 30, args("x", "y"))));
        logger.info("invoke eval: " + eval(mo, "lambda"));
        assertEquals(exec(mo, "lambda"), 60.0);
    }

    @Test
    public void lambdaService() throws Exception  {

        // an entry as a Service lambda
        Model mo = model(ent("x", 10.0), ent("y", 20.0),
                lambda("s1", (Arg[] args) -> {
                    Arg.set(args, "x",  Arg.get(args, "y"));
                    return exec(Arg.selectService(args, "x")); },
                        args("x", "y")));

        logger.info("s1 eval: ", exec(mo, "s1"));
        assertEquals(exec(mo, "s1"), 20.0);
    }

    @Test
    public void lambdaClient() throws Exception {
        // args as ValueCallable and  Requestor lambdas
        Model mo = model(ent("multiply/x1", 10.0), ent("multiply/x2", 50.0),
                lambda("multiply", (Context<Double> model) ->
                        value(model, "multiply/x1") * value(model, "multiply/x2")),
                lambda("multiply2", "multiply", (Service entry, Context scope, Arg[] args) -> {
                    double out = (double)exec(entry, scope);
                    if (out > 400) {
                        putValue(scope, "multiply/x1", 20.0);
                        putValue(scope, "multiply/x2", 50.0);
                        out = (double)exec(entry, scope);
                    }
                    return context(ent("multiply2", out));
                }),
                response("multiply2"));

        Context result = response(mo);
        logger.info("model response: " + result);
        assertTrue(get(result, "multiply2").equals(1000.0));
    }

}
