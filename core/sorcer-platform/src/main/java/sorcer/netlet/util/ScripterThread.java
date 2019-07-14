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
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.context.ServiceContext;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.Function;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.service.*;

import java.rmi.RemoteException;

import static sorcer.so.operator.eval;
import static sorcer.so.operator.exec;
import static sorcer.util.StringUtils.tName;

public class ScripterThread extends Thread {
    private String script;
    private Object result;
    private Object target = null;
    private boolean isExerted = true;
    final private GroovyShell gShell;
    private NetletClassLoader classLoader;
    private ServiceShell serviceShell;

    private final static Logger logger = LoggerFactory.getLogger(ScripterThread.class
            .getName());

    public ScripterThread(String script, NetletClassLoader classLoader) {
        this(script, classLoader, true);
    }

    public ScripterThread(String script, NetletClassLoader classLoader, boolean isExerted) {
        super(tName("Script"));
        this.classLoader = classLoader;
        this.isExerted = isExerted;

        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.setPluginFactory(new ShebangPreprocessorFactory());
        compilerConfig.addCompilationCustomizers(getImports());
        compilerConfig.addCompilationCustomizers(new ASTTransformationCustomizer(new GroovyCodebaseSupport(classLoader)));

        gShell = new GroovyShell(classLoader, new Binding(), compilerConfig);
        this.script = script;
    }

    public void evalScript() {
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
        exert();
    }

    public void exert() {
        try {
            if (target == null) {
                evalScript();
            }

            if (target instanceof ContextDomain &&  !isExerted) {
                result = ((ServiceContext)target).getResponse();
                logger.info(">>>>>>>>>>> model eval result: " + result);
            } else if (target instanceof Mogram) {
                serviceShell = new ServiceShell((Mogram) target);
                if (target instanceof Mogram && isExerted) {
                    result = serviceShell.exert();
                    logger.info(">>>>>>>>>>> serviceShell exerted result: " + result);
                } else{
                    result = serviceShell.evaluate();
                    logger.info(">>>>>>>>>>> serviceShell eval result: " + result);
                }
            } else if (target instanceof Function){
                result = new Function(((Function)target).getName(), exec((Entry<? extends Object>) target));
                logger.info(">>>>>>>>>>> eval entry: " + result);
            } else if (target != null) {
                logger.info(">>>>>>>>>>> eval result: " + target);
            }
        } catch (CompilationFailedException | RemoteException | MogramException e) {
            e.printStackTrace();
        }
    }

    public Object getResult() {
        return result;
    }

    public Object getTarget() {
        return target;
    }

    public ServiceShell getServiceShell() {
        return serviceShell;
    }

    public boolean isExerted() {
        return isExerted;
    }

    private static String[] imports = {
            "sorcer.netlet.annotation",
            "sorcer.service",
            "sorcer.core.exertion",
            "sorcer.service.modeling",
            "sorcer.service",
            "sorcer.core.provider",
            "sorcer.core.provider.rendezvous",
            "sorcer.core.context.model",
            "sorcer.util",
            "java.io"
    };

    private static String[] staticImports = {
            "sorcer.service.Strategy",
            "sorcer.service.Deployment",
            "sorcer.eo.operator",
            "sorcer.co.operator",
            "sorcer.ent.operator",
            "sorcer.so.operator",
            "sorcer.mo.operator",
            "sorcer.util.exec.ExecUtils",
            "sorcer.util.StringUtils"
    };

    private static String[] modelingImports = {
            "sorcer.netlet.annotation",
            "sorcer.service",
            "sorcer.core.exertion",
            "sorcer.service.modeling",
            "sorcer.service",
            "sorcer.core.provider",
            "sorcer.core.provider.rendezvous",
            "sorcer.core.context.model",
            "sorcer.util",
            "java.io",
            // var-oriented modeling
            "sorcer.modeling.core.context.model.var",
            "sorcer.modeling.vfe",
            "sorcer.modeling.vfe.evaluator",
            "sorcer.modeling.vfe.filter",
            "sorcer.modeling.vfe.persist",
            "sorcer.modeling.vfe.util"
    };

    private static String[] modelingStaticImports = {
            "sorcer.service.Strategy",
            "sorcer.service.Deployment",
            "sorcer.co.operator",
            "sorcer.ent.operator",
            "sorcer.mo.operator",
            "sorcer.so.operator",
            "sorcer.util.exec.ExecUtils",
            "sorcer.util.StringUtils",
            // var-oriented modeling
            "sorcer.modeling.vo.operator"
    };

    private ImportCustomizer getImports() {
        ImportCustomizer result = new ImportCustomizer();
        try {
            if (Class.forName("sorcer.modeling.vfe.Var") != null) {
                result.addStarImports(modelingImports);
                result.addStaticStars(modelingStaticImports);
            }
        } catch (ClassNotFoundException e) {
            result.addStarImports(imports);
            result.addStaticStars(staticImports);
        }
        return result;
    }

}
