package sorcer.util.deploy;

import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.impl.client.JiniClient;
import org.rioproject.impl.opstring.OpStringLoader;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.OperationalString;
import sorcer.util.Sorcer;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Helper to deploy Rio {@code OperationalString}s
 *
 * @author Dennis Reedy
 */
public final class DeployHelper {
	static {
		System.setSecurityManager(new SecurityManager());
	}
	
    private DeployHelper() {}

    public static void main(String[] args) throws Exception {
    	deploy(args[0]);
    }
    
    public static void deploy(final String toDeploy) throws Exception {
        if(toDeploy==null)
            throw new IllegalArgumentException("must provide a file to deploy");
        File deploy = new File(toDeploy);
        if(!deploy.exists())
            throw new FileNotFoundException("File to deploy "+toDeploy+" does not exist");

        ProvisionMonitor monitor = waitForProvisioner();
        DeployAdmin deployAdmin = (DeployAdmin) monitor.getAdmin();
        OpStringLoader opStringLoader = new OpStringLoader();
        OperationalString[] operationalString = opStringLoader.parseOperationalString(deploy);
        deployAdmin.deploy(operationalString[0]);
    }

    static ProvisionMonitor waitForProvisioner() throws Exception{
        int waited = 0;
        JiniClient client = new JiniClient();
        client.addRegistrarGroups(Sorcer.getLookupGroups());
        Listener listener = new Listener();
        client.getDiscoveryManager().addDiscoveryListener(listener);

        while(listener.monitor.get()==null && waited < 60) {
            Thread.sleep(500);
            waited++;
        }
        return listener.monitor.get();
    }

    static class Listener implements DiscoveryListener {
        final AtomicReference<ProvisionMonitor> monitor = new AtomicReference<ProvisionMonitor>();
        final ServiceTemplate template = new ServiceTemplate(null, new Class[]{ProvisionMonitor.class}, null);

        public void discovered(DiscoveryEvent discoveryEvent) {
            for(ServiceRegistrar registrar : discoveryEvent.getRegistrars()) {
                try {
                    ProvisionMonitor pm = (ProvisionMonitor) registrar.lookup(template);
                    if(pm!=null) {
                        monitor.set(pm);
                        break;
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
