/*
 * Copyright 2016 the original author or authors.
 * Copyright 2016 SorcerSoft.org.
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

package sorcer.core.signature;

import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.exertion.ObjectTask;
import sorcer.core.invoker.MethodInvoker;
import sorcer.core.provider.exerter.ServiceShell;
import sorcer.service.*;
import sorcer.service.modeling.Model;
import sorcer.service.modeling.Modeling;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;

import static sorcer.eo.operator.*;

public class SigDeployer implements Deployee {

	static final long serialVersionUID = 1L;

    private Signature builder;

    private Mogram deployee;

    public SigDeployer(Signature signature) {
        builder = signature;
    }

    public SigDeployer(Mogram mogram, Signature signature) {
        deployee = mogram;
        builder = signature;
    }

    public Signature getBuilder() {
        return builder;
    }

    public void setBuilder(Signature builder) {
        this.builder = builder;
    }

    @Override
    public String getName() {
        return builder.getName();
    }

    public Mogram getDeployee() {
        return deployee;
    }

    public void setDeployee(Mogram deployee) {
        this.deployee = deployee;
    }

    @Override
    public void deploy() throws MogramException {
        if (deployee != null && builder != null) {
            if (deployee instanceof Model) {
                deployee.deploy(builder);
            }
        } else {
            throw new ModelException("Invalid SigDeployer: " + builder);
        }
    }

}
