package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SamServiceTest {

  @Mock private SamDAO samDAO;

  @Mock private DuosUser duosUser;

  private SamService service;

  @BeforeEach
  void setUp() {
    service = new SamService(samDAO);
  }

  @Test
  void getResourceTypes() throws Exception {
    List<ResourceType> expected = List.of();
    when(samDAO.getResourceTypes(duosUser)).thenReturn(expected);

    List<ResourceType> result = service.getResourceTypes(duosUser);

    assertSame(expected, result);
    verify(samDAO).getResourceTypes(duosUser);
  }

  @Test
  void getRegistrationInfo() throws Exception {
    UserStatusInfo expected = new UserStatusInfo();
    when(samDAO.getRegistrationInfo(duosUser)).thenReturn(expected);

    UserStatusInfo result = service.getRegistrationInfo(duosUser);

    assertSame(expected, result);
    verify(samDAO).getRegistrationInfo(duosUser);
  }

  @Test
  void getSelfDiagnostics() throws Exception {
    UserStatusDiagnostics expected = new UserStatusDiagnostics();
    when(samDAO.getSelfDiagnostics(duosUser)).thenReturn(expected);

    UserStatusDiagnostics result = service.getSelfDiagnostics(duosUser);

    assertSame(expected, result);
    verify(samDAO).getSelfDiagnostics(duosUser);
  }

  @Test
  void postRegistrationInfo() throws Exception {
    UserStatus expected = new UserStatus();
    when(samDAO.postRegistrationInfo(duosUser)).thenReturn(expected);

    UserStatus result = service.postRegistrationInfo(duosUser);

    assertSame(expected, result);
    verify(samDAO).postRegistrationInfo(duosUser);
  }

  @Test
  void asyncPostRegistrationInfo() {
    service.asyncPostRegistrationInfo(duosUser);

    verify(samDAO).asyncPostRegistrationInfo(duosUser);
  }

  @Test
  void getCombinedUserStatusInfo() throws Exception {
    UserStatusInfo expected = new UserStatusInfo();
    when(samDAO.getCombinedUserStatusInfo(duosUser)).thenReturn(expected);

    UserStatusInfo result = service.getCombinedUserStatusInfo(duosUser);

    assertSame(expected, result);
    verify(samDAO).getCombinedUserStatusInfo(duosUser);
  }

  @Test
  void getToSText() throws Exception {
    String expected = "tos-text";
    when(samDAO.getToSText()).thenReturn(expected);

    String result = service.getToSText();

    assertSame(expected, result);
    verify(samDAO).getToSText();
  }

  @Test
  void postTosAcceptedStatus_callsAcceptThenFetchResponse() throws Exception {
    TosResponse expected = new TosResponse("acceptedOn", true, "acceptedVersion", true);
    when(samDAO.getTosResponse(duosUser)).thenReturn(expected);

    TosResponse result = service.postTosAcceptedStatus(duosUser);

    assertSame(expected, result);

    InOrder order = inOrder(samDAO);
    order.verify(samDAO).acceptTosStatus(duosUser);
    order.verify(samDAO).getTosResponse(duosUser);
  }

  @Test
  void removeTosAcceptedStatus_callsRejectThenFetchResponse() throws Exception {
    TosResponse expected = new TosResponse("acceptedOn", true, "acceptedVersion", true);
    when(samDAO.getTosResponse(duosUser)).thenReturn(expected);

    TosResponse result = service.removeTosAcceptedStatus(duosUser);

    assertSame(expected, result);

    InOrder order = inOrder(samDAO);
    order.verify(samDAO).rejectTosStatus(duosUser);
    order.verify(samDAO).getTosResponse(duosUser);
  }
}
