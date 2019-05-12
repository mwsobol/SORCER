package sorcer.sml.contexts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.core.context.ListContext;
import sorcer.core.context.model.ent.Value;
import sorcer.core.provider.rendezvous.ServiceJobber;
import sorcer.ent.operator;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.util.DataTable;
import sorcer.util.Row;

import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.mo.operator.*;
import static sorcer.ent.operator.*;
import static sorcer.so.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class DataModels {
    private final static Logger logger = LoggerFactory.getLogger(ContextModels.class);

    @Test
    public void contextOperator() throws Exception {

        Context<Double> cxt = context(ent("arg/x1", 1.1), ent("arg/x2", 1.2),
                ent("arg/x3", 1.3), ent("arg/x4", 1.4), ent("arg/x5", 1.5));

        add(cxt, ent("arg/x6", 1.6));

        assertTrue(value(cxt, "arg/x1").equals(1.1));
        assertTrue(get(cxt, "arg/x1").equals(1.1));
        assertTrue(asis(cxt, "arg/x1").equals(1.1));

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
    public void slotValues() throws Exception {

        Context cxt = context("add", inVal("arg/x2", 80.0), slot("arg/x1", 20.0));

        // context slot values correspond to as-is out values of slots
        // no multifidelities (different inheritance hierarchy than Tuples)
        assertEquals(value(cxt, "arg/x1"), 20.0);
        assertEquals(value(cxt, "x1"), null);
        assertEquals(softValue(cxt, "arg/var/x1"), 20.0);

    }

    @Test
    public void softValues() throws Exception {

        Context cxt = context("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0));

        // context soft values correspond to a subpath, e.g. "x1"
        // if no match for the exact path. e.g."arg1/x1"
        assertEquals(value(cxt, "arg/x1"), 20.0);
        assertEquals(value(cxt, "x1"), null);
        assertEquals(softValue(cxt, "arg/var/x1"), 20.0);

    }


    @Test
    public void directionalEntries() throws Exception {

        Value<Double> e = val("arg/x1", 10.0);
        assertEquals("arg/x1", key(e));
        // a path is a String - usually a sequence of attributes
        assertEquals(path(e), "arg/x1");

        Value<Double> in = inVal("arg/x2", 10.0);
        assertTrue(path(in).equals("arg/x2"));
        assertTrue(val(in).equals(10.0));

        Value<Double> out = outVal("arg/y", 20.0);
        assertTrue(path(out).equals("arg/y"));
        assertTrue(val(out).equals(20.0));

        Value<Double> inout = inoutVal("arg/z", 30.0);
        assertTrue(path(inout).equals("arg/z"));
        assertTrue(val(inout).equals(30.0));

    }


    @Test
    public void inputsOutputs() throws Exception {

        // PositionalContext maintains both paths and indexes
        Context<Double> cxt = context(val("arg/x1", 1.1), inVal("arg/x2", 1.2),
                inVal("arg/x3", 1.3), inVal("arg/x4", 1.4), inVal("arg/x5", 1.5));

        add(cxt, ent("arg/x6", 1.6));
        add(cxt, outVal("out/y1", 1.7));
        add(cxt, outVal("out/y2", 1.8));
        add(cxt, inoutVal("out/z", 1.9));

        assertTrue(cxt instanceof Context);

        // return the eval at index 1 and 6 in cxt
        assertTrue(get(cxt, 1).equals(1.1));
        assertTrue(get(cxt, 6).equals(1.6));

        // return the eval at position 1 and 6 in cxt
        assertTrue(valueAt(cxt, 1).equals(1.1));
        assertTrue(valueAt(cxt, 6).equals(1.6));

        // return selected values at given positions in cxt
        assertEquals(select(cxt, 2, 4, 5), list(1.2, 1.4, 1.5));

        // get input and output contexts
        List<String> allInputs = list("arg/x2", "arg/x3", "arg/x4", "arg/x5", "out/z");
        List<String> inputs = list("arg/x2", "arg/x3", "arg/x4", "arg/x5");
        List<String> outputs = list("out/y1", "out/y2", "out/z");

        assertTrue(allInputs.equals(paths(allInputs(cxt))));
        assertTrue(inputs.equals(paths(inputs(cxt))));
        assertTrue(outputs.equals(paths(outputs(cxt))));

        // return all values of inEntries
        assertEquals(inCotextValues(cxt), list(1.2, 1.3, 1.4, 1.5));

        // return all paths of inEntries
        assertEquals(inContextPaths(cxt), list("arg/x2", "arg/x3", "arg/x4", "arg/x5"));

        // return all values of outEntries
        assertEquals(outContextValues(cxt), list(1.7, 1.8, 1.9));

        // return all paths of outEntries
        assertEquals(outContextPaths(cxt), list("out/y1", "out/y2", "out/z"));

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
    public void taggingContextPaths() throws Exception {

        Context<Double> cxt = context(ent("arg/x1", 1.1), ent("arg/x2", 1.2),
                ent("arg/x3", 1.3), ent("arg/x4", 1.4), ent("arg/x5", 1.5));

        add(cxt, ent("arg/x6", 1.6));
        add(cxt, inVal("arg/x7", 1.7));
        add(cxt, outVal("arg/y", 1.8));

        // the default tagAssociation (attribute) 'tag'
        tag(cxt, "arg/x1", "tag|set1");
        tag(cxt, "arg/x2", "tag|set1");
        assertEquals(valuesAt(cxt, "tag|set1"), list(1.1, 1.2));

        tag(cxt, "arg/x2", "tag|set2");
        tag(cxt, "arg/x4", "tag|set2");
        assertEquals(valuesAt(cxt, "tag|set2"), list(1.2, 1.4));

        // now the path "arg/x2" is overwritten, so excluded
        assertEquals(valuesAt(cxt, "tag|set1"), list(1.1));

        // the default relation 'triplet', the association:  "triplet|_1|_2|_3"
        tag(cxt, "arg/x1", "triplet|a|x|x");
        tag(cxt, "arg/x2", "triplet|x|b|x");
        tag(cxt, "arg/x3", "triplet|x|x|c");
        assertTrue(valueAt(cxt, "triplet|a|x|x").equals(1.1));
        assertTrue(valueAt(cxt, "triplet|x|b|x").equals(1.2));
        assertTrue(valueAt(cxt, "triplet|x|x|c").equals(1.3));

        tag(cxt, "arg/y", "dnt|open|text|mesh");
        assertTrue(valueAt(cxt, "dnt|open|text|mesh").equals(1.8));

        // still the previous tagAssociation 'tag' holds with 'triplet'
        // for the same paths: arg/x1 and arg/x2
        assertEquals(valuesAt(cxt, "tag|set2"), list( 1.2, 1.4));

        // custom annotation with the association: "person|first|last"
        tagAssociation(cxt, "person|first|last");
        add(cxt, ent("arg/Mike/height", 174.0, "person|Mike|Sobolewski"));
        add(cxt, inVal("arg/John/height", 178.0, "person|John|Doe"));
        assertTrue(valueAt(cxt, "person|Mike|Sobolewski").equals(174.0));
        assertTrue(valueAt(cxt, "person|John|Doe").equals(178.0));

    }


    @Test
    public void linkedContext() throws Exception {

        Context ac = context("add",
                inVal("arg1/eval", 90.0),
                inVal("arg2/eval", 110.0));

        Context mc = context("multiply",
                inVal("arg1/eval", 10.0),
                inVal("arg2/eval", 70.0));

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
        Model ac = entModel("add",
                inVal("arg1/eval", 90.0),
                inVal("arg2/eval", 110.0),
                inVal("arg3/eval", 100.0));

        // make arg1/eval persistent in the network
        URL a1vURL = storeVal(ac, "arg1/eval");

        // make arg1/eval in mc the same as in ac
        Model mc = entModel("multiply",
                dbInVal("arg1/eval", a1vURL),
                inVal("arg2/eval", 70.0),
                inVal("arg3/eval", 200.0));

        // sharing arg1/eval from mc in ac
        assertTrue(exec(ac, "arg1/eval").equals(90.0));
        assertTrue(exec(mc, "arg1/eval").equals(90.0));

        setValues(mc, val("arg1/eval", 200.0));
        assertTrue(exec(ac, "arg1/eval").equals(200.0));
        assertTrue(exec(mc, "arg1/eval").equals(200.0));

        // sharing arg3/eval from ac in mc
        assertTrue(exec(ac, "arg3/eval").equals(100.0));
        assertTrue(exec(mc, "arg3/eval").equals(200.0));
        URL a3vURL = storeVal(mc, "arg3/eval");
        add(ac, pro("arg3/eval", a3vURL));

        setValues(mc, val("arg1/eval", 300.0));
        assertTrue(exec(ac, "arg1/eval").equals(300.0));
        assertTrue(exec(mc, "arg1/eval").equals(300.0));

    }

    @Test
    public void exertContext() throws Exception {
        Context cxt = context(inVal("x1", 20.0d), inVal("x2", 40.0d));
        cxt = exert(cxt, inVal("x1", 50.0));
        assertTrue(value(cxt, "x1").equals(50.0));
    }


    @Test
    public void contextModelService() throws Exception {
        Context cxt = context(inVal("x1", 20.0d), inVal("x2", 40.0d),
                returnPath("x2"));
//        logger.info("service: " + exec(cxt));
        assertEquals(exec(cxt), 40.0);
    }


    @Test
    public void contextModelResponse() throws Exception {
        Context cxt = context(inVal("x1", 20.0d), inVal("x2", 40.0d));
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
        logger.info("response3: " + out);
        assertTrue(out.size() == 1);
    }


    @Test
    public void contextfromEntryList() throws Exception {

        Context cxt = context(list(inVal("x1", 20.0d), inVal("x2", 40.0d)));
        assertTrue(value(cxt, "x1").equals(20.0));
        assertTrue(value(cxt, "x2").equals(40.0));
    }


    @Test
    public void ResponseRowToContexToRow() throws Exception {

        Response row = row(inVal("x1", 20.0d), inVal("x2", 40.0d));
        assertTrue(get(row, "x1").equals(20.0));
        assertTrue(get(row, "x2").equals(40.0));

        Context cxt = context(row);
        assertTrue(value(cxt, "x1").equals(20.0));
        assertTrue(value(cxt, "x2").equals(40.0));

        row = row(cxt);
        assertTrue(get(row, "x1").equals(20.0));
        assertTrue(get(row, "x2").equals(40.0));
    }


    @Test
    public void responeDataTable() throws Exception {

        // default row identifiers in the table are subsequent integers
        DataTable data = dataTable(header("x", "y", "area", "aspect", "perimeter"),
                list(1.0, 1.0, 1.0, 1.0, 4.0),
                list(1.0, 2.0, 2.0, 2.0, 6.0),
                list(2.0, 1.0, 2.0, 0.5, 6.0),
                list(2.0, 2.0, 4.0, 1.0, 8.0),
                list(1.1, 1.0, 1.1, 0.9090909090909091, 4.2),
                list(0.1, 0.1, 0.010000000000000002, 1.0, 0.4));

        Row area = column(data, "area");
//        logger.info("area: " + area);
        assertTrue(area.getRow(0).equals(list(1.0, 2.0, 2.0, 4.0, 1.1, 0.010000000000000002)));

        Row row3 = row(data, 3);
//        logger.info("row3: " + row3);
        assertTrue(row3.getRow(0).equals(list(2.0, 2.0, 4.0, 1.0, 8.0)));
//        logger.info("row3/2: " + get(row3, 2));
        assertTrue(get(row3, 2).equals(4.0));

//        logger.info("area/2: " + get(data, "area", ind(3)));
        // default syntax for columnInd
        assertTrue(get(data, "area", ind(3)).equals(4.0));

        assertTrue(get(data, "area", columnInd(3)).equals(4.0));

//        logger.info("3/3 row: " + get(data, "3", rowInd(3)));
        //the forth element at the forth index
        assertTrue(get(data, "3", rowInd(3)).equals(1.0));
    }

    public Job getArithmeticJob() throws Exception {
        Task t3 = task(
                "t3",
                sig("subtract", SubtractorImpl.class),
                context("subtract", in(val("arg/x1")), in(val("arg/x2")),
                        out(val("result/y"))));

        Task t4 = task(
                "t4",
                sig("multiply", MultiplierImpl.class),
                context("multiply", in(ent("arg/x1", 10.0)), in(ent("arg/x2", 50.0)),
                        out(ent("result/y"))));

        Task t5 = task(
                "t5",
                sig("add", AdderImpl.class),
                context("add", in(ent("arg/x1", 20.0)), in(ent("arg/x2", 80.0)),
                        out(ent("result/y"))));

        Job job = job(sig("exert", ServiceJobber.class),
                "j1", t4, t5, t3,
                pipe(outPoint(t4, "result/y"), inPoint(t3, "arg/x1")),
                pipe(outPoint(t5, "result/y"), inPoint(t3, "arg/x2")));

        return job;
    }

    @Test
    public void contextBag() throws Exception {
        Context cxt1 = context(inVal("x1", 20.0d), inVal("x2", 40.0d));

        Context cxt2 = context(inVal("x3", 30.0d), inVal("x4", 50.0d));

        Task t5 = task("t5", sig("add", AdderImpl.class),
                cxt("add", inVal("arg/x1", 20.0), inVal("arg/x2", 80.0)));

        Job j1 = getArithmeticJob();

        Context bag = context(cxt1, cxt2,
                execEnt(t5, self(selector("result/eval"), true)),
                execEnt(j1, selector("result/y")));

        logger.info("context bag: " + bag);
        assertEquals(value(bag, "j1"), 400.0);
        assertEquals(value(bag, "t5"), 100.0);
        assertEquals(value(bag, "x1"), 20.0);
        assertEquals(value(bag, "x3"), 30.0);
    }

}
