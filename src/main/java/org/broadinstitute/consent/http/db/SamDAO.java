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
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

public class SamDAO implements ConsentLogger {

  private final ExecutorService executorService;
  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration configuration;
  private final Integer connectTimeoutMilliseconds;
  private final SamDefaults samDefaults = new SamDefaults();
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
    Type resourceTypesListType = new TypeToken<ArrayList<ResourceType>>() {
    }.getType();
    Optional<String> body = getResponseBody(authUser, request);
    return body.isPresent() ?
        new Gson().fromJson(body.get(), resourceTypesListType) :
        List.of();
  }

  public UserStatusInfo getRegistrationInfo(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getRegisterUserV2SelfInfoUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    Optional<String> body = getResponseBody(authUser, request);
    return body.isPresent() ?
        new Gson().fromJson(body.get(), UserStatusInfo.class) :
        samDefaults.getDefaultUserStatusInfo(authUser);
  }

  public UserStatusDiagnostics getSelfDiagnostics(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getV2SelfDiagnosticsUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    Optional<String> body = getResponseBody(authUser, request);
    return body.isPresent() ?
        new Gson().fromJson(body.get(), UserStatusDiagnostics.class) :
        samDefaults.getDefaultUserStatusDiagnostics();
  }

  public UserStatus postRegistrationInfo(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.postRegisterUserV2SelfUrl());
    HttpRequest request = clientUtil.buildPostRequest(genericUrl, new EmptyContent(), authUser);
    Optional<String> body = getResponseBody(authUser, request);
    return body.isPresent() ?
        new Gson().fromJson(body.get(), UserStatus.class) :
        samDefaults.getDefaultUserStatus(authUser);
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
            logInfo("Successfully registered user in Sam: %s".formatted(authUser.getEmail()));
          }

          @Override
          public void onFailure(@Nonnull Throwable throwable) {
            logException(
                "Async Post Registration Failure for user: %s".formatted(authUser.getEmail()),
                new Exception(throwable));
          }
        },
        listeningExecutorService);
  }

  public String getToSText() throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getToSTextUrl());
    HttpRequest request = clientUtil.buildUnAuthedGetRequest(genericUrl);
    request.getHeaders().setAccept(MediaType.TEXT_PLAIN);
    Optional<String> body = getResponseBody(null, request);
    return body.isPresent() ?
        body.get() :
        samDefaults.getDefaultToSText();
  }

  public TosResponse getTosResponse(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getSelfTosUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    Optional<String> body = getResponseBody(authUser, request);
    return body.isPresent() ?
        new Gson().fromJson(body.get(), TosResponse.class) :
        samDefaults.getDefaultTosResponse();
  }

  public int acceptTosStatus(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.acceptTosUrl());
    HttpRequest request = clientUtil.buildPutRequest(genericUrl, new EmptyContent(), authUser);
    Optional<Integer> statusCode = getResponseStatusCode(request);
    return statusCode.orElseGet(samDefaults::getDefaultTosStatusCode);
  }

  public int rejectTosStatus(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.rejectTosUrl());
    HttpRequest request = clientUtil.buildPutRequest(genericUrl, new EmptyContent(), authUser);
    Optional<Integer> statusCode = getResponseStatusCode(request);
    return statusCode.orElseGet(samDefaults::getDefaultTosStatusCode);
  }

  public EmailResponse getV1UserByEmail(AuthUser authUser, String email) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getV1UserUrl(email));
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    Optional<String> body = getResponseBody(authUser, request);
    return body.isPresent() ?
        new Gson().fromJson(body.get(), EmailResponse.class) :
        samDefaults.getDefaultEmailResponse(authUser);
  }

  /**
   * Private method to handle the general case of sending requests to Sam. We inject timeouts here
   * to prevent Sam from impacting API performance. The default is 10 seconds which should be more
   * than enough for Sam calls.
   *
   * @param request The HttpRequest
   * @return The Optional response body
   */
  private Optional<String> getResponseBody(@Nullable AuthUser authUser, HttpRequest request) {
    request.setConnectTimeout(connectTimeoutMilliseconds);
    request.setReadTimeout(readTimeoutMilliseconds);
    HttpResponse response;
    try {
      response = clientUtil.handleHttpRequest(request);
      String userEmail = authUser == null ? "unknown" : authUser.getEmail();
      if (HttpStatusCodes.STATUS_CODE_CONFLICT == response.getStatusCode()) {
        ConsentConflictException e = new ConsentConflictException(
            "Conflict error, user already exists in Sam: [%s]".formatted(userEmail));
        // Conflict errors do not need to go to Sentry
        logInfo(e.getMessage());
        throw e;
      }
      if (!response.isSuccessStatusCode()) {
        String errorMsg = "Error calling Sam. Email [%s]. Status Code [%s]. Status Message [%s]".formatted(
            userEmail,
            response.getStatusCode(),
            response.getStatusMessage());
        RuntimeException e = new RuntimeException(errorMsg);
        logException(e);
        throw e;
      }
      return Optional.of(response.parseAsString());
    } catch (ServerErrorException e) {
      logException("Sam is unavailable: ", e);
    } catch (IOException e) {
      logException("Error reading response content: ", e);
    }
    return Optional.empty();
  }

  /**
   * Simpler version of the above method for the response status code.
   *
   * @param request The HttpRequest
   * @return The Optional response status code
   */
  private Optional<Integer> getResponseStatusCode(HttpRequest request) {
    request.setConnectTimeout(connectTimeoutMilliseconds);
    request.setReadTimeout(readTimeoutMilliseconds);
    HttpResponse response;
    try {
      response = clientUtil.handleHttpRequest(request);
      return Optional.of(response.getStatusCode());
    } catch (ServerErrorException e) {
      logException("Sam is unavailable: ", e);
    }
    return Optional.empty();
  }

}
