/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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

package sorcer.core.deploy;

import net.jini.id.Uuid;
import org.rioproject.resolver.Artifact;
import sorcer.core.provider.ServiceTasker;
import sorcer.service.Deployment;
import sorcer.service.Strategy.Provision;
import sorcer.util.Sorcer;

import java.io.File;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Attributes related to signature based deployment.
 *
 * @author Mike Sobolewski
 * @author Dennis Reedy
 */
public class ServiceDeployment implements Serializable, Deployment {
    private static final long serialVersionUID = 1L;

    private boolean isProvisionable = true;
	private Type type = Type.FED;
    private Unique unique = Unique.NO;
    private int maxPerCybernode;

    private String name;
    private Uuid providerUuid;
    private int multiplicity = 1;
    private String[] codebaseJars;
    private String[] classpathJars;

    // serviceInfo and providerName are given in Signatures,
    // can be used for querying relevant Deployments
    // to be associated with signatures
    private String serviceType;
    private String providerName;
    private String impl = ServiceTasker.class.getName();
    private String websterUrl = Sorcer.getWebsterUrl();
    private String config;
    private String architecture;
    private final Set<String> operatingSystems = new HashSet<String>();
    private final Set<String> ips = new HashSet<String>();
    private final Set<String> excludeIps = new HashSet<String>();

    // an idle time for un-provisioning
    private int idle = 0; /* Value is in minutes */
    public static final int DEFAULT_IDLE_TIME = 5;

    private Boolean fork;
    private String jvmArgs;

    public ServiceDeployment() {
    }

    public ServiceDeployment(final String config) {
        setConfig(config);
    }

    public void setConfig(final String config) {
        if(config.startsWith("http")) {
            this.config = config;
        } else if(Artifact.isArtifact(config)) {
            this.config = config;
        } else if(!config.startsWith("/")) {
            this.config = System.getenv("SORCER_HOME") + File.separatorChar + config;
        } else {
            this.config = config;
        }
    }

    public String getConfig() {
        if(config==null) {
            return "-";
        }
        return config;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setIps(final String... ips) {
        Collections.addAll(this.ips, ips);
    }

    public String[] getIps() {
        return ips.toArray(new String[ips.size()]);
    }

    public void setExcludeIps(final String... ips) {
        Collections.addAll(this.excludeIps, ips);
    }

    public String[] getExcludeIps() {
        return excludeIps.toArray(new String[excludeIps.size()]);
    }

    public void setArchitecture(final String architecture) {
        this.architecture = architecture;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setOperatingSystems(final String... operatingSystems) {
        Collections.addAll(this.operatingSystems, operatingSystems);
    }

    public String[] getOperatingSystems() {
        return operatingSystems.toArray(new String[operatingSystems.size()]);
    }

    public String getName() {
        return name;
    }

    public Uuid getProviderUuid() {
        return providerUuid;
    }

    public void setProviderUuid(final Uuid providerUuid) {
        this.providerUuid = providerUuid;
    }

    public int getMultiplicity() {
        return multiplicity;
    }

    public void setMultiplicity(final int multiplicity) {
        this.multiplicity = multiplicity;
    }

    public Integer getMaxPerCybernode() {
        return maxPerCybernode;
    }

    public void setMaxPerCybernode(int maxPerCybernode) {
        this.maxPerCybernode = maxPerCybernode;
    }

    public String[] getCodebaseJars() {
        return codebaseJars;
    }

    public void setCodebaseJars(final String[] dls) {
        for (int i = 0; i < dls.length; i++)
            if (!dls[i].startsWith("file://") || !dls[i].startsWith("http://"))
                dls[i] = websterUrl + File.separatorChar + dls[i];
        this.codebaseJars = dls;
    }

    public String[] getClasspathJars() {
        return classpathJars;
    }

    public void setClasspathJars(final String[] jars) {
        this.classpathJars = jars;
    }

    public String getImpl() {
        return impl;
    }

    public void setImpl(final String impl) {
        this.impl = impl;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(final String serviceType) {
        this.serviceType = serviceType;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(final String providerName) {
        this.providerName = providerName;
    }

    public String getWebsterUrl() {
        return websterUrl;
    }

    public void setWebsterUrl(final String websterUrl) {
        this.websterUrl = websterUrl;
    }

    public int getIdle() {
        return idle;
    }

    public void setIdle(final int idle) {
        this.idle = idle;
    }

    public void setIdle(final String idle) {
        this.idle = parseInt(idle);
    }

    public static int parseInt(String idle) {
		String timeout = idle.trim();
		int delay;
		char last = timeout.charAt(timeout.length()-1);
		if (last == 'h') {
			delay = Integer.parseInt(timeout.substring(0, timeout.length()-1)) * 60;
		} else if (last == 'd') {
			delay = Integer.parseInt(timeout.substring(0, timeout.length()-1)) * 60 * 24;
		} else {
			delay = Integer.parseInt(timeout);
		}
		return delay;
	}

    public Boolean getFork() {
        return fork;
    }

    public void setFork(final boolean fork) {
        this.fork = fork;
    }

    public String getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(final String jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public void setUnique(final Unique unique) {
        this.unique = unique;
    }

    public Unique getUnique() {
        return unique;
    }

    public static String createDeploymentID(final String ssb) throws NoSuchAlgorithmException {
    	MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(ssb.getBytes());
		byte byteData[] = md.digest();
		// convert the byte to hex
		StringBuilder hexString = new StringBuilder();
        for (byte data : byteData) {
            String hex = Integer.toHexString(0xff & data);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
		return hexString.toString();
    }

	public boolean isProvisionable() {
		return isProvisionable;
	}

	public void setProvisionable(boolean isProvisionable) {
		this.isProvisionable = isProvisionable;
	}

	public void setProvisionable(Provision isProvisionable) {
		if (isProvisionable == Provision.YES
				|| isProvisionable == Provision.TRUE) {
			this.isProvisionable = true;
		} else {
			this.isProvisionable = true;
		}
	}

    @Override
    public String toString() {
        return "Deployment {" +
               "type=" + type +
               ", unique=" + unique +
               ", maxPerCybernode=" + maxPerCybernode +
               ", name='" + name + '\'' +
               ", providerUuid=" + providerUuid +
               ", multiplicity=" + multiplicity +
               ", codebaseJars=" + Arrays.toString(codebaseJars) +
               ", classpathJars=" + Arrays.toString(classpathJars) +
               ", serviceInfo='" + serviceType + '\'' +
               ", providerName='" + providerName + '\'' +
               ", impl='" + impl + '\'' +
               ", websterUrl='" + websterUrl + '\'' +
               ", config='" + config + '\'' +
               ", architecture='" + architecture + '\'' +
               ", operatingSystems=" + operatingSystems +
               ", ips=" + ips +
               ", excludeIps=" + excludeIps +
               ", idle=" + idle +
               ", fork=" + fork +
               ", jvmArgs='" + jvmArgs + '\'' +
               '}';
    }
}