package sorcer.util.eval;
/**
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.util.Collections;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Rafał Krupiński
 */
public class PropertyEvaluator {
    private static final String PREFIX = "${";
    private static final String SUFFIX = "}";
    private Map<String, Properties> sources = new HashMap<String, Properties>();

    private Logger logger = LoggerFactory.getLogger(PropertyEvaluator.class);

    public void addDefaultSources() {
        addDefaultSources("sys", "env");
    }

    @SuppressWarnings("unchecked")
    public void addDefaultSources(String sys, String env) {
        addSource(sys, System.getProperties());
        addSource(env, System.getenv());
    }

    public void addSource(String prefix, Map<String, String> source) {
        addSource(prefix, Collections.toProperties(source));
    }

    public void addSource(String prefix, Properties source) {
        sources.put(prefix, source);
    }

    public void eval(Map<String, String> data) {
        boolean dirty;
        do {
            dirty = false;
            for (Map.Entry<String, String> e : data.entrySet()) {
                dirty |= eval(e.getKey(), e.getValue(), data, sources);
            }
        } while (dirty);
    }

    public String eval(String value) {
        Map<String, String> data = new HashMap<String, String>();
        String key = "--KEY--";
        data.put(key, value);
        eval(data);
        if (!value.equals(data.get(key)))
            logger.debug("Replacing: " + value + " with: " + data.get(key));
        return data.get(key);
    }

    private boolean eval(String key, String value, Map<String, String> data, Map<String, Properties> sources) {
        int[] variable = findVar(value);
        if (variable == null) return false;
        String var = value.substring(variable[0], variable[1]);
        if (var.equals(key)) return false;

        int prefEnd = var.indexOf('.');
        Properties source = null;
        String useKey = null;
        if (prefEnd >= 0) {
            String sourceKey = var.substring(0, prefEnd);
            if (sources.containsKey(sourceKey)) {
                source = sources.get(sourceKey);
                useKey = var.substring(prefEnd + 1);
            }
        }
        if (source == null) {
            source = Collections.toProperties(data);
            useKey = var;
        }
        String varValue = source.getProperty(useKey);
        if (varValue != null) {
            String newValue = replaceAll(value, variable[2], variable[3], varValue);
            data.put(key, newValue);
            return true;
        } else {
            return false;
        }

    }

    private String replaceAll(String value, int i, int i1, String varValue) {
        StringBuilder buf = new StringBuilder(value);
        buf.replace(i, i1, varValue);
        return buf.toString();
    }

    /**
     * return null or 4 values:
     * key begin index
     * key end index
     * prefix index
     * suffix index
     */
    private int[] findVar(String value) {
        int open = value.indexOf(PREFIX);
        if (open < 0) return null;
        int close = value.indexOf(SUFFIX, open);
        if (close < 0) return null;

        return new int[]{open + PREFIX.length(), close, open, close + SUFFIX.length()};
    }
}
