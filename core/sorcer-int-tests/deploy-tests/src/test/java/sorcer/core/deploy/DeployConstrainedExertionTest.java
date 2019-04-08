package sorcer.core.deploy;

import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.deploy.SystemRequirements;
import org.rioproject.net.HostUtil;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.ServiceElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import org.sorcer.test.TestsRequiringRio;
import sorcer.service.Job;

import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.core.deploy.DeploySetup.monitor;
import static sorcer.core.deploy.DeploySetup.undeploy;
import static sorcer.core.deploy.DeploySetup.verifySorcerRunning;

/**
 * This test verifies that an exertion can be deployed to a specific machine, and also that an exertion will not be
 * deployed.
 *
 * Note:
 *
 * When creating operating systems to match on, the following transformations occur:
 *
 * If the operating system is provided as "Mac" or "OSX" (case ignored), the outDispatcher is transformed to "Mac OS X"
 * If "Win or "win" is provided, it is translated to "Windows".
 *
 * When entering IP addresses, execEnt either the IP Address or the fully qualified host key
 *
 * If providing machine architecture, the provided eval must be the same as what
 * System.getProperty("os.arch") returns for the required machine architecture
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/deploy-tests")
@Category(TestsRequiringRio.class)
public class DeployConstrainedExertionTest {
    private final static Logger logger = LoggerFactory.getLogger(DeployConstrainedExertionTest.class.getName());

    @BeforeClass
    public static void before() throws Exception {
        verifySorcerRunning();
    }

    @Before
    public void init() {
        ServiceElementFactory.clear();
    }

    @Test
    public void testDeployToCurrentMachine() throws Exception {
        String opSys = System.getProperty("os.name");
        String architecture = System.getProperty("os.arch");
        String hostAddress = getHostAddress();

        Job job = JobUtil.createJobWithIPAndOpSys(new String[]{opSys}, architecture, new String[]{hostAddress}, false);

        Map<ServiceDeployment.Unique, List<OperationalString>> deployments = OperationalStringFactory.create(job);
        List<OperationalString> allOperationalStrings = new ArrayList<>();
        allOperationalStrings.addAll(deployments.get(ServiceDeployment.Unique.YES));
        allOperationalStrings.addAll(deployments.get(ServiceDeployment.Unique.NO));
        assertTrue("Expected 2, got " + allOperationalStrings.size(), allOperationalStrings.size() == 2);

        assertTrue(deployments.get(ServiceDeployment.Unique.NO).size()==2);

        OperationalString multiply = allOperationalStrings.get(0);
        ServiceElement service = multiply.getServices()[0];
        SystemRequirements systemRequirements = service.getServiceLevelAgreements().getSystemRequirements();
        for(int i=0; i<systemRequirements.getSystemComponents().length; i++) {
            System.out.println("["+i+"] "+systemRequirements.getSystemComponents()[i]);
        }
        assertEquals(3, systemRequirements.getSystemComponents().length);
        DeployAdmin deployAdmin = (DeployAdmin) monitor().getAdmin();
        try {
            System.out.println("===> Check for existing deployment for: "+multiply.getName());
            undeploy(deployAdmin, multiply.getName());
            System.out.println("===> Setup listener for: "+multiply.getName());
            DeployListener deployListener = new DeployListener(1);
            System.out.println("===> Deploy: "+multiply.getName());
            deployAdmin.deploy(multiply, deployListener.export());
            deployListener.await();
            Assert.assertTrue(deployListener.success.get());
        } finally {
            undeploy(deployAdmin, multiply.getName());
        }
    }

    @Test
    public void testDeployFailToCurrentMachine() throws Exception {
        String opSys = "CICS";
        String architecture = System.getProperty("os.arch");
        String hostAddress = getHostAddress();

        Job job = JobUtil.createJobWithIPAndOpSys(new String[]{opSys}, architecture, new String[]{hostAddress}, false);

        Map<ServiceDeployment.Unique, List<OperationalString>> deployments = OperationalStringFactory.create(job);
        List<OperationalString> allOperationalStrings = new ArrayList<>();
        allOperationalStrings.addAll(deployments.get(ServiceDeployment.Unique.YES));
        allOperationalStrings.addAll(deployments.get(ServiceDeployment.Unique.NO));
        assertTrue("Expected 2, got " + allOperationalStrings.size(), allOperationalStrings.size() == 2);

        assertTrue(deployments.get(ServiceDeployment.Unique.NO).size()==2);

        OperationalString multiply = allOperationalStrings.get(0);
        DeployAdmin deployAdmin = (DeployAdmin) monitor.getAdmin();
        DeployListener deployListener = new DeployListener(1);
        deployAdmin.deploy(multiply, deployListener.export());
        deployListener.await();
        Assert.assertFalse(deployListener.success.get());
        undeploy(deployAdmin, multiply.getName());
    }

    @Test
    public void testDeployFailToCurrentMachineIPExcludes() throws Exception {
        String opSys = System.getProperty("os.name");
        String architecture = System.getProperty("os.arch");
        String hostAddress = getHostAddress();

        Job job = JobUtil.createJobWithIPAndOpSys(new String[]{opSys}, architecture, new String[]{hostAddress}, true);

        Map<ServiceDeployment.Unique, List<OperationalString>> deployments = OperationalStringFactory.create(job);
        List<OperationalString> allOperationalStrings = new ArrayList<OperationalString>();
        allOperationalStrings.addAll(deployments.get(ServiceDeployment.Unique.YES));
        allOperationalStrings.addAll(deployments.get(ServiceDeployment.Unique.NO));
        assertTrue("Expected 2, got " + allOperationalStrings.size(), allOperationalStrings.size() == 2);

        assertTrue(deployments.get(ServiceDeployment.Unique.NO).size()==2);

        OperationalString multiply = allOperationalStrings.get(0);
        DeployAdmin deployAdmin = (DeployAdmin) monitor.getAdmin();
        DeployListener deployListener = new DeployListener(1);
        deployAdmin.deploy(multiply, deployListener.export());
        deployListener.await();
        Assert.assertFalse(deployListener.success.get());
        undeploy(deployAdmin, multiply.getName());
    }

    private String getHostAddress() throws SocketException {
        return HostUtil.getFirstNonLoopbackAddress(true, false).getHostAddress();
    }

    class DeployListener implements ServiceProvisionListener {
        private final Exporter exporter;
        private ServiceProvisionListener remoteRef;
        private final CountDownLatch countDownLatch;
        private final AtomicBoolean success = new AtomicBoolean(true);

        DeployListener(final int numServices) {
            exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                             new BasicILFactory(),
                                             false,
                                             true);
            countDownLatch = new CountDownLatch(numServices);
        }

        ServiceProvisionListener export() throws ExportException {
            if(remoteRef==null) {
                remoteRef = (ServiceProvisionListener) exporter.export(this);
            }
            return remoteRef;
        }

        void unexport() {
            exporter.unexport(true);
            remoteRef = null;
        }

        void clear() {
            long count = countDownLatch.getCount();
            for(int i=0; i<count; i++) {
                countDownLatch.countDown();
            }
        }

        boolean await() throws InterruptedException {
            countDownLatch.await(3, TimeUnit.SECONDS);
            return success.get();
        }

        public void succeeded(ServiceBeanInstance serviceBeanInstance) throws RemoteException {
            logger.info(String.format("Service [%s/%s] provisioned on machine %s",
                                      serviceBeanInstance.getServiceBeanConfig().getOperationalStringName(),
                                      serviceBeanInstance.getServiceBeanConfig().getName(),
                                      serviceBeanInstance.getHostName()));
            countDownLatch.countDown();
            if(countDownLatch.getCount()==0) {
                unexport();
            }
        }

        public void failed(ServiceElement serviceElement, boolean resubmitted) throws RemoteException {
            logger.warn(String.format("Service [%s/%s] failed, undeploy",
                                         serviceElement.getServiceBeanConfig().getOperationalStringName(),
                                         serviceElement.getServiceBeanConfig().getName()));
            success.set(false);
            unexport();
            while(countDownLatch.getCount()>0) {
                countDownLatch.countDown();
            }
        }
    }

}
