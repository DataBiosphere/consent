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
    checkUserId(userId);
    checkForEmptyName(institutionPayload);
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

  public Institution findInstitutionForEmail(String email) {
    String trimmedEmail = email.trim();
    String domain = trimmedEmail.substring(trimmedEmail.indexOf('@') + 1);
    return institutionDAO.findInstitutionByDomain(domain);
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
}
