package org.broadinstitute.consent.http.service;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.support.CustomRequestField;
import org.broadinstitute.consent.http.models.support.SupportRequestComment;
import org.broadinstitute.consent.http.models.support.SupportRequester;
import org.broadinstitute.consent.http.models.support.SupportTicket;
import org.broadinstitute.consent.http.util.HttpClientUtil;

import javax.ws.rs.ServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SupportRequestService {

    private final HttpClientUtil clientUtil;
    private final ServicesConfiguration configuration;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public SupportRequestService(ServicesConfiguration configuration) {
        this.clientUtil = new HttpClientUtil();
        this.configuration = configuration;
    }

    /**
     * Builds a ticket with the proper structure to request support via Zendesk
     * @param name The name of the user requesting support
     * @param type The type of request ("question", "incident", "problem", "task")
     * @param email The email of the user requesting support
     * @param subject Subject line of the request
     * @param description Description of the task or question
     * @param url The API url of this request
     * @return A structured ticket used to make the request
     */
    public SupportTicket createSupportTicket(String name, String type, String email, String subject, String description, String url) {
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
        customFields.add(new CustomRequestField(360012744452L, type));
        customFields.add(new CustomRequestField(360007369412L, description));
        customFields.add(new CustomRequestField(360012744292L, name));
        customFields.add(new CustomRequestField(360012782111L, email));
        customFields.add(new CustomRequestField(360018545031L, email));
        SupportRequestComment comment = new SupportRequestComment(description + "\n\n------------------\nSubmitted from: " + url);

        return new SupportTicket(requester, subject, customFields, comment);
    }

    public void postTicketToSupport(SupportTicket ticket, AuthUser authUser) throws Exception {
        GenericUrl genericUrl = new GenericUrl(configuration.postSupportRequestUrl());
        //Using GsonBuilder directly to convert ticket to json since GsonFactory does not allow custom FieldNamingPolicy
        String ticketJson = new GsonBuilder()
                .setPrettyPrinting()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
                .toJson(ticket);
        //GsonFactory doesn't do more work on the ticket but an HttpContent object is needed for buildPostRequest
        JsonHttpContent content = new JsonHttpContent(new GsonFactory(), ticketJson);
        HttpRequest request = clientUtil.buildPostRequest(genericUrl, content, authUser);
        HttpResponse response = clientUtil.handleHttpRequest(request);

        if (response.getStatusCode() != HttpStatusCodes.STATUS_CODE_OK) {
            logger.error(response.getStatusMessage());
            throw new ServerErrorException(response.getStatusMessage(), HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
        }
    }


}