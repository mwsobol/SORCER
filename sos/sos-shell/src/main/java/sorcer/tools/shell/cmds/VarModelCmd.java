/*
 * Copyright 2011 the original author or authors.
 * Copyright 2011 SorcerSoft.org.
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

package sorcer.tools.shell.cmds;

import groovy.lang.GroovyShell;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.jini.core.lookup.ServiceItem;
import net.jini.id.Uuid;
import sorcer.jini.lookup.AttributesUtil;
import sorcer.modeling.core.context.model.var.ParametricModel;
import sorcer.modeling.service.ModelingAdmin;
import sorcer.modeling.service.OptimizationModelingAdmin;
import sorcer.modeling.service.ParametricModeling;
import sorcer.modeling.service.ParametricModelingAdmin;
import sorcer.service.ContextException;
import sorcer.service.EvaluationException;
import sorcer.service.MonitorException;
import sorcer.service.modeling.Modeling;
import sorcer.tools.shell.NetworkShell;
import sorcer.tools.shell.ShellCmd;
import sorcer.tools.shell.ShellStarter;
import sorcer.util.SorcerUtil;
import sorcer.util.Table;
/**
 * @author Mike Sobolewski
 * 
 *         This command is used for inspecting Var-oriented models. It allows
 *         for: 1. Inspecting both network (vm) and local (vm -init <model
 *         builder>) var models, for example: vm -init
 *         junit.sorcer.core.context.
 *         model.RosenSuzukiModelBuilder.createModel(); 2. Evaluating (vm -e
 *         {var names}) a collection of observed (selected model input and
 *         output) vars 3. Setting parametric (a collection of selected model
 *         input) vars (vm -s {var names}) 4. Calculating response tables (vm
 *         -rt <parametric table URL>), for example using
 *         geometry/parametric_datafile_long.txt in ${SORCER_HOME}/data
 */
public class VarModelCmd extends ShellCmd {

	{
		COMMAND_NAME = "vm";

		NOT_LOADED_MSG = "***command not loaded due to conflict";

		COMMAND_USAGE = "vm [<model index> | -mod | -var | -v | -x"
				+ "\n\t\t\t  | -ls | -rov  {<var name>} | -rpv  {<var name>}"
				+ "\n\t\t\t  | -st | -pt | -rt"
				+ "\n\t\t\t  | -p  [{<var name>} | -ov | -pv | -mi | -mo | -mc | -mj]"
				+ "\n\t\t\t  | -pp [{<var name>} | -ov | -pv | -mi | -mo | -mc | -mj]"
				+ "\n\t\t\t  | -d  [{<var name>} | -ov | -pv | -mi | -mo | -mc | -mj]"
				+ "\n\t\t\t  | -e  [{<var name>} | [-ov | -pv | -mi | -mo | -mc | -mj | -rt <parametric table URL>]"
				+ "\n\t\t\t  | -s  [{<var name>=<value>} | -pt <parametric table URL> | -rt <response table URL>]"
				+ "\n\t\t\t  | -init [<model builder>]";

		COMMAND_HELP = "Support for Inspecting net and local Var Models;"
				+ "\n  with no options show var model services"
				+ "\n  with <model index> select the model given index"
				+ "\n  -mod  set the model mode; command options applied to the selected model"
				+ "\n  -var  set the var mode; command options applied to the selected var"
				+ "\n  -v    show the selected var model/observed vars"
				+ "\n  -x    clear the selection of model, observed and parametric vars"
				+ "\n  -p    [{<var name>} | -ov | -pv | -mi | -mo | -mc | -mj] print the specified model/vars"
				+ "\n  -pp   [{<var name>} | -ov | -pv | -mi | -mo | -mc | -mj] describe the specified model/vars"
				+ "\n  -d    [{<var name>} | -ov | -pv | -mi | -mo | -mc | -mj] print the var dependencies of the specified model/vars"
				+ "\n  -e    [{<var name>} | [-ov | -pv | -mi | -mo | -mc | -mj | -rt <parametric table URL>]"
				+ "\n           evaluate given vars or response tables"
				+ "\n  -s    [{<var name>=<value>} | -pt <parametric table URL> | -rt <response table URL>]"
				+ "\n           assign corresponding arguments to parametric vars or parametric/response tables"
				+ "\n  -ls   list all current observed and parametric vars in the model"
				+ "\n  -rov  {<var name>} remove given observed vars"
				+ "\n  -rpv  {<var name>} remove given parametric vars"
				+ "\n  -pt   print model parametric table"
				+ "\n  -rt   print model response table"
				+ "\n  -st   print model strategy"
				+ "\n  -init [<model builder>] initialize a net model with its builder or create a model locally";

	}

