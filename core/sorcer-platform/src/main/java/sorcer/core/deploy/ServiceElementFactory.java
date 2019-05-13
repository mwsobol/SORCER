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

import net.jini.config.ConfigurationException;
import org.rioproject.RioVersion;
import org.rioproject.config.Configuration;
import org.rioproject.deploy.SystemComponent;
import org.rioproject.deploy.SystemRequirements;
import org.rioproject.exec.ExecDescriptor;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.ResolverException;
import org.rioproject.sla.ServiceLevelAgreements;
import org.rioproject.system.capability.connectivity.TCPConnectivity;
import org.rioproject.system.capability.platform.OperatingSystem;
import org.rioproject.system.capability.platform.ProcessorArchitecture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.signature.ServiceSignature;
import sorcer.jini.lookup.entry.DeployInfo;
import sorcer.service.Deployment;
import sorcer.util.Sorcer;
import sorcer.util.SorcerEnv;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create a {@link ServiceElement} from a {@link ServiceSignature}.
 *
 * @author Dennis Reedy
 */
public final class ServiceElementFactory  {
    private static final Logger logger = LoggerFactory.getLogger(ServiceElementFactory.class.getName());
    /* The default provider codebase jars */
    private static final List<String> commonDLJars = Collections.singletonList("rio-api-" +RioVersion.VERSION +".jar");
    private static final Map<File, ServiceElement> serviceElementsCreated = new ConcurrentHashMap<>();

    private ServiceElementFactory(){}

    /**
     * Create a {@link ServiceElement}.
     *
     * @param signature The {@link ServiceSignature}, must not be {@code null}.
     *
     * @return A {@code ServiceElement}
     *
     * @throws ConfigurationException if there are problem reading the configuration
     */
    public static ServiceElement create(final ServiceSignature signature, File configFile) throws IOException,
        ConfigurationException,
        ResolverException,
        URISyntaxException {
        return create(signature.getDeployment(), configFile);
    }

    /**
     * Create a {@link ServiceElement}.
     *
     * @param deployment The {@link ServiceDeployment}, must not be {@code null}.
     *
     * @return A {@code ServiceElement}
     *
     * @throws ConfigurationException if there are problem reading the configuration
     */
    public static ServiceElement create(final ServiceDeployment deployment, File configFile) throws IOException,
        ConfigurationException,
        ResolverException,
        URISyntaxException {
        if(configFile==null) {
            return create(deployment, deployment.getConfig());
        }
        ServiceElement service = serviceElementsCreated.get(configFile);
        if (service == null) {
            service = create(deployment, configFile.getAbsolutePath());
            serviceElementsCreated.put(configFile, service);
        }
        return service;

    }

