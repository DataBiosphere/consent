package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;

import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.models.Institution;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.NotFoundException;

public class InstitutionService {

  private final InstitutionDAO institutionDAO;

  @Inject
  public InstitutionService(InstitutionDAO institutionDAO) {
    this.institutionDAO = institutionDAO;
  };

  public Institution createInstitution(Institution institution, Integer userId) {
      checkForEmptyName(institution); 
      checkUserId(userId);
      Date createTimestamp = new Date();
      Integer id = institutionDAO.insertInstitution(
        institution.getName(),
        institution.getItDirectorName(),
        institution.getItDirectorEmail(),
        userId,
        createTimestamp
      );
      return institutionDAO.findInstitutionById(id);
  }

  public Institution updateInstitutionById(Institution institutionPayload, Integer id, Integer userId) {
    Institution targetInstitution = institutionDAO.findInstitutionById(id);
    isInstitutionNull(targetInstitution);
    checkUserId(userId);
    checkForEmptyName(institutionPayload);
    Date updateDate = new Date();
    institutionDAO.updateInstitutionById(
      id, 
      institutionPayload.getName(), 
      institutionPayload.getItDirectorEmail(), 
      institutionPayload.getItDirectorName(), 
      userId, 
      updateDate
    );
    return institutionDAO.findInstitutionById(id);
  }

  public void deleteInstitutionById(Integer id) {
    Institution institution = institutionDAO.findInstitutionById(id);
    isInstitutionNull(institution);
    institutionDAO.deleteInstitutionById(id);
  }

  public Institution findInstitutionById(Integer id) {
    Institution institution = institutionDAO.findInstitutionById(id);
    isInstitutionNull(institution);
    return institutionDAO.findInstitutionById(id);
  }

  public List<Institution> findAllInstitutions() {
    return institutionDAO.findAllInstitutions();
  }

  private void checkForEmptyName(Institution institution) {
    String name = institution.getName();
    if(Objects.isNull(name) || name.isBlank()) {
      throw new IllegalArgumentException("Institution name cannot be null or empty");
    }
  };

  private void checkUserId(Integer userId) {
    if(Objects.isNull(userId)) {
      throw new IllegalArgumentException("User ID is a required parameter");
    }
  }

  private void isInstitutionNull(Institution institution) {
    if(Objects.isNull(institution)) {
      throw new NotFoundException("Institution not found");
    }
  }
}
