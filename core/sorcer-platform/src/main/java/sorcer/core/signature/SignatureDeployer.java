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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static sorcer.eo.operator.*;

public class SignatureDeployer implements Deployee {

	static final long serialVersionUID = 1L;

    private String name;

    private List<Signature> builders;

    private Mogram deployee;

    public SignatureDeployer(Signature... builders) {
        this.builders = Arrays.asList(builders);
    }

    public SignatureDeployer(Mogram mogram, Signature... builders) {
        deployee = mogram;
        this.builders = Arrays.asList(builders);
    }

    public List<Signature> getBuilders() {
        return builders;
    }

    public void setBuilders(List<Signature> builders) {
        this.builders = builders;
    }

    @Override
    public String getName() {
        return name;
    }

    public Mogram getDeployee() {
        return deployee;
    }

    public void setDeployee(Mogram deployee) {
        this.deployee = deployee;
    }

    @Override
    public void deploy() throws ConfigurationException {
        if (deployee != null && builders != null) {
            if (deployee instanceof Model) {
                try {
                    deployee.deploy(builders);
                } catch (MogramException e) {
                    throw new ConfigurationException(e);
                }
            }
        } else {
            throw new ConfigurationException("Invalid builders: " + builders);
        }
    }


}
