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

import java.io.Serializable;

/**
 * @author Dennis Reedy
 */
public class MonitorEventFilter implements Serializable {
    static final long serialVersionUID = 1L;
    private final String owner;

    public MonitorEventFilter() {
        owner = null;
    }

    public MonitorEventFilter(String owner) {
        this.owner = owner;
    }

    /**
     * This method defines the implementation of selection criteria to apply to MonitorEvent matching. Default matching
     * is based on owner id.
     *
     * @param event the MonitorEvent object to test.
     *
     * @return false if the input object fails the filter; true otherwise.
     */
    public boolean accept(MonitorEvent event) {
        if(owner==null)
            return true;
        return event.getOwner().equals(owner);
    }
}
