package org.broadinstitute.consent.http.service.users;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.user.ValidateDelegationResponse;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;

public interface DACUserAPI {

    User updateDACUserById(Map<String, User> dac, Integer userId) throws IllegalArgumentException, NotFoundException, UserRoleHandlerException, MessagingException, IOException, TemplateException;

    User updateDACUserById(User dac, Integer userId) throws IllegalArgumentException, NotFoundException;

    void deleteDACUser(String email) throws IllegalArgumentException, NotFoundException;

    void updateExistentChairPersonToAlumni(Integer dacUserID);

    ValidateDelegationResponse validateNeedsDelegation(User user, String role);

    User updateRoleStatus(UserRole dACUserRole, Integer userId);

    UserRole getRoleStatus(Integer userId);

    User updateNameById(User user, Integer id);

}
