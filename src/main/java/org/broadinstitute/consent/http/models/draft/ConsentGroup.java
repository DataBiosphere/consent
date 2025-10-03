package org.broadinstitute.consent.http.models.draft;

import java.net.URI;
import java.util.List;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;

public record ConsentGroup(
    Integer datasetId,
    String datasetIdentifier,
    String consentGroupName,
    AccessManagement accessManagement,
    Boolean generalResearchUse,
    Boolean hmb,
    List<String> diseaseSpecificUse,
    Boolean poa,
    String otherPrimary,
    Boolean nmds,
    Boolean gso,
    Boolean pub,
    Boolean col,
    Boolean irb,
    String gs,
    Boolean mor,
    String morDate,
    Boolean npu,
    String otherSecondary,
    Integer dataAccessCommitteeId,
    DataLocation dataLocation,
    URI url,
    Integer numberOfParticipants,
    List<FileTypeObject> fileTypes
) {}
