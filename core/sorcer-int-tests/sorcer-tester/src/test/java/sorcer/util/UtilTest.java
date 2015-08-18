package sorcer.util;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.entry.Name;
import net.jini.space.JavaSpace05;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.service.*;
import sorcer.service.space.SpaceAccessor;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Mike Sobolewski
 */

public class UtilTest {

	private final static Logger logger = LoggerFactory.getLogger(UtilTest.class);
	
	
	@Test
	public void getDataServerUrl() throws Exception {
		logger.info("*** DataServerUrl: " + Sorcer.getDataServerUrl());
		assertEquals(SorcerEnv.getProperty("data.server.interface"),
				new URL(Sorcer.getDataServerUrl()).getHost());
		assertEquals(SorcerEnv.getProperty("data.server.port"),
				"" + new URL(Sorcer.getDataServerUrl()).getPort());
	}
		
	@Test
	public void getWebsterUrl() throws Exception {
		logger.info("*** WebsterUrl: " + Sorcer.getWebsterUrl());
		assertEquals(SorcerEnv.getProperty("provider.webster.interface"),
				new URL(Sorcer.getWebsterUrl()).getHost());
		assertEquals(SorcerEnv.getProperty("provider.webster.port"),
				"" + new URL(Sorcer.getWebsterUrl()).getPort());
	}

	@Test
	public void loadEnv() throws ConfigurationException {
		Properties props = Sorcer.getEnvironment();
		logger.info("*** loaded default SORCER env properties:\n"
				+ GenericUtil.getPropertiesString(props));
		assertEquals(props.getProperty("provider.lookup.accessor"), "sorcer.util.ProviderAccessor");
	}
	
	@Test
	public void getEnvProperties() throws ConfigurationException {
		String envFilename = Sorcer.getHome() + File.separator + "configs" + File.separator  + "sorcer.env"; 
		Properties props = Sorcer.loadProperties(envFilename);
		logger.info("*** loaded SORCER env properties:" + envFilename + "\n"
				+ GenericUtil.getPropertiesString(props));
		assertEquals(props.getProperty("provider.lookup.accessor"), "sorcer.util.ProviderAccessor");
	}

	@Test
	public void spaceSuffixTest() throws ExertionException, ContextException,
			SignatureException {

		assertEquals(Sorcer.getSuffixedName(Sorcer.getSpaceName()),
				Sorcer.getSpaceName() + "-" + Sorcer.getNameSuffix());

		if (Sorcer.nameSuffixed())
			assertEquals(Sorcer.getSuffixedName(Sorcer.getSpaceName()), Sorcer.getActualSpaceName());
		else 
			assertEquals(Sorcer.getSpaceName(), Sorcer.getActualSpaceName());
	}

	@Test
	public void getSpaceTest() throws ExertionException, ContextException,
			SignatureException {
		logger.info("exert space:\n" + SpaceAccessor.getSpace());
		
		ServiceTemplate tmpl = new ServiceTemplate(null,
				new Class[] { JavaSpace05.class },
				new Entry[] { new Name(Sorcer.getActualSpaceName())});
		ServiceItem si = Accessor.getServiceItem(tmpl, null, new String[]{Sorcer.getSpaceGroup()});
		logger.info("got service: serviceID=" + si.serviceID + " template="
				+ tmpl + " groups=" + Sorcer.getSpaceGroup());
		assertNotNull(si);
	}
		
}
