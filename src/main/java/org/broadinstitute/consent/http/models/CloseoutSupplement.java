package org.broadinstitute.consent.http.models;

import java.util.List;

public record CloseoutSupplement(
    List<String> reasons, String otherText, Integer signingOfficialId) {}
