package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.bson.Document;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;

public class DatabaseTranslateServiceAPI extends AbstractTranslateServiceAPI {

    Client client;
    private WebTarget target;
    private ServicesConfiguration config;
    private final UseRestrictionConverter converter;

    protected Logger logger() {
        return Logger.getLogger("DatabaseTranslateServiceAPI");
    }

    public static void initInstance(Client client, ServicesConfiguration config, UseRestrictionConverter converter) {
        TranslateAPIHolder.setInstance(new DatabaseTranslateServiceAPI(client, config, converter));
    }

    private DatabaseTranslateServiceAPI(Client client, ServicesConfiguration config, UseRestrictionConverter converter){
        this.client = client;
        this.converter = converter;
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

    @Override
    public String generateStructuredTranslatedRestriction(Document dar, Boolean needsManualReview){
        StringBuilder sb = new StringBuilder();
        String blankSpace = " ";
        String lineBreak  = "<br>";
        String rootId = "[DOID_4(CC)]";
        String healthResearch = "Data will be used for health/medical/biomedical research ";
        sb.append("Samples will be used under the following conditions:" + lineBreak);
        String json = dar.toJson();
        Map<String, Object> darMap = converter.parseAsMap(json);
        Boolean ontologies = Objects.isNull(darMap.get("ontologies"));
        Boolean diseases = getBooleanValue("diseases",darMap);
        List<LinkedHashMap<String,String>> ontologiesList = (List<LinkedHashMap<String,String>>) darMap.get("ontologies");

        if(needsManualReview){
            sb.append("Needs Manual Review." + lineBreak);
        }

        if(diseases){
            String diseasesAnswer = healthResearch;

            if(ontologies || (ontologiesList.size() == 1 && ontologyIdentifierBuilder( ontologiesList.get(0).get("id")).equals(rootId))){
                diseasesAnswer = diseasesAnswer +"[HMB(CC)]";
            }
            sb.append(diseasesAnswer + lineBreak);
        }

        if(getBooleanValue("methods",darMap)){
            sb.append("Data will be used for methods development research [NMDS=0]" + lineBreak);
        }

        if(getBooleanValue("controls",darMap)){
            sb.append("Data will be used as a control sample set [NCTRL=0]" + lineBreak);
        }

        if(getBooleanValue("population",darMap)) {
            sb.append("Data will be used for population structure or normal variation studies [NPNV=0]" + lineBreak);
        }


        if(!ontologies){
            String t = "Data will be used to study: ";
            List<String> ontologiesText= new ArrayList<>();
            for(LinkedHashMap ontology : ontologiesList){
                String id = ontologyIdentifierBuilder((String) ontology.get("id"));
                if(rootId.equals(id)){
                    if(!diseases) {
                        sb.append(healthResearch +"[HMB(CC)]"+ lineBreak);
                    }
                }else {
                    ontologiesText.add(blankSpace + ontology.get("label") + blankSpace + id);
                }
            }
            if(!ontologiesText.isEmpty()){
               sb.append(t+ StringUtils.join(ontologiesText, ',')+ lineBreak);
            }
        }

        if(getBooleanValue("forProfit",darMap)) {
            sb.append("Data will be used for commercial purpose [NPU] " + lineBreak);
        }else{
            sb.append("Data will not be used for commercial purpose" + lineBreak);
        }


        if(getBooleanValue("onegender",darMap)) {
            String gender = (String) darMap.get("gender");
            if(Objects.nonNull(gender)){
            String genderText = "Data will be used to study ONLY a XXXX population [RS-[GENDER]]";
                gender = gender.equals("M") ? "male" : "female";
                sb.append(genderText.replaceAll("XXXX", gender)+ lineBreak);
            }else{
                throw new BadRequestException();
            }
        }


        if(getBooleanValue("pediatric",darMap)) {
            sb.append("Data will be used to study ONLY a pediatric population [RS-[PEDIATRIC]]" + lineBreak);
        }
        return sb.toString();
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


    public String ontologyIdentifierBuilder(String ontologyURI){
        String s = "[XXXX(CC)]";
        String ontolotyId = ontologyURI.substring(ontologyURI.lastIndexOf("/") + 1);
        String ontologyIdFormatted = s.replaceAll("XXXX",ontolotyId);
        return ontologyIdFormatted;

    }

        public Boolean getBooleanValue(String key, Map map){
            Object o =  map.get(key);
            if(Objects.isNull(o)){
                return false;
            }else
            return (Boolean) o;
        }


}
