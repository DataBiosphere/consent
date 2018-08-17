package org.broadinstitute.consent.http.service;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.security.Key;

import org.broadinstitute.consent.http.configurations.NihConfiguration;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.service.users.handler.ResearcherAPI;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Date;

public class NihServiceAPI implements NihAuthApi {

    private NihConfiguration nihConfiguration;
    private ResearcherAPI researcherAPI;

    public NihServiceAPI(NihConfiguration nihConfiguration, ResearcherAPI researcherApi) {
        this.researcherAPI = researcherApi;
        this.nihConfiguration = nihConfiguration;
    }

    @Override
    public Map<String, String> authenticateNih(String jwt, Integer userId) throws SignatureException, MalformedJwtException, UnsupportedJwtException, DecodingException {

        Key key = Keys.hmacShaKeyFor(nihConfiguration.getSigningSecret());

        String nihUserName = Jwts.parser()
                .setSigningKey(key)
                .parsePlaintextJws(jwt).getBody();

        return createNihContent(nihUserName, userId);
    }

    @Override
    public void deleteNihAccountById(Integer userId) {
        List<ResearcherProperty> properties = new ArrayList<>();
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_EXPIRATION_DATE.getValue()));
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_STATUS.getValue()));
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_USERNAME.getValue()));

        researcherAPI.deleteResearcherSpecificProperties(properties);
    }


    private Map<String, String> createNihContent(String nihUserName, Integer userId) {
        Map<String, String> nihComponents = new HashMap<>();
        if (nihUserName != null && !nihUserName.isEmpty()) {
            nihComponents.put(ResearcherFields.ERA_STATUS.getValue(), Boolean.TRUE.toString());
            nihComponents.put(ResearcherFields.ERA_USERNAME.getValue(), nihUserName);
            nihComponents.putAll(generateEraExpirationDates());
            researcherAPI.updateResearcher(nihComponents, userId, false);
        }
        return nihComponents;
    }

    private Map<String, String> generateEraExpirationDates() {
        Map<String, String> dates = new HashMap<>();
        Date currentDate = new Date();

        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c.add(Calendar.DATE, 30);
        Date expires= c.getTime();

        dates.put(ResearcherFields.ERA_EXPIRATION_DATE.getValue(), String.valueOf(expires.getTime()));

        return dates;
    }

}

