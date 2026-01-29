package org.broadinstitute.consent.http.models;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.ClinicalTrialInterventionType;
import org.broadinstitute.consent.http.enumeration.ClinicalTrialPhase;
import org.broadinstitute.consent.http.enumeration.ClinicalTrialStatus;

public record ClinicalTrial(
    String clinicalTrialId,
    String studyId,
    String title,
    String registry,
    String identifier,
    ClinicalTrialStatus status,
    String sponsor,
    LocalDate startDate,
    LocalDate endDate,
    ClinicalTrialInterventionType interventionType,
    String description,
    ClinicalTrialPhase phase,
    URI url,
    List<String> tags) {}
