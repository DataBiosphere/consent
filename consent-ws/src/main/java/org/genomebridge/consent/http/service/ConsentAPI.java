package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.ConsentAssociation;

import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * CRUD!!
 */
public interface ConsentAPI {

    public void create(String id, Consent rec) throws DuplicateIdentifierException;
    public Consent retrieve( String id ) throws UnknownIdentifierException;
    public void update(String id, Consent rec) throws UnknownIdentifierException;

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
    public List<String> getConsentsForAssociation(UriInfo uriInfo, String associationType, String objectId);
}
