/**
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

import org.genomebridge.consent.http.resources.ConsentResource;
import org.genomebridge.consent.http.models.ConsentAssociation;

import java.util.List;

/**
 * CRUD!!
 */
public interface ConsentAPI {

    public void create(String id, ConsentResource rec) throws DuplicateIdentifierException;
    public ConsentResource retrieve( String id ) throws UnknownIdentifierException;
    public void update(String id, ConsentResource rec) throws UnknownIdentifierException;

    /**
     * This isn't actually used in the web services at the moment, but i'm including it for
     * completeness sake.
     *
     * @param id the identifier of the consent to delete (or inactivate)
     * @throws UnknownIdentifierException If the identifier names an inactive or non-existent consent
     * in the database.
     */
    public void delete(String id) throws UnknownIdentifierException;

    // ConsentAssociation methods

    public List<ConsentAssociation> createAssociation(String consentId, List<ConsentAssociation> new_associations);
    public List<ConsentAssociation> updateAssociation(String consentId, List<ConsentAssociation> new_associations);
    public List<ConsentAssociation> getAssociation(String consentId, String associationType, String objectId);
    public List<ConsentAssociation> deleteAssociation(String consentId, String associationType, String objectId);
}
