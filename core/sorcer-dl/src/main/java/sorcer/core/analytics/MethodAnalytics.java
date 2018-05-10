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
package sorcer.core.analytics;

import net.jini.core.lookup.ServiceID;

import java.io.Serializable;

/**
 * @author Dennis Reedy
 */
public class MethodAnalytics implements Serializable {
    static final long serialVersionUID = 1L;
    private int numActiveOperations;
    private int completed;
    private int failed;
    private int totalOperationCalls;
    private long totalCallTime;
    private ServiceID serviceID;
    private String hostName;
    private String activeOperations;
    private String methodName;
    private double averageExecTime;

    public MethodAnalytics(String activeOperations,
                           double averageExecTime,
                           int completed,
                           int failed,
                           String hostName,
                           String methodName,
                           int numActiveOperations,
                           ServiceID serviceID,
                           long totalCallTime,
                           int totalOperationCalls) {
        this.activeOperations = activeOperations;
        this.averageExecTime = averageExecTime;
        this.completed = completed;
        this.failed = failed;
        this.hostName = hostName;
        this.methodName = methodName;
        this.numActiveOperations = numActiveOperations;
        this.serviceID = serviceID;
        this.totalCallTime = totalCallTime;
        this.totalOperationCalls = totalOperationCalls;
    }

    public int getNumActiveOperations() {
        return numActiveOperations;
    }

    public double getAverageExecTime() {
        return averageExecTime;
    }

    public int getTotalOperationCalls() {
        return totalOperationCalls;
    }

    public String getActiveOperations() {
        return activeOperations;
    }

    public ServiceID getServiceID() {
        return serviceID;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getHostName() {
        return hostName;
    }

    public int getCompleted() {
        return completed;
    }

    public int getFailed() {
        return failed;
    }

    public long getTotalCallTime() {
        return totalCallTime;
    }

    @Override public String toString() {
        return String.format("%s, completed: %s, numActiveOps: %s, averageExecTime: %s, " +
                             "totalOperationCalls: %s, activeOperations: %s, totalCallTime: %s",
                             methodName,
                             getCompleted(),
                             getNumActiveOperations(),
                             getAverageExecTime(),
                             getTotalOperationCalls(),
                             getActiveOperations(),
                             getTotalCallTime());
    }
}
