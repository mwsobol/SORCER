/*
 * Copyright 2014 Sorcersoft.com S.A.
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

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.deploy.DeploymentResult;
import org.rioproject.entry.OperationalStringEntry;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.impl.opstring.OpString;
import org.rioproject.opstring.*;
import org.rioproject.resolver.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.co.tuple.Tuple2;
import sorcer.co.tuple.Tuple3;
import sorcer.core.signature.NetSignature;
import sorcer.ext.Provisioner;
import sorcer.ext.ProvisioningException;
import sorcer.jini.lookup.AttributesUtil;
import sorcer.service.Accessor;
import sorcer.service.ServiceDirectory;
import sorcer.service.Signature;
import sorcer.service.SignatureException;
import sorcer.util.SorcerEnv;
import sorcer.util.rio.OpStringUtil;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ServiceDirectoryProvisioner implements Provisioner {

    private static Logger logger = LoggerFactory.getLogger(ServiceDirectoryProvisioner.class);

    private ServiceDirectory srvDirectory = null;

    private ProvisionMonitor provisionMonitor = null;

    private static ServiceDirectoryProvisioner instance = null;

    // Set default undeploy option. Undeploy after 60 seconds of inactivity
    private static final int UNDEPLOY_DEFAULT_IDLE_TIME = 60;

    private int undeployIdleTime = UNDEPLOY_DEFAULT_IDLE_TIME;


    private Set<Tuple3> provisioningQueue = Collections.synchronizedSet(new HashSet<Tuple3>());

    public ServiceDirectoryProvisioner() {
        String propIdleTime = SorcerEnv.getProperty("provisioning.idle.time");
        if (propIdleTime!=null) {
            try {
                undeployIdleTime = Integer.parseInt(propIdleTime);
            } catch (NumberFormatException ne) {
                logger.warn("Could not parse property: provisioning.idle.time: " + propIdleTime + " using default idle time of 60 seconds");
            }
        }
    }

    public static Provisioner getProvisioner() {
        if (instance==null)
            instance = new ServiceDirectoryProvisioner();
        return instance;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T provision(Signature sig) throws ProvisioningException {
        String typeName = null;
        try {
            typeName = (sig.getServiceType()!=null ? sig.getServiceType().getName() : null);
        } catch (SignatureException e) {
            throw new ProvisioningException(e);
        }
        String name = (sig.getProviderName()!=null ? sig.getProviderName().getName() : "*");
        String version = ((sig instanceof NetSignature) ? ((NetSignature)sig).getVersion() : null);
        logger.warn("called provision {} {} {}", typeName, version, name);
        Tuple3 provT = new Tuple3(typeName, (version!=null ? version : "NULL"), (name!=null ? name : "NULL"));
        if (!provisioningQueue.contains(provT)) {
            provisioningQueue.add(provT);
        } else {
            while (provisioningQueue.contains(provT)) {
                logger.debug("already provisioning {} {} {}, waiting!!!", typeName, version, name);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
            }
            T service = null;
            try {
                service = (T) Accessor.get().getService(sig);
            } catch (SignatureException e) {
                throw new ProvisioningException(e);
            }
            if (service!=null) return service;
        }


        OperationalString operationalString = null;
        if (srvDirectory == null) srvDirectory = getServiceDirectory();
        if (srvDirectory == null) removeFromQueueAndThrowException(provT, "ServiceDirectory (Almanac) service not found!", null);
        try {
            operationalString = srvDirectory.getOpString(typeName, version, name);
        } catch (RemoteException re) {
            srvDirectory = getServiceDirectory();
            if (srvDirectory!=null) {
                try {
                    operationalString = srvDirectory.getOpString(typeName, version, name);
                } catch (RemoteException ree) {
                    removeFromQueueAndThrowException(provT, ree.getMessage(), ree);
                }
            }

        }
        if (operationalString == null) {
            String msg = "Service " + typeName + " " + name + " " + version + " not installed in the Almanac database";
            logger.warn(msg);
            removeFromQueueAndThrowException(provT, msg, null);
        }
        logger.debug("Got opString to provision: {}", operationalString.getName());
        T service = null;
        logger.debug("UndeployIdleTime: {}", undeployIdleTime);
        if (undeployIdleTime>0) ((OpString)operationalString).setUndeployOption(getUndeployOption(undeployIdleTime));
        String opStringName = operationalString.getName() + "_" + UUID.randomUUID();
        ((OpString)operationalString).setName(opStringName);
        try {
            DeploymentResult deploymentResult = getDeployAdmin().deploy(operationalString);
            StringBuilder deployErrors = new StringBuilder();
            for (String key : deploymentResult.getErrorMap().keySet()) {
                String msg = "Got error from deployAdmin: " + key + " " + deploymentResult.getErrorMap().get(key).getMessage();
                deployErrors.append(msg);
                logger.info(msg);
            }
            if (!deployErrors.toString().isEmpty())
                throw new ProvisioningException(deployErrors.toString());

            Tuple2<ServiceElement, ClassBundle> classSource = findBundleWithClass(operationalString, typeName, version);
            if (classSource == null)
                throw new ProvisioningException("OperationalString " + name + " doesn't contain definition for " + typeName + "@" + version);
            Class type = OpStringUtil.loadClass(classSource.value(), classSource.key());
            int tries = 0;
            while (tries < 8 && service == null) {
                Thread.sleep(100);
                Entry[] entries = new Entry[]{ new OperationalStringEntry(opStringName) };
                ServiceTemplate template = new ServiceTemplate(null, new Class[]{type}, entries);
                ServiceItem[] items = Accessor.get().getServiceItems(template, null);
                if(items.length>0)
                    service = (T) items[0].service;
                tries++;
            }
            logger.debug(" Found: " + (service != null) + " after times: " + tries);
            if (service == null) {
                ServiceElement sEl = deploymentResult.getOperationalStringManager().getServiceElement(new String[]{typeName}, name);
                if (sEl != null) {
                    logger.warn("trying to redeploy serviceElement {}", sEl.getName());
                    deploymentResult.getOperationalStringManager().increment(sEl, false, null);
                    //deploymentResult.getOperationalStringManager().redeploy(sEl, null, false, 0, null);
                    logger.warn("trying to redeploy {} {} {}", typeName, version, name);
                    while (tries < 15 && service == null) {
                        Thread.sleep(100);
                        ServiceTemplate template = new ServiceTemplate(null,
                                                                       new Class[]{type},
                                                                       new Entry[]{new OperationalStringEntry(operationalString.getName())});
                        ServiceItem[] items = Accessor.get().getServiceItems(template, null);
                        if(items.length>0)
                            service = (T)items[0].service;
                        tries++;
                    }
                } else {
                    logger.warn("redeployment unsuccessful, getting operational string from RIO - null");
                }
            }
        } catch (ProvisioningException pe) {
            logger.warn(pe.getMessage());
            removeFromQueueAndThrowException(provT, pe.getMessage(), pe);
        } catch (Exception e) {
            logger.warn("OpString Error", e);
            removeFromQueueAndThrowException(provT, "Could not parse operational string", e);
        }
        if (service != null) {
            provisioningQueue.remove(provT);
            return service;
        }
        else
            removeFromQueueAndThrowException(provT, "Timed out waiting for the provisioned service to appear: " + typeName + " " + name + " " + version, null);
        throw new ProvisioningException("This line should never be reached!");
    }

    private void removeFromQueueAndThrowException(Tuple3 provTuple, String msg, Exception e) throws ProvisioningException {
        provisioningQueue.remove(provTuple);
        throw new ProvisioningException(msg, e);
    }


    public void unProvision(ServiceID serviceId) throws ProvisioningException {
        ServiceItem[] serviceItems = Accessor.get().getServiceItems(new ServiceTemplate(serviceId, null, null), null);
        OperationalStringEntry opStringEntry = null;
        if(serviceItems.length>0)
            opStringEntry = AttributesUtil.getFirstByType(serviceItems[0].attributeSets, OperationalStringEntry.class);
        if (opStringEntry == null)
            throw new IllegalArgumentException("Service was not provisioned");
        ServiceItem[] provisionMonitors = Accessor.get().getServiceItems(new ServiceTemplate(null, new Class[]{ProvisionMonitor.class}, null),
                                                                         null);
        for (ServiceItem monitor : provisionMonitors) {
            try {
                ((DeployAdmin) ((ProvisionMonitor) monitor.service).getAdmin()).undeploy(opStringEntry.name);
                return;
            } catch (OperationalStringException ignored) {
                //deploy admin does not know our service, try another one
            } catch (RemoteException e) {
            }
        }
        throw new ProvisioningException("No DeployAdmin is able to undeploy service " + serviceId);
    }

    private Tuple2<ServiceElement, ClassBundle> findBundleWithClass(OperationalString operationalString, String typeName, String version) {
        for (ServiceElement serviceElement : operationalString.getServices()) {
            for (ClassBundle classBundle : serviceElement.getExportBundles()) {
                if (typeName.equals(classBundle.getClassName()) && (version == null || version.equals(new Artifact(classBundle.getArtifact()).getVersion())))
                    return new Tuple2<ServiceElement, ClassBundle>(serviceElement, classBundle);
            }
        }
        for (OperationalString nestedOpString : operationalString.getNestedOperationalStrings()) {
            Tuple2<ServiceElement, ClassBundle> result = findBundleWithClass(nestedOpString, typeName, version);
            if (result != null)
                return result;
        }
        return null;
    }

    private DeployAdmin getDeployAdmin() throws ProvisioningException {
        if (provisionMonitor!=null) {
            try {
                provisionMonitor.ping();
            } catch (RemoteException re) {
                provisionMonitor = Accessor.get().getService(null, ProvisionMonitor.class);
            }
        } else {
            provisionMonitor = Accessor.get().getService(null, ProvisionMonitor.class);
        }
        if (provisionMonitor == null) throw new ProvisioningException("No Provision Monitor");
        try {
            return (DeployAdmin) provisionMonitor.getAdmin();
        } catch (RemoteException re) {
            throw new ProvisioningException("No Provision Monitor found");
        }
    }

    private ServiceDirectory getServiceDirectory() {
        return Accessor.get().getService(null, ServiceDirectory.class);
    }

    private static UndeployOption getUndeployOption(final int idleTimeout) {
        UndeployOption undeployOption = null;
        if (idleTimeout > 0) {
            undeployOption = new UndeployOption((long) idleTimeout,
                    UndeployOption.Type.WHEN_IDLE,
                    TimeUnit.SECONDS);
        }
        return undeployOption;
    }
}
