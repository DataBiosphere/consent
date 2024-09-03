package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;

@ExtendWith(MockitoExtension.class)
class InstitutionDAOTest extends DAOTestHelper {

  @Test
  void testInsertInstitution() {
    Institution institution = createInstitution();
    List<Institution> all = institutionDAO.findAllInstitutions();
    assertTrue(all.contains(institution));
  }

  @Test
  void testInsertInstitutionDuplicateName() {
    Institution institution = createInstitution();
    Integer userId = institution.getCreateUserId();
    try {
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
      fail("CREATE should fail due to UNIQUE constraint violation (name)");
      //JBDI wraps ALL SQL exceptions under the generic class UnableToExecuteStatementException
      //Test is specifically looking for UNIQUE constraint violations, so I need to catch and unwrap the error to confirm
    } catch (Exception e) {
      assertEquals("23505", ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  void testUpdateInstitutionById() {
    Integer userId = createUser().getUserId();
    String newValue = "New Value";
    Institution institution = createInstitution();
    institutionDAO.updateInstitutionById(institution.getId(), newValue, newValue, newValue,
        newValue, 100, newValue, newValue, newValue, OrganizationType.FOR_PROFIT.getValue(), userId,
        new Date());
    Institution updated = institutionDAO.findInstitutionById(institution.getId());
    assertEquals(newValue, updated.getName());
    assertEquals(newValue, updated.getItDirectorName());
    assertEquals(newValue, updated.getItDirectorEmail());
    assertEquals(newValue, updated.getInstitutionUrl());
    assertEquals(100, (long) updated.getDunsNumber());
    assertEquals(newValue, updated.getOrgChartUrl());
    assertEquals(newValue, updated.getVerificationUrl());
    assertEquals(newValue, updated.getVerificationFilename());
    assertEquals(OrganizationType.FOR_PROFIT.getValue(),
        updated.getOrganizationType().getValue());
  }

  @Test
  void testUpdateInstitutionByIdDuplicateName() {
    Institution institution = createInstitution();
    Institution secondInstitution = createInstitution();
    try {
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
      fail("UPDATE should fail due to UNIQUE constraint violation (name)");
    } catch (Exception e) {
      assertEquals("23505", ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  void testDeleteInstitutionById() {
    Institution institution = createInstitution();
    Integer id = institution.getId();
    institutionDAO.deleteInstitutionById(id);
    assertNull(institutionDAO.findInstitutionById(id));
  }

  @Test
  void testFindInstitutionById() {
    Institution institution = createInstitution();
    Integer id = institution.getId();
    Institution institutionFromDAO = institutionDAO.findInstitutionById(id);
    assertEquals(institutionFromDAO.getId(), institution.getId());
    assertEquals(institutionFromDAO.getName(), institution.getName());
    assertEquals(institutionFromDAO.getItDirectorName(),
        institution.getItDirectorName());
    assertEquals(institutionFromDAO.getItDirectorEmail(),
        institution.getItDirectorEmail());
    assertEquals(institutionFromDAO.getCreateUserId(),
        institution.getCreateUserId());
    assertEquals(institutionFromDAO.getCreateDate(), institution.getCreateDate());
  }

  @Test
  void testFindAllInstitutions() {
    List<Institution> instituteList = institutionDAO.findAllInstitutions();
    assertEquals(0, instituteList.size());
    createInstitution();
    List<Institution> instituteListUpdated = institutionDAO.findAllInstitutions();
    assertEquals(1, instituteListUpdated.size());
  }

  @Test
  void testFindAllInstitutions_InstitutionWithSOs() {
    List<Institution> instituteList = institutionDAO.findAllInstitutions();
    assertEquals(0, instituteList.size());

    //inserts institution, inserts user with that institution id and SO role
    User user = createUserWithInstitution();

    List<Institution> instituteListUpdated = institutionDAO.findAllInstitutions();
    assertEquals(1, instituteListUpdated.size());

    Institution institution = instituteListUpdated.get(0);
    assertEquals(1, institution.getSigningOfficials().size());
    assertEquals(user.getInstitutionId(), institution.getId());
    assertEquals(user.getDisplayName(),
        institution.getSigningOfficials().get(0).displayName);
  }

  @Test
  void testFindInstitutionsByName() {
    Institution institution = createInstitution();

    List<Institution> found = institutionDAO.findInstitutionsByName(institution.getName());
    assertFalse(found.isEmpty());
    assertEquals(1, found.size());
    assertEquals(institution.getId(), found.get(0).getId());
  }

  @Test
  void testFindInstitutionsByName_Missing() {
    List<Institution> found = institutionDAO.findInstitutionsByName(
        RandomStringUtils.randomAlphabetic(10));
    assertTrue(found.isEmpty());
  }

  @Test
  void testDeleteInstitutionByUserId() {
    Institution institution = createInstitution();
    Integer userId = institution.getCreateUserId();
    institutionDAO.deleteAllInstitutionsByUser(userId);
    assertNull(institutionDAO.findInstitutionById(institution.getId()));
  }

  @Test
  void testFindInstitutionWithSOById() {
    Institution institution = createInstitution();
    User user = createUserWithInstitution();
    Institution institutionWithSO = institutionDAO.findInstitutionWithSOById(user.getInstitutionId());
    assertEquals(1, institutionWithSO.getSigningOfficials().size());
    assertEquals(user.getDisplayName(), institutionWithSO.getSigningOfficials().get(0).displayName);
  }

  private Institution createInstitution() {
    User createUser = createUser();
    Integer id = institutionDAO.insertInstitution(RandomStringUtils.randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        RandomStringUtils.randomAlphabetic(10),
        new Random().nextInt(),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        OrganizationType.NON_PROFIT.getValue(),
        createUser.getUserId(),
        createUser.getCreateDate());
    Institution institution = institutionDAO.findInstitutionById(id);
    User updateUser = createUser();
    institutionDAO.updateInstitutionById(
        id,
        institution.getName(),
        institution.getItDirectorEmail(),
        institution.getItDirectorName(),
        institution.getInstitutionUrl(),
        institution.getDunsNumber(),
        institution.getOrgChartUrl(),
        institution.getVerificationUrl(),
        institution.getVerificationFilename(),
        institution.getOrganizationType().getValue(),
        updateUser.getUserId(),
        new Date()
    );
    return institutionDAO.findInstitutionById(id);
  }

  private User createUserWithInstitution() {
    int i1 = RandomUtils.nextInt(5, 10);
    String email = RandomStringUtils.randomAlphabetic(i1);
    String name = RandomStringUtils.randomAlphabetic(10);
    Integer userId = userDAO.insertUser(email, name, new Date());
    Integer institutionId = institutionDAO.insertInstitution(RandomStringUtils.randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        RandomStringUtils.randomAlphabetic(10),
        new Random().nextInt(),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        OrganizationType.NON_PROFIT.getValue(),
        userId,
        new Date());
    userDAO.updateUser(name, userId, institutionId);
    userRoleDAO.insertSingleUserRole(7, userId);
    return userDAO.findUserById(userId);
  }

}
