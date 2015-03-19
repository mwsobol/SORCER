/*
 * Copyright 2015 the original author or authors.
 * Copyright 2015 SorcerSoft.org.
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
import sorcer.core.Dispatcher;
import sorcer.core.provider.Provider;
import sorcer.core.provider.ServiceProvider;
import sorcer.service.Block;
import sorcer.service.ContextException;
import sorcer.service.Exec;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by sobol on 1/29/15.
 */
public class ModelThread {

    private final static Logger logger = Logger.getLogger(BlockThread.class
            .getName());

    private static final int SLEEP_TIME = 250;
    // doBlock method calls internally
    private Block block;

    private Block result;

    Provider provider;

    public ModelThread(Block block, Provider provider) {
        this.block = block;
        this.provider = provider;
    }

    public void run() {
        logger.finer("*** Exertion dispatcher started with control context ***\n"
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
                    logger.log(Level.WARNING, "Unable to read property from configuration", e1);
                }
            }
            if (exertionDeploymentConfig != null)
                dispatcher = ExertDispatcherFactory.getFactory().createDispatcher(block, provider, exertionDeploymentConfig);
            else
                dispatcher = ExertDispatcherFactory.getFactory().createDispatcher(block, provider);

            try {
                block.getControlContext().appendTrace(provider.getProviderName() +
                        " dispatcher: " + dispatcher.getClass().getName());
            } catch (RemoteException e) {
                // ignore it, locall call
            }
            int COUNT = 1000;
            int count = COUNT;
            while (dispatcher.getState() != Exec.DONE
                    && dispatcher.getState() != Exec.FAILED
                    && dispatcher.getState() != Exec.SUSPENDED) {
                count--;
                if (count < 0) {
                    logger.finer("*** Concatenator's Exertion Dispatcher waiting in state: "
                            + dispatcher.getState());
                    count = COUNT;
                }
                Thread.sleep(SLEEP_TIME);
            }
            logger.finer("*** Dispatcher exit state = " + dispatcher.getClass().getName()  + " state: " + dispatcher.getState()
                    + " for block***\n" + block.getControlContext());
        } catch (DispatcherException de) {
            de.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        result = (Block) dispatcher.getExertion();
    }

    public Block getBlock() {
        return block;
    }

    public Block getResult() throws ContextException {
        return result;
    }
}
