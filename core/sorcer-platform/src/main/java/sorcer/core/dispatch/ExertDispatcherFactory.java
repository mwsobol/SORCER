/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.dispatch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import sorcer.core.Dispatcher;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.exertion.Jobs;
import sorcer.core.exertion.NetTask;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.core.provider.Cataloger;
import sorcer.core.provider.Provider;
import sorcer.core.signature.ServiceSignature;
import sorcer.service.Block;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.Job;
import sorcer.service.ServiceExertion;

/**
 * This class creates instances of appropriate subclasses of Dispatcher. The
 * appropriate subclass is determined by calling the ServiceJob object's
 */
public class ExertDispatcherFactory implements DispatcherFactory {

	private static ExertDispatcherFactory factory;
	public static Cataloger catalog; // The service catalog object
	private final static Logger logger = Logger.getLogger(ExertDispatcherFactory.class.getName());
	
	public static ExertDispatcherFactory getFactory() {
		if (factory == null)
			factory = new ExertDispatcherFactory();
		return factory;
	}

	public static ExertDispatcherFactory getProvisionableFactory() {
		if (factory == null)
			factory = new ExertDispatcherFactory();
		return factory;
	}
	
	/**
	 * Returns an instance of the appropriate subclass of Dispatcher as
	 * determined from information provided by the given Job instance.
	 * 
	 * @param job
	 *            The SORCER job that will be used to perform a collection of
	 *            components exertions
	 */
	public Dispatcher createDispatcher(Exertion exertion, Provider provider) throws DispatcherException {
		return createDispatcher(exertion, new HashSet<Context>(), false, null, provider);
	}
		
	public Dispatcher createDispatcher(Exertion exertion, 
			                           Set<Context> sharedContexts,
			                           boolean isSpawned, 
			                           Provider provider) throws DispatcherException {
		return createDispatcher(exertion, sharedContexts, isSpawned, null, provider);
	}

	public Dispatcher createDispatcher(Exertion exertion,  
			                           Set<Context> sharedContexts,
			                           boolean isSpawned, 
			                           LokiMemberUtil myMemberUtil, 
			                           Provider provider, 
			                           String... config) throws DispatcherException {
		Dispatcher dispatcher = null;
		ProvisionManager provisionManager = null;
		List<ServiceDeployment> deploymnets = ((ServiceExertion)exertion).getDeploymnets();
		
		
		if (deploymnets.size() > 0 && (((ServiceSignature) exertion.getProcessSignature()).isProvisionable() || exertion.isProvisionable()))
			
		provisionManager = new ProvisionManager(exertion);
		
		try {
			if (exertion instanceof NetTask) {
				logger.info("Running Space Task Dispatcher...");
				dispatcher =  new SpaceTaskDispatcher((NetTask)exertion,
						                                    sharedContexts, 
						                                    isSpawned, 
						                                    myMemberUtil,
						                                    provisionManager);

			} else if (Jobs.isCatalogBlock(exertion) && exertion instanceof Block) {
				logger.info("Running Catalog Block Dispatcher...");
				 dispatcher = new CatalogBlockDispatcher((Block)exertion,
						                                  sharedContexts, 
						                                  isSpawned, 
						                                  provider,
						                                  provisionManager);
			} else if (Jobs.isSpaceBlock(exertion) && exertion instanceof Block) {
				logger.info("Running Catalog Block Dispatcher...");
				dispatcher = new SpaceBlockDispatcher((Block)exertion,
						                                  sharedContexts, 
						                                  isSpawned, 
						                                  myMemberUtil, 
						                                  provider,
						                                  provisionManager);
			}
			if (dispatcher != null)
				return dispatcher;
				
			Job job = (Job)exertion;
			if (Jobs.isSpaceSingleton(job)) {
				logger.info("Running Space Sequential Dispatcher...");
				dispatcher = new SpaceSequentialDispatcher(job,
						                                   sharedContexts, 
						                                   isSpawned, 
						                                   myMemberUtil, 
						                                   provider,
						                                   provisionManager);
			} else if (Jobs.isSpaceParallel(job)) {
				logger.info("Running Space Parallel Dispatcher...");
				dispatcher = new SpaceParallelDispatcher(job,
						                                 sharedContexts, 
						                                 isSpawned, 
						                                 myMemberUtil, 
						                                 provider,
						                                 provisionManager);
			} else if (Jobs.isSpaceSequential(job)) {
				logger.info("Running Space Sequential Dispatcher ...");
				dispatcher = new SpaceSequentialDispatcher(job,
						                                   sharedContexts, 
						                                   isSpawned, 
						                                   myMemberUtil, 
						                                   provider,
						                                   provisionManager);
			} else if (Jobs.isCatalogSingleton(job)) {
				logger.info("Running Catalog Singleton Dispatcher...");
				dispatcher = new CatalogSingletonDispatcher(job,
						                                    sharedContexts, 
						                                    isSpawned, 
						                                    provider,
						                                    provisionManager);
			} else if (Jobs.isCatalogParallel(job)) {
				logger.info("Running Catalog Parallel Dispatcher...");
				dispatcher = new CatalogParallelDispatcher(job,
						                                   sharedContexts, 
						                                   isSpawned, 
						                                   provider,
						                                   provisionManager);
			} else if (Jobs.isCatalogSequential(job)) {
				logger.info("Running Catalog Sequential Dispatcher...");
				dispatcher = new CatalogSequentialDispatcher(job,
						                                     sharedContexts, 
						                                     isSpawned, 
						                                     provider,
						                                     provisionManager);
			} 
			logger.info("*** tally of used dispatchers: " + ExertDispatcher.getDispatchers().size());
		} catch (Throwable e) {
//			e.printStackTrace();
			throw new DispatcherException(
					"Failed to create the exertion dispatcher for job: "+ exertion.getName(), e);
		}		
		return dispatcher;
	}

	
	@Override
	public Dispatcher createDispatcher(Exertion exertion, Provider provider, String... config) throws DispatcherException {
		return createDispatcher(exertion, new HashSet<Context>(), false, null, provider, config);
	}
}
