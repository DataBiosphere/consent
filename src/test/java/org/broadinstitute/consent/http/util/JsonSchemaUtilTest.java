package org.broadinstitute.consent.http.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class JsonSchemaUtilTest {

  @Test
  public void testIsValidDataSubmitterObject_v1_case0() {
    String instance = "{}";
    JsonSchemaUtil util = new JsonSchemaUtil();
    boolean valid = util.isValidDataSubmitterObject_v1(instance);
    assertFalse(valid);
  }
}