    private static ServiceElement create(ServiceDeployment deployment,
                                         String configurationFilePath) throws ConfigurationException, IOException {
        logger.debug("Loading {}", configurationFilePath);
        Configuration configuration = Configuration.getInstance(configurationFilePath);
        String component = "sorcer.core.provider.ServiceProvider";

        String name = deployment.getName();
        if(name==null) {
            name = configuration.getEntry(component, "name", String.class, null);
        }
        if(name!=null)
            name = SorcerEnv.getActualName(name);

        String[] interfaces = configuration.getEntry("sorcer.core.exertion.deployment",
            "interfaces",
            String[].class,
            new String[0]);
        if(interfaces.length==0 && deployment.getServiceType()!=null) {
            interfaces = new String[]{deployment.getServiceType()};
        }

        String[] codebaseJars = deployment.getCodebaseJars();
        if(codebaseJars==null) {
            codebaseJars = configuration.getEntry("sorcer.core.exertion.deployment",
                "codebaseJars",
                String[].class,
                new String[0]);
        }
        String[] implJars = deployment.getClasspathJars();
        if(implJars==null) {
            implJars = configuration.getEntry("sorcer.core.exertion.deployment",
                "implJars",
                String[].class,
                new String[0]);
        }
        String jvmArgs = deployment.getJvmArgs();
        if(jvmArgs==null) {
            jvmArgs = configuration.getEntry("sorcer.core.exertion.deployment",
                "jvmArgs",
                String.class,
                null);
        }
        Boolean fork = deployment.getFork();
        if(fork==null) {
            fork = configuration.getEntry("sorcer.core.exertion.deployment",
                "fork",
                Boolean.class,
                Boolean.FALSE);
        }
        String architecture = deployment.getArchitecture();
        if(architecture==null) {
            architecture = configuration.getEntry("sorcer.core.exertion.deployment",
                "arch",
                String.class,
                null);
        }
        String[] operatingSystems = deployment.getOperatingSystems();
        if(operatingSystems.length==0) {
            operatingSystems = configuration.getEntry("sorcer.core.exertion.deployment",
                "opSys",
                String[].class,
                new String[0]);
        }
        String[] ips = deployment.getIps();
        if(ips.length==0) {
            ips = configuration.getEntry("sorcer.core.exertion.deployment",
                "ips",
                String[].class,
                new String[0]);
        }
        String[] excludeIPs = deployment.getExcludeIps();
        if(excludeIPs.length==0) {
            excludeIPs = configuration.getEntry("sorcer.core.exertion.deployment",
                "ips_exclude",
                String[].class,
                new String[0]);
        }
        String providerClass = configuration.getEntry("sorcer.core.exertion.deployment",
            "providerClass",
            String.class,
            null);

        int plannedFromDeployment = deployment.getMultiplicity();
        int plannedFromConfig = configuration.getEntry("sorcer.core.exertion.deployment",
            "planned",
            int.class,
            -1);
        int planned = plannedFromConfig==-1?plannedFromDeployment:plannedFromConfig;


        int maxPerNode = deployment.getMaxPerCybernode();
        if(maxPerNode==0) {
            maxPerNode = configuration.getEntry("sorcer.core.exertion.deployment",
                "perNode",
                int.class,
                1);
        }
        String webster = configuration.getEntry("sorcer.core.exertion.deployment",
            "webster",
            String.class,
            null);

        Boolean fixedFromConfig = configuration.getEntry("sorcer.core.exertion.deployment",
            "fixed",
            Boolean.class,
            null);
        boolean fixed;
        if(fixedFromConfig!=null)
            fixed = fixedFromConfig;
        else
            fixed = deployment.getStrategy().equals(Deployment.Strategy.FIXED);

        ServiceDetails serviceDetails = new ServiceDetails(name,
            interfaces,
            codebaseJars,
            implJars,
            providerClass,
            jvmArgs,
            fork,
            planned,
            maxPerNode,
            architecture,
            operatingSystems,
            ips,
            excludeIPs,
            webster,
            fixed);
        ServiceElement service = create(serviceDetails, deployment);
        if(logger.isTraceEnabled())
            logger.trace("Created ServiceElement\n=================\n{}\n=================\nFrom [{}]",
                service, deployment);
        return service;
    }

