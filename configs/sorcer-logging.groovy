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
 *
 * a) The output file is placed in the directory defined by the "rio.log.dir" System property
 * b) With a name based on the "org.rioproject.service" System property.
 * c) The return value from ManagementFactory.getRuntimeMXBean().getName(). This value is expected to have the
 * following format: pid@hostname. If the return includes the @hostname, the @hostname is stripped off.
 */
def getLogLocationAndName() {
    String logDir = checkEndsWithFileSeparator(System.getProperty("rio.log.dir"))
    return "$logDir${System.getProperty("org.rioproject.service")}"
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
                pattern = "%highlight(%-5level) %d{HH:mm:ss.SSS} %logger{36} - %msg%n%rEx"
            }
        } else {
            encoder(PatternLayoutEncoder) {
                pattern = "%-5level %d{HH:mm:ss.SSS} %logger{36} - %msg%n%rEx"
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
            pattern = "%-5level %d{HH:mm:ss.SSS} %logger{36} - %msg%n%rEx"
        }
    }
    appenders << "ROLLING"
}

/* Set up loggers */
/* ==================================================================
 *  Rio Loggers
 * ==================================================================*/
logger("org.rioproject.cybernode", DEBUG)
logger("org.rioproject.cybernode.loader", DEBUG)
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

logger("org.rioproject.gnostic", INFO)
logger("org.rioproject.gnostic.drools", INFO)
logger("org.rioproject.gnostic.service.DroolsCEPManager", INFO)
logger("org.rioproject.config.GroovyConfig", INFO)

logger("net.jini.discovery.LookupDiscovery", OFF)
logger("net.jini.lookup.JoinManager", OFF)
logger("org.rioproject.resolver.aether.util.ConsoleRepositoryListener", WARN)

/* ==================================================================
 *  SORCER Loggers
 * ==================================================================*/

logger("sorcer.util.ProviderAccessor", WARN)
logger("sorcer.core.provider.cataloger.ServiceCataloger", WARN)
//logger("sorcer.provider.boot", TRACE)
logger("sorcer.core.provider.ServiceProvider", WARN)
//logger("sorcer.core.provider.rendezvous.RendezvousBean", ALL)
//logger("sorcer.core.provider.rendezvous.ServiceModeler", ALL)
logger("sorcer.core.provider.ControlFlowManager", WARN)
logger("sorcer.core.provider.ProviderDelegate", INFO)
logger("sorcer.tools.shell.NetworkShell", WARN)
logger("sorcer.core.provider.exertmonitor.ExertMonitor", WARN)
logger("sorcer.core.provider.SpaceTaker", WARN)
logger("sorcer.core.provider.exertmonitor", TRACE)
logger("sorcer.core.monitor", TRACE)
logger("sorcer.core.dispatch", INFO)
logger("sorcer.core.dispatch.ExertionSorter", WARN)
logger("sorcer.rio.rmi", WARN)
logger("sorcer.service.Accessor", WARN)
logger("sorcer.core.provider.exerter", WARN)
logger("sorcer.platform.logger", WARN)
logger("sorcer.core.provider.logger", WARN)

/* ==================================================================
 *  SORCER Variable oriented loggers
 * ==================================================================*/

/*
logger("sorcer.modeling", OFF)
logger("sorcer.modeling.vfe.Var", OFF)
logger("sorcer.modeling.vfe.filter.Filter", OFF)
logger("sorcer.modeling.vfe.filter.BasicFileFilter", OFF)
logger("sorcer.modeling.vfe.evaluator", OFF)
logger("sorcer.modeling.vfe.ServiceEvaluator", INFO)
logger("sorcer.core.context.model", TRACE)
logger("sorcer.core.context.model.var", TRACE)
logger("sorcer.core.context.model.explore", TRACE)
logger("sorcer.core.context.model.opti", TRACE)
logger("sorcer.core.context.model.explore.Explorer", TRACE)
logger("sorcer.core.context.model.explore.ExploreDispatcher", TRACE)
logger("sorcer.core.context.model.explore.ModelManager", TRACE)
logger("sorcer.core.context.model.opti", TRACE)
logger("sorcer.modeling.vfe.persist.TaskContextSetter", OFF)
logger("sorcer.modeling.vfe.persist.TaskContextSetter", OFF)
*/
logger("sorcer.modeling.core.context.model.var.ResponseModel", WARN)
logger("sorcer.modeling.core.context.model.var.ParametricModel", WARN)

/* ==================================================================
 *  SORCER Other specialized loggers
 * ==================================================================*/
logger("sorcer.core.context.eval", OFF)
logger("sorcer.core.context", TRACE)
logger("sorcer.jini.jeri.SorcerILFactory", WARN)

logger("sorcer.ui.tools", DEBUG)
logger("sorcer.util", DEBUG)

root(INFO, appenders)


