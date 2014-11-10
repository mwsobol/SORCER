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
package sorcer.core.deploy;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import sorcer.core.provider.Exerter;
import sorcer.core.provider.Provider;
import sorcer.service.ServiceExertion;
import sorcer.util.ProviderLookup;
import sorcer.util.Sorcer;
import sorcer.util.exec.ExecUtils;
import sorcer.util.exec.ExecUtils.CmdResult;
import sorcer.util.url.sos.SdbURLStreamHandlerFactory;

/**
 * Base class for test cases that use iGrid
 *
 * @author Mike Sobolewski
 */
@Ignore
public class SorcerStartupTest {
	 
	private final static Logger logger = Logger.getLogger(SorcerStartupTest.class.getName());
	
	static long t0;
	 
	static {
		ServiceExertion.debug = true;
		System.setProperty("java.security.policy", Sorcer.getHome()
				+ "/configs/policy.all");
		System.setSecurityManager(new SecurityManager());
		Sorcer.setCodeBase(new String[] { "ju-arithmetic-beans.jar",  "sorcer-dl.jar" });
		System.out.println("CLASSPATH :" + System.getProperty("java.class.path"));
		
		try {
			URL.setURLStreamHandlerFactory(new SdbURLStreamHandlerFactory());
		} catch (Throwable t) {
			// ignore it is set already
			t.printStackTrace();
		}
		//System.setProperty("java.protocol.handler.pkgs", "sorcer.util.bdb.sos");
	}

	@BeforeClass 
	public static void setUpOnce() throws IOException, InterruptedException {
		t0 = System.currentTimeMillis();
		CmdResult result = ExecUtils.execCommand("ant -f " + Sorcer.getHome() 
				+ "/bin/iGrid-boot-http-spawn.xml");
//		logger.info("out: " + result.getOut());
//		logger.info("err: " + result.getErr());
//		logger.info("status: " + result.getExitValue());
	}
	
    @AfterClass
    public static void tearDown() throws Exception {
		Sorcer.destroyNode(null, Exerter.class);
    }
    
	@Test
	public void confirmLastService() throws Exception {
		Provider provider = (Provider) ProviderLookup.getService(Exerter.class);
		// Provider provider =
		// (Provider)ProviderAccessor.getProvider(Exerter.class);
		Assert.assertNotNull(provider);
		logger.info("Waited " + (System.currentTimeMillis() - t0)
				+ " millis for iGrid");
	}
}
