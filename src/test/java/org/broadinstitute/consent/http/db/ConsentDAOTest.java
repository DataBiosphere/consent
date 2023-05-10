package org.broadinstitute.consent.http.db;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConsentDAOTest extends DAOTestHelper {

    @Test
    public void testFindConsentById() {
        // no-op ... tested in `createConsent()`
    }

    @Test

    public void testFindConsentsFromConsentsIDs() {
        Consent consent1 = createConsent();
        Consent consent2 = createConsent();

        Collection<Consent> consents = consentDAO.findConsentsFromConsentsIDs(Arrays.asList(
                consent1.getConsentId(),
                consent2.getConsentId()));
        Collection<String> ids = consents.stream().map(Consent::getConsentId).toList();
        Assertions.assertNotNull(consents);
        Assertions.assertFalse(consents.isEmpty());
        Assertions.assertEquals(2, consents.size());
        Assertions.assertTrue(ids.contains(consent1.getConsentId()));
        Assertions.assertTrue(ids.contains(consent2.getConsentId()));
    }

    @Test
    public void testCheckConsentById_case1() {
        Consent consent = createConsent();

        String consentId = consentDAO.checkConsentById(consent.getConsentId());
        Assertions.assertEquals(consent.getConsentId(), consentId);
    }

    @Test
    public void testGetIdByName() {
        Consent consent = createConsent();

        String consentId = consentDAO.getIdByName(consent.getName());
        Assertions.assertEquals(consent.getConsentId(), consentId);
    }

    @Test
    public void testFindConsentByName() {
        Consent consent = createConsent();

        Consent foundConsent = consentDAO.findConsentByName(consent.getName());
        Assertions.assertEquals(consent.getConsentId(), foundConsent.getConsentId());
    }


    @Test
    public void testInsertConsent() {
        // no-op ... tested in `createConsent()`
    }

    @Test
    public void testDeleteConsent() {
        Consent consent = createConsent();
        String consentId = consent.getConsentId();
        Consent foundConsent = consentDAO.findConsentById(consentId);
        Assertions.assertNotNull(foundConsent);
        consentDAO.deleteConsent(consentId);
        Consent deletedConsent = consentDAO.findConsentById(consentId);
        Assertions.assertNull(deletedConsent);
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
        Assertions.assertNotNull(foundConsent.getName());
        Assertions.assertEquals("something else", foundConsent.getName());
    }

    @Test
    public void testUpdateConsentSortDate() {
        Consent consent = createConsent();
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date yesterday = cal.getTime();
        consentDAO.updateConsentSortDate(consent.getConsentId(), yesterday);
        Consent foundConsent = consentDAO.findConsentById(consent.getConsentId());
        Assertions.assertTrue(foundConsent.getSortDate().before(consent.getSortDate()));
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
    public void testDeleteAllAssociationsForConsent() {
        Dataset dataset = createDataset();
        Dataset dataset2 = createDataset();
        Consent consent = createConsent();
        String consentId = consent.getConsentId();
        consentDAO.insertConsentAssociation(consentId, ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        consentDAO.insertConsentAssociation(consentId, ASSOCIATION_TYPE_TEST, dataset2.getDataSetId());

        consentDAO.deleteAllAssociationsForConsent(consentId);
        Integer deletedAssociationId1 = consentDAO.findAssociationsByDataSetId(dataset.getDataSetId());
        Assertions.assertNull(deletedAssociationId1);
        Integer deletedAssociationId2 = consentDAO.findAssociationsByDataSetId(dataset2.getDataSetId());
        Assertions.assertNull(deletedAssociationId2);
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
        Assertions.assertNull(deletedAssociationId);
        Integer remainingAssociationId = consentDAO.findAssociationsByDataSetId(dataset2.getDataSetId());
        Assertions.assertNotNull(remainingAssociationId);
    }

    @Test
    public void testFindAssociationTypesForConsent() {
        // no-op ... tested in `testDeleteAllAssociationsForType()`
    }

    @Test
    public void testCheckManualReview() {
        Consent consent = createConsent();
        Consent consent2 = createConsent();
        consentDAO.updateConsent(
                consent2.getConsentId(),
                true,
                consent2.getDataUse().toString(),
                consent2.getDataUseLetter(),
                consent2.getName(),
                consent2.getDulName(),
                new Date(),
                consent2.getSortDate(),
                consent2.getTranslatedUseRestriction(),
                consent2.getGroupName(),
                consent2.getUpdated());

        Assertions.assertFalse(consentDAO.checkManualReview(consent.getConsentId()));
        Assertions.assertTrue(consentDAO.checkManualReview(consent2.getConsentId()));
    }

    @Test
    public void testFindInvalidRestrictions() {
        // no-op ... tested in `testUpdateConsentValidUseRestriction()`
    }

    @Test

    public void testConsentUpdateStatus() {
        Consent consent1 = createConsent();
        consentDAO.updateConsent(
                consent1.getConsentId(),
                consent1.getRequiresManualReview(),
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
        Assertions.assertTrue(consent1Found.getUpdated());
        Consent consent2Found = consentDAO.findConsentById(consent2.getConsentId());
        Assertions.assertFalse(consent2Found.getUpdated());
    }

}
