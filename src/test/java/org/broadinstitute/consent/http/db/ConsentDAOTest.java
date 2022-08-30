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
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.junit.Test;

public class ConsentDAOTest extends DAOTestHelper {

    @Test
    public void testFindConsentById() {
        // no-op ... tested in `createConsent()`
    }

    @Test
    public void testFindConsentFromDatasetID() {
        Dataset dataset = createDataset();
        Consent consent = createConsent();
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        Consent foundConsent = consentDAO.findConsentFromDatasetID(dataset.getDataSetId());
        assertNotNull(foundConsent);
    }

    @Test
    public void testFindConsentNameFromDatasetID() {
        Dataset dataset = createDataset();
        Consent consent = createConsent();
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        String name = consentDAO.findConsentNameFromDatasetID(dataset.getDataSetId());
        assertNotNull(name);
        assertEquals(consent.getName(), name);
    }

    @Test
    public void testFindConsentsFromConsentsIDs() {
        Consent consent1 = createConsent();
        Consent consent2 = createConsent();

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
    public void testFindConsentsFromConsentNames() {
        Consent consent1 = createConsent();
        Consent consent2 = createConsent();

        Collection<Consent> consents = consentDAO.findConsentsFromConsentNames(Arrays.asList(
                consent1.getName(),
                consent2.getName()));
        Collection<String> names = consents.stream().map(Consent::getName).collect(Collectors.toList());
        assertNotNull(consents);
        assertFalse(consents.isEmpty());
        assertEquals(2, consents.size());
        assertTrue(names.contains(consent1.getName()));
        assertTrue(names.contains(consent2.getName()));
    }

    @Test
    public void testCheckConsentById_case1() {
        Consent consent = createConsent();

        String consentId = consentDAO.checkConsentById(consent.getConsentId());
        assertEquals(consent.getConsentId(), consentId);
    }

    @Test
    public void testGetIdByName() {
        Consent consent = createConsent();

        String consentId = consentDAO.getIdByName(consent.getName());
        assertEquals(consent.getConsentId(), consentId);
    }

    @Test
    public void testFindConsentByName() {
        Consent consent = createConsent();

        Consent foundConsent = consentDAO.findConsentByName(consent.getName());
        assertEquals(consent.getConsentId(), foundConsent.getConsentId());
    }


    @Test
    public void testInsertConsent() {
        // no-op ... tested in `createConsent()`
    }

    @Test
    public void testDeleteConsent() {
        // no-op ... tested in `tearDown()`
    }

    @Test
    public void testLogicalDeleteConsent() {
        // no-op ... tested in `testCheckConsentById_case2()`
    }

    @Test
    public void testUpdateConsent() {
        Consent consent = createConsent();

        consentDAO.updateConsent(
                consent.getConsentId(),
                false,
                consent.getUseRestriction().toString(),
                consent.getDataUse().toString(),
                consent.getDataUseLetter(),
                "something else",
                consent.getDulName(),
                new Date(),
                new Date(),
                consent.getTranslatedUseRestriction(),
                consent.getGroupName(),
                consent.getUpdated()
        );

        Consent foundConsent = consentDAO.findConsentById(consent.getConsentId());
        assertNotNull(foundConsent.getName());
        assertEquals("something else", foundConsent.getName());
    }

    @Test
    public void testUpdateConsentTranslatedUseRestriction() {
        Consent consent = createConsent();

        String randomString = RandomStringUtils.random(10);
        consentDAO.updateConsentTranslatedUseRestriction(consent.getConsentId(), randomString);
        Consent foundConsent = consentDAO.findConsentById(consent.getConsentId());
        assertEquals(randomString, foundConsent.getTranslatedUseRestriction());
        assertNotEquals(consent.getTranslatedUseRestriction(), foundConsent.getTranslatedUseRestriction());
    }

    @Test
    public void testUpdateConsentSortDate() {
        Consent consent = createConsent();
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
        Consent consent = createConsent();
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
        Consent consent = createConsent();
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
        Consent consent = createConsent();
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset2.getDataSetId());

        consentDAO.deleteAllAssociationsForType(consent.getConsentId(), ASSOCIATION_TYPE_TEST);
        List<String> associationTypes = consentDAO.findAssociationTypesForConsent(consent.getConsentId());
        assertTrue(associationTypes.isEmpty());
    }

