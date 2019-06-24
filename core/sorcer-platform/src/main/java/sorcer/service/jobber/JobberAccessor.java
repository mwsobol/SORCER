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
package sorcer.service.jobber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.Jobber;
import sorcer.service.Exerter;
import sorcer.service.Accessor;
import sorcer.util.AccessorException;
import sorcer.util.ProviderNameUtil;
import sorcer.util.SorcerProviderNameUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rafał Krupiński
 */
public class JobberAccessor {
    private static final Logger log = LoggerFactory.getLogger(JobberAccessor.class);
    protected static JobberAccessor instance = new JobberAccessor();
    private final Map<String, Jobber> cache = new HashMap<>();
    private ProviderNameUtil providerNameUtil = new SorcerProviderNameUtil();

    /**
     * Returns any SORCER Jobber service provider.
     *
     * @return a SORCER Jobber provider
     * @throws sorcer.util.AccessorException
     */
    public static Jobber getJobber() throws AccessorException {
        return instance.doGetJobber();
    }

    public Jobber doGetJobber() throws AccessorException {
        return doGetJobber(providerNameUtil.getName(Jobber.class));
    }

    /**
     * Returns a SORCER Jobber service provider using Jini lookup and discovery.
     *
     * @param name
     *            the key of a Jobber service provider
     * @return a Jobber proxy
     */
    public static Jobber getJobber(String name) {
        return instance.doGetJobber(name);
    }

    public Jobber doGetJobber(String name) {
        String jobberName = (name == null) ? providerNameUtil.getName(Jobber.class) : name;
        Jobber jobber = cache.get(Jobber.class.getName());

        try {
            if (Accessor.isAlive((Exerter) jobber)) {
                log.info(">>>returned cached Jobber ({}) by {}",
                         ((Exerter) jobber).getProviderID(), Accessor.get().getClass().getName());
            } else {
                jobber = Accessor.get().getService(jobberName, Jobber.class);
                if (jobber!=null)
                    cache.put(Jobber.class.getName(), jobber);
            }
            return jobber;
        } catch (Exception e) {
            log.error("getJobber", e);
            return null;
        }
    }
}
