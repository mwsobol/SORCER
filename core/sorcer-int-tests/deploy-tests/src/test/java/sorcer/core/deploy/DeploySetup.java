package sorcer.core.deploy;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;

/**
 * Class
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/deploy-tests")
public class DeploySetup {
    static ProvisionMonitor monitor;

    @BeforeClass
    public static void verifyIGridRunning() throws Exception {
        long t0 = System.currentTimeMillis();
        monitor = ServiceUtil.waitForService(ProvisionMonitor.class, 10);
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
