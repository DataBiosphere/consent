package org.broadinstitute.consent.http.enumeration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailReferenceTest {

  @Test
  void approval_types_store_a_dar_reference() {
    for (EmailType type : EmailType.values()) {
      if (EmailReference.isApproval(type)) {
        assertTrue(
            List.of(EmailReference.DAR_CODE, EmailReference.DAR_REFERENCE_ID)
                .contains(EmailReference.of(type)),
            type.name());
      }
    }
  }

  @Test
  void kinds_by_type_int_place_each_type_at_its_number() {
    List<String> kinds = EmailReference.kindsByTypeInt();

    for (EmailType type : EmailType.values()) {
      assertEquals(EmailReference.of(type).name(), kinds.get(type.getTypeInt() - 1), type.name());
    }
  }
}
