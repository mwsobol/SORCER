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
package junit.sorcer.core.deploy;

import junit.sorcer.core.provider.Adder;
import junit.sorcer.core.provider.Multiplier;
import junit.sorcer.core.provider.Subtractor;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;

import org.rioproject.impl.client.JiniClient;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;

import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.provider.Jobber;
import sorcer.service.*;
import sorcer.service.Strategy.Provision;
import sorcer.util.Sorcer;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static sorcer.eo.operator.*;

/**
 * @author Dennis Reedy & Mike Sobolewski
 */
public class Util {
    public static long MAX_TIMEOUT = 180000;

    static Job createJob() throws ContextException, SignatureException, ExertionException {
        return createJob(false);
    }

    static Job createJob(boolean fork) throws ContextException, SignatureException, ExertionException {
        Task f4 = task("f4",
                       sig("multiply",
                           Multiplier.class,
                           deploy(configuration(fork?"bin/sorcer/test/arithmetic/configs/multiplier-prv-fork.config":
                                                     "bin/sorcer/test/arithmetic/configs/multiplier-prv.config"),
                                  idle(1),
                                  ServiceDeployment.Type.SELF)),
                       context("multiply", input("arg/x1", 10.0d),
                               input("arg/x2", 50.0d), out("result/y1")));

        Task f5 = task("f5",
                       sig("add",
                           Adder.class,
                           deploy(configuration("bin/sorcer/test/arithmetic/configs/AdderProviderConfig.groovy"))),
                       context("add", input("arg/x3", 20.0d), input("arg/x4", 80.0d),
                               output("result/y2")));

        Task f3 = task("f3",
                       sig("subtract", Subtractor.class,
                           deploy(maintain(2, perNode(2)),
                                  idle(1),
                                  configuration("bin/sorcer/test/arithmetic/configs/subtractor-prv.config"))),
                       context("subtract", input("arg/x5"),
                               input("arg/x6"), output("result/y3")));

        return job("f1", sig("service", Jobber.class, deploy(idle(1))),
                   job("f2", f4, f5), f3,
                   strategy(Provision.YES),
                   pipe(out(f4, "result/y1"), input(f3, "arg/x5")),
                   pipe(out(f5, "result/y2"), input(f3, "arg/x6")));
    }

    static Job createJobWithIPAndOpSys() throws SignatureException, ContextException, ExertionException {
        String[] opSys = new String[]{"OSX", "Linux"};
        String[] ips = new String[]{"10.131.5.106", "10.131.4.201", "macdna.rb.rad-e.wpafb.af.mil", "10.0.1.9"};
        return createJobWithIPAndOpSys(opSys, "x86_64", ips, false);
    }

    static Job createJobWithIPAndOpSys(String[] opSys,
                                       String arch,
                                       String[] ips,
                                       boolean excludeIPs) throws SignatureException, ContextException, ExertionException {
        Task f4;
        if(excludeIPs) {
            f4 = task("f4",
                      sig("multiply",
                          Multiplier.class,
                          deploy(configuration("bin/sorcer/test/arithmetic/configs/multiplier-prv.config"),
                                 idle(1),
                                 opsys(opSys),
                                 arch(arch),
                                 ips_exclude(ips),
                                 ServiceDeployment.Type.SELF)),
                      context("multiply", input("arg/x1", 10.0d),
                              input("arg/x2", 50.0d), out("result/y1")));
        } else {
            f4 = task("f4",
                      sig("multiply",
                          Multiplier.class,
                          deploy(configuration("bin/sorcer/test/arithmetic/configs/multiplier-prv.config"),
                                 idle(1),
                                 opsys(opSys),
                                 arch(arch),
                                 ips(ips),
                                 ServiceDeployment.Type.SELF)),
                      context("multiply", input("arg/x1", 10.0d),
                              input("arg/x2", 50.0d), out("result/y1")));
        }

        Task f5 = task("f5",
                       sig("add",
                           Adder.class,
                           deploy(configuration("bin/sorcer/test/arithmetic/configs/AdderProviderConfig.groovy"))),
                       context("add", input("arg/x3", 20.0d), input("arg/x4", 80.0d),
                               output("result/y2")));

        Task f3 = task("f3",
                       sig("subtract", Subtractor.class,
                           deploy(maintain(2, perNode(2)),
                                  idle(1),
                                  configuration("bin/sorcer/test/arithmetic/configs/subtractor-prv.config"))),
                       context("subtract", input("arg/x5"),
                               input("arg/x6"), output("result/y3")));

        return job("f1", sig("service", Jobber.class, deploy(idle(1))),
                   job("f2", f4, f5), f3,
                   strategy(Provision.YES),
                   pipe(out(f4, "result/y1"), input(f3, "arg/x5")),
                   pipe(out(f5, "result/y2"), input(f3, "arg/x6")));
    }

    @SuppressWarnings("unchecked")
	static Task createTask() throws SignatureException, ContextException, ExertionException {
        return task("f5",
                    sig("add", Adder.class,
                        deploy(configuration("bin/sorcer/test/arithmetic/configs/AdderProviderConfig.groovy"))),
                    context("add", input("arg/x3", 20.0d), input("arg/x4", 80.0d),
                            output("result/y2")),
                    strategy(Provision.YES));
    }

	
    static Job createJobNoDeployment() throws ContextException, SignatureException, ExertionException {
        Task f4 = task("f4",
                       sig("multiply", Multiplier.class),
                       context("multiply", input("arg/x1", 10.0d),
                               input("arg/x2", 50.0d), out("result/y1", null)));

        Task f5 = task("f5",
                       sig("add", Adder.class),
                       context("add", input("arg/x3", 20.0d), input("arg/x4", 80.0d),
                               output("result/y2", null)));

        Task f3 = task("f3",
                       sig("subtract", Subtractor.class),
                       context("subtract", input("arg/x5", null),
                               input("arg/x6", null), output("result/y3", null)));

        return job("f1", job("f2", f4, f5), f3, 
                   pipe(out(f4, "result/y1"), input(f3, "arg/x5")),
                   pipe(out(f5, "result/y2"), input(f3, "arg/x6")));
    }
    
    static void waitForDeployment(OperationalStringManager mgr) throws RemoteException, OperationalStringException, InterruptedException, TimeoutException {
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
