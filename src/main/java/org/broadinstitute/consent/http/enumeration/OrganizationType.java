package org.broadinstitute.consent.http.enumeration;

import liquibase.hub.model.Organization;

public enum OrganizationType {
    FOR_PROFIT("for_profit"),
    NON_PROFIT("non_profit");

    final String value;

    OrganizationType(String value) {
        this.value = value;
    }

    public static OrganizationType getOrganizationTypeFromString(String value) {
        for (OrganizationType e : OrganizationType.values()) {
            if (e.getValue().equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }


    public String getValue() {
        return value;
    }
}
