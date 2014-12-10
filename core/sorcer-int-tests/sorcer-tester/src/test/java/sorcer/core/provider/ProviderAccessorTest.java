package sorcer.core.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.SorcerConstants;
import sorcer.core.signature.NetSignature;
import sorcer.service.Accessor;
import sorcer.service.Service;
import sorcer.util.*;

import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;

/**
 * @author Mike Sobolewski
 */

@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class ProviderAccessorTest implements SorcerConstants {

	private final static Logger logger = Logger
			.getLogger(ProviderAccessorTest.class.getName());

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
