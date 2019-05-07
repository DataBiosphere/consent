package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.DACUserRoleDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsentElectionTest extends ElectionVoteServiceTest {

    public static final int CREATED = Response.Status.CREATED
            .getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    public static final int BADREQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    public static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    private static final String CONSENT_ID = "testId";
    private static final String CONSENT_ID_2 = "testId2";
    private static final String INVALID_CONSENT_ID = "invalidId";
    private static final String INVALID_STATUS = "testStatus";
    private static final String FINAL_RATIONALE = "Test";
    private DACUserRoleDAO userRoleDAO = getApplicationJdbi().onDemand(DACUserRoleDAO.class);
    private static final int member = 1;
    private static final int chair = 2;

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @After
    public void resetDACMemberRoles() {
//        insert into user_role(roleId, dacUserId) values(2,1);
//        insert into user_role(roleId, dacUserId, status) values(5,1,0);
//        insert into user_role(roleId, dacUserId) values(6,1);

//        insert into user_role(roleId, dacUserId) values(1,2);
//        insert into user_role(roleId, dacUserId) values(6,2);

//        insert into user_role(roleId, dacUserId) values(1,3);

//        insert into user_role(roleId, dacUserId) values(1,4);
//        insert into user_role(roleId, dacUserId) values(4,4);

//        insert into user_role(roleId, dacUserId) values(2,5);
//        insert into user_role(roleId, dacUserId) values(4,5);
//        insert into user_role(roleId, dacUserId) values(5,5);
//        insert into user_role(roleId, dacUserId) values(6,5);

        // to avoid integrity constraint violations on insert, delete them all first.
//        List<Integer> roleList = Arrays.asList(member, chair);
//        userRoleDAO.removeUserRoles(1, roleList);
//        userRoleDAO.removeUserRoles(2, roleList);
//        userRoleDAO.removeUserRoles(3, roleList);
//        userRoleDAO.removeUserRoles(4, roleList);
//
//        // now reset to original seed data.
//        userRoleDAO.insertSingleUserRole(chair, 1, false);
//        userRoleDAO.insertSingleUserRole(member, 1, false);
//        userRoleDAO.insertSingleUserRole(member, 2, false);
//        userRoleDAO.insertSingleUserRole(member, 3, false);
//        userRoleDAO.insertSingleUserRole(member, 4, false);
    }

    @Test
    public void testCreateConsentElection() throws IOException {
        Client client = ClientBuilder.newClient();
        Election created = createElection(CONSENT_ID);
        assertThat(created.getElectionType()).isEqualTo(
                ElectionType.TRANSLATE_DUL.getValue());
        assertThat(created.getStatus()).isEqualTo(ElectionStatus.OPEN.getValue());
        assertThat(created.getReferenceId()).isEqualTo(CONSENT_ID);
        assertThat(created.getCreateDate()).isNotNull();
        assertThat(created.getElectionId()).isNotNull();
        assertThat(created.getFinalRationale()).isNull();
        // try to create other election for the same consent
        checkStatus(BADREQUEST,
                post(client, electionConsentPath(CONSENT_ID), created));
        testUpdateConsentElection(created);
        deleteElection(created.getElectionId(), CONSENT_ID);
    }

    public void testUpdateConsentElection(Election created) throws IOException {
        Client client = ClientBuilder.newClient();
        created.setFinalVote(true);
        created.setFinalRationale(FINAL_RATIONALE);
        checkStatus(OK, put(client, electionPathById(created.getElectionId()), created));
        created = retrieveElection(client, electionPathById(created.getElectionId()));
        assertThat(created.getElectionType()).isEqualTo(
                ElectionType.TRANSLATE_DUL.getValue());
        assertThat(created.getReferenceId()).isEqualTo(CONSENT_ID);
        assertThat(created.getCreateDate()).isNotNull();
        assertThat(created.getElectionId()).isNotNull();
    }

    public void deleteElection(Integer electionId, String consentId) throws IOException {
        Client client = ClientBuilder.newClient();
        List<Vote> votes = getJson(client, voteConsentPath(consentId)).readEntity(new GenericType<List<Vote>>() {
        });
        for (Vote vote : votes) {
            checkStatus(OK,
                    delete(client, voteConsentIdPath(consentId, vote.getVoteId())));
        }
        checkStatus(OK,
                delete(client, electionConsentPathById(consentId, electionId)));

    }

    @Test
    public void retrieveElectionWithInvalidConsentId() throws IOException {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND,
                getJson(client, electionConsentPath(INVALID_CONSENT_ID)));
    }

    @Test
    public void testCreateConsentElectionWithInvalidConsent() throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        election.setStatus(ElectionStatus.OPEN.getValue());
        // should return 400 bad request because the consent id does not exist
        checkStatus(BADREQUEST,
                post(client, electionConsentPath(INVALID_CONSENT_ID), election));
    }

    @Test
    public void testUpdateConsentElectionWithInvalidId() throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        // should return 400 bad request because the election id does not exist
        checkStatus(NOT_FOUND,
                put(client, electionPathById(8555), election));
    }

    @Test
    public void testCreateConsentElectionWithInvalidStatus() throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        election.setStatus(INVALID_STATUS);
        // should return 400 bad request because status is invalid
        checkStatus(BAD_REQUEST,
                post(client, electionConsentPath(CONSENT_ID_2), election));
    }

