package org.broadinstitute.consent.http.models.dataset_registration_v1.builder;

import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.accessManagement;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dataLocation;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.fileTypes;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.numberOfParticipants;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.url;

import com.google.gson.JsonArray;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class ConsentGroupFromDataset {

  public ConsentGroup build(Dataset dataset) {
    if (dataset != null) {
      ConsentGroup consentGroup = new ConsentGroup();
      consentGroup.setDatasetId(dataset.getDatasetId());
      consentGroup.setDatasetIdentifier(dataset.getDatasetIdentifier());
      consentGroup.setConsentGroupName(dataset.getName());
      String accessManagementVal = findStringDSPropValue(dataset.getProperties(), accessManagement);
      if (Objects.nonNull(accessManagementVal)) {
        consentGroup.setAccessManagement(AccessManagement.fromValue(accessManagementVal));
      }
      if (dataset.getDataUse() != null) {
        DataUse dataUse = dataset.getDataUse();
        consentGroup.setGeneralResearchUse(dataUse.getGeneralUse());
        consentGroup.setHmb(dataUse.getHmbResearch());
        consentGroup.setDiseaseSpecificUse(dataUse.getDiseaseRestrictions());
        consentGroup.setPoa(dataUse.getPopulationOriginsAncestry());
        consentGroup.setOtherPrimary(dataUse.getOther());
        consentGroup.setNmds(dataUse.getMethodsResearch());
        consentGroup.setGso(dataUse.getGeneticStudiesOnly());
        consentGroup.setPub(dataUse.getPublicationResults());
        consentGroup.setCol(dataUse.getCollaboratorRequired());
        consentGroup.setIrb(dataUse.getEthicsApprovalRequired());
        consentGroup.setGs(dataUse.getGeographicalRestrictions());
        if (!StringUtils.isBlank(dataUse.getPublicationMoratorium())) {
          consentGroup.setMor(true);
          consentGroup.setMorDate(dataUse.getPublicationMoratorium());
        }
        consentGroup.setNpu(dataUse.getNonProfitUse());
        consentGroup.setOtherSecondary(dataUse.getSecondaryOther());
      }
      if (dataset.getDacId() != null) {
        consentGroup.setDataAccessCommitteeId(dataset.getDacId());
      }
      String dataLocationVal = findStringDSPropValue(dataset.getProperties(), dataLocation);
      if (Objects.nonNull(dataLocationVal)) {
        consentGroup.setDataLocation(DataLocation.fromValue(dataLocationVal));
      }
      String urlVal = findStringDSPropValue(dataset.getProperties(), url);
      if (Objects.nonNull(urlVal)) {
        URI uri = URI.create(urlVal);
        consentGroup.setUrl(uri);
      }
      consentGroup.setNumberOfParticipants(
          findIntegerDSPropValue(dataset.getProperties(), numberOfParticipants));
      consentGroup.setFileTypes(findListFTSODSPropValue(dataset.getProperties()));
      return consentGroup;
    }
    return null;
  }

  @Nullable
  private String findStringDSPropValue(Set<DatasetProperty> props, String propName) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getSchemaProperty() != null && p.getSchemaProperty().equalsIgnoreCase(propName))
          .map(DatasetProperty::getPropertyValueAsString)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  @Nullable
  private Integer findIntegerDSPropValue(Set<DatasetProperty> props, String propName) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getSchemaProperty() != null && p.getSchemaProperty().equalsIgnoreCase(propName))
          .map(DatasetProperty::getPropertyValue)
          .map(Object::toString)
          .map(Integer::valueOf)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  @Nullable
  private List<FileTypeObject> findListFTSODSPropValue(Set<DatasetProperty> props) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getSchemaProperty() != null && p.getSchemaProperty().equalsIgnoreCase(fileTypes))
          .map(DatasetProperty::getPropertyValueAsString)
          .map(p -> GsonUtil.getInstance().fromJson(p, JsonArray.class))
          .map(JsonArray::asList)
          .flatMap(List::stream)
          .map(p -> GsonUtil.getInstance().fromJson(p, FileTypeObject.class))
          .toList();
    }
    return null;
  }

}
