package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.MatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchResourceTest {

  @Mock private MatchService service;

  private final AuthUser authUser = new AuthUser("test");
  private final List<UserRole> roles = List.of(UserRoles.Researcher());
  private final User user = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);

  private final DuosUser duosUser = new DuosUser(authUser, user);

  private MatchResource resource;

  private void initResource() {
    resource = new MatchResource(service);
  }

  @Test
  void testGetMatchesForPurpose() {
    initResource();

    Response response =
        resource.getMatchesForLatestDataAccessElectionsByPurposeIds(
            duosUser, UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetMatchesForPurpose_EmptyParam() {
    initResource();
    Response response = resource.getMatchesForLatestDataAccessElectionsByPurposeIds(duosUser, "");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testGetMatchesForPurpose_CommaSeparatedBlanks() {
    initResource();
    Response response =
        resource.getMatchesForLatestDataAccessElectionsByPurposeIds(duosUser, " , , ,");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testGetMatchesForPurpose_PartialValidIds() {
    Match match = new Match();
    match.setId(2);
    when(service.findMatchesForLatestDataAccessElectionsByPurposeIds(anyList()))
        .thenReturn(List.of(match));
    initResource();

    Response response =
        resource.getMatchesForLatestDataAccessElectionsByPurposeIds(duosUser, "3, , 5, ");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testReprocessPurposeMatches() {
    doNothing().when(service).reprocessMatchesForPurpose(any());
    when(service.findMatchesByPurposeId(any())).thenReturn(Collections.singletonList(new Match()));
    initResource();

    Response response = resource.reprocessPurposeMatches(duosUser, UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }
}
