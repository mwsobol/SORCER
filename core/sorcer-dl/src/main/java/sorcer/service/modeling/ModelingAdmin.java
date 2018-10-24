/*
 * Distribution Statement
 * 
 * This computer software has been developed under sponsorship of the United States Air Force Research Lab. Any further
 * distribution or use by anyone or any data contained therein, unless otherwise specifically provided for,
 * is prohibited without the written approval of AFRL/RQVC-MSTC, 2210 8th Street Bldg 146, Room 218, WPAFB, OH  45433
 * 
 * Disclaimer
 * 
 * This material was prepared as an account of work sponsored by an agency of the United States Government. Neither
 * the United States Government nor the United States Air Force, nor any of their employees, makes any warranty,
 * express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness
 * of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately
 * owned rights.
 */
package sorcer.service.modeling;

import sorcer.service.ContextException;
import sorcer.service.Signature;

import java.rmi.RemoteException;
import java.util.List;

public interface ModelingAdmin {

	String getModelName() throws RemoteException;

	String getProviderName() throws RemoteException;

	String describeModel() throws RemoteException;

	String printModel() throws RemoteException;

	String describeVar(String varName) throws RemoteException, ContextException;

	String describeVars(List<String> varNames) throws RemoteException, ContextException;

	String printInputVars() throws RemoteException, ContextException;

	String describeInputVars() throws RemoteException, ContextException;

	String printOutputVars() throws RemoteException, ContextException;

	String describeOutputVars() throws RemoteException, ContextException;

	String printConstraintVars() throws RemoteException, ContextException;

	String describeConstraintVars() throws RemoteException, ContextException;

	String printObjectiveVars() throws RemoteException, ContextException;

	String describeObjectiveVars() throws RemoteException, ContextException;

	String printVar(String varName) throws RemoteException, ContextException;

	String printVars(List<String> varNames) throws RemoteException, ContextException;

	String describeVarInfo(String varName) throws RemoteException, ContextException;

	Object getVarValue(String varName) throws RemoteException, ContextException;

	List<Object> getVarsValues(List<String> varNames) throws RemoteException, ContextException;

	List<Object> getInputValuesWithNames() throws RemoteException, ContextException;

	List<Object> getOutputValuesWithNames() throws RemoteException, ContextException;

	Object setVarValue(String varName, Object value) throws RemoteException, ContextException;

	List<Object> setVarsValues(List<String> varNames, List<Object> values) throws RemoteException, ContextException;

	String printVarDependencies(String varName) throws RemoteException, ContextException;

	String printVarsDependencies(List<String> varNames) throws RemoteException, ContextException;

	String printInputVarsDependencies() throws RemoteException, ContextException;

	String printOutputVarsDependencies() throws RemoteException, ContextException;

	String printConstraintVarsDependencies() throws RemoteException, ContextException;

	String printObjectiveVarsDependencies() throws RemoteException, ContextException;

	String printModelDependencies() throws RemoteException, ContextException;

//    VarList getInputVars() throws RemoteException, ContextException;
//
//    VarList getOutputVars() throws RemoteException, ContextException;

    Signature getBuilderSignature() throws RemoteException;

}
