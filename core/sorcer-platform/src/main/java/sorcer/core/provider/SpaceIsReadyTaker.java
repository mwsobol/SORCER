/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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
package sorcer.core.provider;

import java.util.concurrent.ExecutorService;

import net.jini.core.transaction.Transaction;
import sorcer.core.exertion.ExertionEnvelop;
import sorcer.util.ProviderAccessor;

public class SpaceIsReadyTaker extends SpaceTaker {
	

	public SpaceIsReadyTaker() {
		setDaemon(true);
	}

	public SpaceIsReadyTaker(SpaceTakerData data, ExecutorService pool) {
		this();
		this.data = data;
		this.pool = pool;
		this.transactionLeaseTimeout = getTransactionLeaseTime();
		this.spaceTimeout = getTimeOut();
	}

	public void run() {
		logger.finer("................... run ... ifReady transactional = "
				+ isTransactional + ", lease = " + transactionLeaseTimeout + ", timeOut: " + spaceTimeout);
		while (keepGoing) {
			try {
				space = ProviderAccessor.getSpace(data.spaceName, data.spaceGroup);
				if (space == null) {
					Thread.sleep(spaceTimeout / 6);
					continue;
				}
			} catch (Exception ex) {
				continue;
			}

			try {
				boolean isReady = false;
				ExertionEnvelop ee = (ExertionEnvelop) space.read(data.entry,
						null, SPACE_TIMEOUT);
				if (ee != null) {
					isReady = ((ServiceProvider) data.provider)
							.isReady(data.entry.exertion);
					if (!isReady) {
						logger.finer("########### Provider is NOT ready ...");
						Thread.sleep(SPACE_TIMEOUT / 2);
						continue;
					}
				} else {
					continue;
				}

				Transaction.Created txnCreated = null;
				// logger.log(Level.INFO, "worker space template envelop = "
				// + data.entry.describe() + "\n service provider = "
				// + provider);
				if (isTransactional) {
					txnCreated = createTransaction();
					if (txnCreated == null)
						logger.severe("########### SpaceIsReady Worker DID NOT get transaction ...");
				}

				ee = (ExertionEnvelop) space
						.take(data.entry, txnCreated.transaction, SPACE_TIMEOUT);
				// if (ee != null) {
				// logger.info("...................got entry...................\n"
				// + ee.describe());
				// }
				// after 'take' timeout first cleanup and sleep for a while
				// before 'taking' exertions again
				if (ee == null) {
					if (txnCreated != null) {
						txnCreated.transaction.abort();
						Thread.sleep(SPACE_TIMEOUT / 2);
					}
					continue;
				}
				// check is the exertion execution is abandoned (poisoned) by
				// the requestor
				if (isAbandoned(ee.exertion) == true) {
					if (txnCreated != null) {
						txnCreated.transaction.commit();
					}
				}
				if (((ServiceProvider) data.provider).isSpaceSecurityEnabled()) {
					// if (ee.exertionID.equals(LOKI_ONLY)) {
					initDataMember(ee, txnCreated.transaction);
				}
				
				pool.execute(new SpaceWorker(ee, txnCreated));
			} catch (Exception ex) {
				continue;
			}
		}
	}

}