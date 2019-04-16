package sorcer.util;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.id.Uuid;
import net.jini.lookup.entry.Name;
import net.jini.space.JavaSpace05;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.service.*;
import sorcer.service.space.SpaceAccessor;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.list;

/**
 * @author Mike Sobolewski
 */

public class UtilTest {

	private final static Logger logger = LoggerFactory.getLogger(UtilTest.class);

	@BeforeClass
	public static void init() {
		Accessor.create();
	}

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
	public void spaceSuffixTest() throws RoutineException, ContextException,
			SignatureException {

		assertEquals(Sorcer.getSuffixedName(Sorcer.getSpaceName()),
				Sorcer.getSpaceName() + "-" + Sorcer.getNameSuffix());

		if (Sorcer.nameSuffixed())
			assertEquals(Sorcer.getSuffixedName(Sorcer.getSpaceName()), Sorcer.getActualSpaceName());
		else 
			assertEquals(Sorcer.getSpaceName(), Sorcer.getActualSpaceName());
	}

	@Test
	public void getSpaceTest() throws RoutineException, ContextException,
			SignatureException {
		logger.info("exert space:\n" + SpaceAccessor.getSpace());
		
		ServiceTemplate tmpl = new ServiceTemplate(null,
				new Class[] { JavaSpace05.class },
				new Entry[] { new Name(Sorcer.getActualSpaceName())});
		ServiceItem[] si = Accessor.get().getServiceItems(tmpl, null);
        assertTrue(si.length>0);
		logger.info("got service: serviceID=" + si[0].serviceID + " template="
				+ tmpl + " groups=" + Sorcer.getSpaceGroup());
	}

	@Test
	 public void fileTable() throws Exception {
		Uuid key1 = net.jini.id.UuidFactory.generate();
		logger.info("key1: " + key1);
		Uuid key2 = net.jini.id.UuidFactory.generate();
		logger.info("key2: " + key2);
		FileTable table = new FileTable("test");
		table.put(key1, "String11111");
		table.put(key2, "String22222");
		Uuid key;
		Enumeration e = table.keys();
		while (e.hasMoreElements()) {
			key = (Uuid)e.nextElement();
			table.put(key2,"String22222");
			logger.info(key + ":" + table.get(key));
		}

		assertEquals(table.get(key1), "String11111");
		assertEquals(table.get(key2), "String22222");

//		dataTable.cleanup();
		table.close();

		File obf = new File("test.obf");
		obf.delete();
		File iobf = new File("test-index.obf");
		iobf.delete();

	}

	@Test
	public void numberedFileTable() throws Exception {
		FileTable table = new FileTable("test");
		table.addRow(1, "String11111");
		table.addRow(2, "String22222");
		int key;
		Enumeration<Integer> e = table.keys();
		while (e.hasMoreElements()) {
			key = e.nextElement();
			table.addRow(2, "String22222");
			logger.info(key + ":" + table.get(key));
		}

		assertEquals(table.getRow(1), "String11111");
		assertEquals(table.getRow(2), "String22222");

//		dataTable.cleanup();
		table.close();

		File obf = new File("test.obf");
		obf.delete();
		File iobf = new File("test-index.obf");
		iobf.delete();
	}

	@Test
	public void compareResponses() throws Exception {
		List<Double> expectedValues = list(2.0, 26193.577, 0.888, 12980.178, 1450.230);
		String result = "2.0000000e+00   2.6193577e+04   8.8827207e-01   1.2980178e+04   1.4502304e+03";

		Row r1 = new Row(null, result, " ");
//		logger.info("r1:\n" + r1);

		Row r2 = new Row(null, expectedValues);
//		logger.info("r2:\n" + r2);
		assertTrue(r1.compareTo(r2, 0.005));

	}
}

