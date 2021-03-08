package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.models.Institution;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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

  @Test
  public void testCreateInstitution() {
    initService();
    when(institutionDAO.insertInstitution(anyString(), anyString(), anyString(), anyInt(), eq(new Date()))).thenReturn(getInstitutions().get(0).getId());
    Integer instituteId = service.createInstitution(anyString(), anyString(), anyString(), anyInt(), eq(new Date()));
    Assert.assertSame(getInstitutions().get(0).getId(), instituteId);
  }

  @Test
  public void testUpdateInstitutionById() {
    initService();
    Integer id = institutionDAO.insertInstitution(anyString(), anyString(), anyString(), anyInt(), eq(new Date()));
    try {
      service.updateInstitutionById(eq(id), eq("New Name"), anyString(), anyString(), anyInt(), eq(new Date()));
    } catch (Exception e) {
      Assert.fail("Update should not fail");
    }
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
    Integer id = institutionDAO.insertInstitution(anyString(), anyString(), anyString(), anyInt(), eq(new Date()));
    Assert.assertEquals(institutionDAO.findInstitutionById(id), service.findInstitutionById(id));
  }

  @Test
  public void testFindAllInstitutions() {
    initService();
    when(institutionDAO.findAllInstitutions()).thenReturn(Collections.emptyList());
    initService();
    Assert.assertTrue(service.findAllInstitutions().isEmpty());
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