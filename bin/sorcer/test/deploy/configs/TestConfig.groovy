package junit.sorcer.core.deploy.config

import org.rioproject.config.Component

/**
 * Configuration for the deployment
 */
@Component('sorcer.core.exertion.deployment')
class TestConfig {
    String[] interfaces = ["some.example.interface.Test"]
    String[] codebaseJars = ["ju-arithmetic-dl.jar"]
    String[] implJars = ["ju-arithmetic-beans.jar"]
    String jvmArgs = "-Xmx4G"
    boolean fork = true

}
