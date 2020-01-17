package org.broadinstitute.consent.http.service.users.handler;

import org.broadinstitute.consent.http.models.DACUser;

import java.util.Map;

public interface UserHandlerAPI {

    void updateRoles(Map<String, DACUser> rec);

}
