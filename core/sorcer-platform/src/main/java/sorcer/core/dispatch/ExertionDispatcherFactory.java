    /*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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

package sorcer.core.dispatch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jini.core.lease.Lease;
import net.jini.lease.LeaseRenewalManager;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.monitor.MonitorUtil;
import sorcer.core.monitor.MonitoringSession;
import sorcer.core.provider.Cataloger;
import sorcer.core.Dispatcher;
import sorcer.core.provider.Provider;
import sorcer.core.exertion.Jobs;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.*;

import static sorcer.core.monitor.MonitorUtil.getMonitoringSession;

/**
 * This class creates instances of appropriate subclasses of Dispatcher. The
 * appropriate subclass is determined by calling the ServiceJob object's
 */
public class ExertionDispatcherFactory implements DispatcherFactory {
    public static Cataloger catalog; // The service catalog object
    private final static Logger logger = LoggerFactory.getLogger(ExertionDispatcherFactory.class.getName());

    private LokiMemberUtil loki;

    public static final long LEASE_RENEWAL_PERIOD = 1 * 1000 * 60L;
    public static final long DEFAULT_TIMEOUT_PERIOD = 1 * 1000 * 90L;

    protected ExertionDispatcherFactory(LokiMemberUtil loki){
        this.loki = loki;
	}

	public static DispatcherFactory getFactory() {
		return new ExertionDispatcherFactory(null);
	}

	public static DispatcherFactory getFactory(LokiMemberUtil loki) {
		return new ExertionDispatcherFactory(loki);
	}

    public Dispatcher createDispatcher(Exertion exertion,
                                       Set<Context> sharedContexts,
                                       boolean isSpawned,
                                       Provider provider) throws DispatcherException {
        Dispatcher dispatcher = null;
        ProvisionManager provisionManager = null;
        List<ServiceDeployment> deployments = ((ServiceExertion)exertion).getDeployments();
        if (deployments.size() > 0 && (((ServiceSignature) exertion.getProcessSignature()).isProvisionable() || exertion.isProvisionable()))
            provisionManager = new ProvisionManager(exertion);

        try {
            if(exertion instanceof Job)
                exertion = new ExertionSorter(exertion).getSortedJob();

			if (Jobs.isCatalogBlock(exertion) && exertion instanceof Block) {
				logger.info("Running Catalog Block Dispatcher...");
                dispatcher = new CatalogBlockDispatcher(exertion,
						                                  sharedContexts,
						                                  isSpawned,
						                                  provider,
                         provisionManager);
			} else if (isSpaceSequential(exertion)) {
				logger.info("Running Space Sequential Dispatcher...");
				dispatcher = new SpaceSequentialDispatcher(exertion,
						                                  sharedContexts,
						                                  isSpawned,
						                                  loki,
						                                  provider,
                        provisionManager);
			}
            if (dispatcher==null && exertion instanceof Job) {
                Job job = (Job) exertion;
                if (Jobs.isSpaceParallel(job)) {
                    logger.info("Running Space Parallel Dispatcher...");
                    dispatcher = new SpaceParallelDispatcher(job,
                            sharedContexts,
                            isSpawned,
                            loki,
                            provider,
                            provisionManager);
                } else if (Jobs.isCatalogParallel(job)) {
                    logger.info("Running Catalog Parallel Dispatcher...");
                    dispatcher = new CatalogParallelDispatcher(job,
                            sharedContexts,
                            isSpawned,
                            provider,
                            provisionManager);
                } else if (Jobs.isCatalogSequential(job)) {
                    logger.info("Running Catalog Sequential Dispatcher...");
                    dispatcher = new CatalogSequentialDispatcher(job,
                            sharedContexts,
                            isSpawned,
                            provider,
                            provisionManager);
                }
            }
            assert dispatcher != null;
            MonitoringSession monSession = MonitorUtil.getMonitoringSession(exertion);
            if (exertion.isMonitorable() && monSession!=null) {
                logger.debug("Initializing monitor session for : " + exertion.getName());
                if (!(monSession.getState()==Exec.INSPACE)) {
                    monSession.init((Monitorable) provider.getProxy(), LEASE_RENEWAL_PERIOD,
                            DEFAULT_TIMEOUT_PERIOD);
                } else {
                    monSession.init((Monitorable)provider.getProxy());
                }
                LeaseRenewalManager lrm = new LeaseRenewalManager();
                lrm.renewUntil(monSession.getLease(), Lease.FOREVER, LEASE_RENEWAL_PERIOD, null);
                dispatcher.setLrm(lrm);

                logger.debug("Exertion state: " + Exec.State.name(exertion.getStatus()));
                logger.debug("Session for the exertion = " + monSession);
                logger.debug("Lease to be renewed for duration = " +
                        (monSession.getLease().getExpiration() - System
                                .currentTimeMillis()));
            }

            logger.info("*** tally of used dispatchers: " + ExertDispatcher.getDispatchers().size());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DispatcherException(
                    "Failed to create the exertion dispatcher for job: "+ exertion.getName(), e);
        }
        return dispatcher;
    }

    protected boolean isSpaceSequential(Exertion exertion) {
        if(exertion instanceof Job) {
            Job job = (Job) exertion;
            return Jobs.isSpaceSingleton(job) || Jobs.isSpaceSequential(job);
        }
        return Jobs.isSpaceBlock(exertion);
    }

    /**
     * Returns an instance of the appropriate subclass of Dispatcher as
     * determined from information provided by the given Job instance.
     *
     * @param exertion
     *            The SORCER job that will be used to perform a collection of
     *            components exertions
     */
    @Override
    public Dispatcher createDispatcher(Exertion exertion, Provider provider, String... config) throws DispatcherException {
        return createDispatcher(exertion, new HashSet<Context>(), false, provider);
    }

    @Override
    public SpaceTaskDispatcher createDispatcher(Task task, Provider provider, String... config) throws DispatcherException {
        ProvisionManager provisionManager = null;
        List<ServiceDeployment> deployments = task.getDeployments();
        if (deployments.size() > 0)
            provisionManager = new ProvisionManager(task);

        logger.info("Running Space Task Dispatcher...");
        try {
            return new SpaceTaskDispatcher(task,
                    new HashSet<Context>(),
                    false,
                    loki,
                    provisionManager);
        } catch (ContextException e) {
            throw new DispatcherException(
                    "Failed to create the exertion dispatcher for job: "+ task.getName(), e);
        } catch (ExertionException e) {
            throw new DispatcherException(
                    "Failed to create the exertion dispatcher for job: "+ task.getName(), e);
        }
    }
}
