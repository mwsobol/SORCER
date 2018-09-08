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
import org.rioproject.config.Configuration;
import org.rioproject.impl.opstring.OpString;
import org.rioproject.impl.opstring.OpStringLoader;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.opstring.UndeployOption;
import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.ResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.signature.NetSignature;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.Exertion;
import sorcer.service.ServiceExertion;
import sorcer.service.Signature;
import sorcer.service.SignatureException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Create an {@link OperationalString} from an {@link Exertion}.
 *
 * @author Dennis Reedy
 */
public final class OperationalStringFactory {
    private static final Logger logger = LoggerFactory.getLogger(OperationalStringFactory.class.getName());
    private static FileResolverHandler resolverHandler = new FileResolverHandler();
    private OperationalStringFactory() {
    }

    public static Map<ServiceDeployment.Unique, List<OperationalString>> create(List<Signature> signatures) throws Exception {
        List<Signature> selfies = new ArrayList<>();
        List<Signature> federated = new ArrayList<>();
        List<OperationalString> uniqueOperationalStrings = new ArrayList<>();
        List<OperationalString> operationalStrings = collect(signatures, selfies, federated, uniqueOperationalStrings);
        OpString opString = collectFederated(federated);
        operationalStrings.add(opString);

        Map<ServiceDeployment.Unique, List<OperationalString>> opStringMap = new HashMap<>();
        opStringMap.put(ServiceDeployment.Unique.YES, uniqueOperationalStrings);
        opStringMap.put(ServiceDeployment.Unique.NO, operationalStrings);
        return opStringMap;
    }

    /**
     * Create {@link OperationalString}s from an {@code Exertion}.
     *
     * @param exertion The exertion, must not be {@code null}.
     *
     * @return An {@code Map} of {@code Deployment.Type} keys with{@code List<OperationalString> values composed of
     * services created from {@link ServiceSignature}s. If there are no services, return and empty {@code Map}.
     *
     * @throws IllegalArgumentException if the {@code exertion} is {@code null}.
     * @throws Exception if there are configuration issues, if the iGrid opstring cannot be loaded
     */
    public static Map<ServiceDeployment.Unique, List<OperationalString>> create(final Exertion exertion) throws Exception {
        if(exertion==null)
            throw new IllegalArgumentException("exertion is null");

        Iterable<Signature> netSignatures = getNetSignatures(exertion);
        List<Signature> selfies = new ArrayList<>();
        List<Signature> federated = new ArrayList<>();

        List<OperationalString> uniqueOperationalStrings = new ArrayList<>();
        List<OperationalString> operationalStrings = collect(netSignatures, selfies, federated, uniqueOperationalStrings);

        OpString opString = collectFederated(federated);

        ServiceDeployment eDeployment = (ServiceDeployment)exertion.getProcessSignature().getDeployment();
        ServiceDeployment.Unique unique = eDeployment==null? ServiceDeployment.Unique.NO:eDeployment.getUnique();
        if(unique == ServiceDeployment.Unique.YES) {
            uniqueOperationalStrings.add(opString);
        } else {
            operationalStrings.add(opString);
        }
        Map<ServiceDeployment.Unique, List<OperationalString>> opStringMap = new HashMap<>();
        opStringMap.put(ServiceDeployment.Unique.YES, uniqueOperationalStrings);
        opStringMap.put(ServiceDeployment.Unique.NO, operationalStrings);
        return opStringMap;
    }


    private static List<OperationalString> collect(Iterable<Signature> netSignatures,
                                                   List<Signature> selfies,
                                                   List<Signature> federated,
                                                   List<OperationalString> uniqueOperationalStrings) throws URISyntaxException,
        ResolverException,
        ConfigurationException,
        IOException {
        for(Signature netSignature : netSignatures) {
            if(netSignature.getDeployment()==null)
                continue;
            if(netSignature.getDeployment().getType()==ServiceDeployment.Type.SELF) {
                selfies.add(netSignature);
            } else if(netSignature.getDeployment().getType()==ServiceDeployment.Type.FED) {
                checkAddToFederatedList((NetSignature) netSignature, federated);
            }
        }

        List<OperationalString> operationalStrings = new ArrayList<>();

        for(Signature self : selfies) {
            String config = ((ServiceSignature)self).getDeployment().getConfig();

            File configFile = getConfigFile(config);
            OpString opString = checkIsOpstring(configFile);
            if(opString==null) {
                ServiceElement service = ServiceElementFactory.create((ServiceSignature) self,
                    configFile.exists()?configFile:null);
                opString = new OpString(createDeploymentID(service), null);
                service.setOperationalStringName(opString.getName());
                opString.addService(service);
            }
            opString.setUndeployOption(getUndeployOption((ServiceDeployment)self.getDeployment()));
            if(self.getDeployment().getUnique()== ServiceDeployment.Unique.YES) {
                uniqueOperationalStrings.add(opString);
            } else {
                operationalStrings.add(opString);
            }
        }
        return operationalStrings;
    }

