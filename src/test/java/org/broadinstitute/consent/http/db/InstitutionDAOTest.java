package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

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
        institution.getSigningOfficials().get(0).getDisplayName());
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
  void testFindInstitutionsByNameTrimsInput() {
    Institution institution = createInstitution();

    List<Institution> found = institutionDAO.findInstitutionsByName(
        "  " + institution.getName() + "  ");
    assertFalse(found.isEmpty());
    assertEquals(1, found.size());
    assertEquals(institution.getId(), found.get(0).getId());
  }

  @Test
  void testFindInstitutionsByNameTrimsDb() {
    Institution institution = createInstitution();
    User user = createUser();
    institutionDAO.updateInstitutionById(
        institution.getId(),
        "  " + institution.getName() + "  ",
        institution.getItDirectorEmail(),
        institution.getItDirectorName(),
        institution.getInstitutionUrl(),
        institution.getDunsNumber(),
        institution.getOrgChartUrl(),
        institution.getVerificationUrl(),
        institution.getVerificationFilename(),
        institution.getOrganizationType().getValue(),
        user.getUserId(),
        new Date()
    );
    List<Institution> found = institutionDAO.findInstitutionsByName(institution.getName());
    assertFalse(found.isEmpty());
    assertEquals(1, found.size());
    assertEquals(institution.getId(), found.get(0).getId());
  }


  @Test
  void testFindInstitutionsByName_Missing() {
    List<Institution> found = institutionDAO.findInstitutionsByName(
        randomAlphabetic(10));
    assertTrue(found.isEmpty());
  }

  @Test
  void testDeleteInstitutionByUserId() throws SQLException {
    Institution institution = createInstitution();
    Integer userId = institution.getCreateUserId();
    institutionDAO.deleteAllInstitutionsByUser(userId);
    assertNull(institutionDAO.findInstitutionById(institution.getId()));
  }

  @Test
  void testDeleteInstitutionWithDomainsByUserId() throws SQLException {
    Institution institution = createInstitution();
    institution.setDomains(List.of("domain1.com", "domain2.com"));
    institutionDAO.updateFullInstitution(institution, institution.getCreateUserId());
    Integer userId = institution.getCreateUserId();
    institutionDAO.deleteAllInstitutionsByUser(userId);
    assertNull(institutionDAO.findInstitutionById(institution.getId()));
    jdbi.useHandle(handle -> {
      List<String> domains = handle.createQuery(
              "SELECT domain FROM institution_domains WHERE institution_id = :id")
          .bind("id", institution.getId())
          .mapTo(String.class)
          .list();
      assertTrue(domains.isEmpty(), "Domains should be deleted when institution is deleted");
    });
  }

  @Test
  void testFindInstitutionWithSOById() {
    User user = createUserWithInstitution();
    Institution institutionWithSO = institutionDAO.findInstitutionWithSOById(
        user.getInstitutionId());
    assertEquals(1, institutionWithSO.getSigningOfficials().size());
    assertEquals(user.getDisplayName(),
        institutionWithSO.getSigningOfficials().get(0).getDisplayName());
  }

  private Institution createInstitution() {
    User createUser = createUser();
    Integer id = institutionDAO.insertInstitution(randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        randomAlphabetic(10),
        new Random().nextInt(),
        randomAlphabetic(10),
        randomAlphabetic(10),
        randomAlphabetic(10),
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

  @Test
  void testInsertFullInstitution() throws Exception {
    User user = createUser();
    Institution institution = new Institution();
    institution.setName("Test Institution");
    institution.setItDirectorName("Test Director");
    institution.setItDirectorEmail("email");
    institution.setInstitutionUrl("http://testinstitution.com");
    institution.setDunsNumber(123456789);
    institution.setOrgChartUrl("http://testinstitution.com/orgchart");
    institution.setVerificationUrl("http://testinstitution.com/verification");
    institution.setVerificationFilename("verification.pdf");
    institution.setOrganizationType(OrganizationType.NON_PROFIT);
    institution.setDomains(List.of("domain1.com", "domain2.com"));
    Institution insertedInstitution = institutionDAO.insertFullInstitution(institution,
        user.getUserId());

    assertEquals(institution.getName(), insertedInstitution.getName());
    assertEquals(institution.getItDirectorName(), insertedInstitution.getItDirectorName());
    assertEquals(institution.getItDirectorEmail(), insertedInstitution.getItDirectorEmail());
    assertEquals(institution.getInstitutionUrl(), insertedInstitution.getInstitutionUrl());
    assertEquals(institution.getDunsNumber(), insertedInstitution.getDunsNumber());
    assertEquals(institution.getOrgChartUrl(), insertedInstitution.getOrgChartUrl());
    assertEquals(institution.getVerificationUrl(), insertedInstitution.getVerificationUrl());
    assertEquals(institution.getVerificationFilename(),
        insertedInstitution.getVerificationFilename());
    assertEquals(institution.getDomains().size(), insertedInstitution.getDomains().size());
    institution.getDomains()
        .forEach(domain -> assertTrue(insertedInstitution.getDomains().contains(domain)));
  }

  @Test
  void testInsertFullInstitutionUniqueDomainException_Case1() {
    User user = createUser();
    Institution institution = new Institution();
    institution.setName("Test Institution");
    institution.setItDirectorName("Test Director");
    institution.setItDirectorEmail("email");
    institution.setInstitutionUrl("http://testinstitution.com");
    institution.setDunsNumber(123456789);
    institution.setOrgChartUrl("http://testinstitution.com/orgchart");
    institution.setVerificationUrl("http://testinstitution.com/verification");
    institution.setVerificationFilename("verification.pdf");
    institution.setOrganizationType(OrganizationType.FOR_PROFIT);
    institution.setDomains(List.of("domain1.com", "domain1.com"));
    try {
      institutionDAO.insertFullInstitution(institution, user.getUserId());
    } catch (Exception e) {
      assertEquals(PSQLState.UNIQUE_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  void testUpdateFullInstitution() throws Exception {
    User user = createUser();
    Institution institution = new Institution();
    institution.setName("Test Institution");
    institution.setItDirectorName("Test Director");
    institution.setItDirectorEmail("email");
    institution.setInstitutionUrl("http://testinstitution.com");
    institution.setDunsNumber(123456789);
    institution.setOrgChartUrl("http://testinstitution.com/orgchart");
    institution.setVerificationUrl("http://testinstitution.com/verification");
    institution.setVerificationFilename("verification.pdf");
    institution.setOrganizationType(OrganizationType.NON_PROFIT);
    institution.setDomains(List.of("domain1.com", "domain2.com"));

    Institution insertedInstitution = institutionDAO.insertFullInstitution(institution,
        user.getUserId());

    insertedInstitution.setName("Updated Institution");
    insertedInstitution.setItDirectorName("Updated Director");
    insertedInstitution.setItDirectorEmail("updatedemail");
    insertedInstitution.setInstitutionUrl("http://updatedinstitution.com");
    insertedInstitution.setDunsNumber(987654321);
    insertedInstitution.setOrgChartUrl("http://updatedinstitution.com/orgchart");
    insertedInstitution.setVerificationUrl("http://updatedinstitution.com/verification");
    insertedInstitution.setVerificationFilename("updated_verification.pdf");
    insertedInstitution.setOrganizationType(OrganizationType.FOR_PROFIT);
    insertedInstitution.setDomains(
        List.of("new.domain1.com", "new.domain2.com", "new.domain3.com"));

    Institution updatedInstitution = institutionDAO.updateFullInstitution(insertedInstitution,
        user.getUserId());

    assertEquals(updatedInstitution.getName(), insertedInstitution.getName());
    assertEquals(updatedInstitution.getItDirectorName(), insertedInstitution.getItDirectorName());
    assertEquals(updatedInstitution.getItDirectorEmail(), insertedInstitution.getItDirectorEmail());
    assertEquals(updatedInstitution.getInstitutionUrl(), insertedInstitution.getInstitutionUrl());
    assertEquals(updatedInstitution.getDunsNumber(), insertedInstitution.getDunsNumber());
    assertEquals(updatedInstitution.getOrgChartUrl(), insertedInstitution.getOrgChartUrl());
    assertEquals(updatedInstitution.getVerificationUrl(), insertedInstitution.getVerificationUrl());
    assertEquals(updatedInstitution.getVerificationFilename(),
        insertedInstitution.getVerificationFilename());
    assertEquals(updatedInstitution.getDomains().size(), insertedInstitution.getDomains().size());
    updatedInstitution.getDomains()
        .forEach(domain -> assertTrue(insertedInstitution.getDomains().contains(domain)));

    updatedInstitution.setDomains(null); // Reset domains to test deletion
    Institution reloadedInstitution = institutionDAO.updateFullInstitution(updatedInstitution,
        user.getUserId());
    assertNull(reloadedInstitution.getDomains(), "Domains should be empty after update");
  }
}
