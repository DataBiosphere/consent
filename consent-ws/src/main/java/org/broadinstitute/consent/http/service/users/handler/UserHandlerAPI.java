package org.broadinstitute.consent.http.service.users.handler;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.Map;
import javax.mail.MessagingException;
import org.broadinstitute.consent.http.models.DACUser;

public interface UserHandlerAPI {

    void updateRoles(Map<String, DACUser> rec) throws UserRoleHandlerException, MessagingException, IOException, TemplateException;

}
