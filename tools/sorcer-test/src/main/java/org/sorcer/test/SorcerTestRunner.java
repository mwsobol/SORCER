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
package org.sorcer.test;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.service.Accessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.*;
import java.util.Map;
import java.util.Properties;

/**
 * @author Dennis Reedy
 */
public class SorcerTestRunner extends BlockJUnit4ClassRunner {
    static {
        Policy.setPolicy(new Policy() {
            public PermissionCollection getPermissions(CodeSource codesource) {
                Permissions perms = new Permissions();
                perms.add(new AllPermission());
                return (perms);
            }
            public void refresh() {
            }

        });
        if(System.getSecurityManager()==null)
            System.setSecurityManager(new SecurityManager());
    }
    private static final Logger logger = LoggerFactory.getLogger(SorcerTestRunner.class.getName());

    /**
     * Constructs a new <code>SorcerTestRunner</code>
     *
     * @param clazz the Class object corresponding to the test class to be run
     * @throws org.junit.runners.model.InitializationError If the class cannot be created
     */
    public SorcerTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        logger.debug("SorcerTestRunner constructor called with [" + clazz + "].");
    }

    /**
     * Override running a test to load the test's configuration.
     *
     * @param notifier The @{code RunNotifier}
     */
    @Override
    public void run(RunNotifier notifier) {
        if (System.getProperty("sorcer.home") == null) {
            ProjectContext projectContext = getTestClass().getJavaClass().getAnnotation(ProjectContext.class);
            if (projectContext != null) {
                String rootDir = System.getProperty("user.dir");
                File baseDir = new File(rootDir, projectContext.value());
                System.out.println("Base dir: " + baseDir.getPath());
                File testProperties = new File(baseDir, "build/configs/test.properties");
                System.out.println("Test config: " + testProperties.getPath());
                if (testProperties.exists()) {
                    Properties systemProperties = new Properties();
                    FileInputStream propertiesInput = null;
                    try {
                        propertiesInput = new FileInputStream(testProperties);
                        systemProperties.load(propertiesInput);
                        System.out.println("Loaded " + testProperties.getPath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (propertiesInput != null) {
                            try {
                                propertiesInput.close();
                            } catch (IOException e) {
                                //;
                            }
                        }
                    }

                    for (Map.Entry<Object, Object> entry : systemProperties.entrySet()) {
                        String key = entry.getKey().toString();
                        String value = entry.getValue().toString();
                        System.setProperty(key, value);
                        System.out.println(key + " " + value);
                    }

                    /* Add the test resources directory to the context classloader. I know, it's somewhat of a hack */
                    File testResources = new File(baseDir, "src/test/resources");
                    try {
                        Method addURL = getMethod();
                        URLClassLoader cl = (URLClassLoader) Thread.currentThread().getContextClassLoader();
                        addURL.invoke(cl, testResources.toURI().toURL());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    logger.warn("Unable to load " +testProperties.getPath() +
                                       " for "+getTestClass().getJavaClass().getName()+", test bootstrapping short-circuited");
                }
            } else {
                logger.warn("The "+getTestClass().getJavaClass().getName()+
                                   " does not declare a ProjectContext annotation," +
                                   " test bootstrapping short-circuited");
            }
        }

        /* Prime the Accessor, getValue it setup */
        Accessor.create();

        super.run(notifier);
    }

    private Method getMethod() throws NoSuchMethodException {
        Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        m.setAccessible(true);
        return m;
    }
}
