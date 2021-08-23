package org.broadinstitute.consent.http.util;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import java.io.IOException;
import java.util.Objects;

public class HttpClientUtil {

  public CloseableHttpResponse getHttpResponse(HttpUriRequest request) throws IOException {
    return HttpClients.createMinimal().execute(request);
  }

  public HttpResponse handleHttpRequest(HttpRequest request) throws Exception {
    HttpResponse response = request.setThrowExceptionOnExecuteError(false).execute();
    if (Objects.nonNull(response)) {
      switch (response.getStatusCode()) {
        case HttpStatusCodes.STATUS_CODE_BAD_REQUEST:
          throw new BadRequestException(response.getStatusMessage());
        case HttpStatusCodes.STATUS_CODE_UNAUTHORIZED:
          throw new NotAuthorizedException(response.getStatusMessage());
        case HttpStatusCodes.STATUS_CODE_FORBIDDEN:
          throw new ForbiddenException(response.getStatusMessage());
        case HttpStatusCodes.STATUS_CODE_NOT_FOUND:
          throw new NotFoundException(response.getStatusMessage());
        case HttpStatusCodes.STATUS_CODE_CONFLICT:
          throw new ConsentConflictException(response.getStatusMessage());
        default:
          return response;
      }
    }
    throw new ServerErrorException("Server Error", HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
  }
}
