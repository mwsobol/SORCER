package sorcer.arithmetic.service;

import static org.junit.Assert.assertNotNull;
import static sorcer.eo.operator.sig;

import java.util.logging.Logger;

import org.junit.Ignore;
import org.junit.Test;

import sorcer.core.SorcerConstants;
import sorcer.core.provider.Concatenator;
import sorcer.core.provider.Jobber;
import sorcer.core.provider.Provider;
import sorcer.core.provider.Rendezvous;
import sorcer.core.provider.Spacer;
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
@SuppressWarnings("rawtypes")
public class ServiceAccessorTest implements SorcerConstants {

	private final static Logger logger = Logger
			.getLogger(ServiceAccessorTest.class.getName());

	static {
		String version = "5.0.0-SNAPSHOT";
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "arithmetic-" + version + "-dl.jar",  "sorcer-dl-"+version +".jar" });
		System.setProperty("java.protocol.handler.pkgs", "sorcer.util.url|org.rioproject.url");
		System.setProperty("java.rmi.server.RMIClassLoaderSpi","org.rioproject.rmi.ResolvingLoader");
	}
	
	@Ignore
	@Test
	public void acessor() throws Exception {
		long startTime = System.currentTimeMillis();
		Service provider = Accessor.getService(sig(Rendezvous.class));
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void providerAccessor() throws Exception {
		long startTime = System.currentTimeMillis();
		Provider provider = ProviderAccessor.getProvider(sig(Jobber.class));
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void providerLocator() throws Exception {
		long startTime = System.currentTimeMillis();
		Provider provider = ProviderLocator.getProvider(sig(Spacer.class));
		//logger.info("ProviderLocator provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void providerLookup() throws Exception {
		long startTime = System.currentTimeMillis();
		Provider provider = ProviderLookup.getProvider(sig(Concatenator.class));
//		logger.info("ProviderAccessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}

	@Test
	public void providerAccessorSt() throws Exception {
		long startTime = System.currentTimeMillis();
		Provider provider = ProviderAccessor.getProvider(Jobber.class);
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void providerLocatorSt() throws Exception {
		long startTime = System.currentTimeMillis();
		Provider provider = ProviderLocator.getProvider(Spacer.class);
		//logger.info("ProviderLocator provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void providerLookupSt() throws Exception {
		long startTime = System.currentTimeMillis();
		Provider provider = ProviderLookup.getProvider(Concatenator.class);
//		logger.info("ProviderAccessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}

}
