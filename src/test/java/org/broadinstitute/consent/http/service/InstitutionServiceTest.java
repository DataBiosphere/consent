package org.broadinstitute.consent.http.service;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class InstitutionServiceTest {

    private InstitutionService service;

    @Mock
    private InstitutionDAO institutionDAO;

    @Mock
    private UserDAO userDAO;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    private void initService() {
        service = new InstitutionService(institutionDAO, userDAO);
    }

    private Institution initMockModel() {
        Institution mockInstitution = new Institution();
        mockInstitution.setName("Test Name");
        return mockInstitution;
    }

    @Test
    public void testCreateInstitutionSuccess() {
        Institution mockInstitution = initMockModel();
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
        initService();
        Institution institution = service.createInstitution(mockInstitution, 1);
        Assertions.assertNotNull(institution);
    }

    @Test
    public void testCreateInstitutionBlankName() {
        Institution mockInstitution = initMockModel();
        mockInstitution.setName("");
        initService();
        try {
            service.createInstitution(mockInstitution, 1);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testCreateInstitutionNullName() {
        Institution mockInstitution = initMockModel();
        mockInstitution.setName(null);
        initService();
        try {
            service.createInstitution(mockInstitution, 1);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testUpdateInstitutionById() {
        Institution mockInstitution = initMockModel();
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
        initService();
        mockInstitution.setUpdateDate(new Date());
        //doNothing is default for void methods, no need to mock InstitutionDAO.updateInstitutionById
        Institution updatedInstitution = service.updateInstitutionById(mockInstitution, 1, 1);
        Assertions.assertNotNull(updatedInstitution);
    }

    @Test
    public void testUpdateInstitutionByIdNotFound() {
        Institution mockInstitution = initMockModel();
        when(institutionDAO.findInstitutionById(anyInt())).thenThrow(new NotFoundException());
        initService();
        try {
            service.updateInstitutionById(mockInstitution, 1, 1);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof NotFoundException);
        }
    }

    @Test
    public void testUpdateInstitutionBlankNameFail() {
        Institution mockInstitution = initMockModel();
        mockInstitution.setName("");
        initService();
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
        try {
            service.updateInstitutionById(mockInstitution, 1, 1);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testUpdateInstitutionNullNameFail() {
        Institution mockInstitution = initMockModel();
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
        initService();
        mockInstitution.setName(null);
        try {
            service.updateInstitutionById(mockInstitution, 1, 1);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testDeleteInstitutionById() {
        Institution mockInstitution = initMockModel();
        initService();
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
        try {
            service.deleteInstitutionById(1);
        } catch (Exception e) {
            Assertions.fail("Institution DELETE should not fail");
        }
    }

    @Test
    public void testDeleteInstitutionByIdFail() {
        initService();
        when(institutionDAO.findInstitutionById(anyInt())).thenThrow(new NotFoundException());
        try {
            service.deleteInstitutionById(1);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof NotFoundException);
        }
    }

    @Test
    public void testFindInstitutionByIdNoSigningOfficials() {
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(getInstitutions().get(0));
        when(userDAO.getSOsByInstitution(anyInt())).thenReturn(Collections.emptyList());
        initService();

        Institution institution = service.findInstitutionById(anyInt());
        Assertions.assertEquals(getInstitutions().get(0), institution);
        Assertions.assertEquals(Collections.emptyList(), institution.getSigningOfficials());
    }

    @Test
    public void testFindInstitutionByIdWithSigningOfficials() {
        User u = new User();
        String email = RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(10, 50));
        String displayName = RandomStringUtils.randomAlphabetic(RandomUtils.nextInt(10, 50));
        u.setEmail(email);
        u.setDisplayName(displayName);
        u.setUserId(RandomUtils.nextInt(1, 100));

        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(getInstitutions().get(0));
        when(userDAO.getSOsByInstitution(anyInt())).thenReturn(List.of(u));
        initService();

        Institution institution = service.findInstitutionById(anyInt());
        List<SimplifiedUser> signingOfficials = institution.getSigningOfficials();
        Assertions.assertEquals(getInstitutions().get(0), institution);
        Assertions.assertEquals(1, signingOfficials.size());
        Assertions.assertEquals(u.getDisplayName(), signingOfficials.get(0).displayName);
        Assertions.assertEquals(u.getEmail(), signingOfficials.get(0).email);
        Assertions.assertEquals(u.getUserId(), signingOfficials.get(0).userId);
    }

    @Test
    public void testFindInstitutionByIdFail() {
        initService();
        when(institutionDAO.findInstitutionById(anyInt())).thenReturn(null);
        try {
            service.findInstitutionById(1);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof NotFoundException);
        }
    }

    @Test
    public void testFindAllInstitutions() {
        initService();
        when(institutionDAO.findAllInstitutions()).thenReturn(Collections.emptyList());
        Assertions.assertTrue(service.findAllInstitutions().isEmpty());
    }


    /**
     * @return A list of 5 dacs
     */
    private List<Institution> getInstitutions() {
        return IntStream.range(0, 4).
                mapToObj(i -> {
                    Institution institute = new Institution();
                    institute.setId(i);
                    return institute;
                }).collect(Collectors.toList());
    }
}
