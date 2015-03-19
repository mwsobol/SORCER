package sorcer.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.entry.Name;
import net.jini.space.JavaSpace05;

import org.junit.Ignore;
import org.junit.Test;

import sorcer.service.ConfigurationException;
import sorcer.service.ContextException;
import sorcer.service.ExertionException;
import sorcer.service.SignatureException;
import sorcer.util.GenericUtil;
import sorcer.util.ProviderAccessor;
import sorcer.util.ServiceAccessor;
import sorcer.util.Sorcer;

/**
 * @author Mike Sobolewski
 */

public class UtilTest {
	private final static Logger logger = Logger.getLogger(UtilTest.class
			.getName());
	
	
	@Test
	public void getDataServerUrl() throws ConfigurationException {
		logger.info("*** DataServerUrl: " + Sorcer.getDataServerUrl());
	}
		
	@Test
	public void getWebsterUrl() throws ConfigurationException {
		logger.info("*** WebsterUrl: " + Sorcer.getWebsterUrl());
	}
	
	@Test
	public void getScratchDir() throws ConfigurationException {
		logger.info("*** DocRootDir(): " + Sorcer.getDocRootDir());
		logger.info("*** ScratchDir(): " + Sorcer.getScratchDir());
	}
	
	@Test
	public void loadEnv() throws ConfigurationException {
		Properties props = Sorcer.getEnvironment();
		logger.info("*** loaded default SORCER env properties:\n"
				+ GenericUtil.getPropertiesString(props));
	}
	
	@Test
	public void getEnvProperties() throws ConfigurationException {
		String envFilename = Sorcer.getHome() + File.separator + "configs" + File.separator  + "sorcer.env"; 
		Properties props = Sorcer.loadProperties(envFilename);
		logger.info("*** loaded SORCER env properties:" + envFilename + "\n"
				+ GenericUtil.getPropertiesString(props));
	}


	@Test
	public void spaceSuffixTest() throws ExertionException, ContextException,
			SignatureException {

//		logger.info("space name: " + Sorcer.getSpaceName());
//		logger.info("group space name: " + Sorcer.getSpaceGroup());
//		logger.info("suffixed space name: "
//				+ Sorcer.getSuffixedName(Sorcer.getActualSpaceName()));

		
		assertEquals(Sorcer.getSuffixedName(Sorcer.getSpaceName()),
				Sorcer.getSpaceName() + "-" + Sorcer.getNameSuffix());

		if (Sorcer.nameSuffixed())
			assertEquals(Sorcer.getSuffixedName(Sorcer.getSpaceName()), Sorcer.getActualSpaceName());
		else 
			assertEquals(Sorcer.getSpaceName(), Sorcer.getActualSpaceName());
	}

	@Ignore
	@Test
	public void getSpaceTest() throws ExertionException, ContextException,
			SignatureException {
		logger.info("exert space:\n" + ProviderAccessor.getSpace());
		
		ServiceTemplate tmpl = new ServiceTemplate(null, new Class[] { JavaSpace05.class }, new Entry[] { new Name(Sorcer.getActualSpaceName())});
		ServiceItem si = ServiceAccessor.getServiceItem(tmpl, null, new String[] { Sorcer.getSpaceGroup() });
		logger.info("got service: serviceID=" + si.serviceID + " template="
				+ tmpl + " groups=" + Sorcer.getSpaceGroup());
	}
		
}