    private static OpString checkIsOpstring(File configFile) throws ConfigurationException {
        OpString opString = null;
        if(configFile.exists()) {
            Configuration configuration = Configuration.getInstance(configFile.getPath());
            boolean isOpString = configuration.getEntry("org.rioproject.opstring",
                "isOpString",
                Boolean.class,
                false);

            if (isOpString) {
                OpStringLoader loader = new OpStringLoader();
                try {
                    OperationalString[] opStrings = loader.parseOperationalString(configFile);
                    opString = (OpString) opStrings[0];
                } catch (Exception e) {
                    throw new ConfigurationException("Failed creating opstring", e);
                }
            }
        }
        return opString;
    }

    private static File getConfigFile(String config) throws ResolverException {
        File configFile;
        if (Artifact.isArtifact(config)) {
            configFile = resolverHandler.getFile(config);
        } else {
            configFile = new File(config);
        }
        return configFile;
    }

    private static OpString collectFederated(List<Signature> federated) throws SignatureException,
        URISyntaxException,
        ResolverException,
        ConfigurationException,
        IOException {
        List<ServiceElement> services = new ArrayList<>();
        int idle = 0;
        for(Signature signature : federated) {
            String config = ((ServiceSignature)signature).getDeployment().getConfig();
            File configFile = getConfigFile(config);
            OpString opString = checkIsOpstring(configFile);
            if(opString==null) {
                services.add(ServiceElementFactory.create((ServiceSignature) signature, configFile));
            } else {
                Collections.addAll(services, opString.getServices());
            }
            if(signature.getDeployment().getIdle()>idle) {
                idle = signature.getDeployment().getIdle();
            }
        }
        if(services.isEmpty()) {
            logger.warn("No services configured for exertion");
            return null;
        }
        OpString opString = new OpString(DeploymentIdFactory.create(federated), null);
        for(ServiceElement service : services) {
            service.setOperationalStringName(opString.getName());
            opString.addService(service);
        }
        opString.setUndeployOption(getUndeployOption(idle));

        return opString;
    }

    private static UndeployOption getUndeployOption(final ServiceDeployment deployment) {
        UndeployOption undeployOption = null;
        if(deployment!=null) {
            undeployOption = getUndeployOption(deployment.getIdle());
        }
        return undeployOption;
    }

    private static UndeployOption getUndeployOption(final int idleTimeout) {
        UndeployOption undeployOption = null;
        if (idleTimeout > 0) {
            undeployOption = new UndeployOption((long) idleTimeout,
                UndeployOption.Type.WHEN_IDLE,
                TimeUnit.MINUTES);
        }
        return undeployOption;
    }

    private static Iterable<Signature> getNetSignatures(final Exertion exertion) {
        List<Signature> signatures = new ArrayList<>();
        if(exertion instanceof ServiceExertion) {
            ServiceExertion serviceExertion = (ServiceExertion)exertion;
            signatures.addAll(serviceExertion.getAllNetTaskSignatures());
        }
        return signatures;
    }


    private static void checkAddToFederatedList(NetSignature netSignature, List<Signature> federated) {
        if(netSignature.getDeployment() != null) {
            ServiceDeployment serviceDeployment = netSignature.getDeployment();
            if(serviceDeployment.getConfig()!=null) {
                if(logger.isDebugEnabled())
                    logger.debug("Adding ServiceDeployment: {}", serviceDeployment);
                federated.add(netSignature);
            } else {
                logger.warn("No configuration found for ServiceDeployment: {}", serviceDeployment);
            }
        } else {
            logger.warn("Unknown multitype of Deployment: {}", netSignature.getDeployment().getClass().getName());
        }
    }

    static String createDeploymentID(ServiceElement service) {
        return DeploymentIdFactory.create(service);
    }

}
