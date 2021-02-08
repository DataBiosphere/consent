package org.broadinstitute.consent.http.service.users.handler;

import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.UserProperty;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;


public interface ResearcherService {

    List<UserProperty> setProperties(Map<String, String> researcherProperties, AuthUser authUser) throws NotFoundException, IllegalArgumentException;

    List<UserProperty> updateProperties(Map<String, String> researcherProperties, AuthUser authUser, Boolean validate) throws NotFoundException, IllegalArgumentException;

    Map<String, String> describeResearcherPropertiesMap(Integer userId) throws NotFoundException;

    void deleteResearcherProperties(Integer userId);

    Map<String, String> describeResearcherPropertiesForDAR(Integer userId);

    void deleteResearcherSpecificProperties(List<UserProperty> properties);

}
