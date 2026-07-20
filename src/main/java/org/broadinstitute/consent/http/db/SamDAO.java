package org.broadinstitute.consent.http.db;

import com.google.api.client.http.EmptyContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.exceptions.SamAzureB2CException;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.sam.CombinedState;
import org.broadinstitute.consent.http.models.sam.EmailResponse;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SamDAO implements ConsentLogger {

  private final ExecutorService executorService;
  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration configuration;
  private final Integer connectTimeoutMilliseconds;
  public final Integer readTimeoutMilliseconds;
  private final Gson gson = GsonUtil.getInstance();

  public SamDAO(
      HttpClientUtil clientUtil,
      ServicesConfiguration configuration,
      ExecutorService executorService) {
    this.clientUtil = clientUtil;
    this.configuration = configuration;
    this.executorService = executorService;
    // Defaults to 10 seconds
    this.connectTimeoutMilliseconds = configuration.getTimeoutSeconds() * 1000;
    // Defaults to 60 seconds
    this.readTimeoutMilliseconds = configuration.getTimeoutSeconds() * 6000;
  }

  public List<ResourceType> getResourceTypes(DuosUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getV1ResourceTypesUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException(
          "Error getting resource types from Sam: " + response.getStatusMessage(),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    String body = response.parseAsString();
    Type resourceTypesListType = new TypeToken<ArrayList<ResourceType>>() {}.getType();
    return gson.fromJson(body, resourceTypesListType);
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
    return gson.fromJson(body, UserStatusInfo.class);
  }

  public UserStatusDiagnostics getSelfDiagnostics(DuosUser duosUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getV2SelfDiagnosticsUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, duosUser);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException(
          "Error getting enabled statuses of user from Sam: " + response.getStatusMessage(),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    String body = response.parseAsString();
    return gson.fromJson(body, UserStatusDiagnostics.class);
  }

  public UserStatus postRegistrationInfo(DuosUser duosUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.postRegisterUserV2SelfUrl());
    HttpRequest request = clientUtil.buildPostRequest(genericUrl, new EmptyContent(), duosUser);
    HttpResponse response = executeRequest(request);
    String body = response.parseAsString();
    if (!response.isSuccessStatusCode()) {
      var errorMsg = getErrorMessage(duosUser, body);
      Exception e = new WebApplicationException(errorMsg, response.getStatusCode());
      logException(errorMsg, new Exception(body));
      throw e;
    }
    return new Gson().fromJson(body, UserStatus.class);
  }

  public UserStatusInfo getCombinedUserStatusInfo(AuthUser authUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getCombinedStateUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, authUser);
    try {
      HttpResponse response = executeRequest(request);
      String body = response.parseAsString();
      if (!response.isSuccessStatusCode()) {
        if (body.toLowerCase().contains("cannot update azureb2cid for user")) {
          throw new SamAzureB2CException(
              String.format(
                  "AzureB2C authentication Error for user %s. Please contact support for help with this error.",
                  authUser.getEmail()));
        }
        String errorMsg =
            String.format(
                "Error getting combined user status info for user %s: %s",
                authUser.getEmail(), body);
        Exception e = new WebApplicationException(errorMsg, response.getStatusCode());
        logException(errorMsg, new Exception(body));
        throw e;
      }
      CombinedState combinedState = gson.fromJson(body, CombinedState.class);
      var tosDetails = combinedState.getTermsOfServiceDetails();
      boolean tosAccepted = false;
      if (tosDetails != null) {
        tosAccepted =
            Boolean.TRUE.equals(tosDetails.permitsSystemUsage())
                && Boolean.TRUE.equals(tosDetails.isCurrentVersion());
      }
      return new UserStatusInfo()
          .setUserEmail(combinedState.getSamUser().email())
          .setUserSubjectId(combinedState.getSamUser().googleSubjectId())
          .setEnabled(combinedState.getSamUser().enabled())
          // Ensure that the user has both accepted the ToS and that it is the most recent version.
          .setTosAccepted(tosAccepted);
    } catch (ForbiddenException e) {
      // Sam throws a 403, not a 404, when the user is not found at this API
      // which is re-thrown in executeRequest
      throw new NotFoundException(
          String.format("User %s not found in Sam: %s", authUser.getEmail(), e.getMessage()), e);
    }
  }

  public static String getErrorMessage(DuosUser duosUser, String body) {
    var errorMsg =
        String.format(
            "Error posting user registration information. Email: %s.", duosUser.getEmail());
    if (body == null || body.isEmpty()) {
      return errorMsg;
    }
    try {
      JsonElement messageElement = JsonParser.parseString(body).getAsJsonObject().get("message");
      String message = messageElement != null ? messageElement.getAsString() : body;
      if (message.contains("Cannot update azureB2cId")) {
        return String.format(
            "Email: %s. You may have previously signed in with a different authentication provider (Google or Microsoft). Please sign in with that provider. For more information visit: https://support.terra.bio/hc/en-us/community/posts/24089648317467-Cannot-update-azureB2cId-for-user",
            duosUser.getEmail());
      }
      return String.format(errorMsg + " %s.", message);
    } catch (JsonSyntaxException e) { // If the body is not a valid JSON
      return String.format(errorMsg + " %s.", body);
    }
  }

  public void asyncPostRegistrationInfo(DuosUser duosUser) {
    ListeningExecutorService listeningExecutorService =
        MoreExecutors.listeningDecorator(executorService);
    ListenableFuture<UserStatus> userStatusFuture =
        listeningExecutorService.submit(() -> postRegistrationInfo(duosUser));
    Futures.addCallback(
        userStatusFuture,
        new FutureCallback<>() {
          @Override
          public void onSuccess(@Nullable UserStatus userStatus) {
            logInfo("Successfully registered user in Sam: " + duosUser.getEmail());
          }

          @Override
          public void onFailure(@NonNull Throwable throwable) {
            logWarn(
                "Async Post Registration Failure for user: "
                    + duosUser.getEmail()
                    + "; "
                    + throwable.getMessage());
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
      logException(
          "Error getting Terms of Service text from Sam: " + response.getStatusMessage(),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    return response.parseAsString();
  }

  public TosResponse getTosResponse(DuosUser duosUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getSelfTosUrl());
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, duosUser);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException(
          String.format(
              "Error getting Terms of Service: %s for user %s",
              response.getStatusMessage(), duosUser.getEmail()),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    String body = response.parseAsString();
    return new Gson().fromJson(body, TosResponse.class);
  }

  public int acceptTosStatus(DuosUser duosUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.acceptTosUrl());
    HttpRequest request = clientUtil.buildPutRequest(genericUrl, new EmptyContent(), duosUser);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException(
          String.format(
              "Error accepting Terms of Service: %s for user %s",
              response.getStatusMessage(), duosUser.getEmail()),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    return response.getStatusCode();
  }

  public int rejectTosStatus(DuosUser duosUser) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.rejectTosUrl());
    HttpRequest request = clientUtil.buildPutRequest(genericUrl, new EmptyContent(), duosUser);
    HttpResponse response = executeRequest(request);
    if (!response.isSuccessStatusCode()) {
      logException(
          String.format(
              "Error removing Terms of Service: %s for user %s",
              response.getStatusMessage(), duosUser.getEmail()),
          new ServerErrorException(response.getStatusMessage(), response.getStatusCode()));
    }
    return response.getStatusCode();
  }

  public EmailResponse getV1UserByEmail(DuosUser duosUser, String email) throws Exception {
    GenericUrl genericUrl = new GenericUrl(configuration.getV1UserUrl(email));
    HttpRequest request = clientUtil.buildGetRequest(genericUrl, duosUser);
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
   * Private method to handle the general case of sending requests to Sam. We inject timeouts here
   * to prevent Sam from impacting API performance. The default is 10 seconds which should be more
   * than enough for Sam calls.
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
