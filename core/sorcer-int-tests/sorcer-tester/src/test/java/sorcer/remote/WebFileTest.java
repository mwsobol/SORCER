/*
 * Copyright 2014 Sorcersoft.com S.A.
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

package sorcer.remote;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sorcer.test.ProjectContext;
import org.sorcer.test.SorcerTestRunner;
import sorcer.core.context.ServiceContext;
import sorcer.file.remote.RemoteFileFactory;
import sorcer.service.Context;

import java.io.File;

import static junitx.framework.Assert.assertNotEquals;
import static org.junit.Assume.assumeTrue;

@RunWith(SorcerTestRunner.class)
@ProjectContext("core/sorcer-int-tests/sorcer-tester")
public class WebFileTest {

    @Test
    public void testDoGetFile() throws Exception {
        assumeTrue(!System.getProperty("os.name").toLowerCase().contains("windows"));
        Context c = new ServiceContext();
        c.putValue("data", RemoteFileFactory.INST.forFile(new File("/etc/hosts")));
        File tmpFile = new File("/tmp/webfiletester.tmp");
        if (!tmpFile.exists()) tmpFile.createNewFile();
        c.putValue("dataTmp", RemoteFileFactory.INST.forFile(tmpFile));
        Object data = c.getValue("data");
        assertNotEquals(sorcer.service.Context.none, data);
        Object dataTmp = c.getValue("dataTmp");
        assertNotEquals(sorcer.service.Context.none, dataTmp);
    }
}
