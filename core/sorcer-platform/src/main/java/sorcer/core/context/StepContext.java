/*
 * Copyright 2019 the original author or authors.
 * Copyright 2019 SorcerSoft.org.
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

/**
 * @author Mike Sobolewski
 * 05.08.2019
 */
package sorcer.core.context;

import sorcer.service.*;

import java.rmi.RemoteException;

public class StepContext extends ServiceContext<Object> {

    @Override
    public Object getValue(String path, Arg... args) throws ContextException {
        Object obj = get(path);
        if (obj instanceof Step && ((Step)obj).getKey().equals(path)) {
            return (((Step)obj).getValue(path, args));
        } else if (obj instanceof MultiFiSlot && ((MultiFiSlot)obj).getKey().equals(path)) {
            return ((MultiFiSlot) obj).getData(args);
        } else {
            return super.getValue(path, args);
        }
    }

    @Override
    public Object putValue(String key, Object value) throws ContextException {
        if (value == null) {
            return data.put(key, none);
        } else if (value instanceof Step) {
            return data.put(key, value);
        } else {
            return super.putValue(key, value);
        }
    }

    @Override
    public Object put(String key, Object value) {
        if (value == null) {
            return data.put(key, none);
        } else {
            return data.put(key, value);
        }
    }

    @Override
    public Object get(String key) {
        return data.get(key);
    }
}
