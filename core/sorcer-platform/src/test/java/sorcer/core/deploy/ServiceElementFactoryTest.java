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

import junit.framework.Assert;
import net.jini.config.ConfigurationException;
import org.junit.Test;
import org.rioproject.deploy.SystemComponent;
import org.rioproject.deploy.SystemRequirements;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.ResolverException;
import org.rioproject.system.capability.connectivity.TCPConnectivity;
import org.rioproject.tools.webster.Webster;
import sorcer.core.provider.ServiceProvider;
import sorcer.core.signature.NetSignature;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;

import static sorcer.eo.operator.*;

/**
 * @author Dennis Reedy
 */
public class ServiceElementFactoryTest {

    @Test
    public void testUsingDeploymentForService() throws Exception {
        ServiceDeployment deployment = new ServiceDeployment();
        deployment.setImpl("foo.Impl");
        deployment.setServiceType("foo.Interface");
        deployment.setName("The Great and wonderful Oz");
        deployment.setMultiplicity(10);
        ServiceElement serviceElement = ServiceElementFactory.create(deployment);
        Assert.assertEquals(10, serviceElement.getPlanned());
        Assert.assertEquals("The Great and wonderful Oz", serviceElement.getName());
        Assert.assertEquals("foo.Impl", serviceElement.getComponentBundle().getClassName());
        Assert.assertTrue(serviceElement.getExportBundles().length==1);
        Assert.assertEquals("foo.Interface", serviceElement.getExportBundles()[0].getClassName());
    }

    @Test
    public void setDeploymentWebsterUrl() throws IOException, ConfigurationException, URISyntaxException, ResolverException {
        ServiceDeployment deployment = new ServiceDeployment();
        deployment.setConfig(getConfigDir()+"/TestIP.groovy");
        deployment.setWebsterUrl("http://spongebob:8080");
        ServiceElement serviceElement = ServiceElementFactory.create(deployment);
        Assert.assertTrue(serviceElement.getExportURLs().length > 1);
        Assert.assertEquals(serviceElement.getExportURLs()[0].getHost(), "spongebob");
        Assert.assertEquals(serviceElement.getExportURLs()[0].getPort(), 8080);
    }

    @Test
    public void setDeploymentWebsterOperator() throws IOException, ConfigurationException, URISyntaxException, ResolverException {
        NetSignature methodEN = new NetSignature("executeFoo",
                                                 ServiceProvider.class,
                                                 "Yogi");
        methodEN.setDeployment(deploy(configuration(getConfigDir() + "/TestIP.groovy"),
                                      webster("http://spongebob:8080")));

        ServiceElement serviceElement = ServiceElementFactory.create(methodEN.getDeployment());
        Assert.assertTrue(serviceElement.getExportURLs().length>1);
        Assert.assertEquals(serviceElement.getExportURLs()[0].getHost(), "spongebob");
        Assert.assertEquals(serviceElement.getExportURLs()[0].getPort(), 8080);
    }

    @Test
    public void setDeploymentWebsterFromConfig() throws IOException, ConfigurationException, URISyntaxException, ResolverException {
        ServiceDeployment deployment = new ServiceDeployment();
        deployment.setConfig(getConfigDir()+"/testWebster.config");
        ServiceElement serviceElement = ServiceElementFactory.create(deployment);
        Assert.assertTrue(serviceElement.getExportURLs().length>1);
        Assert.assertEquals(serviceElement.getExportURLs()[0].getHost(), "127.0.0.1");
        Assert.assertEquals(serviceElement.getExportURLs()[0].getPort(), 8080);
    }

    @Test
    public void testConfigurationAsFile() {
        ServiceDeployment deployment = new ServiceDeployment();
        deployment.setConfig(getConfigDir()+"/testIP.config");
        Assert.assertNotNull(deployment.getConfig());
        Assert.assertEquals(getConfigDir()+"/testIP.config",
                            deployment.getConfig());
    }

