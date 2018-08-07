package org.broadinstitute.consent.http.service;


import io.jsonwebtoken.*;
//import io.jsonwebtoken.impl.crypto.MacProvider;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.broadinstitute.consent.http.configurations.NihConfiguration;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.service.users.handler.ResearcherAPI;

import javax.ws.rs.core.Response;
import java.security.Key;
import java.sql.BatchUpdateException;
import java.util.*;
import java.util.stream.Collectors;


public class NihServiceAPI implements NihAuthApi {


    NihConfiguration nihConfiguration;
    ResearcherAPI researcherAPI;
    Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public NihServiceAPI(NihConfiguration nihConfiguration, ResearcherAPI researcherApi) {
        this.researcherAPI = researcherApi;
        this.nihConfiguration = nihConfiguration;
    }

    // For testing pourposes
    @Override
    public String generateToken() {
        String token = Jwts.builder()
                .claim("nihUsername", "EraLeo")
                .signWith(key)
                .compact();
        return token;
    }

    @Override
    public Map<String, String> authenticateNih(String jwt, Integer userId) throws DecodingException{
        // Use this as secret when this is well configured
        byte[] secret = nihConfiguration.getSigningSecret().getBytes();
//        This throws a weak token exception, need to handle somehow
//        Key key = Keys.hmacShaKeyFor(secret);

        try {
            Map<String,String> jws = Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(jwt).getBody()
                    .entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
            return createNihContent(jws, userId);

        } catch (DecodingException ex) {
            throw ex;
        }

    }

    @Override
    public Response deleteNihAccountById(Integer userId) {
        List<ResearcherProperty> properties = new ArrayList<>();
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_EXPIRATION_DATE.getValue()));
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_DATE.getValue()));
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_STATUS.getValue()));
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_TOKEN.getValue()));
        properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_USERNAME.getValue()));

        researcherAPI.deleteResearcherSpecificProperties(properties);
        return Response.ok().build();

    }


    private Map<String, String> createNihContent(Map<String, String> jws, Integer userId) {
        Map<String, String> nihComponents = new HashMap<>();
        if (!jws.isEmpty()) {
            nihComponents.put(ResearcherFields.ERA_STATUS.getValue(), Boolean.TRUE.toString());
            nihComponents.putAll(jws);
            nihComponents.putAll(setUpdateAndExpirationDate());
            researcherAPI.updateResearcher(nihComponents, userId, false);
        }
        return nihComponents;
    }

    private Map<String, String> setUpdateAndExpirationDate() {
        Map<String, String> dates = new HashMap<>();
        Date currentDate = new Date();

        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c.add(Calendar.DATE, 30);
        Date expires= c.getTime();

        dates.put(ResearcherFields.ERA_DATE.getValue(), String.valueOf(currentDate.getTime()));
        dates.put(ResearcherFields.ERA_EXPIRATION_DATE.getValue(), String.valueOf(expires.getTime()));

        return dates;
    }

}

