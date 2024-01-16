package org.broadinstitute.consent.http.models;

public record OidcAuthorityConfiguration(String issuer, String authorization_endpoint, String token_endpoint) {}
