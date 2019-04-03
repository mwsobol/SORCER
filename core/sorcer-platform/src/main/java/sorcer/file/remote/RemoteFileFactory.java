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

package sorcer.file.remote;

import com.google.common.io.Files;
import sorcer.data.DataService;
import sorcer.util.IOUtils;
import sorcer.util.Sorcer;

import java.io.File;
import java.io.IOException;

/**
 * A factory that creates RemoteFile instances based on whether source file is placed in a directory marked as shared.
 *
 * File placed in shared directories cause the factory to return {@link SharedFile}.
 * Files placed in other directories are copied to data directory, and upon request copied over HTTP.
 *
 * @author Rafał Krupiński
 */
public class RemoteFileFactory {

    public static final RemoteFileFactory INST;

    static {
        try {
            INST = new RemoteFileFactory(new File(DataService.getDataDir()), Sorcer.getSharedDirs());
        } catch (IOException e) {
            throw new IllegalStateException("Invalid configuration", e);
        }
    }

    private File[] shareRoots;

    // webster data directory
    private File dataDir;

    public RemoteFileFactory(File data, File[] shareRoots) throws IOException {
        this.dataDir = data;
        this.shareRoots = new File[shareRoots.length];
        for (int i = 0; i < shareRoots.length; i++)
            this.shareRoots[i] = shareRoots[i].getCanonicalFile();
    }

    public RemoteFile forFile(File f) throws IOException {
        File file = f.getCanonicalFile();
        for (File shareRoot : shareRoots) {
            if (IOUtils.isChild(shareRoot, file))
                return new SharedFile(file);
        }
        return createWebFile(file);
    }

    private WebFile createWebFile(File file) throws IOException {
        File published = file;
        if (!IOUtils.isChild(dataDir, file)) {
            // TODO instead of copying a potentially large file, just register it with Webster instance
            published = dataDir.toPath().resolve(file.toPath().getFileName()).toFile();
            Files.copy(file, published);
        }

        return new WebFile(published, Sorcer.getDataURL(published));
    }

}
