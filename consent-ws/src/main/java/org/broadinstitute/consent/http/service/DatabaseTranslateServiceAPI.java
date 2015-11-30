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
    private WebTarget target;
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
    public String translate(String translateFor ,UseRestriction useRestriction) throws IOException {
        String translatedUseRestriction;
        try {
             translatedUseRestriction = translateService(translateFor , useRestriction);
        } catch (IOException e) {
            logger().error("Translate error.", e);
            throw  e;
        }
        return translatedUseRestriction;
    }



    private String translateService(String translateFor , UseRestriction useRestriction) throws IOException {
        if(useRestriction != null ){
            String responseAsString = null;
            String json = new Gson().toJson(useRestriction);
            target = client.target(config.getTranslateURL()).queryParam("for",translateFor);
            Response res = target.request(MediaType.APPLICATION_JSON).post(Entity.json(json));
            if (res.getStatus() == Response.Status.OK.getStatusCode()) {
                StringWriter writer = new StringWriter();
                IOUtils.copy((InputStream) res.getEntity(), writer);
                String theString = writer.toString();
                responseAsString = theString;
            }else if (res.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()){
                throw  new IOException();
            }
          return responseAsString;
        }
        return null;
    }
}
