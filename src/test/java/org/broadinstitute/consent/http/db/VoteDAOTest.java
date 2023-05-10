package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class VoteDAOTest extends DAOTestHelper {

    @Test
    public void testFindVotesByReferenceId() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getUserId(), election.getElectionId());

        List<Vote> votes = voteDAO.findVotesByReferenceId(election.getReferenceId());
        Assert.assertFalse(votes.isEmpty());
        Assert.assertEquals(vote.getVoteId(), votes.get(0).getVoteId());
    }

    @Test
    public void testFindVoteById() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getUserId(), election.getElectionId());

        Vote foundVote = voteDAO.findVoteById(vote.getVoteId());
        assertNotNull(foundVote);
    }

    @Test
    public void testFindVotesByIds() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        User user2 = createUserWithRole(UserRoles.MEMBER.getRoleId());
        User user3 = createUserWithRole(UserRoles.MEMBER.getRoleId());
        User user4 = createUserWithRole(UserRoles.MEMBER.getRoleId());
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getUserId(), election.getElectionId());
        Vote vote2 = createDacVote(user2.getUserId(), election.getElectionId());
        Vote vote3 = createDacVote(user3.getUserId(), election.getElectionId());
        Vote vote4 = createDacVote(user4.getUserId(), election.getElectionId());
        List<Integer> voteIds = List.of(vote.getVoteId(), vote2.getVoteId(), vote3.getVoteId(), vote4.getVoteId());

        List<Vote> foundVotes = voteDAO.findVotesByIds(voteIds);
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertTrue(foundVotes.stream().map(Vote::getVoteId).toList().containsAll(voteIds));
    }

    @Test
    public void testFindVotesByElectionIds() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        createDacVote(user.getUserId(), election.getElectionId());

        Consent consent2 = createConsent();
        Dataset dataset2 = createDataset();
        Election election2 = createDataAccessElection(consent2.getConsentId(), dataset2.getDataSetId());
        createDacVote(user.getUserId(), election2.getElectionId());
        List<Integer> electionIds = Arrays.asList(election.getElectionId(), election2.getElectionId());

        List<Vote> foundVotes = voteDAO.findVotesByElectionIds(electionIds);
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(2, foundVotes.size());
    }

    @Test
    public void testFindVotesByElectionIdAndType() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getUserId(), election.getElectionId());

        List<Vote> foundVotes = voteDAO.findVotesByElectionIdAndType(election.getElectionId(), vote.getType());
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(1, foundVotes.size());

        List<Vote> foundVotes2 = voteDAO.findVotesByElectionIdAndType(election.getElectionId(), vote.getType().toLowerCase());
        assertNotNull(foundVotes2);
        assertFalse(foundVotes2.isEmpty());
        assertEquals(1, foundVotes2.size());

        List<Vote> foundVotes3 = voteDAO.findVotesByElectionIdAndType(election.getElectionId(), vote.getType().toUpperCase());
        assertNotNull(foundVotes3);
        assertFalse(foundVotes3.isEmpty());
        assertEquals(1, foundVotes3.size());
    }

    @Test
    public void testFindPendingVotesByElectionId() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getUserId(), election.getElectionId());

        List<Vote> foundVotes = voteDAO.findPendingVotesByElectionId(election.getElectionId());
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(1, foundVotes.size());
        assertEquals(vote.getVoteId(), foundVotes.get(0).getVoteId());
    }

    @Test
    public void testFindVoteByElectionIdAndDACUserId() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getUserId(), election.getElectionId());

        Vote foundVote = voteDAO.findVoteByElectionIdAndUserId(election.getElectionId(), user.getUserId());
        assertNotNull(foundVote);
        assertEquals(vote.getVoteId(), foundVote.getVoteId());
    }

    @Test
    public void testFindVotesByElectionIdAndDACUserIds() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getUserId(), election.getElectionId());

        List<Vote> foundVotes = voteDAO.findVotesByElectionIdAndUserIds(election.getElectionId(), Collections.singletonList(user.getUserId()));
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(vote.getVoteId(), foundVotes.get(0).getVoteId());
    }

    @Test
    public void testCheckVoteById() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getUserId(), election.getElectionId());

        Integer voteId = voteDAO.checkVoteById(election.getReferenceId(), vote.getVoteId());
        assertNotNull(voteId);
        assertEquals(vote.getVoteId(), voteId);
    }

    @Test
    public void testInsertVote() {
        // no-op ... tested by `createDacVote` and `createFinalVote`
    }

    @Test
    public void testDeleteVoteByReferenceId() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dataset dataset = createDataset();
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();
        Election election = createDataAccessElection(referenceId, datasetId);
        Vote vote1 = createDacVote(user.getUserId(), election.getElectionId());
        Vote vote2 = createDacVote(user.getUserId(), election.getElectionId());
        Vote vote3 = createDacVote(user.getUserId(), election.getElectionId());

        List<Vote> foundVotes = voteDAO.findVotesByReferenceId(referenceId);
        assertEquals(3, foundVotes.size());
        voteDAO.deleteVotesByReferenceId(referenceId);
        foundVotes = voteDAO.findVotesByReferenceId(referenceId);
        assertEquals(0, foundVotes.size());
    }

    @Test
    public void testDeleteVoteByReferenceIds() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dataset dataset = createDataset();
        Integer datasetId = dataset.getDataSetId();
        DataAccessRequest dar1 = createDataAccessRequestV3();
        DataAccessRequest dar2 = createDataAccessRequestV3();

        String referenceId1 = dar1.getReferenceId();
        Election election1 = createDataAccessElection(referenceId1, datasetId);
        Vote vote1 = createDacVote(user.getUserId(), election1.getElectionId());
        Vote vote2 = createDacVote(user.getUserId(), election1.getElectionId());
        Vote vote3 = createDacVote(user.getUserId(), election1.getElectionId());

        String referenceId2 = dar2.getReferenceId();
        Election election2 = createDataAccessElection(referenceId2, datasetId);
        Vote vote4 = createDacVote(user.getUserId(), election2.getElectionId());
        Vote vote5 = createDacVote(user.getUserId(), election2.getElectionId());
        Vote vote6 = createDacVote(user.getUserId(), election2.getElectionId());

        List<Vote> foundVotes = voteDAO.findVotesByReferenceId(referenceId1);
        assertEquals(3, foundVotes.size());

        foundVotes = voteDAO.findVotesByReferenceId(referenceId2);
        assertEquals(3, foundVotes.size());

        voteDAO.deleteVotesByReferenceIds(List.of(referenceId1, referenceId2));

        foundVotes = voteDAO.findVotesByReferenceId(referenceId1);
        assertEquals(0, foundVotes.size());

        foundVotes = voteDAO.findVotesByReferenceId(referenceId2);
        assertEquals(0, foundVotes.size());

    }

    @Test
    public void testCreateVote() {
        User user = createUser();
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote v = createDacVote(user.getUserId(), election.getElectionId());

        Vote vote = voteDAO.findVoteById(v.getVoteId());
        assertNotNull(vote);
        Assert.assertNull(vote.getVote());
    }

    @Test
    public void testUpdateVote() {
        User user = createUser();
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote v = createDacVote(user.getUserId(), election.getElectionId());

        String rationale = "rationale";
        Date now = new Date();
        voteDAO.updateVote(true, rationale, now, v.getVoteId(), true,
                election.getElectionId(), now, true);
        Vote vote = voteDAO.findVoteById(v.getVoteId());
        Assert.assertTrue(vote.getVote());
        Assert.assertTrue(vote.getHasConcerns());
        Assert.assertTrue(vote.getIsReminderSent());
        Assert.assertEquals(vote.getRationale(), rationale);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Assert.assertEquals(sdf.format(vote.getCreateDate()), sdf.format(now));
        Assert.assertEquals(sdf.format(vote.getUpdateDate()), sdf.format(now));
    }

    @Test
    public void testDeleteVotes() {
        // No-op ... tested by `tearDown()`
    }

    @Test
    public void testUpdateVoteReminderFlag() {
        User user = createUser();
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote v = createDacVote(user.getUserId(), election.getElectionId());

        voteDAO.updateVoteReminderFlag(v.getVoteId(), true);
        Vote vote = voteDAO.findVoteById(v.getVoteId());
        assertTrue(vote.getIsReminderSent());

        voteDAO.updateVoteReminderFlag(v.getVoteId(), false);
        Vote vote2 = voteDAO.findVoteById(v.getVoteId());
        assertFalse(vote2.getIsReminderSent());
    }

    @Test
    public void testFindTotalFinalVoteByElectionTypeAndVote() {
        User user = createUser();
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        Consent consent = createConsent();
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        electionDAO.updateElectionById(
                election.getElectionId(),
                ElectionStatus.CLOSED.getValue(),
                new Date());
        Vote v = createFinalVote(user.getUserId(), election.getElectionId());
        boolean voteValue = true;
        voteDAO.updateVote(
                voteValue,
                RandomStringUtils.randomAlphabetic(10),
                new Date(),
                v.getVoteId(),
                false,
                election.getElectionId(),
                v.getCreateDate(),
                false
        );

        int count = voteDAO.findTotalFinalVoteByElectionTypeAndVote(election.getElectionType(), voteValue);
        assertEquals(1, count);

        int count2 = voteDAO.findTotalFinalVoteByElectionTypeAndVote(election.getElectionType().toLowerCase(), voteValue);
        assertEquals(1, count2);

        int count3 = voteDAO.findTotalFinalVoteByElectionTypeAndVote(election.getElectionType().toUpperCase(), voteValue);
        assertEquals(1, count3);
    }

    @Test
    public void testFindMaxNumberOfDACMembers() {
        User user = createUser();
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        electionDAO.updateElectionById(
                election.getElectionId(),
                ElectionStatus.CLOSED.getValue(),
                new Date());
        Vote v = createDacVote(user.getUserId(), election.getElectionId());
        boolean voteValue = true;
        voteDAO.updateVote(
                voteValue,
                RandomStringUtils.random(10),
                new Date(),
                v.getVoteId(),
                false,
                election.getElectionId(),
                v.getCreateDate(),
                false
        );

        int count = voteDAO.findMaxNumberOfDACMembers(Collections.singletonList(election.getElectionId()));
        assertEquals(1, count);
    }

    @Test
    public void testInsertVotes() {
        User user1 = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        User user2 = createUserWithRole(UserRoles.MEMBER.getRoleId());
        User user3 = createUserWithRole(UserRoles.MEMBER.getRoleId());
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        List<Integer> userIds = Arrays.asList(user1.getUserId(), user2.getUserId(), user3.getUserId());

        voteDAO.insertVotes(userIds, election.getElectionId(), VoteType.DAC.getValue());
        List<Vote> votes = voteDAO.findVotesByElectionIds(Collections.singletonList(election.getElectionId()));
        assertNotNull(votes);
        assertFalse(votes.isEmpty());
        assertEquals(3, votes.size());
    }

    @Test
    public void testFindDataOwnerPendingVotesByElectionId() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getUserId(), election.getElectionId());

        List<Vote> votes = voteDAO.findDataOwnerPendingVotesByElectionId(election.getElectionId(), vote.getType());
        assertNotNull(votes);
        assertFalse(votes.isEmpty());
        assertEquals(1, votes.size());
        assertEquals(vote.getVoteId(), votes.get(0).getVoteId());

        List<Vote> votes2 = voteDAO.findDataOwnerPendingVotesByElectionId(election.getElectionId(), vote.getType().toLowerCase());
        assertNotNull(votes2);
        assertFalse(votes2.isEmpty());
        assertEquals(1, votes2.size());
        assertEquals(vote.getVoteId(), votes2.get(0).getVoteId());

        List<Vote> votes3 = voteDAO.findDataOwnerPendingVotesByElectionId(election.getElectionId(), vote.getType().toUpperCase());
        assertNotNull(votes3);
        assertFalse(votes3.isEmpty());
        assertEquals(1, votes3.size());
        assertEquals(vote.getVoteId(), votes3.get(0).getVoteId());

    }

    @Test
    public void testRemoveVotesByIds() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getUserId(), election.getElectionId());

        voteDAO.removeVotesByIds(Collections.singletonList(vote.getVoteId()));
        Vote v = voteDAO.findVoteById(vote.getVoteId());
        assertNull(v);
    }

    @Test
    public void testFindVotesByUserId() {
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        createChairpersonVote(user.getUserId(), election.getElectionId());

        List<Vote> userVotes = voteDAO.findVotesByUserId(user.getUserId());
        assertNotNull(userVotes);
        assertFalse(userVotes.isEmpty());
    }

    @Test
    public void testUpdateRationaleByVoteIds() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        User user = createUserWithRoleInDac(UserRoles.MEMBER.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
        Vote dacVote = createDacVote(user.getUserId(), election.getElectionId());
        assertNull(dacVote.getRationale());

        String rationale = RandomStringUtils.random(10, true, false);
        voteDAO.updateRationaleByVoteIds(List.of(dacVote.getVoteId()), rationale);

        Vote updatedVote = voteDAO.findVoteById(dacVote.getVoteId());
        assertEquals(rationale, updatedVote.getRationale());
    }

    @Test
    public void testFindVoteUsersByElectionReferenceIdList_Empty() {
        // Empty case
        List<User> voteUsers = voteDAO.findVoteUsersByElectionReferenceIdList(List.of("invalid reference id"));
        assertTrue(voteUsers.isEmpty());
    }

    @Test
    public void testFindVoteUsersByElectionReferenceIdList() {
        // Populated case requires:
        // * DAC
        // * Dataset
        // * DarCollection
        // * DarCollection User
        //      helper method also creates elections and votes for user, so make that user a chairperson
        Dac dac = createDac();
        User chair = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        // This creates an election and votes for the user passed in as the creator
        DarCollection collection = createDarCollectionWithDatasetsAndConsentAssociation(chair, List.of(dataset));
        Optional<DataAccessRequest> dar = collection.getDars().values().stream().findFirst();
        assertTrue(dar.isPresent());

        List<User> voteUsers = voteDAO.findVoteUsersByElectionReferenceIdList(List.of(dar.get().getReferenceId()));
        assertFalse(voteUsers.isEmpty());
        assertEquals(1, voteUsers.size());
        assertEquals(chair.getUserId(), voteUsers.get(0).getUserId());
    }

}
