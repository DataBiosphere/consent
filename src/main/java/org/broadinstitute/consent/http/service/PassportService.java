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
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.passport.AffiliationAndRole;
import org.broadinstitute.consent.http.models.passport.ControlledAccessGrants;
import org.broadinstitute.consent.http.models.passport.PassportClaim;
import org.broadinstitute.consent.http.models.passport.ResearcherStatus;
import org.broadinstitute.consent.http.models.passport.Visa;
import org.broadinstitute.consent.http.models.passport.VisaClaim;
import org.broadinstitute.consent.http.models.passport.VisaClaimType;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.util.ConsentLogger;

/**
 * <a href="https://ga4gh.github.io/data-security/ga4gh-passport">GA4GH Passport</a>
 * TODO:
 *    AcceptedTermsAndPolicies
 *    LinkedIdentities
 *    Generate a JWT for the PassportClaim
 */
public class PassportService implements ConsentLogger {

  public static final String ISS = "https://duos.org";

  private final DatasetDAO datasetDAO;
  private final SamDAO samDAO;

  @Inject
  public PassportService(DatasetDAO datasetDAO, SamDAO samDAO) {
    this.datasetDAO = datasetDAO;
    this.samDAO = samDAO;
  }

  public PassportClaim generatePassport(DuosUser duosUser) throws Exception {
    User user = duosUser.getUser();
    UserStatusInfo userStatusInfo = samDAO.getRegistrationInfo(duosUser);
    // Affiliation and Role
    Visa roleVisa = visaFromVisaClaimType(userStatusInfo, new AffiliationAndRole(user));

    // Researcher Status
    Visa researcherVisa = visaFromVisaClaimType(userStatusInfo, new ResearcherStatus(user));

    // Controlled Access Grants
    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    List<Visa> grantVisas = buildControlledAccessGrants(userStatusInfo, approvedDatasets);

    List<Visa> allVisas = Stream.of(grantVisas, List.of(roleVisa), List.of(researcherVisa))
        .flatMap(List::stream).toList();
    PassportClaim claim = new PassportClaim(allVisas);
    logInfo("Generated PassportClaim for user: " + user.getEmail() + ": " + claim);
    return claim;
  }

  protected List<Visa> buildControlledAccessGrants(UserStatusInfo userStatusInfo,
      List<ApprovedDataset> approvedDatasets) {
    return approvedDatasets
        .stream()
        // A user can be approved for a dataset on multiple DARs so filter them here.
        .filter(distinctByKey(ApprovedDataset::getDatasetIdentifier))
        .map(d -> visaFromVisaClaimType(userStatusInfo, new ControlledAccessGrants(d))).toList();
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  private Visa visaFromVisaClaimType(UserStatusInfo userStatusInfo, VisaClaimType type) {
    VisaClaim claim = new VisaClaim(type.type(), type.asserted(), type.value(), type.source(),
        type.by());
    Instant now = Instant.now();
    Long iat = now.toEpochMilli();
    Long exp = now.plusSeconds(3600).toEpochMilli();
    return new Visa(ISS, userStatusInfo.getUserSubjectId(), iat, exp, claim);
  }
}
