package org.broadinstitute.consent.http.service.sam;

import com.google.api.client.http.EmptyContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SamService {

  private final ExecutorService executorService;
  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration configuration;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public SamService(ServicesConfiguration configuration) {
    this.executorService = Executors.newCachedThreadPool();
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

  public void asyncPostRegistrationInfo(AuthUser authUser) {
    ListeningExecutorService listeningExecutorService = MoreExecutors.listeningDecorator(executorService);
    ListenableFuture<UserStatus> userStatusFuture =
        listeningExecutorService.submit(() -> postRegistrationInfo(authUser));
    Futures.addCallback(
        userStatusFuture,
        new FutureCallback<>() {
          @Override
          public void onSuccess(@Nullable UserStatus userStatus) {
            logger.info("Successfully registered user in Sam: " + authUser.getEmail());
          }

          @Override
          public void onFailure(@NonNull Throwable throwable) {
            logger.error(throwable.getMessage());
          }
        },
        listeningExecutorService);
  }

  public String getToSText() throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getToSTextUrl());
    HttpRequest request = clientUtil.buildUnAuthedGetRequest(genericUrl);
    request.getHeaders().setAccept(MediaType.TEXT_PLAIN);
    HttpResponse response = clientUtil.handleHttpRequest(request);
    return response.parseAsString();
  }

  public TosResponse postTosAcceptedStatus(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.postTosAcceptedUrl());
    JsonHttpContent content = new JsonHttpContent(new GsonFactory(), "app.terra.bio/#terms-of-service");
    HttpRequest request = clientUtil.buildPostRequest(genericUrl, content, authUser);
    HttpResponse response = clientUtil.handleHttpRequest(request);
    String body = response.parseAsString();
    return new Gson().fromJson(body, TosResponse.class);
  }
}
