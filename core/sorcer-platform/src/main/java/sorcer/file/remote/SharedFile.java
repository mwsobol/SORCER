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

package sorcer.file.remote;

import java.io.File;
import java.io.IOException;

/**
 * Remote file that uses shared disk for access. File is not copied upon access, but the checksum is verified upon accessing the File object.
 *
 * @author Rafał Krupiński
 */
public class SharedFile extends AbstractRemoteFile {
    private File sharedFile;

    protected SharedFile(File localFile) throws IOException {
        super(localFile);
        setLocalFile(localFile);
    }

    protected void setLocalFile(File localFile) throws IOException {
        this.sharedFile = localFile;
    }

    @Override
    protected File doGetFile() throws IOException {
        return getLocalPath();
    }

    @Override
    protected File getLocalPath() {
        return sharedFile;
    }
}
