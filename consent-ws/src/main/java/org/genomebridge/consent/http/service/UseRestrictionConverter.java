package org.genomebridge.consent.http.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.collections.CollectionUtils;
import org.genomebridge.consent.http.models.grammar.And;
import org.genomebridge.consent.http.models.grammar.Named;
import org.genomebridge.consent.http.models.grammar.UseRestriction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseRestrictionConverter {
    public static final Logger LOGGER = LoggerFactory.getLogger("UseRestrictionConverter");
    private static final ObjectMapper mapper = new ObjectMapper();
    private final UseRestrictionConfig config;
    // Fields to be parsed. We'll see if we can configure it with an external file.
    String[] typeOfResearch = {
            "methods",
            "population",
            "pediatric"
    };

    // Fields to be parsed. We'll see if we can configure it with an external file. AND and NAMED for added ontologies?
    String[] diseaseAreas = {
            "ontologies"
    };
    // Fields to be parsed. We'll see if we can configure it with an external file.
    String[] researchPurposeStatement = {
            "forProfit",
            "onegender"
    };

    public UseRestrictionConverter(UseRestrictionConfig config){
        this.config = config;
    }

    public UseRestriction parseJsonFormulary(String json) {
        And srp = new And();
        List<UseRestriction> operandsList = new ArrayList<>();
        Map<String, Object> form = parseAsMap(json);
        LinkedHashMap darMap = (LinkedHashMap)form.get("_id");
        srp.setReferenceId((String)darMap.get("$oid"));
        for(String field: typeOfResearch){
            if(form.containsKey(field)){
                if((boolean)form.get(field)){
                    operandsList.add(createNamedRestriction(config.getValueByName(field)));
                }
            }
        }

        for(String field: diseaseAreas){
            ArrayList<HashMap<String, String>> ontologies = (ArrayList<HashMap<String, String>>) form.get(field);
            if(CollectionUtils.isNotEmpty(ontologies)){
                operandsList.addAll(ontologies.stream().map(map -> createNamedRestriction((String) map.get("id"))).collect(Collectors.toList()));
            }
        }

        String url;
        if((boolean) form.get(researchPurposeStatement[0])){
            url = config.getProfit();
        } else {
            url = config.getNonProfit();
        }
        operandsList.add(createNamedRestriction(url));

        // limited to one gender
        if((boolean) form.get(researchPurposeStatement[1])){
            if(((form.get("gender")).equals("M"))){
                url = config.getMen();
            } else {
                url = config.getWomen();
            }
            operandsList.add(createNamedRestriction(url));
        }

        UseRestriction[] operands = new UseRestriction[operandsList.size()];
        operands = operandsList.toArray(operands);
        srp.setOperands(operands);
        return srp;
    }

    private Named createNamedRestriction(String name){
        Named r = new Named();
        r.setName(name);
        return r;
    }

    public Map<String, Object> parseAsMap(String str) {
        ObjectReader reader = mapper.reader(Map.class);
        try {
            return reader.readValue(str);
        } catch (IOException e) {
            LOGGER.debug("While parsing as a Map ...", e);
        }
        return null;
    }
}