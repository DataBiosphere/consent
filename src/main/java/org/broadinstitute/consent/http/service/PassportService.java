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
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.passport.AffiliationAndRole;
import org.broadinstitute.consent.http.models.passport.ControlledAccessGrants;
import org.broadinstitute.consent.http.models.passport.PassportClaim;
import org.broadinstitute.consent.http.models.passport.Visa;
import org.broadinstitute.consent.http.models.passport.VisaClaim;
import org.broadinstitute.consent.http.models.passport.VisaClaimType;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;

public class PassportService {

  public static final String ISS = "http://duos.org";

  private final DatasetDAO datasetDAO;
  private final InstitutionDAO institutionDAO;
  private final LibraryCardDAO libraryCardDAO;
  private final UserDAO userDAO;
  private final SamDAO samDAO;

  @Inject
  public PassportService(DatasetDAO datasetDAO, InstitutionDAO institutionDAO, LibraryCardDAO libraryCardDAO, UserDAO userDAO, SamDAO samDAO) {
    this.datasetDAO = datasetDAO;
    this.institutionDAO = institutionDAO;
    this.libraryCardDAO = libraryCardDAO;
    this.userDAO = userDAO;
    this.samDAO = samDAO;
  }

  // TODO: Flesh out:
  // * AcceptedTermsAndPolicies
  // * ResearcherStatus
  // * LinkedIdentities
  public PassportClaim generatePassport(AuthUser authUser) throws Exception {
    User user = userDAO.findUserByEmail(authUser.getEmail());
    // TODO: We need a fully fleshed out user with Library Cards, Institution, etc.
    if (user == null) {
      return new PassportClaim(List.of());
    }
    List<LibraryCard> libraryCards = libraryCardDAO.findLibraryCardsByUserId(user.getUserId());
    user.setLibraryCards(libraryCards);
    if (user.getInstitutionId() != null) {
      Institution i = institutionDAO.findInstitutionById(user.getInstitutionId());
      user.setInstitution(i);
    }

    // Affiliation and Role
    Visa roleVisa = buildAffiliationAndRoleVisa(authUser, user);

    // Controlled Access Grants
    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    List<Visa> grantVisas = buildControlledAccessGrants(user, approvedDatasets);

    List<Visa> allVisas = Stream.of(grantVisas, List.of(roleVisa)).flatMap(List::stream).toList();
    return new PassportClaim(allVisas);
  }

  protected List<Visa> buildControlledAccessGrants(User user, List<ApprovedDataset> approvedDatasets) {
    return approvedDatasets
        .stream()
        // A user can be approved for a dataset on multiple DARs so filter them here.
        .filter(distinctByKey(ApprovedDataset::getDatasetIdentifier))
        .map(d -> {
          VisaClaimType grant = new ControlledAccessGrants(d);
          VisaClaim claim = new VisaClaim(grant.type(), grant.asserted(), grant.value(), grant.source(), grant.by());
          Instant now = Instant.now();
          Integer iat = Long.valueOf(now.toEpochMilli()).intValue();
          Integer exp = Long.valueOf(now.plusSeconds(3600).toEpochMilli()).intValue();
          return new Visa(ISS, user.getEmail(), iat, exp, claim);
        }).toList();
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  // TODO: We need the Sam user id which is not collected in any current Sam call that
  // Consent makes ... look into whatever endpoint provides that info and build it out
  // so we can create a valid SUB value.
  // Look at https://sam.dsde-dev.broadinstitute.org/api/users/v2/self
  // which gives us a little more info:
  // {
  //  "allowed": <boolean>,
  //  "azureB2CId": "<azure UUID>",
  //  "createdAt": "1970-01-01T00:00:00Z",
  //  "email": "<email>",
  //  "googleSubjectId": "<google id>",
  //  "id": "103740509117808340318", <--- I think this is the new Sam ID
  //  "updatedAt": "2023-09-20T16:26:40.930521Z"
  //}
  protected Visa buildAffiliationAndRoleVisa(AuthUser authUser, User user) throws Exception {
    UserStatusDiagnostics samUser = samDAO.getSelfDiagnostics(authUser);
    VisaClaimType affiliationAndRole = new AffiliationAndRole(user);
    VisaClaim affiliationClaim = new VisaClaim(affiliationAndRole.type(), affiliationAndRole.asserted(), affiliationAndRole.value(), affiliationAndRole.source(), affiliationAndRole.by());
    Instant now = Instant.now();
    Integer iat = Long.valueOf(now.toEpochMilli()).intValue();
    Integer exp = Long.valueOf(now.plusSeconds(3600).toEpochMilli()).intValue();
    return new Visa(ISS, user.getEmail(), iat, exp, affiliationClaim);
  }
}
