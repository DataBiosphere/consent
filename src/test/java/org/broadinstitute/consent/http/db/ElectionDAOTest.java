package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ElectionDAOTest extends DAOTestHelper {

    @Test
    public void testGetOpenElectionIdByReferenceId() {
        String accessReferenceId = UUID.randomUUID().toString();
        Dataset dataset = createDataset();
        Election accessElection = createDataAccessElection(accessReferenceId, dataset.getDataSetId());

        Integer electionId = electionDAO.getOpenElectionIdByReferenceId(accessReferenceId);
        Assertions.assertEquals(accessElection.getElectionId(), electionId);
        Integer missingElectionId = electionDAO.getOpenElectionIdByReferenceId("accessReferenceId");
        Assertions.assertNull(missingElectionId);
    }

    @Test
    public void testGetElectionIdsByReferenceIds() {
        String accessReferenceId1 = UUID.randomUUID().toString();
        String accessReferenceId2 = UUID.randomUUID().toString();
        Dataset dataset1 = createDataset();
        Dataset dataset2 = createDataset();
        Election accessElection1 = createDataAccessElection(accessReferenceId1, dataset1.getDataSetId());
        Election accessElection2 = createDataAccessElection(accessReferenceId2, dataset2.getDataSetId());

        List<Integer> electionIds = electionDAO.getElectionIdsByReferenceIds(List.of(accessReferenceId1, accessReferenceId2));
        Assertions.assertEquals(2, electionIds.size());
        assertTrue(electionIds.contains(accessElection1.getElectionId()));
        assertTrue(electionIds.contains(accessElection2.getElectionId()));
        List<Integer> missingElectionIds = electionDAO.getElectionIdsByReferenceIds(List.of("1", "2", "3"));
        assertTrue(missingElectionIds.isEmpty());
    }

    @Test
    public void testFindDacForConsentElection() {
        Dac dac = createDac();
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

        Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
        Assertions.assertNotNull(foundDac);
        Assertions.assertEquals(dac.getDacId(), foundDac.getDacId());
    }

    @Test
    public void testFindDacForConsentElectionWithNoAssociation() {
        Dac dac = createDac();
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

        Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
        Assertions.assertNotNull(foundDac);
        Assertions.assertEquals(dac.getDacId(), foundDac.getDacId());
    }

    @Test
    public void testFindDacForConsentElectionNotFound() {
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

        Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
        Assertions.assertNull(foundDac);
    }

    @Test
    public void testFindElectionByDacId() {
        Dac dac = createDac();
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

        List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
        Assertions.assertNotNull(foundElections);
        Assertions.assertEquals(election.getElectionId(), foundElections.get(0).getElectionId());
    }

    @Test
    public void testFindElectionsByReferenceId() {
        Dataset dataset = createDataset();
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();
        Election election1 = createDataAccessElection(referenceId, datasetId);
        Election election2 = createDataAccessElection(referenceId, datasetId);

        List<Election> found = electionDAO.findElectionsByReferenceId(referenceId);
        Assertions.assertEquals(2, found.size());

        assertTrue(found.contains(election1));
        assertTrue(found.contains(election2));
    }

    @Test
    public void testFindElectionsByReferenceIds() {
        Dataset dataset = createDataset();
        DataAccessRequest dar1 = createDataAccessRequestV3();
        DataAccessRequest dar2 = createDataAccessRequestV3();

        String referenceId1 = dar1.getReferenceId();
        String referenceId2 = dar2.getReferenceId();

        Integer datasetId = dataset.getDataSetId();

        Election election1 = createDataAccessElection(referenceId1, datasetId);
        Election election2 = createDataAccessElection(referenceId1, datasetId);

        Election election3 = createDataAccessElection(referenceId2, datasetId);
        Election election4 = createDataAccessElection(referenceId2, datasetId);

        List<Election> found = electionDAO.findElectionsByReferenceId(referenceId1);
        Assertions.assertEquals(2, found.size());

        found = electionDAO.findElectionsByReferenceId(referenceId2);
        Assertions.assertEquals(2, found.size());

        found = electionDAO.findElectionsByReferenceIds(List.of(referenceId1, referenceId2));
        Assertions.assertEquals(4, found.size());

        assertTrue(found.contains(election1));
        assertTrue(found.contains(election2));
        assertTrue(found.contains(election3));
        assertTrue(found.contains(election4));
    }

    @Test
    public void testFindLastElectionByReferenceIdDatasetIdAndType() {
        // Goal is to create elections for a single dar across two datasets
        // One set of elections will be canceled
        // A new set will then be created
        // We should find ONLY the most recent elections with this method
        User user = createUser();
        String darCode = "DAR-1234567890";
        Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
        DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
        Dataset d1 = createDataset();
        Dataset d2 = createDataset();
        dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDataSetId());
        dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d2.getDataSetId());
        // Create OPEN elections
        List<Integer> firstElectionIds = Stream
                .of(createElectionsForDarDataset(dar, d1), createElectionsForDarDataset(dar, d2))
                .flatMap(List::stream).toList();
        // Cancel those elections
        firstElectionIds.forEach(id -> electionDAO.updateElectionById(id, ElectionStatus.CANCELED.getValue(), new Date(), true));
        // Create a new set of elections
        List<Integer> latestElectionIds = Stream
                .of(createElectionsForDarDataset(dar, d1), createElectionsForDarDataset(dar, d2))
                .flatMap(List::stream).toList();

        Election latestAccessForD1 = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(dar.getReferenceId(), d1.getDataSetId(), ElectionType.DATA_ACCESS.getValue());
        Assertions.assertNotNull(latestAccessForD1);
        Assertions.assertFalse(firstElectionIds.contains(latestAccessForD1.getElectionId()));
        assertTrue(latestElectionIds.contains(latestAccessForD1.getElectionId()));

        Election latestRPForD1 = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(dar.getReferenceId(), d1.getDataSetId(), ElectionType.RP.getValue());
        Assertions.assertNotNull(latestRPForD1);
        Assertions.assertFalse(firstElectionIds.contains(latestRPForD1.getElectionId()));
        assertTrue(latestElectionIds.contains(latestRPForD1.getElectionId()));

        Election latestAccessForD2 = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(dar.getReferenceId(), d2.getDataSetId(), ElectionType.DATA_ACCESS.getValue());
        Assertions.assertNotNull(latestAccessForD2);
        Assertions.assertFalse(firstElectionIds.contains(latestAccessForD2.getElectionId()));
        assertTrue(latestElectionIds.contains(latestAccessForD2.getElectionId()));

        Election latestRPForD2 = electionDAO.findLastElectionByReferenceIdDatasetIdAndType(dar.getReferenceId(), d2.getDataSetId(), ElectionType.RP.getValue());
        Assertions.assertNotNull(latestRPForD2);
        Assertions.assertFalse(firstElectionIds.contains(latestRPForD2.getElectionId()));
        assertTrue(latestElectionIds.contains(latestRPForD2.getElectionId()));
    }

    /**
     * Small helper method for `testFindLastElectionByReferenceIdDatasetIdAndType()`
     * Creates OPEN Access and RP elections for dar/dataset combination
     *
     * @param dar DataAccessRequest
     * @param d   Dataset
     * @return List of created electionIds
     */
    private List<Integer> createElectionsForDarDataset(DataAccessRequest dar, Dataset d) {
        Election accessElection = createDataAccessElection(dar.getReferenceId(), d.getDataSetId());
        Election rpElection = createRPElection(dar.getReferenceId(), d.getDataSetId());
        electionDAO.insertAccessRP(accessElection.getElectionId(), rpElection.getElectionId());
        return List.of(accessElection.getElectionId(), rpElection.getElectionId());
    }

    @Test
    public void testFindElectionsByReferenceIdAndDatasetId() {
        User user = createUser();
        String darCode = "DAR-1234567890";
        Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
        DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
        Dataset d1 = createDataset();
        dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDataSetId());
        Election accessElection = createDataAccessElection(dar.getReferenceId(), d1.getDataSetId());
        Election rpElection = createRPElection(dar.getReferenceId(), d1.getDataSetId());
        electionDAO.insertAccessRP(accessElection.getElectionId(), rpElection.getElectionId());

        List<Election> elections = electionDAO.findElectionsByReferenceIdAndDatasetId(dar.getReferenceId(), d1.getDataSetId());
        Assertions.assertEquals(2, elections.size());
    }

    @Test
    public void testDeleteElectionFromAccessRP() {
        Dac dac = createDac();
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        Election accessElection = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());
        Election rpElection = createRPElection(consent.getConsentId(), dataset.getDataSetId());

        electionDAO.insertAccessRP(accessElection.getElectionId(), rpElection.getElectionId());

        Assertions.assertEquals(rpElection.getElectionId(),
            electionDAO.findRPElectionByElectionAccessId(accessElection.getElectionId()));
        Assertions.assertEquals(accessElection.getElectionId(),
            electionDAO.findAccessElectionByElectionRPId(rpElection.getElectionId()));

        // can delete using access election
        electionDAO.deleteElectionFromAccessRP(accessElection.getElectionId());

        Assertions.assertNull(
            electionDAO.findRPElectionByElectionAccessId(accessElection.getElectionId()));
        Assertions.assertNull(
            electionDAO.findAccessElectionByElectionRPId(rpElection.getElectionId()));

        electionDAO.insertAccessRP(accessElection.getElectionId(), rpElection.getElectionId());

        Assertions.assertEquals(rpElection.getElectionId(),
            electionDAO.findRPElectionByElectionAccessId(accessElection.getElectionId()));
        Assertions.assertEquals(accessElection.getElectionId(),
            electionDAO.findAccessElectionByElectionRPId(rpElection.getElectionId()));

        // or by using rp election
        electionDAO.deleteElectionFromAccessRP(rpElection.getElectionId());

        Assertions.assertNull(
            electionDAO.findRPElectionByElectionAccessId(accessElection.getElectionId()));
        Assertions.assertNull(
            electionDAO.findAccessElectionByElectionRPId(rpElection.getElectionId()));
    }

    @Test
    public void testFindElectionByDacIdWithNoAssociation() {
        Dac dac = createDac();
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

        List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
        Assertions.assertNotNull(foundElections);
        Assertions.assertEquals(election.getElectionId(), foundElections.get(0).getElectionId());
    }

    @Test
    public void testFindElectionByDacIdNotFound() {
        Dac dac = createDac();
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

        List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
        assertTrue(foundElections.isEmpty());
    }

    @Test
    public void testFindAccessElectionWithFinalVoteById() {
        User u = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent c = createConsent();
        Dataset d = createDataset();
        datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

        consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
        Election e = createDataAccessElection(c.getConsentId(), d.getDataSetId());
        Integer voteId = voteDAO.insertVote(u.getUserId(), e.getElectionId(), VoteType.FINAL.getValue());
        voteDAO.updateVote(true, "rationale", new Date(), voteId, false, e.getElectionId(), new Date(), false);
        Vote v = voteDAO.findVoteById(voteId);

        Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
        Assertions.assertNotNull(election);
        Assertions.assertEquals(e.getElectionId(), election.getElectionId());
        Assertions.assertEquals(v.getVote(), election.getFinalVote());
    }

    @Test
    public void testRPFindElectionWithFinalVoteById() {
        User u = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent c = createConsent();
        Dataset d = createDataset();
        datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

        consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
        Election e = createRPElection(c.getConsentId(), d.getDataSetId());
        Vote v = createPopulatedChairpersonVote(u.getUserId(), e.getElectionId());

        Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
        Assertions.assertNotNull(election);
        Assertions.assertEquals(e.getElectionId(), election.getElectionId());
        Assertions.assertEquals(v.getVote(), election.getFinalVote());
    }

    @Test
    public void testDatasetFindElectionWithFinalVoteById() {
        User u = createUserWithRole(UserRoles.DATAOWNER.getRoleId());
        Dac dac = createDac();
        Consent c = createConsent();
        Dataset d = createDataset();
        datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

        consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
        Integer electionId = electionDAO.insertElection(
                ElectionType.DATA_SET.getValue(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                c.getConsentId(),
                d.getDataSetId());
        Election e = electionDAO.findElectionById(electionId);

        Integer voteId = voteDAO.insertVote(u.getUserId(), e.getElectionId(), VoteType.DATA_OWNER.getValue());
        voteDAO.updateVote(true, "rationale", new Date(), voteId, false, e.getElectionId(), new Date(), false);
        Vote v = voteDAO.findVoteById(voteId);

        Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
        Assertions.assertNotNull(election);
        Assertions.assertEquals(e.getElectionId(), election.getElectionId());
        Assertions.assertEquals(v.getVote(), election.getFinalVote());
    }

    @Test
    public void testDULFindElectionWithFinalVoteById() {
        User u = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac dac = createDac();
        Consent c = createConsent();
        Dataset d = createDataset();
        datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

        consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
        Integer electionId = electionDAO.insertElection(
                ElectionType.TRANSLATE_DUL.getValue(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                c.getConsentId(),
                d.getDataSetId());
        Election e = electionDAO.findElectionById(electionId);
        Vote v = createPopulatedChairpersonVote(u.getUserId(), e.getElectionId());

        Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
        Assertions.assertNotNull(election);
        Assertions.assertEquals(e.getElectionId(), election.getElectionId());
        Assertions.assertEquals(v.getVote(), election.getFinalVote());
    }

    @Test
    public void testFindElectionsByReferenceIdCase1() {
        DataAccessRequest dar = createDataAccessRequestV3();
        Dataset d = createDataset();
        createDataAccessElection(dar.getReferenceId(), d.getDataSetId());
        createRPElection(dar.getReferenceId(), d.getDataSetId());

        List<Election> elections = electionDAO.findElectionsByReferenceId(dar.getReferenceId());
        Assertions.assertNotNull(elections);
        Assertions.assertEquals(2, elections.size());
    }

    @Test
    public void testInsertExtendedElection() {
        Dac dac = createDac();
        Consent c = createConsent();
        Dataset d = createDataset();
        User u = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        datasetDAO.updateDatasetDacId(d.getDataSetId(), dac.getDacId());

        consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, d.getDataSetId());
        Integer electionId = electionDAO.insertElection(
                ElectionType.DATA_ACCESS.getValue(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                c.getConsentId(),
                d.getDataSetId());
        Election e = electionDAO.findElectionById(electionId);
        createFinalVote(u.getUserId(), e.getElectionId());
        Election election = electionDAO.findElectionWithFinalVoteById(e.getElectionId());
        Assertions.assertNotNull(election);
        Assertions.assertEquals(e.getElectionId(), election.getElectionId());
    }

    @Test
    public void testFindLastElectionsByReferenceIdsAndType() {
        DataAccessRequest dar = createDataAccessRequestV3();
        Dataset d = createDataset();
        electionDAO.insertElection(
                ElectionType.DATA_ACCESS.getValue(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                dar.getReferenceId(),
                d.getDataSetId());
        List<Election> elections =
                electionDAO.findLastElectionsByReferenceIdsAndType(
                        Collections.singletonList(dar.getReferenceId()), ElectionType.DATA_ACCESS.getValue());
        Assertions.assertNotNull(elections);
        Assertions.assertFalse(elections.isEmpty());
        Assertions.assertEquals(1, elections.size());
    }

    @Test
    public void testFindAllDacsForElectionIds() {
        Dac dac = createDac();
        String accessReferenceId = UUID.randomUUID().toString();
        Dataset dataset = createDataset();
        Integer datasetId = dataset.getDataSetId();
        Consent consent = createConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        Integer electionId = electionDAO.insertElection(
                ElectionType.TRANSLATE_DUL.getValue(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                consent.getConsentId(),
                datasetId);
        Election dulElection = electionDAO.findElectionById(electionId);
        Election accessElection = createDataAccessElection(accessReferenceId, datasetId);
        electionDAO.insertAccessAndConsentElection(accessElection.getElectionId(), dulElection.getElectionId());

        List<Integer> electionIds = Collections.singletonList(accessElection.getElectionId());
        List<Dac> dacList = electionDAO.findAllDacsForElectionIds(electionIds);
        Dac dacRecord = dacList.get(0);
        Assertions.assertEquals(1, dacList.size());
        Assertions.assertEquals(dac.getName(), dacRecord.getName());
        Assertions.assertEquals(dac.getDacId(), dacRecord.getDacId());
    }

    @Test
    public void testFindAllDacsForElectionIds_EmptyList() {
        List<Integer> electionIds = Collections.singletonList(10000);
        List<Dac> dacList = electionDAO.findAllDacsForElectionIds(electionIds);
        assertTrue(dacList.isEmpty());
    }

    @Test
    public void testFindLastElectionsByReferenceIds() {
        Dac dac = createDac();
        Consent consent = createConsent();
        Dataset dataset = createDataset();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();

        String darReferenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();
        dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), datasetId);
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        Election cancelledAccessElection = createDataAccessElection(darReferenceId, datasetId);
        Election cancelledRPElection = createRPElection(darReferenceId, datasetId);
        electionDAO.updateElectionById(
                cancelledAccessElection.getElectionId(), ElectionStatus.CANCELED.getValue(), new Date(), true);
        electionDAO.updateElectionById(
                cancelledRPElection.getElectionId(), ElectionStatus.CANCELED.getValue(), new Date(), true);

        Election prevClosedAccessElection = createDataAccessElection(darReferenceId, dataset.getDataSetId());
        Election prevClosedRPElection = createDataAccessElection(darReferenceId, datasetId);
        electionDAO.updateElectionById(
                prevClosedAccessElection.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);
        electionDAO.updateElectionById(
                prevClosedRPElection.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);

        Election recentClosedAccessElection = createDataAccessElection(darReferenceId, dataset.getDataSetId());
        Election recentClosedRPElection = createRPElection(darReferenceId, datasetId);
        electionDAO.updateElectionById(
                recentClosedAccessElection.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);
        electionDAO.updateElectionById(
                recentClosedRPElection.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);
        List<Election> elections =
                electionDAO.findLastElectionsByReferenceIds(Collections.singletonList(dar.referenceId));
        List<Integer> electionIds = elections.stream().map(Election::getElectionId).toList();
        Assertions.assertFalse(elections.isEmpty());
        Assertions.assertEquals(2, elections.size());
        assertTrue(electionIds.contains(recentClosedAccessElection.getElectionId()));
        assertTrue(electionIds.contains(recentClosedRPElection.getElectionId()));
    }

    @Test
    public void testFindLastElectionsByReferenceIds_EmptyList() {
        List<Election> elections =
                electionDAO.findLastElectionsByReferenceIds(Collections.singletonList(UUID.randomUUID().toString()));
        assertTrue(elections.isEmpty());
    }

    @Test
    public void testFindElectionsByVoteIdsAndType_DataAccess() {
        DataAccessRequest dar = createDataAccessRequestV3();
        Dataset dataset = createDataset();
        String referenceId = dar.getReferenceId();
        int datasetId = dataset.getDataSetId();
        Election accessElection = createDataAccessElection(referenceId, datasetId);
        Election rpElection = createRPElection(referenceId, datasetId);
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        int userId = user.getUserId();
        Vote accessVote = createChairpersonVote(userId, accessElection.getElectionId());
        Vote rpVote = createChairpersonVote(userId, rpElection.getElectionId());
        List<Integer> voteIds = List.of(accessVote.getVoteId(), rpVote.getVoteId());
        List<Election> elections = electionDAO.findElectionsByVoteIdsAndType(voteIds, "dataaccess");

        Assertions.assertEquals(1, elections.size());
        Assertions.assertEquals(accessElection.getElectionId(), elections.get(0).getElectionId());
    }

    @Test
    public void testFindElectionsByVoteIdsAndType_RP() {
        DataAccessRequest dar = createDataAccessRequestV3();
        Dataset dataset = createDataset();
        String referenceId = dar.getReferenceId();
        int datasetId = dataset.getDataSetId();
        Election accessElection = createDataAccessElection(referenceId, datasetId);
        Election rpElection = createRPElection(referenceId, datasetId);
        User user = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        int userId = user.getUserId();
        Vote accessVote = createChairpersonVote(userId, accessElection.getElectionId());
        Vote rpVote = createChairpersonVote(userId, rpElection.getElectionId());
        List<Integer> voteIds = List.of(accessVote.getVoteId(), rpVote.getVoteId());
        List<Election> elections = electionDAO.findElectionsByVoteIdsAndType(voteIds, "rp");

        Assertions.assertEquals(1, elections.size());
        Assertions.assertEquals(rpElection.getElectionId(), elections.get(0).getElectionId());
    }

    @Test
    public void testFindElectionsWithCardHoldingUsersByElectionIds() {
        User lcUser = createUser();
        User nonLCUser = createUser();
        Dataset dataset = createDataset();
        int datasetId = dataset.getDataSetId();
        createLibraryCard(lcUser);
        DataAccessRequest lcDAR = createDataAccessRequestWithUserIdV3(lcUser.getUserId());
        DataAccessRequest nonLCDAR = createDataAccessRequestWithUserIdV3(nonLCUser.getUserId());
        Election lcElection = createDataAccessElection(lcDAR.getReferenceId(), datasetId);
        Election nonLCElection = createDataAccessElection(nonLCDAR.getReferenceId(), datasetId);
        List<Integer> electionIds = List.of(lcElection.getElectionId(), nonLCElection.getElectionId());
        List<Election> elections = electionDAO.findElectionsWithCardHoldingUsersByElectionIds(electionIds);

        Assertions.assertEquals(1, elections.size());
        Assertions.assertEquals(elections.get(0).getElectionId(), lcElection.getElectionId());
    }

    @Test
    public void testFindOpenElectionsByReferenceIds() {
        DataAccessRequest dar = createDataAccessRequestV3();
        Dataset dataset = createDataset();
        String referenceId = dar.getReferenceId();
        int datasetId = dataset.getDataSetId();
        Election accessElection = createDataAccessElection(referenceId, datasetId);
        Election rpElection = createRPElection(referenceId, datasetId);

        List<Election> elections = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
        Assertions.assertEquals(2, elections.size());

        electionDAO.updateElectionStatus(List.of(accessElection.getElectionId(), rpElection.getElectionId()), ElectionStatus.CANCELED.getValue());
        List<Election> electionsV2 = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
        Assertions.assertEquals(0, electionsV2.size());
    }

    @Test
    public void testDeleteByReferenceId() {
        Dataset dataset = createDataset();
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();
        Election e = createDataAccessElection(referenceId, datasetId);

        List<Election> elections = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
        Assertions.assertEquals(1, elections.size());

        electionDAO.deleteElectionById(e.getElectionId());

        elections = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
        Assertions.assertEquals(0, elections.size());

    }

    @Test
    public void testDeleteByReferenceIds() {
        Dataset dataset = createDataset();
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();
        Election accessElection = createDataAccessElection(referenceId, datasetId);
        Election rpElection = createRPElection(referenceId, datasetId);

        List<Election> elections = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
        Assertions.assertEquals(2, elections.size());

        electionDAO.deleteElectionsByIds(List.of(accessElection.getElectionId(), rpElection.getElectionId()));

        elections = electionDAO.findOpenElectionsByReferenceIds(List.of(dar.referenceId));
        Assertions.assertEquals(0, elections.size());
    }


    @Test
    public void testFindElectionsWithFinalVoteByReferenceId() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election accessElection = createDataAccessElection(referenceId, datasetId);
        Election rpElection = createRPElection(referenceId, datasetId);
        createFinalVote(user.getUserId(), accessElection.getElectionId());
        createFinalVote(user.getUserId(), rpElection.getElectionId());

        // create irrelevant elections that should not be returned
        createDataAccessElection(referenceId, datasetId);
        createDataAccessElection(referenceId, datasetId);
        createDataAccessElection(referenceId, datasetId);
        createRPElection(referenceId, datasetId);
        createRPElection(referenceId, datasetId);
        createRPElection(referenceId, datasetId);

        List<Election> elections = electionDAO.findElectionsWithFinalVoteByReferenceId(referenceId);

        Assertions.assertEquals(2, elections.size());
        assertTrue(elections.contains(accessElection));
        assertTrue(elections.contains(rpElection));
    }

    @Test
    public void testInsertAndFindElection() {

        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Date d = new Date();

        Integer id = electionDAO.insertElection(
                ElectionType.DATA_ACCESS.getValue(),
                ElectionStatus.OPEN.getValue(),
                d,
                referenceId,
                datasetId);

        Election e = electionDAO.findElectionById(id);

        Assertions.assertEquals(ElectionType.DATA_ACCESS.getValue(), e.getElectionType());
        Assertions.assertEquals(ElectionStatus.OPEN.getValue(), e.getStatus());
        Assertions.assertNotNull(e.getCreateDate());
        Assertions.assertEquals(referenceId, e.getReferenceId());
        Assertions.assertEquals(datasetId, e.getDataSetId());

    }

    @Test
    public void testUpdateElectionById() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election before = createDataAccessElection(referenceId, datasetId);

        Assertions.assertEquals(ElectionStatus.OPEN.getValue(), before.getStatus());
        Assertions.assertNull(before.getLastUpdate());

        electionDAO.updateElectionById(
                before.getElectionId(),
                ElectionStatus.FINAL.getValue(),
                new Date());

        Election after = electionDAO.findElectionById(before.getElectionId());


        Assertions.assertEquals(ElectionStatus.FINAL.getValue(), after.getStatus());
        Assertions.assertNotNull(after.getLastUpdate());
    }

    @Test
    public void testUpdateElectionById_FinalAccessVote() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election before = createDataAccessElection(referenceId, datasetId);

        Assertions.assertEquals(ElectionStatus.OPEN.getValue(), before.getStatus());
        Assertions.assertNull(before.getLastUpdate());
        Assertions.assertEquals(null, before.getFinalAccessVote());

        electionDAO.updateElectionById(
                before.getElectionId(),
                ElectionStatus.FINAL.getValue(),
                new Date(),
                true);

        Election after = electionDAO.findElectionById(before.getElectionId());


        Assertions.assertEquals(ElectionStatus.FINAL.getValue(), after.getStatus());
        Assertions.assertEquals(true, after.getFinalAccessVote());
        Assertions.assertNotNull(after.getLastUpdate());
    }

    @Test
    public void testUpdateElectionStatus() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e1 = createDataAccessElection(referenceId, datasetId);
        Election e2 = createRPElection(referenceId, datasetId);
        Election e3 = createDataAccessElection(referenceId, datasetId);

        Assertions.assertEquals(ElectionStatus.OPEN.getValue(), e1.getStatus());
        Assertions.assertEquals(ElectionStatus.OPEN.getValue(), e2.getStatus());
        Assertions.assertEquals(ElectionStatus.OPEN.getValue(), e3.getStatus());

        electionDAO.updateElectionStatus(
                List.of(e1.getElectionId(), e2.getElectionId(), e3.getElectionId()),
                ElectionStatus.FINAL.getValue());

        e1 = electionDAO.findElectionById(e1.getElectionId());
        e2 = electionDAO.findElectionById(e2.getElectionId());
        e3 = electionDAO.findElectionById(e3.getElectionId());


        Assertions.assertEquals(ElectionStatus.FINAL.getValue(), e1.getStatus());
        Assertions.assertEquals(ElectionStatus.FINAL.getValue(), e2.getStatus());
        Assertions.assertEquals(ElectionStatus.FINAL.getValue(), e3.getStatus());
    }

    @Test
    public void testGetElectionWithFinalVoteByReferenceIdAndType() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election accessElection = createDataAccessElection(referenceId, datasetId);
        Election rpElection = createRPElection(referenceId, datasetId);
        createFinalVote(user.getUserId(), accessElection.getElectionId());
        createFinalVote(user.getUserId(), rpElection.getElectionId());
        // does not use status, should return given any status
        electionDAO.updateElectionStatus(
                List.of(accessElection.getElectionId()),
                ElectionStatus.CANCELED.getValue());


        // create irrelevant elections that should not be returned
        createDataAccessElection(referenceId, datasetId);
        createRPElection(referenceId, datasetId);

        // returns data access even if rp exists
        Election returned =
                electionDAO.getElectionWithFinalVoteByReferenceIdAndType(
                        referenceId,
                        ElectionType.DATA_ACCESS.getValue());

        Assertions.assertEquals(accessElection.getElectionId(), returned.getElectionId());

        // returns rp even if data access exists
        returned =
                electionDAO.getElectionWithFinalVoteByReferenceIdAndType(
                        referenceId,
                        ElectionType.RP.getValue());

        Assertions.assertEquals(rpElection.getElectionId(), returned.getElectionId());
    }

    @Test
    public void testFindElectionWithFinalVoteById_NotFinal() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e = createDataAccessElection(referenceId, datasetId);

        Election returned =
                electionDAO.findElectionWithFinalVoteById(
                        e.getElectionId());

        Assertions.assertNull(returned);
    }

    @Test
    public void testFindElectionWithFinalVoteById_Success() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e = createDataAccessElection(referenceId, datasetId);
        createFinalVote(user.getUserId(), e.getElectionId());

        Election returned =
                electionDAO.findElectionWithFinalVoteById(
                        e.getElectionId());

        Assertions.assertEquals(e.getElectionId(), returned.getElectionId());
    }

    @Test
    public void testFindElectionByVoteId() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e = createDataAccessElection(referenceId, datasetId);
        Vote v = createFinalVote(user.getUserId(), e.getElectionId());

        Election returned = electionDAO.findElectionByVoteId(v.getVoteId());

        Assertions.assertEquals(e.getElectionId(), returned.getElectionId());
    }

    @Test
    public void testFindElectionsWithFinalVoteByTypeAndStatus_NoMatching() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election accessElection = createDataAccessElection(referenceId, datasetId);
        Election rpElection = createRPElection(referenceId, datasetId);
        createFinalVote(user.getUserId(), accessElection.getElectionId());
        createFinalVote(user.getUserId(), rpElection.getElectionId());
        electionDAO.updateElectionStatus(
                List.of(accessElection.getElectionId()),
                ElectionStatus.CANCELED.getValue());


        // create irrelevant elections that should not be returned
        createDataAccessElection(referenceId, datasetId);
        createRPElection(referenceId, datasetId);

        List<Election> returned =
                electionDAO.findElectionsWithFinalVoteByTypeAndStatus(
                        ElectionType.DATA_ACCESS.getValue(),
                        ElectionStatus.OPEN.getValue());

        Assertions.assertEquals(0, returned.size());
    }

    @Test
    public void testFindElectionsWithFinalVoteByTypeAndStatus_SelectsOnType() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election rpElection = createRPElection(referenceId, datasetId);
        createFinalVote(user.getUserId(), rpElection.getElectionId());


        createDataAccessElection(referenceId, datasetId);
        createRPElection(referenceId, datasetId);

        List<Election> returned =
                electionDAO.findElectionsWithFinalVoteByTypeAndStatus(
                        ElectionType.DATA_ACCESS.getValue(),
                        ElectionStatus.OPEN.getValue());

        Assertions.assertEquals(0, returned.size());
    }

    @Test
    public void testFindElectionsWithFinalVoteByTypeAndStatus_SelectsOnStatus() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election openElection = createDataAccessElection(referenceId, datasetId);
        createFinalVote(user.getUserId(), openElection.getElectionId());
        electionDAO.updateElectionById(
                openElection.getElectionId(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                true);


        List<Election> returned =
                electionDAO.findElectionsWithFinalVoteByTypeAndStatus(
                        ElectionType.DATA_ACCESS.getValue(),
                        ElectionStatus.CANCELED.getValue());

        Assertions.assertEquals(0, returned.size());

    }

    @Test
    public void testFindLastElectionsWithFinalVoteByType_Ordered() {
        Dac dac = createDac();
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());

        Dataset ds1 = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar1 = createDataAccessRequestV3();
        String referenceId1 = dar1.getReferenceId();
        Integer datasetId1 = ds1.getDataSetId();

        Dataset ds2 = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar2 = createDataAccessRequestV3();
        String referenceId2 = dar2.getReferenceId();
        Integer datasetId2 = ds2.getDataSetId();

        Election ds1FirstElection = createDataAccessElection(referenceId1, datasetId1);
        Election ds1SecondElection = createDataAccessElection(referenceId1, datasetId1);
        createFinalVote(user.getUserId(), ds1FirstElection.getElectionId());
        createFinalVote(user.getUserId(), ds1SecondElection.getElectionId());

        electionDAO.updateElectionById(ds1FirstElection.getElectionId(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                true);
        electionDAO.updateElectionById(ds1SecondElection.getElectionId(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                true);

        Election ds2OnlyElection = createDataAccessElection(referenceId2, datasetId2);
        createFinalVote(user.getUserId(), ds2OnlyElection.getElectionId());
        electionDAO.updateElectionById(ds2OnlyElection.getElectionId(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                true);

        List<Election> returned =
                electionDAO.findLastElectionsWithFinalVoteByType(
                        ElectionType.DATA_ACCESS.getValue());

        Assertions.assertEquals(2, returned.size());
        Assertions.assertEquals(ds1SecondElection.getElectionId(), returned.get(0).getElectionId());
        Assertions.assertEquals(ds1SecondElection.getElectionId(), returned.get(0).getElectionId());

    }

    @Test
    public void testFindLastElectionsWithFinalVoteByType_SelectsOnType() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election dataAccessElection = createDataAccessElection(referenceId, datasetId);
        createFinalVote(user.getUserId(), dataAccessElection.getElectionId());
        electionDAO.updateElectionById(
                dataAccessElection.getElectionId(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                true);

        List<Election> returned =
                electionDAO.findLastElectionsWithFinalVoteByType(
                        ElectionType.RP.getValue());

        Assertions.assertEquals(0, returned.size());
    }

    @Test
    public void testFindLastDataAccessElectionsWithFinalVoteByStatus_FiltersByStatus() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election first = createDataAccessElection(referenceId, datasetId);
        Election second = createDataAccessElection(referenceId, datasetId);
        createFinalVote(user.getUserId(), first.getElectionId());
        createFinalVote(user.getUserId(), second.getElectionId());
        electionDAO.updateElectionById(second.getElectionId(),
                ElectionStatus.CLOSED.getValue(),
                new Date(),
                true);

        List<Election> returned =
                electionDAO.findLastDataAccessElectionsWithFinalVoteByStatus(
                        ElectionStatus.OPEN.getValue());

        Assertions.assertEquals(0, returned.size());
    }

    @Test
    public void testFindLastDataAccessElectionsWithFinalVoteByStatus_WrongType() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election rpElection = createRPElection(referenceId, datasetId);
        createFinalVote(user.getUserId(), rpElection.getElectionId());

        List<Election> returned =
                electionDAO.findLastDataAccessElectionsWithFinalVoteByStatus(
                        ElectionStatus.OPEN.getValue());

        Assertions.assertEquals(0, returned.size());
    }

    @Test
    public void testFindTotalElectionsByTypeStatusAndVote() {
        Dac dac = createDac();
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());

        Dataset ds = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = ds.getDataSetId();

        Election e = createDataAccessElection(referenceId, datasetId);
        createFinalVote(user.getUserId(), e.getElectionId());
        electionDAO.updateElectionById(
                e.getElectionId(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                false
        );

        Assertions.assertEquals(0, (long) electionDAO.findTotalElectionsByTypeStatusAndVote(
                ElectionType.DATA_ACCESS.getValue(),
                ElectionStatus.OPEN.getValue(),
                true
        ));

        Assertions.assertEquals(0, (long) electionDAO.findTotalElectionsByTypeStatusAndVote(
                ElectionType.DATA_ACCESS.getValue(),
                ElectionStatus.CANCELED.getValue(),
                false
        ));

        Assertions.assertEquals(0, (long) electionDAO.findTotalElectionsByTypeStatusAndVote(
                ElectionType.RP.getValue(),
                ElectionStatus.OPEN.getValue(),
                false
        ));
    }

    @Test
    public void testFindLastElectionsByReferenceIdAndType() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election dataAccessElection = createDataAccessElection(referenceId, datasetId);

        List<Election> returned =
                electionDAO.findLastElectionsByReferenceIdAndType(
                        referenceId,
                        ElectionType.DATA_ACCESS.getValue());

        Assertions.assertEquals(1, returned.size());
        Assertions.assertEquals(dataAccessElection.getElectionId(),
            returned.get(0).getElectionId());
    }

    @Test
    public void testFindLastElectionsByReferenceIdAndType_WrongType() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        createDataAccessElection(referenceId, datasetId);

        List<Election> returned =
                electionDAO.findLastElectionsByReferenceIdAndType(
                        referenceId,
                        ElectionType.RP.getValue());

        Assertions.assertEquals(0, returned.size());
    }

    @Test
    public void testFindLastElectionByReferenceIdAndStatus() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election dataAccessElection = createDataAccessElection(referenceId, datasetId);

        Election returned =
                electionDAO.findLastElectionByReferenceIdAndStatus(
                        referenceId,
                        ElectionStatus.OPEN.getValue());

        Assertions.assertEquals(dataAccessElection.getElectionId(), returned.getElectionId());
    }

    @Test
    public void testFindLastElectionByReferenceIdAndStatus_WrongType() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        createDataAccessElection(referenceId, datasetId);

        Election returned =
                electionDAO.findLastElectionByReferenceIdAndStatus(
                        referenceId,
                        ElectionStatus.CLOSED.getValue());

        Assertions.assertNull(returned);
    }

    @Test
    public void testFindLastElectionsWithFinalVoteByReferenceIdsTypeAndStatus() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election dataAccessElection = createDataAccessElection(referenceId, datasetId);
        createFinalVote(user.getUserId(), dataAccessElection.getElectionId());
        electionDAO.updateElectionById(
                dataAccessElection.getElectionId(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                true
        );

        List<Election> returned =
                electionDAO.findLastElectionsWithFinalVoteByReferenceIdsTypeAndStatus(
                        List.of(referenceId),
                        ElectionStatus.OPEN.getValue());

        Assertions.assertEquals(1, returned.size());
        Assertions.assertEquals(dataAccessElection.getElectionId(),
            returned.get(0).getElectionId());
    }

    @Test
    public void testFindLastElectionsWithFinalVoteByReferenceIdsTypeAndStatus_WrongType() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election dataAccessElection = createDataAccessElection(referenceId, datasetId);
        createFinalVote(user.getUserId(), dataAccessElection.getElectionId());
        electionDAO.updateElectionById(
                dataAccessElection.getElectionId(),
                ElectionStatus.CLOSED.getValue(),
                new Date(),
                true
        );

        List<Election> returned =
                electionDAO.findLastElectionsWithFinalVoteByReferenceIdsTypeAndStatus(
                        List.of(referenceId),
                        ElectionStatus.OPEN.getValue());

        Assertions.assertEquals(0, returned.size());
    }

    @Test
    public void testFindLastElectionByReferenceIdAndType() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election dataAccessElection = createDataAccessElection(referenceId, datasetId);

        Election returned =
                electionDAO.findLastElectionByReferenceIdAndType(
                        referenceId,
                        ElectionType.DATA_ACCESS.getValue());

        Assertions.assertEquals(dataAccessElection.getElectionId(), returned.getElectionId());
    }

    @Test
    public void testFindLastElectionByReferenceIdAndType_WrongType() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        createRPElection(referenceId, datasetId);

        Election returned =
                electionDAO.findLastElectionByReferenceIdAndType(
                        referenceId,
                        ElectionType.DATA_ACCESS.getValue());

        Assertions.assertNull(returned);
    }

    @Test
    public void testFindElectionByAccessRPAssociation() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election datasetAccessElection = createDataAccessElection(referenceId, datasetId);
        Election rpElection = createRPElection(referenceId, datasetId);

        electionDAO.insertAccessRP(
                datasetAccessElection.getElectionId(),
                rpElection.getElectionId());

        Assertions.assertEquals(datasetAccessElection.getElectionId(), electionDAO.findAccessElectionByElectionRPId(
                rpElection.getElectionId()));

        Assertions.assertEquals(rpElection.getElectionId(), electionDAO.findRPElectionByElectionAccessId(
                datasetAccessElection.getElectionId()));
    }

    @Test
    public void testDeleteAccessRP() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election datasetAccessElection = createDataAccessElection(referenceId, datasetId);
        Election rpElection = createRPElection(referenceId, datasetId);

        electionDAO.insertAccessRP(
                datasetAccessElection.getElectionId(),
                rpElection.getElectionId());

        Assertions.assertEquals(datasetAccessElection.getElectionId(), electionDAO.findAccessElectionByElectionRPId(
                rpElection.getElectionId()));

        electionDAO.deleteAccessRP(datasetAccessElection.getElectionId());

        Assertions.assertNull(electionDAO.findAccessElectionByElectionRPId(
                rpElection.getElectionId()));

    }

    @Test
    public void testDeleteElectionsFromAccessRPs_Access() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election datasetAccessElection = createDataAccessElection(referenceId, datasetId);
        Election rpElection = createRPElection(referenceId, datasetId);

        electionDAO.insertAccessRP(
                datasetAccessElection.getElectionId(),
                rpElection.getElectionId());

        Assertions.assertEquals(datasetAccessElection.getElectionId(), electionDAO.findAccessElectionByElectionRPId(
                rpElection.getElectionId()));

        electionDAO.deleteElectionFromAccessRP(datasetAccessElection.getElectionId());

        Assertions.assertNull(electionDAO.findAccessElectionByElectionRPId(
                rpElection.getElectionId()));
    }

    @Test
    public void testDeleteElectionsFromAccessRPs_RP() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election datasetAccessElection = createDataAccessElection(referenceId, datasetId);
        Election rpElection = createRPElection(referenceId, datasetId);

        electionDAO.insertAccessRP(
                datasetAccessElection.getElectionId(),
                rpElection.getElectionId());

        Assertions.assertEquals(datasetAccessElection.getElectionId(), electionDAO.findAccessElectionByElectionRPId(
                rpElection.getElectionId()));

        electionDAO.deleteElectionFromAccessRP(rpElection.getElectionId());

        Assertions.assertNull(electionDAO.findAccessElectionByElectionRPId(
                rpElection.getElectionId()));
    }

    @Test
    public void testFindElectionsByIds() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election datasetAccessElection = createDataAccessElection(referenceId, datasetId);
        Election rpElection = createRPElection(referenceId, datasetId);

        List<Election> found = electionDAO.findElectionsByIds(List.of(datasetAccessElection.getElectionId(), rpElection.getElectionId()));

        Assertions.assertEquals(2, found.size());

        assertTrue(found.contains(datasetAccessElection));
        assertTrue(found.contains(rpElection));
    }

    @Test
    public void testFindElectionById() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e = createDataAccessElection(referenceId, datasetId);

        Election found = electionDAO.findElectionById(e.getElectionId());

        Assertions.assertEquals(e, found);
    }

    @Test
    public void testGetOpenElectionByReferenceId_NoElection() {

        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();

        Integer found = electionDAO.getOpenElectionIdByReferenceId(referenceId);

        Assertions.assertNull(found);
    }

    @Test
    public void testGetOpenElectionByReferenceId() {

        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e = createDataAccessElection(referenceId, datasetId);

        Integer found = electionDAO.getOpenElectionIdByReferenceId(referenceId);

        Assertions.assertEquals(e.getElectionId(), found);
    }

    @Test
    public void testArchiveElectionById() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e = createDataAccessElection(referenceId, datasetId);

        Assertions.assertEquals(false, e.getArchived());
        Assertions.assertNull(e.getLastUpdate());

        electionDAO.archiveElectionById(e.getElectionId(), new Date());
        e = electionDAO.findElectionById(e.getElectionId());

        Assertions.assertEquals(true, e.getArchived());
        Assertions.assertNotNull(e.getLastUpdate());
    }

    @Test
    public void testArchiveElectionByIds() {
        User user = createUser();
        String darCode = "DAR-1234567890";
        Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
        DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
        Dataset d1 = createDataset();
        dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDataSetId());
        Election accessElection = createDataAccessElection(dar.getReferenceId(), d1.getDataSetId());
        Election rpElection = createRPElection(dar.getReferenceId(), d1.getDataSetId());
        electionDAO.insertAccessRP(accessElection.getElectionId(), rpElection.getElectionId());
        List<Election> elections = electionDAO.findElectionsByReferenceIdAndDatasetId(dar.getReferenceId(), d1.getDataSetId());
        List<Integer> electionIds = elections.stream().map(Election::getElectionId).toList();

        electionDAO.archiveElectionByIds(electionIds, new Date());
        List<Election> archivedElections = electionDAO.findElectionsByIds(electionIds);
        archivedElections.forEach(e -> assertTrue(e.getArchived()));
    }

    @Test
    public void testFindDataAccessClosedElectionsByFinalResult_Success() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e = createDataAccessElection(referenceId, datasetId);
        Vote v = createFinalVote(user.getUserId(), e.getElectionId());
        voteDAO.updateVote(false,
                "",
                new Date(),
                v.getVoteId(),
                false,
                e.getElectionId(),
                new Date(),
                null);

        List<Election> found = electionDAO.findDataAccessClosedElectionsByFinalResult(false);
        Assertions.assertEquals(1, found.size());
        Assertions.assertEquals(e, found.get(0));
    }

    @Test
    public void testFindDataAccessClosedElectionsByFinalResult_None() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e = createDataAccessElection(referenceId, datasetId);
        Vote v = createFinalVote(user.getUserId(), e.getElectionId());
        voteDAO.updateVote(false,
                "",
                new Date(),
                v.getVoteId(),
                false,
                e.getElectionId(),
                new Date(),
                null);

        List<Election> found = electionDAO.findDataAccessClosedElectionsByFinalResult(true);
        Assertions.assertEquals(0, found.size());
    }

    @Test
    public void testFindDataAccessClosedElectionsByFinalResult_NotDataAccess() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e = createRPElection(referenceId, datasetId);
        Vote v = createFinalVote(user.getUserId(), e.getElectionId());
        voteDAO.updateVote(false,
                "",
                new Date(),
                v.getVoteId(),
                false,
                e.getElectionId(),
                new Date(),
                null);

        List<Election> found = electionDAO.findDataAccessClosedElectionsByFinalResult(false);
        Assertions.assertEquals(0, found.size());
    }

    @Test
    public void testFindApprovalAccessElectionDate() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e = createDataAccessElection(referenceId, datasetId);
        Vote v = createFinalVote(user.getUserId(), e.getElectionId());
        voteDAO.updateVote(true,
                "",
                new Date(),
                v.getVoteId(),
                false,
                e.getElectionId(),
                new Date(),
                null);
        electionDAO.updateElectionById(
                e.getElectionId(),
                ElectionStatus.FINAL.getValue(),
                new Date(),
                true);

        Assertions.assertEquals(e.getCreateDate(),
            electionDAO.findApprovalAccessElectionDate(referenceId));
    }

    @Test
    public void testFindApprovalAccessElectionDate_NotApproced() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());
        User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e = createDataAccessElection(referenceId, datasetId);
        Vote v = createFinalVote(user.getUserId(), e.getElectionId());

        Assertions.assertNull(electionDAO.findApprovalAccessElectionDate(referenceId));
    }

    @Test
    public void testFindDacForElection() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e = createDataAccessElection(referenceId, datasetId);

        Assertions.assertEquals(dac.getDacId(),
            electionDAO.findDacForElection(e.getElectionId()).getDacId());
    }

    @Test
    public void testFindOpenElectionsByDacId() {
        Dac dac = createDac();
        Dataset dataset = createDatasetWithDac(dac.getDacId());

        DataAccessRequest dar = createDataAccessRequestV3();
        String referenceId = dar.getReferenceId();
        Integer datasetId = dataset.getDataSetId();

        Election e1 = createDataAccessElection(referenceId, datasetId);
        Election e2 = createRPElection(referenceId, datasetId);
        Election e3 = createDataAccessElection(referenceId, datasetId);
        Election closed = createDataAccessElection(referenceId, datasetId);
        electionDAO.updateElectionById(
                closed.getElectionId(),
                ElectionStatus.CLOSED.getValue(),
                new Date()
        );

        List<Election> found = electionDAO.findOpenElectionsByDacId(dac.getDacId());

        Assertions.assertEquals(3, found.size());
        assertTrue(found.contains(e1));
        assertTrue(found.contains(e2));
        assertTrue(found.contains(e3));
    }


}
