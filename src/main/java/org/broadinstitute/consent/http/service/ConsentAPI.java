package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;

import java.util.List;

public interface ConsentAPI {

    Consent create(Consent rec);

    Consent retrieve(String id) throws UnknownIdentifierException;

    Consent getByName(String name) throws UnknownIdentifierException;

    Consent update(String id, Consent rec) throws UnknownIdentifierException;

    void delete(String id) throws IllegalArgumentException;

    // ConsentAssociation methods

    List<ConsentAssociation> createAssociation(String consentId, List<ConsentAssociation> new_associations, String modifiedByUserEmail);

    List<ConsentAssociation> updateAssociation(String consentId, List<ConsentAssociation> new_associations, String modifiedByUserEmail);

    List<ConsentAssociation> getAssociation(String consentId, String associationType, String objectId);

    List<ConsentAssociation> deleteAssociation(String consentId, String associationType, String objectId);

    /**
     * This method finds the consent related to the datasetId sent as a parameter, by joining the
     * consentassociation table with the consent table.
     *
     * @param datasetId the identifier data set
     */
    Consent getConsentFromDatasetID(Integer datasetId);

    // Data Use Letter methods.
    Consent updateConsentDul(String consentId, String dataUseLetter, String dulName) throws UnknownIdentifierException;

    String getConsentDulUrl(String consentId) throws UnknownIdentifierException;

    Consent deleteConsentDul(String consentId) throws UnknownIdentifierException;

    List<UseRestrictionDTO> getInvalidConsents();

    Consent getConsentFromObjectIdAndType(String objectId, String associationType);

    /**
     * Method to check if a workspace is already associated with a consentId
     * @param workspaceId
     * @return
     */
    boolean hasWorkspaceAssociation(String workspaceId);

    Election retrieveElection(Integer electionId, String consentId);

}
