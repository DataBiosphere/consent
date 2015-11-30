package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.ResearchPurpose;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;

public interface ResearchPurposeAPI {

    ResearchPurpose createResearchPurpose(ResearchPurpose rec) throws IllegalArgumentException;

    ResearchPurpose updateResearchPurpose(ResearchPurpose rec, String id) throws IllegalArgumentException, NotFoundException, IOException;

    ResearchPurpose describeResearchPurpose(String id) throws NotFoundException, IOException;

    List<ResearchPurpose> describeResearchPurposes(String[] id) throws NotFoundException, IOException;

    void deleteResearchPurpose(String id) throws IllegalArgumentException, NotFoundException;

}
