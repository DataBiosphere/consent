package org.broadinstitute.consent.http.service;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.support.CustomRequestField;
import org.broadinstitute.consent.http.models.support.SupportRequestComment;
import org.broadinstitute.consent.http.models.support.SupportRequester;
import org.broadinstitute.consent.http.models.support.SupportTicket;
import org.broadinstitute.consent.http.util.HttpClientUtil;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SupportRequestService {

    private final InstitutionDAO institutionDAO;
    private final UserDAO userDAO;

    private final HttpClientUtil clientUtil;
    private final ServicesConfiguration configuration;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Inject
    public SupportRequestService(ServicesConfiguration configuration, InstitutionDAO institutionDAO, UserDAO userDAO) {
        this.institutionDAO = institutionDAO;
        this.userDAO = userDAO;
        this.clientUtil = new HttpClientUtil();
        this.configuration = configuration;
    }

    /**
     * Builds a ticket with the proper structure to request support via Zendesk
     *
     * @param name        The name of the user requesting support
     * @param type        The type of request ("question", "incident", "problem", "task")
     * @param email       The email of the user requesting support
     * @param subject     Subject line of the request
     * @param description Description of the task or question
     * @param url         The API url of this request
     * @return A structured ticket used to make the request
     */
    public SupportTicket createSupportTicket(String name, SupportRequestType type, String email, String subject, String description, String url) {
        if (Objects.isNull(name) || Objects.isNull(email)) {
            throw new IllegalArgumentException("Name and email of user requesting support is required");
        }
        if (Objects.isNull(subject)) {
            throw new IllegalArgumentException("Support ticket subject is required");
        }
        if (Objects.isNull(description)) {
            throw new IllegalArgumentException("Support ticket description is required");
        }
        if (Objects.isNull(type)) {
            throw new IllegalArgumentException("Support ticket type is required");
        }
        if (Objects.isNull(url)) {
            throw new IllegalArgumentException("Support ticket url is required");
        }

        SupportRequester requester = new SupportRequester(name, email);
        List<CustomRequestField> customFields = new ArrayList<>();
        customFields.add(new CustomRequestField(360012744452L, type.getValue()));
        customFields.add(new CustomRequestField(360007369412L, description));
        customFields.add(new CustomRequestField(360012744292L, name));
        customFields.add(new CustomRequestField(360012782111L, email));
        customFields.add(new CustomRequestField(360018545031L, email));
        SupportRequestComment comment = new SupportRequestComment(description + "\n\n------------------\nSubmitted from: " + url);

        return new SupportTicket(requester, subject, customFields, comment);
    }

    public void postTicketToSupport(SupportTicket ticket) throws Exception {
        if (configuration.isActivateSupportNotifications()) {
            GenericUrl genericUrl = new GenericUrl(configuration.postSupportRequestUrl());
            //Using GsonBuilder directly to convert ticket to json since GsonFactory does not allow custom FieldNamingPolicy
            String ticketJson = new GsonBuilder()
                    .setPrettyPrinting()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create()
                    .toJson(ticket);
            //GsonFactory doesn't do more work on the ticket but an HttpContent object is needed for buildPostRequest
            ByteArrayContent content = new ByteArrayContent("application/json", ticketJson.getBytes(StandardCharsets.UTF_8));
            HttpRequest request = clientUtil.buildUnAuthedPostRequest(genericUrl, content);
            HttpResponse response = clientUtil.handleHttpRequest(request);

            if (response.getStatusCode() != HttpStatusCodes.STATUS_CODE_CREATED) {
                logger.error("Error posting ticket to support: " + response.getStatusMessage());
                throw new ServerErrorException(response.getStatusMessage(), HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
            }
        } else {
            logger.debug("Not configured to send support requests");
        }
    }


    /**
     * Creates and sends a support ticket for a user requesting an unfamiliar institution and/or signing official, if provided
     *
     * @param userUpdateFields A UserUpdateFields object containing update information for the user
     * @param user             The user requesting the institution and/or signing official
     */
    public void handleSuggestedUserFieldsSupportRequest(UserUpdateFields userUpdateFields, User user) {
        if (Objects.nonNull(userUpdateFields) && Objects.nonNull(user)) {
            String suggestedInstitution = userUpdateFields.getSuggestedInstitution();
            String suggestedSigningOfficial = userUpdateFields.getSuggestedSigningOfficial();

            // Only create and send ticket if either a suggestedInstitution or suggestedSigningOfficial has been provided
            if (Objects.nonNull(suggestedInstitution) || Objects.nonNull(suggestedSigningOfficial)) {
                try {
                    SupportTicket ticket = createSuggestedUserFieldsTicket(userUpdateFields, user);
                    postTicketToSupport(ticket);
                } catch (Exception e) {
                    logger.error("Exception sending suggested user fields support request: " + e.getMessage());
                    throw new ServerErrorException("Unable to send support ticket for user with email: " + user.getEmail(),
                            HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
                }
            }
        }
    }



    /**
     * Creates and sends a support ticket for a user selecting an existing or requesting an unfamiliar institution
     * and/or signing official, if provided
     *
     * @param userUpdateFields A UserUpdateFields object containing update information for the user
     * @param user             The user requesting the institution and/or signing official
     */
    public void handleInstitutionSOSupportRequest(UserUpdateFields userUpdateFields, User user) {
        if (Objects.nonNull(userUpdateFields) && Objects.nonNull(user)) {
            boolean updateFieldProvided = Objects.nonNull(userUpdateFields.getSuggestedInstitution())
                    || Objects.nonNull(userUpdateFields.getInstitutionId())
                    || Objects.nonNull(userUpdateFields.getSuggestedSigningOfficial())
                    || Objects.nonNull(userUpdateFields.getSelectedSigningOfficialId());

            //only send ticket if an institution or signing official is provided; ignore otherwise
            if (updateFieldProvided) {
                try {
                    SupportTicket ticket = createInstitutionSOSupportTicket(userUpdateFields, user);
                    postTicketToSupport(ticket);
                } catch (Exception e) {
                    logger.error("Exception sending suggested user fields support request: " + e.getMessage());
                    throw new ServerErrorException("Unable to send support ticket for user with email: " + user.getEmail(),
                            HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
                }
            }
        }
    }

    /**
     * Generates a support ticket for a user selecting an existing or requesting an unfamiliar institution and/or signing official
     *
     * @param userUpdateFields A UserUpdateFields object containing update information for the user
     * @param user             The user requesting the institution and/or signing official
     * @return A support ticket detailing the requested user fields
     */
    public SupportTicket createInstitutionSOSupportTicket(UserUpdateFields userUpdateFields, User user) {
        String suggestedInstitution = userUpdateFields.getSuggestedInstitution();
        Integer selectedInstitutionId = userUpdateFields.getInstitutionId();
        String suggestedSigningOfficial = userUpdateFields.getSuggestedSigningOfficial();
        Integer selectedSigningOfficialId = userUpdateFields.getSelectedSigningOfficialId();

        //Generate subject and description of ticket based on provided fields
        List<String> subjectItems = new ArrayList<>();
        List<String> descriptionItems = new ArrayList<>();
        if (Objects.nonNull(suggestedInstitution)) {
            subjectItems.add("New Institution Suggestion");
            descriptionItems.add("- suggested a new institution: " + suggestedInstitution);
        }
        if (Objects.nonNull(selectedInstitutionId)) {
            Institution selectedInstitution = institutionDAO.findInstitutionById(selectedInstitutionId);
            subjectItems.add("Institution Selection");
            descriptionItems.add(
                    Objects.nonNull(selectedInstitution)
                            ? "- selected an existing institution: " + selectedInstitution.getName()
                            : "- attempted to select institution with id " + selectedInstitutionId + " (not found)"
            );
        }
        if (Objects.nonNull(suggestedSigningOfficial)) {
            subjectItems.add("New Signing Official Suggestion");
            descriptionItems.add("- suggested a new signing official: " + suggestedSigningOfficial);
        }
        if (Objects.nonNull(selectedSigningOfficialId)) {
            User selectedSigningOfficial = userDAO.findUserById(selectedSigningOfficialId);
            subjectItems.add("Signing Official Selection");
            descriptionItems.add(
                    Objects.nonNull(selectedSigningOfficial)
                            ? String.format("- selected an existing signing official: %s, %s",
                            selectedSigningOfficial.getDisplayName(),
                            selectedSigningOfficial.getEmail())
                            : "- attempted to select signing official with id " + selectedSigningOfficialId + " (not found)"
            );
        }

        //Append items for ticket subject and description
        String subject = String.format("%s user updates: %s",
                user.getDisplayName(),
                String.join(", ", subjectItems));
        String description = String.format("User %s [%s] has:\n%s",
                user.getDisplayName(),
                user.getEmail(),
                String.join("\n", descriptionItems));

        return createSupportTicket(
                user.getDisplayName(),
                SupportRequestType.TASK,
                user.getEmail(),
                subject,
                description,
                configuration.postSupportRequestUrl());
    }



    /**
     * Generates a support ticket for a user requesting an unfamiliar institution and/or signing official
     *
     * @param userUpdateFields A UserUpdateFields object containing update information for the user
     * @param user             The user requesting the institution and/or signing official
     * @return A support ticket detailing the requested user fields
     * @throws IllegalArgumentException if both suggestedInstitution and suggestedSigningOfficial are null, preventing ticket creation
     */
    public SupportTicket createSuggestedUserFieldsTicket(UserUpdateFields userUpdateFields, User user) throws IllegalArgumentException {
        String suggestedInstitution = userUpdateFields.getSuggestedInstitution();
        String suggestedSigningOfficial = userUpdateFields.getSuggestedSigningOfficial();
        String subject = null;
        String description = null;

        if (Objects.nonNull(suggestedInstitution) && Objects.nonNull(suggestedSigningOfficial)) {
            subject = "New Institution and Signing Official Request";
            description = String.format("User %s [%s] has requested a new signing official: {%s} and has requested a new institution: {%s}",
                    user.getDisplayName(),
                    user.getEmail(),
                    suggestedSigningOfficial,
                    suggestedInstitution);
        } else if (Objects.nonNull(suggestedInstitution)) {
            subject = "New Institution Request";
            description = String.format("User %s [%s] has requested a new institution: {%s}",
                    user.getDisplayName(),
                    user.getEmail(),
                    suggestedInstitution);
        } else if (Objects.nonNull(suggestedSigningOfficial)) {
            subject = "New Signing Official Request";
            description = String.format("User %s [%s] has requested a new signing official: {%s}",
                    user.getDisplayName(),
                    user.getEmail(),
                    suggestedSigningOfficial);
        }

        return createSupportTicket(
                user.getDisplayName(),
                SupportRequestType.TASK,
                user.getEmail(),
                subject,
                description,
                configuration.postSupportRequestUrl());
    }
}
