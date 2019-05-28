package sorcer.provider.exchange;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.ent.operator;
import sorcer.provider.exchange.impl.ExchangeProviderImpl;
import sorcer.service.Accessor;
import sorcer.service.Signature;
import sorcer.service.Task;
import sorcer.util.ProviderAccessor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static sorcer.so.operator.*;
import static sorcer.eo.operator.*;

@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/exchange")
public class ExchangeTasks {
	private final static Logger logger = LoggerFactory.getLogger(ExchangeTasks.class);
	static final int LENGTH = 256;
	static final int ITERATIONS = 10000;

	private int[] intArray() {
		int[] source = new int[LENGTH];
		for (int n = 0; n < LENGTH; n++) {
			source[n] = 2 * n + 1;
		}
		return source;
	}

	@Test
	public void exchangeInts1Test() throws Exception {
		ExchangeProviderImpl prv = new ExchangeProviderImpl();
		int[] in = intArray();
		long start = System.nanoTime();
		int[] out = prv.exchangeInts(intArray());
		long end = System.nanoTime();
		logger.info("out: " + Arrays.toString(out));
		logger.info("Execution time: " + (end - start)/1000 + " us"); 						// 14 us
		assertEquals(out[0], 2);
	}

	@Test
	public void exchangeInts10KTest() throws Exception {
		ExchangeProviderImpl prv = new ExchangeProviderImpl();
		int[] in = intArray();
		long start = System.nanoTime();
		int[] out = null;
		for (int n = 0; n < ITERATIONS; n++) {
			out = prv.exchangeInts(in);
		}
		long end = System.nanoTime();
		logger.info("out: " + Arrays.toString(out));
		logger.info("Execution time: " + (end - start)/ITERATIONS + " ns");  				// 1 us
		assertEquals(out[0], 10001);
	}

