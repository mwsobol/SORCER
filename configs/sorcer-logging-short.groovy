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

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.classic.jul.LevelChangePropagator

import static ch.qos.logback.classic.Level.*

context = new LevelChangePropagator()
context.resetJUL = true

/* Scan for changes every minute. */
scan()

jmxConfigurator()

/*
 * Utility to check if the passed in string ends with a File.separator
 */
def checkEndsWithFileSeparator(String s) {
    if (!s.endsWith(File.separator))
        s = s+File.separator
    return s
}

/*
 * Naming pattern for the output file:
 */
def getLogLocationAndName() {
    String s = InetAddress.getLocalHost().getHostName()
    int ndx = s.indexOf(".")
    String hostName = s.substring(0, ndx)
    String logDir = checkEndsWithFileSeparator(System.getProperty("rio.log.dir"))
    String name = "${System.getProperty("org.rioproject.service")}-${hostName}"
    System.setProperty("org.rioproject.service", name)
    return "$logDir${name}"
}

def appenders = []

/*
 * Only add the CONSOLE appender if we have a console
 */
if (System.getProperty("forceConsoleLogging")!=null || System.console() != null) {
    appender("CONSOLE", ConsoleAppender) {
        if(!System.getProperty("os.name").startsWith("Windows") && System.console() != null) {
            withJansi = true

            encoder(PatternLayoutEncoder) {
                pattern = "%highlight(%-5level) %d{HH:mm:ss.SSS} %logger{36} %X{mogId} - %msg%n%rEx"
            }
        } else {
            encoder(PatternLayoutEncoder) {
                pattern = "%-5level %d{HH:mm:ss.SSS} %logger{36} %X{mogId} - %msg%n%rEx"
            }
        }
    }
    appenders << "CONSOLE"
}

/*
 * Only add the rolling file appender if we are logging for a service
 */
if (System.getProperty("org.rioproject.service")!=null) {
    def serviceLogFilename = getLogLocationAndName()

    appender("ROLLING", RollingFileAppender) {
        file = serviceLogFilename+".log"
        rollingPolicy(TimeBasedRollingPolicy) {

            /* Rollover daily */
            fileNamePattern = "${serviceLogFilename}-%d{yyyy-MM-dd}.%i.log"

            /* Or whenever the file size reaches 10MB */
            timeBasedFileNamingAndTriggeringPolicy(SizeAndTimeBasedFNATP) {
                maxFileSize = "10MB"
            }

            /* Keep 5 archived logs */
            maxHistory = 5

        }
        encoder(PatternLayoutEncoder) {
            pattern = "%-5level %d{HH:mm:ss.SSS} %logger{36} %X{mogId} - %msg%n%rEx"
        }
    }
    appenders << "ROLLING"
}

/* Set up loggers */
/* ==================================================================
 *  Rio Loggers
 * ==================================================================*/
logger("org.rioproject.cybernode", DEBUG)
logger("org.rioproject.config", INFO)
logger("org.rioproject.resources.servicecore", INFO)
logger("org.rioproject.system", DEBUG)
logger("org.rioproject.impl.container.ServiceBeanLoader", INFO)
logger("org.rioproject.system.measurable", INFO)
logger("org.rioproject.impl.servicebean", INFO)
logger("org.rioproject.associations", INFO)

logger("org.rioproject.monitor", DEBUG)
logger("org.rioproject.monitor.sbi", DEBUG)
logger("org.rioproject.monitor.provision", DEBUG)
logger("org.rioproject.monitor.selector", OFF)
logger("org.rioproject.monitor.services", DEBUG)
logger("org.rioproject.monitor.DeploymentVerifier", INFO)
logger("org.rioproject.monitor.InstantiatorResource", INFO)
logger("org.rioproject.monitor.service.managers.FixedServiceManager", INFO)
logger("org.rioproject.resolver.aether", OFF)

logger("org.rioproject.rmi.ResolvingLoader", OFF)

logger("org.rioproject.config.GroovyConfig", INFO)

logger("net.jini.discovery.LookupDiscovery", OFF)
logger("net.jini.lookup.JoinManager", OFF)
logger("org.rioproject.resolver.aether.util.ConsoleRepositoryListener", WARN)

/* ==================================================================
 *  SORCER Loggers
 * ==================================================================*/
logger("sorcer", WARN)

root(INFO, appenders)


