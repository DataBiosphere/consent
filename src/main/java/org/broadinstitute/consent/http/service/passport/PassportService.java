package org.broadinstitute.consent.http.service.passport;

import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    if (duosUser == null || duosUser.getUser() == null) {
      throw new NotFoundException("User not found");
    }

    User user = duosUser.getUser();
    UserStatusInfo userStatusInfo = duosUser.getUserStatusInfo();
    String userSubjectId =
        userStatusInfo == null
            ? "internal_subject_id_" + user.getUserId()
            : userStatusInfo.getUserSubjectId();
    // Affiliation and Role
    Visa roleVisa = visaFromVisaClaimType(userSubjectId, new AffiliationAndRole(user));

    // Researcher Status
    Visa researcherVisa = visaFromVisaClaimType(userSubjectId, new ResearcherStatus(user));

    // Controlled Access Grants
    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    List<Visa> grantVisas = buildControlledAccessGrants(userSubjectId, approvedDatasets);

    List<Visa> allVisas =
        Stream.of(grantVisas, List.of(roleVisa), List.of(researcherVisa))
            .flatMap(List::stream)
            .toList();
    return new PassportClaim(allVisas);
  }

  protected List<Visa> buildControlledAccessGrants(
      String userSubjectId, List<ApprovedDataset> approvedDatasets) {
    return approvedDatasets.stream()
        .filter(d -> d.getDatasetIdentifier() != null)
        // A user can be approved for a dataset on multiple DARs so filter them here.
        .filter(distinctByKey(ApprovedDataset::getDatasetIdentifier))
        .map(d -> visaFromVisaClaimType(userSubjectId, new ControlledAccessGrants(d)))
        .toList();
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = new HashSet<>();
    return t -> seen.add(keyExtractor.apply(t));
  }

  private Visa visaFromVisaClaimType(String userSubjectId, VisaClaimType type) {
    VisaClaim claim =
        new VisaClaim(type.type(), type.asserted(), type.value(), type.source(), type.by());
    Instant now = Instant.now();
    return new Visa(
        ISS,
        userSubjectId,
        getEpochSeconds(now),
        getEpochSeconds(now.plusSeconds(EXPIRATION_SECONDS)),
        claim);
  }

  public static long getEpochSeconds(Instant instant) {
    return instant.getEpochSecond();
  }
}
