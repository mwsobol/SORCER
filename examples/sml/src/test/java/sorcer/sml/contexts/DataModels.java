package sorcer.sml.contexts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.ListContext;
import sorcer.core.context.model.ent.Entry;
import sorcer.eo.operator;
import sorcer.service.Context;

import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;
import static sorcer.co.operator.asis;
import static sorcer.co.operator.*;
import static sorcer.co.operator.path;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.put;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.*;
import static sorcer.mo.operator.inputs;
import static sorcer.mo.operator.returnPath;
/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class DataModels {
    private final static Logger logger = LoggerFactory.getLogger(EntModels.class);

    @Test
    public void contextOperator() throws Exception {

        Context<Double> cxt = context(ent("arg/x1", 1.1), ent("arg/x2", 1.2),
                ent("arg/x3", 1.3), ent("arg/x4", 1.4), ent("arg/x5", 1.5));

        add(cxt, ent("arg/x6", 1.6));

        assertTrue(value(cxt, "arg/x1").equals(1.1));
        assertTrue(get(cxt, "arg/x1").equals(1.1));
        assertTrue(asis(cxt, "arg/x1").equals(1.1));

        // aliasing with an reactive eval entry - rvEnt
        put(cxt, rvEnt("arg/x1", value(cxt, "arg/x5")));
        assertTrue(value(cxt, "arg/x1").equals(1.5));

        Context<Double> subcxt = context(cxt, list("arg/x4", "arg/x5"));
        logger.info("subcontext: " + subcxt);
        assertNull(get(subcxt, "arg/x1"));
        assertNull(get(subcxt, "arg/x2"));
        assertNull(get(subcxt, "arg/x3"));
        assertTrue(get(cxt, "arg/x4").equals(1.4));
        assertTrue(get(cxt, "arg/x5").equals(1.5));
        assertTrue(get(cxt, "arg/x6").equals(1.6));

    }


    @Test
    public void softValues() throws Exception {

        Context cxt = context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0));

        // context soft values correspond to a subpath, e.g. "x1"
        // if no match for the exact path. e.g."arg1/x1"
        assertEquals(value(cxt, "arg/x1"), 20.0);
        assertEquals(value(cxt, "x1"), null);
        assertEquals(softValue(cxt, "arg/var/x1"), 20.0);

    }


    @Test
    public void directionalEntries() throws Exception {

        Entry<Double> e = ent("arg/x1", 10.0);
        assertEquals("arg/x1", key(e));
        // a path is a String - usually a sequence of attributes
        assertEquals(path(e), "arg/x1");

        Entry<Double> in = inEnt("arg/x2", 10.0);
        assertTrue(path(in).equals("arg/x2"));
        assertTrue(operator.eval(in).equals(10.0));

        Entry<Double> out = outEnt("arg/y", 20.0);
        assertTrue(path(out).equals("arg/y"));
        assertTrue(operator.eval(out).equals(20.0));

        Entry<Double> inout = inoutEnt("arg/z", 30.0);
        assertTrue(path(inout).equals("arg/z"));
        assertTrue(operator.eval(inout).equals(30.0));

    }


    @Test
    public void inputsOutputs() throws Exception {

        // PositionalContext maintains both paths and indexes
        Context<Double> cxt = context(ent("arg/x1", 1.1), inEnt("arg/x2", 1.2),
                inEnt("arg/x3", 1.3), inEnt("arg/x4", 1.4), inEnt("arg/x5", 1.5));

        add(cxt, ent("arg/x6", 1.6));
        add(cxt, outEnt("out/y1", 1.7));
        add(cxt, outEnt("out/y2", 1.8));
        add(cxt, inoutEnt("par/z", 1.9));

        assertTrue(cxt instanceof Context);

        // return the eval at index 1 and 6 in cxt
        assertTrue(get(cxt, 1).equals(1.1));
        assertTrue(get(cxt, 6).equals(1.6));

        // return the eval at position 1 and 6 in cxt
        assertTrue(getAt(cxt, 1).equals(1.1));
        assertTrue(getAt(cxt, 6).equals(1.6));

        // return selected values at given positions in cxt
        assertEquals(select(cxt, 2, 4, 5), list(1.2, 1.4, 1.5));

        // get input and output contexts
        List<String> allInputs = list("arg/x2", "arg/x3", "arg/x4", "arg/x5", "par/z");
        List<String> inputs = list("arg/x2", "arg/x3", "arg/x4", "arg/x5");
        List<String> outputs = list("out/y1", "out/y2", "par/z");
        assertTrue(allInputs.equals(paths(allInputs(cxt))));
        assertTrue(inputs.equals(paths(inputs(cxt))));
        assertTrue(outputs.equals(paths(outputs(cxt))));

        // return all values of inEntries
        assertEquals(inCotextValues(cxt), list(1.5, 1.4, 1.3, 1.2));

        // return all paths of inEntries
        assertEquals(inContextPaths(cxt), list("arg/x5", "arg/x4", "arg/x3", "arg/x2"));

        // return all values of outEntries
        assertEquals(outContextValues(cxt), list(1.8, 1.7, 1.9));

        // return all paths of outEntries
        assertEquals(outContextPaths(cxt), list("out/y2", "out/y1", "par/z"));

    }


    @Test
    public void indexedContextOperator() throws Exception {

        // ListContext complies with Java List API
        ListContext<Double> cxt = listContext(1.1, 1.2, 1.3, 1.4, 1.5);

        assertTrue(cxt instanceof Context);

        assertEquals(cxt.get(1), 1.2);

        cxt.set(1, 5.0);
        assertEquals(cxt.get(1), 5.0);

        assertEquals(cxt.values(), list(1.1, 5.0, 1.3, 1.4, 1.5));

        cxt.set(1, 20.0);
        assertEquals(20.0, cxt.get(1));

        assertEquals(cxt.add(30.0), true);
        assertEquals(cxt.get(5), 30.0);

    }

    @Test
    public void markingContextPaths() throws Exception {

        Context<Double> cxt = context(ent("arg/x1", 1.1), ent("arg/x2", 1.2),
                ent("arg/x3", 1.3), ent("arg/x4", 1.4), ent("arg/x5", 1.5));

        add(cxt, ent("arg/x6", 1.6));
        add(cxt, inEnt("arg/x7", 1.7));
        add(cxt, outEnt("arg/y", 1.8));

        // the default marker (attribute) 'tag'
        mark(cxt, "arg/x1", "tag|set1");
        mark(cxt, "arg/x2", "tag|set1");
        assertEquals(valuesAt(cxt, "tag|set1"), list(1.2, 1.1));

        mark(cxt, "arg/x2", "tag|set2");
        mark(cxt, "arg/x4", "tag|set2");
        assertEquals(valuesAt(cxt, "tag|set2"), list(1.4, 1.2));

        // now the path "arg/x2" is overwritten, so excluded
        assertEquals(valuesAt(cxt, "tag|set1"), list(1.1));

        // the default relation 'triplet', the association:  "triplet|_1|_2|_3"
        mark(cxt, "arg/x1", "triplet|a|x|x");
        mark(cxt, "arg/x2", "triplet|x|b|x");
        mark(cxt, "arg/x3", "triplet|x|x|c");
        assertTrue(getAt(cxt, "triplet|a|x|x").equals(1.1));
        assertTrue(getAt(cxt, "triplet|x|b|x").equals(1.2));
        assertTrue(getAt(cxt, "triplet|x|x|c").equals(1.3));

        mark(cxt, "arg/y", "dnt|open|text|mesh");
        assertTrue(getAt(cxt, "dnt|open|text|mesh").equals(1.8));

        // still the previous marker 'tag' holds with 'triplet'
        // for the same paths: arg/x1 and arg/x2
        assertEquals(valuesAt(cxt, "tag|set2"), list(1.4, 1.2));

        // custom annotation with the association: "person|first|last"
        marker(cxt, "person|first|last");
        add(cxt, ent("arg/Mike/height", 174.0, "person|Mike|Sobolewski"));
        add(cxt, inEnt("arg/John/height", 178.0, "person|John|Doe"));
        assertTrue(getAt(cxt, "person|Mike|Sobolewski").equals(174.0));
        assertTrue(getAt(cxt, "person|John|Doe").equals(178.0));

    }


    @Test
    public void linkedContext() throws Exception {

        Context ac = context("add",
                inEnt("arg1/eval", 90.0),
                inEnt("arg2/eval", 110.0));

        Context mc = context("multiply",
                inEnt("arg1/eval", 10.0),
                inEnt("arg2/eval", 70.0));

        Context lc = context("invoke");

        link(lc, "add", ac);
        link(lc, "multiply", mc);

        ac = context(getLink(lc, "add"));
        mc = context(getLink(lc, "multiply"));

        assertEquals(value(ac, "arg1/eval"), 90.0);
        assertEquals(value(mc, "arg2/eval"), 70.0);

        assertEquals(value(lc, "add/arg1/eval"), 90.0);
        assertEquals(value(lc, "multiply/arg2/eval"), 70.0);

    }


    @Test
    public void sharedContext() throws Exception {

        // two contexts ac and mc sharing arg1/eval
        // and arg3/eval values over the network
        Context ac = context("add",
                inEnt("arg1/eval", 90.0),
                inEnt("arg2/eval", 110.0),
                inEnt("arg3/eval", 100.0));

        // make arg1/eval persistent
        URL a1vURL = storeArg(ac, "arg1/eval");

        // make arg1/eval in mc the same as in ac
        Context mc = context("multiply",
                dbInEnt("arg1/eval", a1vURL),
                inEnt("arg2/eval", 70.0),
                inEnt("arg3/eval", 200.0));

        // sharing arg1/eval from mc in ac
        assertTrue(value(ac, "arg1/eval").equals(90.0));
        assertTrue(value(mc, "arg1/eval").equals(90.0));

        put(mc, "arg1/eval", 200.0);

        assertTrue(value(ac, "arg1/eval").equals(200.0));
        assertTrue(value(mc, "arg1/eval").equals(200.0));

        // sharing arg3/eval from ac in mc
        assertTrue(value(ac, "arg3/eval").equals(100.0));
        assertTrue(value(mc, "arg3/eval").equals(200.0));
        URL a3vURL = storeArg(mc, "arg3/eval");
        add(ac, ent("arg3/eval", a3vURL));

        put(ac, "arg1/eval", 300.0);
        assertTrue(value(ac, "arg1/eval").equals(300.0));
        assertTrue(value(mc, "arg1/eval").equals(300.0));

    }

    @Test
    public void exertContext() throws Exception {
        Context cxt = context(inEnt("x1", 20.0d), inEnt("x2", 40.0d));
        cxt = exert(cxt, inEnt("x1", 50.0d));
        assertTrue(value(cxt, "x1").equals(50.0));
    }


    @Test
    public void contextModelService() throws Exception {
        Context cxt = context(inEnt("x1", 20.0d), inEnt("x2", 40.0d),
                returnPath("x2"));
//        logger.info("service: " + exec(cxt));
        assertEquals(exec(cxt), 40.0);
    }


    @Test
    public void contextModelResponse() throws Exception {
        Context cxt = context(inEnt("x1", 20.0d), inEnt("x2", 40.0d));
        responseUp(cxt, "x1");
        Context out = response(cxt);
//        logger.info("response1: " + out);
        assertTrue(out.size() == 1);
        assertTrue(get(out, "x1").equals(20.0));
        responseUp(cxt, "x2");
        out = response(cxt);
//        logger.info("response2: " + out);
        assertTrue(out.size() == 2);
        assertTrue(get(out, "x1").equals(20.0));
        assertTrue(get(out, "x2").equals(40.0));
        responseDown(cxt, "x2");
        out = response(cxt);
        assertTrue(get(out, "x1").equals(20.0));
//        logger.info("response3: " + out);
        assertTrue(out.size() == 1);
    }

}
