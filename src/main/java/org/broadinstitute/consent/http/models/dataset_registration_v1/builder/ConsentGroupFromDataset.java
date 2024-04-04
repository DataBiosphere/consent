package org.broadinstitute.consent.http.models.dataset_registration_v1.builder;

import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.accessManagement;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.col;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dataAccessCommitteeId;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dataLocation;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.diseaseSpecificUse;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.fileTypes;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.generalResearchUse;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.gs;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.gso;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.hmb;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.irb;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.mor;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.morDate;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.nmds;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.npu;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.numberOfParticipants;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.otherPrimary;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.otherSecondary;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.poa;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.pub;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.url;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class ConsentGroupFromDataset {

  public ConsentGroup build(Dataset dataset) {
    if (Objects.nonNull(dataset)) {
      ConsentGroup consentGroup = new ConsentGroup();
      consentGroup.setDatasetId(dataset.getDataSetId());
      consentGroup.setDatasetIdentifier(dataset.getDatasetIdentifier());
      consentGroup.setConsentGroupName(dataset.getName());
      String accessManagementVal = findStringDSPropValue(dataset.getProperties(), accessManagement);
      if (Objects.nonNull(accessManagementVal)) {
        consentGroup.setAccessManagement(AccessManagement.fromValue(accessManagementVal));
      }
      consentGroup.setGeneralResearchUse(
          findBooleanDSPropValue(dataset.getProperties(), generalResearchUse));
      consentGroup.setHmb(findBooleanDSPropValue(dataset.getProperties(), hmb));
      consentGroup.setDiseaseSpecificUse(findListStringDSPropValue(dataset.getProperties()));
      consentGroup.setPoa(findBooleanDSPropValue(dataset.getProperties(), poa));
      consentGroup.setOtherPrimary(findStringDSPropValue(dataset.getProperties(), otherPrimary));
      consentGroup.setNmds(findBooleanDSPropValue(dataset.getProperties(), nmds));
      consentGroup.setGso(findBooleanDSPropValue(dataset.getProperties(), gso));
      consentGroup.setPub(findBooleanDSPropValue(dataset.getProperties(), pub));
      consentGroup.setCol(findBooleanDSPropValue(dataset.getProperties(), col));
      consentGroup.setIrb(findBooleanDSPropValue(dataset.getProperties(), irb));
      consentGroup.setGs(findStringDSPropValue(dataset.getProperties(), gs));
      consentGroup.setMor(findBooleanDSPropValue(dataset.getProperties(), mor));
      consentGroup.setMorDate(findStringDSPropValue(dataset.getProperties(), morDate));
      consentGroup.setNpu(findBooleanDSPropValue(dataset.getProperties(), npu));
      consentGroup.setOtherSecondary(
          findStringDSPropValue(dataset.getProperties(), otherSecondary));
      consentGroup.setDataAccessCommitteeId(
          findIntegerDSPropValue(dataset.getProperties(), dataAccessCommitteeId));
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
  private Boolean findBooleanDSPropValue(Set<DatasetProperty> props, String propName) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getSchemaProperty() != null && p.getSchemaProperty().equalsIgnoreCase(propName))
          .map(DatasetProperty::getPropertyValue)
          .map(Object::toString)
          .map(Boolean::valueOf)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  @Nullable
  private List<String> findListStringDSPropValue(Set<DatasetProperty> props) {
    if (Objects.nonNull(props) && !props.isEmpty()) {
      return props
          .stream()
          .filter(p -> p.getSchemaProperty() != null && p.getSchemaProperty().equalsIgnoreCase(diseaseSpecificUse))
          .map(DatasetProperty::getPropertyValue)
          .map(p -> GsonUtil.getInstance().fromJson(p.toString(), JsonElement.class))
          .map(JsonElement::getAsJsonArray)
          .map(JsonArray::asList)
          .flatMap(List::stream)
          .map(JsonElement::getAsString)
          .toList();
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
