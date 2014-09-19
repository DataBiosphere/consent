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
package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.models.ConsentAssociation;
import org.genomebridge.consent.http.resources.ConsentResource;

import java.util.List;
import java.util.ArrayList;

public class DatabaseConsentAPI implements ConsentAPI {

    private ConsentDAO consentDAO;

    public DatabaseConsentAPI( ConsentDAO dao ) {
        this.consentDAO = dao;
    }

    @Override
    public void create(String id, ConsentResource rec) throws DuplicateIdentifierException {
        consentDAO.insertConsent(id, rec.requiresManualReview, rec.useRestriction.toString());
    }

    @Override
    public ConsentResource retrieve(String id) throws UnknownIdentifierException {
        return consentDAO.findConsentById(id);
    }

    @Override
    public void update(String id, ConsentResource rec) throws UnknownIdentifierException {
        consentDAO.updateConsent(id, rec.requiresManualReview, rec.useRestriction.toString());
    }

    @Override
    public void delete(String id) throws UnknownIdentifierException {
        consentDAO.deleteConsent(id);
    }

    // ConsentAssociation methods

    @Override
    public List<ConsentAssociation> createAssociation(String consentId, List<ConsentAssociation> new_associations) {
        return new_associations;
    }

    @Override
    public List<ConsentAssociation> updateAssociation(String consentId, List<ConsentAssociation> new_associations) {
        return new_associations;
    }

    @Override
    public List<ConsentAssociation> getAssociation(String consentId, String associationType, String objectId) {
        // STUB
        ArrayList<String> ids = new ArrayList<String>();
        ids.add("SM-1234");
        ArrayList<ConsentAssociation> temp = new ArrayList<ConsentAssociation>();
        temp.add(new ConsentAssociation("sample", ids));
        return temp;
    }

    @Override
    public List<ConsentAssociation> deleteAssociation(String consentId, String associationType, String objectId) {
        return new ArrayList<ConsentAssociation>();
    }
}
