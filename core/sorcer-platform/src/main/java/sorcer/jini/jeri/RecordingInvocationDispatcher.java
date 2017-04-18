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
package sorcer.jini.jeri;

import net.jini.core.constraint.MethodConstraints;
import net.jini.jeri.BasicInvocationDispatcher;
import net.jini.jeri.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.analytics.AnalyticsRecorder;
import sorcer.core.monitoring.MonitorCheck;

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.server.ExportException;
import java.util.Collection;

/**
 * @author Dennis Reedy
 */
public class RecordingInvocationDispatcher extends BasicInvocationDispatcher {
    private Logger logger = LoggerFactory.getLogger(RecordingInvocationDispatcher.class);
    private AnalyticsRecorder recorder;

    public RecordingInvocationDispatcher(Collection methods,
                                         ServerCapabilities serverCapabilities,
                                         MethodConstraints serverConstraints,
                                         Class permissionClass,
                                         ClassLoader loader,
                                         AnalyticsRecorder recorder) throws ExportException {
        super(methods, serverCapabilities, serverConstraints, permissionClass, loader);
        this.recorder = recorder;
    }

    @Override
    protected Object invoke(Remote impl, Method method, Object[] args, Collection context) throws Throwable {
        boolean monitor = MonitorCheck.check(method);
        int id = 0;
        if(monitor)
            id = recorder.inprocess(method.getName());
        try {
            Object result = doInvoke(impl, method, args, context);
            if(monitor)
                recorder.completed(method.getName(), id);
            return result;
        } catch (Throwable t) {
            logger.error("Failed", t);
            if(monitor)
                recorder.failed(method.getName(), id);
            throw t;
        }
    }

    protected Object doInvoke(Remote impl,
                              Method method,
                              Object[] args,
                              Collection context) throws Throwable {
        return super.invoke(impl, method, args, context);
    }
}
