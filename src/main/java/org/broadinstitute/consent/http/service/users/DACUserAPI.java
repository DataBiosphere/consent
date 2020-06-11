package org.broadinstitute.consent.http.service.users;

import freemarker.template.TemplateException;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;

import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @deprecated Use UserService
 */
@Deprecated
public interface DACUserAPI {

    User createDACUser(User user) throws IllegalArgumentException;

    List<User> describeAdminUsersThatWantToReceiveMails();

    User updateDACUserById(Map<String, User> dac, Integer userId) throws IllegalArgumentException, NotFoundException, UserRoleHandlerException, MessagingException, IOException, TemplateException;

    User updateUserStatus(String status, Integer userId);

    User updateUserRationale(String rationale, Integer userId);

    void updateEmailPreference(boolean preference, Integer userId);

}
