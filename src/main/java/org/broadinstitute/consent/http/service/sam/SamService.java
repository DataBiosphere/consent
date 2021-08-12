package org.broadinstitute.consent.http.service.sam;

import com.google.api.client.http.EmptyContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.configurations.SamConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;

public class SamService {

  private final SamConfiguration configuration;

  @Inject
  public SamService(SamConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * Make this call before any Sam API call. This will check to see if the user exists, and if not,
   * create them in Sam.
   *
   * @param authUser The AuthUser
   */
  private void validateUser(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getRegisterSelfUrl());
    HttpRequest request = buildGetRequest(genericUrl, authUser);
    HttpResponse response = request.execute();
    if (response.getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
      GenericUrl postUrl = new GenericUrl(configuration.postRegisterSelfUrl());
      HttpContent content = new EmptyContent();
      HttpRequest postRequest = buildPostRequest(postUrl, content, authUser);
      HttpResponse postResponse = postRequest.execute();
      assert postResponse.getStatusCode() == HttpStatusCodes.STATUS_CODE_CREATED;
    }
  }

  private HttpRequest buildGetRequest(GenericUrl genericUrl, AuthUser authUser) throws Exception {
    HttpTransport transport = new NetHttpTransport();
    HttpRequest request = transport.createRequestFactory().buildGetRequest(genericUrl);
    request.getHeaders().setAuthorization("Bearer " + authUser.getAuthToken());
    return request;
  }

  private HttpRequest buildPostRequest(
      GenericUrl genericUrl, HttpContent content, AuthUser authUser) throws Exception {
    HttpTransport transport = new NetHttpTransport();
    HttpRequest request = transport.createRequestFactory().buildPostRequest(genericUrl, content);
    request.getHeaders().setAuthorization("Bearer " + authUser.getAuthToken());
    return request;
  }

  private HttpRequest buildPutRequest(GenericUrl genericUrl, HttpContent content, AuthUser authUser)
      throws Exception {
    HttpTransport transport = new NetHttpTransport();
    HttpRequest request = transport.createRequestFactory().buildPutRequest(genericUrl, content);
    request.getHeaders().setAuthorization("Bearer " + authUser.getAuthToken());
    return request;
  }

  private HttpRequest buildDeleteRequest(GenericUrl genericUrl, AuthUser authUser)
      throws Exception {
    HttpTransport transport = new NetHttpTransport();
    HttpRequest request = transport.createRequestFactory().buildDeleteRequest(genericUrl);
    request.getHeaders().setAuthorization("Bearer " + authUser.getAuthToken());
    return request;
  }
}
