package org.broadinstitute.consent.http.db;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Institution;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        null,
        null,
        null,
        null,
        null,
        null,
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
    Integer userId = createUser().getUserId();
    String newValue = "New Value";
    Institution institution = createInstitution();
    institutionDAO.updateInstitutionById(institution.getId(), newValue, newValue, newValue, newValue, 100, newValue, newValue, newValue, OrganizationType.FOR_PROFIT.getValue(), userId, new Date());
    Institution updated = institutionDAO.findInstitutionById(institution.getId());
    assertEquals(newValue, updated.getName());
    assertEquals(newValue, updated.getItDirectorName());
    assertEquals(newValue, updated.getItDirectorEmail());
    assertEquals(newValue, updated.getInstitutionUrl());
    assertEquals(100, (long)updated.getDunsNumber());
    assertEquals(newValue, updated.getOrgChartUrl());
    assertEquals(newValue, updated.getVerificationUrl());
    assertEquals(newValue, updated.getVerificationFilename());
    assertEquals(OrganizationType.FOR_PROFIT.getValue(), updated.getOrganizationType().getValue());
  }

  @Test
  public void testUpdateInstitutionByIdDuplicateName() {
    Institution institution = createInstitution();
    Institution secondInstitution = createInstitution();
    try{
      institutionDAO.updateInstitutionById(secondInstitution.getId(),
              institution.getName(),
              secondInstitution.getItDirectorName(),
              secondInstitution.getItDirectorEmail(),
              secondInstitution.getInstitutionUrl(),
              secondInstitution.getDunsNumber(),
              secondInstitution.getOrgChartUrl(),
              secondInstitution.getVerificationUrl(),
              secondInstitution.getVerificationFilename(),
              secondInstitution.getOrganizationType().getValue(),
              secondInstitution.getUpdateUserId(),
              secondInstitution.getUpdateDate());
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

  @Test
  public void testFindAllInstitutions_InstitutionWithSOs() {
    List<Institution> instituteList = institutionDAO.findAllInstitutions();
    assertEquals(0, instituteList.size());

    //inserts institution, inserts user with that institution id and SO role
    User user = createUserWithInstitution();

    List<Institution> instituteListUpdated = institutionDAO.findAllInstitutions();
    assertEquals(1, instituteListUpdated.size());

    Institution institution = instituteListUpdated.get(0);
    assertEquals(1, institution.getSigningOfficials().size());
    assertEquals(user.getInstitutionId(), institution.getId());
    assertEquals(user.getDisplayName(), institution.getSigningOfficials().get(0).displayName);
  }

  @Test
  public void testFindInstitutionsByName() {
    Institution institution = createInstitution();

    List<Institution> found = institutionDAO.findInstitutionsByName(institution.getName());
    assertFalse(found.isEmpty());
    assertEquals(1, found.size());
    assertEquals(institution.getId(), found.get(0).getId());
  }

  @Test
  public void testFindInstitutionsByName_Missing() {
    List<Institution> found = institutionDAO.findInstitutionsByName(RandomStringUtils.randomAlphabetic(10));
    assertTrue(found.isEmpty());
  }
}
