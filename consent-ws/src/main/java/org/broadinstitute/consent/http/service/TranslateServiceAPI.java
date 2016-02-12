package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.grammar.UseRestriction;

import javax.ws.rs.client.Client;
import java.io.IOException;


public interface TranslateServiceAPI {


    String translate(String translateFor, UseRestriction useRestriction) throws IOException;

    String translateAsHtml(String translateFor, UseRestriction useRestriction) throws IOException;

    void setClient(Client client);

}
