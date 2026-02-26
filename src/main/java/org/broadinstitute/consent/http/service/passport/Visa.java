package org.broadinstitute.consent.http.service.passport;

/**
 * VisaClaims encoded in a Visa are represented as a list of Visa objects wrapped by a claim key of:
 * `ga4gh_visa_v1`.
 *
 * @param iss The issuer of the Visa, typically the organization that issued the Visa to the user
 * @param sub The subject of the Visa, typically the user to whom the Visa was issued
 * @param iat The issued at time of the Visa, represented as a Unix timestamp in seconds
 * @param exp The expiration time of the Visa, represented as a Unix timestamp in seconds
 * @param ga4gh_visa_v1 The VisaClaim representing the user's claim
 */
public record Visa(String iss, String sub, Long iat, Long exp, VisaClaim ga4gh_visa_v1) {}
