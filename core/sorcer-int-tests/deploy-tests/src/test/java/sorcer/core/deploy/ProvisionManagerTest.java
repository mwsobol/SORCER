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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import org.sorcer.test.TestsRequiringRio;
import sorcer.core.dispatch.ProvisionManager;
import sorcer.service.Job;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static sorcer.core.deploy.DeploySetup.verifySorcerRunning;

/**
 * Test {@code ProvisionManager} interactions
 *
 * @author Dennis Reedy
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/deploy-tests")
@Category(TestsRequiringRio.class)
public class ProvisionManagerTest {

    @BeforeClass
    public static void before() throws Exception {
        verifySorcerRunning();
    }

    @Test
    public void testDeployServicesSync() throws Exception {
        banner("testDeploy");
        Job f1 = JobUtil.createJob();
        ProvisionManager provisionManager = new ProvisionManager(f1);
        assertTrue(provisionManager.deployServicesSync());
        List<String> deployed = provisionManager.getDeploymentNames();
        assertTrue(deployed.size()==2);
        provisionManager.undeploy();
        assertFalse(provisionManager.getDeployAdmin().hasDeployed(deployed.get(0)));
        assertFalse(provisionManager.getDeployAdmin().hasDeployed(deployed.get(1)));
        assertTrue(provisionManager.getDeploymentNames().size()==0);
    }

    @Test(timeout = 90000)
    public void testConcurrentDeploy2() throws Exception {
        banner("testConcurrentDeploy2");
        Job f1 = JobUtil.createJob();
        List<ProvisionManager> provisionManagers = new ArrayList<ProvisionManager>();
        for(int i=0; i<100; i++) {
            provisionManagers.add(new ProvisionManager(f1));
        }
        System.out.println("Created "+provisionManagers.size()+" ProvisionManagers");
        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
        for(ProvisionManager provisionManager : provisionManagers) {
            Callable<Boolean> exertionVerifier = new DeployVerifier(provisionManager);
            FutureTask<Boolean> task = new FutureTask<Boolean>(exertionVerifier);
            futures.add(task);
            new Thread(task).start();
        }
        System.out.println("Started provisionManager threads now getting them");
        for(Future<Boolean> future : futures) {
            assertTrue(future.get());
        }
        ProvisionManager provisionManager = provisionManagers.get(0);
        System.out.println("Got provisionManager: " + provisionManager);
        List<String> deployed = provisionManager.getDeploymentNames();
        System.out.println("Deployed: " + deployed);
        assertTrue(deployed.size()==2);
        provisionManager.undeploy();
        assertFalse(provisionManager.getDeployAdmin().hasDeployed(deployed.get(0)));
        assertFalse(provisionManager.getDeployAdmin().hasDeployed(deployed.get(1)));
        assertTrue(provisionManager.getDeploymentNames().size()==0);
    }

    @Test(timeout = 90000)
    public void testConcurrentDeploy() throws Exception {
        banner("testConcurrentDeploy");
        Job f1 = JobUtil.createJob();
        ProvisionManager provisionManager = new ProvisionManager(f1);

        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
        for(int i=0; i<50; i++) {
            Callable<Boolean> exertionVerifier = new DeployVerifier(provisionManager);
            FutureTask<Boolean> task = new FutureTask<Boolean>(exertionVerifier);
            futures.add(task);
            new Thread(task).start();
        }
        System.out.println("Created "+futures.size()+" threads to bang on one ProvisionManager");
        for(Future<Boolean> future : futures) {
            assertTrue(future.get());
        }
        List<String> deployed = provisionManager.getDeploymentNames();
        StringBuilder sb = new StringBuilder();
        int i=1;
        for(String d : deployed) {
            if(sb.length()>0)
                sb.append("\n");
            sb.append("[").append(i++).append("] ").append(d);
        }
        assertTrue("Deployed size: "+deployed.size()+", expected 2\n"+sb.toString(), deployed.size()==2);
        provisionManager.undeploy();
        assertFalse(provisionManager.getDeployAdmin().hasDeployed(deployed.get(0)));
        assertFalse(provisionManager.getDeployAdmin().hasDeployed(deployed.get(1)));
        assertTrue(provisionManager.getDeploymentNames().size()==0);
    }


    class DeployVerifier implements Callable<Boolean> {
        final ProvisionManager provisionManager;

        DeployVerifier(ProvisionManager provisionManager) {
            this.provisionManager = provisionManager;
        }

        public Boolean call() throws Exception {
            return provisionManager.deployServices();
        }
    }

    private void banner(String name) {
        StringBuilder b = new StringBuilder();
        b.append("***********************************************\n");
        b.append(name).append("\n");
        b.append("***********************************************");
        System.out.println(b.toString());
    }
}
