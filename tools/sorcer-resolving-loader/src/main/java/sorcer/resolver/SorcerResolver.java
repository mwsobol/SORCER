package sorcer.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * SORCER class
 * User: prubach
 * Date: 16.04.15
 */
public abstract class SorcerResolver {

    private static final Logger logger = LoggerFactory.getLogger(SorcerResolver.class);

    private static SorcerResolver instance;

    static {
        String sorcerResolvingLoaderClassName = null;

        try {
            sorcerResolvingLoaderClassName = System.getProperty("sorcer.resolver.class", null);

            if (sorcerResolvingLoaderClassName!=null) {
                logger.debug("SORCER Resolving Loader: " + sorcerResolvingLoaderClassName);
                Class type = Class.forName(sorcerResolvingLoaderClassName, true, Thread.currentThread().getContextClassLoader());
                if (!SorcerResolver.class.isAssignableFrom(type)) {
                    throw new IllegalArgumentException("Configured class must implement LoaderResolver: " + sorcerResolvingLoaderClassName);
                }
                instance = (SorcerResolver) type.newInstance();
            } else
                instance = new SorcerRioResolver();
        } catch (Exception e) {
            throw new RuntimeException("No SorcerResolver implementation or cannot load: " + sorcerResolvingLoaderClassName,e);
        }
    }

    public static SorcerResolver getInstance() {
        return instance;
    }

    public String[] doResolve(String artifact) throws SorcerResolverException {
        String[] cp = null;

        if (artifact.startsWith("artifact:")) {
            cp = resolveUrl(artifact);
        } else if (artifact.indexOf(':') >= 0) {
            cp = resolveCoords(artifact);
        }
        if (cp == null || cp.length == 0)
            throw new SorcerResolverException("Failed to resolve: " + artifact + " after 5 attempts");
        return cp;
    }

    public abstract String[] resolveUrl(String artifact);

    public abstract String[] resolveCoords(String coords);

    public abstract URL getLocation(String path) throws SorcerResolverException;

    public static URL[] toURLs(String[] filePaths) throws MalformedURLException {
        URL[] result = new URL[filePaths.length];
        for (int i = 0; i < filePaths.length; i++) {
            result[i] = toURI(filePaths[i]).toURL();
        }
        return result;
    }

    public static URI toURI(String filePath) {
        return new File(filePath).toURI();
    }

    public static URI[] toURIs(String[] filePaths) throws URISyntaxException {
        URI[] result = new URI[filePaths.length];
        for (int i = 0; i < filePaths.length; i++) {
            result[i] = toURI(filePaths[i]);
        }
        return result;
    }
}
