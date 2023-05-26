package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.User;
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
    assertNotNull(consents);
    assertFalse(consents.isEmpty());
    assertEquals(2, consents.size());
    assertTrue(ids.contains(consent1.getConsentId()));
    assertTrue(ids.contains(consent2.getConsentId()));
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
    Consent consent = createConsent();
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
    assertNotNull(foundConsent.getName());
    assertEquals("something else", foundConsent.getName());
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
  public void testDeleteAllAssociationsForConsent() {
    Dataset dataset = createDataset();
    Dataset dataset2 = createDataset();
    Consent consent = createConsent();
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
    Consent consent = createConsent();
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        dataset.getDataSetId());
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        dataset2.getDataSetId());

    datasetDAO.deleteConsentAssociationsByDatasetId(dataset.getDataSetId());
    Integer deletedAssociationId = consentDAO.findAssociationsByDataSetId(dataset.getDataSetId());
    assertNull(deletedAssociationId);
    Integer remainingAssociationId = consentDAO.findAssociationsByDataSetId(
        dataset2.getDataSetId());
    assertNotNull(remainingAssociationId);
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

    assertFalse(consentDAO.checkManualReview(consent.getConsentId()));
    assertTrue(consentDAO.checkManualReview(consent2.getConsentId()));
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
    assertTrue(consent1Found.getUpdated());
    Consent consent2Found = consentDAO.findConsentById(consent2.getConsentId());
    assertFalse(consent2Found.getUpdated());
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, false,
        dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDataSetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

}
