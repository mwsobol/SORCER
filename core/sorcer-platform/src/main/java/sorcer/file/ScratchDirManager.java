/*
 * Copyright 2014, 2015 Sorcersoft.com S.A.
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
import sorcer.util.Sorcer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.MILLIS;
import static sorcer.core.SorcerConstants.*;

/**
 * @author Rafał Krupiński
 */
public class ScratchDirManager {
    final private static Logger log = LoggerFactory.getLogger(ScratchDirManager.class);

    final protected static String FORMAT = "%1$td-%1$tH%1$tM%1$tS-%2$s";
    final protected static Pattern FORMAT_RE = Pattern.compile("^\\d{2}-\\d{6}-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    final protected static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-HHmmss");

    final private static long CLEANUP_INTERVAL = TimeUnit.DAYS.toMillis(1);
    final private static String DEFAULT_ROOT = Paths.get(System.getProperty("java.io.tmpdir"), "scratch").toString();

    final public static ScratchDirManager SCRATCH_DIR_FACTORY = new ScratchDirManager();

    private long lastCleanup;
    private File root;
    private long ttl;

    public ScratchDirManager(File scratchDir, long ttl) {
        root = scratchDir;
        this.ttl = ttl;
    }

    public ScratchDirManager() {
        this(new File(System.getProperty(SCRATCH_DIR, DEFAULT_ROOT)), getScratchTTL());
    }

    public File getNewScratchDir() throws IOException {
        return getNewScratchDir("");
    }

    public File getNewScratchDir(String servicePrefix) throws IOException {
        cleanup();

        return getNewScratchDir0(servicePrefix);
    }

    protected File getNewScratchDir0(String servicePrefix) throws IOException {
        Path serviceRoot = servicePrefix == null || servicePrefix.isEmpty() ? root.toPath() : root.toPath().resolve(servicePrefix);
        String name = String.format(FORMAT, new Date(), UUID.randomUUID());
        Path result = serviceRoot.resolve(name);
        java.nio.file.Files.createDirectories(result);
        return result.toFile();
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
            cleanup1();
        }

        void cleanup1() {
            if (!root.exists())
                return;
            TreeTraverser<File> traverser = Files.fileTreeTraverser();
            for (File file : traverser.postOrderTraversal(root)) {
                boolean remove = file.isDirectory() && FORMAT_RE.matcher(file.getName()).matches() && isCutoffTime(file.getName());
                System.out.println(file + " " + remove);
                if (remove)
                    try {
                        log.info("Removing {}", file);
                        FileUtils.deleteDirectory(file);
                    } catch (IOException e) {
                        log.warn("Could not remove directory {}", file, e);
                    }
            }
        }

        private boolean isCutoffTime(String name) {
            // 9 (nine) depends on FORMAT variable - before the second dash
            String timeStr = name.substring(0, 9);
            try {
                ZonedDateTime now = ZonedDateTime.now();
                ZonedDateTime created = ZonedDateTime.ofInstant(DATE_FORMAT.parse(timeStr).toInstant(), now.getZone());

                created = created.withYear(now.getYear()).withMonth(now.getMonthValue());
                if (created.isAfter(now))
                    created = created.minusMonths(1);

                ZonedDateTime cutoff = created.plus(cutOffTime, MILLIS);
                return cutoff.isAfter(now);
            } catch (ParseException e) {
                throw new IllegalStateException("Found non-matching file: " + name);
            }
        }
    }
}
