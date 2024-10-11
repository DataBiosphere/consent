package org.broadinstitute.consent.http.db;

import com.google.api.client.http.HttpStatusCodes;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.EmailResponse;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;

/**
 * This class represents default Sam objects that can be used when Sam is unavailable
 */
public class SamDefaults {

  public UserStatusInfo getDefaultUserStatusInfo(AuthUser authUser) {
    UserStatusInfo userStatusInfo = new UserStatusInfo();
    userStatusInfo.setAdminEnabled(false);
    userStatusInfo.setEnabled(true);
    userStatusInfo.setUserEmail(authUser.getEmail());
    userStatusInfo.setUserSubjectId(authUser.getEmail());
    return userStatusInfo;
  }

  public UserStatusDiagnostics getDefaultUserStatusDiagnostics() {
    UserStatusDiagnostics userStatusDiagnostics = new UserStatusDiagnostics();
    userStatusDiagnostics.setAdminEnabled(false);
    userStatusDiagnostics.setEnabled(true);
    userStatusDiagnostics.setTosAccepted(true);
    userStatusDiagnostics.setInAllUsersGroup(true);
    userStatusDiagnostics.setInGoogleProxyGroup(true);
    return userStatusDiagnostics;
  }

  public UserStatus getDefaultUserStatus(AuthUser authUser) {
    UserStatus userStatus = new UserStatus();
    UserStatus.Enabled enabled = new UserStatus.Enabled();
    enabled.setLdap(true);
    enabled.setAllUsersGroup(true);
    enabled.setGoogle(true);
    UserStatus.UserInfo userInfo = new UserStatus.UserInfo();
    userInfo.setUserEmail(authUser.getEmail());
    userInfo.setUserSubjectId(authUser.getEmail());
    userStatus.setEnabled(enabled);
    userStatus.setUserInfo(userInfo);
    return userStatus;
  }

  public String getDefaultToSText() throws Exception {
    try (InputStream is = this.getClass().getResourceAsStream("/tos.txt")) {
      if (is != null) {
        return IOUtils.toString(is, Charset.defaultCharset());
      }
      return "Terms of Service";
    }
  }

  public TosResponse getDefaultTosResponse() {
    return new TosResponse(
        new Date().toString(),
        true,
        "2023-11-15",
        true);
  }

  public int getDefaultTosStatusCode() {
    return HttpStatusCodes.STATUS_CODE_NO_CONTENT;
  }

  public EmailResponse getDefaultEmailResponse(AuthUser authUser) {
    return new EmailResponse(
        authUser.getEmail(),
        authUser.getEmail(),
        authUser.getEmail());
  }

}
