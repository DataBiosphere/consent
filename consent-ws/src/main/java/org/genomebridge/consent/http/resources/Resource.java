package org.genomebridge.consent.http.resources;

import org.apache.log4j.Logger;

/**
 * Created by egolin on 9/17/14.
 * <p/>
 * Abstract superclass for all Resources.
 */
abstract public class Resource {

    protected Logger logger() {
        return Logger.getLogger("consent-ws");
    }
}
