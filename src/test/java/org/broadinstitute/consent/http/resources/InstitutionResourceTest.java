package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.InstitutionService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitutionResourceTest {

  private final AuthUser authUser = new AuthUser("test@test.com");
  private final User user = new User(1, "test@test.com", "Display Name", new Date(),
      Collections.emptyList());
  private final DuosUser duosUser = new DuosUser(authUser, user);

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

  @BeforeEach
  void initResource() {
    resource = new InstitutionResource(institutionService);
  }

  @Test
  void testGetInstitutionsForAdmin() {
    List<Institution> institutions = Collections.singletonList(mockInstitutionSetup());
    when(institutionService.findAllInstitutions()).thenReturn(institutions);

    try (var adminResponse = resource.getInstitutions(duosUser)) {
      String json = adminResponse.getEntity().toString();
      assertEquals(200, adminResponse.getStatus());
      assertNotNull(json);
    }
  }

  @Test
  void testGetInstitutionsForNonAdmin() {
    List<Institution> institutions = Collections.singletonList(mockInstitutionSetup());
    when(institutionService.findAllInstitutions()).thenReturn(institutions);

    try (var researcherResponse = resource.getInstitutions(duosUser)) {
      String json = researcherResponse.getEntity().toString();
      assertEquals(200, researcherResponse.getStatus());
      assertNotNull(json);
    }
  }

  @Test
  void testGetInstitutionAdmin() {
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.findInstitutionById(anyInt())).thenReturn(mockInstitution);

    try (var adminResponse = resource.getInstitution(duosUser, 1)) {
      String json = adminResponse.getEntity().toString();
      assertEquals(200, adminResponse.getStatus());
      assertNotNull(json);
    }
  }

  @Test
  void testGetInstitutionNonAdmin() {
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.findInstitutionById(anyInt())).thenReturn(mockInstitution);

    try (var researcherResponse = resource.getInstitution(duosUser, 1)) {
      String json = researcherResponse.getEntity().toString();
      assertEquals(200, researcherResponse.getStatus());
      assertNotNull(json);
    }
  }

  @Test
  void testGetInstitutionFail() {
    Exception error = new NotFoundException("Institution not found");
    when(institutionService.findInstitutionById(anyInt())).thenThrow(error);

    try (var response = resource.getInstitution(duosUser, 1)) {
      assertEquals(404, response.getStatus());
    }
  }


  @Test
  void testCreateInstitution() {
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.createInstitution(any(), anyInt())).thenReturn(mockInstitution);

    String requestJson = GsonUtil.getInstance().toJson(mockInstitution, Institution.class);
    try (var response = resource.createInstitution(duosUser, requestJson)) {
      String json = response.getEntity().toString();
      assertEquals(200, response.getStatus());
      assertNotNull(json);
    }
  }

  @Test
  void testCreateInstitutionNullName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.createInstitution(any(), anyInt())).thenThrow(error);

    try (var response = resource.createInstitution(duosUser,
        GsonUtil.getInstance().toJson(mockInstitution))) {
      assertEquals(400, response.getStatus());
    }
  }

  @Test
  void testCreateInstitutionBlankName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.createInstitution(any(), anyInt())).thenThrow(error);

    try (var response = resource.createInstitution(duosUser,
        GsonUtil.getInstance().toJson(mockInstitution))) {
      assertEquals(400, response.getStatus());
    }
  }

  @Test
  void testCreateInstitutionDuplicate() {
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.findAllInstitutionsByName(any())).thenReturn(List.of(mockInstitution));

    try (var response = resource.createInstitution(duosUser,
        GsonUtil.getInstance().toJson(mockInstitution))) {
      assertEquals(409, response.getStatus());
    }
  }

  @Test
  void testPatchInstitution() throws Exception {
    Institution mockInstitution = mockInstitutionSetup();
    mockInstitution.setId(1);
    when(institutionService.findInstitutionById(mockInstitution.getId())).thenReturn(
        mockInstitution);
    when(institutionService.updateInstitutionById(mockInstitution, mockInstitution.getId(),
        duosUser.getUserId())).thenReturn(
        mockInstitution);
    initResource();
    try (var response = resource.patchInstitution(duosUser, 1,
        GsonUtil.getInstance().toJson(mockInstitution))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertNotNull(response.getEntity().toString());
    }
  }

  @Test
  void testPatchInstitutionNotFound() {
    Institution mockInstitution = mockInstitutionSetup();
    mockInstitution.setId(1);
    when(institutionService.findInstitutionById(mockInstitution.getId())).thenThrow(
        new NotFoundException("Institution not found"));
    initResource();
    try (var response = resource.patchInstitution(duosUser, 1,
        GsonUtil.getInstance().toJson(mockInstitution))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
      assertNotNull(response.getEntity().toString());
    }
  }

  @Test
  void testPatchInstitutionBadRequest() {
    Institution mockInstitution = mockInstitutionSetup();
    mockInstitution.setId(1);
    when(institutionService.findInstitutionById(mockInstitution.getId())).thenReturn(
        mockInstitution);
    initResource();
    try (var response = resource.patchInstitution(duosUser, 1, "bad json")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
      assertNotNull(response.getEntity().toString());
    }
  }

  @Test
  void testPatchInstitutionServerError() throws Exception {
    Institution mockInstitution = mockInstitutionSetup();
    mockInstitution.setId(1);
    when(institutionService.findInstitutionById(mockInstitution.getId())).thenReturn(
        mockInstitution);
    when(institutionService.updateInstitutionById(mockInstitution, mockInstitution.getId(),
        duosUser.getUserId())).thenThrow(
        new ServerErrorException(HttpStatusCodes.STATUS_CODE_SERVER_ERROR));
    initResource();
    try (var response = resource.patchInstitution(duosUser, 1,
        GsonUtil.getInstance().toJson(mockInstitution))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
      assertNotNull(response.getEntity().toString());
    }
  }

  @Test
  void testUpdateInstitution() throws Exception {
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenReturn(
        mockInstitution);

    try (var response = resource.updateInstitution(duosUser, 1,
        GsonUtil.getInstance().toJson(mockInstitution))) {
      assertEquals(200, response.getStatus());
      assertNotNull(response.getEntity().toString());
    }
  }

  @Test
  void testUpdateInstitutionNotFound() throws Exception {
    Exception error = new NotFoundException("Institution not found");
    Institution mockInstitution = mockInstitutionSetup();
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenThrow(error);

    try (var response = resource.updateInstitution(duosUser, 1,
        GsonUtil.getInstance().toJson(mockInstitution))) {
      assertEquals(404, response.getStatus());
    }
  }

  @Test
  void testUpdateInstiutionNullName() throws Exception {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    mockInstitution.setName(null);
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenThrow(error);

    try (var response = resource.updateInstitution(duosUser, 1,
        GsonUtil.getInstance().toJson(mockInstitution))) {
      assertEquals(400, response.getStatus());
    }
  }

  @Test
  void testUpdateInstiutionBlankName() throws Exception {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    mockInstitution.setName("");
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenThrow(error);

    try (var response = resource.updateInstitution(duosUser, 1,
        GsonUtil.getInstance().toJson(mockInstitution))) {
      assertEquals(400, response.getStatus());
    }
  }

  @Test
  void testDeleteInstitution() {

    try (var response = resource.deleteInstitution(duosUser, 1)) {
      assertEquals(204, response.getStatus());
    }
  }

  @Test
  void testDeleteInstitutionNotFound() {
    Exception error = new NotFoundException("Institution not found");
    doThrow(error).when(institutionService).deleteInstitutionById(anyInt());

    try (var response = resource.deleteInstitution(duosUser, 1)) {
      assertEquals(404, response.getStatus());
    }
  }

  @Test
  void testUpdateInstitutionDomains() throws Exception {
    Institution mockInstitution = mockInstitutionSetup();
    String institutionDomainMapJson = """
        {
          "institutionDomainMap":
          {
            "%s": ["test.edu"]
          }
        }
        """.formatted(mockInstitution.getName());
    when(institutionService.findAllInstitutionsByName(mockInstitution.getName())).thenReturn(
        List.of(mockInstitution));
    try (var response = resource.updateInstitutionDomains(duosUser, institutionDomainMapJson)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertTrue(mockInstitution.getDomains().contains("test.edu"));
      verify(institutionService, times(1)).updateInstitutionById(mockInstitution,
          mockInstitution.getId(), duosUser.getUserId());
    }
  }

  @Test
  void testUpdateInstitutionDomainsMissingInstitution() throws Exception {
    Institution mockInstitution = mockInstitutionSetup();
    String institutionDomainMapJson = """
        {
          "institutionDomainMap":
          {
            "%s": ["test.edu"]
          }
        }
        """.formatted(mockInstitution.getName());
    when(institutionService.findAllInstitutionsByName(mockInstitution.getName())).thenReturn(
        List.of());
    try (var response = resource.updateInstitutionDomains(duosUser, institutionDomainMapJson)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertNull(mockInstitution.getDomains());
      verify(institutionService, never()).updateInstitutionById(mockInstitution,
          mockInstitution.getId(), duosUser.getUserId());
    }
  }

  @Test
  void testUpdateInstitutionDomainsBadJson() throws Exception {
    try (var response = resource.updateInstitutionDomains(duosUser, "bad json")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
      verify(institutionService, never()).findAllInstitutionsByName(any());
      verify(institutionService, never()).updateInstitutionById(any(), any(), any());
    }
  }
}
