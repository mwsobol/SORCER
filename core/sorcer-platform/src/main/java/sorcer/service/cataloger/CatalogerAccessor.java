package sorcer.service.cataloger;
/**
 *
 * Copyright 2013 Rafał Krupiński.
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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.Cataloger;
import sorcer.core.provider.Provider;
import sorcer.service.Accessor;
import sorcer.util.AccessorException;
import sorcer.util.ServiceAccessor;

/**
 * @author Rafał Krupiński
 */
public class CatalogerAccessor extends ServiceAccessor {
    private static final Logger log = LoggerFactory.getLogger(CatalogerAccessor.class);

    protected static CatalogerAccessor instance = new CatalogerAccessor();

    /**
     * Returns any SORCER Cataloger service provider.
     *
     * @return a SORCER Cataloger provider
     * @throws sorcer.util.AccessorException
     */
    public static Cataloger getCataloger() throws AccessorException {
        return instance.doGetCataloger();
    }

    public Cataloger doGetCataloger() throws AccessorException {
        return doGetCataloger(providerNameUtil.getName(Cataloger.class));
    }

    /**
     * Returns a SORCER Cataloger service provider using Jini lookup and discovery.
     *
     * @param name
     *            the name of a Cataloger service provider
     * @return a Cataloger proxy
     */
    public static Cataloger getCataloger(String name) {
        return instance.doGetCataloger(name);
    }

    public Cataloger doGetCataloger(String name) {
        String CatalogerName = (name == null) ? providerNameUtil.getName(Cataloger.class)
                : name;
        Cataloger Cataloger = (Cataloger) cache.get(Cataloger.class.getName());

        try {
            if (Accessor.isAlive((Provider) Cataloger)) {
                log.info(">>>returned cached Cataloger ("
                        + ((Provider) Cataloger).getProviderID() + ") by "
                        + Accessor.getAccessorType());
            } else {
                Cataloger = Accessor.getService(CatalogerName, Cataloger.class);
                if (Cataloger!=null)
                    cache.put(Cataloger.class.getName(), Cataloger);
            }
            return Cataloger;
        } catch (Exception e) {
            log.error("getCataloger", e);
            return null;
        }
    }
}
