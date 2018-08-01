package org.broadinstitute.consent.http.service;


import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.crypto.MacProvider;
import org.broadinstitute.consent.http.configurations.NihConfiguration;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.service.users.handler.ResearcherAPI;

import javax.ws.rs.core.Response;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;


public class NihAuthServiceAPI implements NihAuthApi {
    private static final Key secret = MacProvider.generateKey(SignatureAlgorithm.HS256);
    private static final byte[] secretBytes = secret.getEncoded();
    private static final String base64SecretBytes = Base64.getEncoder().encodeToString(secretBytes);
    NihConfiguration nihConfiguration;
    ResearcherAPI researcherAPI;
    public NihAuthServiceAPI(NihConfiguration nihConfiguration, ResearcherAPI researcherApi) {
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
                .setIssuedAt(now)
                .setNotBefore(now)
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS256, base64SecretBytes)
                .compact();
        return token;
    }

    @Override
    public Map<String, String> authenticateNih(String jwt, Integer userId, Map<String, String> properties) throws SignatureException{
        // Use this as secret when this is well configured
        String secret = nihConfiguration.getSigningSecret();
//            Claims jws;
        try {
            Map<String,String> jws = Jwts.parser()
                    .setSigningKey(base64SecretBytes)
                    .parseClaimsJws(jwt).getBody()
                    .entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

            if (!jws.isEmpty()) {
                jws.putAll(properties);
                researcherAPI.updateResearcher(jws, userId, false);
            }
            return jws;
        } catch (SignatureException e) {
            throw e;

        }

    }

    @Override
    public Response deleteNihAccountById(Integer userId) {
        if (userId != null) {
            // armar lista de properties a borrar
            List<ResearcherProperty> properties = new ArrayList<ResearcherProperty>();
            properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_EXPIRATION_DATE.getValue(), null));
            properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_DATE.getValue(), null));
            properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_STATUS.getValue(), null));
            properties.add(new ResearcherProperty(userId, ResearcherFields.ERA_TOKEN.getValue(), null));
            properties.add(new ResearcherProperty(userId, ResearcherFields.JTI.getValue(), null));
            properties.add(new ResearcherProperty(userId, ResearcherFields.EXP.getValue(), null));
            properties.add(new ResearcherProperty(userId, ResearcherFields.IAT.getValue(), null));
            properties.add(new ResearcherProperty(userId, ResearcherFields.NBF.getValue(), null));
            researcherAPI.deleteResearcherSpecificProperties(properties);
        }
        return Response.ok().build();
    }

}

