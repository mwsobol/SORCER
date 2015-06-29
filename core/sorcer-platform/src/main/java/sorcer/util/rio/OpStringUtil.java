package sorcer.util.rio;
/**
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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

import edu.emory.mathcs.util.classloader.URIClassLoader;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;

import java.net.*;
import java.util.Arrays;

/**
 * @author Rafał Krupiński
 */
public class OpStringUtil {
    public static Class loadServiceClass(ServiceElement serviceElement) throws ResolverException {
        return loadClass(serviceElement.getComponentBundle(), serviceElement);
    }

    public static Class loadClass(ClassBundle bundle, ServiceElement serviceElement) throws ResolverException {
        return loadClass(bundle, serviceElement, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Load a class from Rio ClassBundle
     *
     * @param bundle   The part of OpString describing a class and required jars
     * @return class referred by the class bundle
     * @throws ResolverException
     */
    public static Class loadClass(ClassBundle bundle, ServiceElement serviceElement, ClassLoader classLoader) throws ResolverException {
        String className = bundle.getClassName();
        try {
            return Class.forName(className, false, getClassLoader(serviceElement, bundle, classLoader));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(className, e);
        }
    }

    public static ClassLoader getClassLoader(ServiceElement serviceElement, ClassBundle classBundle, ClassLoader parentCL) throws ResolverException {
        URL[] urls = null;
        try {
            if (classBundle.getArtifact()!=null)
                urls = ResolverHelper.resolve(classBundle.getArtifact(), ResolverHelper.getResolver(), serviceElement.getRemoteRepositories());
            else {
                urls = classBundle.getJARs();
            }
            return new URLClassLoader(urls, parentCL);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(classBundle.toString(), e);
        }
    }
}
