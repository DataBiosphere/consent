package org.genomebridge.consent.http.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.collections.CollectionUtils;
import org.genomebridge.consent.http.models.grammar.And;
import org.genomebridge.consent.http.models.grammar.Or;
import org.genomebridge.consent.http.models.grammar.Named;
import org.genomebridge.consent.http.models.grammar.UseRestriction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseRestrictionConverter {

    public static final Logger LOGGER = LoggerFactory.getLogger("UseRestrictionConverter");
    private static final ObjectMapper mapper = new ObjectMapper();
    private final UseRestrictionConfig config;

    private final int COMMERCIAL_STATUS = 0;
    private final int LIMITED_TO_ONE_GENDER = 1;
    private final int PEDIATRIC_ONLY = 2;

    // Fields to be parsed. We'll see if we can configure it with an external file.
    String[] typeOfResearch = {
        "methods",
        "population"

    };

    // Fields to be parsed. We'll see if we can configure it with an external file. AND and NAMED for added ontologies?
    String[] diseaseAreas = {
        "ontologies"
    };

    // Fields to be parsed. We'll see if we can configure it with an external file.
    String[] researchPurposeStatement = {
        "forProfit",
        "onegender",
        "pediatric"
    };

    public UseRestrictionConverter(UseRestrictionConfig config) {
        this.config = config;
    }

    public UseRestriction parseJsonFormulary(String json) {
        And srp = new And();
        And methodsRestriction = new And();
        Or diseasesRestriction = new Or();
        And purposesRestriction = new And();

        Map<String, Object> form = parseAsMap(json);

        List<UseRestriction> operandList = new ArrayList<>();

        //
        //    Research related entries 
        // 
        List<UseRestriction> methodsList = new ArrayList<>();
        for (String field : typeOfResearch) {
            if (form.containsKey(field)) {
                if ((boolean) form.get(field)) {
                    methodsList.add(createNamedRestriction(config.getValueByName(field)));
                }
            }
        }
        if (CollectionUtils.isNotEmpty(methodsList)) {
            UseRestriction[] methodsOps = new UseRestriction[methodsList.size()];
            methodsOps = methodsList.toArray(methodsOps);
            methodsRestriction.setOperands(methodsOps);
            operandList.add(methodsRestriction);
        }

        //
        //    Diseases related entries
        //
        List<UseRestriction> diseasesList = new ArrayList<>();
        for (String field : diseaseAreas) {
            ArrayList<HashMap<String, String>> ontologies = (ArrayList<HashMap<String, String>>) form.get(field);
            if (CollectionUtils.isNotEmpty(ontologies)) {
                diseasesList.addAll(ontologies.stream().map(map -> createNamedRestriction((String) map.get("id"))).collect(Collectors.toList()));
            }
        }

        if (CollectionUtils.isNotEmpty(diseasesList)) {
            UseRestriction[] diseasesOps = new UseRestriction[diseasesList.size()];
            diseasesOps = diseasesList.toArray(diseasesOps);
            diseasesRestriction.setOperands(diseasesOps);
            operandList.add(diseasesRestriction);
        }

        //
        //    population and commercial status entries
        //
        String url;
        List<UseRestriction> purposesList = new ArrayList<>();

        // check is forProfit is present in form
        boolean forProfitOnly = (boolean) form.getOrDefault(researchPurposeStatement[COMMERCIAL_STATUS], false);
        url = forProfitOnly == true ? config.getProfit() : config.getNonProfit();
        purposesList.add(createNamedRestriction(url));

        // limited to one gender
        boolean oneGenderOnly = (boolean) form.getOrDefault(researchPurposeStatement[LIMITED_TO_ONE_GENDER], false);
        String selectedGender = (String) form.getOrDefault("gender", "X");
        if (oneGenderOnly && selectedGender.equalsIgnoreCase("M")) {
            purposesList.add(createNamedRestriction(config.getMen()));
        } else if (oneGenderOnly && selectedGender.equalsIgnoreCase("F")) {
            purposesList.add(createNamedRestriction(config.getWomen()));
        } else {
            //TODO: how to handle this ????
            LOGGER.error("Exception: One Gender Limitation without specifying selected Gender....");
            purposesList.add(createNamedRestriction("X"));
        }

        // check if restricted to pediatric population
        boolean pediatricsOnly = (boolean) form.getOrDefault(researchPurposeStatement[PEDIATRIC_ONLY], false);
        if (pediatricsOnly) {
            purposesList.add(createNamedRestriction(config.getPediatric()));
        }

        //
        //    Compose all restrictions into an And one ...
        //
        if (CollectionUtils.isNotEmpty(purposesList)) {
            UseRestriction[] purposesOps = new UseRestriction[purposesList.size()];
            purposesOps = purposesList.toArray(purposesOps);
            purposesRestriction.setOperands(purposesOps);
            operandList.add(purposesRestriction);
        }

        UseRestriction[] operands = new UseRestriction[operandList.size()];
        operands = operandList.toArray(operands);
        srp.setOperands(operands);
        return srp;
    }

    private Named createNamedRestriction(String name) {
        Named r = new Named();
        r.setName(name);
        return r;
    }

    private Map<String, Object> parseAsMap(String str) {
        ObjectReader reader = mapper.reader(Map.class);
        try {
            return reader.readValue(str);
        } catch (IOException e) {
            LOGGER.debug("While parsing as a Map ...", e);
        }
        return null;
    }
}
