package org.broadinstitute.consent.http.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.OntologyEntry;
import org.bson.Document;

public class TranslateServiceImpl extends AbstractTranslateService {

    public static void initInstance() {
        TranslateAPIHolder.setInstance(new TranslateServiceImpl());
    }

    @Override
    public String generateStructuredTranslatedRestriction(Document dar, Boolean needsManualReview) {
        StringBuilder sb = new StringBuilder();
        String blankSpace = " ";
        String lineBreak = "<br>";
        String rootId = "[DOID_4(CC)]";
        String healthResearch = "Data will be used for health/medical/biomedical research ";
        sb.append("Samples will be used under the following conditions:").append(lineBreak);
        DataAccessRequestData darData = DataAccessRequestData.fromString(dar.toJson());
        List<OntologyEntry> ontologiesList = Objects.nonNull(darData.getOntologies()) ?
            darData.getOntologies()
                .stream()
                .filter(Objects::nonNull)
                .filter(e -> Objects.nonNull(e.getId()))
                .filter(e -> Objects.nonNull(e.getDefinition()))
                .filter(e -> Objects.nonNull(e.getLabel()))
                .collect(Collectors.toList()) :
            Collections.emptyList();
        boolean hasOntologies = !ontologiesList.isEmpty();
        boolean hasDiseases = Objects.nonNull(darData.getDiseases()) && darData.getDiseases();

        if (needsManualReview) {
            sb.append("Needs Manual Review.").append(lineBreak);
        }

        if (hasDiseases) {
            String diseasesAnswer = healthResearch;
            if (ontologiesList.size() == 1 && ontologyIdentifierBuilder(ontologiesList.get(0).getId()).equals(rootId)) {
                diseasesAnswer = diseasesAnswer + "[HMB(CC)]";
            }
            sb.append(diseasesAnswer).append(lineBreak);
        }

        if (Objects.nonNull(darData.getMethods()) && darData.getMethods()) {
            sb.append("Data will be used for methods development research [NMDS=0]").append(lineBreak);
        }

        if (Objects.nonNull(darData.getControls()) && darData.getControls()) {
            sb.append("Data will be used as a control sample set [NCTRL=0]").append(lineBreak);
        }

        if (Objects.nonNull(darData.getPopulation()) && darData.getPopulation()) {
            sb.append("Data will be used for population structure or normal variation studies [NPNV=0]").append(lineBreak);
        }

        if (hasOntologies) {
            String t = "Data will be used to study: ";
            List<String> ontologiesText = new ArrayList<>();
            for (OntologyEntry ontology : ontologiesList) {
                String id = ontologyIdentifierBuilder(ontology.getId());
                if (rootId.equals(id)) {
                    if (!hasDiseases) {
                        sb.append(healthResearch).append("[HMB(CC)]").append(lineBreak);
                    }
                } else {
                    ontologiesText.add(blankSpace + ontology.getLabel() + blankSpace + id);
                }
            }
            if (!ontologiesText.isEmpty()) {
                sb.append(t).append(StringUtils.join(ontologiesText, ',')).append(lineBreak);
            }
        }

        if (Objects.nonNull(darData.getForProfit()) && darData.getForProfit()) {
            sb.append("Data will be used for commercial purpose [NPU] ").append(lineBreak);
        } else {
            sb.append("Data will not be used for commercial purpose").append(lineBreak);
        }

        if (Objects.nonNull(darData.getOneGender()) && darData.getOneGender()) {
            String gender = darData.getGender();
            if (Objects.nonNull(gender)) {
                String genderText = "Data will be used to study ONLY a XXXX population [RS-[GENDER]]";
                gender = gender.equals("M") ? "male" : "female";
                sb.append(genderText.replaceAll("XXXX", gender)).append(lineBreak);
            } else {
                throw new BadRequestException();
            }
        }

        if (Objects.nonNull(darData.getPediatric()) && darData.getPediatric()) {
            sb.append("Data will be used to study ONLY a pediatric population [RS-[PEDIATRIC]]").append(lineBreak);
        }
        return sb.toString();
    }

    private String ontologyIdentifierBuilder(String ontologyURI) {
        String s = "[XXXX(CC)]";
        String ontolotyId = ontologyURI.substring(ontologyURI.lastIndexOf("/") + 1);
        return s.replaceAll("XXXX", ontolotyId);

    }

}
