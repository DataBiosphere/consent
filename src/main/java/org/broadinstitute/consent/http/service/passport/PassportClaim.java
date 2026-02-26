package org.broadinstitute.consent.http.service.passport;

import java.util.List;

/**
 * <a href="https://ga4gh.github.io/data-security/ga4gh-passport">GA4GH Passport Claim</a> Visas
 * encoded in a PassportClaim are represented as a list of Visa objects wrapped by a claim key of:
 * `ga4gh_passport_v1`.
 *
 * @param ga4gh_passport_v1 List of Visa objects representing the user's claims
 */
public record PassportClaim(List<Visa> ga4gh_passport_v1) {}