	static private PrintStream out;
	static private ServiceItem[] modelProviders;
	static private int modelIndex = -1;
	static private Modeling model;
	private String selectedVar;
	private Object selectedResult;
	private List<String> observedVars = new ArrayList<String>();
	private List<Object> result = new ArrayList<Object>();
	private List<String> parametricVars = new ArrayList<String>();
	private List<Object> args = new ArrayList<Object>();
	private String selectedParametericVar;
	private Object arg;
	private GroovyShell gShell = new GroovyShell(ShellStarter.getLoader());

	static private Map<Uuid, ServiceItem> modelMap = new HashMap<Uuid, ServiceItem>();
	private boolean isModelMode = true;

	public VarModelCmd() {
	}

	public void execute() throws RemoteException, MonitorException,
			EvaluationException, ContextException, MalformedURLException {
		out = NetworkShell.getShellOutputStream();
		StringTokenizer myTk = NetworkShell.getShellTokenizer();
		int numTokens = myTk.countTokens();

		if (numTokens == 0) {
			showModelServices();
			if (model != null)
				printModel();
			modelIndex = -1;
			isModelMode = true;
		} else if (numTokens == 1) {
			processSingleOption(myTk);
		} else if (numTokens >= 2) {
			processMultipleOptions(myTk);
		} else {
			out.println("Not valid command: " + NetworkShell.getRequest());
		}
	}

	private void processSingleOption(StringTokenizer tokenizer)
			throws RemoteException, ContextException {
		String next;
		next = tokenizer.nextToken();
		if (next.equals("-v")) {
			if (model != null || modelIndex >= 0) {
				printModel();
			} else {
				out.println("No selected model!");
			}
		} else if (next.equals("-ov")) {
			listObservedVars();
		} else if (next.equals("-pv")) {
			listParametricVars();
		} else if (next.equals("-pt")) {
			printParametricTable();
		} else if (next.equals("-rt")) {
			printResponseTable();
		} else if (next.equals("-st")) {
			printModelStrategy();
		} else if (next.equals("-init")) {
			initializeModel();
		} else if (next.equals("-ls")) {
			listCurrentState();
		} else if (next.equals("-mode")) {
			if (isModelMode)
				out.println("you are in 'model' mode");
			else
				out.println("you are in 'var' mode");
		} else if (next.equals("-var")) {
			isModelMode = false;
			out.println("you are in 'var' mode");
			if (selectedVar != null)
				printVar(selectedVar);
		} else if (next.equals("-mod")) {
			out.println("you are in 'model' mode");
			isModelMode = true;
			printModel();
		} else if (next.equals("-p")) {
			if (isModelMode) {
				if (model != null)
					printModel();
				else if (modelIndex >= 0) {
					printModel(modelIndex);
				}
			} else {
				listCurrentState();
			}
		} else if (next.equals("-pp")) {
			if (isModelMode) {
				if (model != null || modelIndex >= 0)
					describeModel();
				else {
					out.println("No Var Model selected!");
				}
			} else if (observedVars.size() > 0) {
				describeVars(observedVars);
			} else {
				out.println("No Vars observed!");
			}
		} else if (next.equals("-d")) {
			if (isModelMode)
				if (model != null || modelIndex >= 0) {
					printModelDependencies();
				} else {
					out.println("No Var Model selected!");
				}
			else if (selectedVar != null) {
				printVarDependencies(selectedVar);
			} else {
				out.println("No Var selected!");
			}
		} else if (next.equals("-x")) {
			clear();
		} else {
			if (next.startsWith("-")) {
				out.println("invalid option: " + next);
			} else {
				// set the index of selected model
				try {
					int myIdx = Integer.parseInt(next);
					modelIndex = myIdx;
				} catch (NumberFormatException e) {
					modelIndex = selectModelByName(next);
					out.println("invalid model index or name: " + next);
				}
				clearVars();
				if (modelIndex >= 0) {
					showModelService(modelIndex, "SELECTED");
				}
			}
		}
	}

