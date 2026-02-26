package org.broadinstitute.consent.http.service.passport;

import java.util.List;

/**
 * <a href="https://ga4gh.github.io/data-security/ga4gh-passport">GA4GH Passport Claim</a>
 *
 * @param visas List of Visa objects representing the user's claims
 */
public record PassportClaim(List<Visa> visas) {}
