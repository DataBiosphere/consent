package org.broadinstitute.consent.http.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataAccessParser {

    public PDAcroForm fillDARForm(Document dar, Map<String, String> researcherProperties, PDAcroForm acroForm) throws IOException {
        for (PDField field : acroForm.getFields()) {
            String fieldName = field.getFullyQualifiedName();
            switch (fieldName) {
                // STEP 1
                case DarConstants.INVESTIGATOR: {
                    field.setValue(getDefaultValue(dar.getString(DarConstants.INVESTIGATOR)));
                    break;
                }
                case DarConstants.INSTITUTION: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.INSTITUTION)));
                    break;
                }
                case DarConstants.DEPARTMENT: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.DEPARTMENT)));
                    break;
                }
                case DarConstants.DIVISION: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.DIVISION)));
                    break;
                }
                case DarConstants.STREET_ADDRESS_1: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.STREET_ADDRESS_1)));
                    break;
                }
                case DarConstants.STREET_ADDRESS_2: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.STREET_ADDRESS_2)));
                    break;
                }
                case DarConstants.CITY: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.CITY)));
                    break;
                }
                case DarConstants.ZIP_POSTAL_CODE: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.ZIP_POSTAL_CODE)));
                    break;
                }
                case DarConstants.COUNTRY: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.COUNTRY)));
                    break;
                }
                case DarConstants.STATE: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.STATE)));
                    break;
                }
                // STEP 2
                case DarConstants.CONTROLS: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.CONTROLS)));
                    break;
                }
                case DarConstants.POPULATION: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.POPULATION)));
                    break;
                }
                case DarConstants.DISEASES: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.DISEASES)));
                    break;
                }
                case DarConstants.METHODS: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.METHODS)));
                    break;
                }
                case DarConstants.OTHER: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.OTHER)));
                    break;
                }
                case DarConstants.OTHER_TEXT: {
                    field.setValue(getDefaultValue(dar.getString(DarConstants.OTHER_TEXT)));
                    break;
                }
                case DarConstants.PROFILE_NAME: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.PROFILE_NAME)));
                    break;
                }
                case DarConstants.ACADEMIC_BUSINESS_EMAIL: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.ACADEMIC_BUSINESS_EMAIL)));
                    break;
                }
                case DarConstants.ERA_COMMONS_ID: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.ERA_COMMONS_ID)));
                    break;
                }
                case DarConstants.PUBMED_ID: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.PUBMED_ID)));
                    break;
                }
                case DarConstants.SCIENTIFIC_URL: {
                    field.setValue(getDefaultValue(researcherProperties.get(DarConstants.SCIENTIFIC_URL)));
                    break;
                }
                case DarConstants.PI_EMAIL: {
                    field.setValue(getDefaultValue(dar.getString(DarConstants.PI_EMAIL)));
                    break;
                }
                case DarConstants.PROJECT_TITLE: {
                    field.setValue(getDefaultValue(dar.getString(DarConstants.PROJECT_TITLE)));
                    break;
                }
                case DarConstants.DATASET_ID: {
                    List<String> datasetIds = dar.get(DarConstants.DATASET_ID, List.class);
                    field.setValue(datasetIds != null ? String.join(", ", datasetIds) :  "--");
                    break;
                }
                case DarConstants.RUS: {
                    field.setValue(getDefaultValue(dar.getString(DarConstants.RUS)));
                    break;
                }
                case DarConstants.NON_TECH_RUS: {
                    field.setValue(getDefaultValue(dar.getString(DarConstants.NON_TECH_RUS)));
                    break;
                }
                case DarConstants.ONTOLOGIES: {
                    List<String> ontologies = generateDiseasesSummary(dar);
                    field.setValue(CollectionUtils.isNotEmpty(ontologies) ? String.join(", ", ontologies) :  "--");
                    break;
                }
                // STEP 3
                case DarConstants.FOR_PROFIT: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.FOR_PROFIT)));
                    break;
                }
                case DarConstants.ONE_GENDER: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.ONE_GENDER)));
                    break;
                }
                case DarConstants.GENDER: {
                    field.setValue(getDefaultValue(dar.getString(DarConstants.GENDER)));
                    break;
                }
                case DarConstants.PEDIATRIC: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.PEDIATRIC)));
                    break;
                }
                case DarConstants.ILLEGAL_BEHAVE: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.ILLEGAL_BEHAVE)));
                    break;
                }
                case DarConstants.ADDICTION: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.ADDICTION)));
                    break;
                }
                case DarConstants.SEXUAL_DISEASES: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.SEXUAL_DISEASES)));
                    break;
                }
                case DarConstants.STIGMATIZED_DISEASES: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.STIGMATIZED_DISEASES)));
                    break;
                }
                case DarConstants.VULNERABLE_POP: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.VULNERABLE_POP)));
                    break;
                }
                case DarConstants.POP_MIGRATION: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.POP_MIGRATION)));
                    break;
                }
                case DarConstants.PSYCH_TRAITS: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.PSYCH_TRAITS)));
                    break;
                }
                case DarConstants.NOT_HEALTH: {
                    field.setValue(getYesOrNotValue(dar.getBoolean(DarConstants.NOT_HEALTH)));
                    break;
                }
            }
        }
        return acroForm;
    }

    private String getYesOrNotValue(Boolean value){
        String YES = "Yes", NO = "No";
        return value == null || !value ? NO : YES;
    }

    private String getDefaultValue(String value){
        return StringUtils.isEmpty(value) ? "--" : value;
    }

    private List<String> generateDiseasesSummary(Document darDocument) {
        List<Map<String, String>> ontologies = (List<Map<String, String>>) darDocument.get("ontologies");
        List<String> diseases = new ArrayList<>();
        if(!CollectionUtils.isEmpty(ontologies)) {
           for (Map<String, String> ontology : ontologies) {
                diseases.add(ontology.get("label"));
            }
        }
        return diseases;
    }
}
