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

import java.io.Serializable;

public class SystemAnalytics implements Serializable {
    static final long serialVersionUID = 1L;
    private String systemCPUPercent;
    private String processCPUUtilization;
    private double systemMemoryTotal;
    private double systemMemoryUsed;
    private double processMemoryTotal;
    private double processMemoryUsed;

    public String getProcessCPUUtilization() {
        return processCPUUtilization;
    }

    public SystemAnalytics setProcessCPUUtilization(String processCPUUtilization) {
        this.processCPUUtilization = processCPUUtilization;
        return this;
    }

    public double getProcessMemoryTotal() {
        return processMemoryTotal;
    }

    public SystemAnalytics setProcessMemoryTotal(double processMemoryTotal) {
        this.processMemoryTotal = processMemoryTotal;
        return this;
    }

    public double getProcessMemoryUsed() {
        return processMemoryUsed;
    }

    public SystemAnalytics setProcessMemoryUsed(double processMemoryUsed) {
        this.processMemoryUsed = processMemoryUsed;
        return this;
    }

    public double getSystemMemoryUsed() {
        return systemMemoryUsed;
    }

    public SystemAnalytics setSystemMemoryUsed(double systemMemoryUsed) {
        this.systemMemoryUsed = systemMemoryUsed;
        return this;
    }

    public double getSystemMemoryTotal() {
        return systemMemoryTotal;
    }

    public SystemAnalytics setSystemMemoryTotal(double systemMemoryTotal) {
        this.systemMemoryTotal = systemMemoryTotal;
        return this;
    }

    public String getSystemCPUPercent() {
        return systemCPUPercent;
    }

    public SystemAnalytics setSystemCPUPercent(String systemCPUPercent) {
        this.systemCPUPercent = systemCPUPercent;
        return this;
    }

    @Override public String toString() {
        return "SystemAnalytics :" +
               "processCPUUtilization='" + processCPUUtilization + '\'' +
               ", systemCPUPercent='" + systemCPUPercent + '\'' +
               ", systemMemoryTotal='" + systemMemoryTotal + '\'' +
               ", systemMemoryUsed='" + systemMemoryUsed + '\'' +
               ", processMemoryTotal='" + processMemoryTotal + '\'' +
               ", processMemoryUsed='" + processMemoryUsed;
    }
}

