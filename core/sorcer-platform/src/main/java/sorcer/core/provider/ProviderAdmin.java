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
package sorcer.core.provider;

import org.rioproject.impl.jmx.JMXConnectionUtil;
import org.rioproject.impl.jmx.MBeanServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.rmi.RemoteException;

/**
 * Implements ProviderMBean as a standard MBean
 *
 * @author Dennis Reedy
 */
public class ProviderAdmin implements ProviderAdminMBean {
    final ServiceExerter provider;
    final static Logger logger = LoggerFactory.getLogger(ProviderAdmin.class);

    public ProviderAdmin(ServiceExerter provider) {
        if(provider==null)
            throw new IllegalArgumentException("provider must not be null");
        this.provider = provider;
    }

    /**
     * Registers the provider to the JVM's MBeanServer
     */
    public void register() {
        try {
            JMXConnectionUtil.createJMXConnection();
            ObjectName objectName = getObjectName();
            MBeanServer mBeanServer = MBeanServerFactory.getMBeanServer();
            if(mBeanServer.isRegistered(objectName)) {
                mBeanServer.unregisterMBean(objectName);
                logger.info("Unregistered {}, update provider [{}] registration with new instance",
                            objectName, getProviderName());
            }
            mBeanServer.registerMBean(this, objectName);

        } catch (Exception e) {
            logger.warn("Could not register MBean for {}", getProviderName(), e);
        }
    }

    /**
     * Un-registers the provider from the JVM's MBeanServer
     */
    public void unregister() {
        try {
            ObjectName objectName = getObjectName();
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            if(mBeanServer.isRegistered(objectName)) {
                mBeanServer.unregisterMBean(objectName);
            }
        } catch (Exception e) {
            logger.warn("Could not unregister MBean from JMX for {}", getProviderName(), e);
        }
    }

    private ObjectName getObjectName() throws MalformedObjectNameException {
        String domain = getClass().getPackage().getName();
        String projectId;
        if(System.getProperty("project.id")!=null)
            projectId = String.format(",projectId=%s", System.getProperty("project.id"));
        else
            projectId="";
        //String objectName = String.format("%s:multitype=Provider,key=%s%s,uuid=%s",
        String objectName = String.format("%s:key=%s%s,uuid=%s",
                                          domain,
                                          getProviderName(),
                                          projectId,
                                          provider.getProviderID().toString());
        return ObjectName.getInstance(objectName);
    }

    @Override public String getProviderName() {
        return provider.getProviderName();
    }

    @Override public void destroy() {
        logger.warn("Destroying {}", provider.getProviderName());
        provider.destroy();
    }
}
