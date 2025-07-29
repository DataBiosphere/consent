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
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.broadinstitute.consent.http.util.InstitutionUtil;

public class InstitutionService {

  private final InstitutionDAO institutionDAO;
  private final UserDAO userDAO;

  @Inject
  public InstitutionService(InstitutionDAO institutionDAO, UserDAO userDAO) {
    this.institutionDAO = institutionDAO;
    this.userDAO = userDAO;
  }

  public Institution createInstitution(Institution institution, Integer userId) {
    checkForEmptyName(institution);
    checkUserId(userId);
    InstitutionUtil.validateInstitutionDomains(institution);
    checkDomainUniqueness(institution);
    try {
      return institutionDAO.insertFullInstitution(institution, userId);
    } catch (SQLException e) {
      throw new ServerErrorException("Could not create institution", HttpStatusCodes.STATUS_CODE_SERVER_ERROR, e);
    }
  }

  public Institution updateInstitutionById(Institution institutionPayload, Integer id,
      Integer userId) throws SQLException {
    Institution targetInstitution = institutionDAO.findInstitutionById(id);
    isInstitutionNull(targetInstitution);
    InstitutionUtil.validateInstitutionDomains(institutionPayload);
    checkUserId(userId);
    checkForEmptyName(institutionPayload);
    checkDomainUniqueness(institutionPayload);
    return institutionDAO.updateFullInstitution(institutionPayload, userId);
  }

  public void deleteInstitutionById(Integer id) {
    Institution institution = institutionDAO.findInstitutionById(id);
    isInstitutionNull(institution);
    institutionDAO.deleteInstitutionById(id);
  }

  public Institution findInstitutionById(Integer id) throws NotFoundException {
    Institution institution = institutionDAO.findInstitutionById(id);
    isInstitutionNull(institution);

    List<SimplifiedUser> signingOfficials = userDAO.getSOsByInstitution(id).stream()
        .map(SimplifiedUser::new)
        .toList();
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
    return institutionDAO.findInstitutionByDomain(trimmedEmailDomain(email));
  }

  /**
   * Finds the institution ID for a given email address. This is a simplified version of the more
   * expansive findInstitutionForEmail method that will only return just the ID for verification and
   * validation of a user's institutional affiliation and library card assignments.
   *
   * @param email the email address to search for
   * @return The Institution ID associated with the email's domain, or null if not found
   */
  public Integer findInstitutionIdForEmail(String email) {
    return institutionDAO.findInstitutionIdByDomain(trimmedEmailDomain(email));
  }

  private String trimmedEmailDomain(String email) {
    String trimmedEmail = email.trim();
    return trimmedEmail.substring(trimmedEmail.indexOf('@') + 1);
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

  private void checkDomainUniqueness(Institution institution) {
    if (institution.getDomains() == null || institution.getDomains().isEmpty()) {
      return;
    }

    List<String> conflictingDomains = institution.getDomains().stream()
        .map(domain -> {
          Integer existingInstitutionId = institutionDAO.findInstitutionIdByDomain(domain);
          if (existingInstitutionId != null && !existingInstitutionId.equals(institution.getId())) {
            // Return the domain if it conflicts with another institution.
            // If the domain is already associated with the institution being updated, it's not a conflict.
            return domain;
          }
          return null; // No conflict
        })
        .filter(Objects::nonNull)
        .toList();

    if (!conflictingDomains.isEmpty()) {
      throw new IllegalArgumentException(
          "Domain(s) already associated with another institution: " + String.join(", ",
              conflictingDomains));
    }
  }
}
