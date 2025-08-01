package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
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
  void testCreateInstitutionSuccess() throws Exception {
    Institution mockInstitution = initMockModel();
    when(institutionDAO.insertFullInstitution(mockInstitution, 1)).thenReturn(mockInstitution);
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
  void testUpdateInstitutionById() throws Exception {
    Institution mockInstitution = initMockModel();
    mockInstitution.setId(1);
    when(institutionDAO.findInstitutionById(mockInstitution.getId())).thenReturn(mockInstitution);
    when(institutionDAO.updateFullInstitution(mockInstitution, 1)).thenReturn(mockInstitution);
    initService();
    mockInstitution.setUpdateDate(new Date());
    //doNothing is default for void methods, no need to mock InstitutionDAO.updateInstitutionById
    Institution updatedInstitution = service.updateInstitutionById(mockInstitution, mockInstitution.getId(), 1);
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
    assertEquals(u.getDisplayName(), signingOfficials.get(0).getDisplayName());
    assertEquals(u.getEmail(), signingOfficials.get(0).getEmail());
    assertEquals(u.getUserId(), signingOfficials.get(0).getUserId());
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

  @Test
  void testCreateInstitutionDomainUniquenessAllUnique() {
    Institution mockInstitution = initMockModel();
    mockInstitution.setDomains(List.of("broadinstitute.org", "broad.mit.edu", "café.com"));

    when(institutionDAO.findInstitutionIdByDomain("broadinstitute.org")).thenReturn(null);
    when(institutionDAO.findInstitutionIdByDomain("broad.mit.edu")).thenReturn(null);
    when(institutionDAO.findInstitutionIdByDomain("café.com")).thenReturn(null);

    initService();

    assertDoesNotThrow(() -> {
      service.createInstitution(mockInstitution, 1);
    });
  }

  @Test
  void testCreateInstitutionDomainUniquenessSomeUnique() {
    Institution mockInstitution = initMockModel();
    mockInstitution.setDomains(List.of("broadinstitute.org", "broad.mit.edu"));

    when(institutionDAO.findInstitutionIdByDomain("broadinstitute.org")).thenReturn(2);
    when(institutionDAO.findInstitutionIdByDomain("broad.mit.edu")).thenReturn(null);

    initService();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      service.createInstitution(mockInstitution, 1);
    });

    assertTrue(exception.getMessage().contains("broadinstitute.org"));
    assertFalse(exception.getMessage().contains("broad.mit.edu"));
  }

  @Test
  void testCreateInstitutionDomainUniquenessNoneUnique() {
    Institution mockInstitution = initMockModel();
    mockInstitution.setDomains(List.of("broadinstitute.org", "broad.mit.edu"));

    when(institutionDAO.findInstitutionIdByDomain("broadinstitute.org")).thenReturn(2);
    when(institutionDAO.findInstitutionIdByDomain("broad.mit.edu")).thenReturn(2);

    initService();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      service.createInstitution(mockInstitution, 1);
    });

    assertTrue(exception.getMessage().contains("broadinstitute.org"));
    assertTrue(exception.getMessage().contains("broad.mit.edu"));
  }

  @Test
  void testCheckDomainUniquenessUpdateSameInstitution() throws Exception {
    Institution mockInstitution = initMockModel();
    mockInstitution.setId(1);
    mockInstitution.setDomains(List.of("broadinstitute.org"));

    // If we're updating the same institution, it should not throw an error
    when(institutionDAO.findInstitutionIdByDomain("broadinstitute.org")).thenReturn(1);
    when(institutionDAO.findInstitutionById(1)).thenReturn(mockInstitution);
    when(institutionDAO.updateFullInstitution(mockInstitution, 1)).thenReturn(mockInstitution);

    initService();

    assertDoesNotThrow(() -> {
      service.updateInstitutionById(mockInstitution, 1, 1);
    });
  }

  @Test
  void testCheckDomainUniquenessUpdateDifferentInstitution() {
    Institution mockInstitution = initMockModel();
    mockInstitution.setId(1);
    mockInstitution.setDomains(List.of("broadinstitute.org"));

    when(institutionDAO.findInstitutionIdByDomain("broadinstitute.org")).thenReturn(2);
    when(institutionDAO.findInstitutionById(1)).thenReturn(mockInstitution);

    initService();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      service.updateInstitutionById(mockInstitution, 1, 1);
    });

    assertEquals("Domain(s) already associated with another institution: broadinstitute.org",
        exception.getMessage());
  }

  @Test
  void testCreateInstitutionNameUniqueness() {
    Institution newInstitution = initMockModel();
    newInstitution.setName("Broad Institute");

    when(institutionDAO.findInstitutionsByName("Broad Institute")).thenReturn(Collections.emptyList());

    initService();

    assertDoesNotThrow(() -> {
      service.createInstitution(newInstitution, 1);
    });
  }

  @Test
  void testCreateInstitutionNameUniquenessConflict() {
    Institution newInstitution = initMockModel();
    newInstitution.setName("Broad Institute");

    Institution existingConflictingInstitution = new Institution();
    existingConflictingInstitution.setId(2);
    existingConflictingInstitution.setName("Broad Institute");

    when(institutionDAO.findInstitutionsByName("Broad Institute")).thenReturn(List.of(existingConflictingInstitution));

    initService();

    ConsentConflictException exception = assertThrows(ConsentConflictException.class, () -> {
      service.createInstitution(newInstitution, 1);
    });

    assertTrue(exception.getMessage().contains("An institution exists with the name of 'Broad Institute'"));
  }

  @Test
  void testUpdateInstitutionNameUniqueness() throws Exception {
    Institution updatedInstitution = initMockModel();
    updatedInstitution.setId(1);
    updatedInstitution.setName("Broad Institute");

    when(institutionDAO.findInstitutionById(1)).thenReturn(updatedInstitution);
    when(institutionDAO.findInstitutionsByName("Broad Institute")).thenReturn(Collections.emptyList());
    when(institutionDAO.updateFullInstitution(updatedInstitution, 1)).thenReturn(updatedInstitution);

    initService();

    assertDoesNotThrow(() -> {
      service.updateInstitutionById(updatedInstitution, 1, 1);
    });
  }

  @Test
  void testUpdateInstitutionNameUniquenessConflict() {
    Institution updatedInstitution = initMockModel();
    updatedInstitution.setId(1);
    updatedInstitution.setName("Broad Institute");

    Institution existingConflictingInstitution = new Institution();
    existingConflictingInstitution.setId(2);
    existingConflictingInstitution.setName("Broad Institute");

    when(institutionDAO.findInstitutionById(1)).thenReturn(updatedInstitution);
    when(institutionDAO.findInstitutionsByName("Broad Institute")).thenReturn(List.of(existingConflictingInstitution));

    initService();

    ConsentConflictException exception = assertThrows(ConsentConflictException.class, () -> {
      service.updateInstitutionById(updatedInstitution, 1, 1);
    });

    assertTrue(exception.getMessage().contains("An institution exists with the name of 'Broad Institute'"));
  }

  @Test
  void testCreateInstitutionDuplicateDomainsInPayload() {
    Institution newInstitution = initMockModel();
    newInstitution.setName("Broad Institute");
    newInstitution.setDomains(List.of("broadinstitute.org", "broadinstitute.org"));

    when(institutionDAO.findInstitutionsByName("Broad Institute")).thenReturn(Collections.emptyList());

    initService();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      service.createInstitution(newInstitution, 1);
    });

    assertEquals("Institution domains must be unique", exception.getMessage());
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
