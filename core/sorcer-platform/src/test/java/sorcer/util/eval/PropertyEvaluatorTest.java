package sorcer.util.eval;
/**
 *
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


import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Rafał Krupiński
 */
public class PropertyEvaluatorTest {

    static PropertyEvaluator eval = new PropertyEvaluator();

    @BeforeClass
    public static void init() {
        eval.addDefaultSources();
    }

    @Test
    public void testEval() throws Exception {
        Map.Entry<String, String> firstEnvEntry = System.getenv().entrySet().iterator().next();
        String envKey = firstEnvEntry.getKey();
        String envValue = firstEnvEntry.getValue();

        assertNotNull(envValue);
        Map<String, String> source = new HashMap<String, String>();
        source.put("key", "replacedValue");
        eval.addSource("source", source);

        Map<String, String> data = new HashMap<String, String>();
        //good
        data.put("key", "${source.key}");
        data.put("envKey", "${env." + envKey + "}");
        data.put("sysKey", "${sys.user.home}");
        data.put("self", "${key}");
        data.put("partial", "other value: <${key}>");
        data.put("multi", "other value: <${key}> and <${self}>");

        //bad
        data.put("selfreferencing", "${selfreferencing}");
        data.put("missing", "${nothere}");
        data.put("invalid", "${invalid");

        eval.eval(data);
        assertEquals("replacedValue", data.get("key"));
        assertEquals(envValue, data.get("envKey"));
        assertEquals(System.getProperty("user.home"), data.get("sysKey"));
        assertEquals("replacedValue", data.get("self"));
        assertEquals("other value: <replacedValue>", data.get("partial"));
        assertEquals("other value: <replacedValue> and <replacedValue>", data.get("multi"));

        assertEquals("${selfreferencing}", data.get("selfreferencing"));
        assertEquals("${nothere}", data.get("missing"));
        assertEquals("${invalid", data.get("invalid"));

    }
}
