package org.broadinstitute.consent.http.service.users;

import com.google.inject.ImplementedBy;
import org.broadinstitute.consent.http.models.User;

/**
 * @deprecated Use UserService
 */
@Deprecated
@ImplementedBy(DatabaseUserAPI.class)
public interface UserAPI {

    User createUser(User user);

}
