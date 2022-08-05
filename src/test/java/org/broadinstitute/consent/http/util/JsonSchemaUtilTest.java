package org.broadinstitute.consent.http.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonSchemaUtilTest {

  @Test
  public void testIsValidDataSubmitterObject_v1_case0() {
    String instance = "{}";
    JsonSchemaUtil util = new JsonSchemaUtil();
    boolean valid = util.isValidDataSubmitterObject_v1(instance);
    assertFalse(valid);
  }

  @Test
  public void testIsValidDataSubmitterObject_v1_case1() {
    String instance = "{ " +
            "  \"studyType\": \"Observational\", " +
            "  \"studyName\": \"name\", " +
            "  \"studyDescription\": \"description\", " +
            "  \"dataTypes\": [\"types\"], " +
            "  \"fileTypes\": [{ " +
            "    \"fileType\": \"Arrays\", " +
            "    \"functionalEquivalence\": \"equivalence\", " +
            "    \"numberOfParticipants\": 2 " +
            "  }], " +
            "  \"phenotypeIndication\": \"phenotype\", " +
            "  \"species\": \"species\", " +
            "  \"piName\": \"PI Name\", " +
            "  \"dataSubmitterUserId\": 1, " +
            "  \"dataCustodianEmail\": [\"email@abc.com\"], " +
            "  \"publicVisibility\": true, " +
            "  \"dataAccessCommitteeId\": 1, " +
            "  \"consentGroups\": [{ " +
            "    \"consentGroupName\": \"name\", " +
            "    \"generalResearchUse\": true " +
            "  }] " +
            "}";
    JsonSchemaUtil util = new JsonSchemaUtil();
    boolean valid = util.isValidDataSubmitterObject_v1(instance);
    assertTrue(valid);
  }
}
