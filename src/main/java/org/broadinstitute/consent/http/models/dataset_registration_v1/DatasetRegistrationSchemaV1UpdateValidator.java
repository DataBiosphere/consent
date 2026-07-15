package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class DatasetRegistrationSchemaV1UpdateValidator {

  private final ExclusionStrategy studyExclusionStrategy =
      new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
          return fieldAttributes.getName().equalsIgnoreCase("dataSubmitterUserId");
        }

        @Override
        public boolean shouldSkipClass(Class<?> aClass) {
          return aClass.getSimpleName().equalsIgnoreCase("ConsentGroup");
        }
      };

  private final ExclusionStrategy consentGroupExclusionStrategy =
      new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
          final HashSet<String> exclusions =
              new HashSet<>(
                  List.of(
                      DatasetRegistrationSchemaV1Builder.accessManagement,
                      DatasetRegistrationSchemaV1Builder.col,
                      DatasetRegistrationSchemaV1Builder.dataAccessCommitteeId,
                      DatasetRegistrationSchemaV1Builder.datasetIdentifier,
                      DatasetRegistrationSchemaV1Builder.diseaseSpecificUse,
                      DatasetRegistrationSchemaV1Builder.generalResearchUse,
                      DatasetRegistrationSchemaV1Builder.gs,
                      DatasetRegistrationSchemaV1Builder.gso,
                      DatasetRegistrationSchemaV1Builder.hmb,
                      DatasetRegistrationSchemaV1Builder.irb,
                      DatasetRegistrationSchemaV1Builder.nmds,
                      DatasetRegistrationSchemaV1Builder.mor,
                      DatasetRegistrationSchemaV1Builder.morDate,
                      DatasetRegistrationSchemaV1Builder.npu,
                      DatasetRegistrationSchemaV1Builder.otherPrimary,
                      DatasetRegistrationSchemaV1Builder.otherSecondary,
                      DatasetRegistrationSchemaV1Builder.pub,
                      DatasetRegistrationSchemaV1Builder.poa));
          return exclusions.contains(fieldAttributes.getName());
        }

        @Override
        public boolean shouldSkipClass(Class<?> aClass) {
          return false;
        }
      };

  /**
   * Create a registration object suitable for the Update operation.
   *
   * @param json DatasetRegistrationSchemaV1 in JSON format
   * @return DatasetRegistrationSchemaV1
   */
  public DatasetRegistrationSchemaV1 deserializeRegistration(String json) {
    Gson studyGson =
        GsonUtil.gsonBuilderWithAdapters()
            .addDeserializationExclusionStrategy(studyExclusionStrategy)
            .create();
    // Create the registration without any ConsentGroups
    DatasetRegistrationSchemaV1 registration =
        studyGson.fromJson(json, DatasetRegistrationSchemaV1.class);
    // Ensure that we have no null entries before parsing them.
    registration.setConsentGroups(new ArrayList<>());

    // Conditionally parse the consent groups
    Gson gson = GsonUtil.getInstance();
    Gson filteredCGGson =
        GsonUtil.gsonBuilderWithAdapters()
            .addDeserializationExclusionStrategy(consentGroupExclusionStrategy)
            .create();
    JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
    JsonArray jsonArray = jsonObject.getAsJsonArray("consentGroups");
    if (jsonArray != null) {
      jsonArray
          .asList()
          .forEach(
              jsonElement -> {
                JsonObject cgJson = jsonElement.getAsJsonObject();
                if (cgJson.has("datasetId")) {
                  // If we have a dataset id, we're updating. Filter out non-updatable fields
                  ConsentGroup cg = filteredCGGson.fromJson(cgJson, ConsentGroup.class);
                  registration.getConsentGroups().add(cg);
                } else {
                  // If we have don't have a dataset id, we're trying to add a new one to the study.
                  ConsentGroup cg = gson.fromJson(cgJson, ConsentGroup.class);
                  registration.getConsentGroups().add(cg);
                }
              });
    }
    return registration;
  }
}
