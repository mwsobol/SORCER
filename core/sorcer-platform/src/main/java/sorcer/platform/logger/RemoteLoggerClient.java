/*
 * Copyright 2014 Sorcersoft.com S.A.
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

package sorcer.platform.logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.RemoteLogger;
import sorcer.service.Accessor;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Reads the log message queue. If RemoteLogger is available, the message is sent.
 * <p>
 * If RemoteLogger is not available, or it fails twice, the messages are discarded.
 *
 * @author Rafał Krupiński
 */
public class RemoteLoggerClient implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(RemoteLoggerClient.class);
    private BlockingQueue<ILoggingEvent> queue;
    private RemoteLogger remoteLogger;

    public RemoteLoggerClient(BlockingQueue<ILoggingEvent> queue) {
        assert queue != null;
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            LinkedList<ILoggingEvent> loggingEvents = new LinkedList<ILoggingEvent>();
            int count = queue.drainTo(loggingEvents);
            if (count == 0)
                return;
            publish(loggingEvents);
        } catch (Throwable t) {
            log.error("Problem: ", t);
        }
    }

    private void publish(List<ILoggingEvent> loggingEvents) {
        if (remoteLogger != null) {
            publishRetry(vo(loggingEvents));
        } else {
            publishNoRetry1(loggingEvents);
        }
    }

    /**
     * Publishes the logging event to RemoteLogger using cashed proxy. Retries once on failure using {@link #publishNoRetry(java.util.List)}.
     *
     * @param vos the logging events to publish
     */
    private void publishRetry(List<LoggingEventVO> vos) {
        try {
            remoteLogger.publish(vos);
        } catch (RemoteException e) {
            log.debug("Could not publish logging event, retrying", e);
            publishNoRetry(vos);
        }
    }

    private void publishNoRetry1(List<ILoggingEvent> loggingEvents) {
        publishNoRetry(vo(loggingEvents));
    }

    /**
     * Gets a new RemoteLogger proxy and if it's available, publishes the logging event to it.
     *
     * @param vos the logging events to publish
     */
    private void publishNoRetry(List<LoggingEventVO> vos) {
        Object o = Accessor.get().getService(null, RemoteLogger.class);
        remoteLogger = (RemoteLogger)o;
        if (remoteLogger != null)
            try {
                remoteLogger.publish(vos);
            } catch (RemoteException e) {
                log.debug("Could not publish logging event", e);
                remoteLogger = null;
            }
    }

    private static List<LoggingEventVO> vo(List<ILoggingEvent> loggingEvents) {
        List<LoggingEventVO> result = new ArrayList<LoggingEventVO>();
        for (ILoggingEvent event : loggingEvents)
            if (event instanceof LoggingEventVO)
                result.add((LoggingEventVO) event);
            else
                result.add(LoggingEventVO.build(event));
        return result;
    }
}
