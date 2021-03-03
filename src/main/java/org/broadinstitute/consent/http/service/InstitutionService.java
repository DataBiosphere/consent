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
  }

  public Integer createInstitution(String name, String itDirectorName, String itDirectorEmail, Integer createUser) {
    return this.institutionDAO.insertInstitution(name, itDirectorName, itDirectorEmail, createUser, new Date());
  }

  public void updateInstitutionById(Integer id,
                                    String institutionName,
                                    String itDirectorName,
                                    String itDirectorEmail,
                                    Integer updateUser) {
    this.institutionDAO.updateInstitutionById(
      id, institutionName, itDirectorName, itDirectorEmail, updateUser, new Date());
  }

  public void deleteInstitutionById(Integer id) {
    this.institutionDAO.deleteInstitutionById(id);
  }

  public Institution findInstitutionById(Integer id) {
    return this.institutionDAO.findInstitutionById(id);
  }

  public List<Institution> findAllInstitutions() { return this.institutionDAO.findAllInstitutions(); }

}
