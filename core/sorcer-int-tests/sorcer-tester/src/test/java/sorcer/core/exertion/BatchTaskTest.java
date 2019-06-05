package sorcer.core.exertion;

//import com.gargoylesoftware,base,testing,TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.arithmetic.tester.provider.impl.AdderImpl;
import sorcer.arithmetic.tester.provider.impl.MultiplierImpl;
import sorcer.arithmetic.tester.provider.impl.SubtractorImpl;
import sorcer.co.operator;
import sorcer.service.Signature;
import sorcer.service.Signature.Direction;
import sorcer.service.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;
import static sorcer.so.operator.exert;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("unchecked")
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class BatchTaskTest {
	private final static Logger logger = LoggerFactory.getLogger(BatchTaskTest.class);
	
	@Test
	public void batchTaskTest() throws Exception {
		// batch for the composition f1(f2(f3((x1, x2), f4(x1, x2)), f5(x1, x2))
		// shared context with named paths
		Task batch3 = task("batch",
				type(sig("multiply", MultiplierImpl.class, result("subtract/x1", Direction.IN)), Signature.PRE),
				type(sig("add", AdderImpl.class, result("subtract/x2", Direction.IN)), Signature.PRE),
				sig("subtract", SubtractorImpl.class, result("result/y", inPaths("subtract/x1", "subtract/x2"))),
				context(operator.inVal("multiply/x1", 10.0), operator.inVal("multiply/x2", 50.0),
						operator.inVal("add/x1", 20.0), operator.inVal("add/x2", 80.0)));
		
		logger.info("task getSelects:" + fi(batch3));
				
		batch3 = exert(batch3);
//		//logger.info("task result/y: " + getValue(batch3, "result/y"));
//		assertEquals("Wrong eval for 400.0", getValue(batch3, "result/y"), 400.0);
	}
	
	
	@Test
	public void prefixedBatchTaskTest() throws Exception {
		// batch for the composition f1(f2(f3((x1, x2), f4(x1, x2)), f5(x1, x2))
		// shared context with prefixed paths
		Task batch3 = task("batch",
				type(sig("multiply#op1", MultiplierImpl.class, result("op3/x1", Direction.IN)), Signature.PRE),
				type(sig("add#op2", AdderImpl.class, result("op3/x2", Direction.IN)), Signature.PRE),
				sig("subtract", SubtractorImpl.class, result("result/y", inPaths("op3/x1", "op3/x2"))),
				context(operator.inVal("op1/x1", 10.0), operator.inVal("op1/x2", 50.0),
						operator.inVal("op2/x1", 20.0), operator.inVal("op2/x2", 80.0)));
		
		batch3 = exert(batch3);
		//logger.info("task result/y: " + getValue(batch3, "result/y"));
		assertEquals("Wrong eval for 400.0", get(batch3, "result/y"), 400.0);
	}
}
	
