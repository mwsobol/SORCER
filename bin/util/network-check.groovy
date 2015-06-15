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
import groovy.io.FileType

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.rmi.MarshalledObject
import java.util.concurrent.TimeUnit

def cli = new CliBuilder(usage: 'network-check.groovy -[cst|u] [-hd]')
cli.header = "Check the status of multicast broadcast and receive capabilities as well as unicast discovery"
cli.c('Run as multicast client')
cli.d('Show classloader details')
cli.h('Show usage information')
cli.s('Run as multicast-server')
cli.t('Specify timeout in seconds', args: 2, argName: 'timeout',)
cli.u('Perform unicast discovery to another machine', args: 1, argName: 'hostname[:port]')
def options = cli.parse(args)

if (args.length == 0 || options.h) {
    cli.usage()
    return
}

String scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
File engHome =  new File(scriptDir, "../../")
System.setProperty("java.security.policy", new File(engHome, "configs/policy.all").absolutePath)
if(System.securityManager==null)
    System.securityManager = new SecurityManager()

def props = new Properties()
new File(scriptDir, "../../gradle.properties").withReader { reader ->
    props.load(reader)
}

File sorcerModelingDist = new File(engHome, "../distributions/sorcer-modeling-${props['sorcer.modeling.version']}")
File rioHome = new File(sorcerModelingDist, "rio-${props['rio.version']}")

def jars = []
rioHome.eachFileRecurse (FileType.FILES) { file ->
    if(file.name.startsWith("rio-platform") ||
       file.name.startsWith("jsk-platform") ||
       file.name.startsWith("jsk-lib") ||
       file.name.startsWith("jsk-dl") ||
       file.name.startsWith("serviceui") ||
       file.name.startsWith("rio-api") ||
       file.name.startsWith("rio-logging-support") ||
       file.name.startsWith("slf4j-api") ||
       file.name.startsWith("logback")) {
        /* Skip the logging/logback directory to avoid having multiple slf4j bindings */
        if(!file.parentFile.path.endsWith("logback"))
            jars << file.toURI().toURL()
    }
}

/* The following is somewhat of a hack to get Rio's RMICLassLoader into the
 * system classloader, allowing for the resolution of the artifact: URL handling */
try {
    Class<URLClassLoader> classLoaderClass = URLClassLoader.class
    URLClassLoader systemClassLoader = ClassLoader.systemClassLoader as URLClassLoader
    Method addURL = classLoaderClass.getDeclaredMethod("addURL", URL.class)
    addURL.setAccessible(true)
    jars.each { jar ->
        addURL.invoke(systemClassLoader, jar);
    }
} catch (Throwable t) {
    t.printStackTrace();
    return
}

["java.rmi.server.useCodebaseOnly" : "false",
 "logback.configurationFile" : "${engHome.path}/configs/mstc-test-logback.xml",
 //"java.rmi.server.RMIClassLoaderSpi" : "org.rioproject.rmi.ResolvingLoader",
 //"org.rioproject.resolver.jar" : new File(rioHome, "lib/resolver/resolver-aether-${props['rio.version']}.jar").path,
 "rio.home" : rioHome.absolutePath].each { key, value ->
    System.setProperty(key, value)
}

if(options.d) {
    GroovyClassLoader gcl = Thread.currentThread().contextClassLoader as GroovyClassLoader
    LinkedList<URLClassLoader> list = new LinkedList<URLClassLoader>()
    list.add(0, gcl)
    URLClassLoader parent = gcl.getParent() as URLClassLoader
    while(parent!=null) {
        list.add(0, parent)
        parent = parent.getParent() as URLClassLoader
    }
    list.each { loader ->
        println "$loader"
        loader.getURLs().each { url ->
            println "\t${url.toExternalForm()}"
        }
    }
}

boolean client = options.c
boolean server = options.s

long seconds = options.t ? Long.parseLong(options.t) : 30
long timeout = TimeUnit.SECONDS.toMillis(seconds)

/** Use Jini's multicast group */
def group = InetAddress.getByName("224.0.1.85")
int port = 8888

long endMillis = System.currentTimeMillis() + timeout

