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
package sorcer.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Step extends MultiFiSlot<String, Step> {

    private Map<String, Step> steps = new HashMap<>();

    public Object getValue(String path, Arg... args) throws ContextException {
        Object obj = steps.get(path);
        List<Step> stepPath = new ArrayList<>();
        String pn = null;
        while (obj instanceof Step) {
            stepPath.add((Step) obj);
            pn = ((Step) obj).getKey();
            obj = steps.get(pn);
        }
        //return the value of stepPath
        return obj;
    }

    private Step getStep(String name) {
        return steps.get(name);
    }

    private Step setStep(Step step) {
        return steps.put(step.getName(), step);
    }
}
