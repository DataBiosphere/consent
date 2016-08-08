package org.broadinstitute.consent.http.enumeration;

import java.util.ArrayList;
import java.util.List;

public enum ResearcherFields {

    PROFILE_NAME("profileName", false),
    ACADEMIC_BUSINESS_EMAIL("academicEmail", true),
    INSTITUTION("institution", true),
    DEPARTMENT("department", true),
    STREET_ADDRESS_1("address1", true),
    CITY("city", true),
    ZIP_POSTAL_CODE("zipcode", true),
    ARE_YOU_PRINCIPAL_INVESTIGATOR("isThePI", true),
    COUNTRY("country", true),
    DIVISION("division", false),
    STREET_ADDRESS_2("address2", false),
    STATE("state", false),
    ERA_COMMONS_ID("eRACommonsID", false),
    PUBMED_ID("pubmedID", false),
    SCIENTIFIC_URL("scientificURL", false),
    DO_YOU_HAVE_PI("havePI", false),
    PI_NAME("piName", false),
    PI_EMAIL("piEmail", false),
    PI_eRA_COMMONS_ID("piERACommonsID", false),
    COMPLETED("completed", false),
    INVESTIGATOR("investigator", false);


    private String value;
    private Boolean required;

    ResearcherFields(String value, Boolean required) {
        this.value = value;
        this.required = required;
    }

    public String getValue() {
        return value;
    }

    public Boolean getRequired() {return required;}

    public static List<ResearcherFields> getRequiredFields(){
        List<ResearcherFields> requiredValues = new ArrayList<>();
        for (ResearcherFields researcherField : ResearcherFields.values()) {
            if(researcherField.getRequired()){
                requiredValues.add(researcherField);
            }
        }
        return requiredValues;
    }

    public static Boolean containsValue(String value){
        for (ResearcherFields researcherField : ResearcherFields.values()) {
            if(researcherField.getValue().equals(value)){
                return true;
            }
        }
        return false;
    }



}