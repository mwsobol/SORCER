package sorcer.core.signature;

import static org.junit.Assert.assertTrue;
import static sorcer.eo.operator.provider;
import static sorcer.eo.operator.sig;

import java.util.logging.Logger;

import org.junit.Ignore;
import org.junit.Test;

import sorcer.arithmetic.tester.provider.Adder;
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
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "arithmetic-beans.jar" });
	}
	
	@Ignore
	@Test
	public void netProviderTest() throws SignatureException  {
		Signature s3 = sig("add", Adder.class);
		logger.info("provider of s3: " + provider(s3));
		assertTrue(provider(s3) instanceof Adder);
	}
	
}