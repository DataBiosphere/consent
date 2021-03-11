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

  public Institution createInstitution(String name, String itDirectorName, String itDirectorEmail, Integer createUser) {
    Date date = new Date();
    Integer id = institutionDAO.insertInstitution(name, itDirectorName, itDirectorEmail, createUser, date);
    return this.findInstitutionById(id);
  }

  public Institution createInstitution(String name, String itDirectorName, String itDirectorEmail, Integer createUser, Date date) {
    Integer id = institutionDAO.insertInstitution(name, itDirectorName, itDirectorEmail, createUser, date);
    return this.findInstitutionById(id);
  }

  public void updateInstitutionById(Integer id,
                                    String institutionName,
                                    String itDirectorName,
                                    String itDirectorEmail,
                                    Integer updateUser) {
    institutionDAO.updateInstitutionById(
      id, institutionName, itDirectorName, itDirectorEmail, updateUser, new Date());
  }

  public void updateInstitutionById(Integer id,
                                    String institutionName,
                                    String itDirectorName,
                                    String itDirectorEmail,
                                    Integer updateUser,
                                    Date date) {
    institutionDAO.updateInstitutionById(
      id, institutionName, itDirectorName, itDirectorEmail, updateUser, date);
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
}
