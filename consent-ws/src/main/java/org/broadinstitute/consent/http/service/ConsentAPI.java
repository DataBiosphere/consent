package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;

import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;

public interface ConsentAPI {

    Consent create(Consent rec);

    Consent retrieve(String id) throws UnknownIdentifierException;

    Collection<Consent> findConsentsByAssociationType(String associationType);

    Collection<Consent> retrieve(List<String> ids);
    
    Consent getByName(String name) throws UnknownIdentifierException;

    Consent update(String id, Consent rec) throws UnknownIdentifierException;

    /**
     * This isn't actually used in the web services at the moment, but i'm including it for
     * completeness sake.
     *
     * @param id the identifier of the consent to delete (or inactivate)
     * @throws UnknownIdentifierException If the identifier names an inactive or non-existent consent
     *                                    in the database.
     */
    void logicalDelete(String id) throws UnknownIdentifierException;

    void delete(String id) throws IllegalArgumentException;

    // ConsentAssociation methods

    List<ConsentAssociation> createAssociation(String consentId, List<ConsentAssociation> new_associations, String modifiedByUserEmail);

    List<ConsentAssociation> updateAssociation(String consentId, List<ConsentAssociation> new_associations, String modifiedByUserEmail);

    List<ConsentAssociation> getAssociation(String consentId, String associationType, String objectId);

    List<ConsentAssociation> deleteAssociation(String consentId, String associationType, String objectId);

    List<String> getConsentsForAssociation(UriInfo uriInfo, String associationType, String objectId);

    /**
     * This method finds the consent related to the datasetId sent as a parameter, by joining the
     * consentassociation table with the consent table.
     *
     * @param datasetId the identifier data set
     */
    Consent getConsentFromDatasetID(String datasetId);

    // Data Use Letter methods.
    Consent updateConsentDul(String consentId, String dataUseLetter, String dulName) throws UnknownIdentifierException;

    String getConsentDulUrl(String consentId) throws UnknownIdentifierException;

    Consent deleteConsentDul(String consentId) throws UnknownIdentifierException;

    List<ConsentManage> describeConsentManage();

    Integer getUnReviewedConsents();

    List<UseRestrictionDTO> getInvalidConsents();

    Consent getConsentFromObjectIdAndType(String objectId, String associationType);

    /**
     * Method to check if a workspace is already associated with a consentId
     * @param workspaceId
     * @return
     */
    boolean hasWorkspaceAssociation(String workspaceId);

}
