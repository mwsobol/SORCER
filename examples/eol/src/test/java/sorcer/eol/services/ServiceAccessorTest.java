package sorcer.eol.services;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.*;
import sorcer.service.Accessor;
import sorcer.service.Service;
import sorcer.util.ProviderAccessor;
import sorcer.util.ProviderLocator;
import sorcer.util.ProviderLookup;
import sorcer.util.Stopwatch;

import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;
import static sorcer.eo.operator.sig;

/**
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class ServiceAccessorTest implements SorcerConstants {
	private final static Logger logger = Logger.getLogger(ServiceAccessorTest.class.getName());

    @BeforeClass
    public static void setup() {
        System.setSecurityManager(new SecurityManager());
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
