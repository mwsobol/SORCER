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

import com.sun.jini.landlord.Landlord;
import com.sun.jini.landlord.LeaseFactory;
import com.sun.jini.landlord.LeasedResource;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.id.ReferentUuid;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.exertmonitor.MonitorSession;
import sorcer.util.Sorcer;

import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static sorcer.util.StringUtils.tName;

public class MonitorLandlord implements Landlord, Runnable, ReferentUuid, Remote {
    private transient LeaseFactory lFactory;
    private transient Uuid landlordUuid;
    private transient Landlord proxy;
    private volatile boolean run = true;

    static transient final Logger logger = LoggerFactory.getLogger(MonitorLandlord.class);

    // A simple leasing policy...10 minute leases.
    protected static final long DEFAULT_MAX_LEASE = TimeUnit.MINUTES.toMillis(10);
    protected long maxLease = DEFAULT_MAX_LEASE;
    protected static final long DEFAULT_SLEEP_TIME = TimeUnit.SECONDS.toMillis(3);
    public static Map<Uuid, LeasedResource> resources;

    public MonitorLandlord() throws ExportException {
        resources = new HashMap<>();
        landlordUuid = UuidFactory.generate();
        export();
        this.lFactory = new LeaseFactory(proxy, landlordUuid);
    }

    public void export() throws ExportException {
        BasicJeriExporter exporter;
        try {
            exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(Sorcer.getLocalHost().getHostAddress(), 0),
                                             new BasicILFactory());
        } catch (UnknownHostException e) {
            logger.warn("Could not resolve hostAddress - starting on default interface");
            exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0), new BasicILFactory());
        }
        proxy = (Landlord) exporter.export(this);
        Thread llt = new Thread(this, tName("MonitorLandlord.checkLeases"));
        llt.setDaemon(true);
        llt.start();
    }

    public Object getServiceProxy() {
        return proxy;
    }

    public Lease newLease(LeasedResource resource) {
        resources.put(resource.getCookie(), resource);
        return lFactory.newLease(resource.getCookie(), resource.getExpiration());
    }

    // Change the maximum lease time from the default.
    public void setMaxLease(int maxLease) {
        this.maxLease = maxLease;
    }

    // Apply the policy to a requested duration
    // to getValue an actual expiration time.
    public long getExpiration(long request) {
        if (request > maxLease || request == Lease.ANY)
            return System.currentTimeMillis() + maxLease;
        else
            return System.currentTimeMillis() + request;
    }

    // Cancel the lease represented by 'cookie'
    public void cancel(Uuid cookie) throws UnknownLeaseException {
        MonitorLeasedResource resource;
        resource = (MonitorLeasedResource) resources.get(cookie);
        if (resource != null) {
            resource.leaseCancelled();
            return;
        }
        throw new UnknownLeaseException(cookie.toString());
    }

    // Cancel a setValue of leases
    public Map cancelAll(Uuid[] cookies) throws RemoteException {
        Map exceptionMap = null;
        for (int i = 0; i < cookies.length; i++) {
            try {
                cancel(cookies[i]);
            } catch (UnknownLeaseException ex) {
                if (exceptionMap == null) {
                    exceptionMap = new HashMap();
                }
                exceptionMap.put(cookies[i], ex);
            }
        }
        return exceptionMap;
    }

    // Renew the lease specified by 'cookie'
    public long renew(Uuid cookie, long extension)
        throws UnknownLeaseException, LeaseDeniedException, RemoteException {

        MonitorLeasedResource resource;
        resource = (MonitorLeasedResource) resources.get(cookie);
        if (resource != null) {
            long expiration = getExpiration(extension);
            resource.setExpiration(expiration);
            // logger.info("Lease renewd for resource ="+resource+
            // " next lease duration="+ (expiration -
            // System.currentTimeMillis()));

            return expiration - System.currentTimeMillis();
        }
        throw new UnknownLeaseException(cookie.toString());
    }

    // Renew a setValue of leases.
    public Landlord.RenewResults renewAll(Uuid[] cookies, long[] extensions)
        throws RemoteException {
        long[] granted = new long[cookies.length];
        Exception[] denied = null;

        for (int i = 0; i < cookies.length; i++) {
            try {
                granted[i] = renew(cookies[i], extensions[i]);
            } catch (Exception ex) {
                if (denied == null) {
                    denied = new Exception[cookies.length + 1];
                }
                denied[i + 1] = ex;
            }
        }
        Landlord.RenewResults results = new Landlord.RenewResults(granted, denied);
        logger.info( "leases renewed Landlord.RenewResults="+ results);
        return results;
    }

    public void run() {
        long timeToSleep = DEFAULT_SLEEP_TIME;
        while (run) {
            long nextWakeup = System.currentTimeMillis() + timeToSleep;
            try {
                Thread.sleep(timeToSleep);
            } catch (InterruptedException ex) {
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
        // logger.info("Checking for leases and time outs");
        MonitorLeasedResource resource;
        Uuid cookie;
        long now = System.currentTimeMillis();
        for (Map.Entry<Uuid, LeasedResource> entry : resources.entrySet()) {
            resource = (MonitorLeasedResource) entry.getValue();
            String name = (((MonitorSession)resource).getRuntimeExertion()!=null ?
                           ((MonitorSession)resource).getRuntimeExertion().getName() : " NOT EXERTION");
            SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
            logger.info("Checking lease: " + name + " " + sdf.format(Math.min(resource.getTimeout(), resource.getExpiration()))
                        + (resource.getExpiration()<resource.getTimeout() ? " E" : " T"));

            if (resource.getExpiration() < now) {
                logger.info( "Lease cancelled for resource ="+ resource);
                resource.leaseCancelled();
                resources.remove(resource.getCookie());
            } /*else if (resource.getTimeout() < now) {
                logger.info( "Timeout for resource =" + resource
                             + " resource.getTimeout()=" + resource.getTimeout()
                             + " now=" + now + " resource.getTimeout()-now="
                             + (resource.getTimeout() - now));
                resource.timedOut();
                resources.remove(resource.getCookie());
            }*/
        }

    }

    public void remove(LeasedResource lr) {
        logger.info( "Removing landlord resource =" + lr);
        resources.remove(lr.getCookie());
    }

    public Map getResources() {
        return resources;
    }

    public Uuid getReferentUuid() {
        return landlordUuid;
    }

    public void terminate() {
        run = false;
    }
}
