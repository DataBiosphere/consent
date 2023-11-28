package org.broadinstitute.consent.http.models;

import java.util.List;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;

public class StudyConversion {

  private String name;
  private String description;
  private List<String> dataTypes;
  private String phenotype;
  private String species;
  private String piName;
  private String dataSubmitterEmail;
  private List<String> dataCustodianEmails;
  private String targetDeliveryDate;
  private String targetPublicReleaseDate;
  private Boolean publicVisibility;
  private NihAnvilUse nihAnvilUse;

  private List<String> primaryTerms;
  private List<String> secondaryTerms;

  private Integer dacId;

  private String dataLocation;

  private Integer numberOfParticipants;

}
