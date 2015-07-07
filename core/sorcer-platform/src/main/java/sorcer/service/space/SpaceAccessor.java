package sorcer.service.space;
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


import net.jini.core.entry.Entry;
import net.jini.lookup.entry.Name;
import net.jini.space.JavaSpace;
import net.jini.space.JavaSpace05;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.service.Accessor;
import sorcer.util.ProviderNameUtil;
import sorcer.util.Sorcer;
import sorcer.util.SorcerProviderNameUtil;


/**
 * @author Rafał Krupiński
 */
public class SpaceAccessor {
    private static final Logger log = LoggerFactory.getLogger(SpaceAccessor.class);

    protected static SpaceAccessor instance = new SpaceAccessor();
    private JavaSpace05 cache;
    private ProviderNameUtil providerNameUtil = new SorcerProviderNameUtil();

    /**
     * Returns a JavaSpace service with a given name.
     *
     * @return JavaSpace proxy
     */
    public static JavaSpace05 getSpace(String spaceName) {
        return getSpace(spaceName, Sorcer.getSpaceGroup());
    }

    /**
     * Returns a JavaSpace service with a given name and group.
     *
     * @return JavaSpace proxy
     */
    public static JavaSpace05 getSpace(String spaceName, String spaceGroup) {
        return instance.doGetSpace(spaceName, spaceGroup);
    }

    public JavaSpace05 doGetSpace(String spaceName, String spaceGroup) {
        // first test if our cached JavaSpace is alive
        // and if it's the case then return it,
        // otherwise get a new JavSpace proxy
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

        Entry[] attrs = null;
        if (spaceName != null) {
            attrs = new Entry[]{new Name(spaceName)};
        }
        String sg = spaceGroup;
        if (spaceGroup == null) {
            sg = Sorcer.getSpaceGroup();
        }
        javaSpace = (JavaSpace05) Accessor.getService(null,
                                                      new Class[]{JavaSpace05.class},
                                                      attrs,
                                                      new String[]{sg});
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
        return doGetSpace(providerNameUtil.getName(JavaSpace05.class), Sorcer.getSpaceGroup());
    }
}
