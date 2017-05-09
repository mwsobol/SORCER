package sorcer.provider.adder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.requestor.adder.AdderRequestor;
import sorcer.service.Context;
import sorcer.util.ModelTable;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.header;
import static sorcer.co.operator.row;
import static sorcer.co.operator.dataTable;
import static sorcer.eo.operator.*;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/service")
public class ServiceRequestor {
	private final static Logger logger = LoggerFactory.getLogger(ServiceRequestor.class);

	@Test
	public void adderRequestorAsService() throws Exception {

//		ExertRequestor req = new ExertRequestor(AdderRequestor.class, "exertion");
//		Context cxt = (Context) req.exec();

		sorcer.core.requestor.ServiceRequestor req = requestor(AdderRequestor.class, "exertion");
//		sorcer.core.requestor.ServiceRequestor req = requestor(AdderRequestor.class, "netlet");

		Context cxt = (Context) exec(req);

		logger.info("out context: " + cxt);
		logger.info("context @ arg/x1: " + get(cxt, "arg/x1"));
		logger.info("context @ arg/x2: " + value(cxt, "arg/x2"));
		logger.info("context @ out/y: " + value(cxt, "out/y"));

		// get a single context argument
		assertEquals(300.0, value(cxt, "out/y"));


		ModelTable dataTable = dataTable(header("x1", "x2"),
				row(1.0, 0.1),
				row(2.0, 0.2),
				row(3.0, 0.3),
				row(4.0, 0.4));

	}

}
	
	
