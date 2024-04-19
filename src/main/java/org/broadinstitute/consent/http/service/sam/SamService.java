package org.broadinstitute.consent.http.service.sam;

import com.google.inject.Inject;
import java.util.List;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class SamService implements ConsentLogger {

  private final SamDAO samDAO;

  @Inject
  public SamService(SamDAO samDAO) {
    this.samDAO = samDAO;
  }

  public List<ResourceType> getResourceTypes(AuthUser authUser) throws Exception {
    return samDAO.getResourceTypes(authUser);
  }

  public UserStatusInfo getRegistrationInfo(AuthUser authUser) throws Exception {
    return samDAO.getRegistrationInfo(authUser);
  }

  public UserStatusDiagnostics getSelfDiagnostics(AuthUser authUser) throws Exception {
    return samDAO.getSelfDiagnostics(authUser);
  }

  public UserStatus postRegistrationInfo(AuthUser authUser) throws Exception {
    return samDAO.postRegistrationInfo(authUser);
  }

  public void asyncPostRegistrationInfo(AuthUser authUser) {
    samDAO.asyncPostRegistrationInfo(authUser);
  }

  public String getToSText() throws Exception {
    return samDAO.getToSText();
  }

  public TosResponse postTosAcceptedStatus(AuthUser authUser) throws Exception {
    samDAO.acceptTosStatus(authUser);
    return samDAO.getTosResponse(authUser);
  }

  public TosResponse removeTosAcceptedStatus(AuthUser authUser) throws Exception {
    samDAO.rejectTosStatus(authUser);
    return samDAO.getTosResponse(authUser);
  }
}
