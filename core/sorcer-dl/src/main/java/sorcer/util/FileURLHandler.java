/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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
package sorcer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Mike Sobolewsko on 6/19/15 based on the DataService class
 */
public interface FileURLHandler {


        /**
         * Get a {@link URL} for a file returnPath.
         *
         * @param path The file returnPath to obtain a URL for.
         *
         * @return A URL that can be used to access the file.
         *
         * @throws IOException if the file does not exist, or the URL cannot be created.
         * @throws IllegalArgumentException if the file cannot be accessed from one of the roots provided.
         * @throws IllegalStateException if the data service is not running.
         */
        public URL getDataURL(final String path) throws IOException;

        /**
         * Get a {@link URL} for a file.
         *
         * @param file The file to obtain a URL for.
         *
         * @return A URL that can be used to access the file.
         *
         * @throws IOException if the file does not exist, or the URL cannot be created.
         * @throws IllegalArgumentException if the file cannot be accessed from one of the roots provided.
         * @throws IllegalStateException if the data service is not available.
         */
        public URL getDataURL(final File file) throws IOException;
        /**
         * Download the contents of a URL to a local file
         *
         * @param url The URL to download
         * @param to The file to download to
         *
         * @throws IOException
         */
        public void download(final URL url, final File to) throws IOException;

        /**
         * Get a File from a URL.
         *
         * @param url The URL to use
         *
         * @return a File derived from the DataService data directory root(s).
         *
         * @throws FileNotFoundException If the URL cannot be accessed from one of the roots provided.
         */
        public File getDataFile(final URL url) throws IOException;


        /**
         * Get the DataService data directory. The {@link DataService#DATA_DIR} system property is first
         * consulted, if that property is not set, the default of
         * System.getProperty("java.io.tmpdir")/sorcer/user/data is used and the {@link DataService#DATA_DIR}
         * system property is set.
         *
         * @return The DataService data directory.
         */
        public String getDir();

        /**
         * Get the execute of the DATA_URL system property
         *
         * @return The execute of the DATA_URL system property
         */
        public String getDataUrl();

    }

