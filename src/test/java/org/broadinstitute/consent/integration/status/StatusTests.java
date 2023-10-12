package org.broadinstitute.consent.integration.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;
import org.broadinstitute.consent.integration.IntegrationTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * These tests are not parameterized because that displays poorly in the results xml, i.e. compare:
 *   Parameterized:
 *     <testcase name="testPageOk(String 1)" ...
 *     <testcase name="testPageOk(String 2)" ...
 *     <testcase name="testPageOk(String 3)" ...
 *   Non-parameterized:
 *     <testcase name="version_page_OK" ...
 *     <testcase name="status_page_OK" ...
 *     <testcase name="liveness_page_OK" ...
 * It would be ideal if the provided display name was used in the xml, but that only appears in the
 * IDE display.
 */
@DisplayName("Status Related Tests")
class StatusTests implements IntegrationTestHelper {

  @DisplayName("Status: Status test")
  @Test
  void status_page_OK() throws Exception {
    SimpleResponse response = fetchGetResponse("status");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.code());
  }

  @DisplayName("Status: Liveness test")
  @Test
  void liveness_page_OK() throws Exception {
    SimpleResponse response = fetchGetResponse("liveness");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.code());
  }

  @DisplayName("Status: Version test")
  @Test
  void version_page_OK() throws Exception {
    SimpleResponse response = fetchGetResponse("version");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.code());
  }

}
