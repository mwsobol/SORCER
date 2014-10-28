/*
 * Copyright to the original author or authors.
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

package sorcer.util.exec;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * A utility that creates a {@link java.net.URLClassLoader} using {@code http} based {@link java.net.URL}s ,
 * and loads the provided class, and invokes the loaded class's {@code main()} method.
 *
 * @author Dennis Reedy
 */
public final class DelegateLauncher {
    public static final String CLASSPATH_PROPERTY="delegate.classpath";
    static final Logger logger = Logger.getLogger(DelegateLauncher.class.getName());

    private DelegateLauncher() {}

    private static String getJVMClassPath() {
        StringBuilder cp = new StringBuilder();
        StringTokenizer st = new StringTokenizer(System.getProperty("java.class.path"), File.pathSeparator);
        while(st.hasMoreTokens()) {
            String element = st.nextToken();
            cp.append("\t").append(element).append("\n");
        }
        return cp.toString();
    }

    public static void main(String[] args) throws Exception {
        String classPath = System.getProperty(CLASSPATH_PROPERTY);
        if(args.length==0)
            throw new IllegalArgumentException("Argument must be provided");
        String classToLoad = args[0];
        String[] providedArgs = new String[args.length-1];
        System.arraycopy(args, 1, providedArgs, 0, args.length - 1);
        StringBuilder argsDebug = new StringBuilder();
        for (String providedArg : providedArgs) {
            argsDebug.append("\t").append(providedArg).append("\n");
        }
        ClassLoader loader;
        if(classPath!=null) {
            List<URL> urls = new ArrayList<URL>();
            StringBuilder classPathDebug = new StringBuilder();
            StringTokenizer st = new StringTokenizer(classPath, " ");
            while(st.hasMoreTokens()) {
                String element = st.nextToken();
                classPathDebug.append("\t").append(element).append("\n");
                urls.add(new URL(element));
            }
            StringBuilder out = new StringBuilder();
            out.append("Creating\n\t").append(classToLoad);
            out.append("\n\nWith arguments\n").append(argsDebug.toString());
            out.append("\nWith system classpath\n").append(getJVMClassPath());
            out.append("\nUsing remote classpath\n").append(classPathDebug.toString());
            logger.info(out.toString());
            loader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
            Thread.currentThread().setContextClassLoader(loader);
        } else {
            StringBuilder out = new StringBuilder();
            out.append("Creating\n\t").append(classToLoad);
            out.append("\n\nWith arguments\n").append(argsDebug.toString());
            out.append("\nWith system classpath\n").append(getJVMClassPath());
            logger.info(out.toString());
            loader = DelegateLauncher.class.getClassLoader();
        }
        try {
            Class<?> delegate = loader.loadClass(classToLoad);
            Method main = delegate.getMethod("main",  String[].class);
            main.invoke(null, new Object[]{providedArgs});
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }
}
