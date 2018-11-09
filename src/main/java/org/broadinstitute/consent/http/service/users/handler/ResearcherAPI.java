package org.broadinstitute.consent.http.service.users.handler;
import com.google.inject.ImplementedBy;
import org.broadinstitute.consent.http.models.ResearcherProperty;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;


@ImplementedBy(DatabaseResearcherAPI.class)
public interface ResearcherAPI {

    List<ResearcherProperty> registerResearcher(Map<String, String> researcherProperties, Integer userId, Boolean validate) throws NotFoundException, IllegalArgumentException, UnsupportedOperationException;

    List<ResearcherProperty> updateResearcher(Map<String, String> researcherProperties, Integer userId, Boolean validate) throws NotFoundException, IllegalArgumentException;

    Map<String, String> describeResearcherPropertiesMap(Integer userId) throws NotFoundException;

    void deleteResearcherProperties(Integer userId);

    Map<String, String> describeResearcherPropertiesForDAR(Integer userId);
}