	@Test
	public void evaluateTask1Test() throws Exception {
		int[] in = intArray();
		Task ex = task(sig("exchangeInts", ExchangeProviderImpl.class),
						cxt(operator.ent("values", in),
						result("values")));

		long start = System.currentTimeMillis();
		int[] out = (int[]) exec(ex);
		logger.info("out: " + Arrays.toString(out));
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end - start) + " ms"); 							// 25 ms
		assertEquals(out[0], 2);
	}

	@Test
	public void evaluateArgTask1Test() throws Exception  {
		int[] in = intArray();
		Task ex = task(sig("exchangeInts", ExchangeProviderImpl.class),
				context(
						types(int[].class),
						args(in),
						result("values")));

		long start = System.currentTimeMillis();
		int[] out = (int[]) exec(ex);
		logger.info("out: " + Arrays.toString(out));
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end - start) + " ms"); 							// 25 ms
		assertEquals(out[0], 2);
	}

	@Test
	public void evaluateLocalContextTask() throws Exception  {
		int[] in = intArray();
		Task ex = task(sig("exchangeInts", ExchangeProviderImpl.class),
					cxt(operator.ent("values", in),
						result("values")));

		int[] out = null;
		long start = System.nanoTime();
		for (int n = 0; n < ITERATIONS; n++) {
			out = (int[]) exec(ex);
		}
		long end = System.nanoTime();
		logger.info("out: " + Arrays.toString(out));
		logger.info("Execution time: " + (end-start)/ITERATIONS/1000 + " us"); 				// 80 us
		assertEquals(out[0], 10001);
	}

	@Test
	public void getExchangeProxy() throws Exception {
		long start = System.currentTimeMillis();
		ProviderAccessor da = (ProviderAccessor)Accessor.get();
		Object ex = da.getProvider(sig(ExchangeRemote.class, prvName("Exchange")));
		long end = System.currentTimeMillis();
		logger.info("first time: " + (end - start) + " ms");								// 957 ms
		start = System.currentTimeMillis();
		ex = da.getProvider(sig(ExchangeRemote.class, prvName("Exchange")));
		end = System.currentTimeMillis();
		logger.info("second time: " + (end - start) + " ms");								// 10 ms
		assertNotNull(ex);
	}

	@Test
	public void evaluate1RMICallTask() throws Exception {
		ProviderAccessor da = (ProviderAccessor)Accessor.get();
		ExchangeRemote ex = (ExchangeRemote)da.getProvider(sig(ExchangeRemote.class, prvName("Exchange")));
		int[] in = intArray();

		long start = System.currentTimeMillis();
		int[] out = ex.exchangeInts(in);
		long end = System.currentTimeMillis();
		logger.info("out: " + Arrays.toString(out));
		logger.info("Execution time: " + (end - start) + " ms");							// 5 ms
		assertEquals(out[0], 2);
	}

	@Test
	public void evaluate1RemoteContextTask() throws Exception {
		int[] in = intArray();
		Task ex = task(sig("exchangeInts", ExchangeRemote.class, prvName("Exchange")),
					cxt(operator.ent("values", in), result("values")));

		long start = System.currentTimeMillis();
		int[] out = (int[]) exec(ex);
		long end = System.currentTimeMillis();
		logger.info("out: " + Arrays.toString(out));
		logger.info("Execution time: " + (end - start) + " ms");							// 702 ms
		assertEquals(out[0], 2);
	}

	@Test
	public void evaluate123LocalContextTask() throws Exception {
		Task ex = task(sig("exchangeInts", ExchangeProviderImpl.class),
					cxt(operator.ent("values", new int[] {1, 2, 3} ),
						result("values")));

		long start = System.currentTimeMillis();
		int[] out = (int[]) exec(ex);
		long end = System.currentTimeMillis();
		logger.info("out: " + Arrays.toString(out));
		logger.info("Execution time: " + (end - start) + " ms");							// 25 ms
		assertEquals(out[0], 2);
	}

	@Test
	public void evaluate123RemoteContextTask() throws Exception {
		Task ex = task(sig("exchangeInts", ExchangeRemote.class),
					cxt(operator.ent("values", new int[] {1, 2, 3} ),
						result("values")));

		long start = System.currentTimeMillis();
		int[] out = (int[]) exec(ex);
		long end = System.currentTimeMillis();
		logger.info("out: " + Arrays.toString(out));
		logger.info("Execution time: " + (end - start) + " ms");							// 693 ms
		assertEquals(out[0], 2);
	}

	@Test
	public void evaluateRemoteContext10KTask() throws Exception  {
		int[] in = intArray();
		Task ex = task(sig("exchangeInts", ExchangeRemote.class, prvName("Exchange")),
				cxt(operator.ent("values", in), result("values")));

		int[] out = null;
		long start = System.currentTimeMillis();
		for (int n = 0; n < ITERATIONS; n++) {
			out = (int[]) exec(ex);
		}
		long end = System.currentTimeMillis();
		logger.info("out: " + Arrays.toString(out));
		logger.info("Execution time: " + (end-start)/ITERATIONS + " ms");					// 5 ms
		assertEquals(out[0], 2);
	}

	@Test
	public void evaluateRemoteContextSmart10KTask() throws Exception  {
		int[] in = intArray();
		Task ex = task(sig("exchangeInts", ExchangeRemote.class, prvName("Smart Exchange")),
				cxt(operator.ent("values", in),
						result("values")));

		int[] out = null;
		long start = System.nanoTime();
		for (int n = 0; n < ITERATIONS; n++) {
			out = (int[]) exec(ex);
		}
		long end = System.nanoTime();
		logger.info("out: " + Arrays.toString(out));
		logger.info("Execution time: " + (end-start)/ITERATIONS/1000 + " us");				// 138 us
		assertEquals(out[0], 10001);
	}

	@Test
	public void evaluateLocalArgTask() throws Exception  {
		int[] in = intArray();
		Task ex = task(sig("exchangeInts", ExchangeProviderImpl.class),
				context(
						types(int[].class),
						args(in),
						result("values")));

		int[] out = null;
		long start = System.nanoTime();
		for (int n = 0; n < ITERATIONS; n++) {
			out = (int[]) exec(ex);
		}
		long end = System.nanoTime();
		logger.info("out: " + Arrays.toString(out));
		logger.info("Execution time: " + (end-start)/ITERATIONS/1000 + " us");				// 79 us
		assertEquals(out[0], 10001);
	}

	@Test
	public void evaluateRemoteArgTask() throws Exception  {
		int[] in = intArray();
		Task ex = task(sig("exchangeInts", ExchangeRemote.class,  prvName("Exchange")),
				context(
						types(int[].class),
						args(in),
						result("values")));

		int[] out = null;
		long start = System.currentTimeMillis();
		for (int n = 0; n < ITERATIONS; n++) {
			out = (int[]) exec(ex);
		}
		long end = System.currentTimeMillis();
		logger.info("out: " + Arrays.toString(out));
		logger.info("Execution time: " + (end-start)/ITERATIONS + " ms");   			// 5 ms
		assertEquals(out[0], 2);

	}

	@Test
	public void ipcSigleIntegerArrayTest() throws IOException {
		int[] in = intArray();
		long start = System.currentTimeMillis();
		int[] out = ipcIntArray(in);
		long end = System.currentTimeMillis();											// read-write 330 us
		logger.info("Execution time: " + (end-start) + " ms");							// 4 ms
		logger.info("read array: " + Arrays.toString(out));
	}

	@Test
	public void ipc10KIntegerArrayNanoTest() throws IOException {
		int[] in = intArray();
		long start = System.nanoTime();
		int[] out = null;
		for (int i = 0; i < ITERATIONS; i++) {
			// the local ipc this test prc
			out = ipcIntArray(in);
		}
		long end = System.nanoTime();
		long timing = end-start;														// read-write 60 us
		logger.info("rt time: " + timing/1000/ITERATIONS + " us");						// 439 us
		logger.info("read array: " + Arrays.toString(out));
	}

	@Test
	public void ipcSmartIntegerArrayProxyTest() throws Exception {
		ProviderAccessor da = (ProviderAccessor)Accessor.get();
		IpcArray ia = (IpcArray)da.getService(sig(IpcArray.class));
		int[] in = intArray();
		long start = System.nanoTime();
		int[] out = ia.ipcIntegerArray(in);
		long end = System.nanoTime();
		logger.info("First execution time: " + (end-start)/1000 + " us");				// 2 ms
		logger.info("read array: " + Arrays.toString(out));
		assertEquals(out[0], 2);

		in = intArray();
		start = System.nanoTime();
		out = ia.ipcIntegerArray(in);
		end = System.nanoTime();
		logger.info("Second execution time: " + (end-start)/1000 + " us");           	// 308 us
		logger.info("read array: " + Arrays.toString(out));
		ia.close();
	}

	@Test
	public void ipcSmartIntegerArrayTest() throws Exception {
		Signature ipcSig = sig("ipcIntegerArray", IpcArray.class);
		Task ipc = task(ipcSig,
					context(types(int[].class),
						args(intArray()),
						result("output")));

		long start = System.currentTimeMillis();
		int[] out = (int[]) exec(ipc);
		long end = System.currentTimeMillis();
		logger.info("Execution time: " + (end-start) + " ms");                        	// 464 ms
		logger.info("read array: " + Arrays.toString(out));
		assertEquals(out[0], 2);
		ipcSig.close();
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
		long start = System.nanoTime();
		int readBytes = 0;
		while (readBytes != -1 && readBytes < BB_SIZE) {
			readBytes = channel.read(bb);
		}
		long end = System.nanoTime();
		logger.info("read/write time: " + (end-start)/1000 + " us");

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
	
	
