package org.broadinstitute.consent.http.models.dto.registration;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudyUpdateRequest extends StudyRegistrationRequest {}
