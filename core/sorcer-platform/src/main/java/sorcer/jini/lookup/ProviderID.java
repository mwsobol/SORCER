
package sorcer.jini.lookup;

import net.jini.core.lookup.ServiceID;


public class ProviderID implements Comparable<Object> {
	private ServiceID serviceID;

	
	public ProviderID(ServiceID id) {
		serviceID = id;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Object arg) {
		if (arg instanceof ProviderID) {
			long l1 = serviceID.getMostSignificantBits();
			long l2 = ((ServiceID) arg).getMostSignificantBits();
			if ((l1 - l2) == 0) {
				return 0;
			} else if ((l1 - l2) > 0) {
				return 1;
			} else {
				return -1;
			}
		}
		throw new RuntimeException("Wrong argument to compare: " + arg);
	}

}
