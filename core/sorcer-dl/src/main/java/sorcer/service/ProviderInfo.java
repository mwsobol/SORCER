package sorcer.service;

import sorcer.jini.lookup.entry.SorcerServiceInfo;

import java.io.Serializable;

/**
 * The ProviderInfo class defines human-oriented information about a SORCER provider.
 * This is not related to its data or class types, and is more oriented towards allowing
 * someone to determine which service failed or behaved abnormally.
 *
 * @author Mike Sobolewski
 */
public class ProviderInfo implements Serializable {
    static final long serialVersionUID = 1L;

    public ProviderInfo() {
    }

    /**
     * Instantiate this class with the provider name, address, and its exectuting operation.
     */
    public ProviderInfo(final String providerName,
                        final String hostAddress,
                        final String operation) {
        this.providerName = providerName;
        this.hostAddress = hostAddress;
        this.operation = operation;
    }

    public ProviderInfo(SorcerServiceInfo info) {
        providerName = info.providerName;
        publishedServices = info.publishedServices;
        hostName = info.hostName;
        hostAddress = info.hostAddress;
        userName = info.userName;
        serviceHome = info. serviceHome;
    }

    public ProviderInfo append(final ProviderInfo info) {
        if (info.providerName != null)
            providerName = info.providerName;
        if (info.operation != null)
            operation = info.operation;
        if (info.serviceHome != null)
            serviceHome = info.serviceHome;
        if (info.hostAddress != null)
           hostAddress = info.hostAddress;
        if (info.userName != null)
            userName = info.userName ;
        if (info.hostName != null)
           hostName = info.hostName;
        if (info.scratchDir != null)
            scratchDir = info.scratchDir;
        if (info.workDir != null)
            workDir = info.workDir;
        if (info.workUrl != null)
            workUrl = info.workUrl;

        return this;
    }

    /**
     * The name of provider (service) itself that is used by clients along with
     * service type (interface) to identify tasks to be executed. These tasks
     * are placed by clients into the SORCER distributed space.
     */
    public String providerName;

    /**
     * The list of published interfaces used by space workers.
     */
    public String[] publishedServices;

    /**
     * The short service description that is presented to users.
     */
    public String shortDescription;

    /**
     * Executed operation.
     */
    public String operation;

    /**
     * The host name this service in running on, for example, "
     * sorcersoft.org".
     */
    public String hostName;

    /**
     * The host IP address this service in running on, for example.
     */
    public String hostAddress;

    /**
     * The user name (login).
     */
    public String userName;

    /**
     * The home directory of the runtime service provider.
     */
    public String serviceHome;

    /**
     * Used scratch directory.
     */
    public String scratchDir;

    /**
     * Used work directory.
     */
    public String workDir;

    /**
     * Used work URL.
     */
    public String workUrl;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (providerName != null)
            sb.append("provider: ").append(providerName).append("\n");
        if (operation != null)
            sb.append("service operation: ").append(operation).append("\n");
        if (serviceHome != null)
            sb.append("service home: ").append(serviceHome).append("\n");
        if (hostAddress != null)
            sb.append("host address: ").append(hostAddress).append("\n");
        if (userName != null)
            sb.append("user name: ").append(userName).append("\n");
        if (hostName != null)
            sb.append("host address: ").append(hostName).append("\n");
        if (scratchDir != null)
            sb.append("scratch dir: ").append(scratchDir).append("\n");
        if (workDir != null)
            sb.append("work dir: ").append(workDir).append("\n");
        if (workUrl != null)
            sb.append("work URL: ").append(workUrl);

        return sb.toString();

    }

}
