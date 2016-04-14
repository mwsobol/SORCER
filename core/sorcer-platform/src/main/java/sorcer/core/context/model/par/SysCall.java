/*
 * Copyright 2016 the original author or authors.
 * Copyright 2016 SorcerSoft.org.
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

package sorcer.core.context.model.par;

import sorcer.core.context.ServiceContext;
import sorcer.core.invoker.CmdInvoker;
import sorcer.service.*;
import sorcer.util.exec.ExecUtils;

import java.io.Serializable;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Properties;

/**
 * Created by Mike Sobolewski on 4/14/16.
 */
public class SysCall extends Par<Context> implements Serializable {

    private CmdInvoker invoker;
    private List<String> inPaths;
    private List<String> outPaths;

    public SysCall(String parname) {
        super(parname);
    }

    public SysCall(String parname, Context context) throws ContextException {
        this(parname);
        String cmd = (String)context.getValue("cmd");
        String[] cmdarray = (String[])context.getValue("cmdarray");
        String scriptFilename = (String)context.getValue("filename");
        Context scope = (Context)context.getValue("scope");

        inPaths = ((ServiceContext)context).getInPaths();
        outPaths = ((ServiceContext)context).getOutPaths();

        CmdInvoker invoker = null;
        if (name != null)
            invoker = new CmdInvoker();
        else
            invoker = new CmdInvoker(name);

        if (scope != null) {
            invoker.setScope(scope);
        }

        if (cmd != null)
            invoker.setCmd(cmd);

        if (cmdarray != null)
            invoker.setCmdarray(cmdarray);
    }

    public Context getValue(Arg... args) throws RemoteException,
            InvocationException {
        Context out = invoker.getScope();
        if (out == null)
            out = new ServiceContext(name);

        try {
            ExecUtils.CmdResult result = (ExecUtils.CmdResult) invoker.invoke(args);
            // get from the result the volume of cylinder and assign to y parameter

            Properties props = new Properties();
            props.load(new StringReader(result.getOut()));
            out.putValue("exit/value", result.getExitValue());

            // copy requested outputs into the context
            if (outPaths != null) {
                for (String key : outPaths) {
                    if (props.containsKey(key)) {
                        out.putValue(key, props.getProperty(key));
                    }
                }
            } else {
                out.putValue("call/out", result.getOut());
            }
        } catch (Exception e) {
            throw new InvocationException(e);
        }

        return out;
    }

}
