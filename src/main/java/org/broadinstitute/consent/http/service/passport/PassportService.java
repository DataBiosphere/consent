package org.broadinstitute.consent.http.service.passport;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.util.ConsentLogger;

/** <a href="https://ga4gh.github.io/data-security/ga4gh-passport">GA4GH Passport</a> */
public class PassportService implements ConsentLogger {

  public static final String ISS = "https://duos.org";
  public static final int EXPIRATION_SECONDS = 3600;

  private final DatasetDAO datasetDAO;

  @Inject
  public PassportService(DatasetDAO datasetDAO) {
    this.datasetDAO = datasetDAO;
  }

  public PassportClaim generatePassport(DuosUser duosUser) {
    User user = duosUser.getUser();
    UserStatusInfo userStatusInfo = duosUser.getUserStatusInfo();
    // Affiliation and Role
    Visa roleVisa = visaFromVisaClaimType(userStatusInfo, new AffiliationAndRole(user));

    // Researcher Status
    Visa researcherVisa = visaFromVisaClaimType(userStatusInfo, new ResearcherStatus(user));

    // Controlled Access Grants
    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    List<Visa> grantVisas = buildControlledAccessGrants(userStatusInfo, approvedDatasets);

    List<Visa> allVisas =
        Stream.of(grantVisas, List.of(roleVisa), List.of(researcherVisa))
            .flatMap(List::stream)
            .toList();
    PassportClaim claim = new PassportClaim(allVisas);
    logInfo("Generated PassportClaim for user: " + user.getEmail() + ": " + claim);
    return claim;
  }

  protected List<Visa> buildControlledAccessGrants(
      UserStatusInfo userStatusInfo, List<ApprovedDataset> approvedDatasets) {
    return approvedDatasets.stream()
        // A user can be approved for a dataset on multiple DARs so filter them here.
        .filter(distinctByKey(ApprovedDataset::getDatasetIdentifier))
        .map(d -> visaFromVisaClaimType(userStatusInfo, new ControlledAccessGrants(d)))
        .toList();
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  private Visa visaFromVisaClaimType(UserStatusInfo userStatusInfo, VisaClaimType type) {
    VisaClaim claim =
        new VisaClaim(type.type(), type.asserted(), type.value(), type.source(), type.by());
    Instant now = Instant.now();
    Long iat = now.getEpochSecond();
    Long exp = now.plusSeconds(EXPIRATION_SECONDS).getEpochSecond();
    return new Visa(ISS, userStatusInfo.getUserSubjectId(), iat, exp, claim);
  }
}
