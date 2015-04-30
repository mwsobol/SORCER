/**
 *
 * Copyright 2013 the original author or authors.
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
package sorcer.netlet.util;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import net.jini.core.transaction.TransactionException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.modeling.Model;

import java.rmi.RemoteException;

import static sorcer.util.StringUtils.tName;

public class ScriptThread extends Thread {
		private String script;
		private Object result;
		private Object target = null;
		final private GroovyShell gShell;
        private NetletClassLoader classLoader;

        private final static Logger logger = LoggerFactory.getLogger(ScriptThread.class
                .getName());

        public ScriptThread(String script, NetletClassLoader classLoader) {
            super(tName("Script"));
            this.classLoader = classLoader;

            CompilerConfiguration compilerConfig = new CompilerConfiguration();
            compilerConfig.setPluginFactory(new ShebangPreprocessorFactory());
            compilerConfig.addCompilationCustomizers(getImports());
            compilerConfig.addCompilationCustomizers(new ASTTransformationCustomizer(new GroovyCodebaseSupport(classLoader)));

            gShell = new GroovyShell(classLoader, new Binding(), compilerConfig);
            this.script = script;
            this.parseScript();
        }

    public void parseScript() {
            ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                synchronized (gShell) {
                    target = gShell.evaluate(script);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(currentCL);
            }
        }

		public void run() {
            if (target==null) parseScript();
            if (target instanceof Exertion) {
                ServiceShell esh = new ServiceShell((Exertion) target);
                try {

/*
                    if (((Exertion) target).isProvisionable() && config!=null) {
                        String configFile;
                        try {
                            configFile = (String) config.getEntry(
                                            "sorcer.tools.shell.NetworkShell",
                                            "exertionDeploymentConfig", String.class,
                                            null);
                            if (configFile != null)
                                result = esh.exert(new ServiceDeployment(configFile));
                            else
                                result = esh.exert();
                        } catch (ConfigurationException e) {
                            result = esh.exert();
                        }
                    } else
*/
                        result = esh.exert();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (TransactionException e) {
                    e.printStackTrace();
                } catch (ExertionException e) {
                    e.printStackTrace();
                }
            } else if (target instanceof Model) {
                try {
                    result = ((Model)target).exert();
                } catch (TransactionException e) {
                    e.printStackTrace();
                } catch (ExertionException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
		}

		public Object getResult() {
			return result;
		}

		public Object getTarget() {
			return target;
		}

    private static String[] imports = {
            "sorcer.netlet.annotation",
            "sorcer.service",
            "sorcer.core.exertion",
            "sorcer.service",
            "sorcer.core.context.model",
            "java.io"
    };

    private static String[] staticImports = {
            "sorcer.service.Strategy",
            "sorcer.eo.operator",
            "sorcer.co.operator",
            "sorcer.po.operator"
    };

    private ImportCustomizer getImports() {
        ImportCustomizer result = new ImportCustomizer();
        result.addStarImports(imports);
        result.addStaticStars(staticImports);
        return result;
    }

}
