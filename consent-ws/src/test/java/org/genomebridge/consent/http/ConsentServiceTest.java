/*
 * Copyright 2014 Broad Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.genomebridge.consent.http;

import com.sun.jersey.api.client.Client;
import org.genomebridge.consent.http.models.Consent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class ConsentServiceTest extends AbstractTest {

    public String consentPath() { return path2Url("/consent"); }

    public String consentPath(String id) {
        try {
            return path2Url(String.format("/consent/%s", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("/consent/%s", id);
        }
    }

    public Consent retrieveConsent(Client client, String url) {
        return get(client, url).getEntity(Consent.class);
    }

}
