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

import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import sorcer.core.analytics.MethodAnalytics;

import java.io.IOException;

/**
 * @author Dennis Reedy
 */
public interface Monitor {
    enum Status {SUBMITTED, ACTIVE, COMPLETED, FAILED }

    /**
     * Clients first register with the {@code Monitor} service in order provide status. Clients are responsible
     * for maintaining the {@code MonitorRegistration}'s provided {@code Lease}.
     *
     * @param identifier An identity or name used to associate event with.
     * @param owner The owner, a userid or organization that is producing the event.
     * @param duration The requested lease duration in milliseconds.
     *
     * @return A new {@code MonitorRegistration}
     *
     * @throws IOException If there is a communication failure between the client and the service.
     * @throws MonitorException If the {@code Monitor} service is unable or unwilling to grant this registration request.
     * @throws IllegalArgumentException If the identifier or owner arguments are null, or if {@code duration} is not
     * positive or Lease.ANY.
     */
    MonitorRegistration register(String identifier, String owner, long duration) throws IOException, MonitorException;

    /**
     * Update the status of the registration
     *
     * @param registration The registration
     * @param status The status
     * @param analytics Analytics for the method (operation) being invoked
     *
     * @throws IOException If there is a communication failure between the client and the service.
     * @throws MonitorException If the {@code Monitor} service is unable to update the status of the registration.
     * This may be due to expired lease or other error(s).
     * @throws IllegalArgumentException If the {@code registration} or {@code status} is {@code null}.
     */
    void update(MonitorRegistration registration, Status status, MethodAnalytics analytics) throws IOException, MonitorException;

    /**
     * The register method creates a leased
     * {@link EventRegistration} for the notification of
     * sorcer.core.monitor.MonitorEvent events that correspond to exertions
     * being monitored for a provided Principal.
     * type passed in based on the requested lease duration. The implied
     * semantics of notification are dependant on
     * {@code org.rioproject.event.EventHandler} specializations.
     *
     * @param eventFilter The {@code MonitorEventFilter} to match the provided {@code MonitorEvent}s.
     *                    If null, all {@code MonitorEvent}s will be matched.
     * @param listener A RemoteEventListener to ve notified of {@code MonitorEvent}s that are accepted by
     *                 the {@code MonitorEventFilter}
     * @param duration Requested EventRegistration lease duration
     *
     * @return An EventRegistration
     *
     * @throws IllegalArgumentException if the {@code listener} parameter is null or the {@code duration}
     * is not positive or Lease.ANY.
     * @throws MonitorException if the duration parameter is not accepted, or if the {@code Monitor} service is
     * unable or unwilling to grant this registration request.
     * @throws IOException if communication errors occur
     */
    EventRegistration register(MonitorEventFilter eventFilter, RemoteEventListener listener, long duration) throws IOException,
                                                                                                                   MonitorException;
}
