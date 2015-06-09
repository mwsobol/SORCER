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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.data.DataService;
import sorcer.util.Sorcer;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.MILLIS;
import static sorcer.core.SorcerConstants.*;

/**
 * @author Rafał Krupiński
 */
public class ScratchDirManager {
    final private static Logger log = LoggerFactory.getLogger(ScratchDirManager.class);

    final private static long CLEANUP_INTERVAL = TimeUnit.DAYS.toMillis(1);
    final private static String DEFAULT_ROOT = Paths.get(System.getProperty("java.io.tmpdir"), "scratch").toString();

    final public static ScratchDirManager SCRATCH_DIR_FACTORY;

    static {
        try {
            SCRATCH_DIR_FACTORY = new ScratchDirManager();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private long lastCleanup;
    private Path root;
    private long ttl;

    public ScratchDirManager(Path scratchDir, long ttl) throws IOException {
        root = scratchDir;
        Files.createDirectories(root);
        this.ttl = ttl;
    }

    public ScratchDirManager() throws IOException {
        this(Paths.get(DataService.getDataDir()), getScratchTTL());
    }

    public File getNewScratchDir() throws IOException {
        return getNewScratchDir(null);
    }

    public File getNewScratchDir(String servicePrefix) throws IOException {
        cleanup();

        return getNewScratchDir0(servicePrefix);
    }

    protected File getNewScratchDir0(String servicePrefix) throws IOException {
        Path scratch = root;
        if (servicePrefix != null)
            scratch = root.resolve(servicePrefix);
        scratch = scratch.resolve(UUID.randomUUID().toString());
        Files.createDirectories(scratch);
        return scratch.toFile();
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        if (lastCleanup + CLEANUP_INTERVAL < now) {
            lastCleanup = now;
            cleanup0(ttl);
        }
    }

    protected void cleanup0(long cutOffTime) {
        Thread t = new Thread(new CleanupThread(cutOffTime), "scratch-cleanup");
        t.setDaemon(true);
        t.start();
    }

    public void cleanup1(long cutOffTime) {
        if (!Files.exists(root))
            return;

        try (DirectoryStream<Path> directoryStream = java.nio.file.Files.newDirectoryStream(root)) {
            for (Path file : directoryStream) {
                boolean remove = Files.isDirectory(file) && isCutoffTime(file, cutOffTime);

                if (!remove)
                    continue;

                try {
                    log.info("Removing {}", file);
                    FileUtils.deleteDirectory(file.toFile());
                } catch (IOException e) {
                    log.warn("Could not remove directory {}", file, e);
                }

            }
        } catch (IOException e) {
            log.warn("Could not read contents of directory {}", root);
        }
    }

    private boolean isCutoffTime(Path path, long cutOffTime) throws IOException {
        ZonedDateTime now = ZonedDateTime.now();
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        ZonedDateTime created = ZonedDateTime.ofInstant(attrs.creationTime().toInstant(), now.getZone());

        created = created.withYear(now.getYear()).withMonth(now.getMonthValue());

        ZonedDateTime cutoff = created.plus(cutOffTime, MILLIS);

        log.info("Created {}", created);
        log.info("now     {}", now);
        log.info("cutoff  {}", cutoff);

        return now.isAfter(cutoff);
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
            cleanup1(cutOffTime);
        }
    }
}