	private void processMultipleOptions(StringTokenizer tokenizer)
			throws RemoteException, ContextException, MalformedURLException {
		int numTokens = tokenizer.countTokens();
		String next = tokenizer.nextToken();
		if (next.equals("-init")) {
			next = tokenizer.nextToken();
			initializeLocalModel();
			return;
		}
		if (modelIndex < 0 && model == null) {
			out.println("No Var Model selected!");
			return;
		}
		if (next.equals("-p")) {
			if (numTokens == 2) {
				next = tokenizer.nextToken();
				if (next.startsWith("-")) {
					if (next.equals("-ov")) {
						printVars(observedVars);
					} else if (next.equals("-pv")) {
						printVars(parametricVars);
					} else if (next.equals("-mi")) {
						printInputVars();
					} else if (next.equals("-mo")) {
						printOutputVars();
					} else if (next.equals("-mc")) {
						printConstraintVars();
					} else if (next.equals("-mj")) {
						printObjectiveVars();
					} else {
						out.println("Invalid option for 'mod -p'");
					}
				} else {
					selectedVar = next;
					printVar(selectedVar);
				}
			} else {
				printVars(getRemainingTokens(tokenizer));
			}
		} else if (next.equals("-pp")) {
			if (numTokens == 2) {
				next = tokenizer.nextToken();
				if (next.startsWith("-")) {
					if (next.equals("-ov")) {
						describeVars(observedVars);
					} else if (next.equals("-pv")) {
						describeVars(parametricVars);
					} else if (next.equals("-mi")) {
						describeInputVars();
					} else if (next.equals("-mo")) {
						describeOutputVars();
					} else if (next.equals("-mc")) {
						describeConstraintVars();
					} else if (next.equals("-mj")) {
						describeObjectiveVars();
					} else {
						out.println("Invalid option for 'mod -pp'");
					}
				} else {
					selectedVar = next;
					describeVar(selectedVar);
				}
			} else {
				describeVars(getRemainingTokens(tokenizer));
			}
		} else if (next.equals("-e")) {
			if (numTokens == 2) {
				next = tokenizer.nextToken();
				if (next.startsWith("-")) {
					if (next.equals("-ov")) {
						evalObservedVars();
					} else if (next.equals("-op")) {
						evalParametricVars();
					} else if (next.equals("-mi")) {
						evalInputVars();
					} else if (next.equals("-mo")) {
						evalOutputVars();
					} else if (next.equals("-mc")) {
						evalConstraintVars();
					} else if (next.equals("-mj")) {
						evalObjectiveVars();
					} else if (next.equals("-rt")) {
						evaluateResponseTable();
					}
				} else {
					selectedVar = next;
					Object obj = getVarValue(selectedVar);
					this.selectedResult = obj;
					out.println(selectedVar + "=" + obj);
				}
			} else {
				next = tokenizer.nextToken();
				if (next.equals("-rt")) {
					String ptUrl = tokenizer.nextToken();
					if (!(ptUrl.startsWith("http://") || ptUrl
							.startsWith("sos://"))) {
						ptUrl = NetworkShell.getWebsterUrl() + "/" + ptUrl;
					}
					evaluateResponseTable(new URL(ptUrl));
				} else {
					List<String> tokens = getRemainingTokens(next, tokenizer);
					List<Object> values = getResult(tokens);
					printResult(tokens, values);
					addToObservedVars(tokens, values);
				}
			}
		} else if (next.equals("-s")) {
			if (numTokens == 2) {
				next = tokenizer.nextToken();
				setVarValue(next);
			} else {
				next = tokenizer.nextToken();
				if (next.startsWith("-")) {
					if (next.equals("-pt")) {
						next = tokenizer.nextToken();
						if (!(next.startsWith("http://") || next
								.startsWith("sos://"))) {
							next = NetworkShell.getWebsterUrl() + "/" + next;
						}
						setParametricTable(new URL(next));
					} else if (next.equals("-rt")) {
						next = tokenizer.nextToken();
						next = tokenizer.nextToken();
						if (!(next.startsWith("http://") || next
								.startsWith("sos://"))) {
							next = NetworkShell.getWebsterUrl() + "/" + next;
						}
						setResponseTable(new URL(next));
					} else {
						out.println("Invalid option for 'mod -s'");
					}
				}
				List<String> tokens = getRemainingTokens(next, tokenizer);
				setVarValues(tokens);
			}
		} else if (next.equals("-d")) {
			if (numTokens == 2) {
				next = tokenizer.nextToken();
				if (next.startsWith("-")) {
					if (next.equals("-ov")) {
						printVarsDependencies(observedVars);
					} else if (next.equals("-pv")) {
						printVarsDependencies(parametricVars);
					} else if (next.equals("-mi")) {
						printInputVarsDependencies();
					} else if (next.equals("-mo")) {
						printOutputVarsDependencies();
					} else if (next.equals("-mc")) {
						printConstraintVarsDependencies();
					} else if (next.equals("-mj")) {
						printObjectiveVarsDependencies();
					} else {
						out.println("Invalid option for 'mod -d'");
					}
				} else {
					selectedVar = tokenizer.nextToken();
					printVarDependencies(selectedVar);
				}
			} else {
				List<String> tokens = getRemainingTokens(tokenizer);
				printVarsDependencies(tokens);
			}
		} else if (next.equals("-rov")) {
			List<String> tokens = getRemainingTokens(tokenizer);
			removeObservedVars(tokens);
		} else if (next.equals("-rpv")) {
			List<String> tokens = getRemainingTokens(tokenizer);
			removeParametricVars(tokens);
		} else {
			out.println(COMMAND_USAGE);
		}
		isModelMode = false;
	}

