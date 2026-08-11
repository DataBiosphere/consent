package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import jakarta.ws.rs.BadRequestException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DacDatasetExternalizationRequest;
import org.broadinstitute.consent.http.models.DacDatasetExternalizationResponse;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.dao.DacServiceDAO;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DacServiceTest extends AbstractTestHelper {

  private DacService service;

  @Mock private Jdbi jdbi;

  @Mock private DacDAO dacDAO;

  @Mock private UserDAO userDAO;

  @Mock private DatasetDAO dataSetDAO;

  @Mock private ElectionDAO electionDAO;

  @Mock DataAccessRequestDAO dataAccessRequestDAO;

  @Mock private VoteService voteService;

  @Mock private ElasticSearchService elasticSearchService;

  @Mock private DacServiceDAO dacServiceDAO;

  @Mock private DACAutomationRuleDAO ruleDAO;

  @BeforeEach
  void setUp() {
    when(jdbi.onDemand(DacDAO.class)).thenReturn(dacDAO);
    when(jdbi.onDemand(UserDAO.class)).thenReturn(userDAO);
    when(jdbi.onDemand(DatasetDAO.class)).thenReturn(dataSetDAO);
    when(jdbi.onDemand(ElectionDAO.class)).thenReturn(electionDAO);
    when(jdbi.onDemand(DataAccessRequestDAO.class)).thenReturn(dataAccessRequestDAO);
    when(jdbi.onDemand(DACAutomationRuleDAO.class)).thenReturn(ruleDAO);
    service = new DacService(jdbi, dacServiceDAO, voteService, elasticSearchService);
  }

  @Test
  void testFindAll() {
    when(dacDAO.findAll()).thenReturn(Collections.emptyList());

    assertTrue(service.findAll().isEmpty());
  }

  @Test
  void testFindAllWithDaas() {
    Dac dac1 = new Dac();
    int dac1Id = randomInt(3, 50);
    dac1.setName("dac1");
    dac1.setDacId(dac1Id);

    Dac dac2 = new Dac();
    int dac2Id = randomInt(3, 50);
    dac2.setName("dac2");
    dac2.setDacId(dac2Id);

    DataAccessAgreement daa1 = new DataAccessAgreement();
    daa1.setDaaId(1);
    daa1.setInitialDacId(dac1Id);
    DataAccessAgreement daa2 = new DataAccessAgreement();
    daa2.setDaaId(2);
    daa2.setInitialDacId(dac2Id);

    dac1.setAssociatedDaa(daa1);
    dac2.setAssociatedDaa(daa2);

    when(dacDAO.findAll()).thenReturn(List.of(dac1, dac2));

    List<Dac> foundDacs = service.findAll();
    assertEquals(2, foundDacs.size());
    assertEquals(daa1, foundDacs.get(0).getAssociatedDaa());
    assertEquals(daa2, foundDacs.get(1).getAssociatedDaa());
  }

  @Test
  void testFindById() {
    int dacId = 1;
    when(dacDAO.findById(dacId)).thenReturn(getDacs().getFirst());
    when(dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.CHAIRPERSON.getRoleId()))
        .thenReturn(Collections.singletonList(getDacUsers().get(0)));
    when(dacDAO.findMembersByDacIdAndRoleId(dacId, UserRoles.MEMBER.getRoleId()))
        .thenReturn(Collections.singletonList(getDacUsers().get(1)));

    Dac dac = service.findById(dacId);
    assertNotNull(dac);
    assertFalse(dac.getChairpersons().isEmpty());
    assertFalse(dac.getMembers().isEmpty());
    assertNotNull(dac.getAssociatedDaa());
  }

  @Test
  void testCreateDac() {
    when(dacDAO.createDac(anyString(), anyString(), any()))
        .thenReturn(getDacs().getFirst().getDacId());

    Integer dacId = service.createDac("name", "description", 1);
    assertEquals(getDacs().getFirst().getDacId(), dacId);
  }

  @Test
  void testCreateDacWithEmail() {
    when(dacDAO.createDac(anyString(), anyString(), anyString(), any()))
        .thenReturn(getDacs().getFirst().getDacId());

    Integer dacId = service.createDac("name", "description", "email@test.com", 1);
    assertEquals(getDacs().getFirst().getDacId(), dacId);
  }

  @Test
  void testUpdateDac() {
    doNothing().when(dacDAO).updateDac(anyString(), anyString(), any(), any());

    try {
      service.updateDac("name", "description", 1, 1);
    } catch (Exception _) {
      fail("Update should not fail");
    }
  }

  @Test
  void testUpdateDacWithEmail() {
    doNothing().when(dacDAO).updateDac(anyString(), anyString(), anyString(), any(), any());

    try {
      service.updateDac("name", "description", "test@email.com", 1, 1);
    } catch (Exception _) {
      fail("Update should not fail");
    }
  }

  @Test
  void testDeleteDacServiceDAOException() {
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(randomInt(1, 10));
    Dac dac = new Dac();
    dac.setDacId(randomInt(100, 1000));
    dac.setDescription("DAC description");
    dac.setName("DAC name");
    dac.setAssociatedDaa(daa);
    when(dacDAO.findById(any())).thenReturn(dac);
    doThrow(new IllegalArgumentException())
        .when(dacServiceDAO)
        .deleteDacAndRemoveDaaAssociation(any(), any());

    assertThrows(IllegalArgumentException.class, () -> service.deleteDac(any(), dac.getDacId()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Broad DAC", "Dac 2", "Dac 3", "Dac 4", "Dac 5"})
  void testDeleteDac(String dacName) {
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(randomInt(1, 10));
    Dac dac = new Dac();
    dac.setDacId(randomInt(100, 1000));
    dac.setDescription(dacName + " description");
    dac.setName(dacName);
    dac.setAssociatedDaa(daa);
    when(dacDAO.findById(any())).thenReturn(dac);

    if (dac.getName().toLowerCase().contains("broad")) {
      assertThrows(IllegalArgumentException.class, () -> service.deleteDac(any(), dac.getDacId()));
    } else {
      assertDoesNotThrow(() -> service.deleteDac(any(), dac.getDacId()));
    }
  }

  @Test
  void testFindDatasetsByDacId() {

    List<Dataset> datasets = getDatasets();
    when(dataSetDAO.findDatasetsAssociatedWithDac(1)).thenReturn(datasets);

    List<Dataset> returned = service.findDatasetsByDacId(1);
    assertNotNull(returned);
    assertEquals(datasets, returned);
  }

  @Test
  void testFindMembersByDacId() {
    when(dacDAO.findMembersByDacId(anyInt()))
        .thenReturn(Collections.singletonList(getDacUsers().getFirst()));
    when(dacDAO.findUserRolesForUsers(any())).thenReturn(getDacUsers().getFirst().getRoles());

    List<User> users = service.findMembersByDacId(1);
    assertNotNull(users);
    assertFalse(users.isEmpty());
  }

  @Test
  void testAddDacMember() {
    Gson gson = new Gson();
    User user = getDacUsers().getFirst();
    Dac dac = getDacs().getFirst();
    when(userDAO.findUserById(any())).thenReturn(user);
    when(userDAO.findUserById(any())).thenReturn(user);
    List<Election> elections =
        getElections().stream()
            .map(
                e -> {
                  Election newE = gson.fromJson(gson.toJson(e), Election.class);
                  newE.setElectionType(ElectionType.DATA_ACCESS.getValue());
                  newE.setReferenceId(UUID.randomUUID().toString());
                  return newE;
                })
            .toList();
    DataAccessRequest dar = new DataAccessRequest();
    dar.setData(new DataAccessRequestData());
    when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
    when(electionDAO.findOpenElectionsByDacId(any())).thenReturn(elections);
    doNothing().when(dacDAO).addDacMember(anyInt(), anyInt(), anyInt(), anyInt());

    Role role = new Role(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    User user1 = service.addDacMember(role, user, dac, 1);
    assertNotNull(user1);
    assertFalse(user1.getRoles().isEmpty());
    verify(voteService, times(elections.size()))
        .createVotesForUser(any(), any(), any(), anyBoolean());
  }

  @Test
  void testRemoveDacMember() {
    Role role = new Role(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName());
    Dac dac = getDacs().getFirst();
    User member = getDacUsers().get(1);
    dac.setChairpersons(Collections.singletonList(getDacUsers().get(0)));
    dac.setMembers(Collections.singletonList(member));
    doNothing().when(dacDAO).removeDacMember(anyInt(), anyInt());
    doNothing().when(voteService).deleteOpenDacVotesForUser(any(), any());

    try {
      service.removeDacMember(role, member, dac, 1);
    } catch (Exception _) {
      fail();
    }
    verify(dacDAO, atLeastOnce()).removeDacMember(anyInt(), anyInt());
    verify(voteService, atLeastOnce()).deleteOpenDacVotesForUser(any(), any());
  }

  @Test
  void testRemoveDacChair() {
    Role role = new Role(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    Dac dac = getDacs().getFirst();
    User chair1 = getDacUsers().getFirst();
    User chair2 = getDacUsers().get(0);
    dac.setChairpersons(Arrays.asList(chair1, chair2));
    dac.setMembers(Collections.singletonList(getDacUsers().get(1)));
    doNothing().when(dacDAO).removeDacMember(anyInt(), anyInt());
    doNothing().when(voteService).deleteOpenDacVotesForUser(any(), any());
    when(ruleDAO.auditedDeleteDACRuleSettingByUser(anyInt(), anyInt(), anyInt())).thenReturn(1);

    try {
      service.removeDacMember(role, chair1, dac, 1);
    } catch (Exception _) {
      fail();
    }
    verify(dacDAO, atLeastOnce()).removeDacMember(anyInt(), anyInt());
    verify(voteService, atLeastOnce()).deleteOpenDacVotesForUser(any(), any());
    verify(ruleDAO, atLeastOnce()).auditedDeleteDACRuleSettingByUser(anyInt(), anyInt(), anyInt());
  }

  @Test
  void testRemoveDacChairFailure() {
    Role role = new Role(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    Dac dac = getDacs().getFirst();
    User chair = getDacUsers().get(0);
    dac.setChairpersons(Collections.singletonList(chair));
    dac.setMembers(Collections.singletonList(getDacUsers().get(1)));

    assertThrows(
        BadRequestException.class,
        () -> {
          service.removeDacMember(role, chair, dac, 1);
          verify(dacDAO, times(0)).removeDacMember(anyInt(), anyInt());
          verify(voteService, times(0)).deleteOpenDacVotesForUser(any(), any());
          verify(ruleDAO, times(0)).auditedDeleteDACRuleSettingByUser(anyInt(), anyInt(), anyInt());
        });
  }

  @Test
  void testFilterDataAccessRequestsByDAC_adminCase() {
    User user = new User();
    user.setRoles(new ArrayList<>());
    user.getRoles().add(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));

    // User is an admin user

    List<DataAccessRequest> dars = getDataAccessRequests();

    List<DataAccessRequest> filtered = service.filterDataAccessRequestsByDac(dars, user);
    // As an admin, all docs should be returned.
    assertEquals(dars.size(), filtered.size());
  }

  @Test
  void testFilterDataAccessRequestsByDAC_memberCase_1() {
    // Member has access to DataSet 1
    List<Dataset> memberDataSets = Collections.singletonList(getDatasets().getFirst());
    when(dataSetDAO.findDatasetIdsByDACUserId(getMember().getUserId()))
        .thenReturn(List.of(memberDataSets.getFirst().getDatasetId()));

    List<DataAccessRequest> dars = getDataAccessRequests();

    List<DataAccessRequest> filtered = service.filterDataAccessRequestsByDac(dars, getMember());

    // Filtered documents should only contain the ones the user has direct access to:
    assertEquals(memberDataSets.size(), filtered.size());
  }

  @Test
  void testFilterDataAccessRequestsByDAC_memberCase_2() {
    // Member has access to datasets
    List<Dataset> memberDataSets = Collections.singletonList(getDatasets().getFirst());
    when(dataSetDAO.findDatasetIdsByDACUserId(getMember().getUserId()))
        .thenReturn(List.of(memberDataSets.getFirst().getDatasetId()));

    List<DataAccessRequest> dars = getDataAccessRequests();

    List<DataAccessRequest> filtered = service.filterDataAccessRequestsByDac(dars, getMember());

    // Filtered documents should only contain the ones the user has direct access to
    assertEquals(memberDataSets.size(), filtered.size());
  }

  @Test
  void testFilterDataAccessRequestsByDAC_memberCase_3() {
    // Member no direct access to datasets
    when(dataSetDAO.findDatasetIdsByDACUserId(getMember().getUserId())).thenReturn(List.of());

    List<DataAccessRequest> dars = getDataAccessRequests();

    List<DataAccessRequest> filtered = service.filterDataAccessRequestsByDac(dars, getMember());

    // Filtered documents should contain the ones the user has direct access to
    assertEquals(0, filtered.size());
  }

  @Test
  void testFindDacsByUserAdminCase() {
    List<Dac> dacs = getDacs();
    when(dacDAO.findAll()).thenReturn(dacs);

    List<Dac> dacsForUser = service.findDacsWithMembersOption(false);
    assertEquals(dacsForUser.size(), dacs.size());
  }

  @Test
  void testFindDacsByUserChairCase() {
    List<Dac> dacs = getDacs();
    when(dacDAO.findAll()).thenReturn(dacs);

    List<Dac> dacsForUser = service.findDacsWithMembersOption(false);
    assertEquals(dacsForUser.size(), dacs.size());
  }

  @Test
  void testConvertDacDatasetsToExternal() throws Exception {
    Dac dac = new Dac();
    dac.setDacId(1);
    when(dacDAO.findById(1)).thenReturn(dac);
    when(dacDAO.findMembersByDacIdAndRoleId(anyInt(), anyInt())).thenReturn(List.of());
    DacDatasetExternalizationRequest request =
        new DacDatasetExternalizationRequest("policy update", false, true, true, null);
    DacDatasetExternalizationResponse expected =
        new DacDatasetExternalizationResponse(
            1, false, "policy update", Instant.now(), Instant.now(), 2, 1, 1, 3, 0, 2);
    when(dacServiceDAO.findConvertibleDatasetIds(anyInt(), any())).thenReturn(List.of(101, 102));
    when(dacServiceDAO.convertDacDatasetsToExternal(anyInt(), anyInt(), any()))
        .thenReturn(expected);

    DacDatasetExternalizationResponse actual = service.convertDacDatasetsToExternal(1, 10, request);
    assertEquals(expected, actual);
    verify(elasticSearchService).indexDatasets(List.of(101, 102));
  }

  @Test
  void testConvertDacDatasetsToExternalDryRunSkipsReindex() throws Exception {
    Dac dac = new Dac();
    dac.setDacId(1);
    when(dacDAO.findById(1)).thenReturn(dac);
    when(dacDAO.findMembersByDacIdAndRoleId(anyInt(), anyInt())).thenReturn(List.of());
    DacDatasetExternalizationRequest request =
        new DacDatasetExternalizationRequest("policy update", true, true, true, null);
    when(dacServiceDAO.findConvertibleDatasetIds(anyInt(), any())).thenReturn(List.of(101, 102));
    when(dacServiceDAO.convertDacDatasetsToExternal(anyInt(), anyInt(), any()))
        .thenReturn(
            new DacDatasetExternalizationResponse(
                1, true, "policy update", Instant.now(), Instant.now(), 2, 1, 1, 3, 0, 2));

    service.convertDacDatasetsToExternal(1, 10, request);

    verify(elasticSearchService, never()).indexDatasets(any());
  }

  @Test
  void testConvertDacDatasetsToExternalRequiresRevocation() {

    DacDatasetExternalizationRequest request =
        new DacDatasetExternalizationRequest("policy update", false, false, true, null);
    assertThrows(
        IllegalArgumentException.class, () -> service.convertDacDatasetsToExternal(1, 10, request));
  }

  /* Helper functions */

  /**
   * @return A list of 5 elections with DataSet ids
   */
  private List<Election> getElections() {
    return IntStream.range(1, 5)
        .mapToObj(
            i -> {
              Election election = new Election();
              election.setDatasetId(i);
              return election;
            })
        .toList();
  }

  /**
   * @return A list of 5 DataAccessRequest with DataSet ids and Reference ids
   */
  private List<DataAccessRequest> getDataAccessRequests() {
    return IntStream.range(1, 5)
        .mapToObj(
            i -> {
              String referenceId = UUID.randomUUID().toString();
              List<Integer> datasetIds = Collections.singletonList(i);
              DataAccessRequest dar = new DataAccessRequest();
              dar.setReferenceId(referenceId);
              DataAccessRequestData data = new DataAccessRequestData();
              dar.setDatasetIds(datasetIds);
              data.setReferenceId(referenceId);
              dar.setData(data);
              return dar;
            })
        .toList();
  }

  /**
   * @return A list of 5 datasets with ids
   */
  private List<Dataset> getDatasets() {
    return IntStream.range(1, 5)
        .mapToObj(
            i -> {
              Dataset dataSet = new Dataset();
              dataSet.setDatasetId(i);
              return dataSet;
            })
        .toList();
  }

  /**
   * @return A list of 5 dacs
   */
  private List<Dac> getDacs() {
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    return IntStream.range(1, 5)
        .mapToObj(
            i -> {
              Dac dac = new Dac();
              dac.setDacId(i);
              dac.setDescription("Dac " + i);
              dac.setName("Dac " + i);
              dac.setAssociatedDaa(daa);
              return dac;
            })
        .toList();
  }

  /**
   * @return A list of two users in a single DAC
   */
  private List<User> getDacUsers() {
    return Arrays.asList(getChair(), getMember());
  }

  private User getChair() {
    User chair = new User();
    chair.setUserId(1);
    chair.setDisplayName("Chair");
    chair.setEmail("chair@duos.org");
    chair.setRoles(new ArrayList<>());
    chair
        .getRoles()
        .add(
            new UserRole(
                1,
                chair.getUserId(),
                UserRoles.CHAIRPERSON.getRoleId(),
                UserRoles.CHAIRPERSON.getRoleName(),
                1));
    return chair;
  }

  private User getMember() {
    User member = new User();
    member.setUserId(2);
    member.setDisplayName("Member");
    member.setEmail("member@duos.org");
    member.setRoles(new ArrayList<>());
    member
        .getRoles()
        .add(
            new UserRole(
                2,
                member.getUserId(),
                UserRoles.MEMBER.getRoleId(),
                UserRoles.MEMBER.getRoleName(),
                1));
    return member;
  }
}
