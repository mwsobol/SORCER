/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.jini.lookup.entry;

import net.jini.core.lookup.ServiceID;
import net.jini.lookup.entry.ServiceType;

import java.awt.*;

/**
 * Human-oriented information about the "type" of a SORCER service. This is not
 * related to its data or class types, and is more oriented towards allowing
 * someone to determine what a service (for example, an optimizer) does and that it
 * is similar to another, without needing to know anything about data or class
 * types for the Java platform.
 * 
 * @author Mike Sobolewski
 */
public class SorcerServiceInfo extends ServiceType {

	private static final long serialVersionUID = 12L;

	public SorcerServiceInfo() {
		
	}

	/**
	 * Instantiate this class with the provider name, its address, and icon name.
	 */
	public SorcerServiceInfo(String providerName, String hostAddress,
			String iconName) {
		this.providerName = providerName;
		this.hostAddress = hostAddress;
		this.iconName = iconName;
	}
    
	public SorcerServiceInfo(String providerName, String hostAddress,
			String iconName, String shortDescription, String userDir,
			String userName) {
		this(providerName, hostAddress, iconName);
		this.shortDescription = shortDescription;
		this.serviceHome = userDir;
		this.userName = userName;

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
	 * If true, then matching is done for the interface only for space computing,
	 * otherwise matching additionally the provide name.
	 */
	public Boolean matchInterfaceOnly;

	/**
	 * If true, then the provider is monitorable for exerting behavior,
	 * otherwise its actions can not be monitored.
	 */
	public Boolean monitorable;
	
	/**
	 * The list of participating groups.
	 */
	public String groups;

	/**
	 * The exertion space group used by the registering provider.
	 */
	public String spaceGroup;

	/**
	 * The exertion space name used by the registering provider.
	 */
	public String spaceName;

	/**
	 * Indicates if the service provider can match required OS.
	 */
	public String osName;

	/**
	 * Indicates if the service provider can match required applications to be available.
	 */
	public java.util.List<String> apps;

	/**
	 * Indicates if the service provider uses Routine Space.
	 */
	public Boolean puller;
	
	/**
	 * The location of this service, for example, "GE AE", "GE CRD".
	 */
	public String location;

	/**
	 * The host name this service in running on, for example, "
	 * hippolyta.cs.ttu.edu".
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
	 * The provider start date.
	 */
	public String startDate;

	/**
	 * The home directory of the runtime service provider.
	 */
	public String serviceHome;

	/**
	 * The data store URL or host:port used by this service.
	 */
	public String repository;
	
	/**
	 * An icon file name associated with this service.
	 */
	public String iconName;
	
	/**
	 * This provider's mutual exclusion lock ID for its service.
	 * 
	 */
	public String mutexId;
	
	/**
	 * A persistent UUID for this particular service.
	 */	
	public ServiceID serviceID;

	/**
	 * Returns the provider's name of this service.
	 * 
	 * @return the provider name.
	 */
	public String getProviderName() {
		return providerName;
	}

	/**
	 * Returns the provider's mutual exclusion lock ID for its service.
	 * 
	 * @return the provider name.
	 */
	public String getMutexId() {
		return mutexId;
	}

	/**
	 * Returns the localized display name of this service.
	 * 
	 * @return the display name.
	 */
	@Override
	public String getDisplayName() {
		return providerName;
	}

	/**
	 * Returns a localized short description of this service.
	 * 
	 * @return the description.
	 */
	@Override
	public String getShortDescription() {
		if (shortDescription == null)
			return "SORCER service provider";
		else
			return shortDescription;
	}

	/**
	 * Returns an icon for this service. Icons displayed in the SORCER service
	 * browser are 16x16 pixels.
	 * <p>
	 * This particular implementation tries to find the resource specified by
	 * iconName in the configuration.
	 * 
	 * @param iconKind
	 *            type of icon requested. See BeanInfo for an explanation.
	 * @return the service icon.
	 * @see net.jini.lookup.entry.ServiceType#getIcon(int)
	 * @see java.beans.BeanInfo#getIcon(int)
	 */
	@Override
	public Image getIcon(int iconKind) {
		if (iconName == null)
			return null;

		try {
			java.net.URL url = getClass().getClassLoader().getResource(iconName);
			if (url == null)
				return null;
			Image image = Toolkit.getDefaultToolkit().getImage(url);
			if (iconKind == 1 || iconKind == 3)
				return image.getScaledInstance(16, 16, 0);
			else
				return image.getScaledInstance(32, 32, 0);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
