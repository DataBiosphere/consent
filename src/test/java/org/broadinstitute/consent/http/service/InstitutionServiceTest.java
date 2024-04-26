package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {

  private InstitutionService service;

  @Mock
  private InstitutionDAO institutionDAO;

  @Mock
  private UserDAO userDAO;

  private void initService() {
    service = new InstitutionService(institutionDAO, userDAO);
  }

  private Institution initMockModel() {
    Institution mockInstitution = new Institution();
    mockInstitution.setName("Test Name");
    return mockInstitution;
  }

  @Test
  void testCreateInstitutionSuccess() {
    Institution mockInstitution = initMockModel();
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    initService();
    Institution institution = service.createInstitution(mockInstitution, 1);
    assertNotNull(institution);
  }

  @Test
  void testCreateInstitutionBlankName() {
    Institution mockInstitution = initMockModel();
    mockInstitution.setName("");
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.createInstitution(mockInstitution, 1);
    });
  }

  @Test
  void testCreateInstitutionNullName() {
    Institution mockInstitution = initMockModel();
    mockInstitution.setName(null);
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.createInstitution(mockInstitution, 1);
    });
  }

  @Test
  void testUpdateInstitutionById() {
    Institution mockInstitution = initMockModel();
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    initService();
    mockInstitution.setUpdateDate(new Date());
    //doNothing is default for void methods, no need to mock InstitutionDAO.updateInstitutionById
    Institution updatedInstitution = service.updateInstitutionById(mockInstitution, 1, 1);
    assertNotNull(updatedInstitution);
  }

  @Test
  void testUpdateInstitutionByIdNotFound() {
    Institution mockInstitution = initMockModel();
    when(institutionDAO.findInstitutionById(anyInt())).thenThrow(new NotFoundException());
    initService();
    assertThrows(NotFoundException.class, () -> {
      service.updateInstitutionById(mockInstitution, 1, 1);
    });
  }

  @Test
  void testUpdateInstitutionBlankNameFail() {
    Institution mockInstitution = initMockModel();
    mockInstitution.setName("");
    initService();
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    assertThrows(IllegalArgumentException.class, () -> {
      service.updateInstitutionById(mockInstitution, 1, 1);
    });
  }

  @Test
  void testUpdateInstitutionNullNameFail() {
    Institution mockInstitution = initMockModel();
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    initService();
    mockInstitution.setName(null);
    assertThrows(IllegalArgumentException.class, () -> {
      service.updateInstitutionById(mockInstitution, 1, 1);
    });
  }

  @Test
  void testDeleteInstitutionById() {
    Institution mockInstitution = initMockModel();
    initService();
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    try {
      service.deleteInstitutionById(1);
    } catch (Exception e) {
      fail("Institution DELETE should not fail");
    }
  }

  @Test
  void testDeleteInstitutionByIdFail() {
    initService();
    when(institutionDAO.findInstitutionById(anyInt())).thenThrow(new NotFoundException());
    assertThrows(NotFoundException.class, () -> {
      service.deleteInstitutionById(1);
    });
  }

  @Test
  void testFindInstitutionByIdNoSigningOfficials() {
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(getInstitutions().get(0));
    when(userDAO.getSOsByInstitution(anyInt())).thenReturn(Collections.emptyList());
    initService();

    Institution institution = service.findInstitutionById(anyInt());
    assertEquals(getInstitutions().get(0), institution);
    assertEquals(Collections.emptyList(), institution.getSigningOfficials());
  }

  @Test
  void testFindInstitutionByIdWithSigningOfficials() {
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
    assertEquals(getInstitutions().get(0), institution);
    assertEquals(1, signingOfficials.size());
    assertEquals(u.getDisplayName(), signingOfficials.get(0).displayName);
    assertEquals(u.getEmail(), signingOfficials.get(0).email);
    assertEquals(u.getUserId(), signingOfficials.get(0).userId);
  }

  @Test
  void testFindInstitutionByIdFail() {
    initService();
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(null);
    assertThrows(NotFoundException.class, () -> {
      service.findInstitutionById(1);
    });
  }

  @Test
  void testFindAllInstitutions() {
    initService();
    when(institutionDAO.findAllInstitutions()).thenReturn(Collections.emptyList());
    assertTrue(service.findAllInstitutions().isEmpty());
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
