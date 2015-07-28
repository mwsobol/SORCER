package sorcer.core.context;
 

import org.junit.Test;
import sorcer.service.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.inEnt;
import static sorcer.eo.operator.context;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ContextTest {
	private final static Logger logger = LoggerFactory.getLogger(ContextTest.class);
	
	@Test
	public void linkedContext() throws Exception {
		Context<Double> addContext = new PositionalContext("add");
		addContext.putInValue("arg1/value", 90.0);
		addContext.putInValue("arg2/value", 110.0);
		
		Context multiplyContext = new PositionalContext("multiply");
		multiplyContext.putInValue("arg1/value", 10.0);
		multiplyContext.putInValue("arg2/value", 70.0);
		
		ServiceContext invokeContext = new ServiceContext("invoke");
//		add additional tests with offset
//		invokeContext.putLink("add", addContext, "offset");
//		invokeContext.putLink("multiply", multiplyContext, "offset");
		
		invokeContext.putLink("add", addContext);
		invokeContext.putLink("multiply", multiplyContext);
		
		ContextLink addLink = (ContextLink)invokeContext.getLink("add");
		ContextLink multiplyLink = (ContextLink)invokeContext.getLink("multiply");
		
//		logger.info("invoke context: " + invokeContext);

//		logger.info("path arg1/value: " + addLink.getContext().getValue("arg1/value"));
		assertEquals(addLink.getContext().getValue("arg1/value"), 90.0);
//		logger.info("path arg2/value: " + multiplyLink.getContext().getValue("arg2/value"));
		assertEquals(multiplyLink.getContext().getValue("arg2/value"), 70.0);
//		logger.info("path add/arg1/value: " + invokeContext.getValue("add/arg1/value"));		
		assertEquals(invokeContext.getValue("add/arg1/value"), 90.0);
//		logger.info("path multiply/arg2/value: " + invokeContext.getValue("multiply/arg2/value"));		
		assertEquals(invokeContext.getValue("multiply/arg2/value"), 70.0);

	}
	
	@Test
	public void softValueTest() throws Exception {
		Context cxt = context("add", inEnt("arg/x1", 20.0), inEnt("arg/x2", 80.0));
		
//		logger.info("arg/x1 = " + cxt.getValue("arg/x1"));
		assertEquals(cxt.getValue("arg/x1"), 20.0);
//		logger.info("val x1 = " + cxt.getValue("x1"));
		assertEquals(cxt.getValue("x1"), null);
//		logger.info("weak x1 = " + cxt.getWeakValue("arg/var/x1"));
		assertEquals(cxt.getSoftValue("arg/var/x1"), 20.0);
	}
}
