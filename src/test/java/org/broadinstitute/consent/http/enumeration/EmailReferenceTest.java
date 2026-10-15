package org.broadinstitute.consent.http.enumeration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmailReferenceTest {

  @Test
  void kinds_by_type_int_place_each_type_at_its_number() {
    List<String> kinds = EmailReference.kindsByTypeInt();

    for (EmailType type : EmailType.values()) {
      assertEquals(EmailReference.of(type).name(), kinds.get(type.getTypeInt() - 1), type.name());
    }
  }
}
