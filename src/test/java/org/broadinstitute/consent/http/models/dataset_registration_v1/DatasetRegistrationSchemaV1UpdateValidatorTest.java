package org.broadinstitute.consent.http.models.dataset_registration_v1;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.service.DatasetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetRegistrationSchemaV1UpdateValidatorTest {

  @Mock
  private DatasetService datasetService;
  private DatasetRegistrationSchemaV1UpdateValidator validator;

  @BeforeEach
  void setUp() {
    validator = new DatasetRegistrationSchemaV1UpdateValidator(datasetService);
  }

  @Test
  void testValidation_valid() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);

    boolean valid = validator.validate(study, registration);
    assertTrue(valid);
  }

  @Test
  void testValidation_valid_study_name_change() {
    Study study = createMockStudy();
    when(datasetService.findAllStudyNames()).thenReturn(Set.of(study.getName()));
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setStudyName("New Name");

    boolean valid = validator.validate(study, registration);
    assertTrue(valid);
  }

  @Test
  void testValidation_invalid_study_name_change() {
    String existingStudyName = RandomStringUtils.randomAlphabetic(10);
    Study study = createMockStudy();
    when(datasetService.findAllStudyNames()).thenReturn(Set.of(study.getName(), existingStudyName));
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setStudyName(existingStudyName);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_empty_consent_groups() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setConsentGroups(List.of());

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_invalid_data_use_changes() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.getConsentGroups().get(0).setGeneralResearchUse(true);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_non_study_dataset() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    // mock data is limited to 10->100
    registration.getConsentGroups().get(0).setDatasetId(10000);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_consent_group_name_change_allowed() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    study.getDatasets().forEach(d -> { d.setName("");});

    boolean valid = validator.validate(study, registration);
    assertTrue(valid);
  }

  @Test
  void testValidation_consent_group_name_change_not_allowed() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    study.getDatasets().forEach(d -> { d.setName("Existing Name");});

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_consent_group_access_management_not_allowed() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.getConsentGroups().forEach(cg -> {
      cg.setAccessManagement(AccessManagement.CONTROLLED);
    });

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_consent_group_data_location_required() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.getConsentGroups().forEach(cg -> {
      cg.setDataLocation(null);
    });

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_invalid_delete_consent_groups() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    Dataset dataset = new Dataset();
    dataset.setName(RandomStringUtils.randomAlphabetic(10));
    // mock data is limited to 10->100
    dataset.setDataSetId(10000);
    dataset.setDacId(RandomUtils.nextInt(10, 100));
    study.getDatasets().add(dataset);
    ArrayList<Integer> datasetIds = new ArrayList<>(study.getDatasetIds().stream().toList());
    datasetIds.add(dataset.getDataSetId());
    study.setDatasetIds(new HashSet<>(datasetIds));

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_study_description() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setStudyDescription(null);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_data_types() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setDataTypes(List.of());

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_public_visibility() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setPublicVisibility(null);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_nih_anvil_use() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setNihAnvilUse(null);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_dbgap_phsid() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(null);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_dbgap_pi_institution() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.randomAlphabetic(10));
    registration.setPiInstitution(null);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_nih_grant_contract_number() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.randomAlphabetic(10));
    registration.setPiInstitution(RandomUtils.nextInt(10, 100));
    registration.setNihGrantContractNumber(null);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_pi_institution() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL);
    registration.setPiInstitution(null);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_pi_institution_nih_grant_contract_number() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_BUT_I_AM_SEEKING_TO_SUBMIT_DATA_TO_AN_VIL);
    registration.setPiInstitution(RandomUtils.nextInt(10, 100));
    registration.setNihGrantContractNumber(null);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_phenotype_indication() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setPhenotypeIndication(null);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_pi_name() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setPiName(null);

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }

  @Test
  void testValidation_data_custodian_email() {
    Study study = createMockStudy();
    DatasetRegistrationSchemaV1 registration = createMockRegistration(study);
    registration.setDataCustodianEmail(List.of());

    assertThrows(BadRequestException.class, () -> {
      validator.validate(study, registration);
    });
  }



  private Study createMockStudy() {
    Study study = new Study();
    study.setName(RandomStringUtils.randomAlphabetic(10));
    Dataset dataset = new Dataset();
    dataset.setName("");
    dataset.setDataSetId(RandomUtils.nextInt(10, 100));
    dataset.setDacId(RandomUtils.nextInt(10, 100));
    study.addDatasets(List.of(dataset));
    study.setDatasetIds(Set.of(dataset.getDataSetId()));
    return study;
  }

  private DatasetRegistrationSchemaV1 createMockRegistration(Study study) {
    DatasetRegistrationSchemaV1 registration = new DatasetRegistrationSchemaV1();
    registration.setStudyName(study.getName());
    registration.setStudyDescription(RandomStringUtils.randomAlphabetic(10));
    registration.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(10)));
    registration.setPublicVisibility(true);
    registration.setNihAnvilUse(NihAnvilUse.I_AM_NHGRI_FUNDED_AND_I_HAVE_A_DB_GA_P_PHS_ID_ALREADY);
    registration.setDbGaPPhsID(RandomStringUtils.randomAlphabetic(10));
    registration.setPiInstitution(RandomUtils.nextInt(1, 100));
    registration.setNihGrantContractNumber(RandomStringUtils.randomAlphabetic(10));
    registration.setPhenotypeIndication(RandomStringUtils.randomAlphabetic(10));
    registration.setPiName(RandomStringUtils.randomAlphabetic(10));
    registration.setDataCustodianEmail(List.of(RandomStringUtils.randomAlphabetic(10)));
    List<ConsentGroup> cgs = study.getDatasets().stream().map(d -> {
      ConsentGroup cg = new ConsentGroup();
      cg.setDataLocation(DataLocation.NOT_DETERMINED);
      cg.setNumberOfParticipants(RandomUtils.nextInt(10, 100));
      cg.setConsentGroupName(RandomStringUtils.randomAlphabetic(10));
      cg.setDatasetId(d.getDataSetId());
      cg.setDataAccessCommitteeId(d.getDacId());
      return cg;
    }).toList();
    registration.setConsentGroups(cgs);
    return registration;
  }

}
