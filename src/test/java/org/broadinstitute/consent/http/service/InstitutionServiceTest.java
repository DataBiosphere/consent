package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.models.Institution;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.anyInt;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InstitutionServiceTest {

  private InstitutionService service;

  @Mock
  private InstitutionDAO institutionDAO;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private void initService() {
    service = new InstitutionService(institutionDAO);
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
    assertNotNull(institution);
  }

  //NOTE: would like to add a test for name duplication, but exception will differ depending on whether name column has the UNIQUE keyword applied
  @Test
  public void testUpdateInstitutionById() {
    Institution mockInstitution = initMockModel();
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    initService();
    mockInstitution.setUpdateDate(new Date());
    //doNothing is default for void methods, no need to mock InstitutionDAO.updateInstitutionById
    Institution updatedInstitution = service.updateInstitutionById(mockInstitution, 1, 1);
    assertNotNull(updatedInstitution);
  }

  @Test
  public void testUpdateInstitutionBlankNameFail() {
    Institution mockInstitution = initMockModel();
    initService();
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    mockInstitution.setName("");
    String exceptionMessage = "Institution name cannot be null or empty";
    try{
      service.updateInstitutionById(mockInstitution, 1,1);
      Assert.fail("Institution PUT should not succeed with a blank name");
    } catch(Exception e) {
      assertEquals(exceptionMessage, e.getMessage());
    }
  }

  @Test
  public void testUpdateInstitutionNullNameFail() {
    Institution mockInstitution = initMockModel();
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    initService();
    String exceptionMessage = "Institution name cannot be null or empty";
    try{
      mockInstitution.setName(null);
      service.updateInstitutionById(mockInstitution, 1,1);
      Assert.fail("Institution PUT should not succeed with a null name");
    } catch(Exception e) {
      assertEquals(exceptionMessage, e.getMessage());
    }
  }

  @Test
  public void testDeleteInstitutionById() {
    initService();
    try {
      service.deleteInstitutionById(1);
    } catch (Exception e) {
      Assert.fail("Institution DELETE should not fail");
    }
  }

  @Test
  public void testFindInstitutionById() {
    initService();
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(getInstitutions().get(0));
    assertEquals(getInstitutions().get(0), service.findInstitutionById(anyInt()));
  }

  @Test
  public void testFindAllInstitutions() {
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
