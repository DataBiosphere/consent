package org.broadinstitute.consent.http.service;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.ecm.LinkInfo;
import org.broadinstitute.consent.http.service.dao.NihServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class NihService implements ConsentLogger {

  private final UserDAO userDAO;
  private final UserPropertyDAO userPropertyDAO;
  private final NihServiceDAO serviceDAO;
  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration configuration;

  @Inject
  public NihService(UserDAO userDAO, UserPropertyDAO userPropertyDAO, NihServiceDAO serviceDAO,
      HttpClientUtil clientUtil, ServicesConfiguration configuration) {
    this.userDAO = userDAO;
    this.userPropertyDAO = userPropertyDAO;
    this.serviceDAO = serviceDAO;
    this.clientUtil = clientUtil;
    this.configuration = configuration;
  }

  public User syncAccount(AuthUser authUser) throws Exception {
    User user = userDAO.findUserByEmail(authUser.getEmail());
    GenericUrl ecmRasProviderUrl = new GenericUrl(configuration.getEcmRasProviderUrl());
    HttpRequest request = clientUtil.buildGetRequest(ecmRasProviderUrl, authUser);
    try {
      HttpResponse response = clientUtil.handleHttpRequest(request);
      if (!response.isSuccessStatusCode()) {
        throw new ServerErrorException(response.getStatusMessage(), response.getStatusCode());
      }
      String body = response.parseAsString();
      LinkInfo linkInfo = parseLinkInfo(body);
      NIHUserAccount nihAccount = new NIHUserAccount(
          linkInfo.externalUserId(), linkInfo.expirationTimestamp(), linkInfo.authenticated());
      serviceDAO.updateUserNihStatus(user, nihAccount);
    } catch (NotFoundException e) {
      deleteNihAccountById(user.getUserId());
    }
    return userDAO.findUserWithPropertiesById(user.getUserId(), UserFields.getValues());
  }

  public void validateNihUserAccount(NIHUserAccount nihAccount, AuthUser authUser)
      throws BadRequestException {
    if (Objects.isNull(nihAccount) || Objects.isNull(nihAccount.getEraExpiration())) {
      logWarn("Invalid NIH Account for user: " + authUser.getEmail());
      throw new BadRequestException("Invalid NIH Authentication for user : " + authUser.getEmail());
    }
  }

  public List<UserProperty> authenticateNih(NIHUserAccount nihAccount, AuthUser authUser,
      Integer userId) throws BadRequestException {
    // fail fast
    validateNihUserAccount(nihAccount, authUser);
    User user = userDAO.findUserById(userId);
    if (Objects.isNull(user)) {
      throw new NotFoundException("User not found: " + authUser.getEmail());
    }
    if (StringUtils.isNotEmpty(nihAccount.getNihUsername()) && !nihAccount.getNihUsername()
        .isEmpty()) {
      nihAccount.setEraExpiration(generateEraExpirationDates());
      nihAccount.setStatus(true);
      try {
        serviceDAO.updateUserNihStatus(user, nihAccount);
      } catch (IllegalArgumentException e) {
        logException(e);
      }
      return userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(userId,
          UserFields.getValues());
    } else {
      throw new BadRequestException("Invalid NIH UserName for user : " + authUser.getEmail());
    }
  }

  public void deleteNihAccountById(Integer userId) {
    serviceDAO.deleteNihAccountById(userId);
  }


  private String generateEraExpirationDates() {
    Date currentDate = new Date();
    Calendar c = Calendar.getInstance();
    c.setTime(currentDate);
    c.add(Calendar.DATE, 30);
    Date expires = c.getTime();
    return String.valueOf(expires.getTime());
  }

  private LinkInfo parseLinkInfo(String body) {
    try {
      return GsonUtil.getInstance().fromJson(body, LinkInfo.class);
    } catch (Exception e) {
      logWarn("Failed to parse ECM response: " + body);
      throw new ServerErrorException("Invalid response from ECM RAS Provider",
          HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
    }
  }
}
