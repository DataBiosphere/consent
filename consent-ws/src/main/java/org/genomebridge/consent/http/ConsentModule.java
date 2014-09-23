/*
 * Copyright 2014 Broad Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.genomebridge.consent.http;

import com.google.inject.AbstractModule;
// import com.google.inject.Provides;
import org.genomebridge.consent.http.service.ConsentAPIProvider;
import org.genomebridge.consent.http.service.ConsentAPI;
import org.apache.log4j.Logger;

// import java.util.logging.Logger;


public class ConsentModule extends AbstractModule {

    private static Logger _logger;
    private Logger logger() {
        if (_logger == null) {
            _logger = Logger.getLogger("ConsentModule");
        }
        return _logger;
    }

    @Override
    protected void configure() {
    }

    // We are continuing to use Guice DI to inject the API object where required, but we are
    // managing the singleton object ourselves, through the ConsentAPIProvider class, to work
    // around a problem with Guice+dropwizard (see ConsentAPIProvider).
/*    @Provides
    public ConsentAPI providesAPI() {
        ConsentAPI api = ConsentAPIProvider.getApi();
        logger().info(String.format("*** providesAPI() called, api='%s'", (api==null?"null":api.toString())));
//        return ConsentAPIProvider.getApi();
        return api;
    }
 */
}
