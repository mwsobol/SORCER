package sorcer.worker.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import sorcer.core.context.ServiceContext;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.util.Log;
import sorcer.worker.provider.InvalidWork;
import sorcer.worker.provider.Work;
import sorcer.worker.provider.impl.WorkerProvider;

/**
 * @author Mike Sobolewski
 *
 */
public class WorkerProviderTest {
	private static Logger logger = Log.getTestLog();
	
	String hostName;
	Context context;
	WorkerProvider provider;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		hostName = InetAddress.getLocalHost().getHostName();
		provider = new WorkerProvider();

		Work work = new Work() {
            public Context exec(Context cxt) throws InvalidWork, ContextException {
                int arg1 = (Integer)cxt.getValue("requestor/operand/1");
                int arg2 = (Integer)cxt.getValue("requestor/operand/2");
                cxt.putOutValue("provider/result", arg1 * arg2);
                return cxt;
            }
        };
        context = new ServiceContext("work");
        context.putValue("requestor/name", hostName);
        context.putValue("requestor/operand/1", 11);
        context.putValue("requestor/operand/2", 21);
        context.putValue("requestor/work", work);
        context.putValue("to/provider/name", "Testing Provider");
	}

	/**
	 * Test method for {@link sorcer.ex2.provider.WorkerProvider#sayHi(sorcer.service.Context)}.
	 * @throws IOException 
	 */
	@Test
	public void testSayHi() throws ContextException, IOException {
		Context result = provider.sayHi(context);
		//logger.info("result: " + result);
		assertTrue(result.getValue("provider/message").equals("Hi " + hostName + "!"));
	}

	/**
	 * Test method for {@link sorcer.ex2.provider.WorkerProvider#sayBye(sorcer.service.Context)}.
	 */
	@Test
	public void testSayBye() throws RemoteException, ContextException {
		Context result = provider.sayBye(context);
		//logger.info("result: " + result);
		assertEquals(result.getValue("provider/message"), "Bye " + hostName + "!");

	}

	/**
	 * Test method for {@link sorcer.ex2.provider.WorkerProvider#doWork(sorcer.service.Context)}.
	 */
	@Test
	public void testDoIt() throws RemoteException, InvalidWork, ContextException {
		Context result = provider.doWork(context);
		//logger.info("result: " + result);
		assertEquals(result.getValue("provider/result"), 231);
	}

}
