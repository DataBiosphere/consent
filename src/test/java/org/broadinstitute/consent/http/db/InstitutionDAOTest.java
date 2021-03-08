package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Institution;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class InstitutionDAOTest extends DAOTestHelper {

  @Test
  public void testInsertInstitution() {
    Institution institution = createInstitution();
    List<Institution> all = institutionDAO.findAllInstitutions();
    Assert.assertTrue(all.contains(institution));
  }
  @Test
  public void testUpdateInstitutionById() {
    Integer userId = createUser().getDacUserId();
    String newValue = "New Value";
    Institution institution = createInstitution();
    institutionDAO.updateInstitutionById(institution.getId(), newValue, newValue, newValue, userId, new Date());
    Institution updated = institutionDAO.findInstitutionById(institution.getId());
    Assert.assertEquals(updated.getName(), newValue);
    Assert.assertEquals(updated.getItDirectorName(), newValue);
    Assert.assertEquals(updated.getItDirectorEmail(), newValue);
  }

  @Test
  public void testDeleteInstitutionById() {
    Institution institution = createInstitution();
    Integer id = institution.getId();
    institutionDAO.deleteInstitutionById(id);
    Assert.assertFalse(institutionDAO.findAllInstitutions().contains(institution));
  }

  @Test
  public void testFindInstitutionById() {
    Institution institution = createInstitution();
    Integer id = institution.getId();
    Assert.assertEquals(institutionDAO.findInstitutionById(id).getName(), institution.getName());
  }

  @Test
  public void testFindAllInstitutions() {
    List<Institution> instituteList = institutionDAO.findAllInstitutions();
    Assert.assertEquals(0, instituteList.size());
    createInstitution();
    List<Institution> instituteListUpdated = institutionDAO.findAllInstitutions();
    Assert.assertEquals(1, instituteListUpdated.size());
  }
}