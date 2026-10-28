package org.broadinstitute.consent.http.util.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

  @Test
  void writesAConstantWithItsOwnBodyAsItsValue() {
    assertEquals("\"custom\"", gson.toJson(WithBody.CUSTOM));
  }

  @Test
  void writesANullValueAsNullAndStillReadsTheName() {
    assertEquals("null", gson.toJson(WithNullValue.UNLABELLED));
    assertEquals(WithNullValue.UNLABELLED, gson.fromJson("\"UNLABELLED\"", WithNullValue.class));
  }

  @Test
  void ignoresAJsonValueThatTakesArguments() {
    assertEquals("\"FIRST\"", gson.toJson(WithArgumentJsonValue.FIRST));
  }

  @Test
  void ignoresEnumItselfAndNonEnums() {
    JsonValueEnumTypeAdapterFactory factory = new JsonValueEnumTypeAdapterFactory();
    assertNull(factory.create(gson, TypeToken.get(Enum.class)));
    assertNull(factory.create(gson, TypeToken.get(String.class)));
  }

  @Test
  void reportsAJsonValueThatThrows() {
    assertThrows(IllegalStateException.class, () -> gson.toJson(WithThrowingValue.BROKEN));
  }

  enum WithBody {
    PLAIN("plain"),
    CUSTOM("custom") {
      @Override
      public String toString() {
        return "overridden";
      }
    };

    private final String value;

    WithBody(String value) {
      this.value = value;
    }

    @JsonValue
    public String value() {
      return value;
    }
  }

  enum WithNullValue {
    UNLABELLED;

    @JsonValue
    public String value() {
      return null;
    }
  }

  enum WithArgumentJsonValue {
    FIRST;

    @JsonValue
    public String value(int index) {
      return name() + index;
    }
  }

  enum WithThrowingValue {
    BROKEN;

    @JsonValue
    public String value() {
      throw new UnsupportedOperationException("no value");
    }
  }
}
