/*
 * Copyright 2018 the original author or authors.
 * Copyright 2018 SorcerSoft.org.
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

package sorcer.service;

import sorcer.co.tuple.ExecDependency;
import sorcer.core.Dispatcher;
import sorcer.core.context.ModelStrategy;
import sorcer.core.context.model.ent.Entry;
import sorcer.core.context.model.ent.Subroutine;
import sorcer.service.modeling.Discipline;
import sorcer.service.modeling.Functionality;
import sorcer.service.modeling.Multidiscipline;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import static sorcer.ent.operator.ent;

public class Transdiscipline extends ServiceDiscipline implements Multidiscipline {

    protected String name;

    protected Map<String, Discipline> disciplines;

    // the default exec order of active disciplines
    protected Paths disciplinePaths;

    protected Dispatcher dispatcher;

    @Override
    public Discipline getDiscipline(String name) {
        return disciplines.get(name);
    }

    @Override
    public Object getId() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

}
