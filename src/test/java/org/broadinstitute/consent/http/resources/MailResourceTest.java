package org.broadinstitute.consent.http.resources;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.broadinstitute.consent.http.service.EmailService;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class MailResourceTest {
    @Mock
    private EmailService emailService;
    private final AuthUser authUser = new AuthUser("test@test.com");

    private MailResource mailResource;

    private void initResource() {
        openMocks(this);
        mailResource = new MailResource(emailService);
    }

    @Test
    public void test_MailResource() {
        initResource();
        when(emailService.fetchEmailMessages(any(), any(), any())).thenReturn(generateMailMessageList());
        Response response = mailResource.getEmailByType(authUser, EmailType.COLLECT,null, null);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void test_MailResourceEmptyListResponse() {
        initResource();
        when(emailService.fetchEmailMessages(any(), any(), any())).thenReturn(new ArrayList<>());
        Response response = mailResource.getEmailByType(authUser, EmailType.COLLECT,null, null);
        assertEquals(200, response.getStatus());
    }

    private List<MailMessage> generateMailMessageList() {
        List<MailMessage> messageList = new ArrayList<>();
        EnumSet.allOf(EmailType.class).forEach(t ->
                messageList.add(generateMailMessage(t.toString())));
        return messageList;
    }
    private MailMessage generateMailMessage(String emailType) {
        return new MailMessage(
                RandomUtils.nextInt(),
                RandomUtils.nextInt(),
                RandomUtils.nextInt(),
                emailType,
                new Date(),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(),
                new Date()
        );
    }
}
