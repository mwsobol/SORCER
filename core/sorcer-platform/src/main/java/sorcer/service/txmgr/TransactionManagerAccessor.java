package sorcer.service.txmgr;
/**
 *
 * Copyright 2013 Rafał Krupiński.
 * Copyright 2013 Sorcersoft.com S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import net.jini.core.transaction.server.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.service.Accessor;
import sorcer.util.ServiceAccessor;

/**
 * @author Rafał Krupiński
 */
public class TransactionManagerAccessor extends ServiceAccessor{
    private static final Logger log = LoggerFactory.getLogger(TransactionManagerAccessor.class);
    protected static TransactionManagerAccessor instance = new TransactionManagerAccessor();

    /**
     * Returns a Jini transaction manager service.
     *
     * @return Jini transaction manager
     */
    public static TransactionManager getTransactionManager() {
        return instance.doGetTransactionManager();
    }
    public  TransactionManager doGetTransactionManager() {
        TransactionManager transactionMgr= (TransactionManager) cache.get(TransactionManager.class.getName());
        try {
            if (transactionMgr == null)
                return doGetNewTransactionManger();
            transactionMgr.getState(-1);
            return transactionMgr;
        } catch (net.jini.core.transaction.UnknownTransactionException ute) {
            return transactionMgr;
        } catch (Exception e) {
            try {
                transactionMgr = getNewTransactionManger();
                return transactionMgr;
            } catch (Exception ex) {
                log.error("error", ex);
                return null;
            }
        }
    }

    private static TransactionManager getNewTransactionManger() {
        return instance.doGetNewTransactionManger();
    }

    private TransactionManager doGetNewTransactionManger() {
        TransactionManager transactionMgr = Accessor.getService(TransactionManager.class);
        if (transactionMgr!=null)
            cache.put(TransactionManager.class.getName(), transactionMgr);
        return transactionMgr;
    }

}
