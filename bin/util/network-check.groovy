#!/usr/bin/env groovy
/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
File sorcerDist =  new File(scriptDir, "../../")
System.setProperty("java.security.policy", new File(sorcerDist, "policy/policy.all").absolutePath)
if(System.securityManager==null)
    System.securityManager = new SecurityManager()

if(!sorcerDist.exists()) {
    println "You must build the SORCER distribution first"
    System.exit(-1)
}
def props = new Properties()
new File(sorcerDist, "configs/versions.properties").withReader { reader ->
    props.load(reader)
}

File rioHome = new File(sorcerDist, "rio-${props['rio.version']}")

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
sorcerDist.eachFileRecurse (FileType.FILES) { file ->
    if(file.name.startsWith("sorcer-dl"))
        jars << file.toURI().toURL()
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
 "logback.configurationFile" : "${sorcerDist.path}/configs/sorcer-logging.groovy",
 "java.net.preferIPv4Stack"  : "true",
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