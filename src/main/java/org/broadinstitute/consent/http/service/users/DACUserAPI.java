package org.broadinstitute.consent.http.service.users;

import freemarker.template.TemplateException;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;

import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DACUserAPI {

    DACUser createDACUser(DACUser dacUser) throws IllegalArgumentException;

    DACUser describeDACUserByEmail(String email) throws NotFoundException;

    List<DACUser> describeAdminUsersThatWantToReceiveMails();

    DACUser describeDACUserById(Integer id) throws IllegalArgumentException;

    DACUser updateDACUserById(Map<String, DACUser> dac, Integer userId) throws IllegalArgumentException, NotFoundException, UserRoleHandlerException, MessagingException, IOException, TemplateException;

    DACUser updateDACUserById(DACUser dac, Integer userId) throws IllegalArgumentException, NotFoundException;

    void deleteDACUser(String email) throws IllegalArgumentException, NotFoundException;

    Collection<DACUser> describeUsers();

    DACUser updateUserStatus(String status, Integer userId);

    DACUser updateUserRationale(String rationale, Integer userId);

    void updateEmailPreference(boolean preference, Integer userId);

}
