package org.broadinstitute.consent.http.models.supportticket;

import java.util.List;

public class SupportTicket {

    private SupportRequest request;

    public SupportTicket(SupportRequester requester, String subject, List<CustomRequestField> customFields, SupportRequestComment comment) {
        this.request = new SupportRequest(requester, subject, customFields, comment);
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
        private final String ticketFormId = "360000669472";


        public SupportRequest(SupportRequester requester, String subject, List<CustomRequestField> customFields, SupportRequestComment comment) {
            this.requester = requester;
            this.subject = subject;
            this.customFields = customFields;
            this.comment = comment;
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

    }
}
