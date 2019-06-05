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
package sorcer.service.space;

import net.jini.lookup.entry.Name;
import net.jini.space.JavaSpace;
import net.jini.space.JavaSpace05;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.service.Accessor;
import sorcer.util.ProviderNameUtil;
import sorcer.util.SorcerProviderNameUtil;


/**
 * @author Rafał Krupiński
 */
public class SpaceAccessor {
    private static final Logger log = LoggerFactory.getLogger(SpaceAccessor.class);
    private static SpaceAccessor instance = new SpaceAccessor();
    private static JavaSpace05 cache;
    private ProviderNameUtil providerNameUtil = new SorcerProviderNameUtil();

    /**
     * Returns a JavaSpace service with a given key.
     *
     * @return JavaSpace proxy
     */
    public static JavaSpace05 getSpace(String spaceName) {
        return doGetSpace(spaceName);
    }

    public static JavaSpace05 doGetSpace(String spaceName) {
        // first test if our cached JavaSpace is alive
        // and if it's the case then return it,
        // otherwise getValue a new JavSpace proxy
        JavaSpace05 javaSpace = cache;
        if (javaSpace != null) {
            try {
                javaSpace.readIfExists(new Name("_SORCER_"), null, JavaSpace.NO_WAIT);
                return javaSpace;
            } catch (Exception e) {
                //log.error("error", e.getMessage());
                cache = null;
            }
        }
        javaSpace = Accessor.get().getService(spaceName, JavaSpace05.class);
        try {
            javaSpace.readIfExists(new Name("_SORCER_"), null, JavaSpace.NO_WAIT);
            cache = javaSpace;
            log.info("JavaSpace is back!");
            return javaSpace;
        } catch (Exception e) {
            log.error("Problem connecting to JavaSpace");
            cache = null;
            return null;
        }
    }

    /**
     * Returns a Jini JavaSpace service.
     *
     * @return Jini JavaSpace
     */
    public static JavaSpace05 getSpace() {
        return instance.doGetSpace();
    }

    public JavaSpace05 doGetSpace() {
        return doGetSpace(providerNameUtil.getName(JavaSpace05.class));
    }
}
