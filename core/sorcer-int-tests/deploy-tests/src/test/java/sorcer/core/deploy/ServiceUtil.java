/*
 * Copyright to the original author or authors.
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
package sorcer.core.deploy;

import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import org.rioproject.impl.client.JiniClient;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import sorcer.util.Sorcer;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility for waiting on service deployments and/or services
 *
 * @author Dennis Reedy
 */
public class ServiceUtil {
    public static long MAX_TIMEOUT = 180000;

    public static void waitForDeployment(OperationalStringManager mgr) throws RemoteException,
                                                                              OperationalStringException,
                                                                              InterruptedException,
                                                                              TimeoutException {
        OperationalString opstring  = mgr.getOperationalString();
        Map<ServiceElement, Integer> deploy = new HashMap<ServiceElement, Integer>();
        int total = 0;
        for (ServiceElement elem: opstring.getServices()) {
            deploy.put(elem, 0);
            total += elem.getPlanned();
        }
        int deployed = 0;
        long sleptFor = 0;
        List<String> deployedServices = new ArrayList<String>();
        while (deployed < total && sleptFor< MAX_TIMEOUT) {
            deployed = 0;
            for (Map.Entry<ServiceElement, Integer> entry: deploy.entrySet()) {
                int numDeployed = entry.getValue();
                ServiceElement elem = entry.getKey();
                if (numDeployed < elem.getPlanned()) {
                    numDeployed = mgr.getServiceBeanInstances(elem).length;
                    deploy.put(elem, numDeployed);
                } else {
                    String name = String.format("%s/%s", elem.getOperationalStringName(), elem.getName());
                    if(!deployedServices.contains(name)) {
                        System.out.println(String.format("Service %s/%-24s is deployed. Planned [%s], deployed [%d]",
                                                         elem.getOperationalStringName(),
                                                         elem.getName(),
                                                         elem.getPlanned(),
                                                         numDeployed));
                        deployedServices.add(name);
                    }
                    deployed += elem.getPlanned();
                }
            }
            if(sleptFor==MAX_TIMEOUT)
                break;
            if (deployed < total) {
                Thread.sleep(1000);
                sleptFor += 1000;
            }
        }

        if(sleptFor>=MAX_TIMEOUT && deployed < total)
            throw new TimeoutException("Timeout waiting for service to be deployed");
    }

    static <T> T waitForService(Class<T> serviceType) throws Exception{
        return waitForService(serviceType, 60);
    }

    @SuppressWarnings("unchecked")
    static <T> T waitForService(Class<T> serviceType, long timeout) throws Exception{
        int waited = 0;
        JiniClient client = new JiniClient();
        client.addRegistrarGroups(Sorcer.getLookupGroups());
        Listener listener = new Listener(serviceType);
        client.getDiscoveryManager().addDiscoveryListener(listener);

        while(listener.monitor.get()==null && waited < timeout) {
            listener.lookup();
            Thread.sleep(500);
            waited++;
        }
        return (T) listener.monitor.get();
    }

    static class Listener implements DiscoveryListener {
        final AtomicReference<Object> monitor = new AtomicReference<Object>();
        private Class<?> serviceType;
        private final List<ServiceRegistrar> lookups = new ArrayList<ServiceRegistrar>();

        public Listener(Class<?> providerInterface) {
            serviceType = providerInterface;
        }

        void lookup() {
            final ServiceTemplate template = new ServiceTemplate(null, new Class[]{serviceType}, null);
            for(ServiceRegistrar registrar : lookups) {
                try {
                    Object service = registrar.lookup(template);
                    if(service!=null) {
                        monitor.set(service);
                        break;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        public void discovered(DiscoveryEvent discoveryEvent) {
            Collections.addAll(lookups, discoveryEvent.getRegistrars());
        }

        public void discarded(DiscoveryEvent discoveryEvent) {
        }
    }
}

