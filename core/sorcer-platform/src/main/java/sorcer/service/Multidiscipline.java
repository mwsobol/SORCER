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

import net.jini.id.Uuid;
import sorcer.service.modeling.Discipline;
import sorcer.service.modeling.Exploration;
import sorcer.service.modeling.Transdiscipline;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static sorcer.ent.operator.ent;

public class Multidiscipline extends ServiceDiscipline implements Transdiscipline {

    protected Map<String, Discipline> disciplines = new HashMap<>();

    // active disciplines
    protected Paths disciplinePaths;

    protected DisciplineExplorer explorer;

    public Multidiscipline(Discipline... disciplines) {
        for (Discipline disc : disciplines) {
            this.disciplines.put(disc.getName(), disc);
            ((ServiceDiscipline)disc).setParent(this);
        }
    }

    @Override
    public Discipline getDiscipline(String name) {
        return disciplines.get(name);
    }

    public Map<String, Discipline> getDisciplines() {
        return disciplines;
    }

    public Exploration getExplorer() {
        return explorer;
    }

    public void setExplorer(Exploration explorer) {
        this.explorer = (DisciplineExplorer) explorer;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public void addDomains(List<Discipline> disciplnes) {
        for (Discipline d : disciplnes) {
            this.disciplines.put(d.getName(), d);
            ((ServiceDiscipline)d).setParent(this);
        }
    }

    @Override
    public Context evaluate(Context context, Arg... args) throws EvaluationException, RemoteException {
        return null;
    }
}