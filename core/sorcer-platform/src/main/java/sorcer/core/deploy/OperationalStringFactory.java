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

import org.rioproject.impl.opstring.OpString;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.opstring.UndeployOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.Exertion;
import sorcer.service.ServiceExertion;
import sorcer.service.Signature;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Create an {@link OperationalString} from an {@link Exertion}.
 *
 * @author Dennis Reedy
 */
public final class OperationalStringFactory {
    private static final Logger logger = LoggerFactory.getLogger(OperationalStringFactory.class.getName());
    private OperationalStringFactory() throws Exception {
    }

    public static List<String> getDeploymentIds(final Exertion exertion) {
        List<String> deploymentIds = new ArrayList<>();
        Iterable<Signature> netSignatures = getNetSignatures(exertion);
        return deploymentIds;
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

        for(Signature netSignature : netSignatures) {
            if(netSignature.getDeployment()==null)
                continue;
            if(netSignature.getDeployment().getType()==ServiceDeployment.Type.SELF) {
                selfies.add(netSignature);
            } else if(netSignature.getDeployment().getType()==ServiceDeployment.Type.FED) {
                federated.add(netSignature);
            }
        }

        List<OperationalString> operationalStrings = new ArrayList<>();

        for(Signature self : selfies) {
            ServiceElement service = ServiceElementFactory.create((ServiceSignature)self);
            OpString opString = new OpString(createDeploymentID(service), null);
            service.setOperationalStringName(opString.getName());
            opString.addService(service);
            opString.setUndeployOption(getUndeployOption((ServiceDeployment)self.getDeployment()));
            if(self.getDeployment().getUnique()== ServiceDeployment.Unique.YES) {
                uniqueOperationalStrings.add(opString);
            } else {
                operationalStrings.add(opString);
            }
        }

        List<ServiceElement> services = new ArrayList<>();
        int idle = 0;
        for(Signature signature : federated) {
            services.add(ServiceElementFactory.create((ServiceSignature)signature));
            if(signature.getDeployment().getIdle()>idle) {
                idle = signature.getDeployment().getIdle();
            }
        }
        if(services.isEmpty()) {
            logger.warn(String.format("No services configured for exertion %s", exertion.getName()));
            return null;
        }
        OpString opString = new OpString(exertion.getDeploymentId(), null);
        for(ServiceElement service : services) {
            service.setOperationalStringName(opString.getName());
            opString.addService(service);
        }
        opString.setUndeployOption(getUndeployOption(idle));
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

    static String createDeploymentID(ServiceElement service) throws NoSuchAlgorithmException {
        return DeploymentIdFactory.create(service);
    }

}