    @Test
    public void testDeleteAllAssociationsForConsent() {
        // no-op ... tested in `tearDown()`
    }

    @Test
    public void testDeleteAssociationsByDataSetId() {
        Dataset dataset = createDataset();
        Dataset dataset2 = createDataset();
        Consent consent = createConsent();
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
        Consent consent = createConsent();
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
        Consent consent = createConsent();

        List<Consent> consents = consentDAO.findUnreviewedConsents();
        List<String> consentIds = consents.stream().map(Consent::getConsentId).collect(Collectors.toList());
        assertTrue(consentIds.contains(consent.getConsentId()));
    }

    @Test
    public void testFindUnreviewedConsentsForDacs() {
        Dac dac1 = createDac();
        Dataset dataset1 = createDatasetWithDac(dac1.getDacId());
        Consent consent1 = createConsent();
        consentDAO.insertConsentAssociation(consent1.getConsentId(), ASSOCIATION_TYPE_TEST, dataset1.getDataSetId());

        Dac dac2 = createDac();
        Dataset dataset2 = createDatasetWithDac(dac2.getDacId());
        Consent consent2 = createConsent();
        consentDAO.insertConsentAssociation(consent2.getConsentId(), ASSOCIATION_TYPE_TEST, dataset2.getDataSetId());


        Dac dac3 = createDac();
        Dataset dataset3 = createDatasetWithDac(dac3.getDacId());
        Consent consent3 = createConsent();
        consentDAO.insertConsentAssociation(consent3.getConsentId(), ASSOCIATION_TYPE_TEST, dataset3.getDataSetId());

        createConsent();
        createConsent();

        List<Consent> consents = consentDAO.findUnreviewedConsentsForDacs(List.of(dac1.getDacId(), dac2.getDacId()));
        List<String> consentIds = consents.stream().map(c->c.getConsentId()).collect(Collectors.toList());


        assertEquals(2, consents.size());
        assertTrue(consentIds.contains(consent1.getConsentId()));
        assertTrue(consentIds.contains(consent2.getConsentId()));
    }

    @Test
    public void testFindUnreviewedConsentsForDacs_WithElection() {
        Dac dac1 = createDac();
        Dataset dataset1 = createDatasetWithDac(dac1.getDacId());
        Consent consent1 = createConsent();
        consentDAO.insertConsentAssociation(consent1.getConsentId(), ASSOCIATION_TYPE_TEST, dataset1.getDataSetId());
        createDataAccessElection(consent1.getConsentId(), dataset1.getDataSetId());

        List<Consent> consents = consentDAO.findUnreviewedConsentsForDacs(List.of(dac1.getDacId()));
        assertEquals(0, consents.size());
    }

    @Test
    public void testCheckManualReview() {
        Consent consent = createConsent();
        Consent consent2 = createConsent();
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
                consent2.getUpdated());

        assertFalse(consentDAO.checkManualReview(consent.getConsentId()));
        assertTrue(consentDAO.checkManualReview(consent2.getConsentId()));
    }

    @Test
    public void testGetAssociationConsentIdsFromDatasetIds() {
        Consent consent = createConsent();
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
    public void testUpdateConsentUpdateStatus() {
        Consent consent1 = createConsent();
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
                true);
        Consent consent2 = createConsent();
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
                false);

        Consent consent1Found = consentDAO.findConsentById(consent1.getConsentId());
        assertTrue(consent1Found.getUpdated());
        Consent consent2Found = consentDAO.findConsentById(consent2.getConsentId());
        assertFalse(consent2Found.getUpdated());
    }


}
