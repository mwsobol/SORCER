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

import groovy.lang.GroovyShell;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import net.jini.core.transaction.TransactionException;
import sorcer.core.deploy.ServiceDeployment;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.rmi.RemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static sorcer.util.StringUtils.tName;

public class ScriptThread extends Thread {
		private String script;
		private File scriptFile;
		private Object result;
		private Object target = null;
		final private GroovyShell gShell;
        private Configuration config;
        private boolean debug = false;

        private final static Logger logger = LoggerFactory.getLogger(ScriptThread.class
                .getName());

        public ScriptThread(String script, ClassLoader classLoader, PrintStream out, Configuration config, boolean debug) {
            super(tName("Script"));
            this.config = config;
            this.debug = debug;
            gShell = new GroovyShell(classLoader);
			this.script = script;
            this.parseScript();
		}

        public ScriptThread(String script, ClassLoader classLoader, PrintStream out, Configuration config) {
            this(script, classLoader, out, config, false);
        }


        public ScriptThread(String script, ClassLoader classLoader, PrintStream out) {
            this(script, classLoader, out, EmptyConfiguration.INSTANCE);
        }

        public ScriptThread(String script, PrintStream out) {
            this(script, null, out);
        }

        public ScriptThread(String script) {
            this(script, null, null);
        }

        public ScriptThread(String script, ClassLoader classLoader) {
            super(tName("Script"));
            this.gShell = new GroovyShell(classLoader);
            this.script = script;
            this.parseScript();
        }

		public ScriptThread(File file, ClassLoader classLoader) {
            super(tName("Script-" + file.getPath()));
            this.gShell = new GroovyShell(classLoader);
			this.scriptFile = file;
            this.parseScript();
		}

        public void parseScript() {
            synchronized (gShell) {
                if (script != null) {
                    target = gShell.evaluate(script);
                }  else {
                    try {
                        target = gShell.evaluate(scriptFile);
                    } catch (IOException e) {
                        logger.error("Problem evaluating Script file: " + scriptFile + ": " + e.getMessage());
                   }
                }
            }
        }

		public void run() {
            if (target==null) parseScript();
            if (target instanceof Exertion) {
                ServiceShell esh = new ServiceShell((Exertion) target);
                try {

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
                        result = esh.exert();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (TransactionException e) {
                    e.printStackTrace();
                } catch (ExertionException e) {
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

        public String printUrls(URL[] urls) {
            StringBuilder sb = new StringBuilder("URLs: [");
            for (URL url : urls) {
                sb.append("\n").append(url.toString());
            }
            sb.append(" ]");
            return sb.toString();
        }

	}
