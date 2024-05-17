package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
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

}
