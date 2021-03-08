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

  private void initDAO() {
    institutionDAO.insertInstitution(anyString(), anyString(), anyString(), anyInt(), eq(new Date()));
    institutionDAO.insertInstitution(anyString(), anyString(), anyString(), anyInt(), eq(new Date()));
  }

  @Test
  public void testCreateInstitution() {
    initService();
    when(institutionDAO.insertInstitution(anyString(), anyString(), anyString(), anyInt(), new Date())).thenReturn(getInstitutions().get(0).getId());
    Integer instituteId = service.createInstitution(anyString(), anyString(), anyString(), anyInt());
    Assert.assertSame(getInstitutions().get(0).getId(), instituteId);
  }

  @Test
  public void testUpdateInstitutionById() {
    initDAO();
    initService();
    institutionDAO.insertInstitution(anyString(), anyString(), anyString(), anyInt(), eq(new Date()));
    service.updateInstitutionById(eq(0), eq("New Name"), anyString(), anyString(), anyInt());
    Assert.assertEquals(institutionDAO.findInstitutionById(0).getName(), "New Name");
  }

  @Test
  public void testDeleteInstitutionById() {
    initDAO();
    initService();
    institutionDAO.insertInstitution(anyString(), anyString(), anyString(), anyInt(), eq(new Date()));
    System.out.println(institutionDAO.findAllInstitutions());
    Assert.assertNotNull(institutionDAO.findInstitutionById(0));
    service.deleteInstitutionById(1);
    Assert.assertNull(institutionDAO.findInstitutionById(0));
  }

  @Test
  public void testFindInstitutionById() {
    initDAO();
    initService();
    int instituteId = 1;
    when(institutionDAO.findInstitutionById(instituteId)).thenReturn(getInstitutions().get(0));
    Assert.assertEquals(getInstitutions().get(0), service.findInstitutionById(instituteId));
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
    return IntStream.range(1, 5).
      mapToObj(i -> {
        Institution institute = new Institution(anyInt(), anyString(), anyString(), anyString(), anyInt(), eq(new Date()));
        institute.setId(i);
        return institute;
      }).collect(Collectors.toList());
  }
}