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

import org.rioproject.event.RemoteServiceEvent;
import sorcer.core.analytics.MethodAnalytics;

/**
 * @author Dennis Reedy
 */
public class MonitorEvent extends RemoteServiceEvent {
    static final long serialVersionUID = 1L;
    private final String identifier;
    private final String owner;
    private final Monitor.Status status;
    private final MethodAnalytics analytics;

    public MonitorEvent(Object source, String identifier, String owner, Monitor.Status status, MethodAnalytics analytics) {
        super(source);
        this.identifier = identifier;
        this.status = status;
        this.owner = owner;
        this.analytics = analytics;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getOwner() {
        return owner;
    }

    public Monitor.Status getStatus() {
        return status;
    }

    public MethodAnalytics getAnalytics() {
        return analytics;
    }
}
