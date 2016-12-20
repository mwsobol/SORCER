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
        MonitorRegistrationResource registrationResource = new MonitorRegistrationResource(UuidFactory.generate());
        ServiceResource sr = new ServiceResource(registrationResource);
        try {
            Lease lease = landlord.newLease(sr, duration);
			if(logger.isDebugEnabled())
                logger.debug("Created new MonitorRegistration identifier: {}, owner: {}, proxy: {}",
                             identifier, owner, monitorProxy);
            return new MonitorRegistration(lease, registrationResource.uuid, identifier, owner, monitorProxy);
        } catch (LeaseDeniedException e) {
            throw new MonitorException("Lease request denied", e);
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

    /**
     * Container class for event registration objects that are created and
     * behave as the resource that is being leased and controlled by the
     * ServiceResource
     */
    private static class MonitorRegistrationResource {
        private Uuid uuid;

        MonitorRegistrationResource(Uuid uuid) {
            this.uuid = uuid;
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
