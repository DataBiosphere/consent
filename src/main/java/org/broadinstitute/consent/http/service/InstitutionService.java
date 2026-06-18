package org.broadinstitute.consent.http.service;

import com.google.api.client.http.HttpStatusCodes;
import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.broadinstitute.consent.http.service.feature.InstitutionAndLibraryCardEnforcement;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.InstitutionUtil;
import org.jdbi.v3.core.Jdbi;

public class InstitutionService implements ConsentLogger {

  private final InstitutionDAO institutionDAO;
  private final UserDAO userDAO;
  private final InstitutionAndLibraryCardEnforcement institutionAndLibraryCardEnforcement;

  @Inject
  public InstitutionService(
      Jdbi jdbi, InstitutionAndLibraryCardEnforcement institutionAndLibraryCardEnforcement) {
    this.institutionDAO = jdbi.onDemand(InstitutionDAO.class);
    this.userDAO = jdbi.onDemand(UserDAO.class);
    this.institutionAndLibraryCardEnforcement = institutionAndLibraryCardEnforcement;
  }

  public Institution createInstitution(Institution institution, Integer userId) {
    checkUserId(userId);
    // Name validation
    checkForEmptyName(institution);
    checkNameUniqueness(institution);
    String canonicalName = InstitutionUtil.canonicalizeInstitutionName(institution.getName());
    institution.setName(canonicalName);

    // Domain validation
    InstitutionUtil.validateInstitutionDomains(institution);
    checkDomainUniqueness(institution);
    try {
      Institution createdInstitution = institutionDAO.insertFullInstitution(institution, userId);
      // Enforce Institution and Library Card rules for all users after an institution is created
      enforceInstitutionAndLibraryCardRules();
      return createdInstitution;
    } catch (SQLException e) {
      throw new ServerErrorException(
          "Could not create institution", HttpStatusCodes.STATUS_CODE_SERVER_ERROR, e);
    }
  }

  public Institution updateInstitutionById(
      Institution institutionPayload, Integer id, Integer userId) throws SQLException {
    checkUserId(userId);
    Institution targetInstitution = institutionDAO.findInstitutionById(id);
    isInstitutionNull(targetInstitution);

    // Name validation
    checkForEmptyName(institutionPayload);
    checkNameUniqueness(institutionPayload);
    String canonicalName =
        InstitutionUtil.canonicalizeInstitutionName(institutionPayload.getName());
    institutionPayload.setName(canonicalName);

    // Domain validation
    InstitutionUtil.validateInstitutionDomains(institutionPayload);
    checkDomainUniqueness(institutionPayload);
    Institution updatedInstitution =
        institutionDAO.updateFullInstitution(institutionPayload, userId);
    // Enforce Institution and Library Card rules for all users after an institution is updated
    enforceInstitutionAndLibraryCardRules();
    return updatedInstitution;
  }

  public void deleteInstitutionById(Integer id) {
    Institution institution = institutionDAO.findInstitutionById(id);
    isInstitutionNull(institution);
    institutionDAO.deleteInstitutionById(id);
    // Enforce Institution and Library Card rules for all users after an institution is deleted
    enforceInstitutionAndLibraryCardRules();
  }

  public Institution findInstitutionById(Integer id) throws NotFoundException {
    Institution institution = institutionDAO.findInstitutionById(id);
    isInstitutionNull(institution);

    List<SimplifiedUser> signingOfficials =
        userDAO.getSOsByInstitution(id).stream().map(SimplifiedUser::new).toList();
    institution.setSigningOfficials(signingOfficials);

    return institution;
  }

  /**
   * Finds the institution for a given email address. This method returns a fully populated
   * institution with signing officials, users, and domains.
   *
   * @param email the email address to search for
   * @return The Institution associated with the email's domain, or null if not found
   */
  public Institution findInstitutionForEmail(String email) {
    return institutionDAO.findInstitutionByDomain(
        institutionAndLibraryCardEnforcement.trimmedEmailDomain(email));
  }

  public List<Institution> findAllInstitutions() {
    return institutionDAO.findAllInstitutions();
  }

  public List<Institution> findAllInstitutionsByName(String name) {
    return institutionDAO.findInstitutionsByName(name);
  }

  private void checkForEmptyName(Institution institution) {
    String name = institution.getName();
    if (Objects.isNull(name) || name.isBlank()) {
      throw new IllegalArgumentException("Institution name cannot be null or empty");
    }
  }

  private void checkUserId(Integer userId) {
    if (Objects.isNull(userId)) {
      throw new IllegalArgumentException("User ID is a required parameter");
    }
  }

  private void isInstitutionNull(Institution institution) {
    if (Objects.isNull(institution)) {
      throw new NotFoundException("Institution not found");
    }
  }

  private void checkNameUniqueness(Institution institution) {
    List<Institution> conflicts =
        findAllInstitutionsByName(institution.getName()).stream()
            // Filter out the institution being updated, so it doesn't conflict with itself
            .filter(existingInstitution -> !existingInstitution.getId().equals(institution.getId()))
            .toList();

    if (!conflicts.isEmpty()) {
      throw new ConsentConflictException(
          "An institution exists with the name of '" + institution.getName() + "'");
    }
  }

  private void checkDomainUniqueness(Institution institution) {
    if (institution.getDomains() == null || institution.getDomains().isEmpty()) {
      return;
    }

    if (institution.getDomains().stream().distinct().count() < institution.getDomains().size()) {
      throw new IllegalArgumentException("Institution domains must be unique");
    }

    List<String> conflictingDomains =
        institution.getDomains().stream()
            .map(
                domain -> {
                  Integer existingInstitutionId = institutionDAO.findInstitutionIdByDomain(domain);
                  if (existingInstitutionId != null
                      && !existingInstitutionId.equals(institution.getId())) {
                    // Return the domain if it conflicts with another institution.
                    // If the domain is already associated with the institution being updated, it's
                    // not a conflict.
                    return domain;
                  }
                  return null; // No conflict
                })
            .filter(Objects::nonNull)
            .toList();

    if (!conflictingDomains.isEmpty()) {
      throw new IllegalArgumentException(
          "Domain(s) already associated with another institution: "
              + String.join(", ", conflictingDomains));
    }
  }

  private void enforceInstitutionAndLibraryCardRules() {
    try {
      institutionAndLibraryCardEnforcement.asyncEnforceInstitutionAndLibraryCardRulesForAllUsers();
    } catch (Exception e) {
      logException(e);
    }
  }
}
