package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.cloud.storage.BlobId;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.io.IOUtils;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.InstitutionService;

import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class InstitutionResourceTest {
  private final int OK = HttpStatusCodes.STATUS_CODE_OK;
  private final int NOT_FOUND = HttpStatusCodes.STATUS_CODE_NOT_FOUND;
  private final int ERROR = HttpStatusCodes.STATUS_CODE_SERVER_ERROR;

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

  private InstitutionResource resource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    resource = new InstitutionResource(userService, institutionService);
  }

  private Institution initInsitutionModel() {
    Institution mockInstitution = new Institution();
    mockInstitution.setName("Test Name");
    return mockInstitution;
  }

  private void initResource() {
    resource = new InstitutionResource(userService, institutionService);
  }

  @Test
  public void testGetInsitutionsForAdmin() {
    List<Institution> institutions = new ArrayList<Institution>();
    Institution mockInstitution = initInsitutionModel();
    mockInstitution.setCreateDate(new Date());
    mockInstitution.setCreateUser(1);
    mockInstitution.setUpdateDate(new Date());
    mockInstitution.setUpdateUser(1);
    mockInstitution.setId(1);
    institutions.add(mockInstitution);
    when(userService.findUserByEmail(anyString())).thenReturn(adminUser);
    when(institutionService.findAllInstitutions()).thenReturn(institutions);
    initResource();
    try{
      Response adminResponse = resource.getInstitutions(authUser);
      String json = adminResponse.getEntity().toString();
      Type institutionType = new TypeToken<List<Institution>>(){}.getType();
      List<Institution> institutionsResponse = new Gson().fromJson(json, institutionType);
      Institution targetInstitution = institutionsResponse.get(0);
      assertEquals(adminResponse.getStatus(), 200);
      assertEquals(targetInstitution.getName(), mockInstitution.getName());
      assertEquals(targetInstitution.getCreateUser(), mockInstitution.getCreateUser());
      assertEquals(targetInstitution.getUpdateUser(), mockInstitution.getUpdateUser());
      assertEquals(targetInstitution.getCreateDate().toString(), mockInstitution.getCreateDate().toString());
      assertEquals(targetInstitution.getUpdateDate().toString(), mockInstitution.getUpdateDate().toString());
    } catch(Exception e) {
      Assert.fail(e.getMessage());
    }
  }
}
