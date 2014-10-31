package sorcer.worker.tests;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import net.jini.core.transaction.TransactionException;

import org.junit.Before;
import org.junit.Test;

import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.ObjectTask;
import sorcer.core.signature.ObjectSignature;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.SignatureException;
import sorcer.util.Log;
import sorcer.worker.provider.Work;
import sorcer.worker.provider.impl.InvalidWork;
import sorcer.worker.provider.impl.WorkerProvider;

/**
 * @author Mike Sobolewski
 * 
 */
@SuppressWarnings("rawtypes")
public class WorkerTaskRequestorTest {
	private static Logger logger = Log.getTestLog();
	
	private Context context;
	private String hostname;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		hostname = InetAddress.getLocalHost().getHostName();

		Work work = new Work() {
			public Context exec(Context cxt) throws InvalidWork, ContextException {
				int arg1 = (Integer)cxt.getValue("requestor/operand/1");
				int arg2 = (Integer)cxt.getValue("requestor/operand/2");
				cxt.putOutValue("provider/result", arg1 * arg2);
				return cxt;
			}
		};

		context = new ServiceContext("work");
		context.putValue("requestor/name", hostname);
		context.putValue("requestor/operand/1", 11);
		context.putValue("requestor/operand/2", 101);
		context.putValue("requestor/work", work);
		context.putValue("to/provider/name", "Testing Provider");
	}

	@Test
	public void providerResultTest() throws RemoteException, ContextException, TransactionException,
	ExertionException, UnknownHostException, SignatureException {

		ObjectSignature signature = new ObjectSignature("doWork", WorkerProvider.class);

		Exertion task = new ObjectTask("work", signature, context);
		task = task.exert();
		//logger.info("result: " + task);
		assertEquals((Integer)task.getContext().getValue("provider/result"), new Integer(1111));
	}

	@Test
	public void providerMessageTest() throws RemoteException, ContextException, TransactionException,
	ExertionException, UnknownHostException, SignatureException {

		ObjectSignature signature = new ObjectSignature("doWork", WorkerProvider.class);

		Exertion task = new ObjectTask("work", signature, context);
		task = task.exert();
		//logger.info("result: " + task);
		assertEquals(task.getContext().getValue("provider/message"), 
				"Done work by: class sorcer.ex2.provider.WorkerProvider");
	}

	@Test
	public void providerHostNameTest() throws RemoteException, ContextException, TransactionException,
	ExertionException, UnknownHostException, SignatureException {

		ObjectSignature signature = new ObjectSignature("doWork", WorkerProvider.class);
		Exertion task = new ObjectTask("work", signature, context);
		task = task.exert();
		//logger.info("result: " + task);
		assertEquals(task.getContext().getValue("provider/host/name"), hostname);
	}
}
