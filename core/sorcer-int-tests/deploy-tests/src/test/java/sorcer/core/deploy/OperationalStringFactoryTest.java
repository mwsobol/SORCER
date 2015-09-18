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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.config.Configuration;
import org.rioproject.deploy.SystemComponent;
import org.rioproject.opstring.*;
import org.rioproject.system.capability.connectivity.TCPConnectivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.service.*;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.co.operator.inEnt;
import static sorcer.eo.operator.*;

/**
 * @author Dennis Reedy
 */
@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/deploy-tests")
public class OperationalStringFactoryTest {
	private final static Logger logger = LoggerFactory.getLogger(OperationalStringFactoryTest.class.getName());

    @Test
    public void testCreateDeploymentID() throws NoSuchAlgorithmException, InterruptedException {
        ServiceElement serviceElement = new ServiceElement();
        ServiceElement serviceElement1 = new ServiceElement();
        serviceElement.setExportBundles(new ClassBundle("on.a.planet.far.far.Away"),
                                        new ClassBundle("use.the.force.Luke"),
                                        new ClassBundle("the.force.is.strong.with.this.One"));

        serviceElement1.setExportBundles(new ClassBundle("the.force.is.strong.with.this.One"),
                                         new ClassBundle("use.the.force.Luke"),
                                         new ClassBundle("on.a.planet.far.far.Away"));

        ServiceBeanConfig serviceConfig = new ServiceBeanConfig(new HashMap<String, Object>(), new String[0]);
        serviceConfig.setName("Luke");
        serviceElement.setServiceBeanConfig(serviceConfig);
        serviceElement1.setServiceBeanConfig(serviceConfig);

        String id = OperationalStringFactory.createDeploymentID(serviceElement);
        String id1 = OperationalStringFactory.createDeploymentID(serviceElement1);
        Assert.assertTrue(id.equals(id1));
    }


    @Test
    public void testOperationalStringCreation() throws Exception {
        Job job = JobUtil.createJob();
        
    	List<Signature> sigs = job.getAllNetTaskSignatures();
		logger.info("job signatures: " + sigs.size());

        Map<ServiceDeployment.Unique, List<OperationalString>> deployments = OperationalStringFactory.create(job);
        List<OperationalString> allOperationalStrings = new ArrayList<OperationalString>();
        allOperationalStrings.addAll(deployments.get(ServiceDeployment.Unique.YES));
        allOperationalStrings.addAll(deployments.get(ServiceDeployment.Unique.NO));
        assertTrue("Expected 2, got " + allOperationalStrings.size(), allOperationalStrings.size() == 2);

        assertTrue(deployments.get(ServiceDeployment.Unique.NO).size()==2);

        OperationalString multiply = allOperationalStrings.get(0);
        assertEquals(1, multiply.getServices().length);
        assertEquals("Multiplier", multiply.getServices()[0].getName());
        UndeployOption undeployOption = multiply.getUndeployOption();
        assertNotNull(undeployOption);
        assertTrue(UndeployOption.Type.WHEN_IDLE.equals(multiply.getUndeployOption().getType()));
        assertTrue(1l==undeployOption.getWhen());

        OperationalString federated = allOperationalStrings.get(1);
        String name = job.getDeploymentId();
        assertTrue(name.equals(federated.getName()));
        assertEquals(2, federated.getServices().length);
        assertEquals("Adder", federated.getServices()[0].getName());

        ServiceElement subtract = federated.getServices()[1];
        Assert.assertTrue(subtract.getPlanned()==2);
        Assert.assertTrue(subtract.getMaxPerMachine()==2);
        Assert.assertTrue(subtract.getMachineBoundary() == ServiceElement.MachineBoundary.VIRTUAL);

        assertNotNull(federated.getUndeployOption());
        assertTrue(UndeployOption.Type.WHEN_IDLE.equals(federated.getUndeployOption().getType()));
        assertTrue(1==federated.getUndeployOption().getWhen());

        assertEquals(2, federated.getServices()[1].getPlanned());
    }