    private static ServiceElement create(final ServiceDetails serviceDetails,
                                         final ServiceDeployment deployment) throws IOException {
        ServiceElement service = new ServiceElement();

        String websterUrl;
        if(serviceDetails.webster==null) {
            if(deployment.getWebsterUrl()==null) {
                websterUrl = Sorcer.getWebsterUrl();
                if(logger.isTraceEnabled())
                    logger.trace("Set code base derived from Sorcer.getWebsterUrl: {}", websterUrl);
            } else {
                websterUrl = deployment.getWebsterUrl();
                if(logger.isTraceEnabled())
                    logger.trace("Set code base derived from Deployment: {}", websterUrl);
            }
        } else {
            websterUrl = serviceDetails.webster;
        }
        /* Create client (export) ClassBundle */
        List<ClassBundle> exports = new ArrayList<>();
        for(String s : serviceDetails.interfaces) {
            ClassBundle export = new ClassBundle(s);
            if(serviceDetails.codebaseJars.length==1 && Artifact.isArtifact(serviceDetails.codebaseJars[0])) {
                export.setArtifact(serviceDetails.codebaseJars[0]);
            } else {
                export.setJARs(appendJars(commonDLJars, serviceDetails.codebaseJars));
                export.setCodebase(websterUrl);
            }
            exports.add(export);
        }

		/* Create service implementation ClassBundle */
        ClassBundle main = new ClassBundle(serviceDetails.providerClass==null?deployment.getImpl():serviceDetails.providerClass);
        if(serviceDetails.implJars.length==1 && Artifact.isArtifact(serviceDetails.implJars[0])) {
            main.setArtifact(serviceDetails.implJars[0]);
        } else {
            main.setJARs(serviceDetails.implJars);
            main.setCodebase(websterUrl);
        }

		/* Set ClassBundles to ServiceElement */
        service.setComponentBundle(main);
        service.setExportBundles(exports.toArray(new ClassBundle[exports.size()]));

        String serviceName = serviceDetails.name;

        if(!serviceDetails.fixed && serviceDetails.maxPerNode>0) {
            service.setMaxPerMachine(serviceDetails.maxPerNode);
        }
        if(serviceDetails.architecture!=null || serviceDetails.operatingSystems.length>0) {
            ServiceLevelAgreements slas = new ServiceLevelAgreements();
            SystemRequirements systemRequirements = new SystemRequirements();
            if(serviceDetails.architecture!=null) {
                Map<String, Object> attributeMap = new HashMap<>();
                attributeMap.put(ProcessorArchitecture.ARCHITECTURE, serviceDetails.architecture);
                SystemComponent systemComponent = new SystemComponent("Processor",
                    ProcessorArchitecture.class.getName(),
                    attributeMap);
                systemRequirements.addSystemComponent(systemComponent);
            }
            for(String s : serviceDetails.operatingSystems) {
                String opSys = checkAndMaybeFixOpSys(s);
                Map<String, Object> attributeMap = new HashMap<>();
                attributeMap.put(OperatingSystem.NAME, opSys);
                SystemComponent operatingSystem =
                    new SystemComponent("OperatingSystem", OperatingSystem.class.getName(), attributeMap);
                systemRequirements.addSystemComponent(operatingSystem);
            }
            slas.setServiceRequirements(systemRequirements);
            service.setServiceLevelAgreements(slas);
        }
        if(serviceDetails.ips.length>0) {
            SystemRequirements systemRequirements = service.getServiceLevelAgreements().getSystemRequirements();
            systemRequirements.addSystemComponent(getSystemComponentAddresses(false, serviceDetails.ips));
        }

        if(serviceDetails.excludeIps.length>0) {
            SystemRequirements systemRequirements = service.getServiceLevelAgreements().getSystemRequirements();
            systemRequirements.addSystemComponent(getSystemComponentAddresses(true, serviceDetails.excludeIps));
        }

		/* Create simple ServiceBeanConfig */
        Map<String, Object> configMap = new HashMap<>();
        configMap.put(ServiceBeanConfig.NAME, serviceName);
        configMap.put(ServiceBeanConfig.GROUPS, Sorcer.getLookupGroups());
        ServiceBeanConfig sbc = new ServiceBeanConfig(configMap, new String[]{deployment.getConfig()});
        sbc.addAdditionalEntries(new DeployInfo(deployment.getType().name(), deployment.getUnique().name(), deployment.getIdle()));
        service.setServiceBeanConfig(sbc);
        service.setPlanned(serviceDetails.planned);
        if(serviceDetails.fixed)
            service.setProvisionType(ServiceElement.ProvisionType.FIXED);

        /* If the service is to be forked, create an ExecDescriptor */
        if(serviceDetails.fork) {
            service.setFork(true);
            if(serviceDetails.jvmArgs!=null) {
                ExecDescriptor execDescriptor = new ExecDescriptor();
                execDescriptor.setInputArgs(serviceDetails.jvmArgs);
                service.setExecDescriptor(execDescriptor);
            }
        }
        if(logger.isDebugEnabled())
            logger.debug("Generated Service Element: {}", service.getName());
        else if(logger.isTraceEnabled()){
            logger.trace("Generated Service Element: {}", service);
        }
        return service;
    }

    private static SystemComponent[] getSystemComponentAddresses(boolean exclude, String[] addresses) {
        List<SystemComponent> machineAddresses = new ArrayList<>();
        for(String ip : addresses) {
            SystemComponent machineAddress = new SystemComponent(TCPConnectivity.ID);
            machineAddress.setExclude(exclude);
            if(isIpAddress(ip)) {
                machineAddress.put(TCPConnectivity.HOST_ADDRESS, ip);
            } else {
                machineAddress.put(TCPConnectivity.HOST_NAME, ip);
            }
            machineAddresses.add(machineAddress);
        }
        return machineAddresses.toArray(new SystemComponent[machineAddresses.size()]);
    }

    private static String checkAndMaybeFixOpSys(final String opSys) {
        String fixed;
        if(opSys.equalsIgnoreCase("Mac") || opSys.equalsIgnoreCase("OSX")) {
            fixed = "Mac OS X";
        } else if(opSys.equalsIgnoreCase("Win")) {
            fixed = "Windows";
        } else {
            fixed = opSys;
        }
        return fixed;
    }

