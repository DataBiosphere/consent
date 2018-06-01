package org.broadinstitute.consent.http.service;

import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import java.io.IOException;
import java.util.Map;

public class DataAccessParser {

    public PDAcroForm fillDARForm(Document dar, Map<String, String> researcherProperties, PDAcroForm acroForm) throws IOException {
        for (PDField field : acroForm.getFields()) {
            String fieldName = field.getFullyQualifiedName();
            switch (fieldName) {
                case DarConstants.PROFILE_NAME: {
                    field.setValue(dar.getString(DarConstants.PROFILE_NAME));
                    break;
                }
                case DarConstants.ACADEMIC_BUSINESS_EMAIL: {
                    field.setValue(dar.getString(DarConstants.ACADEMIC_BUSINESS_EMAIL));
                    break;
                }
                case DarConstants.INSTITUTION: {
                    field.setValue(researcherProperties.get(DarConstants.INSTITUTION));
                    break;
                }
                case DarConstants.DEPARTMENT: {
                    field.setValue(researcherProperties.get(DarConstants.DEPARTMENT));
                    break;
                }
                case DarConstants.STREET_ADDRESS_1: {
                    field.setValue(researcherProperties.get(DarConstants.STREET_ADDRESS_1));
                    break;
                }
                case DarConstants.CITY: {
                    field.setValue(researcherProperties.get(DarConstants.CITY));
                    break;
                }
                case DarConstants.ZIP_POSTAL_CODE: {
                    field.setValue(researcherProperties.get(DarConstants.ZIP_POSTAL_CODE));
                    break;
                }
                case DarConstants.COUNTRY: {
                    field.setValue(researcherProperties.get(DarConstants.COUNTRY));
                    break;
                }
                case DarConstants.DIVISION: {
                    field.setValue(researcherProperties.get(DarConstants.DIVISION));
                    break;
                }
                case DarConstants.STREET_ADDRESS_2: {
                    field.setValue(researcherProperties.get(DarConstants.STREET_ADDRESS_2));
                    break;
                }
                case DarConstants.STATE: {
                    field.setValue(researcherProperties.get(DarConstants.STATE));
                    break;
                }
                case DarConstants.ERA_COMMONS_ID: {
                    field.setValue(dar.getString(DarConstants.ERA_COMMONS_ID));
                    break;
                }
                case DarConstants.PUBMED_ID: {
                    field.setValue(dar.getString(DarConstants.PUBMED_ID));
                    break;
                }
                case DarConstants.SCIENTIFIC_URL: {
                    field.setValue(dar.getString(DarConstants.SCIENTIFIC_URL));
                    break;
                }
                case DarConstants.PI_NAME: {
                    field.setValue(dar.getString(DarConstants.PI_NAME));
                    break;
                }
                case DarConstants.PI_EMAIL: {
                    field.setValue(dar.getString(DarConstants.PI_EMAIL));
                    break;
                }
                case DarConstants.PROJECT_TITLE: {
                    field.setValue(dar.getString(DarConstants.PROJECT_TITLE));
                    break;
                }
                case DarConstants.DATASET_ID: {
                    field.setValue(dar.getString(DarConstants.DATASET_ID));
                    break;
                }
                case DarConstants.RESEARCH_PURPOSE: {
                    field.setValue(dar.getString(DarConstants.RESEARCH_PURPOSE));
                    break;
                }
                case DarConstants.NON_TECH_SUMMARY: {
                    field.setValue(dar.getString(DarConstants.NON_TECH_SUMMARY));
                    break;
                }
                case DarConstants.RESEARCH_DISEASES: {
                    field.setValue(dar.getString(DarConstants.RESEARCH_DISEASES));
                    break;
                }
                case DarConstants.RESEARCH_METHODS: {
                    field.setValue(dar.getString(DarConstants.RESEARCH_METHODS));
                    break;
                }
                case DarConstants.RESEARCH_CONTROLS: {
                    field.setValue(dar.getString(DarConstants.RESEARCH_CONTROLS));
                    break;
                }
                case DarConstants.RESEARCH_POPULATION: {
                    field.setValue(dar.getString(DarConstants.RESEARCH_POPULATION));
                    break;
                }
                case DarConstants.RESEARCH_OTHER: {
                    field.setValue(dar.getString(DarConstants.RESEARCH_OTHER));
                    break;
                }
                case DarConstants.RESEARCH_OTHER_TEXT: {
                    field.setValue(dar.getString(DarConstants.RESEARCH_OTHER_TEXT));
                    break;
                }
                case DarConstants.DISEASE_AREAS: {
                    field.setValue(dar.getString(DarConstants.DISEASE_AREAS));
                    break;
                }
                case DarConstants.PS_COMMERCIAL: {
                    field.setValue(dar.getString(DarConstants.PS_COMMERCIAL));
                    break;
                }
                case DarConstants.PS_GENDER: {
                    field.setValue(dar.getString(DarConstants.PS_GENDER));
                    break;
                }
                case DarConstants.PS_PEDIATRIC: {
                    field.setValue(dar.getString(DarConstants.PS_PEDIATRIC));
                    break;
                }
                case DarConstants.PS_ILLEGAL: {
                    field.setValue(dar.getString(DarConstants.PS_ILLEGAL));
                    break;
                }
                case DarConstants.PS_ADDICTION: {
                    field.setValue(dar.getString(DarConstants.PS_ADDICTION));
                    break;
                }
                case DarConstants.PS_SEXUAL_DISEASE: {
                    field.setValue(dar.getString(DarConstants.PS_SEXUAL_DISEASE));
                    break;
                }
                case DarConstants.PS_STIGMA_DISEASE: {
                    field.setValue(dar.getString(DarConstants.PS_STIGMA_DISEASE));
                    break;
                }
                case DarConstants.PS_VULNERABLE_POP: {
                    field.setValue(dar.getString(DarConstants.PS_VULNERABLE_POP));
                    break;
                }
                case DarConstants.PS_MIGRATION: {
                    field.setValue(dar.getString(DarConstants.PS_MIGRATION));
                    break;
                }
                case DarConstants.PS_PSYCH_TRAITS: {
                    field.setValue(dar.getString(DarConstants.PS_PSYCH_TRAITS));
                    break;
                }
                case DarConstants.PS_NOT_HEALTH: {
                    field.setValue(dar.getString(DarConstants.PS_NOT_HEALTH));
                    break;
                }
                default: {
                    field.setValue("--");
                    break;
                }

            }
        }
        return acroForm;
    }
}
