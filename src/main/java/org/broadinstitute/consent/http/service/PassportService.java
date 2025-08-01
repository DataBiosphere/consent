package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.passport.AffiliationAndRole;
import org.broadinstitute.consent.http.models.passport.ControlledAccessGrants;
import org.broadinstitute.consent.http.models.passport.PassportClaim;
import org.broadinstitute.consent.http.models.passport.Visa;
import org.broadinstitute.consent.http.models.passport.VisaClaim;
import org.broadinstitute.consent.http.models.passport.VisaClaimType;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.util.ConsentLogger;

/**
 * <a href="https://ga4gh.github.io/data-security/ga4gh-passport">GA4GH Passport</a>
 */
public class PassportService implements ConsentLogger {

  public static final String ISS = "https://duos.org";

  private final DatasetDAO datasetDAO;
  private final UserDAO userDAO;
  private final SamDAO samDAO;

  @Inject
  public PassportService(DatasetDAO datasetDAO, UserDAO userDAO, SamDAO samDAO) {
    this.datasetDAO = datasetDAO;
    this.userDAO = userDAO;
    this.samDAO = samDAO;
  }

  // TODO: Flesh out:
  // * AcceptedTermsAndPolicies
  // * ResearcherStatus
  // * LinkedIdentities
  public PassportClaim generatePassport(AuthUser authUser) throws Exception {
    User user = userDAO.findUserByEmail(authUser.getEmail());
    if (user == null) {
      return new PassportClaim(List.of());
    }
    UserStatusInfo userStatusInfo = samDAO.getRegistrationInfo(authUser);
    // Affiliation and Role
    Visa roleVisa = buildAffiliationAndRoleVisa(userStatusInfo, user);

    // Controlled Access Grants
    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    List<Visa> grantVisas = buildControlledAccessGrants(userStatusInfo, approvedDatasets);

    List<Visa> allVisas = Stream.of(grantVisas, List.of(roleVisa)).flatMap(List::stream).toList();
    PassportClaim claim = new PassportClaim(allVisas);
    logInfo("Generated PassportClaim for user: " + user.getEmail() + ": " + claim);
    return claim;
  }

  protected List<Visa> buildControlledAccessGrants(UserStatusInfo userStatusInfo, List<ApprovedDataset> approvedDatasets) {
    return approvedDatasets
        .stream()
        // A user can be approved for a dataset on multiple DARs so filter them here.
        .filter(distinctByKey(ApprovedDataset::getDatasetIdentifier))
        .map(d -> {
          VisaClaimType grant = new ControlledAccessGrants(d);
          VisaClaim claim = new VisaClaim(grant.type(), grant.asserted(), grant.value(), grant.source(), grant.by());
          Instant now = Instant.now();
          Long iat = now.toEpochMilli();
          Long exp = now.plusSeconds(3600).toEpochMilli();
          return new Visa(ISS, userStatusInfo.getUserSubjectId(), iat, exp, claim);
        }).toList();
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  protected Visa buildAffiliationAndRoleVisa(UserStatusInfo userStatusInfo, User user) {
    VisaClaimType affiliationAndRole = new AffiliationAndRole(user);
    VisaClaim affiliationClaim = new VisaClaim(affiliationAndRole.type(), affiliationAndRole.asserted(), affiliationAndRole.value(), affiliationAndRole.source(), affiliationAndRole.by());
    Instant now = Instant.now();
    Long iat = now.toEpochMilli();
    Long exp = now.plusSeconds(3600).toEpochMilli();
    return new Visa(ISS, userStatusInfo.getUserSubjectId(), iat, exp, affiliationClaim);
  }
}
