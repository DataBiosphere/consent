package org.broadinstitute.consent.http.util.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.Gson;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.AlternativeDataSharingPlanAccessManagement;
import org.junit.jupiter.api.Test;

class JsonValueEnumTypeAdapterFactoryTest {

  private final Gson gson = GsonUtil.getRegistrationInstance();

  @Test
  void writesTheJsonValue() {
    assertEquals(
        "\"Open Access\"", gson.toJson(AlternativeDataSharingPlanAccessManagement.OPEN_ACCESS));
  }

  @Test
  void readsTheJsonValueOrTheConstantName() {
    assertEquals(
        AlternativeDataSharingPlanAccessManagement.OPEN_ACCESS,
        gson.fromJson("\"Open Access\"", AlternativeDataSharingPlanAccessManagement.class));
    assertEquals(
        AlternativeDataSharingPlanAccessManagement.OPEN_ACCESS,
        gson.fromJson("\"OPEN_ACCESS\"", AlternativeDataSharingPlanAccessManagement.class));
  }

  @Test
  void readsAnUnknownValueOrNullAsNull() {
    assertNull(gson.fromJson("\"Closed\"", AlternativeDataSharingPlanAccessManagement.class));
    assertNull(gson.fromJson("null", AlternativeDataSharingPlanAccessManagement.class));
    assertEquals("null", gson.toJson(null, AlternativeDataSharingPlanAccessManagement.class));
  }

  @Test
  void leavesTheSharedInstanceWritingConstantNames() {
    // Stored JSON, such as the fileTypes dataset property, is written by the shared instance.
    assertEquals(
        "\"OPEN_ACCESS\"",
        GsonUtil.getInstance().toJson(AlternativeDataSharingPlanAccessManagement.OPEN_ACCESS));
  }

  @Test
  void leavesAnEnumWithoutJsonValueToGson() {
    assertEquals("\"Json\"", gson.toJson(PropertyType.Json));
  }
}
