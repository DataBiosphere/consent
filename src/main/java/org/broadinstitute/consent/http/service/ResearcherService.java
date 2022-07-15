package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import freemarker.template.TemplateException;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ResearcherService {
    private final UserPropertyDAO userPropertyDAO;
    private final UserDAO userDAO;
    private final EmailNotifierService emailNotifierService;
    private static final String ACTION_REGISTERED = "registered";

    protected Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    @Inject
    public ResearcherService(UserPropertyDAO userPropertyDAO, UserDAO userDAO, EmailNotifierService emailNotifierService) {
        this.userPropertyDAO = userPropertyDAO;
        this.userDAO = userDAO;
        this.emailNotifierService = emailNotifierService;
    }

    @Deprecated
    public List<UserProperty> setProperties(Map<String, String> researcherPropertiesMap, AuthUser authUser) throws NotFoundException, IllegalArgumentException {
        User user = validateAuthUser(authUser);
        researcherPropertiesMap.values().removeAll(Collections.singleton(null));
        Map<String, String> validatedProperties = validateExistentFields(researcherPropertiesMap);
        List<UserProperty> properties = getResearcherProperties(validatedProperties, user.getUserId());
        saveProperties(properties);
        notifyAdmins(user.getUserId());
        return describeResearcherProperties(user.getUserId());
    }

    @Deprecated
    public List<UserProperty> updateProperties(Map<String, String> researcherPropertiesMap, AuthUser authUser, Boolean validate) throws NotFoundException, IllegalArgumentException {
        User user = validateAuthUser(authUser);
        researcherPropertiesMap.values().removeAll(Collections.singleton(null));
        if (validate) validateRequiredFields(researcherPropertiesMap);
        Map<String, String> validatedProperties = validateExistentFields(researcherPropertiesMap);
        List<UserProperty> properties = getResearcherProperties(validatedProperties, user.getUserId());
        saveProperties(properties);
        return describeResearcherProperties(user.getUserId());
    }

    private void saveProperties(List<UserProperty> properties) {
        userPropertyDAO.deletePropertiesByUserAndKey(properties);
        userPropertyDAO.insertAll(properties);
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
        return userPropertyDAO.findResearcherPropertiesByUser(userId,
                UserFields.getValues());
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
