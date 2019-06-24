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
package sorcer.service.cataloger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.Cataloger;
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
public class CatalogerAccessor  {
    private static final Logger log = LoggerFactory.getLogger(CatalogerAccessor.class);
    protected static CatalogerAccessor instance = new CatalogerAccessor();
    private final Map<String, Cataloger> cache = new HashMap<>();
    private ProviderNameUtil providerNameUtil = new SorcerProviderNameUtil();

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
     *            the key of a Cataloger service provider
     * @return a Cataloger proxy
     */
    public static Cataloger getCataloger(String name) {
        return instance.doGetCataloger(name);
    }

    public Cataloger doGetCataloger(String name) {
        String catalogerName = (name == null) ? providerNameUtil.getName(Cataloger.class) : name;
        Cataloger cataloger = cache.get(Cataloger.class.getName());
        try {
            if (Accessor.isAlive((Exerter) cataloger)) {
                log.info(">>>returned cached cataloger ({}) by {}",
                         ((Exerter) cataloger).getProviderID(), Accessor.get().getClass().getName());
            } else {
                cataloger = Accessor.get().getService(catalogerName, Cataloger.class);
                if (cataloger!=null)
                    cache.put(Cataloger.class.getName(), cataloger);
            }
            return cataloger;
        } catch (Exception e) {
            log.error("getCataloger", e);
            return null;
        }
    }
}
