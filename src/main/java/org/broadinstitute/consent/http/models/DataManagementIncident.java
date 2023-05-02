package org.broadinstitute.consent.http.models;

import java.util.List;

public record DataManagementIncident(List<String> incidents, String description) {

}
