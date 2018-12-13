package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TranslateServiceImpl extends AbstractTranslateService {

    private final UseRestrictionConverter converter;

    public static void initInstance(UseRestrictionConverter converter) {
        TranslateAPIHolder.setInstance(new TranslateServiceImpl(converter));
    }

    private TranslateServiceImpl(UseRestrictionConverter converter) {
        this.converter = converter;
    }

    @Override
    public String generateStructuredTranslatedRestriction(Document dar, Boolean needsManualReview) {
        StringBuilder sb = new StringBuilder();
        String blankSpace = " ";
        String lineBreak = "<br>";
        String rootId = "[DOID_4(CC)]";
        String healthResearch = "Data will be used for health/medical/biomedical research ";
        sb.append("Samples will be used under the following conditions:").append(lineBreak);
        String json = dar.toJson();
        Map<String, Object> darMap = converter.parseAsMap(json);
        boolean ontologies = Objects.isNull(darMap.get("ontologies"));
        Boolean diseases = getBooleanValue("diseases", darMap);
        List<LinkedHashMap<String, String>> ontologiesList = (List<LinkedHashMap<String, String>>) darMap.get("ontologies");

        if (needsManualReview) {
            sb.append("Needs Manual Review.").append(lineBreak);
        }

        if (diseases) {
            String diseasesAnswer = healthResearch;

            if (ontologies || (ontologiesList.size() == 1 && ontologyIdentifierBuilder(ontologiesList.get(0).get("id")).equals(rootId))) {
                diseasesAnswer = diseasesAnswer + "[HMB(CC)]";
            }
            sb.append(diseasesAnswer).append(lineBreak);
        }

        if (getBooleanValue("methods", darMap)) {
            sb.append("Data will be used for methods development research [NMDS=0]").append(lineBreak);
        }

        if (getBooleanValue("controls", darMap)) {
            sb.append("Data will be used as a control sample set [NCTRL=0]").append(lineBreak);
        }

        if (getBooleanValue("population", darMap)) {
            sb.append("Data will be used for population structure or normal variation studies [NPNV=0]").append(lineBreak);
        }

        if (!ontologies) {
            String t = "Data will be used to study: ";
            List<String> ontologiesText = new ArrayList<>();
            for (LinkedHashMap ontology : ontologiesList) {
                String id = ontologyIdentifierBuilder((String) ontology.get("id"));
                if (rootId.equals(id)) {
                    if (!diseases) {
                        sb.append(healthResearch).append("[HMB(CC)]").append(lineBreak);
                    }
                } else {
                    ontologiesText.add(blankSpace + ontology.get("label") + blankSpace + id);
                }
            }
            if (!ontologiesText.isEmpty()) {
                sb.append(t).append(StringUtils.join(ontologiesText, ',')).append(lineBreak);
            }
        }

        if (getBooleanValue("forProfit", darMap)) {
            sb.append("Data will be used for commercial purpose [NPU] ").append(lineBreak);
        } else {
            sb.append("Data will not be used for commercial purpose").append(lineBreak);
        }

        if (getBooleanValue("onegender", darMap)) {
            String gender = (String) darMap.get("gender");
            if (Objects.nonNull(gender)) {
                String genderText = "Data will be used to study ONLY a XXXX population [RS-[GENDER]]";
                gender = gender.equals("M") ? "male" : "female";
                sb.append(genderText.replaceAll("XXXX", gender)).append(lineBreak);
            } else {
                throw new BadRequestException();
            }
        }

        if (getBooleanValue("pediatric", darMap)) {
            sb.append("Data will be used to study ONLY a pediatric population [RS-[PEDIATRIC]]").append(lineBreak);
        }
        return sb.toString();
    }

    private String ontologyIdentifierBuilder(String ontologyURI) {
        String s = "[XXXX(CC)]";
        String ontolotyId = ontologyURI.substring(ontologyURI.lastIndexOf("/") + 1);
        return s.replaceAll("XXXX", ontolotyId);

    }

    private Boolean getBooleanValue(String key, Map map) {
        Object o = map.get(key);
        if (Objects.isNull(o)) {
            return false;
        } else
            return (Boolean) o;
    }
    
}
