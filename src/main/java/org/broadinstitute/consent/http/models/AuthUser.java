package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.authentication.GoogleUser;

import java.security.Principal;

public class AuthUser implements Principal {

    private String name;
    private String authToken;
    private String email;
    private GoogleUser googleUser;

    public AuthUser() {
    }

    public AuthUser(String email) {
        this.email = email;
    }

    public AuthUser(GoogleUser googleUser) {
        this.email = googleUser.getEmail();
        this.googleUser = googleUser;
    }

    @Override
    public String getName() {
        return name;
    }

    public GoogleUser getGoogleUser() {
        return googleUser;
    }

    public String getAuthToken() {
        return authToken;
    }

    public AuthUser setAuthToken(String authToken) {
        this.authToken = authToken;
        return this;
    }

    public AuthUser setGoogleUser(GoogleUser googleUser) {
        this.googleUser = googleUser;
        return this;
    }

    public AuthUser setEmail(String email) {
        this.email = email;
        return this;
    }
}
