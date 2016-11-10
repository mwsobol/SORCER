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
package sorcer.core.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.Provider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Dennis Reedy
 */
public class MethodInterceptor implements InvocationHandler {
    private final Object target;
    private AnalyticsRecorder recorder = new AnalyticsRecorder(null, null);
    private final Logger logger = LoggerFactory.getLogger(MethodInterceptor.class);


    public MethodInterceptor(Object target) {
        this.target = target;
    }

    public static Object getInstance(Object target) {
        List<Class<?>> interfaces = new ArrayList<>();
        for(Class<?> inf : target.getClass().getInterfaces()) {
            interfaces.add(inf);
        }
        if(interfaces.size()==0)
            return target;
        if(!interfaces.contains(Provider.class))
            interfaces.add(Provider.class);
        LoggerFactory.getLogger(MethodInterceptor.class).info("===> {}", target.getClass().getName(), interfaces);
        return Proxy.newProxyInstance(target.getClass().getClassLoader(),
                                      interfaces.toArray(new Class<?>[interfaces.size()]),
                                      new MethodInterceptor(target));
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        logger.info("===> {}.{}", target.getClass().getName(), method.getName());
        if(method.getName().equals("destroy")) {
            StringBuilder b = new StringBuilder();
            for(Map.Entry<String, MethodAnalytics> entry : recorder.getMethodAnalytics().entrySet()) {
                b.append(String.format("%-15s  %s\n", entry.getKey(), entry.getValue()));
            }
            logger.info(b.toString());
        }
        int id = recorder.inprocess(method.getName());
        try {
            Object result = method.invoke(target, args);
            recorder.completed(method.getName(), id);
            return result;
        } catch(Throwable t) {
            recorder.failed(method.getName(), id);
            throw t;
        }
    }
}
