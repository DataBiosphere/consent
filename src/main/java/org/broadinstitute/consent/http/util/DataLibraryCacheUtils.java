package org.broadinstitute.consent.http.util;

import com.google.gson.JsonArray;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.CacheDocument;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.elastic_search.DacTerm;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.models.elastic_search.InstitutionTerm;
import org.broadinstitute.consent.http.models.elastic_search.StudyTerm;
import org.broadinstitute.consent.http.models.elastic_search.UserTerm;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.service.OntologyService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DataLibraryCacheUtils implements ConsentLogger {
  public static StudyTerm toStudyTerm(Study study, UserDAO userDAO) {
    if (Objects.isNull(study)) {
      return null;
    }

    StudyTerm term = new StudyTerm();

    term.setDescription(study.getDescription());
    term.setStudyName(study.getName());
    term.setStudyId(study.getStudyId());
    term.setDataTypes(study.getDataTypes());
    term.setPiName(study.getPiName());
    term.setPublicVisibility(study.getPublicVisibility());

    findStudyProperty(study.getProperties(), "dbGaPPhsID")
        .ifPresent(prop -> term.setPhsId(prop.getValue().toString()));

    findStudyProperty(study.getProperties(), "phenotypeIndication")
        .ifPresent(prop -> term.setPhenotype(prop.getValue().toString()));

    findStudyProperty(study.getProperties(), "species")
        .ifPresent(prop -> term.setSpecies(prop.getValue().toString()));

    findStudyProperty(study.getProperties(), "dataCustodianEmail")
        .ifPresent(
            prop -> {
              JsonArray jsonArray = (JsonArray) prop.getValue();
              List<String> dataCustodianEmail = new ArrayList<>();
              jsonArray.forEach(email -> dataCustodianEmail.add(email.getAsString()));
              term.setDataCustodianEmail(dataCustodianEmail);
            });

    if (Objects.nonNull(study.getCreateUserId())) {
      term.setDataSubmitterId(study.getCreateUserId());
      User user = userDAO.findUserById(study.getCreateUserId());
      if (Objects.nonNull(user)) {
        study.setCreateUserEmail(user.getEmail());
      }
    }

    if (Objects.nonNull(study.getCreateUserEmail())) {
      term.setDataSubmitterEmail(study.getCreateUserEmail());
    }

    findStudyProperty(study.getProperties(), "assets")
        .ifPresent(
            prop -> {
              Object value = prop.getValue();
              Map<String, Object> assetsMap;
              // When property is loaded from db it is deserialized as JsonObject
              if (value instanceof com.google.gson.JsonElement jsonElement) {
                assetsMap =
                    GsonUtil.getInstance()
                        .fromJson(
                            jsonElement,
                            new com.google.gson.reflect.TypeToken<
                                Map<String, Object>>() {}.getType());
                // Otherwise Gson deserializes JSON and creates a LinkedTreeMap
              } else if (value instanceof Map) {
                assetsMap = (Map<String, Object>) value;
                // Fallback: try to parse as JSON string
              } else {
                assetsMap =
                    GsonUtil.getInstance()
                        .fromJson(
                            value.toString(),
                            new com.google.gson.reflect.TypeToken<
                                Map<String, Object>>() {}.getType());
              }
              term.setAssets(assetsMap);
            });

    return term;
  }

  public DatasetTerm toDatasetTerm(Dataset dataset, UserDAO userDAO, DacDAO dacDAO, InstitutionDAO institutionDAO,
      OntologyService ontologyService) {
    if (Objects.isNull(dataset)) {
      return null;
    }

    DatasetTerm term = new DatasetTerm();

    term.setDatasetId(dataset.getDatasetId());
    Optional.ofNullable(dataset.getCreateUserId())
        .ifPresent(
            userId -> {
              User user = userDAO.findUserById(dataset.getCreateUserId());
              term.setCreateUserId(dataset.getCreateUserId());
              term.setCreateUserDisplayName(user.getDisplayName());
              term.setSubmitter(toUserTerm(user, institutionDAO));
            });
    Optional.ofNullable(dataset.getUpdateUserId())
        .map(userDAO::findUserById)
        .map(e -> toUserTerm(e, institutionDAO))
        .ifPresent(term::setUpdateUser);
    term.setDatasetIdentifier(dataset.getDatasetIdentifier());
    term.setDeletable(dataset.getDeletable());
    term.setDatasetName(dataset.getName());

    if (Objects.nonNull(dataset.getStudy())) {
      term.setStudy(toStudyTerm(dataset.getStudy(), userDAO));
    }

    Optional.ofNullable(dataset.getDacId())
        .ifPresent(
            dacId -> {
              Dac dac = dacDAO.findById(dataset.getDacId());
              term.setDacId(dataset.getDacId());
              if (Objects.nonNull(dataset.getDacApproval())) {
                term.setDacApproval(dataset.getDacApproval());
              }
              term.setDac(toDacTerm(dac));
            });

    if (Objects.nonNull(dataset.getDataUse())) {
      DataUseSummary summary = ontologyService.translateDataUseSummary(dataset.getDataUse());
      if (summary != null) {
        term.setDataUse(summary);
      } else {
        logWarn("No data use summary for dataset id: %d".formatted(dataset.getDatasetId()));
      }
    }

    Optional.ofNullable(dataset.getNihInstitutionalCertificationFile())
        .ifPresent(obj -> term.setHasInstitutionCertification(true));

    findDatasetProperty(dataset.getProperties(), "accessManagement")
        .ifPresent(
            datasetProperty ->
                term.setAccessManagement(datasetProperty.getPropertyValueAsString()));

    findFirstDatasetPropertyByName(dataset.getProperties(), "# of participants")
        .ifPresent(
            datasetProperty -> {
              String value = datasetProperty.getPropertyValueAsString();
              try {
                term.setParticipantCount(Integer.valueOf(value));
              } catch (NumberFormatException nfe) {
                logWarn(nfe.getMessage());
                logWarn(
                    String.format(
                        "Unable to coerce participant count to integer: %s for dataset: %s",
                        value, dataset.getDatasetIdentifier()));
              }
            });

    findDatasetProperty(dataset.getProperties(), "url")
        .ifPresent(datasetProperty -> term.setUrl(datasetProperty.getPropertyValueAsString()));

    findDatasetProperty(dataset.getProperties(), "dataLocation")
        .ifPresent(
            datasetProperty -> term.setDataLocation(datasetProperty.getPropertyValueAsString()));

    return term;
  }

  UserTerm toUserTerm(User user, InstitutionDAO institutionDAO) {
    if (Objects.isNull(user)) {
      return null;
    }
    InstitutionTerm institution =
        (Objects.nonNull(user.getInstitutionId()))
            ? toInstitutionTerm(institutionDAO.findInstitutionById(user.getInstitutionId()))
            : null;
    return new UserTerm(user.getUserId(), user.getDisplayName(), institution);
  }

  static DacTerm toDacTerm(Dac dac) {
    if (Objects.isNull(dac)) {
      return null;
    }
    return new DacTerm(dac.getDacId(), dac.getName(), dac.getEmail());
  }

  static InstitutionTerm toInstitutionTerm(Institution institution) {
    if (Objects.isNull(institution)) {
      return null;
    }
    return new InstitutionTerm(institution.getId(), institution.getName());
  }

  static Optional<DatasetProperty> findDatasetProperty(
      Collection<DatasetProperty> props, String schemaProp) {
    return (props == null)
        ? Optional.empty()
        : props.stream()
            .filter(p -> Objects.nonNull(p.getSchemaProperty()))
            .filter(p -> p.getSchemaProperty().equals(schemaProp))
            .findFirst();
  }

  static Optional<DatasetProperty> findFirstDatasetPropertyByName(
      Collection<DatasetProperty> props, String propertyName) {
    return (props == null)
        ? Optional.empty()
        : props.stream()
            .filter(p -> p.getPropertyName().equalsIgnoreCase(propertyName))
            .findFirst();
  }

  static Optional<StudyProperty> findStudyProperty(Collection<StudyProperty> props, String key) {
    return (props == null)
        ? Optional.empty()
        : props.stream().filter(p -> p.getKey().equals(key)).findFirst();
  }

}
