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


import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import sorcer.service.EvaluationException;

import java.io.File;
import java.io.IOException;

/**
 * @author Rafał Krupiński
 */
abstract public class AbstractRemoteFile implements RemoteFile {
    protected final String checksum;
    protected final static HashFunction hf = Hashing.sha1();

    protected AbstractRemoteFile(File localFile) throws IOException {
        this.checksum = checksum(localFile);
    }

    abstract protected File doGetFile() throws IOException;

    abstract protected File getLocalPath();

    protected static String checksum(File localFile) throws IOException {
        return Files.hash(localFile, hf).toString();
    }

    @Override
    public File getValue() throws EvaluationException {
        try {
            File result = doGetFile();
            String myChecksum = checksum(result);
            if (!checksum.equals(myChecksum))
                throw new IllegalStateException("File exists but has invalid checksum");
            return result;
        } catch (IOException e) {
            throw new EvaluationException("Error getting file", e);
        }
    }
}
