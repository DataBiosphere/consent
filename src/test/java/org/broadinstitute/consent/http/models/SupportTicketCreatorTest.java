package org.broadinstitute.consent.http.models;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.models.support.CustomRequestField;
import org.broadinstitute.consent.http.models.support.SupportTicket;
import org.broadinstitute.consent.http.models.support.SupportTicketCreator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SupportTicketCreatorTest {

    private SupportTicketCreator supportTicketCreator;

    @Mock
    private InstitutionDAO institutionDAO;

    @Mock
    private UserDAO userDAO;

    @Before
    public void setUp() {
        openMocks(this);
        this.supportTicketCreator = new SupportTicketCreator(institutionDAO, userDAO);
    }

    @Test
    public void testCreateInstitutionSOSupportTicket_SuggestedInstitution() {
        String url =  RandomStringUtils.randomAlphabetic(10);
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        String suggestedInstitution = RandomStringUtils.randomAlphabetic(10);
        UserUpdateFields updateFields = new UserUpdateFields();
        updateFields.setSuggestedInstitution(suggestedInstitution);

        SupportTicket ticket = supportTicketCreator.createInstitutionSOSupportTicket(updateFields, user, url);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName, supportRequest.getRequester().getName());
        assertEquals(email, supportRequest.getRequester().getEmail());
        assertEquals(displayName + " user updates: New Institution Request", supportRequest.getSubject());
        assertEquals(360000669472L, supportRequest.getTicketFormId());

        String expectedDescription = String.format("User %s [%s] has:\n- requested a new institution: %s",
                user.getDisplayName(),
                user.getEmail(),
                suggestedInstitution);
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360012744452L, SupportRequestType.TASK.getValue())));
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
        assertTrue(customFields.contains(new CustomRequestField(360012744292L, displayName)));
        assertTrue(customFields.contains(new CustomRequestField(360012782111L, email)));
        assertTrue(customFields.contains(new CustomRequestField(360018545031L, email)));

        String commentBody = expectedDescription + "\n\n------------------\nSubmitted from: " + url;
        assertEquals(commentBody, supportRequest.getComment().getBody());
    }

    @Test
    public void testCreateInstitutionSOSupportTicket_SuggestedSigningOfficial() {
        String url =  RandomStringUtils.randomAlphabetic(10);
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        String suggestedSigningOfficial = RandomStringUtils.randomAlphabetic(10);
        UserUpdateFields updateFields = new UserUpdateFields();
        updateFields.setSuggestedSigningOfficial(suggestedSigningOfficial);

        SupportTicket ticket = supportTicketCreator.createInstitutionSOSupportTicket(updateFields, user, url);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName + " user updates: New Signing Official Request", supportRequest.getSubject());

        String expectedDescription = String.format("User %s [%s] has:\n- requested a new signing official: %s",
                user.getDisplayName(),
                user.getEmail(),
                suggestedSigningOfficial);
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
    }

    @Test
    public void testCreateInstitutionSOSupportTicket_SelectedInstitution() {
        String url =  RandomStringUtils.randomAlphabetic(10);
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        UserUpdateFields updateFields = new UserUpdateFields();
        updateFields.setInstitutionId(1);
        Institution institution = new Institution();
        String institutionName = RandomStringUtils.randomAlphabetic(10);
        institution.setName(institutionName);

        when(institutionDAO.findInstitutionById(1)).thenReturn(institution);

        SupportTicket ticket = supportTicketCreator.createInstitutionSOSupportTicket(updateFields, user, url);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName + " user updates: Institution Selection", supportRequest.getSubject());

        String expectedDescription = String.format("User %s [%s] has:\n- selected an existing institution: %s",
                user.getDisplayName(),
                user.getEmail(),
                institutionName);
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
    }

    @Test
    public void testCreateInstitutionSOSupportTicket_SelectedInstitutionNotFound() {
        String url =  RandomStringUtils.randomAlphabetic(10);
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        UserUpdateFields updateFields = new UserUpdateFields();
        int institutionId = RandomUtils.nextInt();
        updateFields.setInstitutionId(institutionId);

        when(institutionDAO.findInstitutionById(institutionId)).thenReturn(null);

        SupportTicket ticket = supportTicketCreator.createInstitutionSOSupportTicket(updateFields, user, url);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName + " user updates: Institution Selection", supportRequest.getSubject());

        String expectedDescription = String.format("User %s [%s] has:\n- attempted to select institution with id %s (not found)",
                user.getDisplayName(),
                user.getEmail(),
                institutionId);
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
    }

    @Test
    public void testCreateInstitutionSOSupportTicket_SelectedSigningOfficial() {
        String url =  RandomStringUtils.randomAlphabetic(10);
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        UserUpdateFields updateFields = new UserUpdateFields();
        updateFields.setSelectedSigningOfficialId(1);
        User signingOfficial = new User();
        signingOfficial.setDisplayName(RandomStringUtils.randomAlphabetic(10));
        signingOfficial.setEmail(RandomStringUtils.randomAlphabetic(10));

        when(userDAO.findUserById(1)).thenReturn(signingOfficial);

        SupportTicket ticket = supportTicketCreator.createInstitutionSOSupportTicket(updateFields, user, url);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName + " user updates: Signing Official Selection", supportRequest.getSubject());

        String expectedDescription = String.format("User %s [%s] has:\n- selected an existing signing official: %s, %s",
                user.getDisplayName(),
                user.getEmail(),
                signingOfficial.getDisplayName(),
                signingOfficial.getEmail());
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360012744452L, SupportRequestType.TASK.getValue())));
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
    }

    @Test
    public void testCreateInstitutionSOSupportTicket_SelectedSigningOfficialNotFound() {
        String url =  RandomStringUtils.randomAlphabetic(10);
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        UserUpdateFields updateFields = new UserUpdateFields();
        int signingOfficialId = RandomUtils.nextInt();
        updateFields.setSelectedSigningOfficialId(signingOfficialId);

        when(userDAO.findUserById(signingOfficialId)).thenReturn(null);

        SupportTicket ticket = supportTicketCreator.createInstitutionSOSupportTicket(updateFields, user, url);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName + " user updates: Signing Official Selection", supportRequest.getSubject());

        String expectedDescription = String.format("User %s [%s] has:\n- attempted to select signing official with id %s (not found)",
                user.getDisplayName(),
                user.getEmail(),
                signingOfficialId);
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
    }

    @Test
    public void testCreateInstitutionSOSupportTicket_MultipleFields() {
        String url =  RandomStringUtils.randomAlphabetic(10);
        String displayName = RandomStringUtils.randomAlphabetic(10);
        String email = RandomStringUtils.randomAlphabetic(10);
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(email);

        UserUpdateFields updateFields = new UserUpdateFields();
        String suggestedInstitution = RandomStringUtils.randomAlphabetic(10);
        updateFields.setSuggestedInstitution(suggestedInstitution);
        String suggestedSigningOfficial = RandomStringUtils.randomAlphabetic(10);
        updateFields.setSuggestedSigningOfficial(suggestedSigningOfficial);

        SupportTicket ticket = supportTicketCreator.createInstitutionSOSupportTicket(updateFields, user, url);
        SupportTicket.SupportRequest supportRequest = ticket.getRequest();
        assertEquals(displayName + " user updates: New Institution Request, New Signing Official Request", supportRequest.getSubject());

        String expectedDescription = String.format("User %s [%s] has:\n" +
                        "- requested a new institution: %s\n" +
                        "- requested a new signing official: %s",
                user.getDisplayName(),
                user.getEmail(),
                suggestedInstitution,
                suggestedSigningOfficial);
        List<CustomRequestField> customFields = supportRequest.getCustomFields();
        assertEquals(5, customFields.size());
        assertTrue(customFields.contains(new CustomRequestField(360007369412L, expectedDescription)));
    }
}
