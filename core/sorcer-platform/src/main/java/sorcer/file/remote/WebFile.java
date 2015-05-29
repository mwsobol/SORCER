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

import com.google.common.io.Resources;
import sorcer.file.ScratchDirManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

/**
 * Remote file that is copied over web using webster data appliance.
 *
 * @author Rafał Krupiński
 */
public class WebFile extends AbstractRemoteFile implements Serializable {
    private static final long serialVersionUID = -3333474650265576280L;
    private URL remoteUrl;

    public WebFile(File localFile, URL remoteUrl) throws IOException {
        super(localFile);
        this.remoteUrl = remoteUrl;
    }

    @Override
    protected File doGetFile() throws IOException {
        File localFile = getLocalPath();
        try (FileOutputStream local = new FileOutputStream(localFile)) {
            Resources.copy(remoteUrl, local);
        }
        return localFile;
    }

    @Override
    protected File getLocalPath() throws IOException {
        File parent = new ScratchDirManager().getNewScratchDir("remote-file");
        return new File(parent, checksum);
    }
}
