package org.broadinstitute.consent.http.service.users.handler;

import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.service.EmailNotifierAPI;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;

import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseResearcherAPI implements ResearcherAPI{

    private ResearcherPropertyDAO researcherPropertyDAO;
    private DACUserDAO dacUserDAO;
    private final EmailNotifierAPI emailApi;
    private DACUserAPI dacUserAPI = AbstractDACUserAPI.getInstance();
    private static final String ACTION_REGISTERED = "registered";
    private static final String ACTION_UPDATED = "updated";

    protected org.apache.log4j.Logger logger() {
        return org.apache.log4j.Logger.getLogger("DatabaseResearcherAPI");
    }

    public DatabaseResearcherAPI(ResearcherPropertyDAO researcherPropertyDAO, DACUserDAO dacUserDAO, EmailNotifierAPI emailApi) {
        this.researcherPropertyDAO = researcherPropertyDAO;
        this.dacUserDAO = dacUserDAO;
        this.emailApi = emailApi;
    }

    @Override
    public List<ResearcherProperty> registerResearcher(Map<String, String> researcherPropertiesMap, Integer userId, Boolean validate) throws NotFoundException, UnsupportedOperationException {
        validateUser(userId);
        researcherPropertiesMap.values().removeAll(Collections.singleton(null));
        if (validate) validateRequiredFields(researcherPropertiesMap);
        validateExistentFields(researcherPropertiesMap);
        List<ResearcherProperty> properties = getResearcherProperties(researcherPropertiesMap, userId);
        saveProperties(properties);
        notifyAdmins(userId, ACTION_REGISTERED);
        return describeResearcherProperties(userId);
    }

    @Override
    public List<ResearcherProperty> updateResearcher(Map<String, String> researcherPropertiesMap, Integer userId, Boolean validate) throws NotFoundException {
        validateUser(userId);
        researcherPropertiesMap.values().removeAll(Collections.singleton(null));
        if (validate) validateRequiredFields(researcherPropertiesMap);
        validateExistentFields(researcherPropertiesMap);
        Boolean isUpdatedProfileCompleted = Boolean.valueOf(researcherPropertiesMap.get(ResearcherFields.COMPLETED.getValue()));
        String completed = researcherPropertyDAO.isProfileCompleted(userId);
        Boolean isProfileCompleted = Boolean.valueOf(completed);
        List<ResearcherProperty> properties = getResearcherProperties(researcherPropertiesMap, userId);
        if (!isProfileCompleted && isUpdatedProfileCompleted) {
            saveProperties(properties);
            notifyAdmins(userId, ACTION_REGISTERED);
        } else if (hasUpdatedFields(userId, researcherPropertiesMap, isUpdatedProfileCompleted)) {
            deleteResearcherProperties(userId);
            saveProperties(properties);
            DACUserRole dacUserRole = new DACUserRole();
            dacUserRole.setStatus(RoleStatus.PENDING.toString());
            dacUserRole.setRoleId(5);
            dacUserAPI.updateRoleStatus(dacUserRole, userId);
            notifyAdmins(userId, ACTION_UPDATED);
        } else {
            saveProperties(properties);
        }
        return describeResearcherProperties(userId);
    }

    private void saveProperties(List<ResearcherProperty> properties) {
        researcherPropertyDAO.deletePropertiesByUserAndKey(properties);
        researcherPropertyDAO.insertAll(properties);
    }

    @Override
    public Map<String, String> describeResearcherPropertiesMap(Integer userId) {
        return describeResearcherProperties(userId).stream().collect(Collectors.toMap(ResearcherProperty::getPropertyKey,
                ResearcherProperty::getPropertyValue));
    }

    @Override
    public void deleteResearcherProperties(Integer userId) {
        researcherPropertyDAO.deleteAllPropertiesByUser(userId);
    }

    @Override
    public void deleteResearcherSpecificProperties(List<ResearcherProperty> properties) {
        researcherPropertyDAO.deletePropertiesByUserAndKey(properties);
    }

    @Override
    public Map<String, String> describeResearcherPropertiesForDAR(Integer userId) {
        Map<String, String> properties = describeResearcherPropertiesMap(userId);
        return getResearcherPropertiesForDAR(properties, userId);
    }

    private Map<String, String> getResearcherPropertiesForDAR(Map<String, String> properties, Integer userId) {
        Map<String, String> rpForDAR = new HashMap<>();
        rpForDAR.put(ResearcherFields.INVESTIGATOR.getValue(), properties.getOrDefault(ResearcherFields.PI_NAME.getValue(), dacUserDAO.findDACUserById(userId).getDisplayName()));
        rpForDAR.put(ResearcherFields.INSTITUTION.getValue(), properties.getOrDefault(ResearcherFields.INSTITUTION.getValue(),  null));
        rpForDAR.put(ResearcherFields.DEPARTMENT.getValue(), properties.getOrDefault(ResearcherFields.DEPARTMENT.getValue(), null));
        rpForDAR.put(ResearcherFields.STREET_ADDRESS_1.getValue(), properties.getOrDefault(ResearcherFields.STREET_ADDRESS_1.getValue(), null));
        rpForDAR.put(ResearcherFields.CITY.getValue(), properties.getOrDefault(ResearcherFields.CITY.getValue(), null));
        rpForDAR.put(ResearcherFields.ZIP_POSTAL_CODE.getValue(), properties.getOrDefault(ResearcherFields.ZIP_POSTAL_CODE.getValue(), null));
        rpForDAR.put(ResearcherFields.COUNTRY.getValue(), properties.getOrDefault(ResearcherFields.COUNTRY.getValue(), null));
        rpForDAR.put(ResearcherFields.STATE.getValue(), properties.getOrDefault(ResearcherFields.STATE.getValue(), null));
        rpForDAR.put(ResearcherFields.STREET_ADDRESS_2.getValue(), properties.getOrDefault(ResearcherFields.STREET_ADDRESS_2.getValue(), null));
        rpForDAR.put(ResearcherFields.DIVISION.getValue(), properties.getOrDefault(ResearcherFields.DIVISION.getValue(),  null));
        rpForDAR.put(ResearcherFields.ERA_COMMONS_ID.getValue(), properties.getOrDefault(ResearcherFields.ERA_COMMONS_ID.getValue(), null));
        rpForDAR.put(ResearcherFields.PUBMED_ID.getValue(), properties.getOrDefault(ResearcherFields.PUBMED_ID.getValue(), null));
        rpForDAR.put(ResearcherFields.PROFILE_NAME.getValue(), properties.getOrDefault(ResearcherFields.PROFILE_NAME.getValue(), null));
        rpForDAR.put(ResearcherFields.ACADEMIC_BUSINESS_EMAIL.getValue(), properties.getOrDefault(ResearcherFields.ACADEMIC_BUSINESS_EMAIL.getValue(), null));
        rpForDAR.put(ResearcherFields.SCIENTIFIC_URL.getValue(), properties.getOrDefault(ResearcherFields.SCIENTIFIC_URL.getValue(), null));
        rpForDAR.put(ResearcherFields.ARE_YOU_PRINCIPAL_INVESTIGATOR.getValue(), properties.getOrDefault(ResearcherFields.ARE_YOU_PRINCIPAL_INVESTIGATOR.getValue(), null));
        rpForDAR.put(ResearcherFields.PI_NAME.getValue(), properties.getOrDefault(ResearcherFields.PI_NAME.getValue(), null));
        rpForDAR.put(ResearcherFields.PI_EMAIL.getValue(), properties.getOrDefault(ResearcherFields.PI_EMAIL.getValue(), null));
        rpForDAR.put(ResearcherFields.COMPLETED.getValue(), properties.getOrDefault(ResearcherFields.COMPLETED.getValue(), null));
        rpForDAR.put(ResearcherFields.DO_YOU_HAVE_PI.getValue(), properties.getOrDefault(ResearcherFields.DO_YOU_HAVE_PI.getValue(), null));
        rpForDAR.put(ResearcherFields.LINKEDIN_PROFILE.getValue(), properties.getOrDefault(ResearcherFields.LINKEDIN_PROFILE.getValue(), null));
        rpForDAR.put(ResearcherFields.RESEARCHER_GATE.getValue(), properties.getOrDefault(ResearcherFields.RESEARCHER_GATE.getValue(), null));
        rpForDAR.put(ResearcherFields.ORCID.getValue(), properties.getOrDefault(ResearcherFields.ORCID.getValue(), null));
        rpForDAR.put(ResearcherFields.CHECK_NOTIFICATIONS.getValue(), properties.getOrDefault(ResearcherFields.CHECK_NOTIFICATIONS.getValue(), null));
        rpForDAR.put(ResearcherFields.ERA_EXPIRATION_DATE.getValue(), properties.getOrDefault(ResearcherFields.ERA_EXPIRATION_DATE.getValue(), null));
        rpForDAR.put(ResearcherFields.ERA_USERNAME.getValue(), properties.getOrDefault(ResearcherFields.ERA_USERNAME.getValue(), null));
        rpForDAR.put(ResearcherFields.ERA_STATUS.getValue(), properties.getOrDefault(ResearcherFields.ERA_STATUS.getValue(), null));
        rpForDAR.put(ResearcherFields.NAME_DAA.getValue(), properties.getOrDefault(ResearcherFields.NAME_DAA.getValue(), null));
        rpForDAR.put(ResearcherFields.URL_DAA.getValue(), properties.getOrDefault(ResearcherFields.URL_DAA.getValue(), null));
        return rpForDAR;
    }

    private void validateUser(Integer userId) {
        if(dacUserDAO.findDACUserById(userId) == null){
           throw new NotFoundException("User with id: " + userId + "does not exists");
        }
    }

    private List<ResearcherProperty> describeResearcherProperties(Integer userId) {
        validateUser(userId);
        return researcherPropertyDAO.findResearcherPropertiesByUser(userId);
    }

    private void validateRequiredFields(Map<String, String> properties) {
        List<ResearcherFields> requiredFields = ResearcherFields.getRequiredFields();
        requiredFields.forEach(rf -> {
            if (properties.get(rf.getValue()) == null) {
                throw new IllegalArgumentException(rf.getValue() + " is required.");
            }
        });
    }

    private void validateExistentFields(Map<String, String> properties) {
        properties.forEach((propertyKey, propertyValue) -> {
            if (!ResearcherFields.containsValue(propertyKey)) {
                throw new IllegalArgumentException(propertyKey + " is not a valid property.");
            }
        });
    }

    private List<ResearcherProperty> getResearcherProperties(Map<String, String> researcherPropertiesMap, Integer userId) {
        List<ResearcherProperty> properties = new ArrayList<>();
        researcherPropertiesMap.forEach((propertyKey, propertyValue) ->
            properties.add(new ResearcherProperty(userId, propertyKey, propertyValue))
        );
        return properties;
    }

    private Boolean hasUpdatedFields(Integer userId, Map<String, String> researcherPropertiesMap, Boolean isUpdatedProfileCompleted) {
        Boolean hasUpdatedFields = false;
        if(isUpdatedProfileCompleted){
            String institutionName = researcherPropertiesMap.getOrDefault(ResearcherFields.INSTITUTION.getValue(), "");
            String isThePI = researcherPropertiesMap.getOrDefault(ResearcherFields.ARE_YOU_PRINCIPAL_INVESTIGATOR.getValue(), null);
            String havePI = researcherPropertiesMap.getOrDefault(ResearcherFields.DO_YOU_HAVE_PI.getValue(), null);
            String eRACommonsID = researcherPropertiesMap.getOrDefault(ResearcherFields.ERA_COMMONS_ID.getValue(), "");
            String pubmedID = researcherPropertiesMap.getOrDefault(ResearcherFields.PUBMED_ID.getValue(), "");
            String scientificURL = researcherPropertiesMap.getOrDefault(ResearcherFields.SCIENTIFIC_URL.getValue(), "");
            if(StringUtils.isNotEmpty(eRACommonsID) && StringUtils.isEmpty(researcherPropertyDAO.findPropertyValueByPK(userId, ResearcherFields.ERA_COMMONS_ID.getValue())) ||
               StringUtils.isNotEmpty(pubmedID) && StringUtils.isEmpty(researcherPropertyDAO.findPropertyValueByPK(userId, ResearcherFields.PUBMED_ID.getValue())) ||
               StringUtils.isNotEmpty(scientificURL) && StringUtils.isEmpty(researcherPropertyDAO.findPropertyValueByPK(userId, ResearcherFields.SCIENTIFIC_URL.getValue()))) {
                hasUpdatedFields = true;
            } else if (CollectionUtils.isNotEmpty(researcherPropertyDAO.findResearcherProperties(userId, institutionName, isThePI, havePI, eRACommonsID, pubmedID, scientificURL))) {
                hasUpdatedFields = true;
            }
        }
        return hasUpdatedFields;
    }

    private void notifyAdmins(Integer userId, String action) {
        String completed = researcherPropertyDAO.isProfileCompleted(userId);
        if (completed != null && Boolean.valueOf(completed)) {
            try {
                emailApi.sendNewResearcherCreatedMessage(userId, action);
            } catch (IOException | TemplateException | MessagingException e) {
                logger().error("Error when notifying the admin(s) about the new researcher creation: " + dacUserDAO.findDACUserById(userId).getDisplayName());
            }
        }
    }
}
