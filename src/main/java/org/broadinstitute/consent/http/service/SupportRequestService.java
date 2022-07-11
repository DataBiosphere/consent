package org.broadinstitute.consent.http.service;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.supportticket.CustomRequestField;
import org.broadinstitute.consent.http.models.supportticket.SupportRequestComment;
import org.broadinstitute.consent.http.models.supportticket.SupportRequester;
import org.broadinstitute.consent.http.models.supportticket.SupportTicket;
import org.broadinstitute.consent.http.util.HttpClientUtil;

import javax.ws.rs.ServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SupportRequestService {

    private final HttpClientUtil clientUtil;
    private final ServicesConfiguration configuration;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public SupportRequestService(ServicesConfiguration configuration) {
        this.clientUtil = new HttpClientUtil();
        this.configuration = configuration;
    }


    public SupportTicket createSupportTicket(String name, String type, String email, String subject, String description, String url) {
        SupportRequester requester = new SupportRequester(name, email);
        List<CustomRequestField> customFields = new ArrayList<>();
        customFields.add(new CustomRequestField("360012744452", type));
        customFields.add(new CustomRequestField("360007369412", description));
        customFields.add(new CustomRequestField("360012744292", name));
        customFields.add(new CustomRequestField("360012782111", email));
        customFields.add(new CustomRequestField("360018545031", email));
        SupportRequestComment comment = new SupportRequestComment(description, url);

        return new SupportTicket(requester, subject, customFields, comment);
    }

    public void postTicketToSupport(SupportTicket ticket, AuthUser authUser) throws Exception {
        GenericUrl genericUrl = new GenericUrl(configuration.postSupportRequestUrl());
        JsonHttpContent content = new JsonHttpContent(new GsonFactory(), ticket);
        HttpRequest request = clientUtil.buildPostRequest(genericUrl, content, authUser);
        HttpResponse response = clientUtil.handleHttpRequest(request);

        if (response.getStatusCode() != HttpStatusCodes.STATUS_CODE_OK) {
            logger.error(response.getStatusMessage());
            throw new ServerErrorException(response.getStatusMessage(), HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
        }
    }


}
