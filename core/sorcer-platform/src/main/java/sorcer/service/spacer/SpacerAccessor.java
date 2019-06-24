/**
 *
 * Copyright 2013 Rafał Krupiński.
 * Copyright 2013 Sorcersoft.com S.A.
 * Copyright 2015 Dennis Reedy.
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
package sorcer.service.spacer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerConstants;
import sorcer.service.Exerter;
import sorcer.core.provider.Spacer;
import sorcer.service.Accessor;
import sorcer.util.AccessorException;
import sorcer.util.Sorcer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rafał Krupiński
 */
public class SpacerAccessor {
    private static final Logger log = LoggerFactory.getLogger(SpacerAccessor.class);
    private static SpacerAccessor instance = new SpacerAccessor();
    private final Map<String, Spacer> cache = new HashMap<>();

    /**
     * Returns any SORCER Spacer service provider.
     *
     * @return a SORCER Spacer provider
     * @throws sorcer.util.AccessorException
     */
    public static Spacer getSpacer() throws AccessorException {
        return getSpacer(null);
    }

    /**
     * Returns a SORCER Spacer service provider using Jini lookup and discovery.
     *
     * @param name the key of a spacer service provider
     * @return a Spacer proxy
     */
    public static Spacer getSpacer(String name) {
        return instance.doGetSpacer(name);
    }

    public Spacer doGetSpacer(String name) {
        String spacerName = (name == null) ? Sorcer.getProperty(SorcerConstants.S_SPACER_NAME): name;
        Spacer spacer = cache.get(Spacer.class.getName());
        try {
            if (Accessor.isAlive((Exerter) spacer)) {
                log.info(">>>returned cached Spacer ({}) by {}",
                ((Exerter) spacer).getProviderID(), Accessor.get().getClass().getName());
            } else {
                spacer = Accessor.get().getService(spacerName, Spacer.class);
                if (spacer!=null)
                    cache.put(Spacer.class.getName(), spacer);
            }
            return spacer;
        } catch (Exception e) {
            log.error("getSpacer", e);
            return null;
        }
    }
}
