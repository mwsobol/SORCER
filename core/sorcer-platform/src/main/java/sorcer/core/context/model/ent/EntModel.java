package sorcer.core.context.model.ent;

import sorcer.core.context.PositionalContext;
import sorcer.core.plexus.FidelityManager;
import sorcer.service.*;

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
public class EntModel<T> extends PositionalContext<T> implements Invocation<T>, Contexter<T> {

    public static EntModel instance(Signature builder) throws SignatureException {
        EntModel model = (EntModel) sorcer.co.operator.instance(builder);
        model.setBuilder(builder);
        return model;
    }

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

    public EntModel(String name, Signature builder) {
        super(name, builder);
        setModeling(true);
    }

    public EntModel(Context context) throws RemoteException, ContextException {
        super(context);
        name = "Ent Model";
        setSubject("ent/model", new Date());
        setModeling(true);
    }

    /* (non-Javadoc)
     * @see sorcer.service.Invocation#invoke(sorcer.service.Context, sorcer.service.Arg[])
     */
    @Override
    public T invoke(Context<T> context, Arg... entries) throws RemoteException,
            InvocationException {
        try {
            appendContext(context);
            return getValue(entries);
        } catch (Exception e) {
            throw new InvocationException(e);
        }
    }

//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder("Model Response\n");
//        try {
//            if (getResult() != null)
//                sb.append(getResult().toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        sb.append("\nEvaluated Model\n");
//        sb.append(super.toString());
//        return sb.toString();
//    }
}
