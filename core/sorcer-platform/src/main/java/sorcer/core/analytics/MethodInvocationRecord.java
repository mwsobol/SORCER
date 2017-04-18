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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Dennis Reedy
 */
class MethodInvocationRecord {
    AtomicInteger numActiveOperations = new AtomicInteger();
    private AtomicInteger idGenerator = new AtomicInteger();
    private AtomicInteger completed = new AtomicInteger();
    private AtomicInteger failed = new AtomicInteger();
    private final Map<Integer, Long> stopWatch = new ConcurrentHashMap<>();
    private AtomicInteger totalOperationCalls = new AtomicInteger();
    private AtomicLong totalCallTime = new AtomicLong();
    private volatile double averageExecTime;
    private final String methodName;
    static Logger logger = LoggerFactory.getLogger(MethodInvocationRecord.class);

    MethodInvocationRecord(String methodName) {
        this.methodName = methodName;
    }

    int inprocess() {
        int id = idGenerator.incrementAndGet();
        numActiveOperations.incrementAndGet();
        stopWatch.put(id, System.nanoTime());
        return id;
    }

    void failed(int id) {
        handleCallTime(id);
        failed.incrementAndGet();
    }

    void complete(int id) {
        handleCallTime(id);
        completed.incrementAndGet();
    }

    void complete(long startTime) {
        handleCallTime(startTime);
        completed.incrementAndGet();
    }

    MethodAnalytics create(ServiceID serviceID, String hostName) {
        String activeOps = "";
        synchronized (stopWatch) {
            if (stopWatch.size() > 0) {
                StringBuilder b = new StringBuilder();
                for (Integer i : stopWatch.keySet()) {
                    if (b.length() > 0)
                        b.append(", ");
                    b.append(Integer.toString(i));
                }
                activeOps = b.toString();
            }
        }
        return new MethodAnalytics(activeOps,
                                   averageExecTime,
                                   completed.get(),
                                   failed.get(),
                                   hostName,
                                   methodName,
                                   numActiveOperations.get(),
                                   serviceID,
                                   totalCallTime.get(),
                                   totalOperationCalls.get());
    }

    private void handleCallTime(long startTime) {
        long callTime = System.nanoTime() - startTime;;
        totalCallTime.addAndGet(callTime);
        int totalCalls = totalOperationCalls.incrementAndGet();
        averageExecTime = totalCallTime.get() / totalCalls;
        numActiveOperations.decrementAndGet();
    }

    private void handleCallTime(int id)  {
        if(stopWatch.containsKey(id)) {
            handleCallTime(stopWatch.remove(id));
        } else {
            logger.error("{} id not found: {}", id, methodName);
            totalOperationCalls.incrementAndGet();
        }
    }

    @Override public String toString() {
        return String.format("%s: completed: %s, numActiveOps: %s, averageExecTime: %s, " +
                             "totalOperationCalls: %s, activeOperations: %s, totalCallTime: %s",
                             methodName,
                             completed.get(),
                             numActiveOperations.get(),
                             averageExecTime,
                             totalOperationCalls.get(),
                             stopWatch.keySet().toString(),
                             totalCallTime.get());
    }
}
