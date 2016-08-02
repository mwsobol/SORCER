/*
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

package sorcer.core.invoker;

import sorcer.service.EvaluationException;
import sorcer.service.Invocation;

/**
 * Created by Mike Sobolewski on 8/9/15.
 */
public class DoubleIncrementor extends IncrementInvoker<Double> {

    public DoubleIncrementor(String path) {
         super(path);
    }

    public DoubleIncrementor(String path, Double increment) {
        super(path);
        this.increment = increment;
    }

    public DoubleIncrementor(String path, Double increment, Double value) {
        super(path, increment, value);
    }
    public DoubleIncrementor(Invocation invoker, Double increment) {
        super(invoker, increment);
    }

    public void setIncrement(Double increment) {
        this.increment = increment;
    }

    @Override
    protected Double getIncrement(Double value, Double increment) throws EvaluationException {
        Double val = null;
        if (this.value == null)  {
            this.value = increment;
            return this.value;
        }
        return value + increment;
    }
}
