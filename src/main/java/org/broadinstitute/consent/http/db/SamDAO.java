package org.broadinstitute.consent.http.db;

import com.google.api.client.http.EmptyContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MediaType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.EmailResponse;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SamDAO implements ConsentLogger {

  private final ExecutorService executorService;
  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration configuration;
  private final Integer connectTimeoutMilliseconds;
  public final Integer readTimeoutMilliseconds;

  public SamDAO(HttpClientUtil clientUtil, ServicesConfiguration configuration) {
    this.executorService = Executors.newCachedThreadPool();
    this.clientUtil = clientUtil;
    this.configuration = configuration;
    // Defaults to 10 seconds
    this.connectTimeoutMilliseconds = configuration.getTimeoutSeconds() * 1000;
    // Defaults to 60 seconds
    this.readTimeoutMilliseconds = configuration.getTimeoutSeconds() * 6000;
  }

  public List<ResourceType> getResourceTypes(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getV1ResourceTypesUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException("Error getting resource types from Sam: " + response.getStatusMessage(),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    String body = response.parseAsString();
    Type resourceTypesListType = new TypeToken<ArrayList<ResourceType>>() {
    }.getType();
    return new Gson().fromJson(body, resourceTypesListType);
  }

  public UserStatusInfo getRegistrationInfo(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getRegisterUserV2SelfInfoUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException(
          "Error getting user registration information from Sam: " + response.getStatusMessage(),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    String body = response.parseAsString();
    return new Gson().fromJson(body, UserStatusInfo.class);
  }

  public UserStatusDiagnostics getSelfDiagnostics(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getV2SelfDiagnosticsUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException(
          "Error getting enabled statuses of user from Sam: " + response.getStatusMessage(),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    String body = response.parseAsString();
    return new Gson().fromJson(body, UserStatusDiagnostics.class);
  }

  public UserStatus postRegistrationInfo(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.postRegisterUserV2SelfUrl());
    HttpRequest request = clientUtil.buildPostRequest(genericUrl, new EmptyContent(), authUser);
    HttpResponse response = executeRequest(request);
    String body = response.parseAsString();
    if (!response.isSuccessStatusCode()) {
      if (HttpStatusCodes.STATUS_CODE_CONFLICT == response.getStatusCode()) {
        throw new ConsentConflictException("User exists in Sam: " + authUser.getEmail());
      } else {
        String errorMsg = String.format("Error posting user registration information to Sam. Email [%s]. Status Code [%s]; Status Message [%s];  ",
          authUser.getEmail(),
          response.getStatusCode(),
          response.getStatusMessage());
        Exception e = new Exception(body);
        logException(errorMsg, new Exception(body));
        throw e;
      }
    }
    return new Gson().fromJson(body, UserStatus.class);
  }

  public void asyncPostRegistrationInfo(AuthUser authUser) {
    ListeningExecutorService listeningExecutorService = MoreExecutors.listeningDecorator(
        executorService);
    ListenableFuture<UserStatus> userStatusFuture =
        listeningExecutorService.submit(() -> postRegistrationInfo(authUser));
    Futures.addCallback(
        userStatusFuture,
        new FutureCallback<>() {
          @Override
          public void onSuccess(@Nullable UserStatus userStatus) {
            logInfo("Successfully registered user in Sam: " + authUser.getEmail());
          }

          @Override
          public void onFailure(@NonNull Throwable throwable) {
            logWarn("Async Post Registration Failure for user: " + authUser.getEmail() + "; " + throwable.getMessage());
          }
        },
        listeningExecutorService);
  }

  public String getToSText() throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getToSTextUrl());
    HttpRequest request = clientUtil.buildUnAuthedGetRequest(genericUrl);
    request.getHeaders().setAccept(MediaType.TEXT_PLAIN);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException("Error getting Terms of Service text from Sam: " + response.getStatusMessage(),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    return response.parseAsString();
  }

  public TosResponse getTosResponse(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getSelfTosUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException(String.format("Error getting Terms of Service: %s for user %s", response.getStatusMessage(), authUser.getEmail()),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    String body = response.parseAsString();
    return new Gson().fromJson(body, TosResponse.class);
  }

  public int acceptTosStatus(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.acceptTosUrl());
    HttpRequest request = clientUtil.buildPutRequest(genericUrl, new EmptyContent(), authUser);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException(String.format("Error accepting Terms of Service: %s for user %s", response.getStatusMessage(), authUser.getEmail()),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    return response.getStatusCode();
  }

  public int rejectTosStatus(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.rejectTosUrl());
    HttpRequest request = clientUtil.buildPutRequest(genericUrl, new EmptyContent(), authUser);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException(
          String.format("Error removing Terms of Service: %s for user %s", response.getStatusMessage(), authUser.getEmail()),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    return response.getStatusCode();
  }

  public EmailResponse getV1UserByEmail(AuthUser authUser, String email) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getV1UserUrl(email));
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException(
          "Error getting user by email from Sam: " + response.getStatusMessage(),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    String body = response.parseAsString();
    return new Gson().fromJson(body, EmailResponse.class);
  }

  /**
   * Private method to handle the general case of sending requests to Sam.
   * We inject timeouts here to prevent Sam from impacting API performance.
   * The default is 10 seconds which should be more than enough for Sam calls.
   *
   * @param request The HttpRequest
   * @return The HttpResponse
   */
  private HttpResponse executeRequest(HttpRequest request) {
    request.setConnectTimeout(connectTimeoutMilliseconds);
    request.setReadTimeout(readTimeoutMilliseconds);
    return clientUtil.handleHttpRequest(request);
  }

}