	private void printModelServices() throws RemoteException {
		if ((modelProviders != null) && (modelProviders.length > 0)) {
			for (int i = 0; i < modelProviders.length; i++) {
				printModel(i);
			}
		} else {
			findModels();
			if (modelProviders == null || modelProviders.length == 0)
				return;
			else
				printModelServices();
		}
	}

	private void showModelServices() throws RemoteException {
		if ((modelProviders != null) && (modelProviders.length > 0)) {
			for (int i = 0; i < modelProviders.length; i++) {
				showModelService(i);
			}
		} else {
			findModels();
			if (modelProviders == null || modelProviders.length == 0)
				return;
			else
				showModelServices();
		}
	}

	private void showModelService(int index) throws RemoteException {
		showModelService(index, null);
	}

	static private void showModelService(int index, String msg)
			throws RemoteException {
		out.println("---------" + (msg != null ? " " + msg : "")
				+ " VAR MODEL SERVICE # " + index + " ---------");
		out.println("MOD: "
				+ modelProviders[index].serviceID
				+ " at: "
				+ AttributesUtil
						.getHostName(modelProviders[index].attributeSets));
		out.println("Home: "
				+ AttributesUtil
						.getUserDir(modelProviders[index].attributeSets));
		String groups = AttributesUtil
				.getGroups(modelProviders[index].attributeSets);
		out.println("Provider name: "
				+ AttributesUtil
						.getProviderName(modelProviders[index].attributeSets));
		out.println("Groups supported: " + groups);
		try {
			// cast used to test for the existence of modeling admin
			// implementation
			ModelingAdmin service = (ModelingAdmin) modelProviders[index].service;
		} catch (Exception e) {
			out.println("No implementation of 'sorcer.vfe.ModelingAdmin'!");
		}
	}

	private int selectModelByName(String name) {
		if (modelProviders != null) {
			for (int i = 0; i < modelProviders.length; i++) {
				if (AttributesUtil.getProviderName(
						modelProviders[i].attributeSets).equals(name))
					return i;
			}
		}
		return -1;
	}

	public static void setModels(ServiceItem[] models) {
		VarModelCmd.modelProviders = models;
	}

	public static Map<Uuid, ServiceItem> getModelMap() {
		return modelMap;
	}

	static ServiceItem[] findModels() throws RemoteException {
		modelProviders = ShellCmd.lookup(new Class[] { Modeling.class });
		return modelProviders;
	}

	public static ArrayList<Modeling> getModels() {
		ArrayList<Modeling> modelList = new ArrayList<Modeling>();
		for (int i = 0; i < modelProviders.length; i++) {
			modelList.add((Modeling) modelProviders[i].service);
		}
		return modelList;
	}

	private Object getVarValue(String varName) throws RemoteException,
			EvaluationException, ContextException {
		ModelingAdmin modelAdmin = getParametricModelingAdmin();
		return modelAdmin.getVarValue(varName);
	}

