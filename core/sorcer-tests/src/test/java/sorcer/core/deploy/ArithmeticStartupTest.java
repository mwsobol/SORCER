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
package junit.sorcer.core.deploy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static sorcer.eo.operator.exert;
import static sorcer.eo.operator.get;

import java.io.IOException;
import java.net.URL;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import junit.framework.Assert;
import junit.sorcer.core.provider.Adder;
import junit.sorcer.core.provider.Subtractor;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import sorcer.core.SorcerConstants;
import sorcer.core.provider.Provider;
import sorcer.service.ContextException;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.ServiceExertion;
import sorcer.util.ProviderLookup;
import sorcer.util.Sorcer;
import sorcer.util.exec.ExecUtils;
import sorcer.util.exec.ExecUtils.CmdResult;
import sorcer.util.url.sos.SdbURLStreamHandlerFactory;

/**
 * @author Mike Sobolewski
 */
@Ignore
public class ArithmeticStartupTest implements SorcerConstants {
    private final static Logger logger = Logger.getLogger(iGridStartupTest.class.getName());
    static long t0;
    static {
		ServiceExertion.debug = true;
		System.setProperty("java.security.policy", Sorcer.getHome()+ "/configs/policy.all");
		System.setSecurityManager(new RMISecurityManager());
		Sorcer.setCodeBase(new String[] { "ju-arithmetic-beans.jar",  "sorcer-dl.jar" });
		try {
			URL.setURLStreamHandlerFactory(new SdbURLStreamHandlerFactory());
		} catch (Throwable t) {
			// ignore it is set already
			t.printStackTrace();
		}
	}
	
	//@BeforeClass
	public static void setUpOnce() throws IOException, InterruptedException {
		t0 = System.currentTimeMillis();
		CmdResult result = ExecUtils.execCommand("ant -f " + Sorcer.getHome() 
				+ "/modules/sorcer/src/junit/sorcer/core/provider/bin/all-arithmetic-prv-boot-spawn.xml");
//		logger.info("out: " + result.getOut());
//		logger.info("err: " + result.getErr());
//		logger.info("status: " + result.getExitValue());
	}
	
	//@AfterClass
	public static void cleanup() throws RemoteException, InterruptedException {
		Sorcer.destroyNode(null, Adder.class);
	}

    //@Test
    public void execArithmetic() throws Exception {    
    	Provider provider = (Provider) ProviderLookup.getService(Subtractor.class);
		Assert.assertNotNull(provider);
		logger.info("Waited " + (System.currentTimeMillis() - t0)+ " millis for artithmetic");

        Job f1 = Util.createJobNoDeployment();
        verifyExertion(f1);
        /* Run it again */
        verifyExertion(f1);        
    }

    private void verifyExertion(Job job) throws ExertionException, ContextException {
    	t0 = System.currentTimeMillis();
        Exertion out = exert(job);
        System.out.println("Waited "+(System.currentTimeMillis()-t0)+" millis for exerting: " + out.getName());
        assertNotNull(out);
//        logger.info("job f1 context: " + jobContext(out));
//        logger.info("job f1/f3/result/y3: " + get(out, "f1/f3/result/y3"));
        assertEquals(get(out, "f1/f3/result/y3"), 400.0);
    }

}
