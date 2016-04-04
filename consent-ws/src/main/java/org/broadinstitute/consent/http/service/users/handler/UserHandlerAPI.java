package org.broadinstitute.consent.http.service.users.handler;

import java.util.Map;
import org.broadinstitute.consent.http.models.DACUser;

public interface UserHandlerAPI {

    /**
     *
     * @param rec
     * @throws UserRoleHandlerException
     */
    void updateRoles(Map<String, DACUser> rec) throws UserRoleHandlerException;
}
