import org.rioproject.config.Component
import sorcer.util.Sorcer
import org.rioproject.opstring.ClassBundle
import org.rioproject.opstring.ServiceBeanConfig
import org.rioproject.opstring.ServiceElement

import sorcer.util.Sorcer

@Component('sorcer.core.exertion.deployment')
class ExertionDeploymentConfig {
	
	def appendJars(def dlJars) {
		def commonDLJars = ["sorcer-prv-dl.jar", "jsk-dl.jar", "rio-api.jar", "serviceui.jar", "jmx-lookup.jar"]
		dlJars.addAll(commonDLJars)
		return dlJars as String[]
	}

	def getService(String interfaceName) {
		ServiceElement service = new ServiceElement()
		String websterPort = Sorcer.getWebsterPort();
		
		/* Create client (export) ClassBundle */
		ClassBundle export = new ClassBundle(interfaceName)
		export.setJARs(appendJars(["arithmetic-dl.jar", "provider-ui.jar", "exertlet-ui.jar"]))
		String host = InetAddress.getLocalHost().getHostName()
		//export.setCodebase("http://${host}:9010")
		export.setCodebase("http://${host}:" + websterPort);
		
		/* Create service implementation ClassBundle */
		ClassBundle main = new ClassBundle("sorcer.core.provider.ServiceExerter");
		main.setJARs("arithmetic-beans.jar", "sorcer-prv.jar", "sorcer-modeling-lib.jar")
		//main.setCodebase("http://${host}:9010")
		export.setCodebase("http://${host}:" + websterPort);
		
		/* Set ClassBundles to ServiceElement */
		service.setComponentBundle(main)
		service.setExportBundles(export)

		/* Get the (simple) name from the fully qualified interface */
		String name = interfaceName
		int ndx = interfaceName.lastIndexOf(".")
		if (ndx > 0)
			name = interfaceName.substring(ndx + 1)
			
		/* Create simple ServiceBeanConfig */ 
		Map<String, Object> configMap = new HashMap<String, Object>()
		configMap.put(ServiceBeanConfig.NAME, name)
		configMap.put(ServiceBeanConfig.GROUPS, Sorcer.getLookupGroups())
		String sorcerHome = Sorcer.getHomeDir().path
		String configFile = "file:${sorcerHome}/modules/examples/ex6/configs/${name.toLowerCase()}-prv.config"
		ServiceBeanConfig sbc = new ServiceBeanConfig(configMap, configFile)		
		service.setServiceBeanConfig(sbc)

		/* We only need 1 of them */
		service.planned = 1
		return service;
	}

}