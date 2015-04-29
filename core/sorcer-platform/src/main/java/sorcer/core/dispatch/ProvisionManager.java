/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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


import org.rioproject.deploy.DeployAdmin;
import org.rioproject.impl.opstring.OpString;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import sorcer.core.deploy.OperationalStringFactory;
import sorcer.core.deploy.ProvisionMonitorCache;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.service.Exertion;
import sorcer.service.ServiceExertion;
import sorcer.service.Signature;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code ProvisionManager} handles the dynamic creation of {@link OperationalString}s created
 * from {@link Exertion}s.
 *
 * @author Dennis Reedy
 * @author Mike Sobolewski
 */
public class ProvisionManager {
	private static final Logger logger = LoggerFactory.getLogger(ProvisionManager.class.getName());
	private final Exertion exertion;
    private final List<String> deploymentNames = new ArrayList<String>();
    private final Map<ServiceDeployment.Unique, List<OperationalString>> deployments;
    private final ProvisionMonitorCache provisionMonitorCache;
	private volatile DeployAdmin deployAdmin;

	public ProvisionManager(final Exertion exertion) throws DispatcherException {
		this.exertion = exertion;
        try {
            deployments = OperationalStringFactory.create(exertion);
        } catch (Exception e) {
            throw new DispatcherException(String.format("While trying to create deployment for exertion %s",
                                                        exertion.getName()),
                                          e);
        }
        provisionMonitorCache = ProvisionMonitorCache.getInstance();
	}

    public DeployAdmin getDeployAdmin() {
        return deployAdmin;
    }

    private synchronized void doGetDeployAdmin() throws RemoteException {
        if(deployAdmin==null) {
            logger.info(String.format("Discover a DeployAdmin reference using %s ...",
                                      getDiscoveryInfo()));
            deployAdmin = provisionMonitorCache.getDeployAdmin();
        }
    }

    public List<String> getDeploymentNames() {
        List<String> names = new ArrayList<String>();
        names.addAll(deploymentNames);
        return names;
    }

