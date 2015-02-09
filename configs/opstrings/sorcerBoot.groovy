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

/**
 * Deployment configuration for iGrid
 *
 * @author Dennis Reedy
 */
import org.rioproject.RioVersion
import sorcer.util.SorcerEnv

class Sorcer {
    static String sorcerHome = getSorcerHome()
    static Properties versionProps = loadVersions()
    //static String sorcerVersion = versionProps.getProperty("sorcer.version")
    static String sorcerVersion = SorcerEnv.getSorcerVersion()
    static String riverVersion = versionProps.getProperty("river.version")
    static String blitzVersion = versionProps.getProperty("blitz.version")

    static getSorcerHome() {
        String sorcerHome = System.getProperty("sorcer.home", System.getenv("SORCER_HOME"))
        if(sorcerHome==null) {
            throw new RuntimeException("The system property sorcer.home must be set, or the environment SORCER_HOME set")
        }
        sorcerHome
    }
    
    static loadVersions() {
        Properties props = new Properties()
        File propsFile = new File("${sorcerHome}/configs/versions.properties")
        props.load(propsFile.newDataInputStream())
        props
    }
}

def appendJars(def dlJars) {
    dlJars.addAll(getCommonDLs())
    return dlJars as String[]
}

def getCommonDLs() {
    return ["sorcer-dl-${Sorcer.sorcerVersion}.jar",
            "jsk-dl-${Sorcer.riverVersion}.jar",
            "rio-api-${RioVersion.VERSION}.jar",
            "serviceui-${Sorcer.riverVersion}.jar"]
}

def getForkMode() {
    return System.getProperty("fork.mode", "yes")
}

def getDbStorageMaxMem() {
    String maxMem = System.getenv("MAX_MEM")
    if(maxMem == null)
        maxMem = "1G"
    return maxMem
}

