package sorcer.util;

import net.jini.config.EmptyConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.*;
import sorcer.service.Accessor;
import sorcer.service.DynamicAccessor;
import sorcer.service.Exerter;
import sorcer.service.Service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static sorcer.eo.operator.provider;
import static sorcer.eo.operator.sig;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class ServiceAccessors implements SorcerConstants {
	private final static Logger logger = LoggerFactory.getLogger(ServiceAccessors.class);
    static ProviderAccessor providerAccessor;
    static ProviderLookup providerLookup;
	static DynamicAccessor dynamicAccessoror;

    @BeforeClass
    public static void setup() {
        providerAccessor = new ProviderAccessor(EmptyConfiguration.INSTANCE);
        providerLookup = new ProviderLookup();
		dynamicAccessoror = Accessor.get();
    }

	@Test
	public void getProvider() throws Exception {
		long startTime = System.currentTimeMillis();
		Object prv = provider(sig(Jobber.class));
//		logger.info("Accessor provider: " + prv);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertTrue(prv instanceof Jobber);
	}
	
	@Test
	public void providerAccessorSig() throws Exception {
		long startTime = System.currentTimeMillis();
		Exerter provider = providerAccessor.getProvider(sig(Jobber.class));
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertTrue(provider instanceof Jobber);
	}

	@Test
	public void dynamicAccessorSig() throws Exception {
		long startTime = System.currentTimeMillis();
		Exerter provider = (Exerter) dynamicAccessoror.getService(sig(SorcerJobber.class));
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertTrue(provider instanceof Jobber);
	}

	@Test
	public void dynamicAccessorMultitypeSig() throws Exception {
		long startTime = System.currentTimeMillis();
		Exerter provider = (Exerter) dynamicAccessoror.getService(
				sig(sig(SorcerJobber.class), Service.class, Exerter.class));
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertTrue(provider instanceof Jobber);
	}

	@Test
	public void providerLocatorSig() throws Exception {
		long startTime = System.currentTimeMillis();
		Exerter provider = providerLookup.getProvider(sig(Spacer.class));
		//logger.info("ProviderLocator provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertTrue(provider instanceof Spacer);
	}
	
	@Test
	public void providerLookupSig() throws Exception {
		long startTime = System.currentTimeMillis();
		Exerter provider = providerLookup.getProvider(sig(Concatenator.class));
//		logger.info("ProviderAccessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertTrue(provider instanceof Concatenator);
	}

	@Test
	public void providerAccessorType() throws Exception {
		long startTime = System.currentTimeMillis();
		Exerter provider = providerAccessor.getProvider(Jobber.class);
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}

	@Test
	public void providerLocatorType() throws Exception {
		long startTime = System.currentTimeMillis();
		Exerter provider = providerAccessor.getProvider(Spacer.class);
		//logger.info("ProviderLocator provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
	
	@Test
	public void providerLookupType() throws Exception {
		long startTime = System.currentTimeMillis();
        Object  provider = providerLookup.getService(Concatenator.class);
//		logger.info("ProviderAccessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}

	@Test
	public void acessor() throws Exception {
		long startTime = System.currentTimeMillis();
		Object provider = Accessor.get().getService(sig(Jobber.class));
//		logger.info("Accessor provider: " + provider);
		logger.info(Stopwatch.getTimeString(System.currentTimeMillis() - startTime));
		assertNotNull(provider);
	}
}
