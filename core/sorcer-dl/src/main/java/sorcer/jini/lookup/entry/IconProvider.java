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

import net.jini.lookup.entry.ServiceType;

import java.awt.*;

/**
 * Provides an icon for the service associated with tis entry. The valid
 * parameter values are the same as for the getIcon method of
 * {@link java.beans.BeanInfo}.
 */
public class IconProvider extends ServiceType {

	private static final long serialVersionUID = -1121746674438783852L;

	public IconProvider() {
	}

	public IconProvider(String icon) {
		iconName = icon;
	}

	public Image getIcon(int iconKind) {
		try {
			java.net.URL url = getClass().getClassLoader()
					.getResource(iconName);
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

	public String iconName;
}
