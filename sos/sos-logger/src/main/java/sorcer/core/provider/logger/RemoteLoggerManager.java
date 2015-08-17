/*
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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
package sorcer.core.provider.logger;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.LeaseDeniedException;
import org.apache.commons.io.FileUtils;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.EventHandler;
import org.rioproject.event.NoEventConsumerException;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import sorcer.core.provider.RemoteLogger;
import sorcer.core.provider.Provider;
import sorcer.util.SenderEventHandler;
import sorcer.util.Sorcer;

import java.io.File;
import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteLoggerManager implements RemoteLogger {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RemoteLoggerManager.class);

    // The list of all known loggers.
    private List<LoggingConfig> knownLoggers = new LinkedList<LoggingConfig>();

    private LoggerContext loggerFactory;

    private File logDir = new File(Sorcer.getHomeDir(), "logs/remote");

    private Map<Map<String,String>, EventHandler> remoteLogListeners = new ConcurrentHashMap<Map<String,String>, EventHandler>();

    private Map<Long, EventHandler> remoteLogHandlers = new ConcurrentHashMap<Long, EventHandler>();

    private Provider provider;

    public RemoteLoggerManager() {
        ILoggerFactory loggerFactory;
        loggerFactory = LoggerFactory.getILoggerFactory();
        if (!(loggerFactory instanceof LoggerContext))
            throw new IllegalStateException("This service must be running with Logback Classic");
        this.loggerFactory = (LoggerContext) loggerFactory;
    }

    public void init(Provider provider) throws RemoteException {
        this.provider = provider;
    }

    public String[] getLogNames() throws RemoteException {
        if (this.logDir == null)
            return new String[0];
        List<String> list = new LinkedList<String>();

        String[] loggerNames;
        loggerNames = logDir.list();
        for (String loggerName : loggerNames)
            if (loggerName.endsWith(".log"))
                list.add(loggerName);
        return list.toArray(new String[list.size()]);
    }

    public void publish(List<LoggingEventVO> loggingEvents) {
        for (LoggingEventVO vo : loggingEvents)
            publish(vo);
    }

    protected void publish(ILoggingEvent loggingEvent) {
        String loggerName;
        Logger logger;
        synchronized (this) {
            //log.info("Publishing remote log: " + loggingEvent.getMessage().substring(0,Math.min(loggingEvent.getMessage().length(),50)));
            Map<String, String> mdc = loggingEvent.getMDCPropertyMap();
            String exertionId = null;
            if (!remoteLogListeners.isEmpty()) {
                exertionId = mdc.get(KEY_EXERTION_ID);
                Map<String, String> keyMap = new HashMap<String, String>();
                keyMap.put(KEY_EXERTION_ID, exertionId);

                for (Map.Entry<Map<String, String>, EventHandler> entry : remoteLogListeners.entrySet()) {
                    if (mdc.entrySet().containsAll(entry.getKey().entrySet())) {
                        try {
                            LoggerRemoteEvent rse = new LoggerRemoteEvent(provider.getProxy(), loggingEvent);
                            entry.getValue().fire(rse);
                            //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!! Sending log to remote listener exID: " + exertionId + ": " + loggingEvent.getMessage().substring(0, Math.min(loggingEvent.getMessage().length(), 50)));
                        } catch (NoEventConsumerException e) {
                            log.error("Problem sending remote log event, no event consumer available");
                        } catch (RemoteException e) {
                            log.error("Problem getting proxy from provider - should never happen as provider is local!!!");
                        }
                    }
                }
            }
            loggerName = loggingEvent.getLoggerName();
            logger = loggerFactory.getLogger(loggerName);
            Appender<ILoggingEvent> appender = logger.getAppender(loggerName);
            if (appender == null) {
                String hostname;
                if (mdc.containsKey(KEY_HOSTNAME))
                    hostname = mdc.get(KEY_HOSTNAME);
                else
                    hostname = "remote";
                //logger.setAdditive(false);
                logger.addAppender(createAppender(loggerName, hostname));
            }
        }

        logger.callAppenders(loggingEvent);
        LoggingConfig lc = new LoggingConfig(loggerName, null);
        if (!knownLoggers.contains(lc)) {
            lc.setLevel(Level.ALL);
            knownLoggers.add(lc);
        }
    }

    private Appender<ILoggingEvent> createAppender(String loggerName, String prefix) {
        Appender<ILoggingEvent> appender;
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setName(loggerName);
        File file = new File(logDir, prefix + "-" + loggerName + ".log");
        fileAppender.setFile(file.getPath());
        fileAppender.setContext(loggerFactory);
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerFactory);
        encoder.setPattern("%-5level %d{HH:mm:ss.SSS} [%t] %logger{36} - %msg%n%rEx");
        fileAppender.setEncoder(encoder);
        appender = fileAppender;
        encoder.start();
        appender.start();
        return appender;
    }

    public List<String> getLog(String fileName) throws RemoteException {
        try {
            return FileUtils.readLines(new File(logDir, fileName));
        } catch (IOException e) {
            String msg = MessageFormatter.format("Error reading file {}", fileName).getMessage();
            log.warn(msg, e);
            return Arrays.asList(msg);
        }
    }

    public List<LoggingConfig> getLoggers() throws IOException {
        return knownLoggers;
    }

    public void deleteLog(String loggerName) throws RemoteException {
        // TODO implement
    }

    public EventRegistration registerLogListener(RemoteEventListener listener, MarshalledObject handback, long duration, List<Map<String,String>> filterMap) throws LeaseDeniedException, RemoteException {
        log.debug("Registering new listener for remote logs: " + listener);
        EventDescriptor eventDescriptor = new EventDescriptor(ILoggingEvent.class, LoggerRemoteEvent.ID);
        EventHandler eventHandler;
        try {
            //eventHandler = new DispatchEventHandler(eventDescriptor);
            eventHandler = new SenderEventHandler(eventDescriptor);
            EventRegistration evReg = eventHandler.register(provider.getProxy(), listener, handback, duration);
            log.debug("Got evRegID: " + evReg.getID() + " filters: " + filterMap);
            remoteLogHandlers.put(evReg.getID(), eventHandler);
            for (Map<String, String>  fMap : filterMap)
                remoteLogListeners.put(fMap, eventHandler);
            return evReg;
        } catch (Exception e1) {
            log.error("Problem registering to Log listener: " + e1.getMessage());
        }
        return null;
    }

    //@Override
    public void unregisterLogListener(EventRegistration evReg) throws RemoteException {
        log.debug("Unregistering listener for remote logs: " + evReg.getID());
        try {
            EventHandler evHandler = remoteLogHandlers.get(evReg.getID());
            if (evHandler!=null) {
                List<Map<String,String>> toRemove = new ArrayList<Map<String, String>>();
                for (Map.Entry<Map<String, String>, EventHandler> entry : remoteLogListeners.entrySet()) {
                    if (entry.getValue().equals(evHandler)) {
                        toRemove.add(entry.getKey());
                    }
                }
                for (Map<String, String> key : toRemove)
                    remoteLogListeners.remove(key);
                remoteLogHandlers.remove(evReg.getID());
            } else {
                log.error("Problem unregistering, listener for: " + evReg.getID() + " doesn't exist");
            }
        } catch (Exception e1) {
            log.error("Problem unregistering Log listener: " + e1.getMessage());
        }
    }
}
