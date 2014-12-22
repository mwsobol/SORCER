package sorcer.eol.services;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
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
@RunWith(SorcerTestRunner.class)
@ProjectContext("examples/eol")
public class ServiceAccessors implements SorcerConstants {
	private final static Logger logger = Logger.getLogger(ServiceAccessors.class.getName());
	
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
	public void providerAccessorSig() throws Exception {
		long startTime = System.currentTimeMillis();
		Provider provider = ProviderAccessor.getProvider(sig(Jobber.class));
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void providerLocatorSig() throws Exception {
		long startTime = System.currentTimeMillis();
		Provider provider = ProviderLocator.getProvider(sig(Spacer.class));
		//logger.info("ProviderLocator provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void providerLookupSig() throws Exception {
		long startTime = System.currentTimeMillis();
		Provider provider = ProviderLookup.getProvider(sig(Concatenator.class));
//		logger.info("ProviderAccessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}

	@Test
	public void providerAccessorType() throws Exception {
		long startTime = System.currentTimeMillis();
		Provider provider = ProviderAccessor.getProvider(Jobber.class);
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void providerLocatorType() throws Exception {
		long startTime = System.currentTimeMillis();
		Provider provider = ProviderLocator.getProvider(Spacer.class);
		//logger.info("ProviderLocator provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void providerLookupType() throws Exception {
		long startTime = System.currentTimeMillis();
		Provider provider = ProviderLookup.getProvider(Concatenator.class);
//		logger.info("ProviderAccessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}

}