    public synchronized boolean deployServicesSync() throws DispatcherException {
        if(!deployServices()) {
            return false;
        }

        List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();
        for (Map.Entry<ServiceDeployment.Unique, List<OperationalString>> entry : deployments.entrySet()) {
            for(OperationalString deployment : entry.getValue()) {
                tasks.add(new DeploymentFutureTask(deployment));
            }
        }
        final ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        final ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(pool);
        for(Callable<Boolean> task : tasks) {
            completionService.submit(task);
        }
        boolean deployed = true;
        for(int i=0; i<tasks.size(); i++) {
            try {
                if(!completionService.take().get()) {
                    deployed = false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return deployed;
    }

    public boolean deployServices() throws DispatcherException {
        if(deployments.isEmpty()) {
            return false;
        }
        try {
            doGetDeployAdmin();
            if (deployAdmin != null) {
                for (Map.Entry<ServiceDeployment.Unique, List<OperationalString>> entry : deployments.entrySet()) {
                    for (OperationalString deployment : entry.getValue()) {
                        if(logger.isDebugEnabled())
                            logger.debug(String.format("Processing deployment %s", deployment.getName()));
                        if(deployAdmin.hasDeployed(deployment.getName())) {
                            if (entry.getKey() == ServiceDeployment.Unique.YES) {
                                String newName = createDeploymentName(deployment.getName(),
                                                                      deployAdmin.getOperationalStringManagers());
                                if(logger.isDebugEnabled())
                                    logger.debug(String.format("Deployment for %s already exists, created new name [%s], " +
                                                          "proceed with autonomic deployment",
                                                          deployment.getName(), newName));
                                ((OpString)deployment).setName(newName);
                            } else {
                                if(logger.isDebugEnabled())
                                    logger.debug(String.format("Deployment for %s already exists",
                                                          deployment.getName()));
                                if(!deploymentNames.contains(deployment.getName()))
                                    deploymentNames.add(deployment.getName());
                                continue;
                            }
                        } else {
                            if(logger.isDebugEnabled())
                                logger.debug(String.format(
                                        "Deployment for %s not found, request autonomic deployment",
                                        deployment.getName()));
                        }
                        deployAdmin.deploy(deployment);
                        if(!deploymentNames.contains(deployment.getName()))
                            deploymentNames.add(deployment.getName());
                    }
                }
                ServiceDeployment deployment = (ServiceDeployment)exertion.getProcessSignature().getDeployment();
                if(deployment!=null) {
                    deployment.setDeployedNames(deploymentNames);
                } else {
                    logger.warn("There was no ServiceDeployment for %s, "+exertion.getName()+" "+
                                   "try and load all NetSignatures and set deployment names");
                    for (Signature netSignature : getNetSignatures()) {
                        if (netSignature.getDeployment() == null)
                            continue;
                        ((ServiceDeployment) netSignature.getDeployment()).setDeployedNames(deploymentNames);
                    }
                }
            } else {
                String message = String.format("Unable to obtain a ProvisionMonitor for %s using %s",
                                               exertion.getName(),
                                               getDiscoveryInfo());
                logger.warn(message);
                throw new DispatcherException(message);
            }
        } catch (Exception e) {
            if(e instanceof DispatcherException)
                throw (DispatcherException)e;

            logger.warn(
                       String.format("Unable to process deployment for %s", exertion.getName()),
                       e);
            throw new DispatcherException(String.format("While trying to provision exertion %s", exertion.getName()), e);
        }
        return true;
    }

    public void undeploy() {
        if(deployAdmin==null) {
            logger.warn("Unable to undeploy, there is no known DeployAdmin ");
            return;
        }
        List<String> removals = new ArrayList<String>();
        for(String deploymentName : deploymentNames) {
            try {
                deployAdmin.undeploy(deploymentName);
                while(deployAdmin.hasDeployed(deploymentName)) {
                    Thread.sleep(1000);
                }
                removals.add(deploymentName);
            } catch (Exception e) {
                logger.warn("Unable to undeploy "+deploymentName+", "+e.getMessage());
            }
        }
        for(String remove : removals) {
            deploymentNames.remove(remove);
        }
    }

    private String createDeploymentName(final String baseName, OperationalStringManager... managers) throws RemoteException {
        int known = 0;
        for(OperationalStringManager manager : managers) {
            if(manager.getOperationalString().getName().startsWith(baseName)) {
                known++;
            }
        }
        return String.format("%s-(%s)", baseName, known);
    }

    private String getDiscoveryInfo() {
        StringBuilder discoveryInfo = new StringBuilder();
        String groups = provisionMonitorCache.getGroups();
        String locators = provisionMonitorCache.getLocators();
        if(groups!=null) {
            discoveryInfo.append("groups: ").append(groups);
        }
        if(locators!=null) {
            if(discoveryInfo.length()>0)
                discoveryInfo.append(",");
            discoveryInfo.append("locators: ").append(locators);
        }
        return discoveryInfo.toString();
    }

    private Iterable<Signature> getNetSignatures() {
        List<Signature> signatures = new ArrayList<Signature>();
        if(exertion instanceof ServiceExertion) {
            ServiceExertion serviceExertion = (ServiceExertion)exertion;
            signatures.addAll(serviceExertion.getAllNetTaskSignatures());
        }
        return signatures;
    }

    class DeploymentFutureTask implements Callable<Boolean> {
        final OperationalString deployment;

        DeploymentFutureTask(final OperationalString deployment) {
            this.deployment = deployment;
        }

        boolean waitForDeployment()  {
            try {
                OperationalStringManager mgr = null;
                while(mgr==null) {
                    try {
                        mgr = deployAdmin.getOperationalStringManager(deployment.getName());
                    } catch (OperationalStringException ignore) {}
                    Thread.sleep(500);
                }
                Map<ServiceElement, Integer> deploy = new HashMap<ServiceElement, Integer>();
                int total = 0;
                for (ServiceElement elem: deployment.getServices()) {
                    deploy.put(elem, 0);
                    total += elem.getPlanned();
                }
                int deployed = 0;
                int counter = 0;
                List<String> deployedServices = new ArrayList<String>();
                while (deployed < total) {
                    counter++;
                    deployed = 0;
                    for (Map.Entry<ServiceElement, Integer> entry: deploy.entrySet()) {
                        int numDeployed = entry.getValue();
                        ServiceElement elem = entry.getKey();
                        if (numDeployed < elem.getPlanned()) {
                            try {
                                numDeployed = mgr.getServiceBeanInstances(elem).length;
                                deploy.put(elem, numDeployed);
                                if(counter % 50 == 0)
                                System.out.println(String.format(
                                        "Service %s/%-12s is pending. Planned [%s], deployed [%d]",
                                        elem.getOperationalStringName(),
                                        elem.getName(),
                                        elem.getPlanned(),
                                        numDeployed));
                            } catch (OperationalStringException notReady) {
                                if(logger.isTraceEnabled())
                                    logger.trace(notReady.getMessage());
                            }
                        } else {
                            String name = String.format("%s/%s", elem.getOperationalStringName(), elem.getName());
                            if(!deployedServices.contains(name)) {
                                if(logger.isDebugEnabled()) {
                                    System.out.println(String.format(
                                            "Service %s/%-12s is deployed. Planned [%s], deployed [%d]",
                                            elem.getOperationalStringName(),
                                            elem.getName(),
                                            elem.getPlanned(),
                                            numDeployed));
                                }
                                deployedServices.add(name);
                            }
                            deployed += elem.getPlanned();
                        }
                    }
                    if (deployed < total) {
                        Thread.sleep(500);
                    }
                }
            } catch(Exception e) {
                logger.warn("Failed waiting for deployment ["+deployment+"]", e);
                return false;
            }
            return true;
        }

        public Boolean call() throws Exception {
            return waitForDeployment();
        }
    }

}