    private static String[] appendJars(final List<String> base, final String... jars) {
        List<String> jarList = new ArrayList<>();
        jarList.addAll(base);
        for(String jar : jars) {
            if(jarList.contains(jar)) {
                continue;
            }
            jarList.add(jar);
        }
        return jarList.toArray(new String[jarList.size()]);
    }

    static boolean isIpAddress(String ip) {
        String [] parts = ip.split ("\\.");
        for (String s : parts){
            try {
                int i = Integer.parseInt(s);
                if (i < 0 || i > 255){
                    return false;
                }
            } catch(NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private static String getConfigurationAsString(final String configArg) throws IOException {
        if(configArg.equalsIgnoreCase("-")) {
            return configArg;
        }
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        String ls = System.getProperty("line.separator");
        if(configArg.startsWith("http")) {
            URL oracle = new URL(configArg);
            BufferedReader in = new BufferedReader(new InputStreamReader(oracle.openStream()));
            while ((line = in.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
            in.close();
        } else {
            File configFile = new File(configArg);
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append(ls);
                }
            }
        }
        return stringBuilder.toString();
    }

    /*private static class ResolveRequest {
        String artifactConfig;
        ResolverFutureTask resolverFutureTask;
        ServiceDeployment deployment;

        ResolveRequest(String artifactConfig, ServiceDeployment deployment, ResolverFutureTask resolverFutureTask) {
            this.artifactConfig = artifactConfig;
            this.resolverFutureTask = resolverFutureTask;
            this.deployment = deployment;
        }
    }

    private static class ResolverFutureTask implements Callable<ServiceElement> {
        ServiceElement serviceElement;
        private CountDownLatch counter = new CountDownLatch(1);

        void setServiceElement(ServiceElement serviceElement) {
            this.serviceElement = serviceElement;
            counter.countDown();
        }

        public ServiceElement pro() throws InterruptedException, IOException, ClassNotFoundException {
            counter.await();
            return new MarshalledObject<>(serviceElement).get();
        }
    }

    private static class ResolverRunner implements Runnable {

        @Override public void run() {
            try {
                Resolver resolver = ResolverHelper.getResolver();
                while (true) {
                    try {
                        ResolveRequest resolveRequest = resolverQueue.take();
                        ServiceElement serviceElement = serviceElementsCreated.get(resolveRequest.artifactConfig);
                        if(serviceElement==null) {
                            logger.debug("Resolve {}", resolveRequest.artifactConfig);
                            Artifact artifact = new Artifact(resolveRequest.artifactConfig);
                            URL configLocation = resolver.getLocation(artifact.getGAV(), artifact.getType());
                            String configurationFilePath = new File(configLocation.toURI()).getPath();
                            serviceElement = create(resolveRequest.deployment, configurationFilePath);
                            serviceElementsCreated.put(resolveRequest.artifactConfig, serviceElement);
                        }
                        resolveRequest.resolverFutureTask.setServiceElement(serviceElement);
                    } catch (InterruptedException | ResolverException | URISyntaxException | ConfigurationException | IOException e) {
                        logger.error("Unable to resolve artifact", e);
                    }
                }
            } catch(ResolverException e) {
                logger.error("Unable to get Resolver", e);
            }

        }
    }*/

    static void clear() {
        serviceElementsCreated.clear();
    }

    private static class ServiceDetails {
        final String name;
        final String[] interfaces;
        final String[] codebaseJars;
        final String[] implJars;
        final String providerClass;
        final String jvmArgs;
        final boolean fork;
        final int planned;
        final int maxPerNode;
        final String architecture;
        final String[] operatingSystems;
        final String[] ips;
        final String[] excludeIps;
        final String webster;
        final boolean fixed;

        private ServiceDetails(String name,
                               String[] interfaces,
                               String[] codebaseJars,
                               String[] implJars,
                               String providerClass,
                               String jvmArgs,
                               boolean fork,
                               int planned,
                               int maxPerNode,
                               String architecture,
                               String[] operatingSystems,
                               String[] ips,
                               String[] excludeIps,
                               String webster,
                               boolean fixed) {
            this.name = name;
            this.interfaces = interfaces;
            this.codebaseJars = codebaseJars;
            this.implJars = implJars;
            this.providerClass = providerClass;
            this.jvmArgs = jvmArgs;
            this.fork = fork;
            this.planned = planned;
            this.maxPerNode = maxPerNode;
            this.architecture = architecture;
            this.operatingSystems = operatingSystems;
            this.ips = ips;
            this.excludeIps = excludeIps;
            this.webster = webster;
            this.fixed = fixed;
        }
    }
}
