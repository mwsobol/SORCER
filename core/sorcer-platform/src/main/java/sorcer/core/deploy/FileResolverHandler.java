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
package sorcer.core.deploy;

import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Dennis Reedy
 */
public class FileResolverHandler {
    private static final Logger logger = LoggerFactory.getLogger(FileResolverHandler.class);
    private static final Map<String, File> resolvedArtifacts = new ConcurrentHashMap<>();
    private static final BlockingQueue<ResolveRequest> resolverQueue = new LinkedBlockingDeque<>();
    private static final ExecutorService futureExecutor = Executors.newCachedThreadPool();
    private static final ExecutorService execService = Executors.newSingleThreadExecutor();
    static {
        execService.submit(new ResolverRunner());
    }

    File getFile(String artifact) throws ResolverException {
        File file = resolvedArtifacts.get(artifact);
        if (file == null) {
            ResolverFutureTask resolverFutureTask = new ResolverFutureTask();
            ResolveRequest resolveRequest = new ResolveRequest(artifact, resolverFutureTask);
            futureExecutor.submit(resolverFutureTask);
            resolverQueue.offer(resolveRequest);
            try {
                file = resolverFutureTask.call();
                return file;
            } catch (InterruptedException | ClassNotFoundException | IOException e) {
                throw new ResolverException("Failed to getValue artifact location", e);
            }
        } else {
            return file;
        }
    }

    private static class ResolveRequest {
        String artifactConfig;
        ResolverFutureTask resolverFutureTask;

        ResolveRequest(String artifactConfig, ResolverFutureTask resolverFutureTask) {
            this.artifactConfig = artifactConfig;
            this.resolverFutureTask = resolverFutureTask;
        }
    }

    private static class ResolverFutureTask implements Callable<File> {
        File file;
        CountDownLatch counter = new CountDownLatch(1);

        void setFile(File file) {
            this.file = file;
            counter.countDown();
        }

        public File call() throws InterruptedException, IOException, ClassNotFoundException {
            counter.await();
            return file;
        }
    }

    private static class ResolverRunner implements Runnable {

        @Override public void run() {
            try {
                Resolver resolver = ResolverHelper.getResolver();
                while (true) {
                    try {
                        ResolveRequest resolveRequest = resolverQueue.take();
                        File theFile;
                        File file = resolvedArtifacts.get(resolveRequest.artifactConfig);
                        if(file==null) {
                            logger.debug("Resolve {}", resolveRequest.artifactConfig);
                            Artifact artifact = new Artifact(resolveRequest.artifactConfig);
                            URL configLocation = resolver.getLocation(artifact.getGAV(), artifact.getType());
                            file = new File(configLocation.toURI());
                            
                            /* We need to handle groovy opstrings specially */
                            if(file.getName().endsWith(".groovy") && artifact.getClassifier().equals("opstring")) {
                                String name = String.format("%s_%s.groovy", artifact.getArtifactId().replace("-", "_"),
                                                            artifact.getClassifier());
                                theFile = new File(file.getParentFile(), name);
                                Files.copy(file.toPath(), theFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            } else {
                                theFile = file;
                            }
                            resolvedArtifacts.put(resolveRequest.artifactConfig, theFile);
                        } else {
                            theFile = file;
                        }
                        resolveRequest.resolverFutureTask.setFile(theFile);
                    } catch (InterruptedException | ResolverException | URISyntaxException | IOException e) {
                        logger.error("Unable to resolve artifact", e);
                    }
                }
            } catch(ResolverException e) {
                logger.error("Unable to getValue Resolver", e);
            }

        }
    }
}
