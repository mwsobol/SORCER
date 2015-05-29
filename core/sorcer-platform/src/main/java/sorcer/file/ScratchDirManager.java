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

package sorcer.file;

import com.google.common.collect.TreeTraverser;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.data.DataService;
import sorcer.util.Sorcer;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static sorcer.core.SorcerConstants.*;

/**
 * @author Rafał Krupiński
 */
public class ScratchDirManager {
    final private static Logger log = LoggerFactory.getLogger(ScratchDirManager.class);

    final protected static String FORMAT = "%1$td-%1$tH%1$tM%1$tS-%2$s";
    final protected static Pattern FORMAT_RE = Pattern.compile("^\\d{2}-\\d{6}-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    final private static long CLEANUP_INTERVAL = TimeUnit.DAYS.toMillis(1);

    final public static ScratchDirManager SCRATCH_DIR_FACTORY = new ScratchDirManager();

    private long lastCleanup;
    private File root;
    private long ttl;

    public ScratchDirManager(File scratchDir, long ttl) {
        root = scratchDir;
        this.ttl = ttl;
    }

    public ScratchDirManager() {
        this(new File(DataService.getDataDir()), getScratchTTL());
    }

    public File getNewScratchDir() {
        return getNewScratchDir("");
    }

    public File getNewScratchDir(String servicePrefix) {
        cleanup();

        return getNewScratchDir0(servicePrefix);
    }

    protected File getNewScratchDir0(String servicePrefix) {
        File serviceRoot = servicePrefix == null || servicePrefix.isEmpty() ? root : new File(root, servicePrefix);
        String name = String.format(FORMAT, new Date(), UUID.randomUUID());
        return new File(serviceRoot, name);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        if (lastCleanup + CLEANUP_INTERVAL < now) {
            cleanup0(now - ttl);
            lastCleanup = now;
        }
    }

    protected void cleanup0(long cutOffTime) {
        Thread t = new Thread(new CleanupThread(cutOffTime), "scratch-cleanup");
        t.start();
    }

    private static long getScratchTTL() {
        String ttlStr = Sorcer.getProperty(P_SCRATCH_TTL);
        try {
            return Long.parseLong(ttlStr);
        } catch (NumberFormatException e) {
            return SCRATCH_TTL_DEFAULT;
        }
    }

    private class CleanupThread implements Runnable {
        private long cutOffTime;

        private CleanupThread(long cutOffTime) {
            this.cutOffTime = cutOffTime;
        }

        @Override
        public void run() {
            if (!root.exists())
                return;
            TreeTraverser<File> traverser = Files.fileTreeTraverser();
            for (File file : traverser.postOrderTraversal(root)) {
                if (file.isDirectory() && FORMAT_RE.matcher(file.getName()).matches())
                    try {
                        log.info("Removing {}", file);
                        FileUtils.deleteDirectory(file);
                    } catch (IOException e) {
                        log.warn("Could not remove directory {}", file, e);
                    }
            }
        }
    }
}
