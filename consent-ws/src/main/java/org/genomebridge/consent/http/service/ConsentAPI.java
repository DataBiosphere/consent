package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.ConsentAssociation;
import org.genomebridge.consent.http.models.ConsentManage;

import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;

public interface ConsentAPI {

    Consent create(Consent rec);

    Consent retrieve(String id) throws UnknownIdentifierException;

    Collection<Consent> findConsentsByAssociationType(String associationType);

    Collection<Consent> retrieve(List<String> ids);

    void update(String id, Consent rec) throws UnknownIdentifierException;

    /**
     * This isn't actually used in the web services at the moment, but i'm including it for
     * completeness sake.
     *
     * @param id the identifier of the consent to delete (or inactivate)
     * @throws UnknownIdentifierException If the identifier names an inactive or non-existent consent
     *                                    in the database.
     */
    void delete(String id) throws UnknownIdentifierException;

    // ConsentAssociation methods

    List<ConsentAssociation> createAssociation(String consentId, List<ConsentAssociation> new_associations);

    List<ConsentAssociation> updateAssociation(String consentId, List<ConsentAssociation> new_associations);

    List<ConsentAssociation> getAssociation(String consentId, String associationType, String objectId);

    List<ConsentAssociation> deleteAssociation(String consentId, String associationType, String objectId);

    List<String> getConsentsForAssociation(UriInfo uriInfo, String associationType, String objectId);

    // Data Use Letter methods.
    Consent updateConsentDul(String consentId, String dataUseLetter) throws UnknownIdentifierException;

    String getConsentDulUrl(String consentId) throws UnknownIdentifierException;

    Consent deleteConsentDul(String consentId) throws UnknownIdentifierException;

    List<ConsentManage> describeConsentManage();

}
