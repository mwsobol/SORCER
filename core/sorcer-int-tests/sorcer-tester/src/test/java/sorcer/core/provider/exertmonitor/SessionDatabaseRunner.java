package sorcer.core.provider.exertmonitor;

import com.sleepycat.collections.StoredValueSet;
import com.sleepycat.collections.TransactionRunner;
import com.sleepycat.collections.TransactionWorker;
import com.sleepycat.je.DatabaseException;
import net.jini.core.event.RemoteEventListener;
import sorcer.core.exertion.NetTask;
import sorcer.core.exertion.ObjectTask;
import sorcer.core.provider.exertmonitor.db.SessionDatabase;
import sorcer.core.provider.exertmonitor.db.SessionDatabaseViews;
import sorcer.service.MonitorException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * SessionDatabaseRunner is the main entry point for the program and may be run as
 * follows:
 * 
 * <pre>
 * java sorcer.core.provider.exertmonitor.db
 *      [-h <home-directory> ]
 * </pre>
 * 
 * <p>
 * The default for the home directory is ./tmp -- the tmp subdirectory of the
 * current directory where the ServiceProviderDB is run. To specify a different
 * home directory, use the -home option. The home directory must exist before
 * running the sample. To recreate the sample database from scratch, delete all
 * files in the home directory before running the sample.
 * </p>
 * 
 * @author Mike Sobolewski
 */
public class SessionDatabaseRunner {

    private final SessionDatabase db;
    
    private SessionDatabaseViews views;

	/**
     * Run the sample program.
     */
    public static void main(String... args) {
    	if (System.getSecurityManager() == null)
			System.setSecurityManager(new SecurityManager());
        System.out.println("\nRunning sample: " + SessionDatabaseRunner.class);

        // Parse the command line arguments.
        //
        String homeDir = "./tmp";
        for (int i = 0; i < args.length; i += 1) {
            if (args[i].equals("-h") && i < args.length - 1) {
                i += 1;
                homeDir = args[i];
            } else {
                System.err.println("Usage:\n java " + SessionDatabaseRunner.class.getName() +
                                   "\n  [-h <home-directory>]");
                System.exit(2);
            }
        }

        // Run the sample.
        SessionDatabaseRunner runner = null;
        try {
            runner = new SessionDatabaseRunner(homeDir);
            runner.run();
        } catch (Exception e) {
            // If an exception reaches this point, the last transaction did not
            // complete.  If the exception is RunRecoveryException, follow
            // the Berkeley DB recovery procedures before running again.
            e.printStackTrace();
        } 
        finally {
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
    }

	/**
	 * Open the database and views.
	 */
	public SessionDatabaseRunner(String homeDir) throws DatabaseException {
		db = new SessionDatabase(homeDir);
		views = new SessionDatabaseViews(db);
	}

    /**
     * Close the database cleanly.
     */
    public void close()
        throws DatabaseException {

        db.close();
    }

    /**
     * Run two transactions to populate and print the database.  A
     * TransactionRunner is used to ensure consistent handling of transactions,
     * including deadlock retries.  But the best transaction handling mechanism
     * to use depends on the application.
     */
	public void run() throws Exception {
		TransactionRunner runner = new TransactionRunner(db.getEnvironment());
		runner.run(new PopulateDatabase());
		runner.run(new PrintDatabase());
	}

    /**
     * Populate the database in a single transaction.
     */
	private class PopulateDatabase implements TransactionWorker {

		public void doWork() {
			addSessions();
		}
	}

    /**
     * Print the database in a single transaction.  All entities are printed
     * and the indices are used to print the entities for certain keys.
     *
     * <p> Note the use of special iterator() methods.  These are used here
     * with indices to find the runtimes for certain providers.</p>
     */
    private class PrintDatabase implements TransactionWorker {

        public void doWork() {
        	try {
				printValues("Sessions", views.getSessionSet().iterator());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
        }
    }

    /**
     * Populate the session entities in the database.  
     * @throws IOException 
     */
	private void addSessions(ArrayList<MonitorSession> sessions) throws IOException {
		Set<MonitorSession> sessionSet = views.getSessionSet();
        sessionSet.addAll(sessions);
    }

	 /**
     * Populate the session in the database.  
     */
	private void addSession(MonitorSession session) {
		Set<MonitorSession> sessionSet = views.getSessionSet();
        sessionSet.add(session);
    }
	
	 /**
     * Populate the session entities in the database.  
     */
	private void addSessions() {
		StoredValueSet<MonitorSession> sessionSet = views.getSessionSet();
        try {
			sessionSet.add(new MonitorSession(new NetTask("t1"), (RemoteEventListener)null, 10));
			sessionSet.add(new MonitorSession(new ObjectTask("t2"), (RemoteEventListener)null, 100));
//			sessionSet.add(new MonitorSession(new ResponseTask("t3"), (RemoteEventListener)null, 1000));
		} catch (MonitorException e) {
			e.printStackTrace();
		}
    }
	
	 /**
     * Get the exertion names returned by an iterator of entity eval objects.
	 * @throws ClassNotFoundException 
	 * @throws IOException 
     */
	public List<String> returnExertionNames() throws IOException, ClassNotFoundException {
		List<String> names = new ArrayList<String>();
		Iterator<MonitorSession> iterator = views.getSessionSet().iterator();
		while (iterator.hasNext()) {
			MonitorSession ms = iterator.next();
			names.add(ms.getInitialExertion().getName());
		}
		return names;
	}
		
    /**
     * Print the objects returned by an iterator of entity eval objects.
     * @throws ClassNotFoundException 
     * @throws IOException 
     */
	private void printValues(String label, Iterator<MonitorSession> iterator) throws IOException, ClassNotFoundException {
		System.out.println("\n--- " + label + " ---");
		while (iterator.hasNext()) {
			MonitorSession ms = iterator.next();
			System.out.println(ms);
		}
	}
	
	public SessionDatabaseViews getViews() {
		return views;
	}
}
