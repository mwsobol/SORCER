/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
 * Copyright 2013 Sorcersoft.com S.A.
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

package sorcer.service;

import net.jini.config.Configuration;
import net.jini.config.EmptyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.util.ProviderLocator;
import sorcer.util.ProviderLookup;
import sorcer.util.Sorcer;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A service accessing facility that allows to find dynamically a network
 * service provider matching its {@link Signature}. This class uses the Factory
 * Method pattern with the {@link DynamicAccessor} interface.
 *
 * @author Mike Sobolewski
 * @author Dennis Reedy
 */
public class Accessor {
    private static final AtomicReference<DynamicAccessor> accessor = new AtomicReference<>();
    final static Logger logger = LoggerFactory.getLogger(Accessor.class.getName());

    /**
     * Create an instance of a {@link DynamicAccessor} if one has not been created.
     *
     * @param config The Configuration object to use to create the DynamicAccessor. The sorcer.env provider.lookup.accessor
     *               configuration entry will be used to create the DynamicAccessor.
     * @return An instance of DynamicAccessor.
     *
     * @throws RuntimeException If there are issues using the configuration.
     * @throws IllegalArgumentException if the configuration arg is null.
     */
    public static synchronized DynamicAccessor create(Configuration config) {
        if(accessor.get() == null) {
            if(config == null)
                throw new IllegalArgumentException("A Configuration must be provided");
            if(System.getSecurityManager() == null)
                System.setSecurityManager(new SecurityManager());
            String providerType =
                Sorcer.getProperties().getProperty(SorcerConstants.S_SERVICE_ACCESSOR_PROVIDER_NAME);
            try {
                logger.debug("SORCER DynamicAccessor provider: {}", providerType);
                Class<?> type = Class.forName(providerType, true, Thread.currentThread().getContextClassLoader());
                if(!DynamicAccessor.class.isAssignableFrom(type)){
                    throw new IllegalArgumentException("Configured class must implement DynamicAccessor: "+providerType);
                }
                Constructor constructor = type.getDeclaredConstructor(Configuration.class);
                accessor.set((DynamicAccessor) constructor.newInstance(config));
            } catch (Exception e) {
                throw new RuntimeException("No service accessor available for: " + providerType,e);
            }
        }
        return accessor.get();
    }

    public static synchronized DynamicAccessor createLookup() {
        if(System.getSecurityManager() == null)
            System.setSecurityManager(new SecurityManager());
        if(accessor.get() == null) {
            accessor.set(new ProviderLookup());
        }
        return accessor.get();
    }

    /**
     * Get the created {@link DynamicAccessor} instance.
     *
     * @return An instance of DynamicAccessor.
     *
     * @throws IllegalStateException if the DynamicAccessor instance has not been created by this utility.
     */
    public static synchronized DynamicAccessor get() {
        if(accessor.get()==null)
            throw new IllegalStateException("The DynamicAccessor has not yet been created");
        return accessor.get();
    }


    /**
     * Create an instance of a {@link DynamicAccessor} if one has not been created.
     *
     * @return An instance of DynamicAccessor created with no configuration. This method should be used
     * with care, as underlying discovery management cannot be configured.
     */
    public static synchronized DynamicAccessor create() {
        if(accessor.get()==null) {
            try {
                create(EmptyConfiguration.INSTANCE);
            } catch (Exception e) {
                throw new RuntimeException("Unable to create a DynamicAccessor", e);
            }
        }
        return accessor.get();
    }

    /**
     * Test if provider is still replying.
     *
     * @param provider the provider to check
     * @return true if a provider is alive, otherwise false
     */
    public static boolean isAlive(Exerter provider) {
        if (provider == null)
            return false;
        try {
            provider.getProviderName();
            return true;
        } catch (Exception e) {
            logger.debug("ServiceExerter is dead " + e.getMessage());
            return false;
        }
    }

}