    @Test
    public void testOperationalStringCreationWithIPOpSysAndArch() throws Exception {
        Job job = JobUtil.createJobWithIPAndOpSys();
        Map<ServiceDeployment.Unique, List<OperationalString>> deployments = OperationalStringFactory.create(job);
        List<OperationalString> allOperationalStrings = new ArrayList<OperationalString>();
        allOperationalStrings.addAll(deployments.get(ServiceDeployment.Unique.YES));
        allOperationalStrings.addAll(deployments.get(ServiceDeployment.Unique.NO));
        assertTrue("Expected 2, got " + allOperationalStrings.size(), allOperationalStrings.size() == 2);

        assertTrue(deployments.get(ServiceDeployment.Unique.NO).size()==2);

        OperationalString multiply = allOperationalStrings.get(0);
        assertEquals(1, multiply.getServices().length);
        assertEquals("Multiplier", multiply.getServices()[0].getName());
        UndeployOption undeployOption = multiply.getUndeployOption();
        assertNotNull(undeployOption);
        assertTrue(UndeployOption.Type.WHEN_IDLE.equals(multiply.getUndeployOption().getType()));
        assertTrue(1l==undeployOption.getWhen());

        ServiceElement multiplyService = multiply.getServices()[0];

        assertEquals(7, multiplyService.getServiceLevelAgreements().getSystemRequirements().getSystemComponents().length);
        SystemComponent[] components = multiplyService.getServiceLevelAgreements().getSystemRequirements().getSystemComponents();
        SystemComponent processor = getSystemComponent(components, "Processor", "x86_64");
        Assert.assertNotNull(processor);
        SystemComponent linux = getSystemComponent(components, "OperatingSystem", "Linux");
        Assert.assertNotNull(linux);
        SystemComponent osx = getSystemComponent(components, "OperatingSystem", "Mac OS X");
        Assert.assertNotNull(osx);

        SystemComponent[] machines = getSystemComponents(components, TCPConnectivity.ID);
        assertEquals(4, machines.length);
    }

    private SystemComponent getSystemComponent(SystemComponent[] components, String name, String attributeValue) {
        SystemComponent component = null;
        for(SystemComponent s : components) {
            if(name.equals(s.getName())) {
                for(Map.Entry<String, Object> entry : s.getAttributes().entrySet()) {
                    if(attributeValue.equals(entry.getValue())) {
                        component = s;
                        break;
                    }
                }
            }
        }
        return component;
    }

    private SystemComponent[] getSystemComponents(SystemComponent[] components, String name) {
        List<SystemComponent> sysComponents = new ArrayList<SystemComponent>();
        for(SystemComponent s : components) {
            if(name.equals(s.getName())) {
                sysComponents.add(s);
            }
        }
        return sysComponents.toArray(new SystemComponent[sysComponents.size()]);
    }

    @Test
    public void testServiceProperties() throws Exception {
        Task task = task("f5",
                sig("Foo",
                        Service.class,
                        deploy(configuration(JobUtil.getConfigDir() + "/TestConfig.groovy"))),
                context("foo", inEnt("arg/x3", 20.0d), inEnt("arg/x4", 80.0d),
                        result("result/y2")));

        /* totally bogus job definition */
        Job job = job("Some Job", job("f2", task), task, strategy(Strategy.Provision.YES),
                pipe(outPoint(task, "result/y1"), inPoint(task, "arg/x5")),
                pipe(outPoint(task, "result/y2"), inPoint(task, "arg/x6")));
        Map<ServiceDeployment.Unique, List<OperationalString>> deployments = OperationalStringFactory.create(job);
        OperationalString operationalString = deployments.get(ServiceDeployment.Unique.NO).get(0);
        assertEquals(1, operationalString.getServices().length);
        String name = job.getDeploymentId();
        assertEquals(name, operationalString.getName());
        ServiceElement service = operationalString.getServices()[0];
        assertTrue(service.forkService());
        assertNotNull(service.getExecDescriptor());
        assertEquals("-Xmx4G", service.getExecDescriptor().getInputArgs());
        assertTrue(service.getServiceBeanConfig().getConfigArgs().length==1);
        Configuration configuration = Configuration.getInstance(service.getServiceBeanConfig().getConfigArgs());
        String[] codebaseJars = configuration.getEntry("sorcer.core.exertion.deployment",
                                                       "codebaseJars",
                                                       String[].class);
        assertTrue(codebaseJars.length == 1);
        assertTrue(codebaseJars[0].equals("sorcer-tester-"+System.getProperty("sorcer.version")+"-dl.jar"));
    }

}
