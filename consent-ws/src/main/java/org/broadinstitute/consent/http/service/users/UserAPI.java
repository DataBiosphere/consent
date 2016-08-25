package org.broadinstitute.consent.http.service.users;

import com.google.inject.ImplementedBy;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.dto.PatchOperation;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;

import java.util.List;

@ImplementedBy(DatabaseUserAPI.class)
public interface UserAPI {

    DACUser createUser(DACUser user, String userEmail);

    DACUser updateUser(DACUser userToUpdate, String userEmail) throws IllegalArgumentException, UserRoleHandlerException;

    DACUser updatePartialUser(List<PatchOperation> userToUpdate, String name) throws IllegalArgumentException, UserRoleHandlerException;
}
