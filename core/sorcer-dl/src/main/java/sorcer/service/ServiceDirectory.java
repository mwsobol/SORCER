package sorcer.service;

import org.rioproject.opstring.OperationalString;
import sorcer.schema.SchemaEntry;

import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

/**
 * SORCER interface ServiceDirectory implemented by the commercial Almanac service.
 * This interface contains only the methods required by the provisioning code integrated
 * into dispatchers
 *
 * User: prubach
 * Date: 17.11.14
 */

public interface ServiceDirectory extends Remote {

        /**
         * Returns OperationalString object for given service.
         *
         * @param type    Type of service. Required parameter.
         * @param version Version of service artifact. The default is the latest version.
         * @param name    Name of the service. If null, return random, matching type and version.
         * @return Operational String as a String.
         * @throws java.lang.IllegalArgumentException when the service is configured to require version (the default)
         *                                            and version is not passed
         */
        OperationalString getOpString(String type, String version, String name) throws RemoteException;
}
