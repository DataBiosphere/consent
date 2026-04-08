package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.passport.PassportClaim;
import org.broadinstitute.consent.http.service.passport.PassportService;
import org.broadinstitute.consent.http.service.passport.Visa;
import org.broadinstitute.consent.http.service.passport.VisaClaim;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassportResourceTest extends AbstractTestHelper {

  @Mock private PassportService passportService;
  private final AuthUser authUser = new AuthUser("test@test.com");

  @Test
  void testGetPassportSuccess() {
    User user = createUser();
    authUser.setEmail(user.getEmail());
    UserStatusInfo userStatusInfo = createUserStatusInfo(user);
    DuosUser duosUser = new DuosUser(authUser, user);
    duosUser.setUserStatusInfo(userStatusInfo);

    PassportResource resource = new PassportResource(passportService);
    try (Response response = resource.getPassport(duosUser)) {
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testGetPassportFailure() {
    DuosUser duosUser = new DuosUser(authUser, null);
    when(passportService.generatePassport(duosUser))
        .thenThrow(new RuntimeException("Passport generation failed"));

    PassportResource resource = new PassportResource(passportService);
    try (Response response = resource.getPassport(duosUser)) {
      assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testGetPassportNotFoundNullDuosUser() {
    PassportResource resource = new PassportResource(passportService);
    when(passportService.generatePassport(null)).thenThrow(new NotFoundException("User not found"));

    try (Response response = resource.getPassport(null)) {
      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testGetPassportNotFoundNullUser() {
    DuosUser duosUser = new DuosUser(authUser, null);
    PassportResource resource = new PassportResource(passportService);
    when(passportService.generatePassport(duosUser))
        .thenThrow(new NotFoundException("User not found"));

    try (Response response = resource.getPassport(duosUser)) {
      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
  }

  // -----------------------------------------------------------------------
  // getDataPassport
  // -----------------------------------------------------------------------

  @Test
  void testGetDataPassportSuccess() {
    PassportClaim mockClaim = new PassportClaim(List.of(mockVisa()));
    when(passportService.generateDataPassport("DUOS-000001")).thenReturn(mockClaim);

    PassportResource resource = new PassportResource(passportService);
    try (Response response =
        resource.getDataPassport(new DuosUser(authUser, createUser()), "DUOS-000001")) {
      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      assertEquals(mockClaim, response.getEntity());
    }
  }

  @Test
  void testGetDataPassportNotFound() {
    when(passportService.generateDataPassport("DUOS-000001"))
        .thenThrow(new NotFoundException("Dataset not found: DUOS-000001"));

    PassportResource resource = new PassportResource(passportService);
    try (Response response =
        resource.getDataPassport(new DuosUser(authUser, createUser()), "DUOS-000001")) {
      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testGetDataPassportInvalidIdentifier() {
    when(passportService.generateDataPassport("INVALID"))
        .thenThrow(new IllegalArgumentException("Could not parse identifier (INVALID)"));

    PassportResource resource = new PassportResource(passportService);
    try (Response response =
        resource.getDataPassport(new DuosUser(authUser, createUser()), "INVALID")) {
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testGetDataPassportInternalError() {
    when(passportService.generateDataPassport("DUOS-000001"))
        .thenThrow(new RuntimeException("Unexpected error"));

    PassportResource resource = new PassportResource(passportService);
    try (Response response =
        resource.getDataPassport(new DuosUser(authUser, createUser()), "DUOS-000001")) {
      assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }
  }

  private Visa mockVisa() {
    VisaClaim claim =
        new VisaClaim(
            "ConsentedDataUseTerms",
            Instant.now().getEpochSecond(),
            PassportService.ISS + "/dataset/DUOS-000001/dataUse",
            PassportService.ISS,
            "dac");
    return new Visa(
        PassportService.ISS,
        "DUOS-000001",
        Instant.now().getEpochSecond(),
        Instant.now().getEpochSecond() + PassportService.EXPIRATION_SECONDS,
        claim);
  }

  private User createUser() {
    User user = new User();
    user.setUserId(123);
    user.setEmail("test@example.org");
    user.setCreateDate(Timestamp.from(Instant.now()));
    return user;
  }

  private UserStatusInfo createUserStatusInfo(User user) {
    UserStatusInfo info = new UserStatusInfo();
    info.setUserEmail(user.getEmail());
    info.setUserSubjectId(randomAlphanumeric(10));
    info.setEnabled(true);
    info.setTosAccepted(true);
    return info;
  }
}
