package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import java.util.Date;
import java.util.List;

public class InstitutionService {

  private final InstitutionDAO institutionDAO;

  @Inject
  public InstitutionService(InstitutionDAO institutionDAO) {
    this.institutionDAO = institutionDAO;
  };

  //NOTE: if name column has UNIQUE designation in Postgres, can we trust that to throw errors if a name is already used?
  public Institution createInstitution(String payload, User user) {
      Institution institution = buildInstitution(payload);
      //NOTE: should an error be thrown if id is NOT null? Data may be valid, but id present indicates non-normal request
      if(institution.getId() != null) {
        throw new IllegalArgumentException("Cannot pass a value for ID when creating a new Institution");
      }
      checkForEmptyName(institution); 
      Date createTimestamp = new Date();
      institution.setCreateDate(createTimestamp);
      institution.setCreateUser(user.getDacUserId());
      institutionDAO.insertInstitution(
        institution.getName(),
        institution.getItDirectorName(),
        institution.getItDirectorEmail(),
        institution.getCreateUser(),
        institution.getCreateDate()
      );
      return institution;
  }

  public Institution updateInstitutionById(String jsonPayload, Integer id, User user) {
    Institution institutionPayload = buildInstitution(jsonPayload);
    institutionPayload.setId(id);
    Integer userId = user.getDacUserId();
    checkForEmptyName(institutionPayload);
    institutionDAO.updateInstitutionById(
      institutionPayload.getId(), 
      institutionPayload.getName(), 
      institutionPayload.getItDirectorName(), 
      institutionPayload.getItDirectorEmail(), 
      userId, 
      new Date());
    Institution updatedInstitution = institutionDAO.findInstitutionById(id);
    return updatedInstitution;
  }

  public void deleteInstitutionById(Integer id) {
    institutionDAO.deleteInstitutionById(id);
  }

  public Institution findInstitutionById(Integer id) {
    return this.institutionDAO.findInstitutionById(id);
  }

  public List<Institution> findAllInstitutions() {
    return institutionDAO.findAllInstitutions();
  }

  private Institution buildInstitution(String institutionJson) {
    Institution payload = new Gson().fromJson(institutionJson, Institution.class);
    return payload;
  }

  private void checkForEmptyName(Institution institution) {
    String name = institution.getName();
    if(name == null || name.isBlank()) {
      throw new IllegalArgumentException("Institution name cannot be null or empty");
    }
  };
}
