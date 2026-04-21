package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DacBuilder;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DacResourceTest extends AbstractTestHelper {

  @Mock private DacService dacService;

  @Mock private DatasetService datasetService;

  private DacResource dacResource;

  private final AuthUser authUser = new AuthUser("test@test.com");

  private final Gson gson = GsonUtil.buildGson();

  @BeforeEach
  void setUp() {
    dacResource = new DacResource(dacService, datasetService);
  }

  @Test
  void testFindAll_success_1() {
    when(dacService.findDacsWithMembersOption(true)).thenReturn(Collections.emptyList());

    try (Response response = dacResource.findAll(authUser, Optional.of(true))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      JsonArray dacs = getListFromEntityString(response.getEntity().toString());
      assertEquals(0, dacs.size());
    }
  }

  @Test
  void testFindAll_success_2() {
    Dac dac = new DacBuilder().setName("name").setDescription("description").build();
    when(dacService.findDacsWithMembersOption(true)).thenReturn(Collections.singletonList(dac));

    try (Response response = dacResource.findAll(authUser, Optional.of(true))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      JsonArray dacs = getListFromEntityString(response.getEntity().toString());
      assertEquals(1, dacs.size());
    }
  }

  @Test
  void testFindAllWithUsers() {
    when(dacService.findDacsWithMembersOption(false)).thenReturn(Collections.emptyList());

    try (Response response = dacResource.findAll(authUser, Optional.of(false))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      JsonArray dacs = getListFromEntityString(response.getEntity().toString());
      assertEquals(0, dacs.size());
    }
  }

  @Test
  void testFindDatasetsAssociatedWithDac_Success_Admin() {
    Dataset ds = new Dataset();
    ds.setName("test");

    User user = new User();
    user.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, user);

    when(dacService.findById(1)).thenReturn(new Dac());
    when(dacService.findDatasetsByDacId(1)).thenReturn(List.of(ds));

    try (Response response = dacResource.findAllDacDatasets(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(GsonUtil.buildGson().toJson(List.of(ds)), response.getEntity());
    }
  }

  @Test
  void testFindDatasetsAssociatedWithDac_NoDac() {
    User user = new User();
    user.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, user);

    when(dacService.findById(1)).thenReturn(null);

    try (Response response = dacResource.findAllDacDatasets(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testFindDatasetsAssociatedWithDac_Success_Chairperson() {
    Dataset ds = new Dataset();
    ds.setName("test");

    User user = new User();
    user.setUserId(10);
    user.setChairpersonRole();
    DuosUser duosUser = new DuosUser(authUser, user);

    Dac dac = new Dac();
    dac.setChairpersons(List.of(user));

    when(dacService.findById(1)).thenReturn(dac);
    when(dacService.findDatasetsByDacId(1)).thenReturn(List.of(ds));

    try (Response response = dacResource.findAllDacDatasets(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(GsonUtil.buildGson().toJson(List.of(ds)), response.getEntity());
    }
  }

  @Test
  void testFindDatasetsAssociatedWithDac_NotAuthorized() {
    User user = new User();
    user.setUserId(10);
    user.setRoles(List.of());
    DuosUser duosUser = new DuosUser(authUser, user);

    Dac dac = new Dac();
    dac.setChairpersons(List.of());

    when(dacService.findById(1)).thenReturn(dac);

    try (Response response = dacResource.findAllDacDatasets(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
    }
  }

  @Test
  void testCreateDac_success() {
    Dac dac = new DacBuilder().setName("name").setDescription("description").build();
    User actingUser = new User();
    actingUser.setUserId(1);
    DuosUser duosUser = new DuosUser(authUser, actingUser);
    when(dacService.createDac(any(), any(), any())).thenReturn(1);
    when(dacService.findById(1)).thenReturn(dac);

    try (Response response = dacResource.createDac(duosUser, gson.toJson(dac))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCreateDacWithEmail_success() {
    Dac dac =
        new DacBuilder()
            .setName("name")
            .setDescription("description")
            .setEmail("test@email.com")
            .build();
    User actingUser = new User();
    actingUser.setUserId(1);
    DuosUser duosUser = new DuosUser(authUser, actingUser);
    when(dacService.createDac(any(), any(), any(), any())).thenReturn(1);
    when(dacService.findById(1)).thenReturn(dac);

    try (Response response = dacResource.createDac(duosUser, gson.toJson(dac))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCreateDac_badRequest_1() {
    DuosUser duosUser = new DuosUser(authUser, new User());
    try (Response response = dacResource.createDac(duosUser, null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testCreateDac_badRequest_2() {
    Dac dac = new DacBuilder().setName(null).setDescription("description").build();
    DuosUser duosUser = new DuosUser(authUser, new User());
    try (Response response = dacResource.createDac(duosUser, gson.toJson(dac))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testCreateDac_badRequest_3() {
    Dac dac = new DacBuilder().setName("name").setDescription(null).build();
    DuosUser duosUser = new DuosUser(authUser, new User());
    try (Response response = dacResource.createDac(duosUser, gson.toJson(dac))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateDac_success() {
    User user = new User();
    user.setUserId(1);
    user.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, user);
    Dac dac = new DacBuilder().setDacId(1).setName("name").setDescription("description").build();
    doNothing()
        .when(dacService)
        .updateDac(isA(String.class), isA(String.class), isA(Integer.class), isA(Integer.class));
    when(dacService.findById(1)).thenReturn(dac);

    try (Response response = dacResource.updateDac(duosUser, gson.toJson(dac))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testUpdateDacWithEmail_success() {
    User user = new User();
    user.setUserId(1);
    user.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, user);
    Dac dac =
        new DacBuilder()
            .setDacId(1)
            .setName("name")
            .setEmail("test@email.com")
            .setDescription("description")
            .build();
    doNothing()
        .when(dacService)
        .updateDac(
            isA(String.class),
            isA(String.class),
            isA(String.class),
            isA(Integer.class),
            isA(Integer.class));
    when(dacService.findById(1)).thenReturn(dac);

    try (Response response = dacResource.updateDac(duosUser, gson.toJson(dac))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testUpdateDac_badRequest_1() {
    DuosUser duosUser = new DuosUser(authUser, new User());
    try (Response response = dacResource.updateDac(duosUser, null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateDac_badRequest_2() {
    User user = new User();
    user.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, user);
    Dac dac = new DacBuilder().setDacId(null).setName("name").setDescription("description").build();
    try (Response response = dacResource.updateDac(duosUser, gson.toJson(dac))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateDac_badRequest_3() {
    User user = new User();
    user.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, user);
    Dac dac = new DacBuilder().setDacId(1).setName(null).setDescription("description").build();
    try (Response response = dacResource.updateDac(duosUser, gson.toJson(dac))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateDac_badRequest_4() {
    User user = new User();
    user.setAdminRole();
    DuosUser duosUser = new DuosUser(authUser, user);
    Dac dac = new DacBuilder().setDacId(1).setName("name").setDescription(null).build();
    try (Response response = dacResource.updateDac(duosUser, gson.toJson(dac))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateDac_notAuthorized() {
    Dac dac = new DacBuilder().setDacId(1).setName("name").setDescription("description").build();
    User user = new User();
    user.setChairpersonRoleWithDAC(dac.getDacId() + 1);
    DuosUser duosUser = new DuosUser(authUser, user);

    try (Response response = dacResource.updateDac(duosUser, gson.toJson(dac))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
    }
  }

  @Test
  void testFindById_success() {
    Dac dac =
        new DacBuilder()
            .setDacId(1)
            .setName("name")
            .setDescription("description")
            .setAssociatedDaa(new DataAccessAgreement())
            .build();
    when(dacService.findById(1)).thenReturn(dac);

    try (Response response = dacResource.findDacById(authUser, dac.getDacId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testFindById_failure() {
    when(dacService.findById(1)).thenReturn(null);

    try (Response response = dacResource.findDacById(authUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testDeleteDac_success() {
    Dac dac = new DacBuilder().setDacId(1).setName("name").setDescription("description").build();
    when(dacService.findById(1)).thenReturn(dac);
    User user = buildUser();
    DuosUser duosUser = new DuosUser(authUser, user);

    try (Response response = dacResource.deleteDac(duosUser, dac.getDacId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testDeleteDac_failure() {
    when(dacService.findById(1)).thenReturn(null);
    User user = buildUser();
    DuosUser duosUser = new DuosUser(authUser, user);

    try (Response response = dacResource.deleteDac(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testAddDacMemberAsAdmin() {
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    User admin = buildAdmin(authUser);
    DuosUser duosUser = new DuosUser(authUser, admin);
    User member = buildUser();
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    try (Response response =
        dacResource.addDacMember(duosUser, dac.getDacId(), member.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testAddDacMemberAsChairSuccess() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(chair);
    when(dacService.findById(any())).thenReturn(dac);
    DuosUser duosUser = new DuosUser(authUser, chair);
    User member = buildUser();
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    try (Response response =
        dacResource.addDacMember(duosUser, dac.getDacId(), member.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testAddDacMemberAsChairFailure() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    DuosUser duosUser = new DuosUser(authUser, chair);
    User member = buildUser();
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    try (Response response =
        dacResource.addDacMember(duosUser, dac.getDacId(), member.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
    }
  }

  @Test
  void testRemoveDacMemberAsAdmin() {
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    User admin = buildAdmin(authUser);
    DuosUser duosUser = new DuosUser(authUser, admin);
    User member = buildUser();
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    try (Response response =
        dacResource.removeDacMember(duosUser, dac.getDacId(), member.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testRemoveDacMemberAsChairSuccess() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(chair);
    when(dacService.findById(any())).thenReturn(dac);
    DuosUser duosUser = new DuosUser(authUser, chair);
    User member = buildUser();
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    try (Response response =
        dacResource.removeDacMember(duosUser, dac.getDacId(), member.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testRemoveDacMemberAsChairFailure() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    DuosUser duosUser = new DuosUser(authUser, chair);
    User member = buildUser();
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    try (Response response =
        dacResource.removeDacMember(duosUser, dac.getDacId(), member.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
    }
  }

  @Test
  void testAddDacChairAsAdmin() {
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    User admin = buildAdmin(authUser);
    DuosUser duosUser = new DuosUser(authUser, admin);
    User member = buildUser();
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    try (Response response =
        dacResource.addDacChair(duosUser, dac.getDacId(), member.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testAddDacChairAsChairSuccess() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(chair);
    when(dacService.findById(any())).thenReturn(dac);
    DuosUser duosUser = new DuosUser(authUser, chair);
    User member = buildUser();
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    try (Response response =
        dacResource.addDacChair(duosUser, dac.getDacId(), member.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testAddDacChairAsChairFailure() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    DuosUser duosUser = new DuosUser(authUser, chair);
    User member = buildUser();
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    try (Response response =
        dacResource.addDacChair(duosUser, dac.getDacId(), member.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
    }
  }

  @Test
  void testRemoveDacChairAsAdmin() {
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    User admin = buildAdmin(authUser);
    DuosUser duosUser = new DuosUser(authUser, admin);
    User member = buildUser();
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    try (Response response =
        dacResource.removeDacChair(duosUser, dac.getDacId(), member.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testRemoveDacChairAsChairSuccess() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(chair);
    when(dacService.findById(any())).thenReturn(dac);
    DuosUser duosUser = new DuosUser(authUser, chair);
    User member = buildUser();
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    try (Response response =
        dacResource.removeDacChair(duosUser, dac.getDacId(), member.getUserId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testRemoveDacChairAsChairFailure() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    DuosUser duosUser = new DuosUser(authUser, chair);
    User member = buildUser();
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    int dacId = dac.getDacId();
    int memberId = member.getUserId();
    try (Response response = dacResource.removeDacChair(duosUser, dacId, memberId)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, response.getStatus());
    }
  }

  @Test
  void testApproveDataset_UserNotFound() {
    User user = new User();
    DuosUser duosUser = new DuosUser(authUser, user);
    try (Response response = dacResource.approveDataset(duosUser, 1, 1, "test")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testApproveDataset_DacIdMismatch() {
    User user = new User();
    Dataset dataset = new Dataset();
    dataset.setDacId(2);
    DuosUser duosUser = new DuosUser(authUser, user);
    when(datasetService.findDatasetWithoutFSOInformation(anyInt())).thenReturn(dataset);

    try (Response response = dacResource.approveDataset(duosUser, 1, 1, "test")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testApproveDataset_UserDifferentChair() {
    User user = new User();
    user.setChairpersonRoleWithDAC(2);
    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    DuosUser duosUser = new DuosUser(authUser, user);
    when(datasetService.findDatasetWithoutFSOInformation(anyInt())).thenReturn(dataset);
    try (Response response = dacResource.approveDataset(duosUser, 1, 1, "test")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testApproveDataset_EmptyPayload() {
    User user = new User();
    user.setChairpersonRoleWithDAC(1);
    DuosUser duosUser = new DuosUser(authUser, user);
    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    when(datasetService.findDatasetWithoutFSOInformation(anyInt())).thenReturn(dataset);
    try (Response response = dacResource.approveDataset(duosUser, 1, 1, "{}")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testApproveDataset_AlreadyApproved_TrueSubmission() {
    User user = new User();
    user.setChairpersonRoleWithDAC(1);
    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    dataset.setDacApproval(true);
    DuosUser duosUser = new DuosUser(authUser, user);
    when(datasetService.findDatasetWithoutFSOInformation(anyInt())).thenReturn(dataset);
    when(datasetService.approveDataset(any(Dataset.class), any(User.class), anyBoolean()))
        .thenReturn(dataset);
    try (Response response = dacResource.approveDataset(duosUser, 1, 1, "{approval: true}")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(GsonUtil.buildGson().toJson(dataset), response.getEntity());
    }
  }

  @Test
  void testApproveDataset() {
    User user = new User();
    user.setChairpersonRoleWithDAC(1);
    Dataset dataset = new Dataset();
    Dataset datasetResponse = new Dataset();
    datasetResponse.setDacId(1);
    datasetResponse.setDacApproval(true);
    dataset.setDacId(1);
    dataset.setDacApproval(false);
    DuosUser duosUser = new DuosUser(authUser, user);
    when(datasetService.findDatasetWithoutFSOInformation(anyInt())).thenReturn(dataset);
    when(datasetService.approveDataset(any(Dataset.class), any(User.class), anyBoolean()))
        .thenReturn(datasetResponse);
    try (Response response = dacResource.approveDataset(duosUser, 1, 1, "{approval: true}")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(GsonUtil.buildGson().toJson(datasetResponse), response.getEntity());
    }
  }

  @Test
  void testApproveDataset_AlreadyApproved_NonTrueSubmission() {
    User user = new User();
    user.setChairpersonRoleWithDAC(1);
    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    dataset.setDacApproval(true);
    DuosUser duosUser = new DuosUser(authUser, user);
    when(datasetService.findDatasetWithoutFSOInformation(anyInt())).thenReturn(dataset);
    when(datasetService.approveDataset(any(Dataset.class), any(User.class), anyBoolean()))
        .thenThrow(ForbiddenException.class);
    try (Response response = dacResource.approveDataset(duosUser, 1, 1, "{approval: false}")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  private JsonArray getListFromEntityString(String str) {
    return GsonUtil.buildGson().fromJson(str, JsonArray.class);
  }

  private Dac buildDac(User chair) {
    Dac dac =
        new DacBuilder().setDacId(nextInt()).setName("name").setDescription("description").build();
    if (Objects.nonNull(chair)) {
      dac.setChairpersons(Collections.singletonList(chair));
    }
    return dac;
  }

  private User buildAdmin(AuthUser authUser) {
    User user = buildUser();
    user.setUserId(nextInt());
    user.setEmail(authUser.getEmail());
    user.setAdminRole();
    return user;
  }

  private User buildChair(AuthUser authUser) {
    User user = buildUser();
    user.setUserId(nextInt());
    user.setEmail(authUser.getEmail());
    user.setChairpersonRole();
    return user;
  }

  private User buildUser() {
    User user = new User();
    user.setUserId(nextInt());
    return user;
  }
}
