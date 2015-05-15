/*
 * Copyright to the original author or authors.
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
package sorcer.scratch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.data.DataService;
import sorcer.service.Context;
import sorcer.util.SorcerEnv;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static sorcer.core.SorcerConstants.SCRATCH_DIR_KEY;
import static sorcer.core.SorcerConstants.SCRATCH_URL_KEY;

/**
 * Provides support for a {@link ScratchManager}
 *
 * @author Dennis Reedy
 */
public class ScratchManagerSupport implements ScratchManager, Serializable {
    static final long serialVersionUID = 1l;
    private static final Logger logger = LoggerFactory.getLogger(ScratchManagerSupport.class);
    private final AtomicReference<DataService> dataServiceRef = new AtomicReference<>();

    @Override public File getScratchDir() {
        return getScratchDir("");
    }

    @Override public File getScratchDir(String suffix) {
        String dataDir = System.getProperty(DataService.DATA_DIR);
        String scratchDirName = SorcerEnv.getProperty("provider.scratch.dir");

        logger.info("scratch_dir = " + scratchDirName);
        String dirName = String.format("%s/%s/%s", dataDir, scratchDirName, getUniqueId());
        File tempDir = new File(dirName);
        File scratchDir;
        if (suffix == null || suffix.length() == 0) {
            scratchDir = tempDir;
        } else {
            scratchDir = new File(tempDir.getParentFile(),
                                  String.format("%s%s", suffix, tempDir.getName()));
        }
        if(scratchDir.mkdirs() && logger.isInfoEnabled()) {
            logger.info("Created "+scratchDir.getPath());
        }
        return scratchDir;
    }

    @Override public File getScratchDir(Context context, String suffix) {
        File scratchDir = getScratchDir(suffix);

        if (context.containsPath(SCRATCH_DIR_KEY) || context.containsPath(SCRATCH_URL_KEY)) {
            // throw new ContextException(
            // "***error: context already contains scratch dir or scratch url key; "
            // + "do not use this method twice on the same context argument "
            // + "(use getScratchDir() and add scratch dir key and value "
            // + "yourself)");
            logger.warn("*** Warning: context already contains scratch dir or scratch url key; "
                        + "beware of using this method twice on the same context argument "
                        + "(using getScratchDir() and add scratch dir key and value "
                        + "yourself may be better)."
                        + "\n\tcontext name = "
                        + context.getName() + "\n\tcontext = " + context);
        }

        return scratchDir;
    }

    @Override public URL getScratchURL(File scratchFile) {
        synchronized (dataServiceRef) {
            if(dataServiceRef.get()==null) {
                dataServiceRef.set(DataService.getPlatformDataService());
            }
        }
        URL dataURL = null;
        try {
            dataURL = dataServiceRef.get().getDataURL(scratchFile);
        } catch (IOException e) {
            logger.error("Could not create scratch URL ", e);
        }
        return dataURL;
    }

    String getUniqueId() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-HHmmss");
        Calendar c = Calendar.getInstance();
        long time = c.getTime().getTime();

        String uid = UUID.randomUUID().toString();
        return sdf.format(time) + "-" + uid;
    }

}
