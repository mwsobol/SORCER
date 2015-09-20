package sorcer.provider.exchange;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.provider.exchange.impl.ExchangeProviderImpl;
import sorcer.service.Task;
import sorcer.util.ProviderLookup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static sorcer.co.operator.ent;
import static sorcer.eo.operator.*;

@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/calculate")
public class Exchange2Tasks {
	private final static Logger logger = LoggerFactory.getLogger(Exchange2Tasks.class);
	static final int LENGTH = 256;
	static final int ITERATIONS = 10000;

	@Test
	public void evaluate1LocalContextTask() throws Exception {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeProviderImpl.class),
				cxt(ent("input", intArray()),
						result("output")));

		int[] out = (int[]) value(ex);
		logger.info("out: " + Arrays.toString(out));
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end - start) + " ms");
		assertEquals(out.length, 1001);
	}

	@Test
	public void evaluateLocalContextTask() throws Exception  {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeProviderImpl.class),
				cxt(ent("input", intArray()),
						result("output")));

		int[] out = null;
		for (int n = 0; n < ITERATIONS; n++) {
			out = (int[]) value(ex);
//			logger.info("out: " + Arrays.toString(out));
		}
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end-start) + " ms");
		assertEquals(out.length, 1001);
	}

	@Test
	public void getProxy() throws Exception {
		long start = System.currentTimeMillis();
		Object ex = new ProviderLookup().getService(Exchange.class);
//		Object ex = provider(sig("exchange", Exchange.class));
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end - start) + " ms");
		assertNotNull(ex);
	}

	@Test
	public void evaluate1RemoteContextTask() throws Exception {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeRemote.class),
				cxt(ent("input", intArray()),
						result("output")));

		int[] out = (int[]) value(ex);
		logger.info("out: " + Arrays.toString(out));
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end - start) + " ms");
		assertEquals(out.length, 1001);
	}

	@Test
	public void evaluate123LocalContextTask() throws Exception {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeProviderImpl.class),
				cxt(ent("input", new int[] {1, 2, 3} ),
						result("output")));

		int[] out = (int[]) value(ex);
//		logger.info("out: " + Arrays.toString(out));
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end - start) + " ms");
		assertEquals(out.length, 1001);
	}

	@Test
	public void evaluate123RemoteContextTask() throws Exception {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeRemote.class),
				cxt(ent("input", new int[] {1, 2, 3} ),
						result("output")));

		int[] out = (int[]) value(ex);
//		logger.info("out: " + Arrays.toString(out));
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end - start) + " ms");
		assertEquals(out.length, 1001);
	}


	@Test
	public void evaluateRemoteContextTask() throws Exception  {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeRemote.class),
				cxt(ent("input", intArray()),
						result("output")));

		int[] out = null;
		for (int n = 0; n < ITERATIONS; n++) {
			out = (int[]) value(ex);
//			logger.info("out: " + Arrays.toString(out));
		}
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end-start) + " ms");
		assertEquals(out.length, 1001);
	}

	@Test
	public void evaluateRemoteContextSmartProxyTask() throws Exception  {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeRemote.class, prvName("Smart Exchange")),
				cxt(ent("input", intArray()),
						result("output")));

		int[] out = null;
		for (int n = 0; n < ITERATIONS; n++) {
			out = (int[]) value(ex);
//			logger.info("out: " + Arrays.toString(out));
		}
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end-start) + " ms");
		assertEquals(out.length, 1001);
	}

	@Test
	public void evaluateLocalArgTask() throws Exception  {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeProviderImpl.class),
				context(
						parameterTypes(int[].class),
						args(intArray()),
						result("output")));

		int[] out = null;
		for (int n = 0; n < ITERATIONS; n++) {
			out = (int[]) value(ex);
//			logger.info("out: " + Arrays.toString(out));
		}
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end-start) + " ms");
		assertEquals(out.length, 1001);
	}

	@Test
	public void evaluateRemoteArgTask() throws Exception  {
		long start = System.currentTimeMillis();
		Task ex = task(sig("exchange", ExchangeRemote.class),
				context(
						parameterTypes(int[].class),
						args(intArray()),
						result("output")));

		int[] out = null;
		for (int n = 0; n < ITERATIONS; n++) {
			out = (int[]) value(ex);
//			logger.info("out: " + Arrays.toString(out));
		}
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end-start) + " ms");
		assertEquals(out.length, 1001);
	}

	private int[] intArray() {
		int[] source = new int[LENGTH];
		for (int n = 0; n < LENGTH; n++) {
			source[n] = 2 * n + 1;
		}
		return source;
	}

	@Test
	public void ipcIntegerArrayTest() throws IOException {
		long start = System.currentTimeMillis();
		int[] out = ipcIntArray(intArray());
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end-start) + " ms");
		logger.info("read array: " + Arrays.toString(out));
	}

	public int[] ipcIntArray(int[] in) throws IOException {
		int BB_SIZE = 1024;
		ByteBuffer bb = ByteBuffer.allocate(BB_SIZE);
		IntBuffer ib = bb.asIntBuffer();
		ib.put(in);

		SocketChannel channel = SocketChannel.open();
		channel.connect(new InetSocketAddress("127.0.0.1", 9001));
		bb.clear();
		while (bb.hasRemaining()) {
			channel.write(bb);
		}
//		logger.info("written array: " + Arrays.toString(bb.array()));
		bb.clear();
		int readBytes = 0;
		while (readBytes != -1 && readBytes < BB_SIZE) {
			readBytes = channel.read(bb);
		}
//		logger.info("read bytes: " + readBytes);
//		logger.info("read array: " + Arrays.toString(bb.array()));

		int[] out = new int[LENGTH];
		for (int i = 0; i<LENGTH; i++) {
			out[i] = ib.get(i);
		}
		channel.close();
		return out;
	}


}
	
	
