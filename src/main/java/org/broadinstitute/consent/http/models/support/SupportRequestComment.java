package org.broadinstitute.consent.http.models.support;

import java.util.List;

/**
 * Represents the supported fields in a Request Comment describing an issue for a Zendesk support
 * ticket <a
 * href="https://developer.zendesk.com/api-reference/ticketing/tickets/ticket-requests/#request-comments">Requests
 * Comments API Reference</a>
 */
public record SupportRequestComment(String body, List<String> uploads) {

}
