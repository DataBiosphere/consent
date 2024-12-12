package org.broadinstitute.consent.http.models.support;

/**
 * Represents custom fields in a Zendesk support ticket <a
 * href="https://developer.zendesk.com/api-reference/ticketing/tickets/ticket-requests/">Requests
 * API Reference</a>
 */
public record CustomRequestField(long id, String value) {

}
