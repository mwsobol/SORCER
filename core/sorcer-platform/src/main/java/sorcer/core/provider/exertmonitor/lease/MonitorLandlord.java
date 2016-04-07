/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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

package sorcer.core.provider.exertmonitor.lease;

import com.sun.jini.landlord.LeasedResource;
import net.jini.config.Configuration;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.id.Uuid;
import org.rioproject.impl.service.LandlordLessor;
import org.rioproject.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.exertmonitor.MonitorSession;
import sorcer.service.MonitorException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static sorcer.util.StringUtils.tName;

public class MonitorLandlord implements Runnable {

	public interface MonitorLeasedResource extends LeasedResource {

		void leaseCancelled();

		void setTimeout(long timeoutDuration);

		long getTimeout();

		void timedOut();
	}

	private LandlordLessor landlord;
	private volatile boolean run = true;
	private static final Logger logger = LoggerFactory.getLogger(MonitorLandlord.class);
	private static final long DEFAULT_SLEEP_TIME = TimeUnit.SECONDS.toMillis(3);

	public MonitorLandlord(Configuration config) throws IOException {
		landlord = new LandlordLessor(config);
        Thread llt = new Thread(this, tName("MonitorLandlord.checkLeases"));
        llt.setDaemon(true);
        llt.start();
	}

	public Lease newLease(LeasedResource resource, long duration) throws MonitorException {
		try {
            Lease lease = landlord.newLease(resource, duration);
            SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
            logger.info("Granted new lease, requested duration: {}, expiration: {}",
                        TimeUtil.format(duration), sdf.format(lease.getExpiration()));
			return lease;
		} catch (LeaseDeniedException e) {
			logger.warn("Failed granting new lease", e);
			throw new MonitorException("Failed granting new lease", e);
		}
	}

	public void run() {
		long timeToSleep = DEFAULT_SLEEP_TIME;
		while (run) {
			long nextWakeup = System.currentTimeMillis() + timeToSleep;
			try {
				Thread.sleep(timeToSleep);
			} catch (InterruptedException ex) {
                //ignore
			}

			long currentTime = System.currentTimeMillis();
			// see if we're at the next wakeup time
			if (currentTime >= nextWakeup) {
				nextWakeup = currentTime + DEFAULT_SLEEP_TIME;
				// notify
				checkLeasesAndTimeouts();
			}
			timeToSleep = nextWakeup - System.currentTimeMillis();
		}
	}

	public void checkLeasesAndTimeouts() {
		MonitorLeasedResource resource;
		long now = System.currentTimeMillis();
        List<MonitorLeasedResource> removals = new ArrayList<>();
        for(LeasedResource leasedResource : landlord.getLeasedResources())  {
            resource = (MonitorLeasedResource) leasedResource;
            String name = (((MonitorSession)resource).getRuntimeExertion()!=null ?
                           ((MonitorSession)resource).getRuntimeExertion().getName() : " NOT EXERTION");
            SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
            logger.info("Checking lease: {} {}", name, sdf.format(resource.getExpiration()));

			if (resource.getExpiration() < now) {
                removals.add(resource);
			}
		}
        for(MonitorLeasedResource m : removals) {
            logger.info("Lease cancelled for resource = {}", m);
            m.leaseCancelled();
            remove(m);
        }
	}

	public void remove(LeasedResource lr) {
		logger.info("Removing landlord resource = {}", lr);
        try {
            landlord.cancel(lr.getCookie());
        } catch (UnknownLeaseException e) {
            logger.debug("Can not cancel unknown lease");
        }
    }

	public Map<Uuid, LeasedResource> getResources() {
        Map<Uuid, LeasedResource> resources = new HashMap<>();
        for(LeasedResource sr : landlord.getLeasedResources()) {
            resources.put(sr.getCookie(), sr);
        }
        return resources;
	}
	
	public void terminate() {
		run = false;
        landlord.stop();
	}
}
