package org.broadinstitute.consent.http.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class UseRestrictionConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger("UseRestrictionConverter");
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ServicesConfiguration servicesConfiguration;
    private Client client;

    public UseRestrictionConverter(Client client, ServicesConfiguration config) {
        this.client = client;
        this.servicesConfiguration = config;
    }

    @SuppressWarnings("unchecked")
    public DataUse parseDataUsePurpose(String json) {
        Map<String, Object> form = parseAsMap(json);
        DataUse dataUse = new DataUse();

        //
        //    Research related entries
        //
        if (Boolean.valueOf(form.getOrDefault("methods", false).toString())) {
            dataUse.setMethodsResearch(true);
        }
        if (Boolean.valueOf(form.getOrDefault("population", false).toString())) {
            dataUse.setPopulationStructure(true);
        }
        if (Boolean.valueOf(form.getOrDefault("controls", false).toString())) {
            dataUse.setControlSetOption("Yes");
        }

        //
        //    Diseases related entries
        //
        ArrayList<HashMap<String, String>> ontologies = (ArrayList<HashMap<String, String>>) form.get("ontologies");
        if (CollectionUtils.isNotEmpty(ontologies)) {
            dataUse.setDiseaseRestrictions(
                    ontologies.stream().map(hashMap -> hashMap.get("id")).collect(Collectors.toList())
            );
        }

        //
        //    gender, age and commercial status entries
        //
        boolean forProfitOnly = Boolean.valueOf(form.getOrDefault("forProfit", false).toString());
        dataUse.setCommercialUse(forProfitOnly);

        // limited to one gender + children analysis
        boolean oneGenderOnly = Boolean.valueOf(form.getOrDefault("onegender", false).toString());
        String selectedGender = (String) form.getOrDefault("gender", "X");
        boolean pediatricsOnly = Boolean.valueOf(form.getOrDefault("pediatric", false).toString());

        if (oneGenderOnly) {
            if (selectedGender.equalsIgnoreCase("M"))
                dataUse.setGender("Male");
            else if (selectedGender.equalsIgnoreCase("F"))
                dataUse.setGender("Female");
        }

        if (pediatricsOnly) {
            dataUse.setPediatric(true);
        }

        if (Boolean.valueOf(form.getOrDefault("poa", false).toString())) {
            dataUse.setPopulationOriginsAncestry(true);
        }

        if (Boolean.valueOf(form.getOrDefault("hmb", false).toString())) {
            dataUse.setHmbResearch(true);
        }

        return dataUse;
    }

    public UseRestriction parseUseRestriction(DataUse dto) {
        WebTarget target = client.target(servicesConfiguration.getDARTranslateUrl());
        Response response = target.request(MediaType.APPLICATION_JSON).post(Entity.json(dto.toString()));
        if (response.getStatus() == 200) {
            try {
                return response.readEntity(UseRestriction.class);
            } catch (Exception e) {
                LOGGER.error("Error parsing response from Ontology service: " + e);
            }
        }
        LOGGER.error("Error response from Ontology service: " + response.readEntity(String.class));
        return null;
    }

    public Map<String, Object> parseAsMap(String str) {
        ObjectReader reader = mapper.readerFor(Map.class);
        try {
            return reader.readValue(str);
        } catch (IOException e) {
            LOGGER.debug("While parsing as a Map ...", e);
        }
        return null;
    }
}
