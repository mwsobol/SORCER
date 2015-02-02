package sorcer.sml.contexts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.provider.impl.AdderImpl;
import sorcer.arithmetic.provider.impl.MultiplierImpl;
import sorcer.arithmetic.provider.impl.SubtractorImpl;
import sorcer.co.tuple.Entry;
import sorcer.core.context.Copier;
import sorcer.core.context.ListContext;
import sorcer.core.provider.rendezvous.ServiceModeler;
import sorcer.service.Context;
import sorcer.service.Signature;
import sorcer.service.modeling.Model;

import java.net.URL;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static sorcer.co.operator.asis;
import static sorcer.co.operator.*;
import static sorcer.co.operator.path;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.put;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.invoker;
/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class ServiceContexts {
	private final static Logger logger = Logger.getLogger(ServiceContexts.class.getName());

	@Test
	public void contextOperator() throws Exception {

		Context<Double> cxt = context(ent("arg/x1", 1.1), ent("arg/x2", 1.2),
				ent("arg/x3", 1.3), ent("arg/x4", 1.4), ent("arg/x5", 1.5));

		add(cxt, ent("arg/x6", 1.6));

		assertTrue(value(cxt, "arg/x1").equals(1.1));
		assertTrue(get(cxt, "arg/x1").equals(1.1));
		assertTrue(asis(cxt, "arg/x1").equals(1.1));

		// aliasing with an reactive value entry - rvEnt
		put(cxt, rvEnt("arg/x1", value(cxt, "arg/x5")));
		assertTrue(get(cxt, "arg/x1").equals(1.5));

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
		assertTrue(value(in).equals(10.0));

		Entry<Double> out = outEnt("arg/y", 20.0);
		assertTrue(path(out).equals("arg/y"));
		assertTrue(value(out).equals(20.0));

		Entry<Double> inout = inoutEnt("arg/z", 30.0);
		assertTrue(path(inout).equals("arg/z"));
		assertTrue(value(inout).equals(30.0));

	}


	@Test
	public void inputsOutputsOfContexts() throws Exception {

		// PositionaContext has paths and indexes
		Context<Double> cxt = context(ent("arg/x1", 1.1), inEnt("arg/x2", 1.2),
				inEnt("arg/x3", 1.3), inEnt("arg/x4", 1.4), inEnt("arg/x5", 1.5));

		add(cxt, ent("arg/x6", 1.6));
		add(cxt, outEnt("out/y1", 1.7));
		add(cxt, outEnt("out/y2", 1.8));
		add(cxt, inoutEnt("par/z", 1.9));

		assertTrue(cxt instanceof Context);

		// return the value at index 1 and 6 in cxt
		assertTrue(get(cxt, 1).equals(1.1));
		assertTrue(get(cxt, 6).equals(1.6));

		// return the value at position 1 and 6 in cxt
		assertTrue(getAt(cxt, 1).equals(1.1));
		assertTrue(getAt(cxt, 6).equals(1.6));

		// return selected values at given positions in cxt
		assertEquals(select(cxt, 2, 4, 5), list(1.2, 1.4, 1.5));

		// return all values of inEntries
		assertEquals(inValues(cxt), list(1.5, 1.4, 1.3, 1.2, 1.9));

		// return all paths of inEntries
		assertEquals(inPaths(cxt), list("arg/x5", "arg/x4", "arg/x3", "arg/x2", "par/z"));

		// return all values of outEntries
		assertEquals(outValues(cxt), list(1.8, 1.7, 1.9));

		// return all paths of outEntries
		assertEquals(outPaths(cxt), list("out/y2", "out/y1", "par/z"));

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

		mark(cxt, "arg/y", "dnt|engineering|text|mesh");
		assertTrue(getAt(cxt, "dnt|engineering|text|mesh").equals(1.8));

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
				inEnt("arg1/value", 90.0),
				inEnt("arg2/value", 110.0));

		Context mc = context("multiply",
				inEnt("arg1/value", 10.0),
				inEnt("arg2/value", 70.0));

		Context lc = context("invoke");

		link(lc, "add", ac);
		link(lc, "multiply", mc);

		ac = context(getLink(lc, "add"));
		mc = context(getLink(lc, "multiply"));

		assertEquals(value(ac, "arg1/value"), 90.0);
		assertEquals(value(mc, "arg2/value"), 70.0);

		assertEquals(value(lc, "add/arg1/value"), 90.0);
		assertEquals(value(lc, "multiply/arg2/value"), 70.0);

	}


	@Test
	public void sharedContext() throws Exception {

		// two contexts ac and mc sharing arg1/value 
		// and arg3/value values over the network
		Context ac = context("add",
				inEnt("arg1/value", 90.0),
				inEnt("arg2/value", 110.0),
				inEnt("arg3/value", 100.0));

		// make arg1/value persistent
		URL a1vURL = storeArg(ac, "arg1/value");

		// make arg1/value in mc the same as in ac
		Context mc = context("multiply",
				dbInEnt("arg1/value", a1vURL),
				inEnt("arg2/value", 70.0),
				inEnt("arg3/value", 200.0));

		// sharing arg1/value from mc in ac
		assertTrue(value(ac, "arg1/value").equals(90.0));
		assertTrue(value(mc, "arg1/value").equals(90.0));

		put(mc, "arg1/value", 200.0);
		assertTrue(value(ac, "arg1/value").equals(200.0));
		assertTrue(value(mc, "arg1/value").equals(200.0));

		// sharing arg3/value from ac in mc
		assertTrue(value(ac, "arg3/value").equals(100.0));
		assertTrue(value(mc, "arg3/value").equals(200.0));
		URL a3vURL = storeArg(mc, "arg3/value");
		add(ac, ent("arg3/value", a3vURL));

		put(ac, "arg1/value", 300.0);
		assertTrue(value(ac, "arg1/value").equals(300.0));
		assertTrue(value(mc, "arg1/value").equals(300.0));

	}


	@Test
	public void entModeling() throws Exception {

		// create a new entModel as a regular context 
		// or convert any context to entModel(<context>)
		Context<Double> cxt = entModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
				ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

		add(cxt, ent("arg/x6", 6.0));
		assertTrue(value(cxt, "arg/x6").equals(6.0));

		// ent is of the Evaluation tpe
		put(cxt, ent("arg/x6", ent("overwrite", 20.0)));
		assertTrue(value(cxt, "arg/x6").equals(20.0));

		// invoker is of the Invocation type
		add(cxt, ent("arg/x7", invoker("x1 + x3", ents("x1", "x3"))));
		assertTrue(value(cxt, "arg/x7").equals(4.0));

	}

	@Test
	public void modelingTarget() throws Exception {

		Context<Double> cxt = entModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
				ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

		add(cxt, ent("invoke", invoker("x1 + x3", ents("x1", "x3"))));

		// evaluate the model
        addResponse(cxt, "invoke");
		value(cxt);
		assertTrue(value(cxt).equals(4.0));

		// evaluate the model with overwritten inputs
		Double result = value(cxt, ent("arg/x1", 2.0), ent("arg/x2", 3.0));
		assertTrue(result.equals(5.0));


		// evaluate the model with new inputs
		add(cxt, ent("invoke", invoker("x6 * x7 + x1", ents("x1", "x6", "x7"))));
		result = value(cxt, ent("arg/x6", 6.0), ent("arg/x7", 7.0));
		assertTrue(result.equals(44.0));

	}

	@Test
	public void contextDependencies() throws Exception {

		Context<Double> cxt1 = entModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
				ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));
		add(cxt1, ent("y1", invoker("x1 + x3", ents("x1", "x3"))));
		add(cxt1, ent("y2", invoker("x4 * x5", ents("x1", "x3"))));

		// cxt2 depends on values y1 and y2 calculated in cxt1
		Context<Double> cxt2 = entModel(ent("arg/y3", 8.0), ent("arg/y4", 9.0), ent("arg/y5", 10.0));
		add(cxt2, ent("invoke", invoker("y1 + y2 + y4 + y5", ents("y1", "y2", "y4", "y5"))));
        addResponse(cxt2, "invoke");

		// created dependency of cxt2 on cxt1 via a context copier
		Copier cp = copier(cxt1, ents("arg/x1", "arg/x2"), cxt2, ents("y1", "y2"));
		dependsOn(cxt2, cp);

		Double result = value(cxt2);
		assertTrue( value(cxt2).equals(22.0));

	}

	@Test
	public void evaluateMultiResponseModel() throws Exception {
		Context cxt = entModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
				ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

		add(cxt, ent("add", invoker("x1 + x3", ents("x1", "x3"))));

		add(cxt, ent("multiply", invoker("x4 * x5", ents("x4", "x5"))));

		// two responses declared
        addResponse(cxt, "add", "multiply");

		// evaluate the model
		Context result = (Context) value(cxt);

		logger.info("result: " + result);
		assertTrue(result.equals(context(ent("add", 4.0), ent("multiply", 20.0))));
	}

    @Test
    public void exertEntModel() throws Exception {

        Context cxt = entModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
                ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

        add(cxt, ent("add", invoker("x1 + x3", ents("x1", "x3"))));

        add(cxt, ent("multiply", invoker("x4 * x5", ents("x4", "x5"))));

        // two responses declared
        addResponse(cxt, "add", "multiply");

        // exert the model
        Context result = exert(cxt);

        logger.info("result: " + responses(result));
//		assertTrue(result.equals(context(ent("add", 4.0), ent("multiply", 20.0))));
    }

    @Test
    public void exertModelAsContext() throws Exception {
        
        Model m = model(sig("add", AdderImpl.class),
                inEnt("arg/x1", 1.0), inEnt("arg/x2", 2.0),
                ent("arg/x3", 3.0), ent("arg/x4", 4.0), ent("arg/x5", 5.0));

        add(m, ent("add", invoker("x1 + x3", ents("x1", "x3"))));

        add(m, ent("multiply", invoker("x4 * x5", ents("x4", "x5"))));

        // two responses declared
        addResponse(m, "add", "multiply", "result/value");
        // exert the model
        Model model = exert(m);
        // logger.info("model: " + model);
        logger.info("result: " + responses(model));
        
        assertTrue(response(model, "add").equals(4.0));
                
		assertTrue(responses(model).equals(
                context(ent("add", 4.0), ent("multiply", 20.0), ent("result/value", 3.0))));
        
    }

    @Test
    public void exertServiceModel() throws Exception {

        Model m = model(sig("execute", ServiceModeler.class),
                inEnt("multiply/x1", 10.0), inEnt("multiply/x2", 50.0),
                inEnt("add/x1", 20.0), inEnt("add/x2", 80.0),
                ent(sig("multiply", MultiplierImpl.class, result("subtract/x1", Signature.Direction.IN))),
                ent(sig("add", AdderImpl.class, result("subtract/x2", Signature.Direction.IN))),
                ent(sig("subtract", SubtractorImpl.class, result("model/response", from("subtract/x1", "subtract/x2")))));

        dependsOn(m, "subtract", paths("multiply", "add"));
        logger.info("dependentPaths: " + dependentPaths(m));
        
    }
}