package org.broadinstitute.consent.http.service.users;

import com.google.inject.ImplementedBy;
import org.broadinstitute.consent.http.models.DACUser;

/**
 * @deprecated Use UserService
 */
@Deprecated
@ImplementedBy(DatabaseUserAPI.class)
public interface UserAPI {

    DACUser createUser(DACUser user);

}
