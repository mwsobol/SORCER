package sorcer.core.context.model.ent;

import sorcer.core.context.PositionalContext;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.modeling.Model;

import java.rmi.RemoteException;
import java.util.Date;

/*
 * Copyright 2013 the original author or authors.
 * Copyright 20 SorcerSoft.org.
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
 * The EntModel is an active shared service context as a map of entries (ents),
 * with unique name and its argument <name, argument> is the definition of a
 * independent and dependent arguments. Arguments that dependent on other
 * arguments are evaluations (of Evaluation type), so that, each time the evaluations is
 * executed, its arguments for that call can be assigned to the corresponding
 * parameters of evaluations.
 *
 * @author Mike Sobolewski
 */
public class EntModel<T> extends PositionalContext<T> implements Model {

    public EntModel() {
        super();
        name = "Ent Model";
        setSubject("ent/model", new Date());
        setModeling(true);
    }

    public EntModel(String name) {
        super(name);
        setModeling(true);
    }

    public EntModel(Context context) throws RemoteException, ContextException {
        super(context);
        name = "Ent Model";
        setSubject("ent/model", new Date());
        setModeling(true);
    }
}
