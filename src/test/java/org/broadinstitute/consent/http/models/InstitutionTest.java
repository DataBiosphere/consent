package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitutionTest extends AbstractTestHelper {

  @Test
  void testMergeUpdatableFields_AllFields() {
    Institution existing = initExistingInstitution();
    Institution update = new Institution();
    Institution merged = update.mergeUpdatableFields(existing);

    assertEquals(existing.getId(), merged.getId());
    assertEquals(existing.getName(), merged.getName());
    assertEquals(existing.getItDirectorName(), merged.getItDirectorName());
    assertEquals(existing.getItDirectorEmail(), merged.getItDirectorEmail());
    assertEquals(existing.getInstitutionUrl(), merged.getInstitutionUrl());
    assertEquals(existing.getDunsNumber(), merged.getDunsNumber());
    assertEquals(existing.getOrgChartUrl(), merged.getOrgChartUrl());
    assertEquals(existing.getVerificationUrl(), merged.getVerificationUrl());
    assertEquals(existing.getVerificationFilename(), merged.getVerificationFilename());
    assertEquals(existing.getOrganizationType(), merged.getOrganizationType());
    assertEquals(existing.getDomains(), merged.getDomains());
  }

  @Test
  void testMergeUpdatableFields_ID() {
    Institution existing = initExistingInstitution();
    Institution update = new Institution();
    update.setId(2); // Change ID to simulate an update
    Institution merged = update.mergeUpdatableFields(existing);

    assertEquals(existing.getId(), merged.getId()); // ID should not change
    assertEquals(existing.getName(), merged.getName());
    assertEquals(existing.getItDirectorName(), merged.getItDirectorName());
    assertEquals(existing.getItDirectorEmail(), merged.getItDirectorEmail());
    assertEquals(existing.getInstitutionUrl(), merged.getInstitutionUrl());
    assertEquals(existing.getDunsNumber(), merged.getDunsNumber());
    assertEquals(existing.getOrgChartUrl(), merged.getOrgChartUrl());
    assertEquals(existing.getVerificationUrl(), merged.getVerificationUrl());
    assertEquals(existing.getVerificationFilename(), merged.getVerificationFilename());
    assertEquals(existing.getOrganizationType(), merged.getOrganizationType());
    assertEquals(existing.getDomains(), merged.getDomains());
  }

  @Test
  void testMergeUpdatableFields_Name() {
    Institution existing = initExistingInstitution();
    Institution update = new Institution();
    update.setName(randomAlphabetic(10));
    Institution merged = update.mergeUpdatableFields(existing);

    assertEquals(existing.getId(), merged.getId());
    assertNotEquals(existing.getName(), merged.getName());
    assertEquals(existing.getItDirectorName(), merged.getItDirectorName());
    assertEquals(existing.getItDirectorEmail(), merged.getItDirectorEmail());
    assertEquals(existing.getInstitutionUrl(), merged.getInstitutionUrl());
    assertEquals(existing.getDunsNumber(), merged.getDunsNumber());
    assertEquals(existing.getOrgChartUrl(), merged.getOrgChartUrl());
    assertEquals(existing.getVerificationUrl(), merged.getVerificationUrl());
    assertEquals(existing.getVerificationFilename(), merged.getVerificationFilename());
    assertEquals(existing.getOrganizationType(), merged.getOrganizationType());
    assertEquals(existing.getDomains(), merged.getDomains());
  }

  @Test
  void testMergeUpdatableFields_ITDirectorName() {
    Institution existing = initExistingInstitution();
    Institution update = new Institution();
    update.setItDirectorName(randomAlphabetic(10));
    Institution merged = update.mergeUpdatableFields(existing);

    assertEquals(existing.getId(), merged.getId());
    assertEquals(existing.getName(), merged.getName());
    assertNotEquals(existing.getItDirectorName(), merged.getItDirectorName());
    assertEquals(existing.getItDirectorEmail(), merged.getItDirectorEmail());
    assertEquals(existing.getInstitutionUrl(), merged.getInstitutionUrl());
    assertEquals(existing.getDunsNumber(), merged.getDunsNumber());
    assertEquals(existing.getOrgChartUrl(), merged.getOrgChartUrl());
    assertEquals(existing.getVerificationUrl(), merged.getVerificationUrl());
    assertEquals(existing.getVerificationFilename(), merged.getVerificationFilename());
    assertEquals(existing.getOrganizationType(), merged.getOrganizationType());
    assertEquals(existing.getDomains(), merged.getDomains());
  }

  @Test
  void testMergeUpdatableFields_ITDirectorEmail() {
    Institution existing = initExistingInstitution();
    Institution update = new Institution();
    update.setItDirectorEmail(randomAlphabetic(10));
    Institution merged = update.mergeUpdatableFields(existing);

    assertEquals(existing.getId(), merged.getId());
    assertEquals(existing.getName(), merged.getName());
    assertEquals(existing.getItDirectorName(), merged.getItDirectorName());
    assertNotEquals(existing.getItDirectorEmail(), merged.getItDirectorEmail());
    assertEquals(existing.getInstitutionUrl(), merged.getInstitutionUrl());
    assertEquals(existing.getDunsNumber(), merged.getDunsNumber());
    assertEquals(existing.getOrgChartUrl(), merged.getOrgChartUrl());
    assertEquals(existing.getVerificationUrl(), merged.getVerificationUrl());
    assertEquals(existing.getVerificationFilename(), merged.getVerificationFilename());
    assertEquals(existing.getOrganizationType(), merged.getOrganizationType());
    assertEquals(existing.getDomains(), merged.getDomains());
  }

  @Test
  void testMergeUpdatableFields_InstitutionURL() {
    Institution existing = initExistingInstitution();
    Institution update = new Institution();
    update.setInstitutionUrl(randomAlphabetic(10));
    Institution merged = update.mergeUpdatableFields(existing);

    assertEquals(existing.getId(), merged.getId());
    assertEquals(existing.getName(), merged.getName());
    assertEquals(existing.getItDirectorName(), merged.getItDirectorName());
    assertEquals(existing.getItDirectorEmail(), merged.getItDirectorEmail());
    assertNotEquals(existing.getInstitutionUrl(), merged.getInstitutionUrl());
    assertEquals(existing.getDunsNumber(), merged.getDunsNumber());
    assertEquals(existing.getOrgChartUrl(), merged.getOrgChartUrl());
    assertEquals(existing.getVerificationUrl(), merged.getVerificationUrl());
    assertEquals(existing.getVerificationFilename(), merged.getVerificationFilename());
    assertEquals(existing.getOrganizationType(), merged.getOrganizationType());
    assertEquals(existing.getDomains(), merged.getDomains());
  }

  @Test
  void testMergeUpdatableFields_DunsNumber() {
    Institution existing = initExistingInstitution();
    Institution update = new Institution();
    update.setDunsNumber(existing.getDunsNumber() + 1);
    Institution merged = update.mergeUpdatableFields(existing);

    assertEquals(existing.getId(), merged.getId());
    assertEquals(existing.getName(), merged.getName());
    assertEquals(existing.getItDirectorName(), merged.getItDirectorName());
    assertEquals(existing.getItDirectorEmail(), merged.getItDirectorEmail());
    assertEquals(existing.getInstitutionUrl(), merged.getInstitutionUrl());
    assertNotEquals(existing.getDunsNumber(), merged.getDunsNumber());
    assertEquals(existing.getOrgChartUrl(), merged.getOrgChartUrl());
    assertEquals(existing.getVerificationUrl(), merged.getVerificationUrl());
    assertEquals(existing.getVerificationFilename(), merged.getVerificationFilename());
    assertEquals(existing.getOrganizationType(), merged.getOrganizationType());
    assertEquals(existing.getDomains(), merged.getDomains());
  }

  @Test
  void testMergeUpdatableFields_OrgChartUrl() {
    Institution existing = initExistingInstitution();
    Institution update = new Institution();
    update.setOrgChartUrl(randomAlphabetic(10));
    Institution merged = update.mergeUpdatableFields(existing);

    assertEquals(existing.getId(), merged.getId());
    assertEquals(existing.getName(), merged.getName());
    assertEquals(existing.getItDirectorName(), merged.getItDirectorName());
    assertEquals(existing.getItDirectorEmail(), merged.getItDirectorEmail());
    assertEquals(existing.getInstitutionUrl(), merged.getInstitutionUrl());
    assertEquals(existing.getDunsNumber(), merged.getDunsNumber());
    assertNotEquals(existing.getOrgChartUrl(), merged.getOrgChartUrl());
    assertEquals(existing.getVerificationUrl(), merged.getVerificationUrl());
    assertEquals(existing.getVerificationFilename(), merged.getVerificationFilename());
    assertEquals(existing.getOrganizationType(), merged.getOrganizationType());
    assertEquals(existing.getDomains(), merged.getDomains());
  }

  @Test
  void testMergeUpdatableFields_VerificationUrl() {
    Institution existing = initExistingInstitution();
    Institution update = new Institution();
    update.setVerificationUrl(randomAlphabetic(10));
    Institution merged = update.mergeUpdatableFields(existing);

    assertEquals(existing.getId(), merged.getId());
    assertEquals(existing.getName(), merged.getName());
    assertEquals(existing.getItDirectorName(), merged.getItDirectorName());
    assertEquals(existing.getItDirectorEmail(), merged.getItDirectorEmail());
    assertEquals(existing.getInstitutionUrl(), merged.getInstitutionUrl());
    assertEquals(existing.getDunsNumber(), merged.getDunsNumber());
    assertEquals(existing.getOrgChartUrl(), merged.getOrgChartUrl());
    assertNotEquals(existing.getVerificationUrl(), merged.getVerificationUrl());
    assertEquals(existing.getVerificationFilename(), merged.getVerificationFilename());
    assertEquals(existing.getOrganizationType(), merged.getOrganizationType());
    assertEquals(existing.getDomains(), merged.getDomains());
  }

  @Test
  void testMergeUpdatableFields_VerificationFilename() {
    Institution existing = initExistingInstitution();
    Institution update = new Institution();
    update.setVerificationFilename(randomAlphabetic(10));
    Institution merged = update.mergeUpdatableFields(existing);

    assertEquals(existing.getId(), merged.getId());
    assertEquals(existing.getName(), merged.getName());
    assertEquals(existing.getItDirectorName(), merged.getItDirectorName());
    assertEquals(existing.getItDirectorEmail(), merged.getItDirectorEmail());
    assertEquals(existing.getInstitutionUrl(), merged.getInstitutionUrl());
    assertEquals(existing.getDunsNumber(), merged.getDunsNumber());
    assertEquals(existing.getOrgChartUrl(), merged.getOrgChartUrl());
    assertEquals(existing.getVerificationUrl(), merged.getVerificationUrl());
    assertNotEquals(existing.getVerificationFilename(), merged.getVerificationFilename());
    assertEquals(existing.getOrganizationType(), merged.getOrganizationType());
    assertEquals(existing.getDomains(), merged.getDomains());
  }

  @Test
  void testMergeUpdatableFields_OrganizationType() {
    Institution existing = initExistingInstitution();
    Institution update = new Institution();
    update.setOrganizationType(OrganizationType.NON_PROFIT);
    Institution merged = update.mergeUpdatableFields(existing);

    assertEquals(existing.getId(), merged.getId());
    assertEquals(existing.getName(), merged.getName());
    assertEquals(existing.getItDirectorName(), merged.getItDirectorName());
    assertEquals(existing.getItDirectorEmail(), merged.getItDirectorEmail());
    assertEquals(existing.getInstitutionUrl(), merged.getInstitutionUrl());
    assertEquals(existing.getDunsNumber(), merged.getDunsNumber());
    assertEquals(existing.getOrgChartUrl(), merged.getOrgChartUrl());
    assertEquals(existing.getVerificationUrl(), merged.getVerificationUrl());
    assertEquals(existing.getVerificationFilename(), merged.getVerificationFilename());
    assertNotEquals(existing.getOrganizationType(), merged.getOrganizationType());
    assertEquals(existing.getDomains(), merged.getDomains());
  }

  @Test
  void testMergeUpdatableFields_Domains() {
    Institution existing = initExistingInstitution();
    Institution update = new Institution();
    update.setDomains(List.of("newdomain.com"));
    Institution merged = update.mergeUpdatableFields(existing);

    assertEquals(existing.getId(), merged.getId());
    assertEquals(existing.getName(), merged.getName());
    assertEquals(existing.getItDirectorName(), merged.getItDirectorName());
    assertEquals(existing.getItDirectorEmail(), merged.getItDirectorEmail());
    assertEquals(existing.getInstitutionUrl(), merged.getInstitutionUrl());
    assertEquals(existing.getDunsNumber(), merged.getDunsNumber());
    assertEquals(existing.getOrgChartUrl(), merged.getOrgChartUrl());
    assertEquals(existing.getVerificationUrl(), merged.getVerificationUrl());
    assertEquals(existing.getVerificationFilename(), merged.getVerificationFilename());
    assertEquals(existing.getOrganizationType(), merged.getOrganizationType());
    assertNotEquals(existing.getDomains(), merged.getDomains());
    assertEquals(1, merged.getDomains().size());
  }

  private Institution initExistingInstitution() {
    Institution existing = new Institution();
    existing.setId(1);
    existing.setName("Test Institution");
    existing.setItDirectorName("John Doe");
    existing.setItDirectorEmail("email");
    existing.setInstitutionUrl("http://example.com");
    existing.setDunsNumber(123456789);
    existing.setOrgChartUrl("http://orgchart.com");
    existing.setVerificationUrl("http://verification.com");
    existing.setVerificationFilename("verification.pdf");
    existing.setOrganizationType(OrganizationType.FOR_PROFIT);
    existing.addDomain("example1.com");
    existing.addDomain("example2.com");
    return existing;
  }
}
