package org.broadinstitute.consent.integration.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;
import org.broadinstitute.consent.integration.IntegrationTestHelper;
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
class StatusTests implements IntegrationTestHelper {

  @Test
  void testStatus() throws Exception {
    SimpleResponse response = fetchGetResponse("status");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.code());
  }

  @Test
  void testLiveness() throws Exception {
    SimpleResponse response = fetchGetResponse("liveness");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.code());
  }

  @Test
  void testVersion() throws Exception {
    SimpleResponse response = fetchGetResponse("version");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.code());
  }

}