def multicastClient = { ->
    MulticastSocket msocket = new MulticastSocket(port)
    msocket.setTimeToLive(1) // XXX - could it be 0 ?
    msocket.joinGroup(group)
    msocket.setSoTimeout(timeout as int)
    /*
     * Receive thread waits for msocket SOTIMEOUT to receive a packet.
     */
    new Thread("MulticastStatus") {
        public void run() {
            byte[] buf = new byte[1024]
            DatagramPacket recv = new DatagramPacket(buf, buf.length)
            while (true) {
                try {
                    msocket.receive(recv)
                    String message = new String(buf, 0, buf.length)
                    println("Received: " + message)
                } catch (Throwable e) {
                    e.printStackTrace()
                    long remainingMillis = endMillis - System.currentTimeMillis();
                    if (remainingMillis <= 0) {
                        return;
                    }
                }
            }
        }
    }.start()
}

def multicastServer = { ->
    MulticastSocket msocket = new MulticastSocket(port)
    msocket.setTimeToLive(1) // XXX - could it be 0 ?
    msocket.joinGroup(group)
    msocket.setSoTimeout(timeout as int)
    String hostName = InetAddress.getLocalHost().hostName
    int i = 1
    while (System.currentTimeMillis() < endMillis) {
        StringBuilder builder = new StringBuilder()
        builder.append("(${i++}) $hostName date: ").append(new Date(System.currentTimeMillis()))
        final byte[] messageBytes = builder.toString().getBytes()
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, group, port)
        println "Sending:  ${builder.toString()} ..."
        msocket.send(packet);
        Thread.sleep(500)
    }
}

def discover = { host ->
    String[] parts = host.split(":")
    int discoveryPort = parts.length==1?4160:Integer.parseInt(parts[1])
    println "Attempt unicast discovery to ${parts[0]}:$discoveryPort"

    /* The following code is copied from the LookupLocator and avoids
     * issues with creating an instance of the LookupLocator as it relates to getting the
     * script's classpath set correctly */
    Socket sock = new Socket(parts[0], discoveryPort)
    sock.setSoTimeout(timeout as int)
    try {
        sock.setTcpNoDelay(true)
    } catch (SocketException e) {
        // ignore possible failures and proceed anyway
    }
    try {
        sock.setKeepAlive(true)
    } catch (SocketException e) {
        // ignore possible failures and proceed anyway
    }
    DataOutputStream dstr = new DataOutputStream(sock.getOutputStream())
    int protoVersion = 1
    dstr.writeInt(protoVersion);
    dstr.flush();
    ObjectInputStream istr = new ObjectInputStream(sock.getInputStream())
    /* Set the content classloader to the system classloader, this addresses conflicting
     * groovy configuration classloading for the resolver */
    Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader())

    def registrar = ((MarshalledObject)istr.readObject()).get()
    for (int grpCount = istr.readInt(); --grpCount >= 0; ) {
        istr.readUTF(); // ensure proper format, then discard
    }
    println "Groups: ${registrar.groups}"
    println "Registrar locator: ${registrar.locator.toString()}"

    Class cl = ClassLoader.getSystemClassLoader().loadClass("net.jini.core.lookup.ServiceTemplate")
    Constructor c = cl.constructors[0]
    def serviceTemplate = c.newInstance(null, null, null)
    def serviceMatches = registrar.lookup(serviceTemplate, Integer.MAX_VALUE)
    println "Discovered ${serviceMatches.items.length} services\n"
    int i = 1
    serviceMatches.items.each { item ->
        String name
        item.attributeSets.each { entry ->
            if(entry.getClass().name=="net.jini.lookup.entry.ServiceInfo")
                name = entry.name
        }
        println "${i++} $name: ${item.service.getClass().name}"
        /*item.attributeSets.each { entry ->
            println "\t${entry}"
        }*/
    }
    if(serviceMatches.items.length>0)
        println ""
}


if (client || server) {
    if (options.u) {
        println "Ignoring unicast discovery request while processing multicast verification"
    }
    if (client) {
        multicastClient()
    } else {
        multicastServer()
    }
} else {
    discover(options.u)
}