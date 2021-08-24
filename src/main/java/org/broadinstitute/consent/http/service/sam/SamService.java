package org.broadinstitute.consent.http.service.sam;

import com.google.api.client.http.EmptyContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.util.HttpClientUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SamService {

  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration configuration;

  @Inject
  public SamService(ServicesConfiguration configuration) {
    this.clientUtil = new HttpClientUtil();
    this.configuration = configuration;
  }

  public List<ResourceType> getResourceTypes(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getV1ResourceTypesUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    HttpResponse response = clientUtil.handleHttpRequest(request);
    String body = response.parseAsString();
    Type resourceTypesListType = new TypeToken<ArrayList<ResourceType>>() {}.getType();
    return new Gson().fromJson(body, resourceTypesListType);
  }

  public UserStatusInfo getRegistrationInfo(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getRegisterUserV2SelfInfoUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    HttpResponse response = clientUtil.handleHttpRequest(request);
    String body = response.parseAsString();
    return new Gson().fromJson(body, UserStatusInfo.class);
  }

  public UserStatusDiagnostics getSelfDiagnostics(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getV2SelfDiagnosticsUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    HttpResponse response = clientUtil.handleHttpRequest(request);
    String body = response.parseAsString();
    return new Gson().fromJson(body, UserStatusDiagnostics.class);
  }

  public UserStatus postRegistrationInfo(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.postRegisterUserV2SelfUrl());
    HttpRequest request = clientUtil.buildPostRequest(genericUrl, new EmptyContent(), authUser);
    HttpResponse response = clientUtil.handleHttpRequest(request);
    String body = response.parseAsString();
    return new Gson().fromJson(body, UserStatus.class);
  }
}
