package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.junit.Test;

public class ConsentDAOTest extends DAOTestHelper {

    @Test
    public void testFindConsentById() {
        // no-op ... tested in `createConsent()`
    }

    @Test
    public void testFindConsentFromDatasetID() {
        Dataset dataset = createDataset();
        Consent consent = createConsent(null);
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        Consent foundConsent = consentDAO.findConsentFromDatasetID(dataset.getDataSetId());
        assertNotNull(foundConsent);
    }

    @Test
    public void testFindConsentsFromConsentsIDs() {
        Consent consent1 = createConsent(null);
        Consent consent2 = createConsent(null);

        Collection<Consent> consents = consentDAO.findConsentsFromConsentsIDs(Arrays.asList(
                consent1.getConsentId(),
                consent2.getConsentId()));
        Collection<String> ids = consents.stream().map(Consent::getConsentId).collect(Collectors.toList());
        assertNotNull(consents);
        assertFalse(consents.isEmpty());
        assertEquals(2, consents.size());
        assertTrue(ids.contains(consent1.getConsentId()));
        assertTrue(ids.contains(consent2.getConsentId()));
    }

    @Test
    public void testCheckConsentById_case1() {
        Consent consent = createConsent(null);

        String consentId = consentDAO.checkConsentById(consent.getConsentId());
        assertEquals(consent.getConsentId(), consentId);
    }

    @Test
    public void testGetIdByName() {
        Consent consent = createConsent(null);

        String consentId = consentDAO.getIdByName(consent.getName());
        assertEquals(consent.getConsentId(), consentId);
    }

    @Test
    public void testFindConsentByName() {
        Consent consent = createConsent(null);

        Consent foundConsent = consentDAO.findConsentByName(consent.getName());
        assertEquals(consent.getConsentId(), foundConsent.getConsentId());
    }


    @Test
    public void testInsertConsent() {
        // no-op ... tested in `createConsent()`
    }

    @Test
    public void testDeleteConsent() {
        Consent consent = createConsent(null);
        String consentId = consent.getConsentId();
        Consent foundConsent = consentDAO.findConsentById(consentId);
        assertNotNull(foundConsent);
        consentDAO.deleteConsent(consentId);
        Consent deletedConsent = consentDAO.findConsentById(consentId);
        assertNull(deletedConsent);
    }

    @Test
    public void testLogicalDeleteConsent() {
        // no-op ... tested in `testCheckConsentById_case2()`
    }

    @Test
    public void testUpdateConsent() {
        Consent consent = createConsent(null);
        Dac dac = createDac();

        consentDAO.updateConsent(
                consent.getConsentId(),
                false,
                consent.getUseRestriction().toString(),
                consent.getDataUse().toString(),
                consent.getDataUseLetter(),
                consent.getName(),
                consent.getDulName(),
                new Date(),
                new Date(),
                consent.getTranslatedUseRestriction(),
                consent.getGroupName(),
                consent.getUpdated(),
                dac.getDacId()
        );

        Consent foundConsent = consentDAO.findConsentById(consent.getConsentId());
        assertNotNull(foundConsent.getDacId());
        assertEquals(dac.getDacId(), foundConsent.getDacId());
    }

    @Test
    public void testUpdateConsentTranslatedUseRestriction() {
        Consent consent = createConsent(null);

        String randomString = RandomStringUtils.random(10);
        consentDAO.updateConsentTranslatedUseRestriction(consent.getConsentId(), randomString);
        Consent foundConsent = consentDAO.findConsentById(consent.getConsentId());
        assertEquals(randomString, foundConsent.getTranslatedUseRestriction());
        assertNotEquals(consent.getTranslatedUseRestriction(), foundConsent.getTranslatedUseRestriction());
    }

    @Test
    public void testUpdateConsentSortDate() {
        Consent consent = createConsent(null);
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date yesterday = cal.getTime();
        consentDAO.updateConsentSortDate(consent.getConsentId(), yesterday);
        Consent foundConsent = consentDAO.findConsentById(consent.getConsentId());
        assertTrue(foundConsent.getSortDate().before(consent.getSortDate()));
    }

    @Test
    public void testInsertConsentAssociation() {
        // no-op ... tested in `createAssociation()`
    }

