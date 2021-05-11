package org.broadinstitute.consent.http.db;

import com.google.gson.Gson;
import org.broadinstitute.consent.http.models.Institution;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Test;
import org.postgresql.util.PSQLException;

import java.util.Date;
import java.util.List;

public class InstitutionDAOTest extends DAOTestHelper {

  @Test
  public void testInsertInstitution() {
    Institution institution = createInstitution();
    List<Institution> all = institutionDAO.findAllInstitutions();
    assertTrue(all.contains(institution));
  }

  @Test
  public void testInsertInstitutionDuplicateName() {
    Institution institution = createInstitution();
    Integer userId = institution.getCreateUserId();
    try{
      institutionDAO.insertInstitution(
        institution.getName(),
        institution.getItDirectorName(),
        institution.getItDirectorEmail(),
        userId,
        institution.getCreateDate()
      );
      Assert.fail("CREATE should fail due to UNIQUE constraint violation (name)");
    //JBDI wraps ALL SQL exceptions under the generic class UnableToExecuteStatementException
    //Test is specifically looking for UNIQUE constraint violations, so I need to catch and unwrap the error to confirm
    } catch(Exception e) {
      assertEquals("23505", ((PSQLException)e.getCause()).getSQLState());
    }
  }

  @Test
  public void testUpdateInstitutionById() {
    Integer userId = createUser().getDacUserId();
    String newValue = "New Value";
    Institution institution = createInstitution();
    institutionDAO.updateInstitutionById(institution.getId(), newValue, newValue, newValue, userId, new Date());
    Institution updated = institutionDAO.findInstitutionById(institution.getId());
    assertEquals(updated.getName(), newValue);
    assertEquals(updated.getItDirectorName(), newValue);
    assertEquals(updated.getItDirectorEmail(), newValue);
  }

  @Test
  public void testUpdateInstitutionByIdDuplicateName() {
    Institution institution = createInstitution();
    Institution secondInstitution = createInstitution();
    try{
      institutionDAO.updateInstitutionById(secondInstitution.getId(), institution.getName(), secondInstitution.getItDirectorName(), secondInstitution.getItDirectorEmail(), secondInstitution.getUpdateUserId(), secondInstitution.getUpdateDate());
      Assert.fail("UPDATE should fail due to UNIQUE constraint violation (name)");
    }catch(Exception e) {
      assertEquals("23505", ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  public void testDeleteInstitutionById() {
    Institution institution = createInstitution();
    Integer id = institution.getId();
    institutionDAO.deleteInstitutionById(id);
    assertNull(institutionDAO.findInstitutionById(id));
  }

  @Test
  public void testFindInstitutionById() {
    Institution institution = createInstitution();
    Integer id = institution.getId();
    Institution institutionFromDAO = institutionDAO.findInstitutionById(id);
    assertEquals(institutionFromDAO.getId(), institution.getId());
    assertEquals(institutionFromDAO.getName(), institution.getName());
    assertEquals(institutionFromDAO.getItDirectorName(), institution.getItDirectorName());
    assertEquals(institutionFromDAO.getItDirectorEmail(), institution.getItDirectorEmail());
    assertEquals(institutionFromDAO.getCreateUserId(), institution.getCreateUserId());
    assertEquals(institutionFromDAO.getCreateDate(), institution.getCreateDate());
  }

  @Test
  public void testFindAllInstitutions() {
    List<Institution> instituteList = institutionDAO.findAllInstitutions();
    assertEquals(0, instituteList.size());
    createInstitution();
    List<Institution> instituteListUpdated = institutionDAO.findAllInstitutions();
    assertEquals(1, instituteListUpdated.size());
  }
}
