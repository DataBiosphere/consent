package org.broadinstitute.consent.http.models.support;

/**
 * Represents requester creating a ticket to request support via Zendesk <a
 * href="https://developer.zendesk.com/api-reference/ticketing/tickets/ticket-requests/">Requests
 * API Reference</a>
 */
public record SupportRequester(String name, String email) {

}
