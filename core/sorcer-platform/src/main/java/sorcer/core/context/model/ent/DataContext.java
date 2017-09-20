/*
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

package sorcer.core.context.model.ent;

import net.jini.id.UuidFactory;
import sorcer.core.context.ModelStrategy;
import sorcer.core.context.PositionalContext;
import sorcer.service.*;

import java.util.Date;

public class DataContext<T> extends PositionalContext<T> {

    public DataContext() {
        super();
        name = DATA_MODEL;
        setSubject("data/model", new Date());
        isRevaluable = false;
    }

    public DataContext(String name) {
        super();
        initContext();
        if (name == null || name.length() == 0) {
            this.name = defaultName + count++;
        } else {
            this.name = name;
        }
        mogramId = UuidFactory.generate();
        mogramStrategy = new ModelStrategy(this);
        creationDate = new Date();
    }

    public DataContext(String name, Signature builder) {
        this(name);
        this.builder = builder;
    }

    public DataContext(String subjectPath, Object subjectValue) {
        this(subjectPath);
        this.subjectPath = subjectPath;
        this.subjectValue = subjectValue;
    }

    public DataContext(String name, String subjectPath, Object subjectValue) {
        this(name);
        this.subjectPath = subjectPath;
        this.subjectValue = subjectValue;
    }

    public DataContext(Context<T> context) throws ContextException {
        super(context);
    }
    /**
     * Returns a execute of the object at the path
     * (evaluation or invocation on this object if needed).
     *
     * @param path
     *            the variable name
     * @return this model execute at the path
     * @throws ModelException
     */
    @Override
    public T getValue(String path, Arg... args) throws ContextException {
        return data.get(path);
    }

    @Override
    public T putValue(final String path, Object value) throws ContextException {
        if (this.containsPath(path)) {
            if (value == null) {
                return data.put(path, (T)none);
            } else {
                return data.put(path, (T) value);
            }
        }
        else {
            int index = tally++;
            mark(path, Context.INDEX + APS + index);
            return (T)putValueAt(path, value, index);
        }
    }

    @Override
    public void removePath(String path) {
        data.remove(path);
    }
}
