package org.broadinstitute.consent.http.exceptions;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.ClientErrorException;

public class NIHComplianceRuleException extends ClientErrorException {

  public static final String MESSAGE =
      "NIH has received, reviewed, and processed your data access request. This request is denied. Please see Guide Notice [NOT-OD-25-083](https://grants.nih.gov/grants/guide/notice-files/NOT-OD-25-083.html) for more information.";

  public NIHComplianceRuleException() {
    super(MESSAGE, HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY);
  }
}
