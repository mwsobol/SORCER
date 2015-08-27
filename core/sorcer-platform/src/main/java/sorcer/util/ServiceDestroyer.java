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
package sorcer.util;

import com.sun.jini.admin.DestroyAdmin;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.impl.client.JiniClient;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.OperationalStringManager;
import sorcer.util.exec.ExecUtils;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Destroys service using attach API
 *
 * @author Dennis Reedy
 */
public class ServiceDestroyer {

    public static void main(String[] args) throws Exception {
        System.setSecurityManager(new SecurityManager());
        boolean killCybernode = false;
        boolean killMonitor = false;
        List<String> killJVMs = new ArrayList<String>();
        for (String arg : args) {
            if (arg.equalsIgnoreCase("cybernode")) {
                killCybernode = true;
            }
            if (arg.equalsIgnoreCase("monitor")) {
                killMonitor = true;
            }
            if (arg.equalsIgnoreCase("webster")) {
                killJVMs.add("start-sorcer-web.groovy");
            }
            if (arg.startsWith("jvm:")) {
                killJVMs.add(arg.substring(4));
            }
        }
        if(killCybernode || killMonitor) {
            JiniClient client = new JiniClient();
            client.addRegistrarGroups(Sorcer.getLookupGroups());
            Listener listener = new Listener();
            client.getDiscoveryManager().addDiscoveryListener(listener);
            if (killMonitor) {
                int waited = 0;
                while (listener.getProvisionMonitors().length == 0 && waited < 6) {
                    Thread.sleep(500);
                    waited++;
                }
                for (ProvisionMonitor monitor : listener.getProvisionMonitors()) {
                    try {
                        Object admin = monitor.getAdmin();
                        List<String> toUndeploy = new ArrayList<String>();
                        for (OperationalStringManager manager : ((DeployAdmin) admin).getOperationalStringManagers()) {
                            toUndeploy.add(manager.getOperationalString().getName());
                        }
                        for (String undeploy : toUndeploy) {
                            ((DeployAdmin) admin).undeploy(undeploy);
                            System.out.println("Undeployed \"" + undeploy + "\"");
                        }
                        ((DestroyAdmin) admin).destroy();
                        System.out.println("ProvisionMonitor terminated");
                    } catch (RemoteException e) {
                        System.err.println("Could not terminate ProvisionMonitor");
                        e.printStackTrace();
                    }
                }
            }

            if (killCybernode) {
                int waited = 0;
                while (listener.getProvisionMonitors().length == 0 && waited < 6) {
                    Thread.sleep(500);
                    waited++;
                }
                for (Cybernode cybernode : listener.getCybernodes()) {
                    try {
                        Object admin = cybernode.getAdmin();
                        ((DestroyAdmin) admin).destroy();
                        System.out.println("Cybernode terminated");
                    } catch (RemoteException e) {
                        System.err.println("Could not terminate Cybernode");
                        e.printStackTrace();
                    }
                }
            }
        }

        if(!killJVMs.isEmpty()) {
            for(String killJVM : killJVMs) {
                if(killJVM(killJVM))
                    System.out.println("Killed \""+killJVM+"\"");
                else
                    System.out.println("Did not kill \""+killJVM+"\"");
            }
        }
    }

    static boolean killJVM(String id) {
        boolean killed = false;
        List<VirtualMachineDescriptor> vms = VirtualMachine.list();
        for (VirtualMachineDescriptor vm : vms) {
            if(System.getProperty("debug")!=null) {
                System.out.println("vm = " + vm);
                System.out.println("id = " + id);
                System.out.println("vm.displayName() = " + vm.displayName());
            }
            if (vm.displayName().contains(id) && !vm.displayName().contains(ServiceDestroyer.class.getName())) {
                try {
                    String command;
                    if(System.getProperty("os.name").startsWith("Windows")) {
                        command = "taskkill /f /pid ";
                    } else {
                        command = "kill -9 ";
                    }
                    ExecUtils.execCommand(command + vm.id());
                    killed = true;
                } catch (Exception e) {
                    System.err.println("Could not kill ["+id+"]");
                    System.out.println("Could not kill ["+id+"]");
                    System.out.println("Exception e = " + e 
                    		+ "\n" + GenericUtil.arrayToString(e.getStackTrace()));
                    e.printStackTrace();
                }
            }
        }
        return killed;
    }

    static class Listener implements DiscoveryListener {
        private final List<Cybernode> cybernodes = new ArrayList<Cybernode>();
        private final List<ProvisionMonitor> monitors = new ArrayList<ProvisionMonitor>();

        Cybernode[] getCybernodes() {
            Cybernode[] cNodes;
            synchronized (cybernodes) {
                cNodes = cybernodes.toArray(new Cybernode[cybernodes.size()]);
            }
            return cNodes;
        }

        ProvisionMonitor[] getProvisionMonitors() {
            ProvisionMonitor[] pMons;
            synchronized (monitors) {
                pMons = monitors.toArray(new ProvisionMonitor[monitors.size()]);
            }
            return pMons;
        }

        public void discovered(DiscoveryEvent discoveryEvent) {
            ServiceTemplate cybernode = new ServiceTemplate(null, new Class[]{Cybernode.class}, null);
            ServiceTemplate monitor = new ServiceTemplate(null, new Class[]{ProvisionMonitor.class}, null);
            for(ServiceRegistrar registrar : discoveryEvent.getRegistrars()) {
                try {
                    ServiceMatches matches = registrar.lookup(cybernode, Integer.MAX_VALUE);
                    for(ServiceItem item : matches.items) {
                        synchronized (cybernodes) {
                            cybernodes.add((Cybernode)item.service);
                        }
                    }
                    matches = registrar.lookup(monitor, Integer.MAX_VALUE);
                    for(ServiceItem item : matches.items) {
                        synchronized (monitors) {
                            monitors.add((ProvisionMonitor)item.service);
                        }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        public void discarded(DiscoveryEvent discoveryEvent) {
        }
    }
}
