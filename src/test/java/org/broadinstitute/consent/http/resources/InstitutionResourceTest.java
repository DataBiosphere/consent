package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.any;
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

  @Mock InstitutionService institutionService;
  @Mock UserService userService;
  @Mock private UriInfo info;
  @Mock private UriBuilder builder;
  @Mock private User mockUser;
  @Spy InstitutionUtil institutionUtil = new InstitutionUtil();

  private InstitutionResource resource;

  private Institution institutionSetup() {
    Institution mockInstitution = initInsitutionModel();
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
    resource = new InstitutionResource(userService, institutionService, institutionUtil);
  }

  private Institution initInsitutionModel() {
    Institution mockInstitution = new Institution();
    mockInstitution.setName("Test Name");
    return mockInstitution;
  }

  private void initResource() {
    resource = new InstitutionResource(userService, institutionService, institutionUtil);
  }

  @Test
  public void testGetInstitutionsForAdmin() {
    List<Institution> institutions = new ArrayList<Institution>();
    Institution mockInstitution = institutionSetup();
    institutions.add(mockInstitution);
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
    List<Institution> institutions = new ArrayList<Institution>();
    Institution mockInstitution = institutionSetup();
    institutions.add(mockInstitution);
    when(userService.findUserByEmail(anyString())).thenReturn(researcherUser);
    when(institutionService.findAllInstitutions()).thenReturn(institutions);
    initResource();
    Response researcherResponse = resource.getInstitutions(authUser);
    String json = researcherResponse.getEntity().toString();
    assertEquals(200, researcherResponse.getStatus());
    assertNotNull(json);
  }

  @Test
  public void getInstitutionAdmin() {
    Institution mockInstitution = institutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    String id = "1";
    Response adminResponse = resource.getInstitution(authUser, id);
    String json = adminResponse.getEntity().toString();
    assertEquals(adminResponse.getStatus(), 200);
    assertNotNull(json);
  }

  @Test
  public void getInstitutionNonAdmin() {
    Institution mockInstitution = institutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(researcherUser);
    when(institutionService.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    String id = "1";
    Response researcherResponse = resource.getInstitution(authUser, id);
    String json = researcherResponse.getEntity().toString();
    assertEquals(200, researcherResponse.getStatus());
    assertNotNull(json);
  }

  @Test
  public void testCreateInstitution() {
    Institution mockInstitution = institutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.createInstitution(any(), anyInt())).thenReturn(mockInstitution);
    String requestJson = new Gson().toJson(mockInstitution, Institution.class);
    Response response = resource.createInstitution(authUser, requestJson);
    String json = response.getEntity().toString();
    assertEquals(200, response.getStatus());
    assertNotNull(json);
  }

  @Test
  public void testUpdateInstitution() {
    Institution mockInstitution = institutionSetup();
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenReturn(mockInstitution);
    String paramId = "1";
    Response response = resource.updateInstitution(authUser, paramId, new Gson().toJson(mockInstitution));
    assertEquals(200, response.getStatus());
    assertNotNull(response.getEntity().toString());
  }

  @Test
  public void testDeleteInstitution() { 
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.updateInstitutionById(any(), anyInt(), anyInt())).thenReturn(null);
    String paramId = "1";
    Response response = resource.deleteInstitution(authUser, paramId);
    assertEquals(200, response.getStatus());
  }  
}
