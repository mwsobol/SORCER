#!/usr/bin/env groovy
/*
 * Distribution Statement
 *
 * This computer software has been developed under sponsorship of the United States Air Force Research Lab. Any further
 * distribution or use by anyone or any data contained therein, unless otherwise specifically provided for,
 * is prohibited without the written approval of AFRL/RQVC-MSTC, 2210 8th Street Bldg 146, Room 218, WPAFB, OH  45433
 *
 * Disclaimer
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government. Neither
 * the United States Government nor the United States Air Force, nor any of their employees, makes any warranty,
 * express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness
 * of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately
 * owned rights.
 */

/*
 * Starts a Webster serving up Sorcer distribution content
 */

String scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
def config = new ConfigSlurper().parse(new File(scriptDir, "../configs/websterConfig.groovy").toURI().toURL())
boolean spawn = config.webster.spawn
StringBuilder java = new StringBuilder()
java.append(System.getProperty('java.home')).append("/bin/java")

def args = []
if (System.getProperty("os.name").startsWith("Windows")) {
    args << "cmd.exe"
    args << "/C"
}

StringBuilder websterRoots = new StringBuilder()
config.webster.roots.each { root->
    if(websterRoots.length()>0)
        websterRoots.append(";")
    websterRoots.append(root)
}
args << java.toString()

["java.protocol.handler.pkgs": "net.jini.url|sorcer.util.url|org.rioproject.url",
 "java.security.policy" : "${config.paths.sorcerHome}/policy/policy.all",
 "java.rmi.server.useCodebaseOnly" : "false",
 "webster.debug" : "true",
 "webster.port" : "${config.webster.port}",
 "webster.interface" : "${config.webster.address}",
 "webster.tmp.dir" : "${config.paths.sorcerHome}/data",
 "webster.root" : "${websterRoots.toString()}"
].each { key, value ->
    args << "-D${key}=${value}"
}
args << "-Xmx450M"
args << "-jar"
args << "${config.paths.sorcerHome}/lib/sorcer/lib-ext/webster-${config.versions.sorcer}.jar"

ProcessBuilder pb = new ProcessBuilder(args as String[])
Map<String, String> env = pb.environment()
env.put("SORCER_HOME", "${config.paths.sorcerHome}")
env.put("RIO_HOME", "${config.paths.sorcerHome}/rio-${config.versions.rio}")

Process process = pb.start()
if (!spawn) {
    process.consumeProcessOutput(System.out, System.err)
    process.waitFor()
} else {
    println "tempDir: ${config.paths.sorcerHome}/data"
    int rootNum = 0
    config.webster.roots.each { root ->
        println "Root ${rootNum++} = $root"
    }
    println "Webster serving on : ${config.webster.address}:${config.webster.port}"
    process.in.close()
    process.out.close()
    process.err.close()
}