	private List<Object> getResult(List<String> varNames)
			throws RemoteException, EvaluationException, ContextException {
		ModelingAdmin modelAdmin = getParametricModelingAdmin();
		List<Object> values = modelAdmin.getVarsValues(varNames);
		addToObservedVars(varNames, values);
		return values;
	}

	private void addToObservedVars(List<String> varNames, List<Object> values) {
		for (int i = 0; i < varNames.size(); i++) {
			if (observedVars.contains(varNames.get(i))) {
				this.result.set(i, values.get(i));
			} else {
				observedVars.add(varNames.get(i));
				this.result.add(values.get(i));
			}
		}
	}

	private void printResult(List<String> varNames, List<Object> result) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < varNames.size(); i++) {
			sb.append(varNames.get(i)).append("=");
			sb.append(result.get(i)).append(" ");
		}
		out.println(sb.toString());
	}

	private void printVarDependencies(String varName) throws RemoteException,
			ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Var Dependency for " + varName + "\n"
				+ modelAdmin.printVarDependencies(varName));
	}

	private void printVarsDependencies(List<String> varNames)
			throws RemoteException, ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println(modelAdmin.printVarsDependencies(varNames));
	}

	private void printInputVarsDependencies() throws RemoteException,
			ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println(modelAdmin.printInputVarsDependencies());
	}

	private void printOutputVarsDependencies() throws RemoteException,
			ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println(modelAdmin.printOutputVarsDependencies());
	}

	private void printConstraintVarsDependencies() throws RemoteException,
			ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println(modelAdmin.printConstraintVarsDependencies());
	}

	private void printObjectiveVarsDependencies() throws RemoteException,
			ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println(modelAdmin.printObjectiveVarsDependencies());
	}

	private void printModelDependencies() throws RemoteException,
			ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Var Dependency for " + modelAdmin.getProviderName() + "\n"
				+ modelAdmin.printModelDependencies());
	}

	private void printVar(String varName) throws RemoteException,
			ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Var: " + varName + "\n" + modelAdmin.printVar(varName));
	}

	private void printVars(List<String> varNames) throws RemoteException,
			ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Vars: " + varNames + "\n\n"
				+ modelAdmin.printVars(varNames));
	}

	private void describeVar(String varName) throws RemoteException,
			ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Var: " + varName + "\n" + modelAdmin.describeVar(varName));
	}

	private void describeVars(List<String> varNames) throws RemoteException,
			ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Vars: " + varNames + "\n\n"
				+ modelAdmin.describeVars(varNames));
	}

	private void printInputVars() throws RemoteException, ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Input Vars\n" + modelAdmin.printInputVars());
	}

	private void describeInputVars() throws RemoteException, ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Input Vars\n" + modelAdmin.describeInputVars());
	}

	private void printOutputVars() throws RemoteException, ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Output Vars\n" + modelAdmin.printOutputVars());
	}

	private void describeOutputVars() throws RemoteException, ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Output Vars\n" + modelAdmin.describeOutputVars());
	}

	private void printConstraintVars() throws RemoteException, ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Constraint Vars\n" + modelAdmin.printConstraintVars());
	}

	private void describeConstraintVars() throws RemoteException,
			ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Constraint Vars\n" + modelAdmin.describeConstraintVars());
	}

	private void printObjectiveVars() throws RemoteException, ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Objective Vars\n" + modelAdmin.printObjectiveVars());
	}

	private void describeObjectiveVars() throws RemoteException,
			ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		out.println("Objective Vars\n" + modelAdmin.describeObjectiveVars());
	}

	static private void printModel() throws RemoteException {
		if (model != null) {
			out.println("" + model);
		} else if (modelIndex >= 0) {
			printModel(modelIndex);
		}
	}

	private void describeModel() throws RemoteException {
		if (model != null) {
			out.println(((ModelingAdmin) model).describeModel());
		} else if (modelIndex >= 0) {
			describeModel(modelIndex);
		} else {
			NetworkShell.shellOutput.println("No Var Model selected!");
		}
	}

	static private void printModel(int index) throws RemoteException {
		ModelingAdmin modelAdmin = (ModelingAdmin) modelProviders[index].service;
		out.println(modelAdmin.printModel());
	}

	static private void describeModel(int index) throws RemoteException {
		ModelingAdmin modelAdmin = (ModelingAdmin) modelProviders[index].service;
		out.println(modelAdmin.describeModel());
	}

	private String printVarInfo(String varName) throws RemoteException,
			EvaluationException, ContextException {
		ModelingAdmin modelAdmin = getParametricModelingAdmin();
		return modelAdmin.describeVarInfo(varName);
	}

	private void setVarValue(String varName, String script)
			throws RemoteException, EvaluationException, ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		Object obj = modelAdmin.setVarValue(varName, script);
		out.println(obj + " assigned to: " + varName);
	}

	private void setVarValue(String assignment) throws RemoteException,
			EvaluationException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		String[] toknes = SorcerUtil.getTokens(assignment, "=");
		try {
			selectedParametericVar = toknes[0];
			arg = gShell.evaluate(toknes[1]);
		} catch (Exception e) {
			out.println("Ivalid association 'varname=value': " + assignment);
			return;
		}
		Object val = null;
		try {
			val = modelAdmin.setVarValue(selectedParametericVar, arg);
		} catch (Exception e) {
			out.println("No such var in the model: " + assignment);
			return;
		}
		out.println(val + " assigned to: " + selectedParametericVar);
		if (!parametricVars.contains(selectedParametericVar)) {
			parametricVars.add(selectedParametericVar);
			args.add(arg);
		}
	}

	private void setVarValues(List<String> assignments) throws RemoteException,
			EvaluationException, ContextException {
		ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
		List<String> varNames = new ArrayList<String>();
		List<String> scripts = new ArrayList<String>();
		List<Object> args = new ArrayList<Object>();

		for (String assignment : assignments) {
			String[] toknes = SorcerUtil.getTokens(assignment, "=");
			varNames.add(toknes[0]);
			scripts.add(toknes[1]);
		}
		for (String script : scripts) {
			try {
				args.add(gShell.evaluate(script));
			} catch (Exception e) {
				out.println("Invalid argument: " + script);
			}
		}
		this.args = args;
		parametricVars = varNames;
		args = modelAdmin.setVarsValues(varNames, args);
		out.println(args + " assigned to: " + varNames);
	}

	private List<String> getRemainingTokens(String first,
			StringTokenizer tokenizer) {
		List<String> tokens = new ArrayList<String>();
		if (first != null)
			tokens.add(first);
		while (tokenizer.hasMoreElements()) {
			tokens.add(tokenizer.nextToken());
		}
		return tokens;
	}

	private List<String> getRemainingTokens(StringTokenizer tokenizer) {
		return getRemainingTokens(null, tokenizer);
	}

	private void listVars(List<String> names) {
		StringBuilder sb = new StringBuilder();
		for (String name : names) {
			sb.append(name).append(" ");
		}
		out.println(sb.toString());
	}

	private void listCurrentState() throws RemoteException {
		if (model == null && modelIndex < 0) {
			out.println("No model selected");
			return;
		}
		if (model == null) {
			ModelingAdmin modelAdmin = (ModelingAdmin) getModelingAdmin();
			if (modelAdmin != null)
				out.println("Current Var Model: "
						+ modelAdmin.getProviderName());
		} else {
			out.println("Current Var Model: " + model.getName());
		}
		if (observedVars.size() > 0) {
			out.println("Observed Vars");
			listObservedVars();
		} else {
			out.println("No observed vars selected");
		}
		if (parametricVars.size() > 0) {
			out.println("Parameters");
			listParametricVars();
		} else {
			out.println("No parametric vars selected");
		}
		// printModel();
	}

	private void listParametricVars() {
		if (args.size() > 0)
			printResult(parametricVars, args);
		else
			listVars(parametricVars);
	}

	private void listObservedVars() {
		if (result.size() > 0)
			printResult(observedVars, result);
		else
			listVars(observedVars);
	}

	private void printParametricVars() throws RemoteException, ContextException {
		ModelingAdmin modelAdmin = getParametricModelingAdmin();
		args = modelAdmin.getVarsValues(parametricVars);
		printResult(parametricVars, args);
	}

	@SuppressWarnings("unchecked")
	private void evalInputVars() throws RemoteException, ContextException {
		ModelingAdmin modelAdmin = getParametricModelingAdmin();
		args = modelAdmin.getInputValuesWithNames();
		parametricVars = (List<String>) args.get(args.size() - 1);
		args.remove(args.size() - 1);
		printResult(parametricVars, args);
		addToObservedVars(observedVars, result);
	}

	private void evalParametricVars() throws RemoteException, ContextException {
		ModelingAdmin modelAdmin = getParametricModelingAdmin();
		result = modelAdmin.getVarsValues(parametricVars);
		printResult(observedVars, result);
		addToObservedVars(observedVars, result);
	}

	private void evalObservedVars() throws RemoteException, ContextException {
		ModelingAdmin modelAdmin = getParametricModelingAdmin();
		List<Object> values = modelAdmin.getVarsValues(observedVars);
		printResult(observedVars, values);
		addToObservedVars(observedVars, values);
	}

	@SuppressWarnings("unchecked")
	private void evalObjectiveVars() throws RemoteException, ContextException {
		OptimizationModelingAdmin modelAdmin = getOptimizationModelingAdmin();
		if (modelAdmin != null) {
			List<Object> values = modelAdmin.getObjectiveValuesWithNames();
			List<String> varNames = (List<String>) values
					.get(values.size() - 1);
			values.remove(values.size() - 1);
			printResult(varNames, values);
			addToObservedVars(varNames, values);
		}
	}

	@SuppressWarnings("unchecked")
	private void evalConstraintVars() throws RemoteException, ContextException {
		OptimizationModelingAdmin modelAdmin = getOptimizationModelingAdmin();
		if (modelAdmin != null) {
			List<Object> values = modelAdmin.getConstraintValuesWithNames();
			List<String> varNames = (List<String>) values
					.get(values.size() - 1);
			values.remove(values.size() - 1);
			printResult(varNames, values);
			addToObservedVars(varNames, values);
		}
	}

	@SuppressWarnings("unchecked")
	private void evalOutputVars() throws RemoteException, ContextException {
		ModelingAdmin modelAdmin = getModelingAdmin();
		List<Object> values = modelAdmin.getOutputValuesWithNames();
		List<String> varNames = (List<String>) values.get(values.size() - 1);
		values.remove(values.size() - 1);
		printResult(varNames, values);
		addToObservedVars(varNames, values);
	}

	private void printParametricTable() throws RemoteException,
			ContextException {
		Table table = null;
		ParametricModelingAdmin modelAdmin = getParametricModelingAdmin();
		if (modelAdmin != null)
			table = (Table) modelAdmin.getParametricTable();
		if (table == null) {
			out.println("No parametric table in: "
					+ modelAdmin.getProviderName());
		} else {
			out.println("Parametric table: " + table.getName());
			out.println(table);
		}
	}

	private void printResponseTable() throws RemoteException, ContextException {
		Table table = null;
		ParametricModelingAdmin modelAdmin = getParametricModelingAdmin();
		if (modelAdmin != null)
			table = (Table) modelAdmin.getResponseTable();
		if (table == null) {
			out.println("No response table in: " + modelAdmin.getProviderName());
		} else {
			out.println("Response table: " + table.getName());
			out.println(table);
		}
	}

	private void printModelStrategy() throws RemoteException {
		ModelingAdmin admin = getModelingAdmin();
		if (admin instanceof ParametricModelingAdmin) {
			ParametricModelingAdmin modelAdmin = (ParametricModelingAdmin) admin;
			String strategy = modelAdmin.describeStrategy();
			if (strategy == null) {
				out.println("No parametric model strategy in: "
						+ modelAdmin.getProviderName());
			} else {
				out.println("Parametric model strategy: " + strategy);
			}
		} else {
			out.println("The current model is not of type: "
					+ ParametricModel.class);
		}
	}

	private void evaluateResponseTable() throws RemoteException,
			EvaluationException {
		ParametricModeling pmodel = getParametricModeling();
		((Modeling) pmodel).evaluate();
		Table table = (Table) ((ParametricModeling) pmodel).getResponseTable();
		out.println("Calculated response table\n");
		out.println(table);
	}

	private void setParametricTable(URL parametricTableUrl)
			throws RemoteException, EvaluationException {
		ParametricModeling pmodel = getParametricModeling();
		pmodel.resetParametricTableURL(parametricTableUrl);
	}

	private void setResponseTable(URL responseTableUrl) throws RemoteException,
			EvaluationException {
		ParametricModeling pmodel = getParametricModeling();
		pmodel.setResponseTableURL(responseTableUrl);
	}

	private void evaluateResponseTable(URL parametricTableUrl)
			throws RemoteException, EvaluationException {
		ParametricModeling pmodel = getParametricModeling();
		pmodel.resetParametricTableURL(parametricTableUrl);
		pmodel.evaluate();
		Table table = (Table) ((ParametricModeling) pmodel).getResponseTable();
		out.println("Response table for: " + parametricTableUrl + "\n");
		out.println(table);
	}

	private void initializeModel() throws RemoteException, EvaluationException {
		ModelingAdmin admin = getModelingAdmin();
		if (admin instanceof ParametricModelingAdmin) {
			ParametricModelingAdmin modelAdmin = (ParametricModelingAdmin) admin;
			modelAdmin.initilizeModel();
			modelIndex = -1;
		} else {
			out.println("Not initializable model: " + admin.getModelName());
		}
	}

	private void initializeLocalModel() {
		String request = NetworkShell.getRequest();
		int index = request.indexOf("-init");
		String script = request.substring(index + 6);
		model = (Modeling) gShell.evaluate(script);
		out.println("Built the model: \n" + model);
	}

	private ModelingAdmin getModelingAdmin() {
		ModelingAdmin modelAdmin = null;
		if (modelIndex >= 0) {
			modelAdmin = (ModelingAdmin) modelProviders[modelIndex].service;
		} else if (model != null) {
			modelAdmin = (ModelingAdmin) model;
		}
		return modelAdmin;
	}

	private ParametricModeling getParametricModeling() {
		ParametricModeling pmodel = null;
		if (model == null
				&& modelProviders[modelIndex].service instanceof ParametricModeling) {
			pmodel = (ParametricModeling) modelProviders[modelIndex].service;
		} else if (model instanceof ParametricModeling) {
			pmodel = (ParametricModeling) model;
		} else {
			return null;
		}
		return pmodel;
	}

	private ParametricModelingAdmin getParametricModelingAdmin() {
		ParametricModelingAdmin modelAdmin = null;
		if (model == null
				&& modelProviders[modelIndex].service instanceof ParametricModelingAdmin) {
			modelAdmin = (ParametricModelingAdmin) modelProviders[modelIndex].service;
		} else if (model instanceof ParametricModelingAdmin) {
			modelAdmin = (ParametricModelingAdmin) model;
		} else {
			return null;
		}
		return modelAdmin;
	}

	private OptimizationModelingAdmin getOptimizationModelingAdmin() {
		OptimizationModelingAdmin modelAdmin = null;
		if (model == null
				&& modelProviders[modelIndex].service instanceof OptimizationModelingAdmin) {
			modelAdmin = (OptimizationModelingAdmin) modelProviders[modelIndex].service;
		} else if (model instanceof ParametricModelingAdmin) {
			modelAdmin = (OptimizationModelingAdmin) model;
		} else {
			return null;
		}
		return modelAdmin;
	}

	private void removeObservedVars(List<String> varNames) {
		for (int i = 0; i < varNames.size(); i++) {
			if (observedVars.contains(varNames.get(i))) {
				int j = observedVars.indexOf(varNames.get(i));
				observedVars.remove(j);
				result.remove(j);
			}
		}
	}

	private void removeParametricVars(List<String> varNames) {
		for (int i = 0; i < varNames.size(); i++) {
			if (parametricVars.contains(varNames.get(i))) {
				int j = parametricVars.indexOf(varNames.get(i));
				parametricVars.remove(j);
				args.remove(j);
			}
		}
	}

	private void clear() throws RemoteException {
		modelIndex = -1;
		clearVars();
		// refresh existing mmodels
		findModels();
	}

	private void clearVars() throws RemoteException {
		model = null;
		observedVars.clear();
		result.clear();
		parametricVars.clear();
		args.clear();
		selectedVar = null;
		selectedResult = null;
		selectedParametericVar = null;
		arg = null;
	}

	public static void printCurrentModel() throws RemoteException {
		if (modelIndex >= 0) {
			NetworkShell.shellOutput.println("Current var model: ");
			printModel();
		} else if (model != null) {
			NetworkShell.shellOutput.println("Current var model: ");
			NetworkShell.shellOutput.println("" + model);
		} else {
			NetworkShell.shellOutput
					.println("No selected Var Model, use 'vm' command");
		}
	}

}
