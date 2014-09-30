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

import com.sun.jersey.api.NotFoundException;
import org.apache.log4j.Logger;
import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.models.ConsentAssociation;
import org.genomebridge.consent.http.resources.ConsentResource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation class for ConsentAPI on top of ConsentDAO database support.
 */
public class DatabaseConsentAPI implements ConsentAPI {

    private ConsentDAO consentDAO;
    private Logger logger;

    public DatabaseConsentAPI(ConsentDAO dao) {
        this.consentDAO = dao;
        this.logger = Logger.getLogger("DatabaseConsentAPI");
    }

    // Consent Methods

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

    // POST /consent/:consentid/association <body>=List<ConsentAssociation>
    // Create new associations for a consent.  For each ConsentAssociation specified, remove the previous
    // association and create the new one.
    @Override
    public List<ConsentAssociation> createAssociation(String consentId, List<ConsentAssociation> new_associations) {
        logger.trace(String.format("createAssociation consentId='%s' %d associations supplied",
                consentId, new_associations.size()));
        checkConsentExists(consentId);
        validateAssociations(new_associations);

        for (ConsentAssociation association : new_associations) {
            logger.debug(String.format("CreateAssociation, adding associations for '%s', %d ids supplied",
                    association.getAssociationType(), association.getElements().size()));
            // The following two operations should really be done in a transaction.
            consentDAO.deleteAllAssociationsForType(consentId, association.getAssociationType());
            consentDAO.insertAssociations(consentId, association.getAssociationType(), association.getElements());
        }
        return getAllAssociationsForConsent(consentId);
    }

    // PUT /consent/:consentid/association <body>=List<ConsentAssociaiton>
    // Update associations for a consent by adding new associations.  For each ConsentAssociation specified, all
    // the objects specified are added as consent associations.
    @Override
    public List<ConsentAssociation> updateAssociation(String consentId, List<ConsentAssociation> new_associations) {
        logger.trace(String.format("updateAssociation consentId='%s' associations(%d)= '%s'",
                consentId, new_associations.size(), new_associations.toString()));
        checkConsentExists(consentId);
        validateAssociations(new_associations);

        // Loop over all the ConsentAssociations sent in the body.
        for (ConsentAssociation association : new_associations) {
            String atype = association.getAssociationType();
            List<String> new_ids = association.getElements();
            logger.debug(String.format("updateAssociation, adding associations for '%s', ids(cnt=%d) are '%s'",
                    atype, new_ids.size(), new_ids.toString()));
            // First retrieve the existing associations for that type.
            List<String> old_ids = consentDAO.findAssociationsByType(consentId, atype);
            // Remove any objectId's that already exist
            new_ids.removeAll(old_ids);
            // and add the new associations
            consentDAO.insertAssociations(consentId, atype, new_ids);
        }

        return getAllAssociationsForConsent(consentId);
    }

    @Override
    public List<ConsentAssociation> getAssociation(String consentId, String associationType, String objectId) {
        logger.trace(String.format("getAssociation consentId='%s' associationType='%s', objectId='%s'",
                consentId, associationType, objectId));

        checkConsentExists(consentId);
        List<ConsentAssociation> result;
        if (associationType == null)
            result = getAllAssociationsForConsent(consentId);
        else {
            result = new ArrayList<ConsentAssociation>();
            List<String> id_list;
            if (objectId == null)
                id_list = consentDAO.findAssociationsByType(consentId, associationType);
            else { // both associationType and objectId specified
                id_list = new ArrayList<String>();
                if (consentDAO.findAssociationByTypeAndId(consentId, associationType, objectId) != null)
                    id_list.add(objectId);
            }
            result.add(new ConsentAssociation(associationType, id_list));
        }
        return result;
    }

    @Override
    public List<ConsentAssociation> deleteAssociation(String consentId, String associationType, String objectId) {
        logger.trace(String.format("deleteAssociation consentId='%s' associationType='%s', objectId='%s'",
                consentId, (associationType == null ? "<null>" : associationType),
                (objectId == null ? "<null>" : objectId)));

        checkConsentExists(consentId);
        if (associationType == null)
            consentDAO.deleteAllAssociationsForConsent(consentId);
        else if (objectId == null)
            consentDAO.deleteAllAssociationsForType(consentId, associationType);
        else { // both associationType and objectId specified
            if (consentDAO.findAssociationByTypeAndId(consentId, associationType, objectId) == null)
                throw new NotFoundException();
            consentDAO.deleteOneAssociation(consentId, associationType, objectId);
        }
        return getAllAssociationsForConsent(consentId);

    }

    @Override
    public List<String> getConsentsForAssociation(UriInfo uriInfo, String associationType, String objectId) {
        // We get a list of consentId's (UUID's) from the database, and turn them into URIs
        // <base-service-url>/consent/{id}
        List<String> consent_ids = consentDAO.findConsentsForAssociation(associationType, objectId);
        List<String> consent_uris = new ArrayList<String>(consent_ids.size());
        for (String consentId : consent_ids) {
            UriBuilder ub = uriInfo.getBaseUriBuilder();
            URI consentUri = ub.path("consent").path(consentId).build();
            consent_uris.add((consentUri.getPath()));
        }
        logger.debug(String.format("getConsentsForAssociation(%s,%s) returning '%s'", associationType, objectId, consent_uris.toString()));
        return consent_uris;
    }

    // Helper methods for Consent Associations
    //
    // Check that the list of ConsentAssociations given as an agrument is valid, and throw BadRequestException
    // if not.  The only error checked for is duplicate associationType.
    private void validateAssociations(List<ConsentAssociation> assoc_list) {
        if (assoc_list.size() > 1) {
            Set<String> atype_list = new HashSet<String>();
            for (ConsentAssociation assoc : assoc_list) {
                if (atype_list.contains(assoc.getAssociationType()))
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                atype_list.add(assoc.getAssociationType());
            }
        }
    }

    // Check that the specified Consent resource exists, or throw NotFoundException.
    private void checkConsentExists(String consentId) {
        String ck_id = consentDAO.checkConsentbyId(consentId);
        logger.debug(String.format("CreateAssocition, checkConsentbyId returned '%s'", (ck_id == null ? "<null>" : ck_id)));
        if (ck_id == null)
            throw new NotFoundException(String.format("Consent with id '%s' not found", consentId));
    }

    // Get the updated list of all the associations for a Consent Resource, to return as a result.
    private List<ConsentAssociation> getAllAssociationsForConsent(String consentId) {
        List<ConsentAssociation> assoc_list = new ArrayList<ConsentAssociation>();
        List<String> type_list = consentDAO.findAssociationTypesForConsent(consentId);
        logger.debug(String.format("getAllAssociationsForConsent consentId='%s', types='%s'", consentId, type_list.toString()));
        for (String atype : type_list) {
            List<String> id_list = consentDAO.findAssociationsByType(consentId, atype);
            logger.debug(String.format("getAllAssociationsForConsent adding %d ids to type '%s'", id_list.size(), atype));
            ConsentAssociation next_assoc = new ConsentAssociation(atype, id_list);
            assoc_list.add(next_assoc);
        }
        logger.debug(String.format("getAllAssociationsForConsent - returning '%s'", assoc_list.toString()));
        return assoc_list;
    }


}
