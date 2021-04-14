package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.InstitutionService;

import org.broadinstitute.consent.http.util.InstitutionUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class InstitutionResourceTest {
  private final AuthUser authUser = new AuthUser("test@test.com");
  private final List<UserRole> adminRoles = Collections.singletonList(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
  private final List<UserRole> researcherRoles = Collections.singletonList(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
  private final User adminUser = new User(1, authUser.getName(), "Display Name", new Date(), adminRoles, authUser.getName());
  private final User researcherUser = new User(1, authUser.getName(), "Display Name", new Date(), researcherRoles, authUser.getName());

  @Mock private InstitutionService institutionService;
  @Mock private UserService userService;
  @Mock private UriInfo info;
  @Mock private UriBuilder builder;
  @Mock private User mockUser;
  @Spy private InstitutionUtil institutionUtil = new InstitutionUtil();

  private InstitutionResource resource;

  private Institution mockInstitutionSetup() {
    Institution mockInstitution = new Institution();
    mockInstitution.setName("Test Name");
    mockInstitution.setCreateDate(new Date());
    mockInstitution.setCreateUser(1);
    mockInstitution.setUpdateDate(new Date());
    mockInstitution.setUpdateUser(1);
    mockInstitution.setId(1);
    return mockInstitution;
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private void initResource() {
    resource = new InstitutionResource(userService, institutionService);
  }

  @Test
  public void testGetInstitutionsForAdmin() {
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
  public void testGetInstitutionsForNonAdmin() {
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
  public void testGetInstitutionAdmin() {
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
  public void testGetInstitutionNonAdmin() {
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
  public void testGetInstitutionFail() {
    Exception error = new NotFoundException("Institution not found");
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.findInstitutionById(anyInt())).thenThrow(error);
    initResource();
    Response response = resource.getInstitution(authUser, 1);
    assertEquals(404, response.getStatus());
  }
  

  @Test
  public void testCreateInstitution() {
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.createInstitution(any(), anyInt())).thenReturn(mockInstitution);
    initResource();
    String requestJson = new Gson().toJson(mockInstitution, Institution.class);
    Response response = resource.createInstitution(authUser, requestJson);
    String json = response.getEntity().toString();
    assertEquals(200, response.getStatus());
    assertNotNull(json);
  }

  @Test
  public void testCreateInstitutionNullName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.createInstitution(any(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.createInstitution(authUser, new Gson().toJson(mockInstitution));
    assertEquals(400, response.getStatus());
  }

  @Test
  public void testCreateInstitutionBlankName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.createInstitution(any(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.createInstitution(authUser, new Gson().toJson(mockInstitution));
    assertEquals(400, response.getStatus());
  }

  @Test
  public void testUpdateInstitution() {
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenReturn(mockInstitution);
    initResource();
    Response response = resource.updateInstitution(authUser, 1, new Gson().toJson(mockInstitution));
    assertEquals(200, response.getStatus());
    assertNotNull(response.getEntity().toString());
  }

  @Test
  public void testUpdateInstitutionNotFound() {
    Exception error = new NotFoundException("Institution not found");
    Institution mockInstitution = mockInstitutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.updateInstitution(authUser, 1, new Gson().toJson(mockInstitution));
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testUpdateInstiutionNullName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    mockInstitution.setName(null);
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.updateInstitution(authUser, 1, new Gson().toJson(mockInstitution));
    assertEquals(400, response.getStatus());
  }
  @Test
  public void testUpdateInstiutionBlankName() {
    Exception error = new IllegalArgumentException("Institution name cannot be null or empty");
    Institution mockInstitution = mockInstitutionSetup();
    mockInstitution.setName("");
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenThrow(error);
    initResource();
    Response response = resource.updateInstitution(authUser, 1, new Gson().toJson(mockInstitution));
    assertEquals(400, response.getStatus());
  }

  @Test
  public void testDeleteInstitution() { 
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    initResource();
    Response response = resource.deleteInstitution(authUser, 1);
    assertEquals(204, response.getStatus());
  }
  
  @Test
  public void testDeleteInstitutionNotFound() {
    Exception error = new NotFoundException("Institution not found");
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    doThrow(error).when(institutionService).deleteInstitutionById(anyInt());
    initResource();
    Response response = resource.deleteInstitution(authUser, 1);
    assertEquals(404, response.getStatus());
  }
}
