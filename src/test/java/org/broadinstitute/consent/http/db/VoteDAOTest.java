package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ElectionReviewVote;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VoteDAOTest extends DAOTestHelper {

    @Test
    public void testFindVotesByReferenceId() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<Vote> votes = voteDAO.findVotesByReferenceId(election.getReferenceId());
        Assert.assertFalse(votes.isEmpty());
        Assert.assertEquals(vote.getVoteId(), votes.get(0).getVoteId());
    }

    @Test
    public void testFindElectionReviewVotesByElectionId() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        createDacVote(user.getDacUserId(), election.getElectionId());

        List<ElectionReviewVote> votes = voteDAO.findElectionReviewVotesByElectionId(election.getElectionId());
        Assert.assertFalse(votes.isEmpty());
        Assert.assertEquals(user.getEmail(), votes.get(0).getEmail());
    }

    @Test
    public void testFindElectionReviewVotesByElectionIdAndType() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<ElectionReviewVote> votes = voteDAO.findElectionReviewVotesByElectionId(election.getElectionId(), vote.getType());
        Assert.assertFalse(votes.isEmpty());
        Assert.assertEquals(user.getEmail(), votes.get(0).getEmail());

        List<ElectionReviewVote> votes2 = voteDAO.findElectionReviewVotesByElectionId(election.getElectionId(), vote.getType().toUpperCase());
        Assert.assertFalse(votes2.isEmpty());
        Assert.assertEquals(user.getEmail(), votes2.get(0).getEmail());

        List<ElectionReviewVote> votes3 = voteDAO.findElectionReviewVotesByElectionId(election.getElectionId(), vote.getType().toLowerCase());
        Assert.assertFalse(votes3.isEmpty());
        Assert.assertEquals(user.getEmail(), votes3.get(0).getEmail());
    }

    @Test
    public void testFindVoteById() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        Vote foundVote = voteDAO.findVoteById(vote.getVoteId());
        assertNotNull(foundVote);
    }

    @Test
    public void testFindDACVotesByElectionId() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<Vote> foundVotes = voteDAO.findDACVotesByElectionId(election.getElectionId());
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(1, foundVotes.size());
        assertEquals(vote.getVoteId(), foundVotes.get(0).getVoteId());
    }

    @Test
    public void testFindVotesByElectionIds() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        createDacVote(user.getDacUserId(), election.getElectionId());

        Consent consent2 = createConsent(null);
        DataSet dataset2 = createDataset();
        Election election2 = createElection(consent2.getConsentId(), dataset2.getDataSetId());
        createDacVote(user.getDacUserId(), election2.getElectionId());
        List<Integer> electionIds = Arrays.asList(election.getElectionId(), election2.getElectionId());

        List<Vote> foundVotes = voteDAO.findVotesByElectionIds(electionIds);
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(2, foundVotes.size());
    }

    @Test
    public void testFindVotesByTypeAndElectionIds() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        Consent consent2 = createConsent(null);
        DataSet dataset2 = createDataset();
        Election election2 = createElection(consent2.getConsentId(), dataset2.getDataSetId());
        createDacVote(user.getDacUserId(), election2.getElectionId());
        List<Integer> electionIds = Arrays.asList(election.getElectionId(), election2.getElectionId());

        List<Vote> foundVotes = voteDAO.findVotesByTypeAndElectionIds(electionIds, vote.getType());
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(2, foundVotes.size());

        List<Vote> foundVotes2 = voteDAO.findVotesByTypeAndElectionIds(electionIds, vote.getType().toLowerCase());
        assertNotNull(foundVotes2);
        assertFalse(foundVotes2.isEmpty());
        assertEquals(2, foundVotes2.size());

        List<Vote> foundVotes3 = voteDAO.findVotesByTypeAndElectionIds(electionIds, vote.getType().toUpperCase());
        assertNotNull(foundVotes3);
        assertFalse(foundVotes3.isEmpty());
        assertEquals(2, foundVotes3.size());
    }

    @Test
    public void testFindVotesByElectionIdAndType() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

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
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<Vote> foundVotes = voteDAO.findPendingVotesByElectionId(election.getElectionId());
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(1, foundVotes.size());
        assertEquals(vote.getVoteId(), foundVotes.get(0).getVoteId());
    }

    @Test
    public void testFindVoteByElectionIdAndDACUserId() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        Vote foundVote = voteDAO.findVoteByElectionIdAndDACUserId(election.getElectionId(), user.getDacUserId());
        assertNotNull(foundVote);
        assertEquals(vote.getVoteId(), foundVote.getVoteId());
    }

    @Test
    public void testFindVotesByElectionIdAndDACUserIds() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<Vote> foundVotes = voteDAO.findVotesByElectionIdAndDACUserIds(election.getElectionId(), Collections.singletonList(user.getDacUserId()));
        assertNotNull(foundVotes);
        assertFalse(foundVotes.isEmpty());
        assertEquals(vote.getVoteId(), foundVotes.get(0).getVoteId());
    }

    @Test
    public void testFindVoteByElectionIdAndType() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        Vote foundVote = voteDAO.findVoteByElectionIdAndType(election.getElectionId(), vote.getType());
        assertNotNull(foundVote);
        assertEquals(vote.getVoteId(), foundVote.getVoteId());

        Vote foundVote2 = voteDAO.findVoteByElectionIdAndType(election.getElectionId(), vote.getType().toLowerCase());
        assertNotNull(foundVote2);
        assertEquals(vote.getVoteId(), foundVote2.getVoteId());

        Vote foundVote3 = voteDAO.findVoteByElectionIdAndType(election.getElectionId(), vote.getType().toUpperCase());
        assertNotNull(foundVote3);
        assertEquals(vote.getVoteId(), foundVote3.getVoteId());
    }

    @Test
    public void testFindChairPersonVoteByElectionIdAndDACUserId() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createFinalVote(user.getDacUserId(), election.getElectionId());

        Vote foundVote = voteDAO.findChairPersonVoteByElectionIdAndDACUserId(election.getElectionId(), user.getDacUserId());
        assertNotNull(foundVote);
        assertEquals(vote.getVoteId(), foundVote.getVoteId());
    }

    @Test
    public void testCheckVoteById() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        Integer voteId = voteDAO.checkVoteById(election.getReferenceId(), vote.getVoteId());
        assertNotNull(voteId);
        assertEquals(vote.getVoteId(), voteId);
    }

    @Test
    public void testInsertVote() {
        // no-op ... tested by `createDacVote` and `createFinalVote`
    }

    @Test
    public void testDeleteVoteById() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        voteDAO.deleteVoteById(vote.getVoteId());
        Vote foundVote = voteDAO.findVoteById(vote.getVoteId());
        assertNull(foundVote);
    }

    @Test
    public void testCreateVote() {
        DACUser user = createUser();
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote v = createDacVote(user.getDacUserId(), election.getElectionId());

        Vote vote = voteDAO.findVoteById(v.getVoteId());
        assertNotNull(vote);
        Assert.assertNull(vote.getVote());
    }

    @Test
    public void testUpdateVote() {
        DACUser user = createUser();
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote v = createDacVote(user.getDacUserId(), election.getElectionId());

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
        DACUser user = createUser();
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote v = createDacVote(user.getDacUserId(), election.getElectionId());

        voteDAO.updateVoteReminderFlag(v.getVoteId(), true);
        Vote vote = voteDAO.findVoteById(v.getVoteId());
        assertTrue(vote.getIsReminderSent());

        voteDAO.updateVoteReminderFlag(v.getVoteId(), false);
        Vote vote2 = voteDAO.findVoteById(v.getVoteId());
        assertFalse(vote2.getIsReminderSent());
    }

    @Test
    public void testFindVotesByReferenceIdTypeAndUser() {
        DACUser user = createUser();
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote v = createDacVote(user.getDacUserId(), election.getElectionId());

        Vote vote = voteDAO.findVotesByReferenceIdTypeAndUser(election.getReferenceId(), user.getDacUserId(), v.getType());
        assertNotNull(vote);
        assertEquals(v.getVoteId(), vote.getVoteId());

        Vote vote2 = voteDAO.findVotesByReferenceIdTypeAndUser(election.getReferenceId(), user.getDacUserId(), v.getType().toLowerCase());
        assertNotNull(vote2);
        assertEquals(v.getVoteId(), vote2.getVoteId());

        Vote vote3 = voteDAO.findVotesByReferenceIdTypeAndUser(election.getReferenceId(), user.getDacUserId(), v.getType().toUpperCase());
        assertNotNull(vote3);
        assertEquals(v.getVoteId(), vote3.getVoteId());
    }

    @Test
    public void testFindVoteByTypeAndElectionId() {
        DACUser user = createUser();
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote v = createDacVote(user.getDacUserId(), election.getElectionId());

        List<Vote> votes = voteDAO.findVoteByTypeAndElectionId(election.getElectionId(), v.getType());
        assertNotNull(votes);
        assertFalse(votes.isEmpty());
        assertEquals(v.getVoteId(), votes.get(0).getVoteId());

        List<Vote> votes2 = voteDAO.findVoteByTypeAndElectionId(election.getElectionId(), v.getType().toLowerCase());
        assertNotNull(votes2);
        assertFalse(votes2.isEmpty());
        assertEquals(v.getVoteId(), votes2.get(0).getVoteId());

        List<Vote> votes3 = voteDAO.findVoteByTypeAndElectionId(election.getElectionId(), v.getType().toUpperCase());
        assertNotNull(votes3);
        assertFalse(votes3.isEmpty());
        assertEquals(v.getVoteId(), votes3.get(0).getVoteId());
    }

    @Test
    public void testFindTotalFinalVoteByElectionTypeAndVote() {
        DACUser user = createUser();
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        closeElection(election);
        Vote v = createFinalVote(user.getDacUserId(), election.getElectionId());
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

        int count = voteDAO.findTotalFinalVoteByElectionTypeAndVote(election.getElectionType(), voteValue);
        assertEquals(1, count);

        int count2 = voteDAO.findTotalFinalVoteByElectionTypeAndVote(election.getElectionType().toLowerCase(), voteValue);
        assertEquals(1, count2);

        int count3 = voteDAO.findTotalFinalVoteByElectionTypeAndVote(election.getElectionType().toUpperCase(), voteValue);
        assertEquals(1, count3);
    }

    @Test
    public void testFindMaxNumberOfDACMembers() {
        DACUser user = createUser();
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        closeElection(election);
        Vote v = createDacVote(user.getDacUserId(), election.getElectionId());
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
        DACUser user1 = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DACUser user2 = createUserWithRole(UserRoles.MEMBER.getRoleId());
        DACUser user3 = createUserWithRole(UserRoles.MEMBER.getRoleId());
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        List<Integer> userIds = Arrays.asList(user1.getDacUserId(), user2.getDacUserId(), user3.getDacUserId());

        voteDAO.insertVotes(userIds, election.getElectionId(), VoteType.DAC.getValue());
        List<Vote> votes = voteDAO.findVotesByElectionIds(Collections.singletonList(election.getElectionId()));
        assertNotNull(votes);
        assertFalse(votes.isEmpty());
        assertEquals(3, votes.size());
    }

    @Test
    public void testFindDataOwnerPendingVotesByElectionId() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

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
    public void testRemoveVotesByElectionIdAndUser() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        voteDAO.removeVotesByElectionIdAndUser(Collections.singletonList(election.getElectionId()), user.getDacUserId());
        Vote v = voteDAO.findVoteById(vote.getVoteId());
        assertNull(v);
    }

    @Test
    public void testDelegateDataSetOpenElectionsVotes() {
        DACUser fromUser = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DACUser toUser = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(fromUser.getDacUserId(), election.getElectionId());

        voteDAO.delegateDataSetOpenElectionsVotes(
                fromUser.getDacUserId(),
                Collections.singletonList(election.getElectionId()),
                toUser.getDacUserId());
        Vote v = voteDAO.findVoteById(vote.getVoteId());
        assertNotNull(v);
        assertEquals(toUser.getDacUserId(), v.getDacUserId());
        assertEquals(vote.getVoteId(), v.getDacUserId());
        assertNull(v.getVote());
        assertNull(v.getCreateDate());
        assertNull(v.getUpdateDate());
    }

    @Test
    public void testFindVotesOnOpenElections() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<Vote> votes = voteDAO.findVotesOnOpenElections(user.getDacUserId());
        assertNotNull(votes);
        assertFalse(votes.isEmpty());
        assertEquals(1, votes.size());
        assertEquals(vote.getVoteId(), votes.get(0).getDacUserId());
    }

    @Test
    public void testRemoveVotesByIds() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        voteDAO.removeVotesByIds(Collections.singletonList(vote.getVoteId()));
        Vote v = voteDAO.findVoteById(vote.getVoteId());
        assertNull(v);
    }

    @Test
    public void testFindVotesByElectionIdsAndUser() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createDacVote(user.getDacUserId(), election.getElectionId());

        List<Vote> votes = voteDAO.findVotesByElectionIdsAndUser(
                Collections.singletonList(election.getElectionId()),
                user.getDacUserId()
        );
        assertNotNull(votes);
        assertFalse(votes.isEmpty());
        assertEquals(1, votes.size());
        assertEquals(vote.getVoteId(), votes.get(0).getVoteId());
    }

    @Test
    public void testFindChairPersonVoteByElectionId() {
        DACUser user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Vote vote = createChairpersonVote(user.getDacUserId(), election.getElectionId());
        voteDAO.updateVote(
                true,
                RandomStringUtils.random(10),
                new Date(),
                vote.getVoteId(),
                false,
                election.getElectionId(),
                vote.getCreateDate(),
                false);

        Boolean v = voteDAO.findChairPersonVoteByElectionId(election.getElectionId());
        assertNotNull(v);
        assertTrue(v);
    }

}
