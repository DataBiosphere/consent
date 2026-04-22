package org.broadinstitute.consent.http.service.passport;

import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.util.ConsentLogger;

/** <a href="https://ga4gh.github.io/data-security/ga4gh-passport">GA4GH Passport</a> */
public class PassportService implements ConsentLogger {

  public static final String ISS = "https://duos.org";
  public static final int EXPIRATION_SECONDS = 3600;

  private final DatasetDAO datasetDAO;
  private final DacService dacService;

  @Inject
  public PassportService(DatasetDAO datasetDAO, DacService dacService) {
    this.datasetDAO = datasetDAO;
    this.dacService = dacService;
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

  /**
   * Generates a Data Passport for a specific dataset, as proposed in the GA4GH Data Passports
   * specification (see <a *
   * href="https://papers.ssrn.com/sol3/papers.cfm?abstract_id=5372874">GA4GH Data Passports</a>).
   * The returned {@link PassportClaim} uses the same envelope as a Researcher Passport but contains
   * dataset-centric visas:
   *
   * <ul>
   *   <li>{@link ApprovedUsersVisa} — links to the dataset's approved user API endpoint
   *   <li>{@link ConsentedDataUseTermsVisa} — links to the dataset's DUO-coded data use terms
   *   <li>{@link OversightBodiesVisa} — identifies the DAC governing the dataset
   *   <li>{@link RequiredAgreementsVisa} — references the DAA users must accept (if one exists)
   * </ul>
   *
   * <p>The {@code sub} field of each visa is the dataset identifier (e.g. {@code DUOS-000001})
   * rather than a user subject ID, reflecting the dataset-centric nature of the passport.
   *
   * @param datasetIdentifier the formatted DUOS identifier, e.g. {@code DUOS-000001}
   * @return a {@link PassportClaim} containing the Data Passport visas for the dataset
   * @throws NotFoundException if the dataset does not exist
   */
  public PassportClaim generateDataPassport(String datasetIdentifier) {
    Integer alias = Dataset.parseIdentifierToAlias(datasetIdentifier);
    Dataset dataset = datasetDAO.findDatasetByAlias(alias);
    if (dataset == null) {
      throw new NotFoundException("Dataset not found: " + datasetIdentifier);
    }

    List<Visa> visas = new ArrayList<>();

    // ApprovedUsers - links to the API endpoint describing approved users for the dataset
    visas.add(visaFromVisaClaimType(datasetIdentifier, new ApprovedUsersVisa(datasetIdentifier)));

    // ConsentedDataUseTerms — always present if the dataset exists
    visas.add(visaFromVisaClaimType(datasetIdentifier, new ConsentedDataUseTermsVisa(dataset)));

    // OversightBodies + RequiredAgreements — only when the dataset is associated with a DAC
    if (dataset.getDacId() != null) {
      try {
        Dac dac = dacService.findById(dataset.getDacId());
        addDacBackedVisas(datasetIdentifier, visas, dac);
      } catch (UnsupportedOperationException e) {
        logWarn(
            "Unable to build DAC-backed visas for dataset %s; returning consented-data-use visa only"
                .formatted(datasetIdentifier),
            e);
      }
    }

    return new PassportClaim(visas);
  }

  private void addDacBackedVisas(String datasetIdentifier, List<Visa> visas, Dac dac) {
    if (dac == null) {
      return;
    }
    visas.add(visaFromVisaClaimType(datasetIdentifier, new OversightBodiesVisa(dac)));
    if (dac.getAssociatedDaa() != null) {
      visas.add(
          visaFromVisaClaimType(
              datasetIdentifier, new RequiredAgreementsVisa(dac.getAssociatedDaa())));
    }
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

  public static String getApprovedUsersEndpoint(String datasetIdentifier) {
    return "https://consent.dsde-prod.broadinstitute.org/api/datataset/%s/approvedUsers"
        .formatted(datasetIdentifier);
  }
}
