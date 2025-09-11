package org.broadinstitute.consent.http.service.sam;

import com.google.inject.Inject;
import java.util.List;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
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

  public List<ResourceType> getResourceTypes(DuosUser duosUser) throws Exception {
    return samDAO.getResourceTypes(duosUser);
  }

  public UserStatusInfo getRegistrationInfo(DuosUser duosUser) throws Exception {
    return samDAO.getRegistrationInfo(duosUser);
  }

  public UserStatusDiagnostics getSelfDiagnostics(DuosUser duosUser) throws Exception {
    return samDAO.getSelfDiagnostics(duosUser);
  }

  public UserStatus postRegistrationInfo(DuosUser duosUser) throws Exception {
    return samDAO.postRegistrationInfo(duosUser);
  }

  public void asyncPostRegistrationInfo(DuosUser duosUser) {
    samDAO.asyncPostRegistrationInfo(duosUser);
  }

  public String getToSText() throws Exception {
    return samDAO.getToSText();
  }

  public TosResponse postTosAcceptedStatus(DuosUser duosUser) throws Exception {
    samDAO.acceptTosStatus(duosUser);
    return samDAO.getTosResponse(duosUser);
  }

  public TosResponse removeTosAcceptedStatus(DuosUser duosUser) throws Exception {
    samDAO.rejectTosStatus(duosUser);
    return samDAO.getTosResponse(duosUser);
  }
}
