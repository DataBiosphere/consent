package org.broadinstitute.consent.http.service;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.SupportRequestTicket;
import org.broadinstitute.consent.http.models.supportticket.SupportTicket;
import org.broadinstitute.consent.http.util.HttpClientUtil;

import javax.ws.rs.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupportRequestService {

    private final HttpClientUtil clientUtil;
    private final ServicesConfiguration configuration;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public SupportRequestService(ServicesConfiguration configuration) {
        this.clientUtil = new HttpClientUtil();
        this.configuration = configuration;
    }

    /*
    createTicket: (name, type, email, subject, description, attachmentToken, url) => {
    const ticket = {};
    ticket.request = {
      requester: { name: name, email: email },
      subject: subject,
      // BEWARE changing the following ids or values! If you change them then you must thoroughly test.
      custom_fields: [
        { id: 360012744452, value: type},
        { id: 360007369412, value: description},
        { id: 360012744292, value: name},
        { id: 360012782111, value: email },
        { id: 360018545031, value: email }
      ],
      comment: {
        body: description + '\n\n------------------\nSubmitted from: ' + url,
        uploads: attachmentToken
      },
      ticket_form_id: 360000669472
    };
    return ticket;
  }
     */


    public void postTicketToSupport(SupportTicket ticket, AuthUser authUser) throws Exception {
        GenericUrl genericUrl = new GenericUrl(configuration.postSupportRequestUrl());
        JsonHttpContent content = new JsonHttpContent(new GsonFactory(), ticket);
        HttpRequest request = clientUtil.buildPostRequest(genericUrl, content, authUser);
        HttpResponse response = clientUtil.handleHttpRequest(request);
        if (response.getStatusCode() != 200) {
            throw new ServerErrorException(response.getStatusMessage(), 500);
        }
    }


}
