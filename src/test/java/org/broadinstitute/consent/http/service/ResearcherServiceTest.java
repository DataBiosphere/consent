package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResearcherServiceTest {

  @Mock
  private UserPropertyDAO userPropertyDAO;

  @Mock
  private UserDAO userDAO;

  private ResearcherService service;

  private AuthUser authUser;

  private User user;

  @BeforeEach
  void setUp() {
    authUser = new AuthUser().setEmail("test@gmail.com").setName("Test User");
    user = new User();
    user.setEmail(authUser.getEmail());
    user.setUserId(RandomUtils.nextInt(1, 10));
    user.setDisplayName(RandomStringUtils.randomAlphabetic(10));
  }

  private void initService() {
    service = new ResearcherService(userPropertyDAO, userDAO);
  }

  @Test
  void testUpdateProperties() {
    when(userDAO.findUserById(any())).thenReturn(user);
    when(userDAO.findUserByEmail(any())).thenReturn(user);
    when(userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(any(), any())).thenReturn(
        List.of());
    doNothing().when(userPropertyDAO).insertAll(any());
    initService();
    Map<String, String> props = new HashMap<>();
    props.put(UserFields.SUGGESTED_INSTITUTION.getValue(), "suggestion");
    props.put(UserFields.SUGGESTED_INSTITUTION.getValue(), "suggestion");
    props.put(UserFields.SELECTED_SIGNING_OFFICIAL_ID.getValue(), "suggestion");
    props.put(UserFields.ERA_STATUS.getValue(), "suggestion");
    props.put(UserFields.ERA_EXPIRATION_DATE.getValue(), "suggestion");
    List<UserProperty> userProps = service.updateProperties(props, authUser, true);
    assertTrue(userProps.isEmpty());
  }

}