//    @Test
//    public void createElectionWithSingleChairperson() throws IOException {
//        // Test Seed Data creates 3 DAC members and 1 chairperson.
//        // We need to remove the DAC member roles for this test
//        List<Integer> roleList = Collections.singletonList(member);
//        userRoleDAO.removeUserRoles(2, roleList);
//        userRoleDAO.removeUserRoles(3, roleList);
//        userRoleDAO.removeUserRoles(4, roleList);
//
//        Client client = ClientBuilder.newClient();
//        Election election = new Election();
//        election.setStatus(ElectionStatus.OPEN.getValue());
//        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
//        Response response = checkStatus(CREATED,
//                post(client, electionConsentPath(CONSENT_ID), election));
//        String createdLocation = checkHeader(response, "Location");
//        assertThat(createdLocation).isNotNull();
//        assertThat(createdLocation).isNotEmpty();
//    }
//
//    @Test
//    public void createElectionWithNoDACMembersOrChair() throws IOException {
//        // Test Seed Data creates 3 DAC members and 1 chairperson.
//        // We need to remove all roles for this test
//        List<Integer> roleList = Arrays.asList(member, chair);
//        userRoleDAO.removeUserRoles(1, roleList);
//        userRoleDAO.removeUserRoles(2, roleList);
//        userRoleDAO.removeUserRoles(3, roleList);
//        userRoleDAO.removeUserRoles(4, roleList);
//
//        Client client = ClientBuilder.newClient();
//        Election election = new Election();
//        election.setStatus(ElectionStatus.OPEN.getValue());
//        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
//        checkStatus(BADREQUEST, post(client, electionConsentPath(CONSENT_ID), election));
//    }
//
//    @Test
//    public void createElectionWithNoChair() throws IOException {
//        // Test Seed Data creates 3 DAC members and 1 chairperson.
//        // We need to remove the chairperson role for this test
//        List<Integer> roleList = Collections.singletonList(chair);
//        userRoleDAO.removeUserRoles(1, roleList);
//
//        Client client = ClientBuilder.newClient();
//        Election election = new Election();
//        election.setStatus(ElectionStatus.OPEN.getValue());
//        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
//        checkStatus(BADREQUEST, post(client, electionConsentPath(CONSENT_ID), election));
//    }

    public Election createElection(String consentId) throws IOException {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        Response response = checkStatus(CREATED,
                post(client, electionConsentPath(consentId), election));
        String createdLocation = checkHeader(response, "Location");
        return retrieveElection(client, createdLocation);
    }

}
