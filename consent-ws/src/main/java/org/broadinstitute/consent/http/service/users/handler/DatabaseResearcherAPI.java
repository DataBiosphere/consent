package org.broadinstitute.consent.http.service.users.handler;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.ResearcherProperty;

import javax.ws.rs.NotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class DatabaseResearcherAPI implements ResearcherAPI{

    private ResearcherPropertyDAO researcherPropertyDAO;
    private DACUserDAO dacUserDAO;

    public DatabaseResearcherAPI(ResearcherPropertyDAO researcherPropertyDAO, DACUserDAO dacUserDAO) {
        this.researcherPropertyDAO = researcherPropertyDAO;
        this.dacUserDAO = dacUserDAO;
    }

    @Override
    public List<ResearcherProperty> registerResearcher(Map<String, String> researcherPropertiesMap, Integer userId, Boolean validate) throws NotFoundException, UnsupportedOperationException {
        validateUser(userId);
        checkExistentProperties(userId);
        researcherPropertiesMap.values().removeAll(Collections.singleton(null));
        if(validate) validateRequiredFields(researcherPropertiesMap);
        validateExistentFields(researcherPropertiesMap);
        List<ResearcherProperty> properties = getResearcherProperties(researcherPropertiesMap, userId, new Date(), null);
        researcherPropertyDAO.insertAll(properties);
        return describeResearcherProperties(userId);
    }

    @Override
    public List<ResearcherProperty> updateResearcher(Map<String, String> researcherPropertiesMap, Integer userId, Boolean validate) throws NotFoundException {
        validateUser(userId);
        researcherPropertiesMap.values().removeAll(Collections.singleton(null));
        if(validate) validateRequiredFields(researcherPropertiesMap);
        validateExistentFields(researcherPropertiesMap);
        List<ResearcherProperty> properties = getResearcherProperties(researcherPropertiesMap, userId, null, new Date());
        researcherPropertyDAO.deleteAllPropertiesByUser(userId);
        researcherPropertyDAO.insertAll(properties);
        return describeResearcherProperties(userId);
    }

    @Override
    public Map<String, String> describeResearcherPropertiesMap(Integer userId) throws NotFoundException {
        return describeResearcherProperties(userId).stream().collect(Collectors.toMap(ResearcherProperty::getPropertyKey,
                        ResearcherProperty::getPropertyValue));
    }

    @Override
    public void deleteResearcherProperties(Integer userId) {
        researcherPropertyDAO.deleteAllPropertiesByUser(userId);
    }

    @Override
    public Map<String, String> describeResearcherPropertiesForDAR(Integer userId) {
        Map<String, String> properties = describeResearcherPropertiesMap(userId);
        return getResearcherPropertiesForDAR(properties, userId);
    }

    private Map<String, String> getResearcherPropertiesForDAR(Map<String, String> properties, Integer userId) {
        Map<String, String> rpForDAR = new HashMap<>();
        rpForDAR.put(ResearcherFields.INVESTIGATOR.getValue(), properties.containsKey(ResearcherFields.PI_NAME.getValue()) ? properties.get(ResearcherFields.PI_NAME.getValue()) : dacUserDAO.findDACUserById(userId).getDisplayName());
        rpForDAR.put(ResearcherFields.INSTITUTION.getValue(), properties.containsKey(ResearcherFields.INSTITUTION.getValue()) ? properties.get(ResearcherFields.INSTITUTION.getValue()) : null);
        rpForDAR.put(ResearcherFields.DEPARTMENT.getValue(), properties.containsKey(ResearcherFields.DEPARTMENT.getValue()) ? properties.get(ResearcherFields.DEPARTMENT.getValue()) : null);
        rpForDAR.put(ResearcherFields.STREET_ADDRESS_1.getValue(), properties.containsKey(ResearcherFields.STREET_ADDRESS_1.getValue()) ? properties.get(ResearcherFields.STREET_ADDRESS_1.getValue()) : null);
        rpForDAR.put(ResearcherFields.CITY.getValue(), properties.containsKey(ResearcherFields.CITY.getValue()) ? properties.get(ResearcherFields.CITY.getValue()) : null);
        rpForDAR.put(ResearcherFields.ZIP_POSTAL_CODE.getValue(), properties.containsKey(ResearcherFields.ZIP_POSTAL_CODE.getValue()) ? properties.get(ResearcherFields.ZIP_POSTAL_CODE.getValue()) : null);
        rpForDAR.put(ResearcherFields.COUNTRY.getValue(), properties.containsKey(ResearcherFields.COUNTRY.getValue()) ? properties.get(ResearcherFields.COUNTRY.getValue()) : null);
        rpForDAR.put(ResearcherFields.STATE.getValue(), properties.containsKey(ResearcherFields.STATE.getValue()) ? properties.get(ResearcherFields.STATE.getValue()) : null);
        rpForDAR.put(ResearcherFields.STREET_ADDRESS_2.getValue(), properties.containsKey(ResearcherFields.STREET_ADDRESS_2.getValue()) ? properties.get(ResearcherFields.STREET_ADDRESS_2.getValue()) : null);
        rpForDAR.put(ResearcherFields.DIVISION.getValue(), properties.containsKey(ResearcherFields.DIVISION.getValue()) ? properties.get(ResearcherFields.DIVISION.getValue()) : null);
        return rpForDAR;
    }

    private void validateUser(Integer userId) throws NotFoundException{
        if(dacUserDAO.findDACUserById(userId) == null){
            throw new NotFoundException("User with id: " + userId + "does not exists");
        }
    }

    private List<ResearcherProperty> describeResearcherProperties(Integer userId) throws NotFoundException {
        validateUser(userId);
        return researcherPropertyDAO.findResearcherPropertiesByUser(userId);
    }

    private void validateRequiredFields(Map<String, String> properties) {
        List<ResearcherFields> requiredFields = ResearcherFields.getRequiredFields();
        requiredFields.stream().forEach(rf -> {
            if(properties.get(rf.getValue()) == null) {
                throw new IllegalArgumentException(rf.getValue() + " is required.");
            }
        });
    }

    private void validateExistentFields(Map<String, String> properties){
        properties.forEach((propertyKey, propertyValue) -> {
          if(!ResearcherFields.containsValue(propertyKey)){
            throw new IllegalArgumentException(propertyKey + " is not a valid property.");
          }
        });
    }

    private List<ResearcherProperty> getResearcherProperties(Map<String, String> researcherPropertiesMap, Integer userId, Date createDate, Date updateDate) {
        List<ResearcherProperty> properties = new ArrayList<>();
        researcherPropertiesMap.forEach((propertyKey, propertyValue) -> {
            properties.add(new ResearcherProperty(userId, propertyKey, propertyValue));
        });
        return properties;
    }

    private void checkExistentProperties(Integer userId) {
        if(!CollectionUtils.isEmpty(researcherPropertyDAO.findResearcherPropertiesByUser(userId))){
            throw new UnsupportedOperationException("User have already created properties");
        }
    }
}
