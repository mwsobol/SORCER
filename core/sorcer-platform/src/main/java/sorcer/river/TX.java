package sorcer.river;

import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.transaction.*;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.lease.LeaseRenewalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.service.Accessor;
import sorcer.service.txmgr.TransactionManagerAccessor;

import java.rmi.RemoteException;


/**
 * Extracted from SpaceTaker
 *
 * @author Rafał Krupiński
 */
public class TX {
    protected static LeaseRenewalManager leaseManager= new LeaseRenewalManager();
    private static Logger logger = LoggerFactory.getLogger(TX.class);

    public static void abortTransaction(Transaction.Created txn) throws UnknownLeaseException, UnknownTransactionException, CannotAbortException, RemoteException {
        leaseManager.remove(txn.lease);
        txn.transaction.abort();
    }

    public static void commitTransaction(Transaction.Created txn) throws UnknownLeaseException, UnknownTransactionException, CannotCommitException, RemoteException  {
        leaseManager.remove(txn.lease);
        txn.transaction.commit();
    }

    synchronized public static Transaction.Created createTransaction(long transactionLeaseTimeout) {
        try {
            //TransactionManager tManager = Accessor.getService(TransactionManager.class);
            TransactionManager tManager = TransactionManagerAccessor.getTransactionManager();
            if (tManager == null) {
                return null;
            }
            Transaction.Created created = TransactionFactory.create(tManager,
                    transactionLeaseTimeout);
            logger.debug("Transaction created {}", created);

            leaseManager.renewFor(created.lease, Lease.FOREVER, transactionLeaseTimeout, null);

            return created;
        } catch (RemoteException e) {
            logger.warn("Error while creating transaction", e);
        } catch (LeaseDeniedException e) {
            logger.warn("Lease denied", e);
        }
        return null;
    }
}
