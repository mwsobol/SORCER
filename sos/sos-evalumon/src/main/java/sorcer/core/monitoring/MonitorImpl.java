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
package sorcer.core.monitoring;

import com.sun.jini.landlord.LeasedResource;
import net.jini.config.Configuration;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.rioproject.annotation.SetConfiguration;
import org.rioproject.annotation.SetProxy;
import org.rioproject.impl.service.LandlordLessor;
import org.rioproject.impl.service.ServiceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.analytics.MethodAnalytics;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dennis Reedy
 */
@SuppressWarnings("unused")
public class MonitorImpl implements Monitor {
    private LandlordLessor landlord;
    private MonitorEventHandler eventHandler;
    private Monitor monitorProxy;
    private Configuration config;
    private static final Logger logger = LoggerFactory.getLogger(MonitorImpl.class);

    @SetConfiguration
    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    @SetProxy
    public void setProxy(Object monitorProxy) {
        this.monitorProxy = (Monitor)monitorProxy;
    }

    @PostConstruct
    public void setup() throws IOException {
        landlord = new LandlordLessor(config);
        eventHandler = new MonitorEventHandler(config);
    }

    @Override public MonitorRegistration register(String identifier, String owner, long duration) throws MonitorException {
        MonitorRegistrationResource registrationResource = findMonitorRegistrationResource(identifier, owner);
        if(registrationResource==null) {
            registrationResource = new MonitorRegistrationResource(UuidFactory.generate(), owner, identifier);
            ServiceResource sr = new ServiceResource(registrationResource);
            try {
                Lease lease = landlord.newLease(sr, duration);
                registrationResource.lease = lease;
                if (logger.isDebugEnabled())
                    logger.debug("Created new MonitorRegistration identifier: {}, owner: {}, proxy: {}",
                                 identifier, owner, monitorProxy);
                return new MonitorRegistration(lease, registrationResource.uuid, identifier, owner, monitorProxy);
            } catch (LeaseDeniedException e) {
                throw new MonitorException("Lease request denied", e);
            }
        } else {
            return new MonitorRegistration(registrationResource.lease, registrationResource.uuid, identifier, owner, monitorProxy);
        }
    }

    @Override
    public void update(MonitorRegistration registration, Status status, MethodAnalytics analytics) throws MonitorException {
        if(!isValid(registration)) {
            throw new MonitorException("Invalid MonitorRegistration for "+registration.getIdentifier());
        }
        String name = analytics==null?registration.getIdentifier():String.format("%s#%s",
                                                                                 registration.getIdentifier(),
                                                                                 analytics.getMethodName());
        eventHandler.fire(new MonitorEvent(monitorProxy,
                                           name,
                                           registration.getOwner(),
                                           status,
                                           analytics));
    }

    private boolean isValid(MonitorRegistration registration) {
        boolean valid = false;
        for(LeasedResource resource : landlord.getLeasedResources()) {
            ServiceResource sR = (ServiceResource) resource;
            MonitorRegistrationResource registrationResource = (MonitorRegistrationResource) sR.getResource();
            if (registrationResource.uuid.equals(registration.getUuid())) {
                valid = true;
                break;
            }
        }
        return valid;
    }

    private MonitorRegistrationResource findMonitorRegistrationResource(String identifier, String owner) {
        MonitorRegistrationResource monitorRegistrationResource = null;
        for(LeasedResource resource : landlord.getLeasedResources()) {
            ServiceResource sR = (ServiceResource) resource;
            MonitorRegistrationResource registrationResource = (MonitorRegistrationResource) sR.getResource();
            if(registrationResource.owner.equals(owner) && registrationResource.identifier.equals(identifier)) {
                monitorRegistrationResource = registrationResource;
                break;
            }
        }
        return monitorRegistrationResource;
    }

    @Override
    public EventRegistration register(MonitorEventFilter eventFilter, RemoteEventListener listener, long duration) throws MonitorException {
        if(listener==null)
            throw new IllegalArgumentException("A RemoteEventListener must be provided");
        try {
            return eventHandler.register(monitorProxy, listener, eventFilter, duration);
        } catch (LeaseDeniedException | IOException e) {
            logger.error("Could not create event registration", e);
            throw new MonitorException("Could not create event registration", e);
        }
    }

    @Override public Map<String, List<String>> getRegisteredIdentifiers() {
        Map<String, List<String>> registeredIdentifiers = new HashMap<>();
        for(LeasedResource resource : landlord.getLeasedResources()) {
            ServiceResource sR = (ServiceResource) resource;
            MonitorRegistrationResource registrationResource = (MonitorRegistrationResource) sR.getResource();
            List<String> identifiers = registeredIdentifiers.get(registrationResource.owner);
            if(identifiers==null) {
                identifiers = new ArrayList<>();
            }
            identifiers.add(registrationResource.identifier);
            registeredIdentifiers.put(registrationResource.owner, identifiers);
        }
        return registeredIdentifiers;
    }

    /**
     * Container class for event registration objects that are created and
     * behave as the resource that is being leased and controlled by the
     * ServiceResource
     */
    private static class MonitorRegistrationResource {
        Uuid uuid;
        String owner;
        String identifier;
        Lease lease;

        MonitorRegistrationResource(Uuid uuid, String owner, String identifier) {
            this.uuid = uuid;
            this.owner = owner;
            this.identifier = identifier;
        }
    }

    /*
     * Opened up for testing
     */
    Object getProxy() {
        return monitorProxy;
    }

    /*
     * Opened up for testing
     */
    Configuration getConfig() {
        return config;
    }
}
