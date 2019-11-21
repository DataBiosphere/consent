package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.authentication.GoogleUser;

import java.security.Principal;

public class AuthUser implements Principal {

    private String name;
    private GoogleUser googleUser;

    public AuthUser(String name) {
        this.name = name;
    }

    public AuthUser(GoogleUser googleUser) {
        this.name = googleUser.getEmail();
        this.googleUser = googleUser;
    }

    @Override
    public String getName() {
        return name;
    }

    public GoogleUser getGoogleUser() {
        return googleUser;
    }

}
