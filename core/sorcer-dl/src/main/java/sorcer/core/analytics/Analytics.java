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
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dennis Reedy
 */
public class Analytics implements Serializable {
    static final long serialVersionUID = 1L;
    private final Map<String, MethodAnalytics> methodAnalyticsMap = new HashMap<>();
    private final SystemAnalytics systemAnalytics;

    public Analytics(Map<String, MethodAnalytics> methodAnalyticsMap, SystemAnalytics systemAnalytics) {
        this.methodAnalyticsMap.putAll(methodAnalyticsMap);
        this.systemAnalytics = systemAnalytics;
    }

    public Map<String, MethodAnalytics> getMethodAnalytics() {
        return methodAnalyticsMap;
    }

    public MethodAnalytics getMethodAnalytics(String name) {
        return methodAnalyticsMap.get(name);
    }

    public SystemAnalytics getSystemAnalytics() {
        return systemAnalytics;
    }

}
