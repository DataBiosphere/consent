package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.service.dao.NihServiceDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NihServiceTest {

  @Mock
  private ResearcherService researcherService;

  @Mock
  private UserDAO userDAO;

  @Mock
  private NihServiceDAO nihServiceDAO;

  private NihService service;
  private NIHUserAccount nihUserAccount;
  private AuthUser authUser;

  @BeforeEach
  public void setUp() throws Exception {
    nihUserAccount = new NIHUserAccount("nih username", new ArrayList(), new Date().toString(),
        true);
    authUser = new AuthUser("test@test.com");
    MockitoAnnotations.openMocks(this);
  }

  private void initService() {
    service = new NihService(researcherService, userDAO, nihServiceDAO);
  }

  @Test
  public void testAuthenticateNih_InvalidUser() {
    initService();
    assertThrows(NotFoundException.class, () -> {
      service.authenticateNih(nihUserAccount, new AuthUser("test@test.com"), 1);
    });
  }

  @Test
  public void testAuthenticateNih() {
    List<UserProperty> props = Collections.singletonList(new UserProperty(1, 1, "test", "value"));
    when(researcherService.describeUserProperties(any())).thenReturn(props);
    User user = new User();
    user.setUserId(1);
    when(userDAO.findUserById(any())).thenReturn(user);
    initService();
    try {
      List<UserProperty> properties = service.authenticateNih(nihUserAccount, authUser,
          user.getUserId());
      assertEquals(1, properties.size());
      assertEquals(Integer.valueOf(1), properties.get(0).getPropertyId());
      verify(nihServiceDAO, times(1)).updateUserNihStatus(user, nihUserAccount);
    } catch (BadRequestException bre) {
      assert false;
    }
  }

  @Test
  public void testAuthenticateNih_BadRequest() {
    User user = new User();
    user.setUserId(1);
    when(userDAO.findUserById(any())).thenReturn(user);
    nihUserAccount.setNihUsername("");
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.authenticateNih(nihUserAccount, authUser, 1);
    });
  }

  @Test
  public void testAuthenticateNih_BadRequestNullAccount() {
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.authenticateNih(null, authUser, 1);
    });
  }

  @Test
  public void testAuthenticateNih_BadRequestNullAccountExpiration() {
    NIHUserAccount account = new NIHUserAccount();
    account.setStatus(true);
    initService();
    assertThrows(BadRequestException.class, () -> {
      service.authenticateNih(account, authUser, 1);
    });
  }

  @Test
  public void testDeleteNihAccountById() {
    doNothing().when(researcherService).deleteResearcherProperties(any());
    initService();
    service.deleteNihAccountById(1);
  }
}