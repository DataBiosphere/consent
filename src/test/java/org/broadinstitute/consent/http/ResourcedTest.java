package org.broadinstitute.consent.http;

import com.google.common.io.Resources;

import java.net.URL;

/**
 * Abstract super-class for tests, that allows easy loading of resources from the classpath.
 */
abstract public class ResourcedTest {

    public static String resourceFilePath(String name) {
        URL resource = Resources.getResource(name);
        if (resource == null) {
            throw new IllegalStateException("Couldn't find resource " + name);
        }
        return resource.getFile();
    }
}
