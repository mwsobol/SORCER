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

import sorcer.service.*;
import sorcer.util.ModelTable;

import java.rmi.RemoteException;
import java.util.List;

/**
 * @author Mike Sobolewski, Ray Kolonay
 */
public interface ResponseModeling extends Modeling, Configurable {

	public Context getSnapshot(Context context) throws ContextException,
			RemoteException;

//    public Context getSnapshot(VarInfoList... varInfoLists) throws ContextException,
//			RemoteException;

	public Response evaluateResponse(String... varNames) throws EvaluationException,
			RemoteException;

	public double getPartiaDerivative(String varname, String wrt)
			throws EvaluationException, RemoteException;

	public double getPartialDerivative(String varname, String gradient,
                                       String wrt) throws EvaluationException, RemoteException;

	public double getPartiaDerivative(String varname, String gradient,
                                      String evaluationName, String wrt) throws EvaluationException,
			RemoteException;

	public double[] getPartialDerivativeGradient(String varname,
                                                 List<String> wrt) throws EvaluationException, RemoteException;

	public double[][] getPartialDerivativeMatrix(List<String> varnames,
                                                 List<String> wrt) throws EvaluationException, RemoteException;

	public ModelTable getPartialDerivativeTable(String varname)
			throws EvaluationException, RemoteException;

	public ModelTable getPartialDerivativeTable(String varname, String gradient)
			throws EvaluationException, RemoteException;

	public double getTotalDerivative(String varname, String wrt)
			throws EvaluationException, RemoteException;

	public double getTotalDerivative(String varname, String gradient, String wrt)
			throws EvaluationException, RemoteException;

	public double getTotalDerivative(String varname, String gradient,
                                     String evaluationName, String wrt) throws EvaluationException,
			RemoteException;

	public double[] getTotalDerivativeGradient(String varname, List<String> wrt)
			throws EvaluationException, RemoteException;

	public double[][] getTotalDerivativeMatrix(List<String> varnames,
                                               List<String> wrt) throws EvaluationException, RemoteException;

	public ModelTable getTotalDerivativeTable(String varname)
			throws EvaluationException, RemoteException;

	public ModelTable getTotalDerivativeTable(String varname, String gradient)
			throws EvaluationException, RemoteException;

}
