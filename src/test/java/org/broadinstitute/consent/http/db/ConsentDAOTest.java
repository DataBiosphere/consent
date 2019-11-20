package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentDataSet;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConsentDAOTest extends AbstractTest {

    // TODO: We have to track what we add because there are tests that rely on existing data.
    // Once those are replaced, these can be replaced with a delete all.
    private List<Integer> createdDataSetIds = new ArrayList<>();
    private List<Integer> createdDacIds = new ArrayList<>();
    private List<String> createdConsentIds = new ArrayList<>();
    private List<Integer> createdElectionIds = new ArrayList<>();

    private ConsentDAO consentDAO;
    private DacDAO dacDAO;
    private DataSetDAO dataSetDAO;
    private ElectionDAO electionDAO;

    private String ASSOCIATION_TYPE_TEST;

    @SuppressWarnings("UnstableApiUsage")
    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Before
    public void setUp() {
        consentDAO = getApplicationJdbi().onDemand(ConsentDAO.class);
        dacDAO = getApplicationJdbi().onDemand(DacDAO.class);
        dataSetDAO = getApplicationJdbi().onDemand(DataSetDAO.class);
        electionDAO = getApplicationJdbi().onDemand(ElectionDAO.class);
        ASSOCIATION_TYPE_TEST = RandomStringUtils.random(10, true, false);
    }

    @After
    public void tearDown() {
        // Order is important for FK constraints
        createdConsentIds.forEach(id -> {
            consentDAO.deleteAllAssociationsForConsent(id);
            consentDAO.deleteConsent(id);
        });
        createdElectionIds.forEach(id -> electionDAO.deleteElectionById(id));
        dataSetDAO.deleteDataSets(createdDataSetIds);
        createdDacIds.forEach(id -> dacDAO.deleteDac(id));
    }

    @Test
    public void testFindConsentById() {
        // no-op ... tested in `createConsent()`
    }

    @Test
    public void testFindConsentFromDatasetID() {
        DataSet dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Consent foundConsent = consentDAO.findConsentFromDatasetID(dataset.getDataSetId());
        Assert.assertNotNull(foundConsent);
    }

    @Test
    public void testFindConsentNameFromDatasetID() {
        DataSet dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        String name = consentDAO.findConsentNameFromDatasetID(dataset.getDataSetId().toString());
        Assert.assertNotNull(name);
        Assert.assertEquals(consent.getName(), name);
    }

    @Test
    public void testFindConsentsFromConsentsIDs() {
        Consent consent1 = createConsent(null);
        Consent consent2 = createConsent(null);

        Collection<Consent> consents = consentDAO.findConsentsFromConsentsIDs(Arrays.asList(
                consent1.getConsentId(),
                consent2.getConsentId()));
        Collection<String> ids = consents.stream().map(Consent::getConsentId).collect(Collectors.toList());
        Assert.assertNotNull(consents);
        Assert.assertFalse(consents.isEmpty());
        Assert.assertEquals(2, consents.size());
        Assert.assertTrue(ids.contains(consent1.getConsentId()));
        Assert.assertTrue(ids.contains(consent2.getConsentId()));
    }

    @Test
    public void testFindConsentsFromConsentNames() {
        Consent consent1 = createConsent(null);
        Consent consent2 = createConsent(null);

        Collection<Consent> consents = consentDAO.findConsentsFromConsentNames(Arrays.asList(
                consent1.getName(),
                consent2.getName()));
        Collection<String> names = consents.stream().map(Consent::getName).collect(Collectors.toList());
        Assert.assertNotNull(consents);
        Assert.assertFalse(consents.isEmpty());
        Assert.assertEquals(2, consents.size());
        Assert.assertTrue(names.contains(consent1.getName()));
        Assert.assertTrue(names.contains(consent2.getName()));
    }

    @Test
    public void testGetConsentIdAndDataSets() {
        DataSet dataset1 = createDataset();
        Consent consent1 = createConsent(null);
        createAssociation(consent1.getConsentId(), dataset1.getDataSetId());

        DataSet dataset2 = createDataset();
        Consent consent2 = createConsent(null);
        createAssociation(consent2.getConsentId(), dataset2.getDataSetId());

        Set<ConsentDataSet> consentDataSets = consentDAO.getConsentIdAndDataSets(Arrays.asList(
                dataset1.getDataSetId(), dataset2.getDataSetId()));
        Collection<String> consentIds = consentDataSets.stream().map(ConsentDataSet::getConsentId).collect(Collectors.toList());

        Assert.assertNotNull(consentDataSets);
        Assert.assertFalse(consentDataSets.isEmpty());
        Assert.assertEquals(2, consentDataSets.size());
        consentDataSets.forEach(c -> Assert.assertTrue(consentIds.contains(c.getConsentId())));
    }

    @Test
    public void testCheckConsentById_case1() {
        Consent consent = createConsent(null);

        String consentId = consentDAO.checkConsentById(consent.getConsentId());
        Assert.assertEquals(consent.getConsentId(), consentId);
    }

    @Test
    public void testCheckConsentById_case2() {
        Consent consent = createConsent(null);
        consentDAO.logicalDeleteConsent(consent.getConsentId());

        String consentId = consentDAO.checkConsentById(consent.getConsentId());
        Assert.assertNull(consentId);
    }

    @Test
    public void testGetIdByName() {
        Consent consent = createConsent(null);

        String consentId = consentDAO.getIdByName(consent.getName());
        Assert.assertEquals(consent.getConsentId(), consentId);
    }

    @Test
    public void testFindConsentByName() {
        Consent consent = createConsent(null);

        Consent foundConsent = consentDAO.findConsentByName(consent.getName());
        Assert.assertEquals(consent.getConsentId(), foundConsent.getConsentId());
    }

    @Test
    public void testFindConsentsByAssociationType() {
        DataSet dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Collection<Consent> foundConsents = consentDAO.findConsentsByAssociationType(ASSOCIATION_TYPE_TEST);

        Assert.assertNotNull(foundConsents);
        Assert.assertFalse(foundConsents.isEmpty());
        Assert.assertEquals(1, foundConsents.size());

        Optional<Consent> foundConsent = foundConsents.stream().findFirst();
        Assert.assertTrue(foundConsent.isPresent());
        Assert.assertEquals(consent.getConsentId(), foundConsent.get().getConsentId());
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
        Assert.assertNotNull(foundConsent.getDacId());
        Assert.assertEquals(dac.getDacId(), foundConsent.getDacId());
    }

    @Test
    public void testUpdateConsentSortDate() {
        Consent consent = createConsent(null);

        consentDAO.updateConsentSortDate(consent.getConsentId(), yesterday());
        Consent foundConsent = consentDAO.findConsentById(consent.getConsentId());
        Assert.assertTrue(foundConsent.getSortDate().before(consent.getSortDate()));
    }

    @Test
    public void testBulkUpdateConsentSortDate() {
        Consent consent = createConsent(null);

        consentDAO.bulkUpdateConsentSortDate(Collections.singletonList(consent.getConsentId()), new Date(), yesterday());
        Consent foundConsent = consentDAO.findConsentById(consent.getConsentId());
        Assert.assertTrue(foundConsent.getSortDate().before(consent.getSortDate()));
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
        DataSet dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        List<String> associations = consentDAO.findAssociationsByType(consent.getConsentId(), ASSOCIATION_TYPE_TEST);
        Assert.assertNotNull(associations);
        Assert.assertFalse(associations.isEmpty());
        String objectId = associations.get(0);

        String association = consentDAO.findAssociationByTypeAndId(consent.getConsentId(), ASSOCIATION_TYPE_TEST, objectId);
        Assert.assertNotNull(association);
        Assert.assertEquals(objectId, association);
    }

    @Test
    public void testDeleteOneAssociation() {
        DataSet dataset = createDataset();
        DataSet dataset2 = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        createAssociation(consent.getConsentId(), dataset2.getDataSetId());

        consentDAO.deleteOneAssociation(
                consent.getConsentId(),
                ASSOCIATION_TYPE_TEST,
                dataset.getDataSetId());
        Integer deletedAssociationId = consentDAO.findAssociationsByDataSetId(dataset.getDataSetId());
        Assert.assertNull(deletedAssociationId);
        Integer remainingAssociationId = consentDAO.findAssociationsByDataSetId(dataset2.getDataSetId());
        Assert.assertNotNull(remainingAssociationId);
    }

    @Test
    public void testDeleteAllAssociationsForType() {
        DataSet dataset = createDataset();
        DataSet dataset2 = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        createAssociation(consent.getConsentId(), dataset2.getDataSetId());

        consentDAO.deleteAllAssociationsForType(consent.getConsentId(), ASSOCIATION_TYPE_TEST);
        List<String> associationTypes = consentDAO.findAssociationTypesForConsent(consent.getConsentId());
        Assert.assertTrue(associationTypes.isEmpty());
    }

    @Test
    public void testDeleteAllAssociationsForConsent() {
        // no-op ... tested in `tearDown()`
    }

    @Test
    public void testDeleteAssociationsByDataSetId() {
        DataSet dataset = createDataset();
        DataSet dataset2 = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        createAssociation(consent.getConsentId(), dataset2.getDataSetId());

        consentDAO.deleteAssociationsByDataSetId(dataset.getDataSetId());
        Integer deletedAssociationId = consentDAO.findAssociationsByDataSetId(dataset.getDataSetId());
        Assert.assertNull(deletedAssociationId);
        Integer remainingAssociationId = consentDAO.findAssociationsByDataSetId(dataset2.getDataSetId());
        Assert.assertNotNull(remainingAssociationId);
    }

    @Test
    public void testFindAssociationTypesForConsent() {
        // no-op ... tested in `testDeleteAllAssociationsForType()`
    }

    @Test
    public void testFindConsentsForAssociation() {
        DataSet dataset = createDataset();
        DataSet dataset2 = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        createAssociation(consent.getConsentId(), dataset2.getDataSetId());

        List<String> associationTypes = consentDAO.findAssociationTypesForConsent(consent.getConsentId());
        Assert.assertNotNull(associationTypes);
        Assert.assertFalse(associationTypes.isEmpty());
        Assert.assertEquals(1, associationTypes.size());
        Assert.assertEquals(ASSOCIATION_TYPE_TEST, associationTypes.get(0));
    }

    @Test
    public void testFindConsentByAssociationAndObjectId() {
        DataSet dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        List<String> associations = consentDAO.findAssociationsByType(consent.getConsentId(), ASSOCIATION_TYPE_TEST);
        String objectId = associations.get(0);

        Consent foundConsent = consentDAO.findConsentByAssociationAndObjectId(ASSOCIATION_TYPE_TEST, objectId);
        Assert.assertNotNull(foundConsent);
        Assert.assertEquals(consent.getConsentId(), foundConsent.getConsentId());
    }

    @Test
    public void testFindUnreviewedConsents() {
        Consent consent = createConsent(null);

        List<Consent> consents = consentDAO.findUnreviewedConsents();
        List<String> consentIds = consents.stream().map(Consent::getConsentId).collect(Collectors.toList());
        Assert.assertTrue(consentIds.contains(consent.getConsentId()));
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

        Assert.assertFalse(consentDAO.checkManualReview(consent.getConsentId()));
        Assert.assertTrue(consentDAO.checkManualReview(consent2.getConsentId()));
    }

    @Test
    public void testFindConsentManageByStatus() {
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());

        List<ConsentManage> consentManages = consentDAO.findConsentManageByStatus(election.getStatus());
        List<String> consentIds = consentManages.stream().map(ConsentManage::getConsentId).collect(Collectors.toList());
        Assert.assertTrue(consentIds.contains(consent.getConsentId()));
    }

    @Test
    public void testGetAssociationConsentIdsFromDatasetIds() {
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        List<String> dataSetIds = Stream.of(String.valueOf(dataset.getDataSetId())).collect(Collectors.toList());

        List<String> consentIds = consentDAO.getAssociationConsentIdsFromDatasetIds(dataSetIds);
        Assert.assertFalse(consentIds.isEmpty());
        Assert.assertTrue(consentIds.contains(consent.getConsentId()));
    }

    @Test
    public void testFindInvalidRestrictions() {
        // no-op ... tested in `testUpdateConsentValidUseRestriction()`
    }

    @Test
    public void testFindConsentUseRestrictions() {
        Consent consent = createConsent(null);

        List<UseRestrictionDTO> useRestrictions = consentDAO.findConsentUseRestrictions();
        Assert.assertFalse(useRestrictions.isEmpty());
    }

    @Test
    public void testUpdateConsentValidUseRestriction() {
        // TODO
        // updateConsentValidUseRestriction
    }

    @Test
    public void testUpdateConsentUpdateStatus() {
        // TODO
        // updateConsentUpdateStatus
    }

    @Test
    public void testUpdateConsentGroupName() {
        // TODO
        // updateConsentGroupName
    }

    @Test
    public void testUpdateConsentDac() {
        // TODO
        // updateConsentDac
    }

    private void createAssociation(String consentId, Integer datasetId) {
        consentDAO.insertConsentAssociation(consentId, ASSOCIATION_TYPE_TEST, datasetId);
    }

    private Election createElection(String referenceId, Integer datasetId) {
        Integer electionId = electionDAO.insertElection(
                ElectionType.DATA_ACCESS.getValue(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                referenceId,
                datasetId
        );
        createdElectionIds.add(electionId);
        return electionDAO.findElectionById(electionId);
    }

    @SuppressWarnings("SameParameterValue")
    private Consent createConsent(Integer dacId) {
        String consentId = UUID.randomUUID().toString();
        consentDAO.insertConsent(consentId,
                false,
                "{\"type\":\"everything\"}",
                "{\"generalUse\": true }",
                "dul",
                consentId,
                "dulName",
                new Date(),
                new Date(),
                "Everything",
                true,
                "Group",
                dacId);
        createdConsentIds.add(consentId);
        return consentDAO.findConsentById(consentId);
    }

    private Dac createDac() {
        Integer id = dacDAO.createDac(
                "Test_" + RandomStringUtils.random(20, true, true),
                "Test_" + RandomStringUtils.random(20, true, true),
                new Date());
        createdDacIds.add(id);
        return dacDAO.findById(id);
    }

    private DataSet createDataset() {
        DataSet ds = new DataSet();
        ds.setName("Name_" + RandomStringUtils.random(20, true, true));
        ds.setCreateDate(new Date());
        ds.setObjectId("Object ID_" + RandomStringUtils.random(20, true, true));
        ds.setActive(true);
        ds.setAlias(RandomUtils.nextInt(1, 1000));
        Integer id = dataSetDAO.insertDataset(ds.getName(), ds.getCreateDate(), ds.getObjectId(), ds.getActive(), ds.getAlias());
        createdDataSetIds.add(id);
        return dataSetDAO.findDataSetById(id);
    }

    private Date yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

}
