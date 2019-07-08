/*
 * Copyright 2019 the original author or authors.
 * Copyright 2019 SorcerSoft.org.
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

package sorcer.core.service;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.service.*;
import sorcer.service.modeling.Discipline;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class Governance implements Contextion, Collaboration, FederatedRequest {

	private static final long serialVersionUID = 1L;

	protected static Logger logger = LoggerFactory.getLogger(Governance.class.getName());

	private static int count = 0;

	protected Uuid id = UuidFactory.generate();

	protected  String name;

    // the input of this discipline
    protected Context input;

    // the output of this discipline
    protected Context output;

    protected Fi multiFi;

	protected Morpher morpher;

    // active Conntextions
    protected Paths contextionPaths;

    protected GovernanceExplorer explorer;

    protected Map<String, Mogram> mograms = new HashMap<>();

    protected Map<String, Contextion> contextions = new HashMap<>();

    public Governance() {
        this(null);
    }

    public Governance(String name) {
        if (name == null) {
            this.name = getClass().getSimpleName() + "-" + count++;
        } else {
            this.name = name;
        }
    }
    public Governance(String name, Contextion[] contextions) {
        this(name);
        for (Contextion cxt : contextions) {
            if (cxt instanceof Collaboration) {
                this.mograms.put(cxt.getName(), (Mogram) cxt);
            } else {
                this.contextions.put(cxt.getName(), cxt);
            }
        }
    }

    public Context getInput() {
        return input;
    }

    public void setInput(Context input) {
        this.input = input;
    }

    public Context getOutput() {
        return output;
    }

    public void setOutput(Context output) {
        this.output = output;
    }

    public Paths getContextionPaths() {
        return contextionPaths;
    }

    public void setContextionPaths(Paths contextionPaths) {
        this.contextionPaths = contextionPaths;
    }

    public Map<String, Contextion> getContextions() {
		return contextions;
	}

	public Contextion getContextion(String name) {
		return contextions.get(name);
	}

    public GovernanceExplorer getExplorer() {
        return explorer;
    }

    public void setExplorer(GovernanceExplorer explorer) {
        this.explorer = explorer;
    }

	public Discipline getDiscipline(String name) {
        Contextion cxt = contextions.get(name);
        if (cxt instanceof Discipline) {
            return (Discipline) cxt;
        } else {
            new ConfigurationException("no such discipline: " + name);
        }
        return null;
    }

	// default instance new Return(Context.RETURN);
	protected Context.Return contextReturn;

	@Override
	public Context evaluate(Context context, Arg... args) throws EvaluationException, RemoteException {
		return null;
	}

	@Override
	public Context getContext() throws ContextException {
		return null;
	}

	@Override
	public void setContext(Context input) throws ContextException {

	}

	@Override
	public Context appendContext(Context context) throws ContextException, RemoteException {
		return null;
	}

	@Override
	public Context getContext(Context contextTemplate) throws RemoteException, ContextException {
		return null;
	}

	@Override
	public Context appendContext(Context context, String path) throws ContextException, RemoteException {
		return null;
	}

	@Override
	public Context getContext(String path) throws ContextException, RemoteException {
		return null;
	}

	@Override
	public Context.Return getContextReturn() {
		return contextReturn;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Fi getMultiFi() {
		return multiFi;
	}

	@Override
	public Morpher getMorpher() {
		return morpher;
	}

	@Override
	public Object getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object execute(Arg... args) throws ServiceException, RemoteException {
		return null;
	}

    @Override
    public Map<String, Mogram> getChildren() {
        return mograms;
    }

    @Override
    public Mogram getChild(String name) {
        return mograms.get(name);
    }
}
