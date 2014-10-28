package junit.sorcer.core.signature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.eo.operator.*;

import java.rmi.RMISecurityManager;
import java.util.logging.Logger;

import junit.sorcer.core.provider.Adder;
import junit.sorcer.core.provider.AdderImpl;

import org.junit.Ignore;
import org.junit.Test;

import sorcer.core.provider.Jobber;
import sorcer.service.ContextException;
import sorcer.service.ExertionException;
import sorcer.service.Signature;
import sorcer.service.SignatureException;
import sorcer.util.Sorcer;


/**
 * @author Mike Sobolewski
 */

public class SignatureTest {
	private final static Logger logger = Logger
			.getLogger(SignatureTest.class.getName());

	static {
		System.setProperty("java.util.logging.config.file",
				Sorcer.getHome() + "/configs/sorcer.logging");
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new RMISecurityManager());
		Sorcer.setCodeBase(new String[] { "arithmetic-beans.jar" });
	}
	
	@Ignore
	@Test
	public void netProviderTest() throws SignatureException  {
		Signature s3 = sig("add", Adder.class);
		logger.info("provider of s3: " + provider(s3));
		assertTrue(provider(s3) instanceof Adder);
	}
	
	@Test
	public void deploySigTest() throws SignatureException  {
		Signature deploySig = sig("service", Jobber.class, "Jobber", deploy(idle(1)));
		logger.info("deploySig: " + deploySig);
		assertEquals(deploySig.getProviderName(), Sorcer.getActualName("Jobber"));
		assertEquals(deploySig.getSelector(), "service");
	}
	
}