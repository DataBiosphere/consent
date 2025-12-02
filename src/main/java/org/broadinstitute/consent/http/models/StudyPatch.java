package org.broadinstitute.consent.http.models;

import java.util.List;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.StudyType;

public record StudyPatch(
    String name,
    StudyType studyType,
    String description,
    List<String> dataTypes,
    String phenotypeIndication,
    String species,
    String piName,
    List<String> dataCustodianEmail,
    String alternativeDataSharingPlanTargetDeliveryDate,
    String alternativeDataSharingPlanTargetPublicReleaseDate,
    Boolean publicVisibility) {}
