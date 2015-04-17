package sorcer.sml.contexts;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.Copier;
import sorcer.core.context.ListContext;
import sorcer.service.Context;
import sorcer.service.modeling.Model;

import java.net.URL;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static sorcer.co.operator.asis;
import static sorcer.co.operator.*;
import static sorcer.co.operator.path;
import static sorcer.co.operator.srv;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.inPaths;
import static sorcer.eo.operator.outPaths;
import static sorcer.eo.operator.put;
import static sorcer.eo.operator.value;
import static sorcer.po.operator.invoker;
/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/sml")
public class EntModels {
	private final static Logger logger = Logger.getLogger(EntModels.class.getName());

	@Test
	public void entryModel() throws Exception {

		// create a new entModel as a regular context 
		// or convert any context to entModel(<context>)
		Model cxt = entModel(ent("arg/x1", 1.0), ent("arg/x2", 2.0),
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

}