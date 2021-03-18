package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.cloud.storage.BlobId;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Arrays;
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

  private InstitutionResource institutionResource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    Institution mockInstitution = new Institution();
    mockInstitution.setName("Test Name");
    when(institutionService.findInstitutionById(anyInt())).thenReturn(mockInstitution);
    when(institutionService.createInstitution(Mockito.any(Institution.class), anyInt())).thenReturn(mockInstitution);
    when(institutionService.updateInstitutionById(Mockito.any(Institution.class), anyInt(), anyInt())).thenReturn(mockInstitution);
    when(institutionService.findAllInstitutions()).thenReturn(Collections.emptyList());
    institutionResource = new InstitutionResource(userService, institutionService);
  }

  @Test
  public void testGetInsitutions() {}
}
