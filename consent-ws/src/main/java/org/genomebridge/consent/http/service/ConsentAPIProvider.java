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

package org.genomebridge.consent.http.service;


/**
 * Created by egolin on 9/23/14.
 * Explicit code to manage a singleton ConsentAPI object, to get around a GUICE/Dropwizard issue.
 * We manage this singleton ourselves, instead of relying on dependency injection, due to problems
 * with Dropwizard + Guice lifecycle. See https://github.com/HubSpot/dropwizard-guice/issues/19 for discussion
 * of those lifecycle problems.
 */
public class ConsentAPIProvider {

    // singleton management code
    private ConsentAPIProvider() {}

    private static class ConsentAPIProviderHolder {
        public static ConsentAPIProvider INSTANCE = new ConsentAPIProvider();
    }

    public static ConsentAPIProvider getInstance() {
        return ConsentAPIProviderHolder.INSTANCE;
    }

    // Manage the ConsentAPI object
    private static ConsentAPI _api;

    public static void setApi(ConsentAPI api) {
        _api = api;
    }

    public static ConsentAPI getApi() {
        return _api;
    }
}
