package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class DatabaseTranslateServiceAPI extends AbstractTranslateServiceAPI {

    Client client;
    private ServicesConfiguration config;

    protected Logger logger() {
        return Logger.getLogger("DatabaseTranslateServiceAPI");
    }

    public static void initInstance(Client client, ServicesConfiguration config) {
        TranslateAPIHolder.setInstance(new DatabaseTranslateServiceAPI(client, config));
    }

    private DatabaseTranslateServiceAPI(Client client, ServicesConfiguration config){
        this.client = client;
        client.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        client.property(ClientProperties.READ_TIMEOUT,    10000);
        this.config = config;
    }

    @Override
    public void setClient(Client client){
        this.client = client;
    }


    @Override
    public String translate(String translateFor, UseRestriction useRestriction) throws IOException {
        String translatedUseRestriction;
        try {
             translatedUseRestriction = translateService(translateFor , useRestriction, MediaType.APPLICATION_JSON, config.getTranslateURL());
        } catch (IOException e) {
            logger().error("Translate error.", e);
            throw  e;
        }
        return translatedUseRestriction;
    }

    @Override
    public String translateAsHtml(String translateFor, UseRestriction useRestriction) throws IOException {
        String translatedUseRestriction;
        try {
             translatedUseRestriction = translateService(translateFor , useRestriction, MediaType.TEXT_HTML, config.getTranslateURL() + "/html");
        } catch (IOException e) {
            logger().error("Translate error.", e);
            throw  e;
        }
        return translatedUseRestriction;
    }

    private String translateService(
        String translateFor ,
        UseRestriction useRestriction,
        String mediaType,
        String url) throws IOException {
        if(useRestriction != null ){
            String responseAsString = null;
            String json = new Gson().toJson(useRestriction);
            WebTarget target = client.target(url).queryParam("for",translateFor);
            Response res = target.request(mediaType).post(Entity.json(json));
            if (res.getStatus() == Response.Status.OK.getStatusCode()) {
                StringWriter writer = new StringWriter();
                IOUtils.copy((InputStream) res.getEntity(), writer);
                responseAsString = writer.toString();
            }else if (res.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()){
                throw  new IOException();
            }
          return responseAsString;
        }
        return null;
    }

}
