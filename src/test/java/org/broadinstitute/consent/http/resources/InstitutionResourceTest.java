package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.InstitutionService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InstitutionResourceTest {

  private final AuthUser authUser = new AuthUser("test@test.com");
  private final List<UserRole> adminRoles = Collections.singletonList(UserRoles.Admin());
  private final List<UserRole> researcherRoles = Collections.singletonList(UserRoles.Researcher());
  private final User adminUser = new User(1, authUser.getEmail(), "Display Name", new Date(),
      adminRoles);
  private final User researcherUser = new User(1, authUser.getEmail(), "Display Name", new Date(),
      researcherRoles);

  @Mock
  private InstitutionService institutionService;
  @Mock
  private UserService userService;

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
    resource = new InstitutionResource(userService, institutionService);
  }

  @Test
  void testGetInstitutionsForAdmin() {
    List<Institution> institutions = Collections.singletonList(mockInstitutionSetup());
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.findAllInstitutions()).thenReturn(institutions);
    initResource();
    Response adminResponse = resource.getInstitutions(authUser);
    String json = adminResponse.getEntity().toString();
    assertEquals(200, adminResponse.getStatus());
    assertNotNull(json);
  }

  @Test
  void testGetInstitutionsForNonAdmin() {
    List<Institution> institutions = Collections.singletonList(mockInstitutionSetup());
    when(userService.findUserByEmail(anyString())).thenReturn(researcherUser);
    when(institutionService.findAllInstitutions()).thenReturn(institutions);
    initResource();
    Response researcherResponse = resource.getInstitutions(authUser);
    String json = researcherResponse.getEntity().toString();
    assertEquals(200, researcherResponse.getStatus());
    assertNotNull(json);
  }

  @Test
  void testGetInstitutionAdmin() {
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    initResource();
    Response adminResponse = resource.getInstitution(authUser, 1);
    String json = adminResponse.getEntity().toString();
    assertEquals(adminResponse.getStatus(), 200);
    assertNotNull(json);
  }

  @Test
  void testGetInstitutionNonAdmin() {
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(researcherUser);
    when(institutionService.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    initResource();
    Response researcherResponse = resource.getInstitution(authUser, 1);
    String json = researcherResponse.getEntity().toString();
    assertEquals(200, researcherResponse.getStatus());
    assertNotNull(json);
  }

  @Test
  void testGetInstitutionFail() {
    Exception error = new NotFoundException("Institution not found");
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.findInstitutionById(anyInt())).thenThrow(error);
    initResource();
    Response response = resource.getInstitution(authUser, 1);
    assertEquals(404, response.getStatus());
  }


  @Test
  void testCreateInstitution() {
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.createInstitution(any(), anyInt())).thenReturn(mockInstitution);
    initResource();
    String requestJson = GsonUtil.gsonBuilderWithAdapters().create().toJson(mockInstitution, Institution.class);
    Response response = resource.createInstitution(authUser, requestJson);
    String json = response.getEntity().toString();
    assertEquals(200, response.getStatus());
    assertNotNull(json);
  }

  @Test
  void testCreateInstitutionNullName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.createInstitution(any(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.createInstitution(authUser,
        GsonUtil.gsonBuilderWithAdapters().create().toJson(mockInstitution));
    assertEquals(400, response.getStatus());
  }

  @Test
  void testCreateInstitutionBlankName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.createInstitution(any(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.createInstitution(authUser,
        GsonUtil.gsonBuilderWithAdapters().create().toJson(mockInstitution));
    assertEquals(400, response.getStatus());
  }

  @Test
  void testCreateInstitutionDuplicate() {
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.findAllInstitutionsByName(any())).thenReturn(List.of(mockInstitution));
    initResource();
    Response response = resource.createInstitution(authUser,
        GsonUtil.gsonBuilderWithAdapters().create().toJson(mockInstitution));
    assertEquals(409, response.getStatus());
  }

  @Test
  void testUpdateInstitution() {
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenReturn(
        mockInstitution);
    initResource();
    Response response = resource.updateInstitution(authUser, 1,
        GsonUtil.gsonBuilderWithAdapters().create().toJson(mockInstitution));
    assertEquals(200, response.getStatus());
    assertNotNull(response.getEntity().toString());
  }

  @Test
  void testUpdateInstitutionNotFound() {
    Exception error = new NotFoundException("Institution not found");
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.updateInstitution(authUser, 1,
        GsonUtil.gsonBuilderWithAdapters().create().toJson(mockInstitution));
    assertEquals(404, response.getStatus());
  }

  @Test
  void testUpdateInstiutionNullName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    mockInstitution.setName(null);
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.updateInstitution(authUser, 1,
        GsonUtil.gsonBuilderWithAdapters().create().toJson(mockInstitution));
    assertEquals(400, response.getStatus());
  }

  @Test
  void testUpdateInstiutionBlankName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    mockInstitution.setName("");
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.updateInstitution(authUser, 1,
        GsonUtil.gsonBuilderWithAdapters().create().toJson(mockInstitution));
    assertEquals(400, response.getStatus());
  }

  @Test
  void testDeleteInstitution() {
    initResource();
    Response response = resource.deleteInstitution(authUser, 1);
    assertEquals(204, response.getStatus());
  }

  @Test
  void testDeleteInstitutionNotFound() {
    Exception error = new NotFoundException("Institution not found");
    doThrow(error).when(institutionService).deleteInstitutionById(anyInt());
    initResource();
    Response response = resource.deleteInstitution(authUser, 1);
    assertEquals(404, response.getStatus());
  }
}
