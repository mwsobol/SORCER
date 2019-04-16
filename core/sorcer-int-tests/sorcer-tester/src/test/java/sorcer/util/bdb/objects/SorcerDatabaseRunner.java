package sorcer.util.bdb.objects;

import com.sleepycat.collections.StoredValueSet;
import com.sleepycat.collections.TransactionRunner;
import com.sleepycat.collections.TransactionWorker;
import com.sleepycat.je.DatabaseException;
import sorcer.arithmetic.tester.provider.Adder;
import sorcer.arithmetic.tester.provider.Multiplier;
import sorcer.arithmetic.tester.provider.Subtractor;
import sorcer.co.operator;
import sorcer.core.context.ServiceContext;
import sorcer.core.provider.exertmonitor.SessionDatabaseRunner;
import sorcer.service.*;
import sorcer.util.DataTable;
import sorcer.util.ModelTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static sorcer.co.operator.*;
import static sorcer.eo.operator.*;

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
@SuppressWarnings("unchecked")
public class SorcerDatabaseRunner {

    protected final SorcerDatabase sdb;
    
    protected SorcerDatabaseViews views;

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
	public SorcerDatabaseRunner(String homeDir) throws DatabaseException {
		sdb = new SorcerDatabase(homeDir);
		initViews();
	}

	protected void initViews() {
		views = new SorcerDatabaseViews(sdb);
	}
	
    /**
     * Close the database cleanly.
     */
    public void close()
        throws DatabaseException {

        sdb.close();
    }

    /**
     * Run two transactions to populate and print the database.  A
     * TransactionRunner is used to ensure consistent handling of transactions,
     * including deadlock retries.  But the best transaction handling mechanism
     * to use depends on the application.
     */
	public void run() throws Exception {
		TransactionRunner runner = new TransactionRunner(sdb.getEnvironment());
		runner.run(new PopulateContextDatabase());
		runner.run(new PopulateTableDatabase());
		runner.run(new PopulateExertionDatabase());
		runner.run(new PopulateUuidObjectDatabase());
		
		runner.run(new PrintContextDatabase());
		runner.run(new PrintTableDatabase());
		runner.run(new PrintExertionDatabase());
		runner.run(new PrintUuidObjectDatabase());
	}

    /**
     * Populate the Context database in a single transaction.
     */
	public class PopulateContextDatabase implements TransactionWorker {

		public void doWork() {
			addContexts();
		}
	}

	 /**
     * Populate the Table database in a single transaction.
     */
	public class PopulateTableDatabase implements TransactionWorker {

		public void doWork() {
			try {
				addTables();
			} catch (EvaluationException e) {
				e.printStackTrace();
			}
		}
	}
	
	 /**
     * Populate the Routine database in a single transaction.
     */
	public class PopulateExertionDatabase implements TransactionWorker {

