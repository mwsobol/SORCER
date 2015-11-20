package sorcer.core.loki.group;

import java.rmi.Remote;
import java.rmi.RemoteException;

import sorcer.core.proxy.RemotePartner;
import sorcer.service.Context;

/**
 * The group management interface lays the interface for database
 * interaction, wrapping context passing to send and retrieve information
 * to the database
 * 
 * @author Daniel Kerr
 */

public interface GroupManagement extends Remote, RemotePartner
{
	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * get the provider identification information
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getProviderID (Context context) throws RemoteException;
	
	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * exert update
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context executeUpdate(Context context) throws RemoteException;
	/**
	 * exert query
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context executeQuery(Context context) throws RemoteException;
	
	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * add activity entry
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context addActivityEntry(Context context) throws RemoteException;
	/**
	 * add exectuion entry
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context addExecutionEntry(Context context) throws RemoteException;
	/**
	 * add exertion entry
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context addExertionEntry(Context context) throws RemoteException;
	/**
	 * add group entry
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context addGroupEntry(Context context) throws RemoteException;
	/**
	 * add member entry
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context addMemberEntry(Context context) throws RemoteException;
	/**
	 * add membership entry
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context addMembershipEntry(Context context) throws RemoteException;

	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * get all groups
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getGroups(Context context) throws RemoteException;
	/**
	 * get group exertions
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getGroupExertions(Context context) throws RemoteException;
	/**
	 * get group members
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getGroupMembers(Context context) throws RemoteException;
	/**
	 * get group action
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getGroupAction(Context context) throws RemoteException;
	/**
	 * get action info
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getActionInfo(Context context) throws RemoteException;
	
	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * get activity entry
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getActivityEntry(Context context) throws RemoteException;
	/**
	 * get exertion entry
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getExertionEntry(Context context) throws RemoteException;
	/**
	 * get group entry
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getGroupEntry(Context context) throws RemoteException;
	/**
	 * get member entry
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getMemberEntry(Context context) throws RemoteException;

	//------------------------------------------------------------------------------------------------------------
	
	/**
	 * get all activities
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getActivities(Context context) throws RemoteException;
	/**
	 * get all executions
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getExecutions(Context context) throws RemoteException;
	/**
	 * get all exertions
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getExertions(Context context) throws RemoteException;
	/**
	 * get all members
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getMembers(Context context) throws RemoteException;
	/**
	 * get all memberships
	 * 
	 * @param context		information context
	 * @return				results context
	 */
	public Context getMemberships(Context context) throws RemoteException;
	
	//------------------------------------------------------------------------------------------------------------
}