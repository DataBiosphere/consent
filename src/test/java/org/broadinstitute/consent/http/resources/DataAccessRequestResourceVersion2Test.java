package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.MatchProcessAPI;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AbstractMatchProcessAPI.class})
public class DataAccessRequestResourceVersion2Test {

  @Mock private DataAccessRequestService dataAccessRequestService;
  @Mock private MatchProcessAPI matchProcessAPI;
  @Mock private EmailNotifierService emailNotifierService;
  @Mock private UserService userService;
  @Mock UriInfo info;
  @Mock UriBuilder builder;

  private final AuthUser authUser = new AuthUser("test@test.com");
  private final User user = new User(1, authUser.getName(), "Display Name", new Date());

  private DataAccessRequestResourceVersion2 resource;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    PowerMockito.mockStatic(AbstractMatchProcessAPI.class);
  }

  private void initResource() {
    try {
      when(userService.findUserByEmail(any())).thenReturn(user);
      when(builder.path(anyString())).thenReturn(builder);
      when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
      when(info.getRequestUriBuilder()).thenReturn(builder);
      when(dataAccessRequestService.createDataAccessRequest(any(), any()))
          .thenReturn(Collections.emptyList());
      doNothing().when(matchProcessAPI).processMatchesForPurpose(any());
      doNothing().when(emailNotifierService).sendNewDARRequestMessage(any(), any());
      when(AbstractMatchProcessAPI.getInstance()).thenReturn(matchProcessAPI);
      resource =
          new DataAccessRequestResourceVersion2(
              dataAccessRequestService, emailNotifierService, userService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }
  }

  @Test
  public void testPost() {
    initResource();
    Response response = resource.createDataAccessRequest(authUser, info, "");
    assertEquals(201, response.getStatus());
  }
}