    @Test
    public void testFindAssociationsByType() {
        // no-op ... tested in `testFindAssociationByTypeAndId()`
    }

    @Test
    public void testFindAssociationsByDataSetId() {
        // no-op ... tested in `testDeleteOneAssociation()`
    }

    @Test
    public void testFindAssociationByTypeAndId() {
        Dataset dataset = createDataset();
        Consent consent = createConsent(null);
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        List<String> associations = consentDAO.findAssociationsByType(consent.getConsentId(), ASSOCIATION_TYPE_TEST);
        assertNotNull(associations);
        assertFalse(associations.isEmpty());
        String objectId = associations.get(0);

        String association = consentDAO.findAssociationByTypeAndId(consent.getConsentId(), ASSOCIATION_TYPE_TEST, objectId);
        assertNotNull(association);
        assertEquals(objectId, association);
    }

    @Test
    public void testDeleteOneAssociation() {
        Dataset dataset = createDataset();
        Dataset dataset2 = createDataset();
        Consent consent = createConsent(null);
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset2.getDataSetId());

        consentDAO.deleteOneAssociation(
                consent.getConsentId(),
                ASSOCIATION_TYPE_TEST,
                dataset.getDataSetId());
        Integer deletedAssociationId = consentDAO.findAssociationsByDataSetId(dataset.getDataSetId());
        assertNull(deletedAssociationId);
        Integer remainingAssociationId = consentDAO.findAssociationsByDataSetId(dataset2.getDataSetId());
        assertNotNull(remainingAssociationId);
    }

    @Test
    public void testDeleteAllAssociationsForType() {
        Dataset dataset = createDataset();
        Dataset dataset2 = createDataset();
        Consent consent = createConsent(null);
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset2.getDataSetId());

        consentDAO.deleteAllAssociationsForType(consent.getConsentId(), ASSOCIATION_TYPE_TEST);
        List<String> associationTypes = consentDAO.findAssociationTypesForConsent(consent.getConsentId());
        assertTrue(associationTypes.isEmpty());
    }

    @Test
    public void testDeleteAllAssociationsForConsent() {
        Dataset dataset = createDataset();
        Dataset dataset2 = createDataset();
        Consent consent = createConsent(null);
        String consentId = consent.getConsentId();
        consentDAO.insertConsentAssociation(consentId, ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        consentDAO.insertConsentAssociation(consentId, ASSOCIATION_TYPE_TEST, dataset2.getDataSetId());

        consentDAO.deleteAllAssociationsForConsent(consentId);
        Integer deletedAssociationId1 = consentDAO.findAssociationsByDataSetId(dataset.getDataSetId());
        assertNull(deletedAssociationId1);
        Integer deletedAssociationId2 = consentDAO.findAssociationsByDataSetId(dataset2.getDataSetId());
        assertNull(deletedAssociationId2);
    }

    @Test
    public void testDeleteAssociationsByDataSetId() {
        Dataset dataset = createDataset();
        Dataset dataset2 = createDataset();
        Consent consent = createConsent(null);
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset2.getDataSetId());

        datasetDAO.deleteConsentAssociationsByDatasetId(dataset.getDataSetId());
        Integer deletedAssociationId = consentDAO.findAssociationsByDataSetId(dataset.getDataSetId());
        assertNull(deletedAssociationId);
        Integer remainingAssociationId = consentDAO.findAssociationsByDataSetId(dataset2.getDataSetId());
        assertNotNull(remainingAssociationId);
    }

    @Test
    public void testFindAssociationTypesForConsent() {
        // no-op ... tested in `testDeleteAllAssociationsForType()`
    }

    @Test
    public void testFindConsentsForAssociation() {
        Dataset dataset = createDataset();
        Dataset dataset2 = createDataset();
        Consent consent = createConsent(null);
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset2.getDataSetId());

        List<String> associationTypes = consentDAO.findAssociationTypesForConsent(consent.getConsentId());
        assertNotNull(associationTypes);
        assertFalse(associationTypes.isEmpty());
        assertEquals(1, associationTypes.size());
        assertEquals(ASSOCIATION_TYPE_TEST, associationTypes.get(0));
    }

    @Test
    public void testFindUnreviewedConsents() {
        Consent consent = createConsent(null);

        List<Consent> consents = consentDAO.findUnreviewedConsents();
        List<String> consentIds = consents.stream().map(Consent::getConsentId).collect(Collectors.toList());
        assertTrue(consentIds.contains(consent.getConsentId()));
    }

    @Test
    public void testCheckManualReview() {
        Consent consent = createConsent(null);
        Consent consent2 = createConsent(null);
        consentDAO.updateConsent(
                consent2.getConsentId(),
                true,
                consent2.getUseRestriction().toString(),
                consent2.getDataUse().toString(),
                consent2.getDataUseLetter(),
                consent2.getName(),
                consent2.getDulName(),
                new Date(),
                consent2.getSortDate(),
                consent2.getTranslatedUseRestriction(),
                consent2.getGroupName(),
                consent2.getUpdated(),
                consent2.getDacId());

        assertFalse(consentDAO.checkManualReview(consent.getConsentId()));
        assertTrue(consentDAO.checkManualReview(consent2.getConsentId()));
    }

    @Test
    public void testFindConsentManageByStatus() {
        Consent consent = createConsent(null);
        Dataset dataset = createDataset();
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        Election election = createDataAccessElection(consent.getConsentId(), dataset.getDataSetId());

        List<ConsentManage> consentManages = consentDAO.findConsentManageByStatus(election.getStatus());
        List<String> consentIds = consentManages.stream().map(ConsentManage::getConsentId).collect(Collectors.toList());
        assertTrue(consentIds.contains(consent.getConsentId()));
    }

    @Test
    public void testGetAssociationConsentIdsFromDatasetIds() {
        Consent consent = createConsent(null);
        Dataset dataset = createDataset();
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        List<Integer> dataSetIds = Stream.of(dataset.getDataSetId()).collect(Collectors.toList());

        List<String> consentIds = consentDAO.getAssociationConsentIdsFromDatasetIds(dataSetIds);
        assertFalse(consentIds.isEmpty());
        assertTrue(consentIds.contains(consent.getConsentId()));
    }

    @Test
    public void testFindInvalidRestrictions() {
        // no-op ... tested in `testUpdateConsentValidUseRestriction()`
    }

    @Test
    public void testConsentUpdateStatus() {
        Consent consent1 = createConsent(null);
        consentDAO.updateConsent(
                consent1.getConsentId(),
                consent1.getRequiresManualReview(),
                consent1.getUseRestriction().toString(),
                consent1.getDataUse().toString(),
                consent1.getDataUseLetter(),
                consent1.getName(),
                consent1.getDulName(),
                new Date(),
                consent1.getSortDate(),
                consent1.getTranslatedUseRestriction(),
                consent1.getGroupName(),
                true,
                consent1.getDacId());
        Consent consent2 = createConsent(null);
        consentDAO.updateConsent(
                consent2.getConsentId(),
                consent2.getRequiresManualReview(),
                consent2.getUseRestriction().toString(),
                consent2.getDataUse().toString(),
                consent2.getDataUseLetter(),
                consent2.getName(),
                consent2.getDulName(),
                new Date(),
                consent2.getSortDate(),
                consent2.getTranslatedUseRestriction(),
                consent2.getGroupName(),
                false,
                consent2.getDacId());

        Consent consent1Found = consentDAO.findConsentById(consent1.getConsentId());
        assertTrue(consent1Found.getUpdated());
        Consent consent2Found = consentDAO.findConsentById(consent2.getConsentId());
        assertFalse(consent2Found.getUpdated());
    }

    @Test
    public void testUpdateConsentUpdateStatus() {
        Consent consent = createConsent(null);
        consentDAO.updateConsentUpdateStatus(consent.getConsentId(), true);
        Consent consentFound = consentDAO.findConsentById(consent.getConsentId());
        assertTrue(consentFound.getUpdated());
    }

    @Test
    public void testUpdateConsentDac() {
        Consent consent = createConsent(null);
        Dac dac = createDac();

        consentDAO.updateConsentDac(consent.getConsentId(), dac.getDacId());
        Consent foundConsent = consentDAO.findConsentById(consent.getConsentId());
        assertEquals(dac.getDacId(), foundConsent.getDacId());
    }

}
