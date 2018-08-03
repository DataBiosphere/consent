package org.broadinstitute.consent.http.service;


import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.crypto.MacProvider;
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
    private static final Key secret = MacProvider.generateKey(SignatureAlgorithm.HS256);
    private static final byte[] secretBytes = secret.getEncoded();
    private static final String base64SecretBytes = Base64.getEncoder().encodeToString(secretBytes);
    NihConfiguration nihConfiguration;
    ResearcherAPI researcherAPI;
    public NihServiceAPI(NihConfiguration nihConfiguration, ResearcherAPI researcherApi) {
        this.researcherAPI = researcherApi;
        this.nihConfiguration = nihConfiguration;
    }

    // For testing pourposes
    @Override
    public String generateToken() {
        String id = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        Date exp = new Date(System.currentTimeMillis() + (1000 * 30)); // 30 seconds

        String token = Jwts.builder()
                .setId(id)
                .signWith(SignatureAlgorithm.HS256, base64SecretBytes)
                .compact();
        return token;
    }

    @Override
    public Map<String, String> authenticateNih(String jwt, Integer userId) throws SignatureException{
        // Use this as secret when this is well configured
        String secret = nihConfiguration.getSigningSecret();

        try {
            Map<String,String> jws = Jwts.parser()
                    .setSigningKey(base64SecretBytes)
                    .parseClaimsJws(jwt).getBody()
                    .entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
            return createNihContent(jws, userId);

        } catch (SignatureException e) {
            throw e;
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

