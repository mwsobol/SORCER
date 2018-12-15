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
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dennis Reedy
 */
public class AnalyticsProducerTest {
    
    @Test
    public void testAnalyticsProducer() throws IOException {
        AnalyticsProducerPhony analyticsProducer = new AnalyticsProducerPhony();
        int id = analyticsProducer.analyticsRecorder.inprocess("foo");
        analyticsProducer.analyticsRecorder.completed("foo", id);

        Analytics analytics = analyticsProducer.getAnalytics();
        assertNotNull(analytics);
        assertTrue(analytics.getMethodAnalytics().size()>0);
        MethodAnalytics methodAnalytics1 = analytics.getMethodAnalytics("foo");
        assertNotNull(methodAnalytics1);
        assertNotNull(analytics.getSystemAnalytics());

        assertNotNull(analytics.getScratchFree());
        assertNotNull(analytics.getScratchUrl());

        Map<String, MethodAnalytics> methodAnalyticsMap = analyticsProducer.getMethodAnalytics();
        assertNotNull(methodAnalyticsMap);
        assertTrue(methodAnalyticsMap.size()>0);
        MethodAnalytics methodAnalytics2 = analyticsProducer.getMethodAnalytics("foo");
        assertNotNull(methodAnalytics2);

        assertNotNull(analyticsProducer.getSystemAnalytics());
    }
    
    class AnalyticsProducerPhony implements AnalyticsProducer {
        AnalyticsRecorder analyticsRecorder;
        AnalyticsProducerPhony() throws UnknownHostException {
            Uuid uuid = UuidFactory.generate();
            ServiceID serviceID = new ServiceID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
            InetAddress inetAddress = InetAddress.getLocalHost();
            analyticsRecorder = new AnalyticsRecorder(inetAddress.getHostName(),
                                                      inetAddress.getHostAddress(),
                                                      serviceID,
                                                      "test",
                                                      System.getProperty("user.name"));
        }


        @Override public Analytics getAnalytics() {
            return new Analytics("Ferris",
                                 analyticsRecorder.getMethodAnalytics(),
                                 analyticsRecorder.getSystemAnalytics(),
                                 23,
                                 System.currentTimeMillis(),
                                 "http://foo:9090",
                                 "1024.0");
        }

        @Override public Map<String, MethodAnalytics> getMethodAnalytics() {
            return analyticsRecorder.getMethodAnalytics();
        }

        @Override public MethodAnalytics getMethodAnalytics(String name) {
            return analyticsRecorder.getMethodAnalytics(name);
        }

        @Override public SystemAnalytics getSystemAnalytics() {
            return analyticsRecorder.getSystemAnalytics();
        }
    }
}
