package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosAuthUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.InstitutionService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitutionResourceTest {

  private final AuthUser authUser = new AuthUser("test@test.com");
  private final User user = new User(1, "test@test.com", "Display Name", new Date(),
      Collections.emptyList());
  private final DuosAuthUser duosAuthUserUser = new DuosAuthUser(authUser, user);

  @Mock
  private InstitutionService institutionService;

  private InstitutionResource resource;

  private Institution mockInstitutionSetup() {
    Institution mockInstitution = new Institution();
    mockInstitution.setName("Test Name");
    mockInstitution.setCreateDate(new Date());
    mockInstitution.setCreateUserId(1);
    mockInstitution.setUpdateDate(new Date());
    mockInstitution.setUpdateUserId(1);
    mockInstitution.setId(1);
    return mockInstitution;
  }

  private void initResource() {
    resource = new InstitutionResource(institutionService);
  }

  @Test
  void testGetInstitutionsForAdmin() {
    List<Institution> institutions = Collections.singletonList(mockInstitutionSetup());
    when(institutionService.findAllInstitutions()).thenReturn(institutions);
    initResource();
    Response adminResponse = resource.getInstitutions(duosAuthUserUser);
    String json = adminResponse.getEntity().toString();
    assertEquals(200, adminResponse.getStatus());
    assertNotNull(json);
  }

  @Test
  void testGetInstitutionsForNonAdmin() {
    List<Institution> institutions = Collections.singletonList(mockInstitutionSetup());
    when(institutionService.findAllInstitutions()).thenReturn(institutions);
    initResource();
    Response researcherResponse = resource.getInstitutions(duosAuthUserUser);
    String json = researcherResponse.getEntity().toString();
    assertEquals(200, researcherResponse.getStatus());
    assertNotNull(json);
  }

  @Test
  void testGetInstitutionAdmin() {
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    initResource();
    Response adminResponse = resource.getInstitution(duosAuthUserUser, 1);
    String json = adminResponse.getEntity().toString();
    assertEquals(200, adminResponse.getStatus());
    assertNotNull(json);
  }

  @Test
  void testGetInstitutionNonAdmin() {
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    initResource();
    Response researcherResponse = resource.getInstitution(duosAuthUserUser, 1);
    String json = researcherResponse.getEntity().toString();
    assertEquals(200, researcherResponse.getStatus());
    assertNotNull(json);
  }

  @Test
  void testGetInstitutionFail() {
    Exception error = new NotFoundException("Institution not found");
    when(institutionService.findInstitutionById(anyInt())).thenThrow(error);
    initResource();
    Response response = resource.getInstitution(duosAuthUserUser, 1);
    assertEquals(404, response.getStatus());
  }


  @Test
  void testCreateInstitution() {
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.createInstitution(any(), anyInt())).thenReturn(mockInstitution);
    initResource();
    String requestJson = GsonUtil.getInstance().toJson(mockInstitution, Institution.class);
    Response response = resource.createInstitution(duosAuthUserUser, requestJson);
    String json = response.getEntity().toString();
    assertEquals(200, response.getStatus());
    assertNotNull(json);
  }

  @Test
  void testCreateInstitutionNullName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.createInstitution(any(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.createInstitution(duosAuthUserUser,
        GsonUtil.getInstance().toJson(mockInstitution));
    assertEquals(400, response.getStatus());
  }

  @Test
  void testCreateInstitutionBlankName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.createInstitution(any(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.createInstitution(duosAuthUserUser,
        GsonUtil.getInstance().toJson(mockInstitution));
    assertEquals(400, response.getStatus());
  }

  @Test
  void testCreateInstitutionDuplicate() {
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.findAllInstitutionsByName(any())).thenReturn(List.of(mockInstitution));
    initResource();
    Response response = resource.createInstitution(duosAuthUserUser,
        GsonUtil.getInstance().toJson(mockInstitution));
    assertEquals(409, response.getStatus());
  }

  @Test
  void testUpdateInstitution() {
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenReturn(
        mockInstitution);
    initResource();
    Response response = resource.updateInstitution(duosAuthUserUser, 1,
        GsonUtil.getInstance().toJson(mockInstitution));
    assertEquals(200, response.getStatus());
    assertNotNull(response.getEntity().toString());
  }

  @Test
  void testUpdateInstitutionNotFound() {
    Exception error = new NotFoundException("Institution not found");
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.updateInstitution(duosAuthUserUser, 1,
        GsonUtil.getInstance().toJson(mockInstitution));
    assertEquals(404, response.getStatus());
  }

  @Test
  void testUpdateInstiutionNullName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    mockInstitution.setName(null);
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.updateInstitution(duosAuthUserUser, 1,
        GsonUtil.getInstance().toJson(mockInstitution));
    assertEquals(400, response.getStatus());
  }

  @Test
  void testUpdateInstiutionBlankName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    mockInstitution.setName("");
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.updateInstitution(duosAuthUserUser, 1,
        GsonUtil.getInstance().toJson(mockInstitution));
    assertEquals(400, response.getStatus());
  }

  @Test
  void testDeleteInstitution() {
    initResource();
    Response response = resource.deleteInstitution(duosAuthUserUser,1);
    assertEquals(204, response.getStatus());
  }

  @Test
  void testDeleteInstitutionNotFound() {
    Exception error = new NotFoundException("Institution not found");
    doThrow(error).when(institutionService).deleteInstitutionById(anyInt());
    initResource();
    Response response = resource.deleteInstitution(duosAuthUserUser, 1);
    assertEquals(404, response.getStatus());
  }
}
