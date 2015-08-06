package sorcer.provider.exchange;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.provider.Provider;
import sorcer.provider.exchange.impl.ExchangeBean;
import sorcer.service.Task;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static sorcer.co.operator.ent;
import static sorcer.eo.operator.*;

@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/calculate-service")
public class ExchangeTasks {
	private final static Logger logger = LoggerFactory.getLogger(ExchangeTasks.class);
	static final int LENGTH = 1001;
	static final int ITERATIONS = 10000;

	@Test
	public void evaluate1LocalContextTask() throws Exception {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeBean.class),
				cxt(ent("input", intArray()),
						result("output")));

		int[] out = (int[]) value(ex);
//		logger.info("out: " + Arrays.toString(out));
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end - start) + " ms");
	}

	@Test
	public void evaluateLocalContextTask() throws Exception  {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeBean.class),
				cxt(ent("input", intArray()),
						result("output")));

		for (int n = 0; n < ITERATIONS; n++) {
			int[] out = (int[]) value(ex);
//			logger.info("out: " + Arrays.toString(out));
			Arrays.sort(out);
		}
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end-start) + " ms");
	}

	@Test
	public void getProxy() throws Exception {
		long start = System.currentTimeMillis();
		Provider ex = (Provider)provider(sig("exchange", Exchange.class));
		long end = System.currentTimeMillis();
		assertNotNull(ex);
		logger.info("Execution time: " + (end - start) + " ms");
	}

	@Test
	public void evaluate1RemoteContextTask() throws Exception {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", Exchange.class),
				cxt(ent("input", intArray()),
						result("output")));

		int[] out = (int[]) value(ex);
//		logger.info("out: " + Arrays.toString(out));
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end - start) + " ms");
	}

	@Test
	public void evaluate123LocalContextTask() throws Exception {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeBean.class),
				cxt(ent("input", new int[] {1, 2, 3} ),
						result("output")));

		int[] out = (int[]) value(ex);
//		logger.info("out: " + Arrays.toString(out));
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end - start) + " ms");
	}

	@Test
	public void evaluate123RemoteContextTask() throws Exception {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", Exchange.class),
				cxt(ent("input", new int[] {1, 2, 3} ),
						result("output")));

		int[] out = (int[]) value(ex);
//		logger.info("out: " + Arrays.toString(out));
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end - start) + " ms");
	}


	@Test
	public void evaluateRemoteContextTask() throws Exception  {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", Exchange.class),
				cxt(ent("input", intArray()),
						result("output")));

		for (int n = 0; n < ITERATIONS; n++) {
			int[] out = (int[]) value(ex);
//			logger.info("out: " + Arrays.toString(out));
			Arrays.sort(out);
		}
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end-start) + " ms");
	}

	@Test
	public void evaluateLocalArgTask() throws Exception  {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeBean.class),
				context(
						parameterTypes(int[].class),
						args(intArray()),
						result("output")));

		for (int n = 0; n < ITERATIONS; n++) {
			int[] out = (int[]) value(ex);
//			logger.info("out: " + Arrays.toString(out));
			Arrays.sort(out);
		}
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end-start) + " ms");
	}

	@Test
	public void evaluateRemoteArgTask() throws Exception  {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", Exchange.class),
				context(
						parameterTypes(int[].class),
						args(intArray()),
						result("output")));

		for (int n = 0; n < ITERATIONS; n++) {
			int[] out = (int[]) value(ex);
//			logger.info("out: " + Arrays.toString(out));
			Arrays.sort(out);
		}
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end-start) + " ms");
	}

	private int[] intArray() {
		int[] source = new int[LENGTH];
		for (int n = 0; n < LENGTH; n++) {
			source[n] = 2 * n + 1;
		}
		return source;
	}
}
	
	
