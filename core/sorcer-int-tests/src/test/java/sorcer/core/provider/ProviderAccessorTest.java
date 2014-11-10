package sorcer.core.provider;

import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import org.junit.Test;

import sorcer.core.SorcerConstants;
import sorcer.core.signature.NetSignature;
import sorcer.service.Accessor;
import sorcer.service.Service;
import sorcer.util.ProviderAccessor;
import sorcer.util.ProviderLocator;
import sorcer.util.ProviderLookup;
import sorcer.util.Sorcer;
import sorcer.util.Stopwatch;

/**
 * @author Mike Sobolewski
 */

public class ProviderAccessorTest implements SorcerConstants {

	private final static Logger logger = Logger
			.getLogger(ProviderAccessorTest.class.getName());

	static {
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "arithmetic-beans.jar" });
		System.setProperty("java.protocol.handler.pkgs", "sorcer.util.url|org.rioproject.url");
		//System.setProperty("java.rmi.server.RMIClassLoaderSpi","org.rioproject.rmi.ResolvingLoader");
	}
	
	@Test
	public void providerAcessorTest() throws Exception {
		long startTime = System.currentTimeMillis();
		Service provider = Accessor.getService(new NetSignature(Jobber.class));
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
		
		startTime = System.currentTimeMillis();
		provider = ProviderAccessor.getProvider(Jobber.class);
//		logger.info("ProviderAccessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void providerLookupTest() throws Exception {
		long startTime = System.currentTimeMillis();
		Service provider = (Service)ProviderLookup.getService(Jobber.class);
		//logger.info("ProviderLookup provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);

	}
	
	@Test
	public void accessingConcatenatorTest() throws Exception {
		long startTime = System.currentTimeMillis();
		Service provider = Accessor.getService(new NetSignature(Concatenator.class));
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
		
		startTime = System.currentTimeMillis();
		provider = ProviderAccessor.getProvider(Concatenator.class);
//		logger.info("ProviderAccessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}

	@Test
	public void providerLookatorTest() throws Exception {
		long startTime = System.currentTimeMillis();
		Service provider = (Service)ProviderLocator.getService(Jobber.class);
		//logger.info("ProviderLocator provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);

	}

}
