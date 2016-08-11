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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Dennis Reedy
 */
public final class ClassPathAppender {
    private static final URLClassLoader urlClassLoader = getClassLoader();
    private static Method addURL;

    private ClassPathAppender() {
    }

    /**
     * Add all jar names found in the provided file. The file must contain args
     * of jar file names, each line a jar file.
     *
     * @param file The file containing jar names.
     *
     * @throws IOException
     */
    public static void addAll(final File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        try {
            String line = br.readLine().trim();
            while (line != null) {
                line = line.trim();
                URL jarURL;
                if(!line.startsWith("http")) {
                    File jar = new File(line);
                    jarURL = jar.toURI().toURL();
                } else {
                    jarURL = new URL(line);
                }
                add(jarURL);
                line = br.readLine();
            }
        } finally {
            br.close();
        }
    }

    public static void add(final URL url) {
        try {
            if(urlClassLoader==null)
                getClassLoader();
            if(addURL==null)
                addURL = getMethod();
            addURL.invoke(urlClassLoader, url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Method getMethod() throws NoSuchMethodException {
        Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        m.setAccessible(true);
        return m;
    }

    private static URLClassLoader getClassLoader() {
        URLClassLoader urlClassLoader;
        final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        if(systemClassLoader instanceof URLClassLoader){
            urlClassLoader = (URLClassLoader) systemClassLoader;
        } else{
            throw new IllegalStateException("Not a URLClassLoader: "+ systemClassLoader);
        }
        return urlClassLoader;
    }

    public static void main(String[] args) {
        if(args.length>0) {
            try {
                File file = new File(args[0]);
                addAll(file);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
