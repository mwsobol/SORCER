package sorcer.core.deploy;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import sorcer.util.Sorcer;
import sorcer.util.exec.ExecUtils;

import java.io.File;
import java.io.IOException;

/**
 * Class
 */
public class DeploySetup {
    static {
        System.setProperty("java.security.policy", Sorcer.getHome() + "/configs/policy.all");
        System.setSecurityManager(new SecurityManager());
    }
    static ProvisionMonitor monitor;


    private static void setUp() throws IOException, InterruptedException {
        String antHome = System.getenv("ANT_HOME");
        String antExecutable;
        if (antHome != null) {
            antExecutable = String.format("%s/bin/ant", antHome);
        } else {
            antExecutable = "ant";
        }
        File boot = new File(Sorcer.getHome() + "/bin/rio/iGrid-Rio-boot.xml");
        Assert.assertTrue(boot.exists());
        ExecUtils.execCommand(antExecutable + " -f " + boot.getPath());
    }

    @BeforeClass
    public static void verifyIGridRunning() throws Exception {
        long t0 = System.currentTimeMillis();
        monitor = Util.waitForService(ProvisionMonitor.class, 5);
        if(monitor==null) {
            setUp();
            monitor = Util.waitForService(ProvisionMonitor.class);
        }
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
        Util.waitForDeployment(manager);
        System.out.println("Waited " + (System.currentTimeMillis() - t0) + " millis for [Sorcer OS] provisioning");
    }
}
