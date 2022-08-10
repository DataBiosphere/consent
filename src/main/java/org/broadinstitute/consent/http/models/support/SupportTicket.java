package org.broadinstitute.consent.http.models.support;

import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserUpdateFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents ticket to request support required by the Zendesk API https://broadinstitute.zendesk.com/api/v2/requests.json
 */
public class SupportTicket {

    private SupportRequest request;

    public SupportTicket(SupportRequester requester, String subject, List<CustomRequestField> customFields, SupportRequestComment comment) {
        this.request = new SupportRequest(requester, subject, customFields, comment);
    }

    public SupportTicket(String name, SupportRequestType type, String email, String subject, String description, String url) {
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

        this.request = new SupportRequest(name, type, email, subject, description, url);
    }

    public SupportTicket createInstitutionSOSupportTicket(UserUpdateFields userUpdateFields, User user, String url) {
        String suggestedInstitution = userUpdateFields.getSuggestedInstitution();
        Integer selectedInstitutionId = userUpdateFields.getInstitutionId();
        String suggestedSigningOfficial = userUpdateFields.getSuggestedSigningOfficial();
        Integer selectedSigningOfficialId = userUpdateFields.getSelectedSigningOfficialId();

        //Generate subject and description of ticket based on provided fields
        List<String> subjectItems = new ArrayList<>();
        List<String> descriptionItems = new ArrayList<>();
        if (Objects.nonNull(suggestedInstitution)) {
            subjectItems.add("New Institution Request");
            descriptionItems.add("- requested a new institution: " + suggestedInstitution);
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
            subjectItems.add("New Signing Official Request");
            descriptionItems.add("- requested a new signing official: " + suggestedSigningOfficial);
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

        return new SupportTicket(
                user.getDisplayName(),
                SupportRequestType.TASK,
                user.getEmail(),
                subject,
                description,
                url);
    }


    public SupportRequest getRequest() {
        return request;
    }

    public void setRequest(SupportRequest request) {
        this.request = request;
    }


    public static class SupportRequest {

        private SupportRequester requester;
        private String subject;
        private List<CustomRequestField> customFields;
        private SupportRequestComment comment;
        private final long ticketFormId = 360000669472L;


        public SupportRequest(SupportRequester requester, String subject, List<CustomRequestField> customFields, SupportRequestComment comment) {
            this.requester = requester;
            this.subject = subject;
            this.customFields = customFields;
            this.comment = comment;
        }

        public SupportRequest(String name, SupportRequestType type, String email, String subject, String description, String url) {
            this.requester = new SupportRequester(name, email);
            this.subject = subject;
            this.customFields = createCustomFields(name, type, email, description);
            this.comment = new SupportRequestComment(description + "\n\n------------------\nSubmitted from: " + url);
        }

        public SupportRequester getRequester() {
            return requester;
        }

        public void setRequester(SupportRequester requester) {
            this.requester = requester;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public List<CustomRequestField> getCustomFields() {
            return customFields;
        }

        public void setCustomFields(List<CustomRequestField> customFields) {
            this.customFields = customFields;
        }

        public SupportRequestComment getComment() {
            return comment;
        }

        public void setComment(SupportRequestComment comment) {
            this.comment = comment;
        }

        public long getTicketFormId() {
            return ticketFormId;
        }

        private List<CustomRequestField> createCustomFields(String name, SupportRequestType type, String email, String description) {
            List<CustomRequestField> customFields = new ArrayList<>();
            customFields.add(new CustomRequestField(360012744452L, type.getValue()));
            customFields.add(new CustomRequestField(360007369412L, description));
            customFields.add(new CustomRequestField(360012744292L, name));
            customFields.add(new CustomRequestField(360012782111L, email));
            customFields.add(new CustomRequestField(360018545031L, email));
            return customFields;
        }
    }
}
