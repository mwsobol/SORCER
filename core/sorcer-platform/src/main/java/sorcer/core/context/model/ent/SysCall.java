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

package sorcer.core.context.model.ent;

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
public class SysCall extends Prc<Context> implements Serializable {

    private CmdInvoker invoker;
    private ServiceContext dataContext;
    private List<String> inPaths;
    private List<String> outPaths;

    public SysCall(String parname) {
        super(parname);
    }

    public SysCall(String parname, Context context) throws ContextException {
        this(parname);
        String cmd = null;
        try {
            cmd = (String)context.getValue("cmd");
            String[] cmdarray = (String[])context.getValue("cmdarray");
            String scriptFilename = (String)context.getValue("filename");
            Context scope = (Context)context.getValue("scope");

            dataContext = (ServiceContext) context;
            inPaths = ((ServiceContext)context).getInPaths();
            outPaths = ((ServiceContext)context).getOutPaths();

            if (name != null)
                invoker = new CmdInvoker();
            else
                invoker = new CmdInvoker(name);

            if (scope != null) {
                invoker.setInvokeContext(scope);
            }

            if (cmd != null)
                invoker.setCmd(cmd);

            if (cmdarray != null)
                invoker.setCmdarray(cmdarray);
        } catch (RemoteException e) {
            throw new ContextException(e);
        }

    }

    public Context evaluate(Arg... args) throws RemoteException,
            InvocationException {
        Context out = invoker.getInvokeContext();
        if (out == null)
            out = new ServiceContext(name);

        try {
            if (invoker.getCmd() != null && inPaths.size() > 0) {
                // add input arguments
                StringBuilder cmd = new StringBuilder(invoker.getCmd());
                Object val = null;
                for (String path : inPaths) {
                    val = invoker.getInvokeContext().getValue(path);
                    if (val == null || val == Context.none) {
                        cmd.append(" " + path);
                    } else {
                        cmd.append(" -" + path);
                        cmd.append("=" + val);
                    }
                }
                invoker.setCmd(cmd.toString());
            }

            ExecUtils.CmdResult result = (ExecUtils.CmdResult) invoker.evaluate(args);
            // getValue from the result the volume of cylinder and assign to y parameter

            Properties props = new Properties();
            props.load(new StringReader(result.getOut()));
            out.putValue("exit/eval", result.getExitValue());

            // copy requested outputs into the context
            if (outPaths != null) {
                for (String key : outPaths) {
                    if (props.containsKey(key)) {
                        out.putValue(key, getTypedValue(key, props.getProperty(key)));
                    }
                }
            } else {
                out.putValue("prc/out", result.getOut());
            }
        } catch (Exception e) {
            throw new InvocationException(e);
        }

        return out;
    }

    private Object getTypedValue(String path, String value) throws ContextException {
        Object obj = value;
        if (dataContext.isDouble(path)) {
            obj = new Double(value);
        } else if (dataContext.isInt(path)) {
            obj = new Integer(value);
        } else if (dataContext.isLong(path)) {
            obj = new Long(value);
        } else if (dataContext.isFloat(path)) {
            obj = new Float(value);
        } else if (dataContext.isShort(path)) {
            obj = new Short(value);
        } else if (dataContext.isByte(path)) {
            obj = new Byte(value);
        } else if (dataContext.isBoolean(path)) {
            obj = new Boolean(value);
        }
        return obj;
    }
}
