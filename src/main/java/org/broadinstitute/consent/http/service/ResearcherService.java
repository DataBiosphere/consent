package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ResearcherService {
    private final UserPropertyDAO userPropertyDAO;
    private final UserDAO userDAO;
    private final EmailNotifierService emailNotifierService;
    private final UserService userService;
    private static final String ACTION_REGISTERED = "registered";

    protected Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    @Inject
    public ResearcherService(UserPropertyDAO userPropertyDAO, UserDAO userDAO, EmailNotifierService emailNotifierService, UserService userService) {
        this.userPropertyDAO = userPropertyDAO;
        this.userDAO = userDAO;
        this.emailNotifierService = emailNotifierService;
        this.userService = userService;
    }

    public List<UserProperty> setProperties(Map<String, String> researcherPropertiesMap, AuthUser authUser) throws NotFoundException, IllegalArgumentException {
        User user = validateAuthUser(authUser);
        researcherPropertiesMap.values().removeAll(Collections.singleton(null));
        Map<String, String> validatedProperties = validateExistentFields(researcherPropertiesMap);
        List<UserProperty> properties = getResearcherProperties(validatedProperties, user.getDacUserId());
        saveProperties(properties);
        notifyAdmins(user.getDacUserId());
        return describeResearcherProperties(user.getDacUserId());
    }

    public List<UserProperty> updateProperties(Map<String, String> researcherPropertiesMap, AuthUser authUser, Boolean validate) throws NotFoundException, IllegalArgumentException {
        User user = validateAuthUser(authUser);
        researcherPropertiesMap.values().removeAll(Collections.singleton(null));
        if (validate) validateRequiredFields(researcherPropertiesMap);
        Map<String, String> validatedProperties = validateExistentFields(researcherPropertiesMap);
        boolean isUpdatedProfileCompleted = Boolean.parseBoolean(validatedProperties.get(UserFields.COMPLETED.getValue()));
        String completed = userPropertyDAO.isProfileCompleted(user.getDacUserId());
        boolean isProfileCompleted = Boolean.parseBoolean(completed);
        List<UserProperty> properties = getResearcherProperties(validatedProperties, user.getDacUserId());
        if (!isProfileCompleted && isUpdatedProfileCompleted) {
            saveProperties(properties);
            notifyAdmins(user.getDacUserId());
        } else if (hasUpdatedFields(user.getDacUserId(), validatedProperties, isUpdatedProfileCompleted)) {
            deleteResearcherProperties(user.getDacUserId());
            saveProperties(properties);
            userService.updateUserStatus(RoleStatus.PENDING.toString(), user.getDacUserId());
        } else {
            saveProperties(properties);
        }
        return describeResearcherProperties(user.getDacUserId());
    }

    private void saveProperties(List<UserProperty> properties) {
        userPropertyDAO.deletePropertiesByUserAndKey(properties);
        userPropertyDAO.insertAll(properties);
    }

    public Map<String, String> describeResearcherPropertiesMap(Integer userId) {
        return describeResearcherProperties(userId).stream().collect(Collectors.toMap(UserProperty::getPropertyKey,
                UserProperty::getPropertyValue));
    }

    public void deleteResearcherProperties(Integer userId) {
        userPropertyDAO.deleteAllPropertiesByUser(userId);
    }

    public void deleteResearcherSpecificProperties(List<UserProperty> properties) {
        userPropertyDAO.deletePropertiesByUserAndKey(properties);
    }

    private void validateUser(Integer userId) {
        if (userDAO.findUserById(userId) == null) {
            throw new NotFoundException("User with id: " + userId + " does not exists");
        }
    }

    private User validateAuthUser(AuthUser authUser) {
        User user = userDAO.findUserByEmail(authUser.getEmail());
        if (user == null) {
            throw new NotFoundException("Auth User with email: " + authUser.getEmail() + " does not exist");
        }
        return user;
    }

    private List<UserProperty> describeResearcherProperties(Integer userId) {
        validateUser(userId);
        return userPropertyDAO.findResearcherPropertiesByUser(userId);
    }

    private void validateRequiredFields(Map<String, String> properties) {
        List<UserFields> requiredFields = UserFields.getRequiredFields();
        requiredFields.forEach(rf -> {
            if (properties.get(rf.getValue()) == null) {
                throw new IllegalArgumentException(rf.getValue() + " is required.");
            }
        });
    }

    private Map<String, String> validateExistentFields(Map<String, String> properties) {
        Map<String, String> newProps = new HashMap<>();
        properties.forEach((propertyKey, propertyValue) -> {
            if (UserFields.containsValue(propertyKey)) {
                newProps.put(propertyKey, propertyValue);
            }
        });
        return newProps;
    }

    private List<UserProperty> getResearcherProperties(Map<String, String> researcherPropertiesMap, Integer userId) {
        List<UserProperty> properties = new ArrayList<>();
        researcherPropertiesMap.forEach((propertyKey, propertyValue) ->
                properties.add(new UserProperty(userId, propertyKey, propertyValue))
        );
        return properties;
    }

    private Boolean hasUpdatedFields(Integer userId, Map<String, String> researcherPropertiesMap, Boolean isUpdatedProfileCompleted) {
        boolean hasUpdatedFields = false;
        if (isUpdatedProfileCompleted) {
            String isThePI = researcherPropertiesMap.getOrDefault(UserFields.ARE_YOU_PRINCIPAL_INVESTIGATOR.getValue(), null);
            String havePI = researcherPropertiesMap.getOrDefault(UserFields.DO_YOU_HAVE_PI.getValue(), null);
            String pubmedID = researcherPropertiesMap.getOrDefault(UserFields.PUBMED_ID.getValue(), "");
            String scientificURL = researcherPropertiesMap.getOrDefault(UserFields.SCIENTIFIC_URL.getValue(), "");
            if (StringUtils.isNotEmpty(pubmedID) && StringUtils.isEmpty(
                            userPropertyDAO.findPropertyValueByPK(userId, UserFields.PUBMED_ID.getValue())) ||
                    StringUtils.isNotEmpty(scientificURL) && StringUtils.isEmpty(
                            userPropertyDAO.findPropertyValueByPK(userId, UserFields.SCIENTIFIC_URL.getValue()))) {
                hasUpdatedFields = true;
            } else if (CollectionUtils.isNotEmpty(userPropertyDAO
                    .findResearcherProperties(userId, isThePI, havePI, pubmedID, scientificURL))) {
                hasUpdatedFields = true;
            }
        }
        return hasUpdatedFields;
    }

    private void notifyAdmins(Integer userId) {
        String completed = userPropertyDAO.isProfileCompleted(userId);
        if (Boolean.parseBoolean(completed)) {
            try {
                emailNotifierService.sendNewResearcherCreatedMessage(userId, ResearcherService.ACTION_REGISTERED);
            } catch (IOException | TemplateException | MessagingException e) {
                logger().error("Error when notifying the admin(s) about the researcher action: " +
                        ResearcherService.ACTION_REGISTERED + ", for user: " +
                        userDAO.findUserById(userId).getDisplayName());
            }
        }
    }
}
