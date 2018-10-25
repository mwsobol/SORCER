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

import sorcer.service.modeling.Discipline;
import sorcer.service.modeling.Multidiscipline;

import java.util.Map;

public class Transdiscipline extends ServiceDiscipline implements Multidiscipline {

    protected Map<String, Discipline> disciplines;

    // naming in a dependency tree
    protected Paths disciplinePaths;

    @Override
    public Discipline getDiscipline(String name) {
        return disciplines.get(name);
    }
}
