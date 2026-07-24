package org.broadinstitute.consent.http.models.datause;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.BadRequestException;
import java.util.List;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DataUsePrimaryValidatorTest {

  @Test
  void rejectNullDataUse() {
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> DataUsePrimaryValidator.validate((DataUse) null, AccessManagement.OPEN));

    assertEquals("Data Use is required", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("accessManagementValidationCases")
  void validateAccessManagementAgainstPrimaryShape(
      AccessManagement accessManagement,
      DataUsePrimaryClassification classification,
      boolean valid) {
    if (valid) {
      assertDoesNotThrow(() -> DataUsePrimaryValidator.validate(classification, accessManagement));
    } else {
      BadRequestException exception =
          assertThrows(
              BadRequestException.class,
              () -> DataUsePrimaryValidator.validate(classification, accessManagement));
      assertEquals(DataUsePrimaryValidator.VALIDATION_MESSAGE, exception.getMessage());
    }
  }

  static Stream<Arguments> accessManagementValidationCases() {
    DataUsePrimaryClassification none = classification();
    DataUsePrimaryClassification single = classification(DataUsePrimaryCategory.GRU);
    DataUsePrimaryClassification multiple =
        classification(DataUsePrimaryCategory.GRU, DataUsePrimaryCategory.HMB);
    return Stream.of(
        Arguments.of(AccessManagement.OPEN, none, true),
        Arguments.of(AccessManagement.OPEN, single, false),
        Arguments.of(AccessManagement.OPEN, multiple, false),
        Arguments.of(AccessManagement.CONTROLLED, none, false),
        Arguments.of(AccessManagement.CONTROLLED, single, true),
        Arguments.of(AccessManagement.CONTROLLED, multiple, false),
        Arguments.of(AccessManagement.EXTERNAL, none, false),
        Arguments.of(AccessManagement.EXTERNAL, single, true),
        Arguments.of(AccessManagement.EXTERNAL, multiple, false),
        Arguments.of(null, none, false),
        Arguments.of(null, single, true),
        Arguments.of(null, multiple, false));
  }

  private static DataUsePrimaryClassification classification(DataUsePrimaryCategory... categories) {
    return new DataUsePrimaryClassification(List.of(categories));
  }
}
