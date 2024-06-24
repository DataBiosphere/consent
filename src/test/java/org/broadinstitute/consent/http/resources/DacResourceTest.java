package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DacBuilder;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DacResourceTest {

  @Mock
  private DacService dacService;

  @Mock
  private DatasetService datasetService;

  @Mock
  private UserService userService;

  @Mock
  private ElasticSearchService elasticSearchService;

  private DacResource dacResource;

  private final AuthUser authUser = new AuthUser("test@test.com");

  private final Gson gson = GsonUtil.buildGson();

  @BeforeEach
  void setUp() {
    dacResource = new DacResource(dacService, userService, datasetService, elasticSearchService);
  }

  @Test
  void testFindAll_success_1() {
    when(dacService.findDacsWithMembersOption(true)).thenReturn(Collections.emptyList());

    Response response = dacResource.findAll(authUser, Optional.of(true));
    assertEquals(200, response.getStatus());
    JsonArray dacs = getListFromEntityString(response.getEntity().toString());
    assertEquals(0, dacs.size());
  }

  @Test
  void testFindAll_success_2() {
    Dac dac = new DacBuilder()
        .setName("name")
        .setDescription("description")
        .build();
    when(dacService.findDacsWithMembersOption(true)).thenReturn(Collections.singletonList(dac));

    Response response = dacResource.findAll(authUser, Optional.of(true));
    assertEquals(200, response.getStatus());
    JsonArray dacs = getListFromEntityString(response.getEntity().toString());
    assertEquals(1, dacs.size());
  }

  @Test
  void testFindAllWithUsers() {
    when(dacService.findDacsWithMembersOption(false)).thenReturn(Collections.emptyList());

    Response response = dacResource.findAll(authUser, Optional.of(false));
    assertEquals(200, response.getStatus());
    JsonArray dacs = getListFromEntityString(response.getEntity().toString());
    assertEquals(0, dacs.size());
  }

  @Test
  void testFindDatasetsAssociatedWithDac_Success_Admin() {
    Dataset ds = new Dataset();
    ds.setName("test");

    User user = new User();
    user.setAdminRole();

    when(dacService.findById(1)).thenReturn(new Dac());
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(dacService.findDatasetsByDacId(1)).thenReturn(List.of(ds));

    Response response = dacResource.findAllDacDatasets(authUser, 1);
    assertEquals(200, response.getStatus());
    assertEquals(GsonUtil.buildGson().toJson(List.of(ds)), response.getEntity());
  }

  @Test
  void testFindDatasetsAssociatedWithDac_NoDac() {
    Dataset ds = new Dataset();
    ds.setName("test");

    User user = new User();
    user.setAdminRole();

    when(dacService.findById(1)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> {
      dacResource.findAllDacDatasets(authUser, 1);
    });
  }

  @Test
  void testFindDatasetsAssociatedWithDac_Success_Chairperson() {
    Dataset ds = new Dataset();
    ds.setName("test");

    User user = new User();
    user.setUserId(10);
    user.setChairpersonRole();

    Dac dac = new Dac();
    dac.setChairpersons(List.of(user));

    when(dacService.findById(1)).thenReturn(dac);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(dacService.findDatasetsByDacId(1)).thenReturn(List.of(ds));

    Response response = dacResource.findAllDacDatasets(authUser, 1);
    assertEquals(200, response.getStatus());
    assertEquals(GsonUtil.buildGson().toJson(List.of(ds)), response.getEntity());
  }

  @Test
  void testFindDatasetsAssociatedWithDac_NotAuthorized() {
    User user = new User();
    user.setUserId(10);
    user.setRoles(List.of());

    Dac dac = new Dac();
    dac.setChairpersons(List.of());

    when(dacService.findById(1)).thenReturn(dac);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);

    assertThrows(NotAuthorizedException.class, () -> {
      dacResource.findAllDacDatasets(authUser, 1);
    });
  }


  @Test
  void testCreateDac_success() throws Exception {
    Dac dac = new DacBuilder()
        .setName("name")
        .setDescription("description")
        .build();
    when(dacService.createDac(any(), any())).thenReturn(1);
    when(dacService.findById(1)).thenReturn(dac);

    Response response = dacResource.createDac(authUser, gson.toJson(dac));
    assertEquals(200, response.getStatus());
  }

  @Test
  void testCreateDacWithEmail_success() throws Exception {
    Dac dac = new DacBuilder()
        .setName("name")
        .setDescription("description")
        .setEmail("test@email.com")
        .build();
    when(dacService.createDac(any(), any(), any())).thenReturn(1);
    when(dacService.findById(1)).thenReturn(dac);

    Response response = dacResource.createDac(authUser, gson.toJson(dac));
    assertEquals(200, response.getStatus());
  }

  @Test
  void testCreateDac_badRequest_1() {
    assertThrows(BadRequestException.class, () -> {
      dacResource.createDac(authUser, null);
    });
  }

  @Test
  void testCreateDac_badRequest_2() {
    Dac dac = new DacBuilder()
        .setName(null)
        .setDescription("description")
        .build();

    assertThrows(BadRequestException.class, () -> {
      dacResource.createDac(authUser, gson.toJson(dac));
    });
  }

  @Test
  void testCreateDac_badRequest_3() {
    Dac dac = new DacBuilder()
        .setName("name")
        .setDescription(null)
        .build();

    assertThrows(BadRequestException.class, () -> {
      dacResource.createDac(authUser, gson.toJson(dac));
    });
  }


  @Test
  void testUpdateDac_success() {
    Dac dac = new DacBuilder()
        .setDacId(1)
        .setName("name")
        .setDescription("description")
        .build();
    doNothing().when(dacService)
        .updateDac(isA(String.class), isA(String.class), isA(Integer.class));
    when(dacService.findById(1)).thenReturn(dac);

    Response response = dacResource.updateDac(authUser, gson.toJson(dac));
    assertEquals(200, response.getStatus());
  }

  @Test
  void testUpdateDacWithEmail_success() {
    Dac dac = new DacBuilder()
        .setDacId(1)
        .setName("name")
        .setEmail("test@email.com")
        .setDescription("description")
        .build();
    doNothing().when(dacService)
        .updateDac(isA(String.class), isA(String.class), isA(String.class), isA(Integer.class));
    when(dacService.findById(1)).thenReturn(dac);

    Response response = dacResource.updateDac(authUser, gson.toJson(dac));
    assertEquals(200, response.getStatus());
  }

  @Test
  void testUpdateDac_badRequest_1() {
    assertThrows(BadRequestException.class, () -> {
      dacResource.updateDac(authUser, null);
    });
  }

  @Test
  void testUpdateDac_badRequest_2() {
    Dac dac = new DacBuilder()
        .setDacId(null)
        .setName("name")
        .setDescription("description")
        .build();
    assertThrows(BadRequestException.class, () -> {
      dacResource.updateDac(authUser, gson.toJson(dac));
    });
  }

  @Test
  void testUpdateDac_badRequest_3() {
    Dac dac = new DacBuilder()
        .setDacId(1)
        .setName(null)
        .setDescription("description")
        .build();
    assertThrows(BadRequestException.class, () -> {
      dacResource.updateDac(authUser, gson.toJson(dac));
    });
  }

  @Test
  void testUpdateDac_badRequest_4() {
    Dac dac = new DacBuilder()
        .setDacId(1)
        .setName("name")
        .setDescription(null)
        .build();
    assertThrows(BadRequestException.class, () -> {
      dacResource.updateDac(authUser, gson.toJson(dac));
    });
  }

  @Test
  void testFindById_success() {
    Dac dac = new DacBuilder()
        .setDacId(1)
        .setName("name")
        .setDescription("description")
        .setAssociatedDaa(new DataAccessAgreement())
        .build();
    when(dacService.findById(1)).thenReturn(dac);

    Response response = dacResource.findById(dac.getDacId());
    assertEquals(200, response.getStatus());
  }

  @Test
  void testFindById_failure() {
    when(dacService.findById(1)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> {
      dacResource.findById(1);
    });
  }

  @Test
  void testDeleteDac_success() {
    Dac dac = new DacBuilder()
        .setDacId(1)
        .setName("name")
        .setDescription("description")
        .build();
    when(dacService.findById(1)).thenReturn(dac);

    Response response = dacResource.deleteDac(dac.getDacId());
    assertEquals(200, response.getStatus());

  }

  @Test
  void testDeleteDac_failure() {
    when(dacService.findById(1)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> {
      dacResource.deleteDac(1);
    });
  }

  @Test
  void testAddDacMemberAsAdmin() {
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    User admin = buildAdmin(authUser);
    User member = buildUser();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(admin);
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    Response response = dacResource.addDacMember(authUser, dac.getDacId(), member.getUserId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testAddDacMemberAsChairSuccess() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(chair);
    when(dacService.findById(any())).thenReturn(dac);
    User member = buildUser();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(chair);
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    Response response = dacResource.addDacMember(authUser, dac.getDacId(), member.getUserId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testAddDacMemberAsChairFailure() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    User member = buildUser();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(chair);
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    assertThrows(NotAuthorizedException.class, () -> {
      dacResource.addDacMember(authUser, dac.getDacId(), member.getUserId());
    });
  }

  @Test
  void testRemoveDacMemberAsAdmin() {
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    User admin = buildAdmin(authUser);
    User member = buildUser();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(admin);
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    Response response = dacResource.removeDacMember(authUser, dac.getDacId(), member.getUserId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testRemoveDacMemberAsChairSuccess() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(chair);
    when(dacService.findById(any())).thenReturn(dac);
    User member = buildUser();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(chair);
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    Response response = dacResource.removeDacMember(authUser, dac.getDacId(), member.getUserId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testRemoveDacMemberAsChairFailure() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    User member = buildUser();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(chair);
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    assertThrows(NotAuthorizedException.class, () -> {
      dacResource.removeDacMember(authUser, dac.getDacId(), member.getUserId());
    });
  }

  @Test
  void testAddDacChairAsAdmin() {
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    User admin = buildAdmin(authUser);
    User member = buildUser();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(admin);
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    Response response = dacResource.addDacChair(authUser, dac.getDacId(), member.getUserId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testAddDacChairAsChairSuccess() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(chair);
    when(dacService.findById(any())).thenReturn(dac);
    User member = buildUser();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(chair);
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    Response response = dacResource.addDacChair(authUser, dac.getDacId(), member.getUserId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testAddDacChairAsChairFailure() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    User member = buildUser();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(chair);
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    assertThrows(NotAuthorizedException.class, () -> {
      dacResource.addDacChair(authUser, dac.getDacId(), member.getUserId());
    });
  }

  @Test
  void testRemoveDacChairAsAdmin() {
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    User admin = buildAdmin(authUser);
    User member = buildUser();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(admin);
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    Response response = dacResource.removeDacChair(authUser, dac.getDacId(), member.getUserId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testRemoveDacChairAsChairSuccess() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(chair);
    when(dacService.findById(any())).thenReturn(dac);
    User member = buildUser();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(chair);
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    Response response = dacResource.removeDacChair(authUser, dac.getDacId(), member.getUserId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testRemoveDacChairAsChairFailure() {
    User chair = buildChair(authUser);
    Dac dac = buildDac(null);
    when(dacService.findById(any())).thenReturn(dac);
    User member = buildUser();
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(chair);
    when(dacService.findUserById(member.getUserId())).thenReturn(member);

    assertThrows(NotAuthorizedException.class, () -> {
      dacResource.removeDacChair(authUser, dac.getDacId(), member.getUserId());
    });
  }

  @Test
  void testApproveDataset_UserNotFound() {
    when(userService.findUserByEmail(anyString())).thenThrow(NotFoundException.class);
    Response response = dacResource.approveDataset(authUser, 1, 1, "test");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testApproveDataset_DacIdMismatch() {
    User user = new User();
    Dataset dataset = new Dataset();
    dataset.setDacId(2);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(datasetService.findDatasetById(anyInt())).thenReturn(dataset);

    Response response = dacResource.approveDataset(authUser, 1, 1, "test");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testApproveDataset_UserDifferentChair() {
    User user = new User();
    user.setChairpersonRoleWithDAC(2);
    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(datasetService.findDatasetById(anyInt())).thenReturn(dataset);
    Response response = dacResource.approveDataset(authUser, 1, 1, "test");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testApproveDataset_EmptyPayload() {
    User user = new User();
    user.setChairpersonRoleWithDAC(1);
    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(datasetService.findDatasetById(anyInt())).thenReturn(dataset);
    Response response = dacResource.approveDataset(authUser, 1, 1, "{}");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testApproveDataset_AlreadyApproved_TrueSubmission() throws Exception {
    User user = new User();
    user.setChairpersonRoleWithDAC(1);
    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    dataset.setDacApproval(true);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(datasetService.findDatasetById(anyInt())).thenReturn(dataset);
    when(datasetService.approveDataset(any(Dataset.class), any(User.class), anyBoolean()))
        .thenReturn(dataset);
    when(elasticSearchService.indexDataset(any())).thenReturn(Response.ok().build());
    Response response = dacResource.approveDataset(authUser, 1, 1, "{approval: true}");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(GsonUtil.buildGson().toJson(dataset), response.getEntity());
  }

  @Test
  void testApproveDataset() throws Exception {
    User user = new User();
    user.setChairpersonRoleWithDAC(1);
    Dataset dataset = new Dataset();
    Dataset datasetResponse = new Dataset();
    datasetResponse.setDacId(1);
    datasetResponse.setDacApproval(true);
    dataset.setDacId(1);
    dataset.setDacApproval(false);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(datasetService.findDatasetById(anyInt())).thenReturn(dataset);
    when(datasetService.approveDataset(any(Dataset.class), any(User.class), anyBoolean()))
        .thenReturn(datasetResponse);
    when(elasticSearchService.indexDataset(any())).thenReturn(Response.ok().build());
    Response response = dacResource.approveDataset(authUser, 1, 1, "{approval: true}");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(GsonUtil.buildGson().toJson(datasetResponse), response.getEntity());
  }

  @Test
  void testApproveDataset_AlreadyApproved_NonTrueSubmission() {
    User user = new User();
    user.setChairpersonRoleWithDAC(1);
    Dataset dataset = new Dataset();
    dataset.setDacId(1);
    dataset.setDacApproval(true);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(datasetService.findDatasetById(anyInt())).thenReturn(dataset);
    when(datasetService.approveDataset(any(Dataset.class), any(User.class), anyBoolean()))
        .thenThrow(ForbiddenException.class);
    Response response = dacResource.approveDataset(authUser, 1, 1, "{approval: false}");
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
  }

  @Test
  void testApproveDataset_ElasticSearchNonSuccessResponseDoesNotFail() throws Exception {
    User user = new User();
    user.setChairpersonRoleWithDAC(1);
    Dataset dataset = new Dataset();
    Dataset datasetResponse = new Dataset();
    datasetResponse.setDacId(1);
    datasetResponse.setDacApproval(true);
    dataset.setDacId(1);
    dataset.setDacApproval(false);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(datasetService.findDatasetById(anyInt())).thenReturn(dataset);
    when(datasetService.approveDataset(any(Dataset.class), any(User.class), anyBoolean()))
        .thenReturn(datasetResponse);
    when(elasticSearchService.indexDataset(any())).thenReturn(Response.serverError().build());
    Response response = dacResource.approveDataset(authUser, 1, 1, "{approval: true}");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(GsonUtil.buildGson().toJson(datasetResponse), response.getEntity());
  }

  @Test
  void testApproveDataset_ElasticSearchExceptionDoesNotFail() throws Exception {
    User user = new User();
    user.setChairpersonRoleWithDAC(1);
    Dataset dataset = new Dataset();
    Dataset datasetResponse = new Dataset();
    datasetResponse.setDacId(1);
    datasetResponse.setDacApproval(true);
    dataset.setDacId(1);
    dataset.setDacApproval(false);
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(datasetService.findDatasetById(anyInt())).thenReturn(dataset);
    when(datasetService.approveDataset(any(Dataset.class), any(User.class), anyBoolean()))
        .thenReturn(datasetResponse);
    doThrow(new IOException("Something went wrong")).when(elasticSearchService).indexDataset(any());
    Response response = dacResource.approveDataset(authUser, 1, 1, "{approval: true}");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(GsonUtil.buildGson().toJson(datasetResponse), response.getEntity());
  }

  private JsonArray getListFromEntityString(String str) {
    return GsonUtil.buildGson().fromJson(str, JsonArray.class);
  }

  private Dac buildDac(User chair) {
    Dac dac = new DacBuilder()
        .setDacId(RandomUtils.nextInt())
        .setName("name")
        .setDescription("description")
        .build();
    if (Objects.nonNull(chair)) {
      dac.setChairpersons(Collections.singletonList(chair));
    }
    return dac;
  }

  private User buildAdmin(AuthUser authUser) {
    User user = buildUser();
    user.setUserId(RandomUtils.nextInt());
    user.setEmail(authUser.getEmail());
    user.setAdminRole();
    return user;
  }

  private User buildChair(AuthUser authUser) {
    User user = buildUser();
    user.setUserId(RandomUtils.nextInt());
    user.setEmail(authUser.getEmail());
    user.setChairpersonRole();
    return user;
  }

  private User buildUser() {
    User user = new User();
    user.setUserId(RandomUtils.nextInt());
    return user;
  }
}
