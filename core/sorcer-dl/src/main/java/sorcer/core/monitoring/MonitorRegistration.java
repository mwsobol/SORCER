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

import net.jini.core.lease.Lease;
import net.jini.id.Uuid;

import java.io.Serializable;

/**
 * @author Dennis Reedy
 */
public class MonitorRegistration implements Serializable {
    static final long serialVersionUID = 1L;
    private final Uuid uuid;
    private final Lease lease;
    private final String identifier;
    private final String owner;
    private final Monitor monitor;

    public MonitorRegistration(Lease lease, Uuid uuid, String identifier, String owner, Monitor monitor) {
        this.lease = lease;
        this.uuid = uuid;
        this.identifier = identifier;
        this.owner = owner;
        this.monitor = monitor;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getOwner() {
        return owner;
    }

    public Lease getLease() {
        return lease;
    }

    public Uuid getUuid() {
        return uuid;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        MonitorRegistration that = (MonitorRegistration) o;
        return uuid.equals(that.uuid);
    }

    @Override public int hashCode() {
        return uuid.hashCode();
    }
}
