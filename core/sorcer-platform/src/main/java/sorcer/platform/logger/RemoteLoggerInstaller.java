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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.sun.jini.admin.DestroyAdmin;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.config.Component;
import sorcer.config.ConfigEntry;
import sorcer.util.ConfigurableThreadFactory;
import sorcer.util.Sorcer;

import java.io.File;
import java.net.UnknownHostException;
import java.util.concurrent.*;

/**
 * Install RemoteLoggerAppender in Logback
 *
 * @author Rafał Krupiński
 */
@Component("sorcer.core.RemoteLogger")
public class RemoteLoggerInstaller implements DestroyAdmin {
    private static final Logger log = LoggerFactory.getLogger(RemoteLoggerInstaller.class);

    private static RemoteLoggerInstaller instance = null;

    @ConfigEntry(required = false)
    public long rate = 200;

    @ConfigEntry(required = false)
    public String hostname;

    private ScheduledFuture<?> scheduledFuture;

    {
        try {
            hostname = Sorcer.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Couldn't determine own hostname");
            hostname = "remote-unknown";
        }
    }

    public static RemoteLoggerInstaller getInstance() {
        if (instance==null) instance = new RemoteLoggerInstaller();
        return instance;
    }

    private RemoteLoggerInstaller() {
        init();
        localInit();
    }

    private void init() {
        BlockingQueue<ILoggingEvent> queue = new LinkedBlockingDeque<ILoggingEvent>();

        installClient(queue);

        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (!(loggerFactory instanceof LoggerContext))
            throw new IllegalStateException("This service must be run with Logback Classic");

        LoggerContext loggerContext = (LoggerContext) loggerFactory;
        ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        String appenderName = getClass().getName();
        Appender<ILoggingEvent> appender = root.getAppender(appenderName);
        if (appender != null)
            throw new IllegalStateException("Appender " + appenderName + " already configured");

        RemoteLoggerAppender remoteAppender = new RemoteLoggerAppender(queue, hostname);
        remoteAppender.setContext(loggerContext);
        remoteAppender.addFilter(MDCFilter.instance);
        remoteAppender.start();
        root.addAppender(remoteAppender);
    }

    private void installClient(BlockingQueue<ILoggingEvent> queue) {
        ConfigurableThreadFactory threadFactory = new ConfigurableThreadFactory();
        threadFactory.setNameFormat("Logger");

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        scheduledFuture = scheduler.scheduleAtFixedRate(new RemoteLoggerClient(queue), 0, rate, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        if (scheduledFuture != null)
            scheduledFuture.cancel(false);
        else
            log.debug("No RemoteLoggerClient started");
    }

    private void localInit() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            // Don't Prc context.reset() to keep the previous configuration
            configurator.doConfigure(new File(Sorcer.getHomeDir(),"configs/apps-logback.xml"));
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        log.info("Extra logback configuration applied");

    }
}
