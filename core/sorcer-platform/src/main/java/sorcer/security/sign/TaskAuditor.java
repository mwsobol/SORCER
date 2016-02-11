/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.security.sign;

/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.LookupDiscovery;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryManager;
import sorcer.core.context.ServiceContext;
import sorcer.service.Service;

import javax.security.auth.Subject;
import java.security.AccessController;

/**
 * <p>
 * All secure data or transactions in SCAF are saved using an Auditor Service.
 * This class starts a new worker thread that looks for Auditor Service and then
 * calls auditor method of that to save the task submitted.
 */

public class TaskAuditor {
	/**
	 * Auditor Service Provider
	 */
	protected sorcer.core.provider.Auditor auditor;

	/**
	 * Audits the SignedServiceTask.
	 * 
	 * @param task
	 *            a signed service task that needs to be saved.
	 */
	public void audit(SignedServiceTask task) {
		try {
			Thread thread = new Thread(new AuditThread(task));
			thread.start();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	/**
	 * Inner class of <code>TaskAuditor<</code> whih starts a new thread to
	 * start looking for Auditor Provider. It takes the Subject from
	 * AccessController and makes a new ServiceContext with this subject and the
	 * task that needs to be saved.
	 * 
	 * @author Saurabh Bhatla
	 * @see TaskAuditor
	 */
	protected class AuditThread implements Runnable {
		/**
		 * Subject that is received from client
		 */
		Subject subject = null;

		/**
		 * Context that takes subject and task to provider
		 */
		ServiceContext ctx = null;

		/**
		 * Default Constructor.
		 * 
		 * It takes the Subject from AccessController and makes a new
		 * ServiceContext with this subject and the task that needs to be saved.
		 * 
		 * @param task
		 *            a signed service task that needs to be saved.
		 */
		public AuditThread(SignedServiceTask task) {
			try {
				Subject subject = Subject.getSubject(AccessController
						.getContext());
				ctx = new ServiceContext("Auditor" + this);
				System.out.println("subject=" + subject);
				if (subject != null)
					ctx.putValue("SUBJECT", subject);
				ctx.putValue("TASK", task);
				this.ctx = ctx;
				System.out.println("ctx=" + ctx);
			} catch (Exception ex) {
				System.out.println("Error in AuditThread");
				ex.printStackTrace();
			}
			// auditor.audit(ctx);
		}

		/**
		 * Looks for auditor.
		 * 
		 * @return Auditor that was found
		 */
		public sorcer.core.provider.Auditor getAuditor() {
			final int MAX_TRIES = 100;
			ServiceDiscoveryManager sdm = null;
			LookupCache lCache1 = null;
			final int SLEEP_TIME = 100;
			try {
				LookupDiscovery disco = new LookupDiscovery(
						LookupDiscovery.ALL_GROUPS);
				sdm = new ServiceDiscoveryManager(disco,
						new LeaseRenewalManager());
				lCache1 = sdm.createLookupCache(new ServiceTemplate(null,
						new Class[] { Service.class }, null),
						null, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			int tries = 0;
			while (tries < MAX_TRIES) {
				ServiceItem[] items = (lCache1.lookup(null, Integer.MAX_VALUE));
				for (int i = 0; i < items.length; i++)
					if (items[i].service != null
							&& items[i].service instanceof sorcer.core.provider.Auditor) {
						System.out.println(tries + "" + items[i].service);
						System.out.println("GOT Auditor  SERVICE - SERVICE ID="
								+ items[i].serviceID);
						return (sorcer.core.provider.Auditor) items[i].service;
					}
				tries++;
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (Exception e) {
				}
			}
			// Util.debug(this, tries+"null");
			return null;
		}

		/**
		 * Causes a new thread to be spawned.
		 */
		public void run() {
			try {
				if (auditor == null)
					auditor = getAuditor();
				auditor.audit(ctx);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
