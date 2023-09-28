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
 *     <testcase name="testStatus(String 1)" classname="org.broadinstitute.consent.integration.status.StatusTests" time="1.004"/>
 *     <testcase name="testStatus(String 2)" classname="org.broadinstitute.consent.integration.status.StatusTests" time="0.184"/>
 *     <testcase name="testStatus(String 3)" classname="org.broadinstitute.consent.integration.status.StatusTests" time="0.144"/>
 *   Non-parameterized:
 *     <testcase name="testVersion" classname="org.broadinstitute.consent.integration.status.StatusTests" time="1.004"/>
 *     <testcase name="testStatus" classname="org.broadinstitute.consent.integration.status.StatusTests" time="0.184"/>
 *     <testcase name="testLiveness" classname="org.broadinstitute.consent.integration.status.StatusTests" time="0.144"/>
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
