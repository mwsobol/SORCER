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
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static sorcer.core.SorcerConstants.*;

/**
 * Provides support for a {@link ScratchManager}
 *
 * @author Dennis Reedy
 */
public class ScratchManagerSupport implements ScratchManager, Serializable {
    static final long serialVersionUID = 1l;
    private static final Logger logger = LoggerFactory.getLogger(ScratchManagerSupport.class);
    private final AtomicReference<DataService> dataServiceRef = new AtomicReference<>();
    private Properties properties;
    private final File root;
    static final String DEFAULT_SCRATCH_DIR = "default-scratch";

    public ScratchManagerSupport() {
        this(DataService.getDataDir());
    }

    public ScratchManagerSupport(String root) {
        this(new File(root));
    }

    public ScratchManagerSupport(File root) {
        this.root = root;
        if(!root.exists()) {
            if(this.root.mkdirs()) {
                logger.info("Created {}", root.getPath());
            }
        }
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override public File getScratchDir() {
        return getScratchDir("");
    }

    @Override public File getScratchDir(String suffix) {
        if(suffix==null || suffix.length()==0) {
            String scratchDirName = getProperty(SCRATCH_DIR);
            if(scratchDirName==null)
                scratchDirName = getProperty(P_SCRATCH_DIR)==null?
                                 getProperty(R_SCRATCH_DIR):SorcerEnv.getProperty(P_SCRATCH_DIR);
            if(scratchDirName==null) {
                //scratchDirName = String.format("%s-%s", DEFAULT_SCRATCH_DIR, getNext(dataDir));
                scratchDirName = DEFAULT_SCRATCH_DIR;
                logger.warn("scratch directory name cannot be derived from any of the " +
                            "following properties: {}, {}, {}, will default to {}",
                            SCRATCH_DIR, P_SCRATCH_DIR, R_SCRATCH_DIR, scratchDirName);
            }
            suffix = scratchDirName;
        }

        logger.info("scratch dir suffix = {}", suffix);
        String dirName = String.format("%s-%s", suffix, getUniqueId());
        File scratchDir = new File(root, dirName);
        if(scratchDir.mkdirs() && logger.isInfoEnabled()) {
            logger.info("Created "+scratchDir.getPath());
        }
        return scratchDir;
    }

    public DataService getDataService() {
        return dataServiceRef.get();
    }

    @Override public File getScratchDir(Context context, String suffix) {
        File scratchDir = getScratchDir(suffix);

        if (context.containsPath(SCRATCH_DIR_KEY) || context.containsPath(SCRATCH_URL_KEY)) {
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
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd:HH.mm.SSS");
        Calendar c = Calendar.getInstance();
        long time = c.getTime().getTime();

        String uid = UUID.randomUUID().toString();
        return sdf.format(time) + "-" + uid;
    }

    String getNext(String dataDir) {
        File dir = new File(dataDir);
        int last = 0;
        for(String f : dir.list()) {
            if(f.startsWith(DEFAULT_SCRATCH_DIR)) {
                String s = f.substring(f.lastIndexOf("-")+1);
                try {
                    int i = Integer.parseInt(s);
                    last = i>last?i:last;
                } catch(NumberFormatException e) {
                    //ignore
                }
            }
        }
        int next = last+1;
        return Integer.toString(next);
    }

    private String getProperty(String key) {
        return properties == null? SorcerEnv.getProperty(key):properties.getProperty(key);
    }

}
