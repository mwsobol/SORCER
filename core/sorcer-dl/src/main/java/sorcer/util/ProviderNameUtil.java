/*
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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
package sorcer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rafał Krupiński
 */
public class ProviderNameUtil {
    protected Map<String, String> names = new HashMap<String, String>();
    protected final Logger log = LoggerFactory.getLogger(getClass());

    {
        names = new PropertiesLoader().loadAsMap(getClass());
    }

	public String getName(Class<?> providerType) {
		return names.get(providerType.getName());
	}
}
