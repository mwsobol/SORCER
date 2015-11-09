package sorcer.core.deploy;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.sorcer.test.TestsRequiringRio;

/**
 * Class
 */
@Category(TestsRequiringRio.class)
//@Categories.IncludeCategory(TestsRequiringRio.class)
public class DeploySetup {
    static ProvisionMonitor monitor;

    @BeforeClass
    public static void verifySorcerRunning() throws Exception {
        long t0 = System.currentTimeMillis();
        monitor = ServiceUtil.waitForService(ProvisionMonitor.class, 20);
        Assert.assertNotNull(monitor);
        System.out.println("Waited "+(System.currentTimeMillis()-t0)+" millis for ProvisionMonitor discovery");
        DeployAdmin deployAdmin = (DeployAdmin) monitor.getAdmin();
        OperationalStringManager manager = null;
        while(manager==null) {
            try {
                manager = deployAdmin.getOperationalStringManager("Sorcer OS");
            } catch(OperationalStringException e) {
                Thread.sleep(500);
            }
        }
        t0 = System.currentTimeMillis();
        ServiceUtil.waitForDeployment(manager);
        System.out.println("Waited " + (System.currentTimeMillis() - t0) + " millis for [Sorcer OS] provisioning");
    }
}
