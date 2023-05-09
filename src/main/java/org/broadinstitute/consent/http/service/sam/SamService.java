package org.broadinstitute.consent.http.service.sam;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;

import java.util.List;

public class SamService {

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
        return samDAO.postTosAcceptedStatus(authUser);
    }

    public TosResponse removeTosAcceptedStatus(AuthUser authUser) throws Exception {
        return samDAO.removeTosAcceptedStatus(authUser);
    }
}
