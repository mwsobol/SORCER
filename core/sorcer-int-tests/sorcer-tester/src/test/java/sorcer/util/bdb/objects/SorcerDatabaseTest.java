package sorcer.util.bdb.objects;

import com.sleepycat.collections.StoredMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.SorcerConstants;
import sorcer.service.Context;
import sorcer.service.Routine;
import sorcer.util.ModelTable;
import sorcer.util.SorcerUtil;

import java.io.File;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.list;

/**
 * @author Mike Sobolewski
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class SorcerDatabaseTest implements SorcerConstants {

	private final static Logger logger = LoggerFactory.getLogger(SorcerDatabaseTest.class);

	private static SorcerDatabaseRunner runner;
	private static File dbDir;
	
	@BeforeClass 
	public static void setUpOnce() throws Exception {
		dbDir = new File("./tmp/test-sorcer-db");
		SorcerUtil.deleteDir(dbDir);
		System.out.println("Sorcer DB dir: " + dbDir.getCanonicalPath());
		dbDir.mkdirs();
		String homeDir = "./tmp/test-sorcer-db";
		runner = new SorcerDatabaseRunner(homeDir);
        runner.run();
	}
	
	@AfterClass 
	public static void cleanup() throws Exception {
		if (runner != null) {
            try {
                // Always attempt to close the database cleanly.
                runner.close();
            } catch (Exception e) {
                System.err.println("Exception during database close:");
                e.printStackTrace();
            }
        }
		// delete database home directory and close database
		SorcerUtil.deleteDir(dbDir);
	}
	
	@Test
	public void storedcContextSetTest() throws Exception {
        // getValue from the database three contexts persisted
		List<String> names = runner.returnContextNames();
        List<String> ln = list("c1", "c2", "c3");
		Collections.sort(names);
		logger.info("names: " + names);

        assertEquals(ln, names);
	}
	
	@Test
	public void storedContextMapTest() throws Exception {
		StoredMap<UuidKey, Context> sm = runner.getViews()
				.getContextMap();
		
		Iterator<Map.Entry<UuidKey, Context>> mei = sm
				.entrySet().iterator();
				
		List<String> names = new ArrayList<String>();
		Map.Entry<UuidKey, Context> entry = null;

		while (mei.hasNext()) {
			entry = mei.next();
			names.add(entry.getValue().getName());
		}
        List<String> ln = list("c1", "c2", "c3");
		Collections.sort(names);
		logger.info("names: " + names);

        assertEquals(ln, names);
	}
	
	@Test
	public void storedTableSetTest() throws Exception {
        // getValue from the database three tables persisted twice
		List<String> names = runner.returnTableNames();
		List<String> ln = list("undefined0", "undefined1", "undefined2");
		Collections.sort(names);
		logger.info("table names: " + names);

        assertEquals(ln, names);
	}
	
	@Test
	public void storedTableMapTest() throws Exception {
		StoredMap<UuidKey, ModelTable> sm = runner.getViews()
				.getTableMap();
		
		Iterator<Map.Entry<UuidKey, ModelTable>> it = sm
				.entrySet().iterator();
				
		List<String> names = new ArrayList<String>();
		Map.Entry<UuidKey, ModelTable> entry = null;

		while (it.hasNext()) {
			entry = it.next();
			names.add(entry.getValue().getName());
		}
		List<String> ln = list("undefined0", "undefined1", "undefined2");
		Collections.sort(names);
		logger.info("table names: " + names);

        assertEquals(ln, names);
	}
	
	@Test
	public void storedExertionSetTest() throws Exception {
        // getValue from the database two mograms persisted twice
		List<String> names = runner.returnExertionNames();
        List<String> ln = list("f1", "f4");
		Collections.sort(names);
		logger.info("names: " + names);

        assertEquals(ln, names);
	}
	
	@Test
	public void storedExertionMapTest() throws Exception {
		StoredMap<UuidKey, Routine> sm = runner.getViews()
				.getRoutineMap();
		
		Iterator<Map.Entry<UuidKey, Routine>> it = sm
				.entrySet().iterator();
				
		List<String> names = new ArrayList<String>();
		Map.Entry<UuidKey, Routine> entry = null;

		while (it.hasNext()) {
			entry = it.next();
			names.add(entry.getValue().getName());
		}
        List<String> ln = list("f1", "f4");
		Collections.sort(names);
		logger.info("names: " + names);

        assertEquals(ln, names);
	}
	
	@Test
	public void storedUuidObjectSetTest() throws Exception {
        // getValue from the database three sessions persisted with three tasks
		List<String> names = runner.returnUuidObjectNames();
		List<String> ln = list("Mike", "Sobolewski");
		Collections.sort(names);
		logger.info("names: " + names);

        assertEquals(ln, names);
	}
	
	@Test
	public void storedUuidObjectMapTest() throws Exception {
		StoredMap<UuidKey, UuidObject> sm = runner.getViews()
				.getUuidObjectMap();
		
		Iterator<Map.Entry<UuidKey, UuidObject>> it = sm
				.entrySet().iterator();
				
		List<String> names = new ArrayList<String>();
		Map.Entry<UuidKey, UuidObject> entry = null;

		while (it.hasNext()) {
			entry = it.next();
			names.add(entry.getValue().getObject().toString());
		}
		List<String> ln = list("Mike", "Sobolewski");
		Collections.sort(names);
		logger.info("names: " + names);

        assertEquals(ln, names);
	}
	
	//@Test
	public void sdbURL() throws Exception {
		URL sbdUrl = new URL("sbd://myIterface/key#context=2345");
		Object obj = sbdUrl.openConnection().getContent();
	}
}
