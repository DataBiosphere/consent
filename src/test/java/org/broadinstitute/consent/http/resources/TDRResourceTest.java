package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.tdr.ApprovedUser;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.TDRService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TDRResourceTest {

  @Mock
  private TDRService tdrService;
  @Mock
  private DatasetService datasetService;
  private TDRResource resource;

  @Mock
  private User user;

  @Mock
  private DuosUser duosUser;

  private void initResource() {
    try {
      resource = new TDRResource(tdrService, datasetService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }
  }

  @Test
  void testGetApprovedUsersForDataset() {
    String ds = "DUOS-00003";
    List<ApprovedUser> users = List.of(
        new ApprovedUser("asdf1@gmail.com"),
        new ApprovedUser("asdf2@gmail.com"));
    ApprovedUsers approvedUsers = new ApprovedUsers(users);

    Dataset d = new Dataset();
    Study study = new Study();
    study.setPublicVisibility(Boolean.TRUE);
    d.setStudy(study);

    when(tdrService.getApprovedUsersForDataset(any(), any())).thenReturn(approvedUsers);
    when(datasetService.findMinimalDatasetByIdentifier(user, ds, false)).thenReturn(d);
    when(duosUser.getUser()).thenReturn(user);

    initResource();

    Response r = resource.getApprovedUsers(duosUser, ds);
    assertEquals(200, r.getStatus());
    assertEquals(approvedUsers, r.getEntity());
  }

  @Test
  void testGetApprovedUsersForDataset404() {
    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findMinimalDatasetByIdentifier(user, "DUOS-00003", false)).thenReturn(null);

    initResource();

    Response r = resource.getApprovedUsers(duosUser, "DUOS-00003");

    assertEquals(404, r.getStatus());
  }

  @Test
  void testGetDatasetByIdentifier() {

    Dataset d = new Dataset();
    d.setName("test");

    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findMinimalDatasetByIdentifier(user, "DUOS-00003", true)).thenReturn(d);

    initResource();

    Response r = resource.getDatasetByIdentifier(duosUser, "DUOS-00003");

    assertEquals(200, r.getStatus());
    assertEquals(GsonUtil.buildGson().toJson(d), r.getEntity());
  }


  @Test
  void testGetDatasetByIdentifier404() {
    when(duosUser.getUser()).thenReturn(user);
    when(datasetService.findMinimalDatasetByIdentifier(user, "DUOS-00003", true)).thenReturn(null);

    initResource();

    Response r = resource.getDatasetByIdentifier(duosUser, "DUOS-00003");

    assertEquals(404, r.getStatus());
  }

}