    @Test
    public void testConfigurationAsHTTP() throws IOException, ConfigurationException {
        String configDirToServe = getConfigDir();
        Webster webster = new Webster(0, configDirToServe, null);
        try {
            ServiceDeployment deployment = new ServiceDeployment();
            String hostName = InetAddress.getLocalHost().getHostName();
            int port = webster.getPort();
            deployment.setConfig("http://"+hostName+":"+port+"/testIP.config");
            Assert.assertNotNull(deployment.getConfig());
            Assert.assertEquals("http://"+hostName+":"+port+"/testIP.config",
                                deployment.getConfig());
        } finally {
            webster.terminate();
        }
    }

    @Test
    public void testIPAddresses() throws Exception {
        ServiceDeployment deployment = new ServiceDeployment();
        deployment.setIps("10.0.1.9", "canebay.local");
        deployment.setExcludeIps("10.0.1.7", "stingray.foo.local.net");
        ServiceElement serviceElement = ServiceElementFactory.create(deployment);
        SystemRequirements systemRequirements = serviceElement.getServiceLevelAgreements().getSystemRequirements();
        Assert.assertEquals(4, systemRequirements.getSystemComponents().length);

        verify(get("10.0.1.9", false, systemRequirements.getSystemComponents()), false);
        verify(get("canebay.local", true, systemRequirements.getSystemComponents()), false);
        verify(get("10.0.1.7", false, systemRequirements.getSystemComponents()), true);
        verify(get("stingray.foo.local.net", true, systemRequirements.getSystemComponents()), true);
    }

    @Test
    public void testIPAddressesUsingConfiguration() throws IOException, ConfigurationException, URISyntaxException, ResolverException {
        ServiceDeployment deployment = new ServiceDeployment();
        deployment.setConfig(getConfigDir()+"/testIP.config");
        verifyServiceElement(ServiceElementFactory.create(deployment));
    }

    @Test
    public void testIPAddressesUsingGroovyConfiguration() throws IOException, ConfigurationException, URISyntaxException, ResolverException {
        ServiceDeployment deployment = new ServiceDeployment();
        deployment.setConfig(getConfigDir()+"/TestIP.groovy");
        verifyServiceElement(ServiceElementFactory.create(deployment));
    }

    private String getConfigDir() {
        return String.format("%s/src/test/resources/deploy/configs/", System.getProperty("user.dir"));
    }

    private void verifyServiceElement(ServiceElement serviceElement) {
        SystemRequirements systemRequirements = serviceElement.getServiceLevelAgreements().getSystemRequirements();
        Assert.assertEquals(8, systemRequirements.getSystemComponents().length);

        Assert.assertTrue(ServiceElementFactory.isIpAddress("10.131.5.106"));
        Assert.assertTrue(ServiceElementFactory.isIpAddress("10.131.4.201"));
        Assert.assertTrue(ServiceElementFactory.isIpAddress("10.0.1.9"));
        Assert.assertFalse(ServiceElementFactory.isIpAddress("macdna.rb.rad-e.wpafb.af.mil"));

        verify(get("10.131.5.106", false, systemRequirements.getSystemComponents()), false);
        verify(get("10.131.4.201", false, systemRequirements.getSystemComponents()), false);
        verify(get("macdna.rb.rad-e.wpafb.af.mil", true, systemRequirements.getSystemComponents()), false);
        verify(get("10.0.1.9", false, systemRequirements.getSystemComponents()), false);
        verify(get("127.0.0.1", false, systemRequirements.getSystemComponents()), true);
    }

    private void verify(SystemComponent s, boolean excluded) {
        Assert.assertNotNull(s);
        if(excluded) {
            Assert.assertTrue(s.exclude());
        } else {
            Assert.assertFalse(s.exclude());
        }
    }

    private SystemComponent get(String address, boolean hostName, SystemComponent[] systemComponents) {
        SystemComponent systemComponent = null;
        String key = hostName?TCPConnectivity.HOST_NAME:TCPConnectivity.HOST_ADDRESS;
        for(SystemComponent component : systemComponents) {
            Object value = component.getAttributes().get(key);
            if(value!=null && value.equals(address)) {
                systemComponent = component;
                break;
            }
        }
        return systemComponent;
    }
}
