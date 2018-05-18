/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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

package sorcer.co.tuple;

import sorcer.core.context.model.ent.Subroutine;
import sorcer.service.Conditional;
import sorcer.service.Path;
import sorcer.service.Signature;

import java.util.List;

/**
 * Created by Mike Sobolewski on 6/21/15.
 */
public class ExecDependency extends Subroutine<List<Path>> {
    private static final long serialVersionUID = 1L;

    private Conditional condition;

    private Signature.Paths dependees;

    @SuppressWarnings("unchecked")
    public ExecDependency(String path, List<Path> paths) {
        key = path;
        name = path;
        impl = paths;
    }

    public ExecDependency(List<Path> paths) {
        this("_init_", paths);
    }

    public ExecDependency(String path, Conditional condition, List<Path> paths) {
        this(path, paths);
        this.condition = condition;
    }

    public List<Path> getDependees() {
        return dependees;
    }

    public void setDependees(Signature.Paths dependees) {
        this.dependees = dependees;
    }

    public Conditional getCondition() {
        return condition;
    }

    public void setCondition(Conditional condition) {
        this.condition = condition;
    }

}
