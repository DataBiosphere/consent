package org.broadinstitute.consent.http.authentication;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GoogleUser {

    private String sub;
    private String name;
    private String givenName;
    private String familyName;
    private String profile;
    private String picture;
    private String email;
    private Boolean emailVerified;
    private String locale;
    private String hd;

    public GoogleUser() {}

    /**
     * Convenience method to deserialize from Google's userinfo API, e.g.:
     * {
     *   "sub": "...",
     *   "name": "Gregory Rushton",
     *   "given_name": "Gregory",
     *   "family_name": "Rushton",
     *   "profile": "https://plus.google.com/...",
     *   "picture": "https://lh3.googleusercontent.com/...",
     *   "email": "grushton@broadinstitute.org",
     *   "email_verified": true,
     *   "locale": "en",
     *   "hd": "broadinstitute.org"
     * }
     *
     * @param json The JSON string to deserialize
     */
    GoogleUser(String json) {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        GoogleUser u = gson.fromJson(json, GoogleUser.class);
        this.setSub(u.getSub());
        this.setName(u.getName());
        this.setGivenName(u.getGivenName());
        this.setFamilyName(u.getFamilyName());
        this.setProfile(u.getProfile());
        this.setPicture(u.getPicture());
        this.setEmail(u.getEmail());
        this.setEmailVerified(u.getEmailVerified());
        this.setLocale(u.getLocale());
        this.setHd(u.getHd());
    }

    private String getSub() {
        return sub;
    }

    private void setSub(String sub) {
        this.sub = sub;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String getGivenName() {
        return givenName;
    }

    private void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    private String getFamilyName() {
        return familyName;
    }

    private void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    private String getProfile() {
        return profile;
    }

    private void setProfile(String profile) {
        this.profile = profile;
    }

    private String getPicture() {
        return picture;
    }

    private void setPicture(String picture) {
        this.picture = picture;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    private Boolean getEmailVerified() {
        return emailVerified;
    }

    private void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    private String getLocale() {
        return locale;
    }

    private void setLocale(String locale) {
        this.locale = locale;
    }

    private String getHd() {
        return hd;
    }

    private void setHd(String hd) {
        this.hd = hd;
    }

}
