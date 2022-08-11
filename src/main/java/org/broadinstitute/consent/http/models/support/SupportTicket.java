package org.broadinstitute.consent.http.models.support;

import org.broadinstitute.consent.http.enumeration.SupportRequestType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents ticket to request support required by the Zendesk API https://broadinstitute.zendesk.com/api/v2/requests.json
 */
public class SupportTicket {

    private SupportRequest request;

    /**
     * Constructs a ticket with the proper structure to request support via Zendesk
     *
     * @param name        The name of the user requesting support
     * @param type        The type of request ("question", "incident", "problem", "task")
     * @param email       The email of the user requesting support
     * @param subject     Subject line of the request
     * @param description Description of the task or question
     * @param url         The API url of this request
     */
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
