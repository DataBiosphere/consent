package org.broadinstitute.consent.http.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class DataAccessParser {

    private static final Logger log = LoggerFactory.getLogger(DataAccessParser.class);

    PDAcroForm fillDARForm(Document dar, Map<String, String> researcherProperties, DACUserRole role, Boolean manualReview, PDAcroForm acroForm, String sDUR) throws IOException {
        for (PDField field : acroForm.getFields()) {
            String fieldName = field.getFullyQualifiedName();
            switch (fieldName) {
                 case DarConstants.INSTITUTION: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.INSTITUTION)));
                    break;
                }
                case DarConstants.DEPARTMENT: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.DEPARTMENT)));
                    break;
                }
                case DarConstants.DIVISION: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.DIVISION)));
                    break;
                }
                case DarConstants.STREET_ADDRESS_1: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.STREET_ADDRESS_1)));
                    break;
                }
                case DarConstants.STREET_ADDRESS_2: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.STREET_ADDRESS_2)));
                    break;
                }
                case DarConstants.CITY: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.CITY)));
                    break;
                }
                case DarConstants.ZIP_POSTAL_CODE: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.ZIP_POSTAL_CODE)));
                    break;
                }
                case DarConstants.COUNTRY: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.COUNTRY)));
                    break;
                }
                case DarConstants.STATE: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.STATE)));
                    break;
                }
                case DarConstants.CONTROLS: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.CONTROLS)));
                    break;
                }
                case DarConstants.POPULATION: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.POPULATION)));
                    break;
                }
                case DarConstants.DISEASES: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.DISEASES)));
                    break;
                }
                case DarConstants.METHODS: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.METHODS)));
                    break;
                }
                case DarConstants.OTHER: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.OTHER)));
                    break;
                }
                case DarConstants.OTHER_TEXT: {
                    field.setValue(getDefaultValue(dar, dar.getString(DarConstants.OTHER_TEXT)));
                    break;
                }
                case DarConstants.PROFILE_NAME: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.PROFILE_NAME)));
                    break;
                }
                case DarConstants.ACADEMIC_BUSINESS_EMAIL: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.ACADEMIC_BUSINESS_EMAIL)));
                    break;
                }
                case DarConstants.ERA_COMMONS_ID: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.ERA_COMMONS_ID)));
                    break;
                }
                case DarConstants.PUBMED_ID: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.PUBMED_ID)));
                    break;
                }
                case DarConstants.SCIENTIFIC_URL: {
                    field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.SCIENTIFIC_URL)));
                    break;
                }
                case DarConstants.PI_EMAIL: {
                    if(Boolean.valueOf(researcherProperties.get("isThePI"))){
                        field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.ACADEMIC_BUSINESS_EMAIL)));
                    } else {
                        field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.PI_EMAIL)));
                    }

                    break;
                }
                case DarConstants.INVESTIGATOR: {
                    if(Boolean.valueOf(researcherProperties.get("isThePI"))){
                        field.setValue(getDefaultValue(dar, researcherProperties.get(DarConstants.PROFILE_NAME)));
                    } else {
                        field.setValue(getDefaultValue(dar, dar.getString(DarConstants.INVESTIGATOR)));
                    }
                    break;
                }
                case DarConstants.PROJECT_TITLE: {
                    field.setValue(getDefaultValue(dar, dar.getString(DarConstants.PROJECT_TITLE)));
                    break;
                }
                case DarConstants.DATASET_ID: {
                    String parsedDatasets = parseDatasetDetail(dar);
                    field.setValue(parsedDatasets);
                    break;
                }
                case DarConstants.RUS: {
                    field.setValue(getDefaultValue(dar, dar.getString(DarConstants.RUS)));
                    break;
                }
                case DarConstants.NON_TECH_RUS: {
                    field.setValue(getDefaultValue(dar, dar.getString(DarConstants.NON_TECH_RUS)));
                    break;
                }
                case DarConstants.ONTOLOGIES: {
                    List<String> ontologies = generateDiseasesSummary(dar);
                    field.setValue(CollectionUtils.isNotEmpty(ontologies) ? String.join(", ", ontologies) :  "--");
                    break;
                }
                case DarConstants.FOR_PROFIT: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.FOR_PROFIT)));
                    break;
                }
                case DarConstants.ONE_GENDER: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.ONE_GENDER)));
                    break;
                }
                case DarConstants.GENDER: {
                    field.setValue(getDefaultValue(dar, dar.getString(DarConstants.GENDER)));
                    break;
                }
                case DarConstants.PEDIATRIC: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.PEDIATRIC)));
                    break;
                }
                case DarConstants.ILLEGAL_BEHAVE: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.ILLEGAL_BEHAVE)));
                    break;
                }
                case DarConstants.ADDICTION: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.ADDICTION)));
                    break;
                }
                case DarConstants.SEXUAL_DISEASES: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.SEXUAL_DISEASES)));
                    break;
                }
                case DarConstants.STIGMATIZED_DISEASES: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.STIGMATIZED_DISEASES)));
                    break;
                }
                case DarConstants.VULNERABLE_POP: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.VULNERABLE_POP)));
                    break;
                }
                case DarConstants.POP_MIGRATION: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.POP_MIGRATION)));
                    break;
                }
                case DarConstants.PSYCH_TRAITS: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.PSYCH_TRAITS)));
                    break;
                }
                case DarConstants.NOT_HEALTH: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.NOT_HEALTH)));
                    break;
                }
                case DarConstants.HEALTH: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.HMB)));
                    break;
                }
                case DarConstants.ORIGINS: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.POA)));
                    break;
                }
                case DarConstants.CHECK_COLLABORATOR: {
                    field.setValue(getYesOrNoValue(dar.getBoolean(DarConstants.CHECK_COLLABORATOR)));
                    break;
                }
                case DarConstants.USER_STATUS: {
                    field.setValue(getDefaultValue(dar, StringUtils.capitalize(role.getStatus())));
                    break;
                }
                case DarConstants.ADMIN_COMMENT: {
                    field.setValue(getDefaultValue(dar, StringUtils.capitalize(role.getRationale())));
                    break;
                }
                case DarConstants.MANUAL_REVIEW: {
                    field.setValue(getDefaultValue(dar, checkRequiresManualReview(manualReview)));
                    break;
                }
                case DarConstants.NIH_USERNAME: {
                    field.setValue(getDefaultValue(dar, dar.getString(DarConstants.NIH_USERNAME)));
                    break;
                }
                case DarConstants.LINKEDIN: {
                    field.setValue(getDefaultValue(dar, dar.getString(DarConstants.LINKEDIN)));
                    break;
                }
                case DarConstants.ORCID: {
                    field.setValue(getDefaultValue(dar, dar.getString(DarConstants.ORCID)));
                    break;
                }
                case DarConstants.RESEARCHER_GATE: {
                    field.setValue(getDefaultValue(dar, dar.getString(DarConstants.RESEARCHER_GATE)));
                    break;
                }
                case DarConstants.DATA_ACCESS_AGREEMENT: {
                    Boolean existDataAccessAgreement = (StringUtils.isNotEmpty(dar.getString(DarConstants.DATA_ACCESS_AGREEMENT_URL)) &&
                            StringUtils.isNotEmpty(dar.getString(DarConstants.DATA_ACCESS_AGREEMENT_NAME)));
                    field.setValue(getYesOrNoValue(existDataAccessAgreement));
                    break;
                }
                case DarConstants.TRANSLATED_RESTRICTION: {
                    field.setValue(getDefaultValue(dar, sDUR.replaceAll("<[^>]*>","\n")));
                    break;
                }
            }
        }
        return acroForm;
    }

    /**
     * PDFBox has a lot of trouble with non-printable characters. Strip from the PDF content and
     * log a warning so we can follow up with the user and/or clean up the Data Access Request.
     *
     * @param dar Data Access Request Document
     * @param value String value to filter
     * @return Filtered string value
     */
    private String filterNonPrintableValue(Document dar, String value) {
        String NON_PRINTABLE_PATTERN = "\\P{Print}";
        if (Pattern.compile(NON_PRINTABLE_PATTERN).matcher(value).find()) {
            return value.replaceAll(NON_PRINTABLE_PATTERN, "");
        }
        return value;
    }
    
    @SuppressWarnings("unchecked")
    private String parseDatasetDetail(Document dar) {
        ArrayList<Document> datasetDetail = (ArrayList<Document>) dar.get(DarConstants.DATASET_DETAIL);
        Map<String, String> datasetDetailMap = new HashMap<>();

        datasetDetail.forEach((doc) -> {
            String objectId = doc.getString(DarConstants.OBJECT_ID) != null ? " | ".concat(doc.getString(DarConstants.OBJECT_ID)) : "";
            datasetDetailMap.put(doc.getString("name"), objectId);
        });

        return filterNonPrintableValue(
                dar,
                datasetDetailMap.
                        entrySet().
                        stream().
                        map(entry -> entry.getKey() + entry.getValue()).
                        collect(Collectors.joining("\n")));
    }

    private String getYesOrNoValue(Boolean value){
        return value == null || !value ? "No" : "Yes";
    }

    private String getDefaultValue(Document dar, String value){
        return StringUtils.isEmpty(value) ? "--" : filterNonPrintableValue(dar, value);
    }

    @SuppressWarnings("unchecked")
    private List<String> generateDiseasesSummary(Document darDocument) {
        List<Map<String, String>> ontologies = (List<Map<String, String>>) darDocument.get(DarConstants.ONTOLOGIES);
        List<String> diseases = new ArrayList<>();
        if(!CollectionUtils.isEmpty(ontologies)) {
           for (Map<String, String> ontology : ontologies) {
                diseases.add(filterNonPrintableValue(darDocument, ontology.get("label")));
            }
        }
        return diseases;
    }

    private String checkRequiresManualReview(Boolean requiresManualReview) {
        if (requiresManualReview) {
            return "THIS DATA ACCESS REQUEST REQUIRES MANUAL REVIEW";
        }
        return "THIS DATA ACCESS REQUEST DOES NOT REQUIRE MANUAL REVIEW";
    }

}
