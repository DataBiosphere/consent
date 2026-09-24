package org.broadinstitute.consent.http.models;

import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.StudyType;

/**
 * Builds a {@link StudyPatch} for tests by naming the fields it sets.
 *
 * <p>StudyPatch has eighteen components, several of them adjacent Strings, so a positional call
 * site is a row of nulls with a value buried in it - unreadable, and it silently keeps compiling if
 * two neighbours are transposed or a component is inserted. Tests that care about one or two fields
 * should say which ones.
 *
 * <p>Deliberately test-only: the production callers build a patch from a request body through
 * {@link StudyPatch#fromJson(String)}, which needs no builder.
 */
public final class StudyPatchBuilder {

  private String name;
  private StudyType studyType;
  private String description;
  private List<String> dataTypes;
  private String phenotypeIndication;
  private String species;
  private String piName;
  private String piEmail;
  private Integer piInstitutionId;
  private String piOrcid;
  private String piLinkedinUrl;
  private String piWebsiteUrl;
  private List<String> dataCustodianEmail;
  private String alternativeDataSharingPlanTargetDeliveryDate;
  private String alternativeDataSharingPlanTargetPublicReleaseDate;
  private Boolean publicVisibility;
  private String externalIdentifier;
  private String externalIdentifierType;
  private Set<String> explicitNulls = Set.of();

  public static StudyPatchBuilder patch() {
    return new StudyPatchBuilder();
  }

  public StudyPatchBuilder name(String value) {
    this.name = value;
    return this;
  }

  public StudyPatchBuilder studyType(StudyType value) {
    this.studyType = value;
    return this;
  }

  public StudyPatchBuilder description(String value) {
    this.description = value;
    return this;
  }

  public StudyPatchBuilder dataTypes(List<String> value) {
    this.dataTypes = value;
    return this;
  }

  public StudyPatchBuilder phenotypeIndication(String value) {
    this.phenotypeIndication = value;
    return this;
  }

  public StudyPatchBuilder species(String value) {
    this.species = value;
    return this;
  }

  public StudyPatchBuilder piName(String value) {
    this.piName = value;
    return this;
  }

  public StudyPatchBuilder piEmail(String value) {
    this.piEmail = value;
    return this;
  }

  public StudyPatchBuilder piInstitutionId(Integer value) {
    this.piInstitutionId = value;
    return this;
  }

  public StudyPatchBuilder piOrcid(String value) {
    this.piOrcid = value;
    return this;
  }

  public StudyPatchBuilder piLinkedinUrl(String value) {
    this.piLinkedinUrl = value;
    return this;
  }

  public StudyPatchBuilder piWebsiteUrl(String value) {
    this.piWebsiteUrl = value;
    return this;
  }

  public StudyPatchBuilder dataCustodianEmail(List<String> value) {
    this.dataCustodianEmail = value;
    return this;
  }

  public StudyPatchBuilder targetDeliveryDate(String value) {
    this.alternativeDataSharingPlanTargetDeliveryDate = value;
    return this;
  }

  public StudyPatchBuilder targetPublicReleaseDate(String value) {
    this.alternativeDataSharingPlanTargetPublicReleaseDate = value;
    return this;
  }

  public StudyPatchBuilder publicVisibility(Boolean value) {
    this.publicVisibility = value;
    return this;
  }

  public StudyPatchBuilder externalIdentifier(String value) {
    this.externalIdentifier = value;
    return this;
  }

  public StudyPatchBuilder externalIdentifierType(String value) {
    this.externalIdentifierType = value;
    return this;
  }

  /**
   * The fields the body sent as an explicit JSON null, which the PI columns read as "clear". A
   * patch built without them behaves like one built directly rather than from a request body.
   */
  public StudyPatchBuilder explicitNulls(String... fields) {
    this.explicitNulls = Set.of(fields);
    return this;
  }

  public StudyPatch build() {
    return new StudyPatch(
        name,
        studyType,
        description,
        dataTypes,
        phenotypeIndication,
        species,
        piName,
        piEmail,
        piInstitutionId,
        piOrcid,
        piLinkedinUrl,
        piWebsiteUrl,
        dataCustodianEmail,
        alternativeDataSharingPlanTargetDeliveryDate,
        alternativeDataSharingPlanTargetPublicReleaseDate,
        publicVisibility,
        externalIdentifier,
        externalIdentifierType,
        explicitNulls);
  }
}
