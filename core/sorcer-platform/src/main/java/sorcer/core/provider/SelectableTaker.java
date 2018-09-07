/*
 * Copyright 2018 the original author or authors.
 * Copyright 2018 SorcerSoft.org.
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

import net.jini.core.transaction.Transaction;
import sorcer.co.operator;
import sorcer.core.exertion.ExertionEnvelop;
import sorcer.service.space.SpaceAccessor;
import sorcer.co.operator.Tokens;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static sorcer.co.operator.list;

/*
 * This space taker firts reads taks and takes only tasks with indicated OS
 * matching OS of the platform the SpaceOSTaker is running.
 */
public class SelectableTaker extends SpaceTaker {

	public SelectableTaker() {
	}

	public SelectableTaker(SpaceTakerData data, ExecutorService pool) {
		this();
		this.data = data;
		this.pool = pool;
		this.transactionLeaseTimeout = getTransactionLeaseTime();
		this.spaceTimeout = getTimeOut();
	}

	public void run() {
		logger.debug("................... run ... ifReady transactional = "
                + isTransactional + ", lease = " + transactionLeaseTimeout + ", timeOut: " + spaceTimeout);
		while (keepGoing) {
			try {
				space = SpaceAccessor.getSpace(data.spaceName);
				if (space == null) {
					Thread.sleep(spaceTimeout / 6);
					continue;
				}
			} catch (Exception ex) {
				continue;
			}

			list("a", "b");

			try {
				ExertionEnvelop ee = (ExertionEnvelop) space.read(data.entry, null, SPACE_TIMEOUT);
				if (ee != null) {
					operator.Tokens matchTokens =  ee.getMatchTokens();
					if (matchTokens != null) {
						if (matchTokens.getType().equals("LIST")) {
							boolean isOK = true;
							for (Object obj : matchTokens) {
								if (obj instanceof Tokens) {
									if (((Tokens) obj).getType().equals("OS")) {
										if (!matchTokens.contains(data.osName)) {
											logger.debug("########### Provider does NOT match OS name ...");
											isOK = false;
										}
									}
									if (((Tokens) obj).getType().equals("APP")) {
										if (!matchTokens.containsAll(data.appNames)) {
											logger.debug("########### Provider does NOT match App names ...");
											isOK = false;
										}
									}
								}
							}
							if (!isOK) {
								Thread.sleep(SPACE_TIMEOUT / 2);
								continue;
							}
						} else if (matchTokens.getType().equals("OS")) {
							if (!matchTokens.contains(data.osName)) {
								logger.debug("########### Provider does NOT match OS name ...");

							}
						} else if (matchTokens.getType().equals("APP")) {
							if (!matchTokens.containsAll(data.appNames)) {
								logger.debug("########### Provider does NOT match App names ...");
								Thread.sleep(SPACE_TIMEOUT / 2);
								continue;
							}
						}
						Thread.sleep(SPACE_TIMEOUT / 2);
						continue;
					}
				} else {
					continue;
				}

				Transaction.Created txnCreated = null;
				// logger.info("worker space template envelop = "
				// + data.entry.describe() + "\n service provider = "
				// + provider);
				if (isTransactional) {
					txnCreated = createTransaction();
					if (txnCreated == null)
						logger.error("########### SpaceOSTaker DID NOT get transaction ...");
				}

				ee = (ExertionEnvelop) space
					.take(data.entry, txnCreated.transaction, SPACE_TIMEOUT);
				// if (ee != null) {
				// logger.info("...................got entry...................\n"
				// + ee.describe());
				// }
				// after 'take' timeout first cleanup and sleep for a while
				// before 'taking' mograms again
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
				
				pool.execute(new SpaceWorker(ee, txnCreated, data.provider, remoteLogging));
			} catch (Exception ex) {
				continue;
			}
		}
	}

}