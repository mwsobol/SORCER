package junit.sorcer.core.deploy.configs

import org.rioproject.config.Component

@Component('sorcer.core.exertion.deployment')
class TestIP {
    String[] interfaces = ["some.example.interface.Test"]
    String[] codebaseJars = ["ju-arithmetic-dl.jar"]
    String[] implJars = ["ju-arithmetic-beans.jar"]
    String jvmArgs = "-Xmx4G"
    boolean fork = true

    def opSys = ["Linux", "Mac"] as String[]

    String arch = "x86_64"

    def ips = ["10.131.5.106", "10.131.4.201", "macdna.rb.rad-e.wpafb.af.mil", "10.0.1.9"] as String[]

    def ips_exclude = ["127.0.0.1"] as String[]
}