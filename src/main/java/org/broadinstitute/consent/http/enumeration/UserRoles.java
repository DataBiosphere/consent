package org.broadinstitute.consent.http.enumeration;

import org.broadinstitute.consent.http.resources.Resource;

public enum UserRoles {

    MEMBER(Resource.MEMBER),
    CHAIRPERSON(Resource.CHAIRPERSON),
    ALUMNI(Resource.ALUMNI),
    ADMIN(Resource.ADMIN),
    RESEARCHER(Resource.RESEARCHER),
    DATAOWNER(Resource.DATAOWNER);

    private String value;

    UserRoles(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String getValue(String value) {
        for (UserRoles e : UserRoles.values()) {
            if (e.getValue().equalsIgnoreCase(value)) {
                return e.getValue();
            }
        }
        return null;
    }

}
