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
import org.apache.log4j.Logger;

/**
 * This class was used to do DI with GUICE, but we have removed that code due to some
 * issues with Guice+dropwizard and initializing singletons.  This class is left for
 * the skeleton, but can probably be removed (and removed from ConsentApplication).
 */
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
}
