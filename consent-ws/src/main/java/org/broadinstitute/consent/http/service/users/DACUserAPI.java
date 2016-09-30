package org.broadinstitute.consent.http.service.users;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.user.ValidateDelegationResponse;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;

public interface DACUserAPI {

    DACUser createDACUser(DACUser dacUser) throws IllegalArgumentException;

    DACUser describeDACUserByEmail(String email) throws NotFoundException;

    List<DACUser> describeAdminUsersThatWantToReceiveMails();

    DACUser describeDACUserById(Integer id) throws IllegalArgumentException;

    DACUser updateDACUserById(Map<String,DACUser> dac, Integer userId) throws IllegalArgumentException, NotFoundException, UserRoleHandlerException, MessagingException, IOException, TemplateException;

    void deleteDACUser(String email) throws IllegalArgumentException, NotFoundException;

    void updateExistentChairPersonToAlumni(Integer dacUserID);

    Collection<DACUser> describeUsers();

    ValidateDelegationResponse validateNeedsDelegation(DACUser user, String role);

    DACUser updateRoleStatus(DACUserRole dACUserRole, Integer userId);

    DACUserRole getRoleStatus(Integer userId);

    DACUser updateNameById(DACUser user, Integer id);

    public boolean hasUserRole(String userRole, DACUser user);

}
