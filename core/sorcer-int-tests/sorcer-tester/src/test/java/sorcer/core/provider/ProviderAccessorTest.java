package sorcer.core.provider;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.SorcerConstants;
import sorcer.core.signature.NetSignature;
import sorcer.service.Accessor;
import sorcer.service.Servicer;
import sorcer.util.ProviderAccessor;
import sorcer.util.ProviderLookup;
import sorcer.util.Stopwatch;

import static org.junit.Assert.assertNotNull;

/**
 * @author Mike Sobolewski
 */

@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class ProviderAccessorTest implements SorcerConstants {
	private static final Logger logger = LoggerFactory.getLogger(ProviderAccessorTest.class);
    static ProviderAccessor providerAccessor;

    @BeforeClass
    public static void setup() {
        providerAccessor = new ProviderAccessor();
    }

	@Test
	public void providerAcessorTest() throws Exception {
		long startTime = System.currentTimeMillis();
		Object provider = Accessor.get().getService(new NetSignature(Jobber.class));
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
		
		startTime = System.currentTimeMillis();
		provider = providerAccessor.getProvider(Jobber.class);
//		logger.info("ProviderAccessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void providerLookupTest() throws Exception {
		long startTime = System.currentTimeMillis();
		Servicer provider = (Servicer)new ProviderLookup().getService(Jobber.class);
		//logger.info("ProviderLookup provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);

	}
	
	@Test
	public void accessingConcatenatorTest() throws Exception {
		long startTime = System.currentTimeMillis();
		Object provider = Accessor.get().getService(new NetSignature(Concatenator.class));
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
		
		startTime = System.currentTimeMillis();
		provider = providerAccessor.getProvider(Concatenator.class);
//		logger.info("ProviderAccessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}

}
