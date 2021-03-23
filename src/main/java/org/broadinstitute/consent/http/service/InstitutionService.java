package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;

import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.models.Institution;
import java.util.Date;
import java.util.List;

public class InstitutionService {

  private final InstitutionDAO institutionDAO;

  @Inject
  public InstitutionService(InstitutionDAO institutionDAO) {
    this.institutionDAO = institutionDAO;
  };

  //NOTE: if NAME column has the UNIQUE attribute in Postgres, can we trust that to throw errors in Java?
  public Institution createInstitution(Institution institution, Integer userId) {
      //NOTE: should an error be thrown if id is NOT null? Data may be valid, but id present indicates non-normal request
      if(institution.getId() != null) {
        throw new IllegalArgumentException("Cannot pass a value for ID when creating a new Institution");
      }
      checkForEmptyName(institution); 
      Date createTimestamp = new Date();
      institution.setCreateDate(createTimestamp);
      institution.setCreateUser(userId);
      Integer id = institutionDAO.insertInstitution(
        institution.getName(),
        institution.getItDirectorName(),
        institution.getItDirectorEmail(),
        institution.getCreateUser(),
        institution.getCreateDate()
      );
      return institutionDAO.findInstitutionById(id);
  }

  public Institution updateInstitutionById(Institution institutionPayload, Integer id, Integer userId) {
    institutionPayload.setId(id);
    institutionPayload.setUpdateDate(new Date());
    institutionPayload.setUpdateUser(userId);
    checkForEmptyName(institutionPayload);
    institutionDAO.updateInstitutionById(
      institutionPayload.getId(), 
      institutionPayload.getName(), 
      institutionPayload.getItDirectorName(), 
      institutionPayload.getItDirectorEmail(), 
      institutionPayload.getUpdateUser(),
      institutionPayload.getUpdateDate());
    return institutionDAO.findInstitutionById(id);
  }

  public void deleteInstitutionById(Integer id) {
    institutionDAO.deleteInstitutionById(id);
  }

  public Institution findInstitutionById(Integer id) {
    return institutionDAO.findInstitutionById(id);
  }

  public List<Institution> findAllInstitutions() {
    return institutionDAO.findAllInstitutions();
  }

  private void checkForEmptyName(Institution institution) {
    String name = institution.getName();
    if(name == null || name.isBlank()) {
      throw new IllegalArgumentException("Institution name cannot be null or empty");
    }
  };
}
