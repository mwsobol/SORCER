package sorcer.provider.adder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.requestor.ExertRequestor;
import sorcer.provider.adder.impl.AdderImpl;
import sorcer.requestor.adder.AdderRequestor;
import sorcer.service.*;
import sorcer.service.modeling.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.eo.operator.get;
import static sorcer.eo.operator.value;
import static sorcer.mo.operator.response;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/service")
public class ServiceRequestor {
	private final static Logger logger = LoggerFactory.getLogger(ServiceRequestor.class);

	@Test
	public void adderRequestor() throws Exception {

		ExertRequestor requestor = new ExertRequestor(AdderRequestor.class, "exertion");

		Context cxt = (Context) requestor.exec();
		logger.info("out context: " + cxt);
//		logger.info("context @ arg/x1: " + get(cxt, "arg/x1"));
//		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
//		logger.info("context @ result/value: " + value(cxt, "result/value"));
//
//		// get a single context argument
//		assertEquals(100.0, value(cxt, "result/value"));
//
//		// get the subcontext output from the context
//		assertTrue(context(ent("arg/x1", 20.0), ent("result/value", 100.0)).equals(
//				subcontext(cxt, paths("arg/x1", "result/value"))));

	}

}
	
	
