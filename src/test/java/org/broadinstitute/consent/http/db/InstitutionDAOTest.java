package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;

public class InstitutionDAOTest extends DAOTestHelper {

    @Test
    public void testInsertInstitution() {
        Institution institution = createInstitution();
        List<Institution> all = institutionDAO.findAllInstitutions();
        Assertions.assertTrue(all.contains(institution));
    }

    @Test
    public void testInsertInstitutionDuplicateName() {
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
            Assertions.fail("CREATE should fail due to UNIQUE constraint violation (name)");
            //JBDI wraps ALL SQL exceptions under the generic class UnableToExecuteStatementException
            //Test is specifically looking for UNIQUE constraint violations, so I need to catch and unwrap the error to confirm
        } catch (Exception e) {
            Assertions.assertEquals("23505", ((PSQLException) e.getCause()).getSQLState());
        }
    }

    @Test
    public void testUpdateInstitutionById() {
        Integer userId = createUser().getUserId();
        String newValue = "New Value";
        Institution institution = createInstitution();
        institutionDAO.updateInstitutionById(institution.getId(), newValue, newValue, newValue, newValue, 100, newValue, newValue, newValue, OrganizationType.FOR_PROFIT.getValue(), userId, new Date());
        Institution updated = institutionDAO.findInstitutionById(institution.getId());
        Assertions.assertEquals(newValue, updated.getName());
        Assertions.assertEquals(newValue, updated.getItDirectorName());
        Assertions.assertEquals(newValue, updated.getItDirectorEmail());
        Assertions.assertEquals(newValue, updated.getInstitutionUrl());
        Assertions.assertEquals(100, (long) updated.getDunsNumber());
        Assertions.assertEquals(newValue, updated.getOrgChartUrl());
        Assertions.assertEquals(newValue, updated.getVerificationUrl());
        Assertions.assertEquals(newValue, updated.getVerificationFilename());
        Assertions.assertEquals(OrganizationType.FOR_PROFIT.getValue(),
            updated.getOrganizationType().getValue());
    }

    @Test
    public void testUpdateInstitutionByIdDuplicateName() {
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
            Assertions.fail("UPDATE should fail due to UNIQUE constraint violation (name)");
        } catch (Exception e) {
            Assertions.assertEquals("23505", ((PSQLException) e.getCause()).getSQLState());
        }
    }

    @Test
    public void testDeleteInstitutionById() {
        Institution institution = createInstitution();
        Integer id = institution.getId();
        institutionDAO.deleteInstitutionById(id);
        Assertions.assertNull(institutionDAO.findInstitutionById(id));
    }

    @Test
    public void testFindInstitutionById() {
        Institution institution = createInstitution();
        Integer id = institution.getId();
        Institution institutionFromDAO = institutionDAO.findInstitutionById(id);
        Assertions.assertEquals(institutionFromDAO.getId(), institution.getId());
        Assertions.assertEquals(institutionFromDAO.getName(), institution.getName());
        Assertions.assertEquals(institutionFromDAO.getItDirectorName(),
            institution.getItDirectorName());
        Assertions.assertEquals(institutionFromDAO.getItDirectorEmail(),
            institution.getItDirectorEmail());
        Assertions.assertEquals(institutionFromDAO.getCreateUserId(),
            institution.getCreateUserId());
        Assertions.assertEquals(institutionFromDAO.getCreateDate(), institution.getCreateDate());
    }

    @Test
    public void testFindAllInstitutions() {
        List<Institution> instituteList = institutionDAO.findAllInstitutions();
        Assertions.assertEquals(0, instituteList.size());
        createInstitution();
        List<Institution> instituteListUpdated = institutionDAO.findAllInstitutions();
        Assertions.assertEquals(1, instituteListUpdated.size());
    }

    @Test
    public void testFindAllInstitutions_InstitutionWithSOs() {
        List<Institution> instituteList = institutionDAO.findAllInstitutions();
        Assertions.assertEquals(0, instituteList.size());

        //inserts institution, inserts user with that institution id and SO role
        User user = createUserWithInstitution();

        List<Institution> instituteListUpdated = institutionDAO.findAllInstitutions();
        Assertions.assertEquals(1, instituteListUpdated.size());

        Institution institution = instituteListUpdated.get(0);
        Assertions.assertEquals(1, institution.getSigningOfficials().size());
        Assertions.assertEquals(user.getInstitutionId(), institution.getId());
        Assertions.assertEquals(user.getDisplayName(),
            institution.getSigningOfficials().get(0).displayName);
    }

    @Test
    public void testFindInstitutionsByName() {
        Institution institution = createInstitution();

        List<Institution> found = institutionDAO.findInstitutionsByName(institution.getName());
        Assertions.assertFalse(found.isEmpty());
        Assertions.assertEquals(1, found.size());
        Assertions.assertEquals(institution.getId(), found.get(0).getId());
    }

    @Test
    public void testFindInstitutionsByName_Missing() {
        List<Institution> found = institutionDAO.findInstitutionsByName(RandomStringUtils.randomAlphabetic(10));
        Assertions.assertTrue(found.isEmpty());
    }

    @Test
    public void testDeleteInstitutionByUserId() {
        Institution institution = createInstitution();
        Integer userId = institution.getCreateUserId();
        institutionDAO.deleteAllInstitutionsByUser(userId);
        Assertions.assertNull(institutionDAO.findInstitutionById(institution.getId()));
    }
}
