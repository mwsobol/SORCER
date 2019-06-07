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
import sorcer.service.Setup;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static sorcer.co.operator.setValue;
import static sorcer.co.operator.setup;
import static sorcer.co.operator.val;
import static sorcer.eo.operator.context;

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
        data.put("partial", "other eval: <${key}>");
        data.put("multi", "other eval: <${key}> and <${self}>");

        //bad
        data.put("selfreferencing", "${selfreferencing}");
        data.put("missing", "${nothere}");
        data.put("invalid", "${invalid");

        eval.eval(data);
        assertEquals("replacedValue", data.get("key"));
        assertEquals(envValue, data.get("envKey"));
        assertEquals(System.getProperty("user.home"), data.get("sysKey"));
        assertEquals("replacedValue", data.get("self"));
        assertEquals("other eval: <replacedValue>", data.get("partial"));
        assertEquals("other eval: <replacedValue> and <replacedValue>", data.get("multi"));

        assertEquals("${selfreferencing}", data.get("selfreferencing"));
        assertEquals("${nothere}", data.get("missing"));
        assertEquals("${invalid", data.get("invalid"));

    }

    // setups via provided context with a path for the aspect of setup
    @Test
    public void setup1() throws Exception {
        Setup cxtEnt = setup("context/execute", context(val("arg/x1", 100.0), val("arg/x2", 20.0)));
        assertEquals(100.0, val(cxtEnt, "arg/x1"));
    }

    @Test
    public void setup2() throws Exception {
        Setup cxtEnt = setup("context/execute", val("arg/x1", 100.0), val("arg/x2", 20.0));
        assertEquals(100.0, val(cxtEnt, "arg/x1"));
    }

    @Test
    public void setValueOfSetup1() throws Exception {
        Setup cxtEnt = setup("context/execute", val("arg/x1", 100.0), val("arg/x2", 20.0));
        setValue(cxtEnt, "arg/x1", 80.0);
        assertEquals(80.0, val(cxtEnt, "arg/x1"));
    }

    @Test
    public void setValueOfSetup2() throws Exception {
        Setup cxtEnt = setup("context/execute", val("arg/x1", 100.0), val("arg/x2", 20.0));
        setValue(cxtEnt, val("arg/x1", 80.0), val("arg/x2", 10.0));
        assertEquals(80.0, val(cxtEnt, "arg/x1"));
        assertEquals(10.0, val(cxtEnt, "arg/x2"));
    }

}
