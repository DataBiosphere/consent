package org.broadinstitute.consent.http.service.sam;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;

@SuppressWarnings("unused")
public class SamService {

  private final ServicesConfiguration configuration;

  @Inject
  public SamService(ServicesConfiguration configuration) {
    this.configuration = configuration;
  }

  public HttpResponse getResourceTypes(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(getV1ResourceTypesUrl());
    HttpRequest request = buildGetRequest(genericUrl, authUser);
    return request.execute();
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

  private String getV1ResourceTypesUrl() {
    return configuration.getSamUrl() + "/api/config/v1/resourceTypes";
  }
}