		public void doWork() {
			try {
				addExertions();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
    * Populate the UuidObject database in a single transaction.
    */
	public class PopulateUuidObjectDatabase implements TransactionWorker {

		public void doWork() {
			try {
				addUuidObjects();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
    
    /**
     * Print the database in a single transaction.  All entities are printed
     * and the indices are used to print the entities for certain keys.
     *
     * <p> Note the use of special iterator() methods.  These are used here
     * with indices to find the runtimes for certain providers.</p>
     */
	public class PrintContextDatabase implements TransactionWorker {

        public void doWork() {
        	try {
				printValues("Contexts", views.getContextSet().iterator());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
        }
    }

	public class PrintTableDatabase implements TransactionWorker {

        public void doWork() {
        	try {
				printValues("Tables", views.getTableSet().iterator());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
        }
    }
    
	public class PrintExertionDatabase implements TransactionWorker {

        public void doWork() {
        	try {
				printValues("Exertions", views.getRoutineSet().iterator());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
        }
    }
    
	public class PrintUuidObjectDatabase implements TransactionWorker {

        public void doWork() {
        	try {
				printValues("Object", views.getUuidObjectSet().iterator());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
        }
    }
    
    /**
     * Populate the context entities in the database.  
     * @throws IOException 
     */
	private void addContexts(ArrayList<Context> contexts) throws IOException {
		Set<Context> contextSet = views.getContextSet();
        contextSet.addAll(contexts);
    }

	 /**
     * Populate the context in the database.  
     */
	private void addContext(Context context) {
		Set<Context> contextSet = views.getContextSet();
        contextSet.add(context);
    }
	
	 /**
     * Populate the context entities in the database.  
     */
	private void addContexts() {
		StoredValueSet<Context> contextSet = views.getContextSet();
        	contextSet.add(new ServiceContext("c1"));
        	contextSet.add(new ServiceContext("c2"));
        	contextSet.add(new ServiceContext("c3"));
    }
	
	 /**
     * Populate the tables entities in the database.  
	 * @throws EvaluationException 
     */
	private void addTables() throws EvaluationException {
		StoredValueSet<ModelTable> tableSet = views.getTableSet();
        	tableSet.add((new DataTable()));
        	tableSet.add(new DataTable());
        	tableSet.add(new DataTable());
    }
	
	private Task getTask() throws RoutineException, SignatureException, ContextException {
		Task f4 = task("f4", sig("multiply", Multiplier.class), 
				context("multiply", operator.inVal("arg/x1", 10.0), operator.inVal("arg/x2", 50.0),
						outVal("result/y1")));
		return f4;
	}
		
	private Job getJob() throws RoutineException, SignatureException, ContextException {
		Task f4 = task("f4", sig("multiply", Multiplier.class), 
				context("multiply", operator.inVal("arg/x1", 10.0), operator.inVal("arg/x2", 50.0),
						outVal("result/y1")));

		Task f5 = task("f5", sig("add", Adder.class), 
				context("add", operator.inVal("arg/x3", 20.0), operator.inVal("arg/x4", 80.0),
						outVal("result/y2")));

		Task f3 = task("f3", sig("subtract", Subtractor.class), 
				context("subtract", operator.inVal("arg/x5"), operator.inVal("arg/x6"),
						outVal("result/y3")));

		// Service Composition f1(f2(x1, x2), f3(x1, x2))
		// Service Composition f2(f4(x1, x2), f5(x1, x2))
		//Job f1= job("f1", job("f2", f4, f5, strategy(Flow.PAR, Access.PULL)), f3,
		Job f1= job("f1", job("f2", f4, f5), f3,
				pipe(outPoint(f4, "result/y1"), inPoint(f3, "arg/x5")),
				pipe(outPoint(f5, "result/y2"), inPoint(f3, "arg/x6")));
		return f1;
	}
	
	 /**
     * Populate the exertion entities in the database.  
	 * @throws ContextException 
	 * @throws SignatureException 
	 * @throws RoutineException
     */
	private void addExertions() throws RoutineException, SignatureException, ContextException {
		StoredValueSet<Routine> exertionSet = views.getRoutineSet();
		exertionSet.add(getTask());
		exertionSet.add(getJob());
	}

	 /**
     * Populate the VarModel entities in the database.  
	 * @throws EvaluationException 
     */
	private void addUuidObjects() throws EvaluationException {
		StoredValueSet<UuidObject> uuidObjetSet = views.getUuidObjectSet();
		uuidObjetSet.add(new UuidObject("Mike"));
		uuidObjetSet.add(new UuidObject("Sobolewski"));
	}
	
	 /**
     * Get the context names returned by an iterator of entity eval objects.
	 * @throws ClassNotFoundException 
	 * @throws IOException 
     */
	public List<String> returnContextNames() throws IOException, ClassNotFoundException {
		List<String> names = new ArrayList<String>();
		Iterator<Context> iterator = views.getContextSet().iterator();
		while (iterator.hasNext()) {
			Context cxt = iterator.next();
			names.add(cxt.getName());
		}
		return names;
	}
		
	
	 /**
     * Get the dataTable names returned by an iterator of entity eval objects.
	 * @throws ClassNotFoundException 
	 * @throws IOException 
     */
	public List<String> returnTableNames() throws IOException, ClassNotFoundException {
		List<String> names = new ArrayList<String>();
		Iterator<ModelTable> iterator = views.getTableSet().iterator();
		while (iterator.hasNext()) {
			ModelTable table = iterator.next();
			names.add(table.getName());
		}
		return names;
	}
	
	 /**
     * Get the exertion names returned by an iterator of entity eval objects.
	 * @throws ClassNotFoundException 
	 * @throws IOException 
     */
	public List<String> returnExertionNames() throws IOException, ClassNotFoundException {
		List<String> names = new ArrayList<String>();
		Iterator<Routine> iterator = views.getRoutineSet().iterator();
		while (iterator.hasNext()) {
			Routine xrt = iterator.next();
			names.add(xrt.getName());
		}
		return names;
	}
	
	 /**
     * Get the UuidObject names returned by an iterator of entity eval objects.
	 * @throws ClassNotFoundException 
	 * @throws IOException 
     */
	public List<String> returnUuidObjectNames() throws IOException, ClassNotFoundException {
		List<String> names = new ArrayList<String>();
		Iterator<UuidObject> iterator = views.getUuidObjectSet().iterator();
		while (iterator.hasNext()) {
			UuidObject object = iterator.next();
			names.add("" + object.getObject());
		}
		return names;
	}
	
    /**
     * Print the objects returned by an iterator of entity eval objects.
     * @throws ClassNotFoundException 
     * @throws IOException 
     */
	protected void printValues(String label, Iterator iterator) throws IOException, ClassNotFoundException {
		System.out.println("\n--- " + label + " ---");
		while (iterator.hasNext()) {
			Object obj = iterator.next();
			System.out.println(obj);
		}
	}
	
	public SorcerDatabaseViews getViews() {
		return views;
	}
}
