package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.models.Institution;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

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

  @Test
  public void testCreateInstitution() {
    initService();
    //mock findByInstitution (not insertInstitution) because that is the call that service.createInstitution returns
    when(institutionDAO.findInstitutionById(anyInt())).thenReturn(getInstitutions().get(0));
    Institution institute = service.createInstitution(anyString(), anyString(), anyString(), anyInt(), eq(new Date()));
    assertEquals(getInstitutions().get(0), institute);
  }

  @Test
  public void testUpdateInstitutionById() {
    initService();
    //doNothing is default for void methods, no need to mock InstitutionDAO.updateInstitutionById
    try {
      service.updateInstitutionById(anyInt(), eq("New Name"), anyString(), anyString(), anyInt(), eq(new Date()));
    } catch (Exception e) {
      Assert.fail("Update should not fail");
    }
  }

  @Test(expected = Throwable.class)
  public void testUpdateInstitutionByIdFail() {
    initService();
    doThrow(new Exception("Update method should pass on error from DAO")).when(institutionDAO).updateInstitutionById(anyInt(), anyString(), anyString(), anyString(), anyInt(), eq(new Date()));
    service.updateInstitutionById(anyInt(), eq("New Name"), anyString(), anyString(), anyInt(), eq(new Date()));
  }

  @Test
  public void testDeleteInstitutionById() {
    initService();
    try {
        service.deleteInstitutionById(1);
    } catch (Exception e) {
        Assert.fail("Delete should not fail");
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
    initService();
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
