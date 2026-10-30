package org.broadinstitute.consent.http.models;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/** A row of study_biospecimen. */
public record StudyBiospecimenRecord(
    Integer biospecimenId,
    Integer studyId,
    String legacyKey,
    String sampleId,
    String donorId,
    String specimenType,
    String preservationMethod,
    String preservationDetails,
    String dateOfCollection,
    BigDecimal postMortemIntervalValue,
    String postMortemIntervalUnit,
    String sex,
    BigDecimal age,
    String race,
    String countryOfOrigin,
    List<String> extractedDiagnoses,
    String pathology,
    String organization,
    String sourceSite,
    String optionalDataUse,
    Timestamp createDate,
    Timestamp updateDate,
    Timestamp retiredDate) {}
