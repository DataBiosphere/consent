package org.genomebridge.consent.http;

import java.net.URL;

/**
 * Abstract super-class for tests, that allows easy loading of resources from the classpath.
 */
abstract public class ResourcedTest {

    public static String resourceFilePath(String name) {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        URL resource = loader.getResource(name);
        if (resource == null) {
            throw new IllegalStateException("Couldn't find resource " + name);
        }
        return resource.getFile();
    }
}