deployment(name: "Sorcer OS") {

    groups SorcerEnv.getLookupGroups()

    codebase SorcerEnv.getWebsterUrl()

    service(name: SorcerEnv.getActualName('Transaction Manager')) {
        interfaces {
            classes 'net.jini.core.transaction.server.TransactionManager'
            resources "mahalo-dl-${Sorcer.riverVersion}.jar", "jsk-dl-${Sorcer.riverVersion}.jar"
        }
        implementation(class: 'com.sun.jini.mahalo.TransientMahaloImpl') {
            resources "mahalo-${Sorcer.riverVersion}.jar"
        }
        configuration new File("${Sorcer.sorcerHome}/bin/jini/configs/mahalo.config").text
        maintain 1
    }

    service(name: SorcerEnv.getActualSpaceName(), fork:getForkMode(), jvmArgs:"-Dsorcer.home=${Sorcer.sorcerHome}") {
        interfaces {
            classes 'net.jini.space.JavaSpace05'
            resources "blitz-dl-${Sorcer.blitzVersion}.jar", "blitzui-${Sorcer.blitzVersion}.jar"
        }
        implementation(class: 'org.dancres.blitz.remote.BlitzServiceImpl') {
            resources "blitz-${Sorcer.blitzVersion}.jar", "blitzui-${Sorcer.blitzVersion}.jar", "serviceui-${Sorcer.riverVersion}.jar", "outrigger-dl-${Sorcer.riverVersion}.jar"
        }
        configuration new File("${Sorcer.sorcerHome}/bin/blitz/configs/blitz.config").text
        maintain 1
    }

    service(name: SorcerEnv.getActualName("Rendezvous"), fork:getForkMode(), jvmArgs:"-Dsorcer.home=${Sorcer.sorcerHome}") {
        interfaces {
            classes "sorcer.core.provider.Rendezvous",
                    "sorcer.core.provider.Jobber",
                    "sorcer.core.provider.Spacer"
            resources appendJars(["sorcer-ui-${Sorcer.sorcerVersion}.jar"])
        }
        implementation(class: "sorcer.core.provider.ServiceProvider") {
            resources "sorcer-lib-${Sorcer.sorcerVersion}.jar", "rio-api-${RioVersion.VERSION}.jar"
        }
        configuration new File("${Sorcer.sorcerHome}/bin/sorcer/rendezvous/configs/all-rendezvous-prv.config").text
        maintain 1
    }

    service(name: SorcerEnv.getActualName("Cataloger"), fork:getForkMode()) {
        interfaces {
            classes 'sorcer.core.provider.Cataloger'
            resources appendJars(["sorcer-ui-${Sorcer.sorcerVersion}.jar"])
        }
        implementation(class: 'sorcer.core.provider.cataloger.ServiceCataloger') {
            resources "sos-cataloger-${Sorcer.sorcerVersion}.jar", "sorcer-lib-${Sorcer.sorcerVersion}.jar"
        }
        configuration new File("${Sorcer.sorcerHome}/bin/sorcer/cataloger/configs/cataloger-prv.config").text
        maintain 1
    }

    service(name: SorcerEnv.getActualName("Logger")) {
        interfaces {
            classes 'sorcer.core.provider.RemoteLogger'
            resources appendJars(["sos-logger-${Sorcer.sorcerVersion}-ui.jar", "sorcer-ui-${Sorcer.sorcerVersion}.jar"])
        }
        implementation(class: 'sorcer.core.provider.logger.ServiceLogger') {
            resources "sos-logger-${Sorcer.sorcerVersion}.jar", "sorcer-lib-${Sorcer.sorcerVersion}.jar"
        }
        configuration new File("${Sorcer.sorcerHome}/bin/sorcer/logger/configs/logger-prv.config").text
        maintain 1
    }

    service(name: SorcerEnv.getActualName("Exert Monitor"), fork:getForkMode(), jvmArgs:"-Dsorcer.home=${Sorcer.sorcerHome}") {
        interfaces {
            classes 'sorcer.core.provider.MonitoringManagement'
            resources appendJars(["sorcer-ui-${Sorcer.sorcerVersion}.jar"])
        }
        implementation(class: 'sorcer.core.provider.exertmonitor.ExertMonitor') {
            resources "sos-exertmonitor-${Sorcer.sorcerVersion}.jar", "sorcer-lib-${Sorcer.sorcerVersion}.jar"
        }
        configuration new File("${Sorcer.sorcerHome}/bin/sorcer/exertmonitor/configs/exertmonitor-prv.config").text
        maintain 1
    }
    
    service(name: SorcerEnv.getActualName("Exerter")) {
        interfaces {
            classes 'sorcer.core.provider.Exerter'
            resources appendJars(["sorcer-ui-${Sorcer.sorcerVersion}.jar"])
        }
        implementation(class: 'sorcer.core.provider.ServiceTasker') {
            resources  "sorcer-lib-${Sorcer.sorcerVersion}.jar", "rio-api-${RioVersion.VERSION}.jar"
        }
        configuration new File("${Sorcer.sorcerHome}/bin/sorcer/exerter/configs/exerter-prv.config").text
        maintain 1
    }
    
    service(name: SorcerEnv.getActualName("Database Storage"),
            fork:getForkMode(),
            jvmArgs:"-Dsorcer.home=${Sorcer.sorcerHome} -Xmx${getDbStorageMaxMem()}") {
        interfaces {
            classes 'sorcer.core.provider.DatabaseStorer'
            resources appendJars(["sorcer-ui-${Sorcer.sorcerVersion}.jar"])
        }
        implementation(class: 'sorcer.core.provider.dbp.DatabaseProvider') {
            resources "sos-db-prv-${Sorcer.sorcerVersion}.jar", "sorcer-lib-${Sorcer.sorcerVersion}.jar", "rio-api-${RioVersion.VERSION}.jar"
        }
        configuration new File("${Sorcer.sorcerHome}/bin/sorcer/dbp/configs/dbp-prv.config").text
        maintain 1
    }
}
