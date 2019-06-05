package sorcer.core.provider.exertmonitor;

import com.sleepycat.collections.StoredMap;
import com.sleepycat.je.DatabaseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.SorcerConstants;
import sorcer.core.provider.MonitorManagementSession;
import sorcer.util.GenericUtil;
import sorcer.util.Sorcer;
import sorcer.util.SorcerUtil;
import sorcer.util.bdb.objects.UuidKey;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static sorcer.co.operator.list;

/**
 * @author Mike Sobolewski
 */

@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class SessionDatabaseTest implements SorcerConstants {

	private final static Logger logger = LoggerFactory.getLogger(SessionDatabaseTest.class);

	static {
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
	}
	
	private static SessionDatabaseRunner runner;
	private static File dbDir;
	
	@BeforeClass 
	public static void setUpOnce() throws IOException, DatabaseException {
		dbDir = new File("./tmp/ju-session-db");
		GenericUtil.deleteFilesAndSubDirs(dbDir);
		dbDir.mkdirs();
		String homeDir = "./tmp/ju-session-db";
		runner = new SessionDatabaseRunner(homeDir);
	}
	
	@AfterClass 
	public static void cleanup() throws Exception {
		// delete database home directory and close database
		SorcerUtil.deleteDir(dbDir);
		if (runner != null) {
            try {
                // Always attempt to close the database cleanly.
                runner.close();
            } catch (Exception e) {
                System.err.println("Exception during database close:");
                e.printStackTrace();
            }
        }
	}
	
	@Test
	public void sessionDatabaseTest() throws Exception {
        runner.run();
        // getValue from the database three sessions transactionally persisted with three tasks
		List<String> names = runner.returnExertionNames();
		List<String> ln = list("t1", "t2");
		Collections.sort(names);
		logger.info("names: " + names);
		
		assertEquals(names, ln);

		// retrieval
		StoredMap<UuidKey, MonitorManagementSession> sm = runner.getViews()
				.getSessionMap();
		
		Iterator<Map.Entry<UuidKey, MonitorManagementSession>> mei = sm
				.entrySet().iterator();
				
		names = new ArrayList<String>();
		Map.Entry<UuidKey, MonitorManagementSession> entry = null;

		while (mei.hasNext()) {
			entry = mei.next();
			names.add(((MonitorSession)entry.getValue()).getInitialExertion().getName());
		}

		Collections.sort(names);
		assertEquals(names, ln);
	}
}
