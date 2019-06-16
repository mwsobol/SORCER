package sorcer.core.context;
 

import org.junit.Test;
import sorcer.co.operator;
import sorcer.service.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.*;
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
		addContext.putInValue("arg1/eval", 90.0);
		addContext.putInValue("arg2/eval", 110.0);
		
		Context multiplyContext = new PositionalContext("multiply");
		multiplyContext.putInValue("arg1/eval", 10.0);
		multiplyContext.putInValue("arg2/eval", 70.0);
		
		ServiceContext invokeContext = new ServiceContext("invoke");
//		add additional tests with offset
//		invokeContext.putLink("add", addContext, "offset");
//		invokeContext.putLink("multiply", multiplyContext, "offset");
		
		invokeContext.putLink("add", addContext);
		invokeContext.putLink("multiply", multiplyContext);
		
		ContextLink addLink = (ContextLink)invokeContext.getLink("add");
		ContextLink multiplyLink = (ContextLink)invokeContext.getLink("multiply");
		
//		logger.info("invoke context: " + invokeContext);

//		logger.info("returnPath arg1/eval: " + addLink.getContext().execute("arg1/eval"));
		assertEquals(addLink.getContext().getValue("arg1/eval"), 90.0);
//		logger.info("returnPath arg2/eval: " + multiplyLink.getContext().execute("arg2/eval"));
		assertEquals(multiplyLink.getContext().getValue("arg2/eval"), 70.0);
//		logger.info("returnPath add/arg1/eval: " + invokeContext.execute("add/arg1/eval"));
		assertEquals(invokeContext.getValue("add/arg1/eval"), 90.0);
//		logger.info("returnPath multiply/arg2/eval: " + invokeContext.execute("multiply/arg2/eval"));
		assertEquals(invokeContext.getValue("multiply/arg2/eval"), 70.0);

	}
	
	@Test
	public void softValueTest() throws Exception {
		Context cxt = context("add", operator.inVal("arg/x1", 20.0), operator.inVal("arg/x2", 80.0));
		
//		logger.info("arg/x1 = " + cxt.execute("arg/x1"));
		assertEquals(cxt.getValue("arg/x1"), 20.0);
//		logger.info("val x1 = " + cxt.execute("x1"));
		assertEquals(cxt.getValue("x1"), null);
//		logger.info("weak x1 = " + cxt.getSoftValue("arg/var/x1"));
		assertEquals(cxt.getSoftValue("arg/var/x1"), 20.0);
	}
}
