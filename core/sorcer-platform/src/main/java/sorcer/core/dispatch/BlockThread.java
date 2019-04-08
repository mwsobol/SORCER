/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

import net.jini.config.ConfigurationException;
import sorcer.core.DispatchResult;
import sorcer.core.Dispatcher;
import sorcer.core.provider.Provider;
import sorcer.core.provider.ServiceProvider;
import sorcer.service.Arg;
import sorcer.service.Block;
import sorcer.service.ContextException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockThread extends Thread {
	private final static Logger logger = LoggerFactory.getLogger(BlockThread.class
			.getName());

	private static final int SLEEP_TIME = 250;
	// doBlock method calls internally
	private Block block;
	private Arg[] args;
	private Block result;
	Provider provider;

	public BlockThread(Block block, Provider provider, Arg... args) {
		this.args = args;
		this.block = block;
		this.provider = provider;
	}

	public void run() {
		logger.debug("*** Exertion explorer started with control context ***\n"
				+ block.getControlContext());
		Dispatcher dispatcher = null;
		try {
			String exertionDeploymentConfig = null;
			if (block.isProvisionable()) {
				try {
					exertionDeploymentConfig =
							(String)((ServiceProvider)provider).getProviderConfiguration().getEntry("sorcer.core.provider.ServiceProvider",
									"exertionDeploymentConfig",
									String.class,
									null);
				} catch (ConfigurationException e1) {
					logger.warn("Unable to read property from configuration", e1);
				}
			}
			if (exertionDeploymentConfig != null)
				dispatcher = MogramDispatcherFactory.getFactory().createDispatcher(block, provider, exertionDeploymentConfig);
			else
				dispatcher = MogramDispatcherFactory.getFactory().createDispatcher(block, provider);

            dispatcher.exec(args);
            DispatchResult result = dispatcher.getResult();

			/*int COUNT = 1000;
			int count = COUNT;
			while (explorer.getState() != Exec.DONE
					&& explorer.getState() != Exec.FAILED
					&& explorer.getState() != Exec.SUSPENDED) {
				count--;
				if (count < 0) {
					logger.debug("*** Concatenator's Exertion Dispatcher waiting in state: "
							+ explorer.getState());
					count = COUNT;
				}
				Thread.sleep(SLEEP_TIME);
			} */

			logger.debug("*** Dispatcher exit state = " + dispatcher.getClass().getName()  + " state: " + result.state
					+ " for block***\n" + block.getControlContext());
            this.result = (Block) result.exertion;
        } catch (DispatcherException de) {
			de.printStackTrace();
		}
		//outDispatcher = (Block) explorer.getMogram();
	}

	public Block getBlock() {
		return block;
	}

	public Block getResult() throws ContextException {
		return result;
	}
}
